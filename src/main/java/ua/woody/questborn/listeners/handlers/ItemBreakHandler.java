package ua.woody.questborn.listeners.handlers;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

public class ItemBreakHandler extends AbstractQuestHandler {
    public ItemBreakHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onItemBreak(org.bukkit.event.player.PlayerItemBreakEvent e) {
        Player p = e.getPlayer();

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.ITEM_BREAK)
            return;

        ItemStack brokenItem = e.getBrokenItem();
        if (brokenItem == null)
            return;

        if (o.isTargetItem(brokenItem)) {
            progress(p, q, 1);
        }
    }
}
