package ua.woody.questborn.listeners.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InteractionHandler extends AbstractQuestHandler {
    private final Map<UUID, Long> lastInteractCooldown = new HashMap<>();
    private final Map<UUID, Long> lastRawInteract = new HashMap<>();
    private static final long COOLDOWN_MS = 800L;

    private static class BlockInteractState {
        String questId;
        long startTime;
        int stage;
        final java.util.Set<org.bukkit.Location> blocks = new java.util.HashSet<>();
    }
    private final Map<UUID, BlockInteractState> interactStates = new HashMap<>();

    public InteractionHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void clearInteractState(UUID playerId) {
        interactStates.remove(playerId);
        lastInteractCooldown.remove(playerId);
        lastRawInteract.remove(playerId);
    }

    private boolean canInteract(Player p) {
        long now = System.currentTimeMillis();
        long lastRaw = lastRawInteract.getOrDefault(p.getUniqueId(), 0L);
        lastRawInteract.put(p.getUniqueId(), now);

        long lastAccepted = lastInteractCooldown.getOrDefault(p.getUniqueId(), 0L);
        boolean hasBypass = p.hasPermission("questborn.antiabuse.bypass");

        long timeSinceRaw = now - lastRaw;

        if (!hasBypass) {
            if (now - lastAccepted < COOLDOWN_MS) return false;
            lastInteractCooldown.put(p.getUniqueId(), now);
        } else {
            boolean isHolding = timeSinceRaw < 300;
            if (isHolding) {
                if (now - lastAccepted < 3000L) {
                    return false;
                }
            } else {
                if (now - lastAccepted < 50L) {
                    return false;
                }
            }
            lastInteractCooldown.put(p.getUniqueId(), now);
        }

        if (lastInteractCooldown.size() > 5000) {
            lastInteractCooldown.entrySet().removeIf(en -> now - en.getValue() > 60_000);
            lastRawInteract.entrySet().removeIf(en -> now - en.getValue() > 60_000);
        }

        return true;
    }

    public void onBlockInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();

        if (!canInteract(p)) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null) return;

        if (o.getType() != QuestObjectiveType.INTERACT_BLOCK) return;

        org.bukkit.block.Block clicked = e.getClickedBlock();
        if (o.isTargetBlock(clicked)) {
            boolean hasBypass = p.hasPermission("questborn.antiabuse.bypass");
            if (!hasBypass) {
                BlockInteractState state = interactStates.computeIfAbsent(p.getUniqueId(), k -> new BlockInteractState());
                ua.woody.questborn.model.PlayerQuestProgress data = plugin.getPlayerDataStore().get(p.getUniqueId());
                String currentId = data != null ? data.getTrackedQuestId() : q.getId();
                int currentStage = data != null ? (data.getTrackedQuestId() != null && data.getQuestData(data.getTrackedQuestId()) != null ? data.getQuestData(data.getTrackedQuestId()).getCurrentStage() : 1) : 1;
                long currentStartTime = data != null ? (data.getTrackedQuestId() != null && data.getQuestData(data.getTrackedQuestId()) != null ? data.getQuestData(data.getTrackedQuestId()).getStartTime() : 0L) : 0L;

                if (!java.util.Objects.equals(state.questId, currentId) || state.stage != currentStage || state.startTime != currentStartTime) {
                    state.questId = currentId;
                    state.stage = currentStage;
                    state.startTime = currentStartTime;
                    state.blocks.clear();
                }

                if (!state.blocks.add(e.getClickedBlock().getLocation())) {
                    return;
                }
            }

            progress(p, q, 1);
        }
    }

    public void onQuit(Player p) {
        lastInteractCooldown.remove(p.getUniqueId());
        lastRawInteract.remove(p.getUniqueId());
        interactStates.remove(p.getUniqueId());
    }

    public void onInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();

        if (!canInteract(p)) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null) return;

        if (o.getType() != QuestObjectiveType.INTERACT_ENTITY) return;

        if (o.isTargetEntity(e.getRightClicked())) {
            progress(p, q, 1);
        }
    }

    public void onUseItem(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null) return;

        if (o.getType() != QuestObjectiveType.USE_ITEM) return;
        if (e.getItem() == null) return;

        if (o.isTargetItem(e.getItem().getType())) {
            progress(p, q, 1);
        }
    }

    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String msg = e.getMessage() == null ? "" : e.getMessage().trim();

        plugin.getFoliaLib().getImpl().runAtEntity(p, __ -> {
            QuestDefinition q = getActiveQuest(p);
            if (q == null) return;

            QuestObjective o = getCurrentObjective(p, q);
            if (o == null) return;

            if (o.getType() != QuestObjectiveType.CHAT_MESSAGE) return;

            String target = o.getMessage();

            String msgLower = msg.toLowerCase();
            String targetLower = (target == null ? null : target.toLowerCase());

            if (targetLower == null || targetLower.isEmpty() || msgLower.contains(targetLower)) {
                progress(p, q, 1);
            }
        });
    }

    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null) return;

        if (o.getType() != QuestObjectiveType.EXECUTE_COMMAND) return;

        String executed = e.getMessage();
        if (executed == null) return;

        executed = executed.startsWith("/") ? executed.substring(1) : executed;
        executed = executed.trim().toLowerCase();

        String configured = o.getCommand();
        if (configured == null || configured.isEmpty()) {
            progress(p, q, 1);
            return;
        }

        configured = configured.trim().toLowerCase();

        if (executed.equals(configured) || executed.startsWith(configured + " ")) {
            progress(p, q, 1);
        }
    }

    public void onCakeConsume(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() != Material.CAKE) return;

        final Player p = e.getPlayer();

        final QuestDefinition qNow = getActiveQuest(p);
        if (qNow == null) return;

        final QuestObjective oNow = getCurrentObjective(p, qNow);
        if (oNow == null) return;

        if (oNow.getType() != QuestObjectiveType.CONSUME_ITEM) return;
        if (!oNow.isTargetItem(Material.CAKE)) return;

        final org.bukkit.block.Block cake = e.getClickedBlock();
        final org.bukkit.block.data.BlockData before = cake.getBlockData();

        final int bitesBefore = (before instanceof org.bukkit.block.data.type.Cake cb) ? cb.getBites() : 0;

        plugin.getFoliaLib().getImpl().runAtEntity(p, __ -> {
            QuestDefinition q = getActiveQuest(p);
            if (q == null) return;

            QuestObjective o = getCurrentObjective(p, q);
            if (o == null) return;

            if (o.getType() != QuestObjectiveType.CONSUME_ITEM) return;
            if (!o.isTargetItem(Material.CAKE)) return;

            org.bukkit.block.data.BlockData after = cake.getBlockData();
            if (after.getMaterial() == Material.AIR) {
                progress(p, q, 1);
                return;
            }
            if (!(after instanceof org.bukkit.block.data.type.Cake ca)) return;

            if (ca.getBites() > bitesBefore) {
                progress(p, q, 1);
            }
        });
    }
}
