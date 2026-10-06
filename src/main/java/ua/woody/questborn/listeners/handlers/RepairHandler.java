package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

public class RepairHandler extends AbstractQuestHandler {
    public RepairHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p))
            return;

        Inventory top = e.getView().getTopInventory();
        InventoryType invType = top != null ? top.getType() : e.getInventory().getType();

        boolean isRepairInventory = invType == InventoryType.GRINDSTONE ||
                invType == InventoryType.CRAFTING ||
                invType == InventoryType.WORKBENCH;

        if (!isRepairInventory)
            return;
        if (e.getSlotType() != InventoryType.SlotType.RESULT)
            return;

        ItemStack result = e.getCurrentItem();
        if (result == null || result.getType() == Material.AIR)
            return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.ITEM_REPAIR)
            return;

        if (invType == InventoryType.GRINDSTONE) {
            ItemStack first = top.getItem(0);
            ItemStack second = top.getItem(1);

            boolean hasDamaged = isDamaged(first) || isDamaged(second);
            if (!hasDamaged)
                return;
        }

        if (invType == InventoryType.CRAFTING || invType == InventoryType.WORKBENCH) {
            if (!(top instanceof CraftingInventory ci))
                return;

            ItemStack[] matrix = ci.getMatrix();
            if (!foundRepairPair(matrix))
                return;
        }

        boolean hasItemRequirement = (o.getItem() != null && !o.getItem().isEmpty()) ||
                (o.getTargetItems() != null && !o.getTargetItems().isEmpty());

        if (hasItemRequirement && !o.isTargetItem(result))
            return;

        progress(p, q, 1);
    }

    private boolean foundRepairPair(ItemStack[] matrix) {
        if (matrix == null || matrix.length == 0)
            return false;

        for (int i = 0; i < matrix.length; i++) {
            ItemStack a = matrix[i];
            if (a == null || a.getType() == Material.AIR)
                continue;

            for (int j = i + 1; j < matrix.length; j++) {
                ItemStack b = matrix[j];
                if (b == null || b.getType() == Material.AIR)
                    continue;

                if (a.getType() != b.getType())
                    continue;

                boolean dmgA = isDamaged(a);
                boolean dmgB = isDamaged(b);

                if (dmgA || dmgB)
                    return true;
            }
        }
        return false;
    }

    private boolean isDamaged(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return false;
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable d))
            return false;
        return d.hasDamage() && d.getDamage() > 0;
    }
}
