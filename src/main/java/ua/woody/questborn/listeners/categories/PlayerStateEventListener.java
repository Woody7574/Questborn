package ua.woody.questborn.listeners.categories;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import ua.woody.questborn.listeners.QuestProgressListener;
import ua.woody.questborn.managers.QuestManager;
import ua.woody.questborn.model.QuestObjectiveType;

public class PlayerStateEventListener implements Listener {
    private final QuestProgressListener listener;

    public PlayerStateEventListener(QuestProgressListener listener) {
        this.listener = listener;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        listener.getWorldHandler().onJoinServer(e);
        listener.updatePlayerCache(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        listener.getEntityHandler().onPlayerQuit(e);
        listener.getWorldHandler().onQuitServer(e);
        listener.getItemInteractionHandler().onQuit(e);
        listener.getInteractionHandler().onQuit(player);
        listener.getLastMoveTime().remove(player.getUniqueId());
        listener.getMovementHandler().clearCacheForPlayer(player.getUniqueId());
        listener.getRegionHandler().clearCacheForPlayer(player.getUniqueId());
        listener.clearPlayerCache(player.getUniqueId());
        listener.getPlugin().getQuestManager().getActiveQuestPlayers().remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent e) {
        Player player = e.getPlayer();
        listener.getPlugin().getFoliaLib().getImpl().runAtEntityLater(player, () -> {
            if (player.isOnline()) {
                QuestManager qm = listener.getPlugin().getQuestManager();
                listener.updatePlayerCache(player);
                qm.getActionBarManager().sendForPlayer(player);
                qm.getBossBarManager().updateBar(player);
                qm.getScoreboardManager().updateBoard(player);
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent e) {
        if (!(e.getEntity().getShooter() instanceof Player player)) return;

        listener.executeForActiveObjectives(player, ctx -> {
            if (ctx.objective().getType() == QuestObjectiveType.POTION_SPLASH) {
                listener.getMagicHandler().onPotionSplash(e);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConduitPowerGain(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;

        listener.executeForActiveObjectives(player, ctx -> {
            if (ctx.objective().getType() == QuestObjectiveType.CONDUIT_ACTIVATE) {
                listener.getMagicHandler().onConduitPowerGain(e);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevelUp(PlayerLevelChangeEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            QuestObjectiveType type = ctx.objective().getType();
            if (type == QuestObjectiveType.LEVEL_UP_REACH || type == QuestObjectiveType.LEVEL_UP_GAIN) {
                listener.getLevelAndExperienceHandler().onLevelUp(e);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExperiencePickup(PlayerExpChangeEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            if (ctx.objective().getType() == QuestObjectiveType.EXPERIENCE_ORB_PICKUP) {
                listener.getLevelAndExperienceHandler().onExperiencePickup(e);
            }
        });
    }
}
