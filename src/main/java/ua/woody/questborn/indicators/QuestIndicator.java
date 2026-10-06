package ua.woody.questborn.indicators;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public interface QuestIndicator {
    void updateLocation(Location loc);
    void setBobbing(boolean bobbing);
    void updateName(String name);
    void remove();
    boolean isValid();
    Entity getBukkitEntity();
}
