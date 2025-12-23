package ua.woody.questborn.listeners.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.*;

public class AnimalHandler extends AbstractQuestHandler {

    /* ==== DISTANCE TRACKING FOR ENTITY_RIDE ==== */
    private final Map<UUID, Location> lastRidePos = new HashMap<>();
    private final Map<UUID, Double> rideTracker = new HashMap<>();
    private final Set<UUID> riding = new HashSet<>();

    public AnimalHandler(QuestbornPlugin plugin) {
        super(plugin);
        startRideDistanceWatcher();
    }

    /* ==================== TAME_ANIMAL ==================== */
    public void onAnimalTame(EntityTameEvent e) {
        if (!(e.getOwner() instanceof Player p)) return;
        QuestDefinition q = getActiveQuest(p); if (q == null) return;

        var o = q.getObjective();
        if (o.getType() == QuestObjectiveType.TAME_ANIMAL && o.isTargetEntity(e.getEntityType())) {
            progress(p, q, 1);
        }
    }

    /* ==================== BREED_ANIMALS ==================== */
    public void onAnimalBreed(EntityBreedEvent e) {
        if (!(e.getBreeder() instanceof Player p)) return;
        QuestDefinition q = getActiveQuest(p); if (q == null) return;

        var o = q.getObjective();
        if (o.getType() == QuestObjectiveType.BREED_ANIMALS && o.isTargetEntity(e.getEntityType())) {
            progress(p, q, 1);
        }
    }

    /* ==================== MILK_COW ==================== */
    public void onMilkCow(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        Entity entity = e.getRightClicked();
        if (!(entity instanceof Cow || entity instanceof MushroomCow)) return;

        ItemStack item = p.getInventory().getItem(e.getHand());
        if (item == null || item.getType() != Material.BUCKET) return;

        QuestDefinition q = getActiveQuest(p); if (q == null) return;

        var o = q.getObjective();
        if (o.getType() == QuestObjectiveType.MILK_COW && o.isTargetEntity(entity.getType())) {
            progress(p, q, 1);
        }
    }

    /* ==================== SHEAR_SHEEP ==================== */
    public void onShearSheep(PlayerShearEntityEvent e) {
        Player p = e.getPlayer();
        if (!(e.getEntity() instanceof Sheep)) return;

        QuestDefinition q = getActiveQuest(p); if (q == null) return;

        var o = q.getObjective();
        if (o.getType() == QuestObjectiveType.SHEAR_SHEEP && o.isTargetEntity(EntityType.SHEEP)) {
            progress(p, q, 1);
        }
    }

    /* ==================== ENTITY_RIDE (Vehicle - boat/minecart тільки якщо ти додаси їх у isRideable) ==================== */
    public void onEntityRide(VehicleEnterEvent e) {
        if (!(e.getEntered() instanceof Player p)) return;
        handleRideStart(p, e.getVehicle());
    }

    public void onVehicleExit(VehicleExitEvent e) {
        if (!(e.getExited() instanceof Player p)) return;
        stopRideTracking(p.getUniqueId());
    }

    /* ==================== ENTITY_RIDE (Mount - коні/свині/страйдер/верблюд і т.д.) ==================== */
    public void onEntityMount(EntityMountEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        handleRideStart(p, e.getMount());
    }

    public void onEntityDismount(EntityDismountEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        stopRideTracking(p.getUniqueId());
    }

    private void handleRideStart(Player p, Entity mount) {
        if (mount == null || !isRideable(mount)) return;

        QuestDefinition q = getActiveQuest(p); if (q == null) return;
        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.ENTITY_RIDE) return;

        // distance відсутній → рахуємо просто сідання
        if (o.getDistance() <= 0) {
            progress(p, q, 1);
            return;
        }

        UUID id = p.getUniqueId();
        riding.add(id);
        rideTracker.put(id, 0.0);
        lastRidePos.put(id, p.getLocation().clone());
    }

    private void stopRideTracking(UUID id) {
        riding.remove(id);
        rideTracker.remove(id);
        lastRidePos.remove(id);
    }

    /* ==================== THROW_EGG ==================== */
    public void onThrowEgg(PlayerEggThrowEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p); if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.THROW_EGG) return;

        // твоя логіка: завжди +1
        progress(p, q, 1);
    }

    /* ===========================================================
       🔥 Watcher — кожні 10 тік (0.5s) підрахунок пройденої дистанції
       =========================================================== */
    private void startRideDistanceWatcher() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            for (UUID id : new HashSet<>(riding)) {

                Player p = Bukkit.getPlayer(id);
                if (p == null || !p.isOnline()) {
                    stopRideTracking(id);
                    continue;
                }

                QuestDefinition q = getActiveQuest(p);
                if (q == null) {
                    stopRideTracking(id);
                    continue;
                }

                var o = q.getObjective();
                if (o.getType() != QuestObjectiveType.ENTITY_RIDE) {
                    stopRideTracking(id);
                    continue;
                }

                double need = o.getDistance();
                if (need <= 0) {
                    stopRideTracking(id);
                    continue;
                }

                // ✅ якщо вже не сидить на транспорті — стоп
                Entity currentVehicle = p.getVehicle();
                if (currentVehicle == null || !isRideable(currentVehicle)) {
                    stopRideTracking(id);
                    continue;
                }

                Location cur = p.getLocation();
                Location last = lastRidePos.get(id);

                if (last == null || last.getWorld() != cur.getWorld()) {
                    lastRidePos.put(id, cur.clone());
                    continue;
                }

                double add = last.distance(cur);
                if (add > 0.25) {
                    double total = rideTracker.getOrDefault(id, 0.0) + add;
                    rideTracker.put(id, total);

                    // прогрес у блоках
                    progress(p, q, add);

                    if (total >= need) {
                        complete(p, q);
                        stopRideTracking(id);
                        continue;
                    }
                }

                lastRidePos.put(id, cur.clone());
            }

        }, 10L, 10L);
    }

    private boolean isRideable(Entity e) {
        return e instanceof Horse || e instanceof Donkey || e instanceof Mule ||
                e instanceof SkeletonHorse || e instanceof ZombieHorse ||
                e instanceof Llama || e instanceof Camel ||
                e instanceof Pig || e instanceof Strider;
    }
}
