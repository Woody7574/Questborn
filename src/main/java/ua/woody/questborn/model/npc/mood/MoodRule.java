package ua.woody.questborn.model.npc.mood;

import org.bukkit.entity.Player;
import ua.woody.questborn.model.NpcConfig;
import java.util.List;

public class MoodRule {
    private final String mood;
    private final int priority;
    private final List<MoodCondition> conditions;

    public MoodRule(String mood, int priority, List<MoodCondition> conditions) {
        this.mood = mood;
        this.priority = priority;
        this.conditions = conditions;
    }

    public String getMood() {
        return mood;
    }

    public int getPriority() {
        return priority;
    }

    public List<MoodCondition> getConditions() {
        return conditions;
    }

    public boolean isApplicable(Player player, NpcConfig npc) {
        for (MoodCondition condition : conditions) {
            if (!condition.check(player, npc)) {
                return false;
            }
        }
        return true;
    }
}
