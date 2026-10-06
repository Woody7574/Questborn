package ua.woody.questborn.listeners.handlers;

import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

public class TradeHandler extends AbstractQuestHandler {
    public TradeHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onVillagerTrade(PlayerTradeEvent e) {
        Player p = e.getPlayer();
        AbstractVillager trader = e.getVillager();

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        QuestObjectiveType type = o.getType();
        if (type != QuestObjectiveType.VILLAGER_SELL_ITEM && type != QuestObjectiveType.VILLAGER_BUY_ITEM)
            return;

        if (!o.isTargetEntity(trader))
            return;

        boolean hasItemRequirement = (o.getItem() != null && !o.getItem().isEmpty()) ||
                (o.getTargetItems() != null && !o.getTargetItems().isEmpty());

        MerchantRecipe recipe = e.getTrade();
        if (recipe == null)
            return;

        if (!hasItemRequirement) {
            int amount = 1;
            if (!recipe.getIngredients().isEmpty() && recipe.getIngredients().get(0) != null) {
                amount = recipe.getIngredients().get(0).getAmount();
            }
            progress(p, q, amount);
            return;
        }

        int amountProgress = 0;

        if (type == QuestObjectiveType.VILLAGER_SELL_ITEM) {
            for (ItemStack ingredient : recipe.getIngredients()) {
                if (ingredient != null && o.isTargetItem(ingredient)) {
                    amountProgress += ingredient.getAmount();
                }
            }
        } else if (type == QuestObjectiveType.VILLAGER_BUY_ITEM) {
            ItemStack result = recipe.getResult();
            if (result != null && o.isTargetItem(result)) {
                amountProgress += result.getAmount();
            }
        }

        if (amountProgress > 0) {
            progress(p, q, amountProgress);
        }
    }
}
