package ua.woody.questborn.listeners.handlers;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MovementHandler extends AbstractQuestHandler {
    private final Map<UUID, Long> lastJumpTime = new HashMap<>();
    private final Map<UUID, Double> sprintBuffer = new HashMap<>();
    private final Map<UUID, Double> crouchBuffer = new HashMap<>();
    private final Map<UUID, Double> swimBuffer = new HashMap<>();
    private final Map<UUID, Double> elytraBuffer = new HashMap<>();
    private final Map<UUID, Double> boatBuffer = new HashMap<>();
    private final Map<UUID, Double> minecartBuffer = new HashMap<>();
    private final Map<UUID, Double> fallBuffer = new HashMap<>();
    private final Map<UUID, Float> peakFallDistance = new HashMap<>();
    private final Map<UUID, Long> lastFallDamageTime = new HashMap<>();

    private final Map<UUID, Location> lastSprintPosition = new HashMap<>();
    private final Map<UUID, Location> lastCrouchPosition = new HashMap<>();
    private final Map<UUID, Location> lastSwimPosition = new HashMap<>();
    private final Map<UUID, Location> lastElytraPosition = new HashMap<>();

    private final Set<UUID> reachedLocation = new HashSet<>();

    private final Map<UUID, String> lastBiome = new HashMap<>();

    private static final double REACH_RADIUS = 2.0;

    public MovementHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onFallDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p))
            return;
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL)
            return;

        UUID id = p.getUniqueId();

        if (p.isDead() || (p.getHealth() - e.getFinalDamage() <= 0)) {
            peakFallDistance.remove(id);
            return;
        }

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null || o.getType() != QuestObjectiveType.FALL_DISTANCE)
            return;

        float fallDistance = Math.max(p.getFallDistance(), peakFallDistance.getOrDefault(id, 0f));
        peakFallDistance.remove(id);
        lastFallDamageTime.put(id, System.currentTimeMillis());

        int blocks = (int) Math.floor(fallDistance);
        if (blocks < 1)
            return;

        handleFallProgress(p, q, o, blocks);
    }

    public void onFallMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null || o.getType() != QuestObjectiveType.FALL_DISTANCE)
            return;

        if (p.isFlying() || p.isGliding() || p.isInsideVehicle()) {
            peakFallDistance.remove(id);
            return;
        }

        float currentBukkitFall = p.getFallDistance();

        if (!p.isOnGround() && currentBukkitFall > 0f) {
            float prevPeak = peakFallDistance.getOrDefault(id, 0f);
            if (currentBukkitFall > prevPeak) {
                peakFallDistance.put(id, currentBukkitFall);
            }
            return;
        }

        if (peakFallDistance.containsKey(id)) {
            float peak = peakFallDistance.remove(id);

            long lastDamage = lastFallDamageTime.getOrDefault(id, 0L);
            if (System.currentTimeMillis() - lastDamage < 500) {
                return;
            }

            if (peak >= 3.0f && !p.isDead()) {
                int blocks = (int) Math.floor(peak);
                if (blocks > 0) {
                    handleFallProgress(p, q, o, blocks);
                }
            }
        }
    }

    private void handleFallProgress(Player p, QuestDefinition q, QuestObjective o, int blocks) {
        UUID id = p.getUniqueId();
        double buffer = fallBuffer.getOrDefault(id, 0.0) + blocks;
        fallBuffer.put(id, buffer);

        int minSave = plugin.getQuestManager().getMinDistanceSave();
        int fullBlocks = (int) Math.floor(buffer);

        int target = plugin.getQuestManager().getTargetAmount(o);
        int currentProgress = plugin.getQuestManager().getProgressValue(getData(p), q);

        if (fullBlocks >= minSave || (currentProgress + fullBlocks >= target)) {
            progress(p, q, fullBlocks);
            fallBuffer.put(id, buffer - fullBlocks);
        }
    }

    public void onBoatTravel(VehicleMoveEvent e) {
        if (!(e.getVehicle() instanceof Boat boat))
            return;
        if (boat.getPassengers().isEmpty())
            return;
        if (!(boat.getPassengers().get(0) instanceof Player p))
            return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.BOAT_TRAVEL)
            return;

        double delta = e.getFrom().distance(e.getTo());
        if (delta < 0.05)
            return;

        UUID id = p.getUniqueId();
        double buffer = boatBuffer.getOrDefault(id, 0D) + delta;
        boatBuffer.put(id, buffer);

        int minSave = plugin.getQuestManager().getMinDistanceSave();
        int fullBlocks = (int) Math.floor(buffer);

        if (fullBlocks >= minSave) {
            progress(p, q, fullBlocks);
            boatBuffer.put(id, buffer - fullBlocks);
        }
    }

    public void onMinecartTravel(VehicleMoveEvent e) {
        Entity vehicle = e.getVehicle();
        if (!(vehicle instanceof Minecart))
            return;

        if (vehicle.getPassengers().isEmpty())
            return;
        if (!(vehicle.getPassengers().get(0) instanceof Player p))
            return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.MINECART_TRAVEL)
            return;

        double delta = e.getFrom().distance(e.getTo());
        if (delta < 0.01)
            return;

        UUID id = p.getUniqueId();
        double buffer = minecartBuffer.getOrDefault(id, 0.0) + delta;
        minecartBuffer.put(id, buffer);

        int minSave = plugin.getQuestManager().getMinDistanceSave();
        int fullBlocks = (int) Math.floor(buffer);
        if (fullBlocks < minSave)
            return;

        progress(p, q, fullBlocks);
        minecartBuffer.put(id, buffer - fullBlocks);
    }

    public void onElytraToggle(EntityToggleGlideEvent e) {
        if (!(e.getEntity() instanceof Player p))
            return;

        UUID id = p.getUniqueId();

        if (e.isGliding()) {
            lastElytraPosition.put(id, p.getLocation());
        } else {
            Double buffer = elytraBuffer.get(id);
            if (buffer != null && buffer >= 1.0) {
                QuestDefinition q = getActiveQuest(p);
                if (q != null) {
                    QuestObjective o = getCurrentObjective(p, q);
                    if (o != null && o.getType() == QuestObjectiveType.ELYTRA_FLY) {
                        int blocks = (int) Math.floor(buffer);
                        if (blocks > 0) {
                            progress(p, q, blocks);
                        }
                    }
                }
            }
            lastElytraPosition.remove(id);
            elytraBuffer.remove(id);
        }
    }

    public void onElytraFlightDistance(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        if (!p.isGliding())
            return;
        if (!lastElytraPosition.containsKey(id)) {
            lastElytraPosition.put(id, e.getFrom());
            return;
        }

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.ELYTRA_FLY)
            return;

        Location lastPos = lastElytraPosition.get(id);
        Location currentPos = e.getTo();

        if (lastPos.getWorld() != currentPos.getWorld()) {
            lastElytraPosition.put(id, currentPos);
            return;
        }

        double distance = lastPos.distance(currentPos);
        if (distance < 0.1)
            return;

        lastElytraPosition.put(id, currentPos);

        double buffer = elytraBuffer.getOrDefault(id, 0.0) + distance;
        elytraBuffer.put(id, buffer);

        int minSave = plugin.getQuestManager().getMinDistanceSave();
        int blocks = (int) Math.floor(buffer);
        if (blocks >= minSave) {
            progress(p, q, blocks);
            elytraBuffer.put(id, buffer - blocks);
        }
    }

    public void onJump(PlayerMoveEvent e) {
        Player p = e.getPlayer();

        double fromY = e.getFrom().getY();
        double toY = e.getTo().getY();

        if (toY > fromY && p.getVelocity().getY() > 0.1) {
            long currentTime = System.currentTimeMillis();
            long lastJump = lastJumpTime.getOrDefault(p.getUniqueId(), 0L);

            if (currentTime - lastJump > 200) {
                lastJumpTime.put(p.getUniqueId(), currentTime);

                QuestDefinition q = getActiveQuest(p);
                if (q != null) {
                    QuestObjective o = getCurrentObjective(p, q);
                    if (o != null && o.getType() == QuestObjectiveType.JUMP) {
                        progress(p, q, 1);
                    }
                }
            }
        }
    }

    public void onCrouch(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();

        if (e.isSneaking()) {
            QuestDefinition q = getActiveQuest(p);
            if (q != null) {
                QuestObjective o = getCurrentObjective(p, q);
                if (o != null && o.getType() == QuestObjectiveType.CROUCH) {
                    progress(p, q, 1);
                }
            }
        }
    }

    public void onCrouchToggle(PlayerToggleSneakEvent e) {
        UUID id = e.getPlayer().getUniqueId();

        if (e.isSneaking()) {
            lastCrouchPosition.put(id, e.getPlayer().getLocation());
        } else {
            Double buffer = crouchBuffer.get(id);
            if (buffer != null && buffer >= 1.0) {
                Player p = e.getPlayer();
                QuestDefinition q = getActiveQuest(p);
                if (q != null) {
                    QuestObjective o = getCurrentObjective(p, q);
                    if (o != null && o.getType() == QuestObjectiveType.CROUCH_DISTANCE) {
                        int blocks = (int) Math.floor(buffer);
                        if (blocks > 0) {
                            progress(p, q, blocks);
                        }
                    }
                }
            }
            lastCrouchPosition.remove(id);
            crouchBuffer.remove(id);
        }
    }

    public void onCrouchMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        if (!p.isSneaking())
            return;
        if (!lastCrouchPosition.containsKey(id)) {
            lastCrouchPosition.put(id, e.getFrom());
            return;
        }

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null) return;

        if (o.getType() != QuestObjectiveType.CROUCH_DISTANCE)
            return;

        Location lastPos = lastCrouchPosition.get(id);
        Location currentPos = e.getTo();

        if (p.isFlying()) {
            lastCrouchPosition.put(id, currentPos);
            return;
        }

        if (lastPos.getWorld() != currentPos.getWorld()) {
            lastCrouchPosition.put(id, currentPos);
            return;
        }

        double sdx = currentPos.getX() - lastPos.getX();
        double sdz = currentPos.getZ() - lastPos.getZ();
        double distance = Math.sqrt(sdx * sdx + sdz * sdz);
        if (distance < 0.1) return;
        if (distance > 8.0) {
            lastCrouchPosition.put(id, currentPos);
            return;
        }

        lastCrouchPosition.put(id, currentPos);

        double buffer = crouchBuffer.getOrDefault(id, 0.0) + distance;
        crouchBuffer.put(id, buffer);

        int minSave = plugin.getQuestManager().getMinDistanceSave();
        int blocks = (int) Math.floor(buffer);
        if (blocks >= minSave) {
            progress(p, q, blocks);
            crouchBuffer.put(id, buffer - blocks);
        }
    }

    public void onSwimToggle(EntityToggleSwimEvent e) {
        UUID id = e.getEntity().getUniqueId();

        if (e.isSwimming()) {
            lastSwimPosition.put(id, e.getEntity().getLocation());
        } else {
            Double buffer = swimBuffer.get(id);
            if (buffer != null && buffer >= 1.0) {
                Player p = (Player) e.getEntity();
                QuestDefinition q = getActiveQuest(p);
                if (q != null) {
                    QuestObjective o = getCurrentObjective(p, q);
                    if (o != null && o.getType() == QuestObjectiveType.SWIM_DISTANCE) {
                        int blocks = (int) Math.floor(buffer);
                        if (blocks > 0) {
                            progress(p, q, blocks);
                        }
                    }
                }
            }
            lastSwimPosition.remove(id);
            swimBuffer.remove(id);
        }
    }

    public void stopSwimTracking(Player p) {
        UUID id = p.getUniqueId();
        if (swimBuffer.containsKey(id)) {
            Double buffer = swimBuffer.remove(id);
            if (buffer != null && buffer >= 1.0) {
                QuestDefinition q = getActiveQuest(p);
                if (q != null) {
                    QuestObjective o = getCurrentObjective(p, q);
                    if (o != null && o.getType() == QuestObjectiveType.SWIM_DISTANCE) {
                        int blocks = (int) Math.floor(buffer);
                        if (blocks > 0) {
                            progress(p, q, blocks);
                        }
                    }
                }
            }
            lastSwimPosition.remove(id);
        }
    }

    public void onSwimMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        if (!lastSwimPosition.containsKey(id)) {
            lastSwimPosition.put(id, e.getFrom());
            return;
        }

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null) return;

        if (o.getType() != QuestObjectiveType.SWIM_DISTANCE)
            return;

        Location lastPos = lastSwimPosition.get(id);
        Location currentPos = e.getTo();

        if (lastPos.getWorld() != currentPos.getWorld()) {
            lastSwimPosition.put(id, currentPos);
            return;
        }

        double sdx = currentPos.getX() - lastPos.getX();
        double sdz = currentPos.getZ() - lastPos.getZ();
        double sdy = currentPos.getY() - lastPos.getY();
        double distance = Math.sqrt(sdx * sdx + sdy * sdy + sdz * sdz);
        if (distance < 0.1) return;
        if (distance > 8.0) {
            lastSwimPosition.put(id, currentPos);
            return;
        }

        lastSwimPosition.put(id, currentPos);

        double buffer = swimBuffer.getOrDefault(id, 0.0) + distance;
        swimBuffer.put(id, buffer);

        int minSave = plugin.getQuestManager().getMinDistanceSave();
        int blocks = (int) Math.floor(buffer);
        if (blocks >= minSave) {
            progress(p, q, blocks);
            swimBuffer.put(id, buffer - blocks);
        }
    }

    public void onSprintToggle(PlayerToggleSprintEvent e) {
        UUID id = e.getPlayer().getUniqueId();

        if (e.isSprinting()) {
            lastSprintPosition.put(id, e.getPlayer().getLocation());
        } else {
            Double buffer = sprintBuffer.get(id);
            if (buffer != null && buffer >= 1.0) {
                Player p = e.getPlayer();
                QuestDefinition q = getActiveQuest(p);
                if (q != null) {
                    QuestObjective o = getCurrentObjective(p, q);
                    if (o != null && o.getType() == QuestObjectiveType.SPRINT_DISTANCE) {
                        int blocks = (int) Math.floor(buffer);
                        if (blocks > 0) {
                            progress(p, q, blocks);
                        }
                    }
                }
            }
            lastSprintPosition.remove(id);
            sprintBuffer.remove(id);
        }
    }

    public void onSprintMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        if (!p.isSprinting())
            return;
        if (!lastSprintPosition.containsKey(id)) {
            lastSprintPosition.put(id, e.getFrom());
            return;
        }

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.SPRINT_DISTANCE)
            return;

        Location lastPos = lastSprintPosition.get(id);
        Location currentPos = e.getTo();

        if (p.isFlying()) {
            lastSprintPosition.put(id, currentPos);
            return;
        }

        if (lastPos.getWorld() != currentPos.getWorld()) {
            lastSprintPosition.put(id, currentPos);
            return;
        }

        double sdx = currentPos.getX() - lastPos.getX();
        double sdz = currentPos.getZ() - lastPos.getZ();
        double distance = Math.sqrt(sdx * sdx + sdz * sdz);
        if (distance < 0.1)
            return;

        if (distance > 8.0) {
            lastSprintPosition.put(id, currentPos);
            return;
        }

        lastSprintPosition.put(id, currentPos);

        double buffer = sprintBuffer.getOrDefault(id, 0.0) + distance;
        sprintBuffer.put(id, buffer);

        int minSave = plugin.getQuestManager().getMinDistanceSave();
        int blocks = (int) Math.floor(buffer);
        if (blocks >= minSave) {
            progress(p, q, blocks);
            sprintBuffer.put(id, buffer - blocks);
        }
    }

    public void onReachLocation(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null || o.getType() != QuestObjectiveType.REACH_LOCATION) return;

        if (reachedLocation.contains(id)) {
            double dx = e.getTo().getX() - o.getX();
            double dy = e.getTo().getY() - o.getY();
            double dz = e.getTo().getZ() - o.getZ();
            boolean stillInside = Math.abs(dx) <= REACH_RADIUS && Math.abs(dy) <= REACH_RADIUS && Math.abs(dz) <= REACH_RADIUS;
            if (!stillInside) {
                reachedLocation.remove(id);
            }
            return;
        }

        String targetWorld = o.getWorld();
        if (targetWorld != null && !targetWorld.isEmpty()) {
            World w = p.getWorld();
            if (!w.getName().equalsIgnoreCase(targetWorld)) return;
        }

        double dx = e.getTo().getX() - o.getX();
        double dy = e.getTo().getY() - o.getY();
        double dz = e.getTo().getZ() - o.getZ();

        if (Math.abs(dx) <= REACH_RADIUS && Math.abs(dy) <= REACH_RADIUS && Math.abs(dz) <= REACH_RADIUS) {
            reachedLocation.add(id);
            progress(p, q, 1);
        }
    }

    public void onReachBiome(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        QuestDefinition q = getActiveQuest(p);
        if (q == null) {
            lastBiome.remove(id);
            return;
        }

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null || o.getType() != QuestObjectiveType.REACH_BIOME) {
            lastBiome.remove(id);
            return;
        }

        String targetBiome = o.getMessage();
        if (targetBiome == null || targetBiome.isEmpty()) return;

        org.bukkit.block.Biome currentBiome = e.getTo().getBlock().getBiome();
        String currentBiomeName = currentBiome.name().toLowerCase(java.util.Locale.ROOT);

        String prevBiome = lastBiome.get(id);

        if (targetBiome.equalsIgnoreCase(currentBiomeName)) {
            if (!targetBiome.equalsIgnoreCase(prevBiome)) {
                lastBiome.put(id, currentBiomeName);
                progress(p, q, 1);
            }
        } else {
            if (prevBiome != null && prevBiome.equalsIgnoreCase(targetBiome)) {
                lastBiome.remove(id);
            }
        }
    }

    public void clearCacheForPlayer(UUID uuid) {
        lastJumpTime.remove(uuid);
        sprintBuffer.remove(uuid);
        crouchBuffer.remove(uuid);
        swimBuffer.remove(uuid);
        elytraBuffer.remove(uuid);
        boatBuffer.remove(uuid);
        minecartBuffer.remove(uuid);
        lastSprintPosition.remove(uuid);
        lastCrouchPosition.remove(uuid);
        lastSwimPosition.remove(uuid);
        lastElytraPosition.remove(uuid);
        reachedLocation.remove(uuid);
        lastBiome.remove(uuid);
        peakFallDistance.remove(uuid);
        lastFallDamageTime.remove(uuid);
        fallBuffer.remove(uuid);
    }
}
