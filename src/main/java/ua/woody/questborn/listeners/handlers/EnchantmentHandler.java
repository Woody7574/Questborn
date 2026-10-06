package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

public class EnchantmentHandler extends AbstractQuestHandler {
    public EnchantmentHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onEnchantItem(EnchantItemEvent e) {
        Player p = e.getEnchanter();

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null) return;

        if (o.getType() != QuestObjectiveType.ENCHANT_TABLE_USE) return;

        ItemStack item = e.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        boolean hasItemRequirement =
                (o.getItem() != null && !o.getItem().isEmpty()) ||
                        (o.getTargetItems() != null && !o.getTargetItems().isEmpty());

        if (hasItemRequirement) {
            if (o.isTargetItem(item)) {
                progress(p, q, 1);
            }
        } else {
            progress(p, q, 1);
        }
    }
}
