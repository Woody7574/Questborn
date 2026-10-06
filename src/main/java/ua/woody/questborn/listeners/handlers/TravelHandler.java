package ua.woody.questborn.listeners.handlers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class TravelHandler extends AbstractQuestHandler {
    private final Map<String, Location> lastLoc = new HashMap<>();
    private final Map<String, Double> travelBuffer = new HashMap<>();
    private final Map<String, Double> boatBuffer = new HashMap<>();
    private final Map<String, Double> minecartBuffer = new HashMap<>();

    private static final double MIN_DISTANCE = 0.05;

    public TravelHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    private String getKey(UUID playerId, String questId) {
        return playerId.toString() + ":" + questId;
    }

    public void onTravel(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.TRAVEL_DISTANCE)
            return;

        Location to = e.getTo();
        if (to == null)
            return;

        String key = getKey(id, q.getId());

        if (p.isFlying()) {
            lastLoc.put(key, to);
            return;
        }

        Location prev = lastLoc.get(key);
        if (prev == null || prev.getWorld() != to.getWorld()) {
            lastLoc.put(key, to);
            return;
        }

        double dx = to.getX() - prev.getX();
        double dz = to.getZ() - prev.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        lastLoc.put(key, to);

        if (dist < MIN_DISTANCE)
            return;

        if (dist > 8.0)
            return;

        double buffer = travelBuffer.getOrDefault(key, 0.0) + dist;
        travelBuffer.put(key, buffer);

        int minSave = plugin.getQuestManager().getMinDistanceSave();
        int blocks = (int) Math.floor(buffer);

        if (blocks >= minSave) {
            progress(p, q, blocks);
            travelBuffer.put(key, buffer - blocks);
        }
    }

    public void onBoatTravel(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.BOAT_TRAVEL)
            return;
        if (!p.isInsideVehicle())
            return;

        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null || from.getWorld() != to.getWorld())
            return;

        double dist = from.distance(to);
        if (dist < MIN_DISTANCE)
            return;

        String key = getKey(id, q.getId());
        double buffer = boatBuffer.getOrDefault(key, 0.0) + dist;
        int blocks = (int) Math.floor(buffer);

        if (blocks > 0) {
            progress(p, q, blocks);
            buffer -= blocks;
        }
        boatBuffer.put(key, buffer);
    }

    public void onMinecartTravel(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.MINECART_TRAVEL)
            return;
        if (!p.isInsideVehicle())
            return;

        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null || from.getWorld() != to.getWorld())
            return;

        double dist = from.distance(to);
        if (dist < MIN_DISTANCE)
            return;

        String key = getKey(id, q.getId());
        double buffer = minecartBuffer.getOrDefault(key, 0.0) + dist;
        int blocks = (int) Math.floor(buffer);

        if (blocks > 0) {
            progress(p, q, blocks);
            buffer -= blocks;
        }
        minecartBuffer.put(key, buffer);
    }

    public void clearCacheForPlayer(UUID uuid) {
        String prefix = uuid.toString() + ":";
        lastLoc.keySet().removeIf(k -> k.startsWith(prefix));
        travelBuffer.keySet().removeIf(k -> k.startsWith(prefix));
        boatBuffer.keySet().removeIf(k -> k.startsWith(prefix));
        minecartBuffer.keySet().removeIf(k -> k.startsWith(prefix));
    }
}
