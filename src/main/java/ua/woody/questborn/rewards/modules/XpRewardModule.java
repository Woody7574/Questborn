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
        if (config == null) return;

        // legacy: xp: 250  -> points
        if (!(config instanceof Map<?, ?> data)) {
            int points;
            try {
                points = Integer.parseInt(String.valueOf(config));
            } catch (Exception ex) {
                // якщо в тебе немає reward.xp.invalid — заміни на будь-який існуючий ключ
                ctx.send(player, "reward.xp.invalid");
                return;
            }

            if (points <= 0) return;

            player.giveExp(points);

            if (!ctx.isGlobalSilent()) {
                ctx.send(player, "reward.xp.points",
                        Map.of("amount", String.valueOf(points)));
            }
            return;
        }

        // new format: xp: { points: 250, levels: 2, silent: true }
        boolean silent = RewardHandler.getSilent(data);

        // LEVELS
        if (data.containsKey("levels")) {
            try {
                int levels = Integer.parseInt(String.valueOf(data.get("levels")));
                if (levels > 0) {
                    player.giveExpLevels(levels);
                    if (!silent && !ctx.isGlobalSilent()) {
                        ctx.send(player, "reward.xp.levels",
                                Map.of("amount", String.valueOf(levels)));
                    }
                }
            } catch (Exception ignored) {}
        }

        // POINTS
        if (data.containsKey("points")) {
            try {
                int points = Integer.parseInt(String.valueOf(data.get("points")));
                if (points > 0) {
                    player.giveExp(points);
                    if (!silent && !ctx.isGlobalSilent()) {
                        ctx.send(player, "reward.xp.points",
                                Map.of("amount", String.valueOf(points)));
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}
