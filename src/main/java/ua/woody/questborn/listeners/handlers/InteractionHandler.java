package ua.woody.questborn.listeners.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InteractionHandler extends AbstractQuestHandler {

    private final Map<UUID, Long> lastInteractCooldown = new HashMap<>();

    public InteractionHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onBlockInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();

        long now = System.currentTimeMillis();
        long lastTime = lastInteractCooldown.getOrDefault(p.getUniqueId(), 0L);
        if (now - lastTime < 800) {
            return;
        }

        lastInteractCooldown.put(p.getUniqueId(), now);

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.INTERACT_BLOCK) return;

        Material clicked = e.getClickedBlock().getType();
        if (o.isTargetBlock(clicked)) {
            progress(p, q, 1);
        }
    }

    public void onEntityLoveMode(EntityEnterLoveModeEvent e) {
        if (!(e.getHumanEntity() instanceof Player)) return;

        Player p = (Player) e.getHumanEntity();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.INTERACT_ENTITY) return;

        if (o.isTargetEntity(e.getEntityType())) {
            progress(p, q, 1);
        }
    }

    public void onUseItem(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.USE_ITEM) return;
        if (e.getItem() == null) return;

        if (o.isTargetItem(e.getItem().getType())) {
            progress(p, q, 1);
        }
    }

    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.CHAT_MESSAGE) return;

        String target = o.getMessage();
        String msg = e.getMessage().trim().toLowerCase();

        if (target == null || msg.contains(target.toLowerCase())) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                progress(p, q, 1);
            });
        }
    }

    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.EXECUTE_COMMAND) return;

        String configured = o.getCommand();
        String executed = e.getMessage().substring(1).trim().toLowerCase();

        if (configured == null || configured.isEmpty()) {
            progress(p, q, 1);
            return;
        }

        configured = configured.toLowerCase();

        if (executed.equals(configured) || executed.startsWith(configured + " ")) {
            progress(p, q, 1);
        }
    }

    public void onCakeConsume(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() != Material.CAKE) return;

        final Player p = e.getPlayer();
        final QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.CONSUME_ITEM) return;
        if (!o.isTargetItem(Material.CAKE)) return;

        final org.bukkit.block.Block cake = e.getClickedBlock();
        final org.bukkit.block.data.BlockData before = cake.getBlockData();

        final int bitesBefore;
        if (before instanceof org.bukkit.block.data.type.Cake cb) {
            bitesBefore = cb.getBites();
        } else {
            bitesBefore = 0;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            org.bukkit.block.data.BlockData after = cake.getBlockData();
            if (!(after instanceof org.bukkit.block.data.type.Cake ca)) return;

            int bitesAfter = ca.getBites();
            if (bitesAfter > bitesBefore) {
                progress(p, q, 1);
            }
        });
    }
}