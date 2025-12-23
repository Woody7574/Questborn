package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;

public class ItemHandler extends AbstractQuestHandler {

    public ItemHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.ITEM_CRAFT) return;

        ItemStack result = e.getRecipe().getResult();
        if (o.isTargetItem(result.getType())) {
            progress(p, q, result.getAmount());
        }
    }

    public void onFurnaceExtract(FurnaceExtractEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() == QuestObjectiveType.ITEM_SMELT ||
                o.getType() == QuestObjectiveType.ITEM_COOK) {

            if (o.isTargetItem(e.getItemType())) {
                progress(p, q, e.getItemAmount());
            }
        }
    }

    public void onConsume(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.CONSUME_ITEM) return;

        ItemStack item = e.getItem();
        if (o.isTargetItem(item.getType())) {
            progress(p, q, 1);
        }
    }

    public void onEnchant(EnchantItemEvent e) {
        Player p = e.getEnchanter();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.ITEM_ENCHANT) return;

        ItemStack item = e.getItem();
        if (o.isTargetItem(item.getType())) {
            progress(p, q, 1);
        }
    }
}