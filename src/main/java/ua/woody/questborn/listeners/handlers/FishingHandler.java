package ua.woody.questborn.listeners.handlers;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

public class FishingHandler extends AbstractQuestHandler {
    public FishingHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH)
            return;

        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.ITEM_FISH)
            return;

        boolean hasItemRequirement = (o.getItem() != null && !o.getItem().isEmpty()) ||
                (o.getTargetItems() != null && !o.getTargetItems().isEmpty());

        if (!hasItemRequirement) {
            progress(p, q, 1);
            return;
        }

        if (e.getCaught() instanceof Item caughtItem) {
            ItemStack stack = caughtItem.getItemStack();
            if (o.isTargetItem(stack)) {
                progress(p, q, stack.getAmount());
            }
        }
    }
}
