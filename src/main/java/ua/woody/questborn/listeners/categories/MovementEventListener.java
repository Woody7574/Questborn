package ua.woody.questborn.listeners.categories;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

import ua.woody.questborn.listeners.QuestProgressListener;
import ua.woody.questborn.model.QuestObjectiveType;

public class MovementEventListener implements Listener {
    private final QuestProgressListener listener;

    public MovementEventListener(QuestProgressListener listener) {
        this.listener = listener;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e.getTo() == null) return;

        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        if (e instanceof PlayerTeleportEvent) {
            listener.getTravelHandler().clearCacheForPlayer(uuid);
            listener.getMovementHandler().clearCacheForPlayer(uuid);
            return;
        }

        if (e.getFrom().getX() == e.getTo().getX()
            && e.getFrom().getY() == e.getTo().getY()
            && e.getFrom().getZ() == e.getTo().getZ()) {
            return;
        }

        long now = System.currentTimeMillis();
        Long lastCheck = listener.getLastMoveTime().get(uuid);
        if (lastCheck != null && now - lastCheck < listener.getMoveCooldownMs()) {
            return;
        }
        listener.getLastMoveTime().put(uuid, now);

        listener.executeForActiveObjectives(player, ctx -> {
            QuestObjectiveType type = ctx.objective().getType();
            switch (type) {
                case TRAVEL_DISTANCE:
                case ELYTRA_FLY:
                    listener.getTravelHandler().onTravel(e);
                    break;
                case JUMP:
                    listener.getMovementHandler().onJump(e);
                    break;
                default:
                    break;
            }

            if (type == QuestObjectiveType.SPRINT_DISTANCE && player.isSprinting()) {
                listener.getMovementHandler().onSprintMove(e);
            }
            if (type == QuestObjectiveType.CROUCH_DISTANCE && player.isSneaking()) {
                listener.getMovementHandler().onCrouchMove(e);
            }
            if (type == QuestObjectiveType.SWIM_DISTANCE) {
                if (player.isInWater() && !player.isFlying()) {
                    listener.getMovementHandler().onSwimMove(e);
                } else {
                    listener.getMovementHandler().stopSwimTracking(player);
                }
            }
            if (type == QuestObjectiveType.ELYTRA_FLY && player.isGliding()) {
                listener.getMovementHandler().onElytraFlightDistance(e);
            }
            if (type == QuestObjectiveType.REACH_LOCATION) {
                listener.getMovementHandler().onReachLocation(e);
            }
            if (type == QuestObjectiveType.REACH_BIOME) {
                listener.getMovementHandler().onReachBiome(e);
            }
            if (type == QuestObjectiveType.FALL_DISTANCE) {
                listener.getMovementHandler().onFallMove(e);
            }
            if (type == QuestObjectiveType.ENTER_REGION || type == QuestObjectiveType.LEAVE_REGION) {
                listener.getRegionHandler().onMove(e);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent e) {
        listener.getMovementHandler().onBoatTravel(e);
        listener.getMovementHandler().onMinecartTravel(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onToggleSprint(PlayerToggleSprintEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            listener.getMovementHandler().onSprintToggle(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onToggleSneak(PlayerToggleSneakEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            listener.getMovementHandler().onCrouchToggle(e);
            listener.getMovementHandler().onCrouch(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onToggleGlide(EntityToggleGlideEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;

        listener.executeForActiveObjectives(player, ctx -> {
            listener.getMovementHandler().onElytraToggle(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnterBed(PlayerBedEnterEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            listener.getWorldHandler().onEnterBed(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeaveBed(PlayerBedLeaveEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            listener.getWorldHandler().onLeaveBed(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangeDimension(PlayerChangedWorldEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            listener.getWorldHandler().onChangeDimension(e);
        });
    }
}
