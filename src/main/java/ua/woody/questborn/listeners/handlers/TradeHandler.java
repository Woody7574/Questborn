package ua.woody.questborn.listeners.handlers;

import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;
import io.papermc.paper.event.player.PlayerTradeEvent;

public class TradeHandler extends AbstractQuestHandler {

    public TradeHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onVillagerTrade(PlayerTradeEvent e) {
        Player p = e.getPlayer();
        AbstractVillager trader = e.getVillager();

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.TRADE_WITH_VILLAGER) return;

        // Перевіряємо тип торговця
        if (!o.isTargetEntity(trader.getType())) return;

        // Перевіряємо предмет торгівлі - тепер враховуємо і item, і targetItems
        boolean hasItemRequirement = (o.getItem() != null && !o.getItem().isEmpty()) ||
                (o.getTargetItems() != null && !o.getTargetItems().isEmpty());

        if (hasItemRequirement) {
            MerchantRecipe recipe = e.getTrade();
            boolean foundMatchingItem = false;

            for (ItemStack ingredient : recipe.getIngredients()) {
                if (ingredient != null && o.isTargetItem(ingredient.getType())) {
                    foundMatchingItem = true;
                    break;
                }
            }

            // Якщо знайшли відповідний предмет - зараховуємо
            if (foundMatchingItem) {
                progress(p, q, 1);
            }
        } else {
            // Якщо предмет не вказано взагалі - будь-яка успішна торгівля підходить
            progress(p, q, 1);
        }
    }
}