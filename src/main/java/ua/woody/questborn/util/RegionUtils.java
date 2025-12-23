package ua.woody.questborn.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;

public class RegionUtils {

    public static Set<String> getRegionsAt(Location loc) {
        World world = loc.getWorld();
        if (world == null) return Set.of();

        RegionManager rm = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(world));

        if (rm == null) return Set.of();

        ApplicableRegionSet set = rm.getApplicableRegions(
                BukkitAdapter.asBlockVector(loc)
        );

        Set<String> names = new HashSet<>();
        for (ProtectedRegion r : set) {
            names.add(r.getId().toLowerCase());
        }
        return names;
    }
}
