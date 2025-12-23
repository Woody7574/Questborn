package ua.woody.questborn.rewards.modules;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ua.woody.questborn.rewards.RewardExecutionContext;
import ua.woody.questborn.rewards.RewardHandler;
import ua.woody.questborn.rewards.RewardModule;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PotionEffectsRewardModule implements RewardModule {

    @Override
    public String getKey() {
        return "effects";
    }

    @Override
    public void execute(RewardExecutionContext ctx, Player player, Object config) {
        if (config == null) return;

        boolean silent = false;
        Object listObj;

        if (config instanceof Map<?, ?> map) {
            silent = RewardHandler.getSilent(map);
            listObj = map.get("list");
        } else {
            listObj = config;
        }

        if (!(listObj instanceof List<?> list)) return;

        for (Object o : list) {
            if (o == null) continue;

            String raw = String.valueOf(o);
            String[] p = raw.split(":");

            if (p.length < 3) continue;

            PotionEffectType type = PotionEffectType.getByName(p[0].toUpperCase(Locale.ROOT));
            if (type == null) continue;

            try {
                int amplifier = Integer.parseInt(p[1]);
                int duration = Integer.parseInt(p[2]);

                player.addPotionEffect(new PotionEffect(type, duration * 20, amplifier));

                if (!silent && !ctx.isGlobalSilent()) {
                    ctx.send(player, "reward.effect.added",
                            Map.of(
                                    "effect", p[0],
                                    "amplifier", p[1],
                                    "duration", p[2]
                            ));
                }
            } catch (Exception ignored) {}
        }
    }
}
