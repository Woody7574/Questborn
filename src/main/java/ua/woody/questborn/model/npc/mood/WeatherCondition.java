package ua.woody.questborn.model.npc.mood;

import org.bukkit.entity.Player;
import ua.woody.questborn.model.NpcConfig;

public class WeatherCondition implements MoodCondition {
    private final String requiredState;

    public WeatherCondition(String requiredState) {
        this.requiredState = requiredState.toUpperCase();
    }

    public String getRequiredState() {
        return requiredState;
    }

    @Override
    public boolean check(Player player, NpcConfig npc) {
        boolean hasStorm = player.getWorld().hasStorm();
        if ("STORM".equals(requiredState) || "RAIN".equals(requiredState)) {
            return hasStorm;
        } else if ("CLEAR".equals(requiredState) || "SUN".equals(requiredState)) {
            return !hasStorm;
        }
        return false;
    }
}
