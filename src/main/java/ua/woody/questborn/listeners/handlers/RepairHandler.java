package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class RepairHandler extends AbstractQuestHandler {

    public RepairHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        InventoryType invType = e.getInventory().getType();

        // Обробляємо тільки грандстоун та крафт (ковадло обробляється в AnvilHandler)
        boolean isRepairInventory =
                invType == InventoryType.GRINDSTONE || // Грандстоун
                        invType == InventoryType.CRAFTING ||   // Крафт 2x2
                        invType == InventoryType.WORKBENCH;    // Верстак 3x3

        if (!isRepairInventory) return;
        if (e.getSlotType() != InventoryType.SlotType.RESULT) return;

        ItemStack result = e.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.ITEM_REPAIR) return;

        // ============ ПЕРЕВІРКА ДЛЯ КОЖНОГО ТИПУ ІНВЕНТАРЯ ============

        // Для грандстоуна - перевіряємо, чи це пошкоджений предмет
        if (invType == InventoryType.GRINDSTONE) {
            ItemStack firstItem = e.getInventory().getItem(0);
            ItemStack secondItem = e.getInventory().getItem(1);

            boolean hasDamagedItem = false;

            // Перевіряємо перший слот
            if (firstItem != null && firstItem.hasItemMeta() &&
                    firstItem.getItemMeta() instanceof Damageable damageable1) {
                if (damageable1.hasDamage() && damageable1.getDamage() > 0) {
                    hasDamagedItem = true;
                }
            }

            // Перевіряємо другий слот
            if (!hasDamagedItem && secondItem != null && secondItem.hasItemMeta() &&
                    secondItem.getItemMeta() instanceof Damageable damageable2) {
                if (damageable2.hasDamage() && damageable2.getDamage() > 0) {
                    hasDamagedItem = true;
                }
            }

            if (!hasDamagedItem) {
                return; // Жоден предмет не пошкоджений
            }
        }

        // Для крафту 2x2 та верстака 3x3 - перевіряємо, чи це ремонт двох однакових пошкоджених предметів
        if (invType == InventoryType.CRAFTING || invType == InventoryType.WORKBENCH) {
            ItemStack[] matrix = (invType == InventoryType.CRAFTING) ?
                    new ItemStack[4] : e.getInventory().getContents();

            if (invType == InventoryType.CRAFTING) {
                for (int i = 0; i < 4; i++) {
                    matrix[i] = e.getInventory().getItem(i);
                }
            }

            boolean foundRepairCraft = false;

            // Шукаємо дві копії одного й того ж пошкодженого предмета
            for (int i = 0; i < matrix.length; i++) {
                ItemStack item1 = matrix[i];
                if (item1 == null || item1.getType() == Material.AIR) continue;

                for (int j = i + 1; j < matrix.length; j++) {
                    ItemStack item2 = matrix[j];
                    if (item2 == null || item2.getType() == Material.AIR) continue;

                    // Перевіряємо, чи це однакові предмети
                    if (item1.getType() == item2.getType()) {
                        // Перевіряємо, чи обидва пошкоджені
                        boolean item1Damaged = isDamaged(item1);
                        boolean item2Damaged = isDamaged(item2);

                        // Для ремонту потрібно: обидва пошкоджені АБО один пошкоджений, а другий може бути ремонтним матеріалом
                        if ((item1Damaged && item2Damaged) ||
                                (item1Damaged && !item2Damaged) ||
                                (!item1Damaged && item2Damaged)) {
                            foundRepairCraft = true;
                            break;
                        }
                    }
                }
                if (foundRepairCraft) break;
            }

            if (!foundRepairCraft) {
                return; // Не знайдено двох однакових пошкоджених предметів
            }
        }

        // ============ ПІСЛЯ ВСІХ ПЕРЕВІРОК - ПЕРЕВІРКА ПРЕДМЕТА З КВЕСТУ ============

        // Перевіряємо тип предмета (якщо вказано)
        if (o.getItem() != null && !o.getItem().isEmpty() ||
                (o.getTargetItems() != null && !o.getTargetItems().isEmpty())) {
            if (!o.isTargetItem(result.getType())) {
                return; // Предмет не відповідає типу з квесту
            }
        }

        // Якщо дійшли до цього моменту - все в порядку
        progress(p, q, 1);
    }

    // Допоміжний метод для перевірки пошкодженого предмета
    private boolean isDamaged(ItemStack item) {
        if (item == null) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            return damageable.hasDamage() && damageable.getDamage() > 0;
        }
        return false;
    }
}