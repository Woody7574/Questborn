package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnvilHandler extends AbstractQuestHandler {

    private final Map<UUID, ItemStack> preparedAnvilResults = new HashMap<>();
    private final Map<UUID, String> preparedAnvilOriginalNames = new HashMap<>();

    public AnvilHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent e) {
        if (e.getViewers().isEmpty()) return;

        Player p = (Player) e.getViewers().get(0);
        ItemStack result = e.getResult();
        ItemStack firstItem = e.getInventory().getItem(0);

        // Зберігаємо результат ковадла
        if (result != null && result.getType() != Material.AIR) {
            preparedAnvilResults.put(p.getUniqueId(), result.clone());

            // Зберігаємо оригінальну назву першого предмета
            if (firstItem != null && firstItem.hasItemMeta()) {
                ItemMeta meta = firstItem.getItemMeta();
                if (meta.hasDisplayName()) {
                    preparedAnvilOriginalNames.put(p.getUniqueId(), meta.getDisplayName());
                } else {
                    preparedAnvilOriginalNames.put(p.getUniqueId(), null);
                }
            } else {
                preparedAnvilOriginalNames.put(p.getUniqueId(), null);
            }
        } else {
            preparedAnvilResults.remove(p.getUniqueId());
            preparedAnvilOriginalNames.remove(p.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAnvilTake(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getInventory().getType() != InventoryType.ANVIL) return;

        if (e.getSlot() != 2) return;

        // Тільки кліки, які призводять до взяття предмета
        if (e.getClick() == ClickType.LEFT || e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_LEFT) {
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            // Отримуємо збережений результат
            ItemStack preparedResult = preparedAnvilResults.get(p.getUniqueId());
            if (preparedResult == null) return;

            // Перевіряємо схожість предметів
            if (!clickedItem.isSimilar(preparedResult)) return;

            // Запобігаємо повторному обробленню того самого предмета
            preparedAnvilResults.remove(p.getUniqueId());

            // ЗАПАМ'ЯТАЄМО ВМІСТ КОВАДЛА ПЕРЕД ТИМ, ЯК ГРАВЕЦЬ ВІЗЬМЕ ПРЕДМЕТ
            AnvilInventory anvil = (AnvilInventory) e.getInventory();
            ItemStack firstItem = anvil.getItem(0);  // Перший слот ПЕРЕД взяттям
            ItemStack secondItem = anvil.getItem(1); // Другий слот ПЕРЕД взяттям

            // Чекаємо 2 тіки, щоб перевірити, чи предмет дійсно забрано
            getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> {
                // Перевіряємо, чи гравець ще онлайн
                if (!p.isOnline()) return;

                // Перевіряємо, чи гравець ще відкриває ковадло
                if (p.getOpenInventory().getType() != InventoryType.ANVIL) {
                    return;
                }

                AnvilInventory anvilAfter = (AnvilInventory) p.getOpenInventory().getTopInventory();
                ItemStack resultSlot = anvilAfter.getItem(2);

                // Якщо слот порожній - предмет забрано
                if (resultSlot == null || resultSlot.getType() == Material.AIR) {
                    // ОТРИМУЄМО ПОТОЧНИЙ КВЕСТ
                    QuestDefinition q = getActiveQuest(p);
                    if (q == null) return;

                    var o = q.getObjective();

                    // ============ ПЕРЕВІРКА ДЛЯ ANVIL_USE ============
                    if (o.getType() == QuestObjectiveType.ANVIL_USE) {
                        if (!o.getTargetItems().isEmpty() || (o.getItem() != null && !o.getItem().isEmpty())) {
                            if (o.isTargetItem(clickedItem.getType())) {
                                progress(p, q, 1);
                            }
                        } else {
                            progress(p, q, 1);
                        }
                    }
                    // ============ ПЕРЕВІРКА ДЛЯ ITEM_REPAIR ============
                    else if (o.getType() == QuestObjectiveType.ITEM_REPAIR) {
                        // Перевіряємо, чи це ремонт (у другому слоті є предмет - матеріал для ремонту)
                        boolean isRepair = false;

                        if (firstItem != null && secondItem != null) {
                            // Два предмети в ковадлі - можливо ремонт

                            // Перевірка за пошкодженням
                            if (firstItem.hasItemMeta() && firstItem.getItemMeta() instanceof Damageable damageable1) {
                                if (damageable1.hasDamage() && damageable1.getDamage() > 0) {
                                    // Перший предмет пошкоджений
                                    isRepair = true;
                                }
                            }

                            // Перевірка за типами предметів для ремонту
                            if (!isRepair) {
                                Material firstType = firstItem.getType();
                                Material secondType = secondItem.getType();

                                // Перевірка на ремонт предметів одним матеріалом
                                String repairMaterial = getRepairMaterial(firstType);
                                if (repairMaterial != null && secondType.name().equals(repairMaterial)) {
                                    isRepair = true;
                                }
                            }
                        }

                        // Якщо це ремонт, продовжуємо
                        if (isRepair) {
                            boolean hasItemRequirement = (o.getItem() != null && !o.getItem().isEmpty()) ||
                                    (o.getTargetItems() != null && !o.getTargetItems().isEmpty());

                            if (hasItemRequirement) {
                                if (o.isTargetItem(clickedItem.getType())) {
                                    progress(p, q, 1);
                                }
                            } else {
                                // Якщо предмет не вказано - будь-який ремонт підходить
                                progress(p, q, 1);
                            }
                        }
                    }
                    // ============ ПЕРЕВІРКА ДЛЯ ITEM_RENAME ============
                    else if (o.getType() == QuestObjectiveType.ITEM_RENAME) {
                        // Отримуємо оригінальну назву з підготовлених даних
                        String originalName = preparedAnvilOriginalNames.get(p.getUniqueId());

                        // Перевіряємо, чи результат має назву
                        if (clickedItem.hasItemMeta()) {
                            ItemMeta meta = clickedItem.getItemMeta();
                            if (meta.hasDisplayName()) {
                                String newName = meta.getDisplayName();

                                // Перевіряємо, чи назва змінилась
                                boolean wasRenamed = true;
                                if (originalName != null && originalName.equals(newName)) {
                                    wasRenamed = false;
                                }

                                if (wasRenamed) {
                                    // ВИЗНАЧАЄМО, ЧИ ПІДХОДИТЬ НАЗВА
                                    boolean nameMatches = true;

                                    // Якщо вказано конкретну назву в квесті - перевіряємо її
                                    if (o.getMessage() != null && !o.getMessage().isEmpty()) {
                                        // Можна використовувати різні стратегії перевірки:
                                        // 1. Точне співпадіння (з ігноруванням регістру)
                                        // 2. Містить підрядок
                                        // 3. Використовує регулярний вираз

                                        // Тут використовуємо містить підрядок (більш гнучко)
                                        nameMatches = newName.contains(o.getMessage());

                                        // Або точне співпадіння (з ігноруванням регістру):
                                        // nameMatches = newName.equalsIgnoreCase(o.getMessage());
                                    }

                                    // ВИЗНАЧАЄМО, ЧИ ПІДХОДИТЬ ПРЕДМЕТ
                                    boolean itemMatches = true;

                                    // Якщо вказано конкретний предмет - перевіряємо його
                                    if (o.getItem() != null && !o.getItem().isEmpty() ||
                                            (o.getTargetItems() != null && !o.getTargetItems().isEmpty())) {
                                        itemMatches = o.isTargetItem(clickedItem.getType());
                                    }

                                    // Якщо і назва, і предмет підходять - зараховуємо
                                    if (nameMatches && itemMatches) {
                                        progress(p, q, 1);
                                    }
                                }
                            }
                        }

                        // Очищаємо дані
                        preparedAnvilOriginalNames.remove(p.getUniqueId());
                    }
                }
            }, 2L);
        }
    }

    // Допоміжний метод для визначення матеріалу ремонту
    private String getRepairMaterial(Material toolType) {
        String name = toolType.name();

        if (name.contains("DIAMOND_")) return "DIAMOND";
        if (name.contains("IRON_")) return "IRON_INGOT";
        if (name.contains("GOLDEN_")) return "GOLD_INGOT";
        if (name.contains("GOLD_") && !name.contains("GOLDEN_")) return "GOLD_INGOT";
        if (name.contains("STONE_")) return "COBBLESTONE";
        if (name.contains("WOODEN_")) return "OAK_PLANKS";
        if (name.contains("NETHERITE_")) return "NETHERITE_INGOT";
        if (name.contains("LEATHER_")) return "LEATHER";
        if (name.contains("CHAINMAIL_")) return "IRON_INGOT";

        // Специфічні предмети
        switch (toolType) {
            case ELYTRA: return "PHANTOM_MEMBRANE";
            case TURTLE_HELMET: return "SCUTE";
            case CARVED_PUMPKIN: return "PUMPKIN";
            case SHIELD: return "OAK_PLANKS";
            case BOW: return "STRING";
            case CROSSBOW: return "STRING";
            case FISHING_ROD: return "STRING";
            case FLINT_AND_STEEL: return "IRON_INGOT";
            case SHEARS: return "IRON_INGOT";
            default: return null;
        }
    }

    @EventHandler
    public void onAnvilInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (e.getInventory().getType() != InventoryType.ANVIL) return;

        // Очищаємо дані ковадла при закритті інвентаря
        preparedAnvilResults.remove(p.getUniqueId());
        preparedAnvilOriginalNames.remove(p.getUniqueId());
    }
}