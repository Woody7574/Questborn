package ua.woody.questborn.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ua.woody.questborn.QuestbornPlugin;

import java.util.UUID;

public class PlayerDataListener implements Listener {
    private final QuestbornPlugin plugin;

    public PlayerDataListener(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        plugin.getPlayerDataStore().loadPlayerAsync(uuid).thenRun(() -> {
            plugin.getFoliaLib().getImpl().runNextTick((task) -> {
                org.bukkit.entity.Player p = e.getPlayer();
                if (p != null && p.isOnline()) {
                    for (ua.woody.questborn.model.QuestTypeConfig typeConfig : plugin.getQuestManager().getQuestTypeManager().getAllTypes()) {
                        if (typeConfig.getEngine() == ua.woody.questborn.model.EngineType.ROTATION) {
                            plugin.getQuestManager().getOrAssignRotationQuests(p, typeConfig);
                        }
                    }
                }
            });
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();

        plugin.getPlayerDataStore().evictPlayer(uuid);
    }
}
