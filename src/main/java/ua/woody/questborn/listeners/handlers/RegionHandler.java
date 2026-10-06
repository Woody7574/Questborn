package ua.woody.questborn.listeners.handlers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RegionHandler extends AbstractQuestHandler {
    private final Map<UUID, Set<String>> previousRegions = new HashMap<>();

    public RegionHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onMove(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;

        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        QuestDefinition q = getActiveQuest(p);
        if (q == null) {
            previousRegions.remove(id);
            return;
        }

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null) {
            previousRegions.remove(id);
            return;
        }

        QuestObjectiveType type = o.getType();
        if (type != QuestObjectiveType.ENTER_REGION && type != QuestObjectiveType.LEAVE_REGION) {
            previousRegions.remove(id);
            return;
        }

        String targetRegion = o.getRegion();
        if (targetRegion == null || targetRegion.isEmpty()) return;

        Set<String> current = getRegionsAt(to);
        Set<String> previous = previousRegions.getOrDefault(id, new HashSet<>());
        previousRegions.put(id, current);

        if (type == QuestObjectiveType.ENTER_REGION) {
            if (current.contains(targetRegion.toLowerCase()) && !previous.contains(targetRegion.toLowerCase())) {
                progress(p, q, 1);
            }
        } else {
            if (previous.contains(targetRegion.toLowerCase()) && !current.contains(targetRegion.toLowerCase())) {
                progress(p, q, 1);
            }
        }
    }

    private Set<String> getRegionsAt(Location loc) {
        Set<String> names = new HashSet<>();
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            if (container == null) return names;

            RegionManager manager = container.get(BukkitAdapter.adapt(loc.getWorld()));
            if (manager == null) return names;

            com.sk89q.worldedit.math.BlockVector3 pos = BukkitAdapter.asBlockVector(loc);

            for (ProtectedRegion region : manager.getApplicableRegions(pos)) {
                names.add(region.getId().toLowerCase());
            }
        } catch (Exception ignored) {
        }
        return names;
    }

    public void clearCacheForPlayer(UUID uuid) {
        previousRegions.remove(uuid);
    }
}
