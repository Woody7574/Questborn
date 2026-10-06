package ua.woody.questborn.listeners.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.*;

public class ItemInteractionHandler extends AbstractQuestHandler {
    private final Map<UUID, com.tcoded.folialib.wrapper.task.WrappedTask> holdTasks = new java.util.concurrent.ConcurrentHashMap<>();

    private static class ContainerOpenState {
        String questId;
        long startTime;
        int stage;
        final java.util.Set<org.bukkit.Location> locations = new java.util.HashSet<>();
    }
    private final Map<UUID, ContainerOpenState> containerStates = new HashMap<>();

    public ItemInteractionHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onArmorChange(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p))
            return;

        if (e.getInventory().getType() != InventoryType.PLAYER &&
                e.getSlotType() != InventoryType.SlotType.ARMOR &&
                !e.isShiftClick())
            return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.WEAR_ARMOR)
            return;

        plugin.getFoliaLib().getImpl().runAtEntity(p, __ -> {
            QuestDefinition q2 = getActiveQuest(p);
            if (q2 == null)
                return;

            QuestObjective o2 = getCurrentObjective(p, q2);
            if (o2 == null || o2.getType() != QuestObjectiveType.WEAR_ARMOR)
                return;

            int wearing = countArmor(p, o2);
            int needed = Math.max(1, o2.getAmount());
            int desired = Math.min(wearing, needed);

            PlayerQuestProgress data = plugin.getPlayerDataStore().get(p.getUniqueId());
            int currentProgress = plugin.getQuestManager().getProgressValue(data, q2);
            int current = Math.max(0, Math.min(currentProgress, needed));

            int diff = desired - current;
            if (diff != 0)
                progress(p, q2, diff);
        });
    }

    public void onArmorEquipInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = e.getItem();
        if (item == null) return;

        String name = item.getType().name();
        boolean isArmor = name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
                || name.equals("ELYTRA") || name.equals("TURTLE_HELMET")
                || name.equals("CARVED_PUMPKIN") || name.endsWith("_HEAD")
                || name.endsWith("_SKULL");
        if (!isArmor) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null || o.getType() != QuestObjectiveType.WEAR_ARMOR) return;

        plugin.getFoliaLib().getImpl().runAtEntity(p, __ -> {
            QuestDefinition q2 = getActiveQuest(p);
            if (q2 == null) return;

            QuestObjective o2 = getCurrentObjective(p, q2);
            if (o2 == null || o2.getType() != QuestObjectiveType.WEAR_ARMOR) return;

            int wearing = countArmor(p, o2);
            int needed = Math.max(1, o2.getAmount());
            int desired = Math.min(wearing, needed);

            PlayerQuestProgress data = plugin.getPlayerDataStore().get(p.getUniqueId());
            int currentProgress = plugin.getQuestManager().getProgressValue(data, q2);
            int current = Math.max(0, Math.min(currentProgress, needed));

            int diff = desired - current;
            if (diff != 0) progress(p, q2, diff);
        });
    }

    private int countArmor(Player p, QuestObjective o) {
        int c = 0;
        for (ItemStack item : p.getInventory().getArmorContents()) {
            if (item != null && item.getType() != Material.AIR && o.isTargetItem(item))
                c++;
        }
        return c;
    }

    private void stop(Player p) {
        stopHoldCheckerForPlayer(p.getUniqueId());
    }

    private void tryHoldStart(Player p) {
        QuestDefinition q = getActiveQuest(p);
        if (q == null) {
            stop(p);
            return;
        }

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null || o.getType() != QuestObjectiveType.HOLD_ITEM) {
            stop(p);
            return;
        }

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR || !o.isTargetItem(hand)) {
            stop(p);
            return;
        }

        startHoldCheckerForPlayer(p);
    }

    public void onSlotChange(PlayerItemHeldEvent e) {
        plugin.getFoliaLib().getImpl().runAtEntity(e.getPlayer(), __ -> tryHoldStart(e.getPlayer()));
    }

    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p) {
            plugin.getFoliaLib().getImpl().runAtEntity(p, __ -> tryHoldStart(p));
        }
    }

    public void onSwap(PlayerSwapHandItemsEvent e) {
        plugin.getFoliaLib().getImpl().runAtEntity(e.getPlayer(), __ -> tryHoldStart(e.getPlayer()));
    }

    private void startHoldCheckerForPlayer(Player p) {
        UUID id = p.getUniqueId();
        stopHoldCheckerForPlayer(id);
        com.tcoded.folialib.wrapper.task.WrappedTask task = plugin.getFoliaLib().getImpl().runAtEntityTimer(p, () -> {
            if (!p.isOnline()) {
                stopHoldCheckerForPlayer(id);
                return;
            }

            QuestDefinition q = getActiveQuest(p);
            if (q == null) {
                stopHoldCheckerForPlayer(id);
                return;
            }

            QuestObjective o = getCurrentObjective(p, q);
            if (o == null || o.getType() != QuestObjectiveType.HOLD_ITEM) {
                stopHoldCheckerForPlayer(id);
                return;
            }

            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || !o.isTargetItem(hand)) {
                stopHoldCheckerForPlayer(id);
                return;
            }

            progress(p, q, 1);
        }, 20L, 20L);
        holdTasks.put(id, task);
    }

    private void stopHoldCheckerForPlayer(UUID id) {
        com.tcoded.folialib.wrapper.task.WrappedTask t = holdTasks.remove(id);
        if (t != null) t.cancel();
    }

    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() == QuestObjectiveType.DROP_ITEM &&
                o.isTargetItem(e.getItemDrop().getItemStack())) {
            ItemStack item = e.getItemDrop().getItemStack();
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "questborn_dropped");
                if (meta.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.BYTE)) {
                    return;
                }
                meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                item.setItemMeta(meta);
                e.getItemDrop().setItemStack(item);
            }

            progress(p, q, e.getItemDrop().getItemStack().getAmount());
        }
    }

    public void onOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p))
            return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.OPEN_CONTAINER)
            return;

        InventoryType type = e.getInventory().getType();
        if (!isRealContainer(type))
            return;

        if (o.isTargetContainerType(type.name())) {
            org.bukkit.Location loc = e.getInventory().getLocation();
            if (loc != null) {
                boolean hasBypass = p.hasPermission("questborn.antiabuse.bypass");
                if (!hasBypass) {
                    ContainerOpenState state = containerStates.computeIfAbsent(p.getUniqueId(), k -> new ContainerOpenState());
                    PlayerQuestProgress data = plugin.getPlayerDataStore().get(p.getUniqueId());
                    String currentId = data != null ? data.getTrackedQuestId() : q.getId();
                    int currentStage = data != null ? (data.getTrackedQuestId() != null && data.getQuestData(data.getTrackedQuestId()) != null ? data.getQuestData(data.getTrackedQuestId()).getCurrentStage() : 1) : 1;
                    long currentStartTime = data != null ? (data.getTrackedQuestId() != null && data.getQuestData(data.getTrackedQuestId()) != null ? data.getQuestData(data.getTrackedQuestId()).getStartTime() : 0L) : 0L;

                    if (!java.util.Objects.equals(state.questId, currentId) || state.stage != currentStage || state.startTime != currentStartTime) {
                        state.questId = currentId;
                        state.stage = currentStage;
                        state.startTime = currentStartTime;
                        state.locations.clear();
                    }

                    if (!state.locations.add(loc)) {
                        return;
                    }
                }
            }
            progress(p, q, 1);
        }
    }

    private boolean isRealContainer(InventoryType type) {
        return switch (type.name()) {
            case "CHEST", "BARREL", "SHULKER_BOX", "DISPENSER", "DROPPER", "HOPPER",
                    "FURNACE", "BLAST_FURNACE", "SMOKER", "BREWING", "ENCHANTING",
                    "ANVIL", "BEACON", "WORKBENCH", "LECTERN", "CARTOGRAPHY_TABLE",
                    "GRINDSTONE", "SMITHING_TABLE", "STONECUTTER", "LOOM" ->
                true;
            default -> false;
        };
    }

    public void onSign(SignChangeEvent e) {
        Player p = e.getPlayer();

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.SIGN_EDIT)
            return;

        String need = o.getMessage();
        if (need == null || need.isEmpty()) {
            progress(p, q, 1);
            return;
        }

        for (String line : e.getLines()) {
            if (line != null && line.contains(need)) {
                progress(p, q, 1);
                break;
            }
        }
    }

    public void onBook(PlayerEditBookEvent e) {
        Player p = e.getPlayer();

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.BOOK_EDIT)
            return;

        String requiredMessage = o.getMessage();

        if (requiredMessage == null || requiredMessage.isEmpty()) {
            progress(p, q, 1);
            return;
        }

        BookMeta meta = e.getNewBookMeta();
        List<String> pages = meta.getPages();

        for (String page : pages) {
            if (page != null && page.contains(requiredMessage)) {
                progress(p, q, 1);
                return;
            }
        }
    }

    public void onQuit(PlayerQuitEvent e) {
        stop(e.getPlayer());
        containerStates.remove(e.getPlayer().getUniqueId());
    }
}
