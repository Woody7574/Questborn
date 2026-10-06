package ua.woody.questborn.listeners.categories;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.spigotmc.event.entity.EntityMountEvent;

import ua.woody.questborn.listeners.QuestProgressListener;

public class EntityEventListener implements Listener {
    private final QuestProgressListener listener;

    public EntityEventListener(QuestProgressListener listener) {
        this.listener = listener;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        listener.executeForActiveObjectives(killer, ctx -> {
            listener.getEntityHandler().onKill(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(CreatureSpawnEvent e) {
        listener.getEntityHandler().onEntitySpawn(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e) {
        listener.executeForActiveObjectives(e.getEntity(), ctx -> {
            listener.getEntityHandler().onPlayerDeath(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDealDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player player)) return;

        listener.executeForActiveObjectives(player, ctx -> {
            listener.getEntityHandler().onDealDamage(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnyDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;

        listener.executeForActiveObjectives(player, ctx -> {
            listener.getEntityHandler().onTakeDamage(e);
            listener.getMovementHandler().onFallDamage(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            listener.getInteractionHandler().onInteractEntity(e);
            listener.getAnimalHandler().onPlayerInteractEntity(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnimalTame(EntityTameEvent e) {
        if (!(e.getOwner() instanceof Player player)) return;

        listener.executeForActiveObjectives(player, ctx -> {
            listener.getAnimalHandler().onAnimalTame(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnimalBreed(EntityBreedEvent e) {
        if (!(e.getBreeder() instanceof Player player)) return;

        listener.executeForActiveObjectives(player, ctx -> {
            listener.getAnimalHandler().onAnimalBreed(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShearSheep(PlayerShearEntityEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            listener.getAnimalHandler().onPlayerShearEntity(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onThrowEgg(PlayerEggThrowEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            listener.getAnimalHandler().onThrowEgg(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent e) {
        if (!(e.getEntered() instanceof Player player)) return;

        listener.executeForActiveObjectives(player, ctx -> {
            listener.getAnimalHandler().onEntityMount(new EntityMountEvent(e.getEntered(), e.getVehicle()));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleExit(VehicleExitEvent e) {
        if (!(e.getExited() instanceof Player player)) return;

        listener.executeForActiveObjectives(player, ctx -> {
            listener.getAnimalHandler().stopRideTracking(player.getUniqueId());
        });
    }
}
