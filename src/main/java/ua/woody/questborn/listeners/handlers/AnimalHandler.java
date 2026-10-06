package ua.woody.questborn.listeners.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.spigotmc.event.entity.EntityMountEvent;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.*;

public class AnimalHandler extends AbstractQuestHandler {
    private final Map<UUID, Location> lastRidePos = new HashMap<>();
    private final Map<UUID, com.tcoded.folialib.wrapper.task.WrappedTask> rideTasks = new HashMap<>();

    public AnimalHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onAnimalTame(EntityTameEvent e) {
        if (!(e.getOwner() instanceof Player p))
            return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() == QuestObjectiveType.TAME_ANIMAL && o.isTargetEntity(e.getEntity())) {
            progress(p, q, 1);
        }
    }

    public void onAnimalBreed(EntityBreedEvent e) {
        if (!(e.getBreeder() instanceof Player p))
            return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() == QuestObjectiveType.BREED_ANIMALS && o.isTargetEntity(e.getEntity())) {
            progress(p, q, 1);
        }
    }

    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        Entity entity = e.getRightClicked();

        ItemStack mainHand = p.getInventory().getItemInMainHand();
        if (mainHand.getType() == Material.BUCKET && entity.getType() == EntityType.COW) {
            if (e.getHand() == EquipmentSlot.HAND) {
                QuestDefinition q = getActiveQuest(p);
                if (q == null)
                    return;

                QuestObjective o = getCurrentObjective(p, q);
                if (o == null)
                    return;

                if (o.getType() == QuestObjectiveType.MILK_COW && o.isTargetEntity(entity)) {
                    progress(p, q, 1);
                }
            }
        }
    }

    public void onPlayerShearEntity(PlayerShearEntityEvent e) {
        Player p = e.getPlayer();
        Entity entity = e.getEntity();

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() == QuestObjectiveType.SHEAR_SHEEP && o.isTargetEntity(entity)) {
            progress(p, q, 1);
        }
    }

    public void onEntityMount(EntityMountEvent e) {
        if (!(e.getEntity() instanceof Player p))
            return;

        Entity mount = e.getMount();
        if (!isRideable(mount))
            return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.ENTITY_RIDE)
            return;

        if (!o.isTargetEntity(mount))
            return;

        if (o.getDistance() <= 0) {
            progress(p, q, 1);
            return;
        }

        UUID id = p.getUniqueId();
        stopRideTracking(id);
        lastRidePos.put(id, p.getLocation().clone());

        com.tcoded.folialib.wrapper.task.WrappedTask task = plugin.getFoliaLib().getImpl().runAtEntityTimer(p, () -> {
            QuestDefinition currentQ = getActiveQuest(p);
            if (currentQ == null) {
                stopRideTracking(id);
                return;
            }

            QuestObjective currentO = getCurrentObjective(p, currentQ);
            if (currentO == null || currentO.getType() != QuestObjectiveType.ENTITY_RIDE || currentO.getDistance() <= 0) {
                stopRideTracking(id);
                return;
            }

            Entity currentVehicle = p.getVehicle();
            if (currentVehicle == null || !isRideable(currentVehicle) || !currentO.isTargetEntity(currentVehicle)) {
                stopRideTracking(id);
                return;
            }

            Location cur = p.getLocation();
            Location last = lastRidePos.get(id);

            if (last == null || last.getWorld() != cur.getWorld()) {
                lastRidePos.put(id, cur.clone());
                return;
            }

            double add = last.distance(cur);
            if (add > 0.25) {
                progress(p, currentQ, add);
                lastRidePos.put(id, cur.clone());
            }
        }, 10L, 10L);
        rideTasks.put(id, task);
    }

    public void stopRideTracking(UUID id) {
        if (id == null)
            return;
        com.tcoded.folialib.wrapper.task.WrappedTask task = rideTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
        lastRidePos.remove(id);
    }

    public void onThrowEgg(PlayerEggThrowEvent e) {
        Player p = e.getPlayer();

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.THROW_EGG)
            return;

        progress(p, q, 1);
    }

    private boolean isRideable(Entity e) {
        if (e instanceof Horse || e instanceof Donkey || e instanceof Mule ||
                e instanceof SkeletonHorse || e instanceof ZombieHorse ||
                e instanceof Llama || e instanceof Pig || e instanceof Strider) {
            return true;
        }

        try {
            EntityType type = e.getType();
            if (type.name().equals("CAMEL"))
                return true;
        } catch (Exception ignored) {
        }

        return false;
    }
}
