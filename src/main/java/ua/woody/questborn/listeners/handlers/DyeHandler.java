package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DyeHandler extends AbstractQuestHandler {
    private final Map<UUID, DyeData> preparedDyeItems = new HashMap<>();

    public DyeHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onPrepareCraft(PrepareItemCraftEvent e) {
        if (e.getViewers().isEmpty())
            return;
        if (!(e.getViewers().get(0) instanceof Player p))
            return;

        ItemStack result = e.getInventory().getResult();
        if (result == null || result.getType() == Material.AIR)
            return;

        Material itemType = result.getType();
        boolean isBanner = itemType.name().endsWith("_BANNER");
        boolean isLeatherArmor = itemType.name().startsWith("LEATHER_") &&
                (itemType.name().endsWith("_HELMET") ||
                        itemType.name().endsWith("_CHESTPLATE") ||
                        itemType.name().endsWith("_LEGGINGS") ||
                        itemType.name().endsWith("_BOOTS"));
        if (!isBanner && !isLeatherArmor)
            return;

        boolean hasDye = false;
        for (ItemStack ingredient : e.getInventory().getMatrix()) {
            if (ingredient == null || ingredient.getType() == Material.AIR)
                continue;
            String n = ingredient.getType().name();
            if (n.endsWith("_DYE") || n.equals("INK_SAC") || n.equals("GLOW_INK_SAC")) {
                hasDye = true;
                break;
            }
        }
        if (!hasDye)
            return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.DYE_ITEM)
            return;

        if (o.isTargetItem(result)) {
            int stage = (plugin.getPlayerDataStore().get(p.getUniqueId()).getTrackedQuestId() != null && plugin.getPlayerDataStore().get(p.getUniqueId()).getQuestData(plugin.getPlayerDataStore().get(p.getUniqueId()).getTrackedQuestId()) != null ? plugin.getPlayerDataStore().get(p.getUniqueId()).getQuestData(plugin.getPlayerDataStore().get(p.getUniqueId()).getTrackedQuestId()).getCurrentStage() : 1);
            preparedDyeItems.put(p.getUniqueId(),
                    new DyeData(q.getId(), stage, result.clone(), System.currentTimeMillis()));
        }
    }

    public void onCraftItemTake(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p))
            return;
        if (e.getSlotType() != InventoryType.SlotType.RESULT)
            return;

        ItemStack result = e.getCurrentItem();
        if (result == null || result.getType() == Material.AIR)
            return;

        Material itemType = result.getType();
        boolean isBanner = itemType.name().endsWith("_BANNER");
        boolean isLeatherArmor = itemType.name().startsWith("LEATHER_") &&
                (itemType.name().endsWith("_HELMET") ||
                        itemType.name().endsWith("_CHESTPLATE") ||
                        itemType.name().endsWith("_LEGGINGS") ||
                        itemType.name().endsWith("_BOOTS"));
        if (!isBanner && !isLeatherArmor)
            return;

        DyeData dyeData = preparedDyeItems.get(p.getUniqueId());
        if (dyeData == null)
            return;

        if (System.currentTimeMillis() - dyeData.timestamp > 5000) {
            preparedDyeItems.remove(p.getUniqueId());
            return;
        }

        QuestDefinition q = getActiveQuest(p);
        if (q == null) {
            preparedDyeItems.remove(p.getUniqueId());
            return;
        }

        int currentStage = (plugin.getPlayerDataStore().get(p.getUniqueId()).getTrackedQuestId() != null && plugin.getPlayerDataStore().get(p.getUniqueId()).getQuestData(plugin.getPlayerDataStore().get(p.getUniqueId()).getTrackedQuestId()) != null ? plugin.getPlayerDataStore().get(p.getUniqueId()).getQuestData(plugin.getPlayerDataStore().get(p.getUniqueId()).getTrackedQuestId()).getCurrentStage() : 1);
        if (!q.getId().equalsIgnoreCase(dyeData.questId) || currentStage != dyeData.stageNumber) {
            preparedDyeItems.remove(p.getUniqueId());
            return;
        }

        if (!result.isSimilar(dyeData.item)) {
            preparedDyeItems.remove(p.getUniqueId());
            return;
        }

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null) {
            preparedDyeItems.remove(p.getUniqueId());
            return;
        }

        if (o.getType() == QuestObjectiveType.DYE_ITEM && o.isTargetItem(result)) {
            progress(p, q, 1);
        }

        preparedDyeItems.remove(p.getUniqueId());
    }

    public void onDyeInventoryClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) {
            preparedDyeItems.remove(p.getUniqueId());
        }
    }

    private static class DyeData {
        final String questId;
        final int stageNumber;
        final ItemStack item;
        final long timestamp;

        DyeData(String questId, int stageNumber, ItemStack item, long timestamp) {
            this.questId = questId;
            this.stageNumber = stageNumber;
            this.item = item;
            this.timestamp = timestamp;
        }
    }
}
