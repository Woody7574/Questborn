package ua.woody.questborn.rewards.modules;

import org.bukkit.entity.Player;
import ua.woody.questborn.rewards.RewardExecutionContext;
import ua.woody.questborn.rewards.RewardHandler;
import ua.woody.questborn.rewards.RewardModule;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ChanceRewardModule implements RewardModule {
    @Override
    public String getKey() {
        return "chance";
    }

    @Override
    public void execute(RewardExecutionContext ctx, Player player, Object config) {
        if (config == null || player == null)
            return;

        if (!(config instanceof Map<?, ?> map))
            return;

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) map;

        Object listObj = data.get("list");
        if (listObj instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> entryMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> entryData = (Map<String, Object>) entryMap;
                    processChanceEntry(ctx, player, entryData);
                }
            }
        } else {
            if (data.containsKey("percentage") || data.containsKey("chance") || data.containsKey("rewards")) {
                processChanceEntry(ctx, player, data);
            } else {
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    if (entry.getValue() instanceof Map<?, ?> entryMap) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> entryData = (Map<String, Object>) entryMap;
                        processChanceEntry(ctx, player, entryData);
                    }
                }
            }
        }
    }

    private void processChanceEntry(RewardExecutionContext ctx, Player player, Map<String, Object> data) {
        double chance = 100.0;
        if (data.containsKey("chance")) {
            try {
                chance = Double.parseDouble(String.valueOf(data.get("chance")));
            } catch (Exception ignored) {
            }
        } else if (data.containsKey("percentage")) {
            try {
                chance = Double.parseDouble(String.valueOf(data.get("percentage")));
            } catch (Exception ignored) {
            }
        }

        double roll = ThreadLocalRandom.current().nextDouble(0.0, 100.0);
        if (roll > chance) {
            return;
        }

        Object rewardsObj = data.get("rewards");
        if (rewardsObj instanceof Map<?, ?> rewardsMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> customRewards = (Map<String, Object>) rewardsMap;
            RewardHandler.giveCustomRewards(player, ctx.getQuest(), customRewards, ctx.getMultiplier());
        }
    }
}
