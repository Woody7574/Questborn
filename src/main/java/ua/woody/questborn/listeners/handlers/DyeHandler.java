package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DyeHandler extends AbstractQuestHandler {

    private final Map<UUID, DyeData> preparedDyeItems = new HashMap<>();

    public DyeHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        if (e.getViewers().isEmpty()) return;
        if (!(e.getViewers().get(0) instanceof Player p)) return;

        // Перевіряємо результат
        ItemStack result = e.getInventory().getResult();
        if (result == null || result.getType() == Material.AIR) return;

        // Перевіряємо, чи це прапор або шкіряна броня
        Material itemType = result.getType();
        boolean isBanner = itemType.name().endsWith("_BANNER");
        boolean isLeatherArmor = itemType.name().startsWith("LEATHER_") &&
                (itemType.name().endsWith("_HELMET") ||
                        itemType.name().endsWith("_CHESTPLATE") ||
                        itemType.name().endsWith("_LEGGINGS") ||
                        itemType.name().endsWith("_BOOTS"));

        if (!isBanner && !isLeatherArmor) return;

        // Перевіряємо, чи є у рецепті фарба
        boolean hasDye = false;
        ItemStack[] matrix = e.getInventory().getMatrix();
        for (ItemStack ingredient : matrix) {
            if (ingredient != null && !ingredient.getType().equals(Material.AIR)) {
                String materialName = ingredient.getType().name();
                // Перевіряємо різні типи фарби
                if (materialName.endsWith("_DYE") ||
                        materialName.equals("INK_SAC") ||
                        materialName.equals("GLOW_INK_SAC") ||
                        materialName.equals("RED_DYE") ||
                        materialName.equals("GREEN_DYE") ||
                        materialName.equals("BLUE_DYE") ||
                        materialName.equals("WHITE_DYE") ||
                        materialName.equals("BLACK_DYE") ||
                        materialName.equals("YELLOW_DYE") ||
                        materialName.equals("PURPLE_DYE") ||
                        materialName.equals("CYAN_DYE") ||
                        materialName.equals("LIGHT_GRAY_DYE") ||
                        materialName.equals("GRAY_DYE") ||
                        materialName.equals("PINK_DYE") ||
                        materialName.equals("LIME_DYE") ||
                        materialName.equals("MAGENTA_DYE") ||
                        materialName.equals("ORANGE_DYE") ||
                        materialName.equals("LIGHT_BLUE_DYE") ||
                        materialName.equals("BROWN_DYE")) {
                    hasDye = true;
                    break;
                }
            }
        }

        if (!hasDye) return; // Немає фарби у рецепті

        // Перевіряємо квест
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.DYE_ITEM) return;

        // Перевіряємо, чи це цільовий предмет
        if (o.isTargetItem(itemType)) {
            // Зберігаємо інформацію про те, що гравець підготував фарбування
            preparedDyeItems.put(p.getUniqueId(), new DyeData(result.clone(), System.currentTimeMillis()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCraftItemTake(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        // Перевіряємо тільки кліки по результативних слотах
        if (e.getSlotType() != InventoryType.SlotType.RESULT) return;

        ItemStack result = e.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) return;

        // Перевіряємо, чи це прапор або шкіряна броня
        Material itemType = result.getType();
        boolean isBanner = itemType.name().endsWith("_BANNER");
        boolean isLeatherArmor = itemType.name().startsWith("LEATHER_") &&
                (itemType.name().endsWith("_HELMET") ||
                        itemType.name().endsWith("_CHESTPLATE") ||
                        itemType.name().endsWith("_LEGGINGS") ||
                        itemType.name().endsWith("_BOOTS"));

        if (!isBanner && !isLeatherArmor) return;

        // Перевіряємо, чи це підготовлене фарбування
        DyeData dyeData = preparedDyeItems.get(p.getUniqueId());
        if (dyeData == null) return;

        // Перевіряємо схожість та актуальність (не старше 5 секунд)
        if (!result.isSimilar(dyeData.item) ||
                System.currentTimeMillis() - dyeData.timestamp > 5000) {
            preparedDyeItems.remove(p.getUniqueId());
            return;
        }

        QuestDefinition q = getActiveQuest(p);
        if (q == null) {
            preparedDyeItems.remove(p.getUniqueId());
            return;
        }

        var o = q.getObjective();
        if (o.getType() == QuestObjectiveType.DYE_ITEM && o.isTargetItem(result.getType())) {
            progress(p, q, 1);
        }

        // Завжди видаляємо після обробки, навіть якщо квест не знайдено
        preparedDyeItems.remove(p.getUniqueId());
    }

    // Очищення даних фарбування при закритті інвентаря
    @EventHandler
    public void onDyeInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;

        // Очищаємо дані фарбування при закритті інвентаря
        preparedDyeItems.remove(p.getUniqueId());
    }

    // Допоміжний клас для зберігання даних про фарбування
    private static class DyeData {
        final ItemStack item;
        final long timestamp;

        DyeData(ItemStack item, long timestamp) {
            this.item = item;
            this.timestamp = timestamp;
        }
    }
}