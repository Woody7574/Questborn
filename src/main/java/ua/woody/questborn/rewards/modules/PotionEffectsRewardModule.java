package ua.woody.questborn.rewards.modules;

import org.bukkit.NamespacedKey;
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
        if (config == null)
            return;

        boolean silent = false;
        Object listObj;

        if (config instanceof Map<?, ?> map) {
            silent = RewardHandler.getSilent(map);
            listObj = map.get("list");
        } else {
            listObj = config;
        }

        if (!(listObj instanceof List<?> list))
            return;

        for (Object o : list) {
            if (o == null)
                continue;

            String raw = String.valueOf(o);
            String[] p = raw.split(":");

            if (p.length < 3)
                continue;

            PotionEffectType type = parsePotionEffectType(p[0]);
            if (type == null)
                continue;

            try {
                int amplifier = Integer.parseInt(p[1]);
                int duration = Integer.parseInt(p[2]);

                player.addPotionEffect(new PotionEffect(type, duration * 20, amplifier));

                if (player != null && !silent && !ctx.isSilent()) {
                    ctx.lang().sendMessage(player, "rewards.effect.added", Map.of(
                            "effect", "<translate:effect.minecraft." + getMinecraftId(type) + ">",
                            "amplifier", String.valueOf(amplifier + 1),
                            "duration", ua.woody.questborn.util.TimeFormatter.format(duration)));
                }
            } catch (Exception ignored) {
            }
        }
    }

    @SuppressWarnings("deprecation")
    private PotionEffectType parsePotionEffectType(String name) {
        String cleanName = name.trim().toUpperCase(Locale.ROOT);
        if (cleanName.startsWith("MINECRAFT:")) {
            cleanName = cleanName.substring(10);
        }
        PotionEffectType type = PotionEffectType.getByName(cleanName);
        if (type != null) return type;

        switch (cleanName) {
            case "HASTE": return PotionEffectType.FAST_DIGGING;
            case "MINING_FATIGUE": return PotionEffectType.SLOW_DIGGING;
            case "NAUSEA": return PotionEffectType.CONFUSION;
            case "RESISTANCE": return PotionEffectType.DAMAGE_RESISTANCE;
            case "INSTANT_HEALTH": case "HEAL": return PotionEffectType.HEAL;
            case "INSTANT_DAMAGE": case "HARM": return PotionEffectType.HARM;
            case "JUMP_BOOST": return PotionEffectType.JUMP;
            case "SLOWNESS": return PotionEffectType.SLOW;
            case "STRENGTH": return PotionEffectType.INCREASE_DAMAGE;
            case "SPEED": return PotionEffectType.SPEED;
            default: return null;
        }
    }

    private String getMinecraftId(PotionEffectType type) {
        String name = type.getName().toLowerCase(Locale.ROOT);
        switch (name) {
            case "fast_digging": return "haste";
            case "slow_digging": return "mining_fatigue";
            case "confusion": return "nausea";
            case "damage_resistance": return "resistance";
            case "water_breathing": return "water_breathing";
            case "heal": return "instant_health";
            case "harm": return "instant_damage";
            case "jump": return "jump_boost";
            case "slow": return "slowness";
            case "increase_damage": return "strength";
            default: return name;
        }
    }
}
