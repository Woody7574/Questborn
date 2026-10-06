package ua.woody.questborn.model.npc.mood;

import org.bukkit.entity.Player;
import ua.woody.questborn.model.NpcConfig;

public class TimeCondition implements MoodCondition {
    private final long minTime;
    private final long maxTime;

    public TimeCondition(String range) {
        String[] parts = range.split("-");
        if (parts.length == 2) {
            this.minTime = Long.parseLong(parts[0].trim());
            this.maxTime = Long.parseLong(parts[1].trim());
        } else {
            this.minTime = 0;
            this.maxTime = 24000;
        }
    }

    public String getRange() {
        return minTime + "-" + maxTime;
    }

    @Override
    public boolean check(Player player, NpcConfig npc) {
        long time = player.getWorld().getTime();
        if (minTime <= maxTime) {
            return time >= minTime && time <= maxTime;
        } else {
            return time >= minTime || time <= maxTime;
        }
    }
}
