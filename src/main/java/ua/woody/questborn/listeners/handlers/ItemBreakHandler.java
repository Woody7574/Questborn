package ua.woody.questborn.listeners.handlers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;

public class ItemBreakHandler extends AbstractQuestHandler {

    public ItemBreakHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onItemBreak(org.bukkit.event.player.PlayerItemBreakEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.ITEM_BREAK) return;

        ItemStack brokenItem = e.getBrokenItem();
        if (o.isTargetItem(brokenItem.getType())) {
            progress(p, q, 1);
        }
    }
}