package ua.woody.questborn.rewards;

import org.bukkit.entity.Player;

public interface RewardModule {
    String getKey();

    void execute(RewardExecutionContext ctx, Player player, Object config);
}
