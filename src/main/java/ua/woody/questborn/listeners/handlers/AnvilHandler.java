package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnvilHandler extends AbstractQuestHandler {
    private final Map<UUID, ItemStack> preparedAnvilResults = new HashMap<>();
    private final Map<UUID, String> preparedAnvilOriginalNames = new HashMap<>();

    public AnvilHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onAnvilPrepare(org.bukkit.event.inventory.PrepareAnvilEvent e) {
        if (e.getViewers().isEmpty()) return;
        if (!(e.getViewers().get(0) instanceof Player p)) return;

        ItemStack result = e.getResult();
        ItemStack firstItem = e.getInventory().getItem(0);

        if (result != null && result.getType() != Material.AIR) {
            preparedAnvilResults.put(p.getUniqueId(), result.clone());

            if (firstItem != null && firstItem.hasItemMeta()) {
                ItemMeta meta = firstItem.getItemMeta();
                preparedAnvilOriginalNames.put(p.getUniqueId(), meta.hasDisplayName() ? meta.getDisplayName() : null);
            } else {
                preparedAnvilOriginalNames.put(p.getUniqueId(), null);
            }
        } else {
            preparedAnvilResults.remove(p.getUniqueId());
            preparedAnvilOriginalNames.remove(p.getUniqueId());
        }
    }

    public void onAnvilTake(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getInventory().getType() != org.bukkit.event.inventory.InventoryType.ANVIL) return;
        if (e.getSlot() != 2) return;

        if (!(e.getClick() == org.bukkit.event.inventory.ClickType.LEFT ||
                e.getClick() == org.bukkit.event.inventory.ClickType.RIGHT ||
                e.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_LEFT ||
                e.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT ||
                e.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY ||
                e.getClick() == org.bukkit.event.inventory.ClickType.DROP ||
                e.getClick() == org.bukkit.event.inventory.ClickType.CONTROL_DROP)) {
            return;
        }

        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        ItemStack preparedResult = preparedAnvilResults.get(p.getUniqueId());
        if (preparedResult == null) return;

        if (!clickedItem.isSimilar(preparedResult)) return;

        AnvilInventory anvil = (AnvilInventory) e.getInventory();
        ItemStack firstItem = anvil.getItem(0);
        ItemStack secondItem = anvil.getItem(1);

        final ItemStack firstItemClone = firstItem != null ? firstItem.clone() : null;
        final ItemStack secondItemClone = secondItem != null ? secondItem.clone() : null;
        final ItemStack clickedItemClone = clickedItem.clone();
        final String originalName = preparedAnvilOriginalNames.get(p.getUniqueId());

        final QuestDefinition q = getActiveQuest(p);
        if (q == null) {
            return;
        }

        plugin.getFoliaLib().getImpl().runAtEntity(p, __ -> {
            if (!p.isOnline()) {
                return;
            }

            QuestObjective o = getCurrentObjective(p, q);
            if (o == null) {
                return;
            }

            if (o.getType() == QuestObjectiveType.ANVIL_USE) {
                if (hasItemRequirement(o)) {
                    if (o.isTargetItem(clickedItemClone)) {
                        progress(p, q, 1);
                    }
                } else {
                    progress(p, q, 1);
                }
            }

            else if (o.getType() == QuestObjectiveType.ITEM_REPAIR) {
                boolean isRepair = isRepairAction(firstItemClone, secondItemClone, clickedItemClone);

                if (isRepair) {
                    if (hasItemRequirement(o)) {
                        if (o.isTargetItem(clickedItemClone)) {
                            progress(p, q, 1);
                        }
                    } else {
                        progress(p, q, 1);
                    }
                }
            }

            else if (o.getType() == QuestObjectiveType.ITEM_RENAME) {
                if (clickedItemClone.hasItemMeta()) {
                    ItemMeta meta = clickedItemClone.getItemMeta();
                    if (meta != null && meta.hasDisplayName()) {
                        String newName = meta.getDisplayName();
                        boolean wasRenamed = originalName == null || !originalName.equals(newName);

                        if (wasRenamed) {
                            boolean nameMatches = o.getMessage() == null || o.getMessage().isEmpty() ||
                                    newName.contains(o.getMessage());
                            boolean itemMatches = !hasItemRequirement(o) || o.isTargetItem(clickedItemClone);

                            if (nameMatches && itemMatches) {
                                progress(p, q, 1);
                            }
                        }
                    }
                }
            }

            else if (o.getType() == QuestObjectiveType.ITEM_ENCHANT) {
                boolean enchantmentsAdded = false;
                if (firstItemClone != null) {
                    Map<org.bukkit.enchantments.Enchantment, Integer> before = firstItemClone.getEnchantments();
                    Map<org.bukkit.enchantments.Enchantment, Integer> after = clickedItemClone.getEnchantments();

                    if (after.size() > before.size()) {
                        enchantmentsAdded = true;
                    } else {
                        for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : after.entrySet()) {
                            if (entry.getValue() > before.getOrDefault(entry.getKey(), 0)) {
                                enchantmentsAdded = true;
                                break;
                            }
                        }
                    }
                }

                if (!enchantmentsAdded && secondItemClone != null && secondItemClone.getType() == Material.ENCHANTED_BOOK) {
                    enchantmentsAdded = true;
                }

                if (enchantmentsAdded) {
                    if (hasItemRequirement(o)) {
                        if (o.isTargetItem(clickedItemClone)) {
                            progress(p, q, 1);
                        }
                    } else {
                        progress(p, q, 1);
                    }
                }
            }
        });
    }

    private boolean hasItemRequirement(QuestObjective o) {
        return (o.getItem() != null && !o.getItem().isEmpty()) ||
                (o.getTargetItems() != null && !o.getTargetItems().isEmpty()) ||
                (o.getTargetMaterials() != null && !o.getTargetMaterials().isEmpty()) ||
                (o.getTargetBlockIds() != null && !o.getTargetBlockIds().isEmpty());
    }

    private boolean isRepairAction(ItemStack firstItem, ItemStack secondItem, ItemStack result) {
        if (firstItem == null || secondItem == null) return false;

        if (firstItem.hasItemMeta() && firstItem.getItemMeta() instanceof Damageable dmg) {
            if (dmg.hasDamage() && dmg.getDamage() > 0) {
                return true;
            }
        }

        Material firstType = firstItem.getType();
        Material secondType = secondItem.getType();
        String repairMaterial = getRepairMaterial(firstType);

        return repairMaterial != null && secondType.name().equals(repairMaterial);
    }

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

        return switch (toolType) {
            case ELYTRA -> "PHANTOM_MEMBRANE";
            case TURTLE_HELMET -> "SCUTE";
            case CARVED_PUMPKIN -> "PUMPKIN";
            case SHIELD -> "OAK_PLANKS";
            case BOW, CROSSBOW, FISHING_ROD -> "STRING";
            case FLINT_AND_STEEL, SHEARS -> "IRON_INGOT";
            default -> null;
        };
    }

    private void cleanup(UUID uuid) {
        preparedAnvilOriginalNames.remove(uuid);
        preparedAnvilResults.remove(uuid);
    }

    public void onAnvilInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        cleanup(p.getUniqueId());
    }
}
