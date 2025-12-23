package ua.woody.questborn.listeners.handlers;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;

public class FishingHandler extends AbstractQuestHandler {

    public FishingHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    // ==================== ITEM_FISH (перенесений з ItemHandler) ====================
    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.ITEM_FISH) return;

        if (o.getTargetItems().isEmpty() && (o.getItem() == null || o.getItem().isEmpty())) {
            progress(p, q, 1);
            return;
        }

        if (e.getCaught() instanceof Item caughtItem) {
            ItemStack stack = caughtItem.getItemStack();
            if (o.isTargetItem(stack.getType())) {
                progress(p, q, stack.getAmount());
            }
        }
    }

    // ==================== FISHING_BOBBER_HOOK (НОВИЙ) ====================
    @EventHandler
    public void onFishingHook(PlayerFishEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.FISHING_BOBBER_HOOK) return;

        // Ловимо будь-яке захоплення поплавця (риба, предмети, моби)
        switch (e.getState()) {
            case CAUGHT_FISH:      // звичайна риба
            case CAUGHT_ENTITY:    // інші сутності (наприклад, предмети)
                progress(p, q, 1);
                break;

            // Можна також враховувати інші стани за потребою:
            // case BITE:           // клювання
            // case IN_GROUND:      // зачепився за блок
            // case FAILED_ATTEMPT: // невдала спроба
            default:
                break;
        }
    }
}