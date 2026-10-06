package ua.woody.questborn.rewards.modules;

import org.bukkit.entity.Player;
import ua.woody.questborn.rewards.RewardExecutionContext;
import ua.woody.questborn.rewards.RewardHandler;
import ua.woody.questborn.rewards.RewardModule;

import java.util.Map;

public class XpRewardModule implements RewardModule {
    @Override
    public String getKey() {
        return "xp";
    }

    @Override
    public void execute(RewardExecutionContext ctx, Player player, Object config) {
        if (config == null)
            return;

        if (!(config instanceof Map<?, ?> data)) {
            int points;
            try {
                points = Integer.parseInt(String.valueOf(config));
            } catch (Exception ex) {
                ctx.send(player, "rewards.xp.invalid");
                return;
            }

            if (points <= 0)
                return;

            points *= ctx.getMultiplier();

            player.giveExp(points);

            if (!ctx.isGlobalSilent()) {
                ctx.send(player, "rewards.xp.points",
                        Map.of("amount", String.valueOf(points)));
            }
            return;
        }

        boolean silent = RewardHandler.getSilent(data);

        if (data.containsKey("levels")) {
            try {
                int levels = Integer.parseInt(String.valueOf(data.get("levels")));
                if (levels > 0) {
                    levels *= ctx.getMultiplier();
                    player.giveExpLevels(levels);
                    if (!silent && !ctx.isGlobalSilent()) {
                        if (player != null && !ctx.isSilent()) {
                            String msg = ctx.lang().tr("rewards.xp.levels", Map.of("amount", String.valueOf(levels)));
                            if (msg != null)
                                player.sendMessage(msg);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (data.containsKey("points")) {
            try {
                int points = Integer.parseInt(String.valueOf(data.get("points")));
                if (points > 0) {
                    points *= ctx.getMultiplier();
                    player.giveExp(points);
                    if (!silent && !ctx.isGlobalSilent()) {
                        ctx.send(player, "rewards.xp.points",
                                Map.of("amount", String.valueOf(points)));
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}
