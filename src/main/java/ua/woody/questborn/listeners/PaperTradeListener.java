package ua.woody.questborn.listeners;

import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.listeners.handlers.TradeHandler;

public class PaperTradeListener implements Listener {
    private final QuestbornPlugin plugin;
    private final TradeHandler tradeHandler;

    public PaperTradeListener(QuestbornPlugin plugin, TradeHandler tradeHandler) {
        this.plugin = plugin;
        this.tradeHandler = tradeHandler;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVillagerTrade(PlayerTradeEvent e) {
        tradeHandler.onVillagerTrade(e);
    }
}
