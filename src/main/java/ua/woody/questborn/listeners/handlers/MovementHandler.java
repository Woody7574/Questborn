package ua.woody.questborn.listeners.handlers;

import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MovementHandler extends AbstractQuestHandler {

    private final Map<UUID, Double> fallDistanceMap = new HashMap<>();
    private final Map<UUID, Double> sprintDistanceMap = new HashMap<>();
    private final Map<UUID, Long> lastJumpTime = new HashMap<>();
    private final Map<UUID, Integer> jumpCountMap = new HashMap<>();
    private final Map<UUID, Integer> crouchCountMap = new HashMap<>();
    private final Map<UUID, Double> minecartTravelBuffer = new HashMap<>();
    private final Map<UUID, Double> sprintProgress = new HashMap<>();
    private final Map<UUID, Location> lastPosition = new HashMap<>();
    private final Map<UUID, Double> elytraDistance = new HashMap<>();


    public MovementHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    // ==================== FALL_DISTANCE ====================
    @EventHandler
    public void onFallDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        Player p = (Player) e.getEntity();
        double fallDistance = p.getFallDistance();

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() == QuestObjectiveType.FALL_DISTANCE) {
            // Перевіряємо, чи падіння було достатньо високим (якщо вказано в amount)
            if (o.getAmount() > 0) {
                if (fallDistance >= o.getAmount()) {
                    progress(p, q, 1);
                }
            } else {
                // Якщо не вказано - будь-яке падіння з шкодою
                progress(p, q, 1);
            }
        }
    }

    // ==================== BOAT_TRAVEL ====================
    private final Map<UUID, Double> boatProgress = new HashMap<>();

    @EventHandler
    public void onBoatTravel(VehicleMoveEvent e) {
        if (!(e.getVehicle() instanceof Boat boat)) return;
        if (boat.getPassengers().isEmpty()) return;
        if (!(boat.getPassengers().get(0) instanceof Player p)) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.BOAT_TRAVEL) return;

        double delta = e.getFrom().distance(e.getTo());
        if (delta < 0.05) return; // рух мінімальний → ігноруємо шум

        UUID id = p.getUniqueId();
        boatProgress.put(id, boatProgress.getOrDefault(id, 0D) + delta);

        int fullBlocks = (int) Math.floor(boatProgress.get(id));
        if (fullBlocks >= 1) {
            progress(p, q, fullBlocks);
            boatProgress.put(id, boatProgress.get(id) - fullBlocks);
        }
    }

    // ==================== MINECART_TRAVEL ====================
    @EventHandler
    public void onMinecartTravel(VehicleMoveEvent e) {
        Entity vehicle = e.getVehicle();
        if (!(vehicle instanceof Minecart)) return;

        if (vehicle.getPassengers().isEmpty()) return;
        if (!(vehicle.getPassengers().get(0) instanceof Player p)) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.MINECART_TRAVEL) return;

        // Відстань між попередньою та новою позицією вагонетки
        double delta = e.getFrom().distance(e.getTo());

        // Ігноруємо мікрорух/лаги
        if (delta < 0.01) return;

        UUID id = p.getUniqueId();

        // Накопичуємо дистанцію у буфері
        double total = minecartTravelBuffer.getOrDefault(id, 0.0) + delta;
        minecartTravelBuffer.put(id, total);

        // Скільки повних блоків набігло
        int fullBlocks = (int) Math.floor(total);
        if (fullBlocks <= 0) return;

        // Додаємо прогрес
        progress(p, q, fullBlocks);

        // Залишаємо тільки “хвіст” після цілих блоків
        minecartTravelBuffer.put(id, total - fullBlocks);
    }


    // ==================== ELYTRA_FLY ====================
    @EventHandler
    public void onElytraToggle(EntityToggleGlideEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        UUID id = p.getUniqueId();

        // Початок польоту
        if (e.isGliding()) {
            elytraDistance.put(id, 0.0);
        }
        // Кінець польоту — залишаємо або очищаємо (за бажанням)
        else {
            elytraDistance.remove(id);
        }
    }

    @EventHandler
    public void onElytraFlightDistance(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        // Якщо він не летить — виходимо
        if (!p.isGliding()) return;
        if (!elytraDistance.containsKey(id)) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.ELYTRA_FLY) return;

        Location from = e.getFrom();
        Location to = e.getTo();
        if (from.distanceSquared(to) == 0) return;

        // Відстань між координатами у блоках
        double dist = from.distance(to);
        elytraDistance.put(id, elytraDistance.get(id) + dist);

        // Коли накопичилось ≥ 1 блок → зараховуємо
        int blocks = elytraDistance.get(id).intValue();
        if (blocks > 0) {
            progress(p, q, blocks);
            elytraDistance.put(id, elytraDistance.get(id) - blocks); // залишок < 1 зберігаємо
        }
    }



    // ==================== JUMP ====================
    @EventHandler
    public void onJump(PlayerMoveEvent e) {
        Player p = e.getPlayer();

        // Перевіряємо, чи гравець стрибнув (зміна Y координати вгору)
        double fromY = e.getFrom().getY();
        double toY = e.getTo().getY();

        if (toY > fromY && p.getVelocity().getY() > 0.1) {
            long currentTime = System.currentTimeMillis();
            long lastJump = lastJumpTime.getOrDefault(p.getUniqueId(), 0L);

            // Запобігаємо багаторазовому зарахованю одного стрибка
            if (currentTime - lastJump > 200) { // Мінімум 200 мс між стрибками
                lastJumpTime.put(p.getUniqueId(), currentTime);

                QuestDefinition q = getActiveQuest(p);
                if (q != null) {
                    var o = q.getObjective();
                    if (o.getType() == QuestObjectiveType.JUMP) {
                        progress(p, q, 1);
                    }
                }
            }
        }
    }

    // ==================== CROUCH ====================
    @EventHandler
    public void onCrouch(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();

        // Враховуємо тільки коли гравець починає присідати
        if (e.isSneaking()) {
            QuestDefinition q = getActiveQuest(p);
            if (q != null) {
                var o = q.getObjective();
                if (o.getType() == QuestObjectiveType.CROUCH) {
                    progress(p, q, 1);
                }
            }
        }
    }

    // ==================== SPRINT_DISTANCE ====================
    // Коли гравець починає/закінчує біг
    @EventHandler
    public void onSprintToggle(PlayerToggleSprintEvent e) {
        UUID id = e.getPlayer().getUniqueId();

        if (e.isSprinting()) {
            // Почали біг → запам’ятовуємо стартову позицію
            lastPosition.put(id, e.getPlayer().getLocation());
            sprintProgress.put(id, 0.0);
        } else {
            // Перестав бігти → видаляємо щоб не продовжувався після зупинки
            lastPosition.remove(id);
            sprintProgress.remove(id);
        }
    }

    // Облік руху під час спринту
    @EventHandler
    public void onSprintMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        // Гравець має реально спринтити і ми повинні відслідковувати його
        if (!p.isSprinting()) return;
        if (!sprintProgress.containsKey(id)) return;

        Location from = e.getFrom();
        Location to = e.getTo();

        if (from.getWorld() != to.getWorld()) return;
        if (from.distanceSquared(to) == 0) return; // немає руху

        double dist = from.distance(to);

        // Додаємо до буфера
        sprintProgress.put(id, sprintProgress.get(id) + dist);

        // Якщо накопичено мінімум 1 блок → списуємо та даємо прогрес
        if (sprintProgress.get(id) >= 1) {
            QuestDefinition q = getActiveQuest(p);
            if (q != null && q.getObjective().getType() == QuestObjectiveType.SPRINT_DISTANCE) {

                int blocks = sprintProgress.get(id).intValue(); // або (int)(double)
                progress(p, q, blocks);
                sprintProgress.put(id, sprintProgress.get(id) - blocks); // залишок зберігаємо

            }
        }
    }
}