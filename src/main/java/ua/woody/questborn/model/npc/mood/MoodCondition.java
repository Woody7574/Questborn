package ua.woody.questborn.model.npc.mood;

import org.bukkit.entity.Player;
import ua.woody.questborn.model.NpcConfig;

public interface MoodCondition {
    boolean check(Player player, NpcConfig npc);
}
