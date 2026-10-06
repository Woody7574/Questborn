package ua.woody.questborn.model.npc.mood;

import org.bukkit.entity.Player;
import ua.woody.questborn.model.NpcConfig;

public class ChanceCondition implements MoodCondition {
    private final double percentage;

    public ChanceCondition(double percentage) {
        this.percentage = percentage;
    }

    public double getPercentage() {
        return percentage;
    }

    @Override
    public boolean check(Player player, NpcConfig npc) {
        return Math.random() * 100.0 < percentage;
    }
}
