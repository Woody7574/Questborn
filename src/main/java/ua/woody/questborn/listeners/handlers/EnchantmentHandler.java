package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;

public class EnchantmentHandler extends AbstractQuestHandler {

    public EnchantmentHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onEnchantTableUse(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() != Material.ENCHANTING_TABLE) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.ENCHANT_TABLE_USE) return;

        // Перевіряємо, чи гравець тримає предмет для зачарування
        ItemStack itemInHand = p.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) return;

        // Якщо вказано конкретні предмети - перевіряємо
        if (!o.getTargetItems().isEmpty() || (o.getItem() != null && !o.getItem().isEmpty())) {
            if (o.isTargetItem(itemInHand.getType())) {
                progress(p, q, 1);
            }
        } else {
            // Якщо не вказано - будь-яке використання столика підходить
            progress(p, q, 1);
        }
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent e) {
        Player p = e.getEnchanter();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.ENCHANT_TABLE_USE) return;

        // Перевіряємо предмет, який зачаровується
        ItemStack item = e.getItem();
        if (!o.getTargetItems().isEmpty() || (o.getItem() != null && !o.getItem().isEmpty())) {
            if (o.isTargetItem(item.getType())) {
                progress(p, q, 1);
            }
        } else {
            // Якщо не вказано - будь-яке зачарування підходить
            progress(p, q, 1);
        }
    }
}