package ua.woody.questborn.integration.npc;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Collection;

public interface NpcProvider {
    boolean isNpc(Entity entity);

    String getId(Entity entity);

    String getName(Entity entity);

    String getName(String id);

    Location getStoredLocation(String id);

    Entity getEntity(String id);

    org.bukkit.entity.EntityType getType(String id);

    double getHeight(String id);

    Collection<String> getAllNpcIds();

    String getSkinTextureBase64(String id);

    default String getTargetNpcId(org.bukkit.entity.Player player, int range) {
        Entity target = player.getTargetEntity(range, false);
        if (target != null) {
            return getId(target);
        }
        return null;
    }

    String createNpc(String name, Location location);

    void registerListeners();

    void shutdown();
}
