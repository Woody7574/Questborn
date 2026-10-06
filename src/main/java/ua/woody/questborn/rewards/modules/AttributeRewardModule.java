package ua.woody.questborn.rewards.modules;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import ua.woody.questborn.rewards.RewardExecutionContext;
import ua.woody.questborn.rewards.RewardHandler;
import ua.woody.questborn.rewards.RewardModule;

import java.util.Locale;
import java.util.Map;

public class AttributeRewardModule implements RewardModule {
    @Override
    public String getKey() {
        return "attributes";
    }

    @Override
    public void execute(RewardExecutionContext ctx, Player player, Object config) {
        if (!(config instanceof Map<?, ?> map))
            return;

        boolean silent = RewardHandler.getSilent(map);

        for (var e : map.entrySet()) {
            String keyStr = String.valueOf(e.getKey());
            if ("silent".equalsIgnoreCase(keyStr))
                continue;

            String attrName = keyStr;
            double val = 0;
            Double maxCap = null;

            if (e.getValue() instanceof Map<?, ?> valMap) {
                try {
                    val = Double.parseDouble(String.valueOf(valMap.get("value")));
                } catch (Exception ignored) {
                    continue;
                }
                if (valMap.containsKey("max")) {
                    try {
                        maxCap = Double.parseDouble(String.valueOf(valMap.get("max")));
                    } catch (Exception ignored) {
                    }
                } else if (valMap.containsKey("cap")) {
                    try {
                        maxCap = Double.parseDouble(String.valueOf(valMap.get("cap")));
                    } catch (Exception ignored) {
                    }
                }
            } else {
                try {
                    val = Double.parseDouble(String.valueOf(e.getValue()));
                } catch (Exception ignored) {
                    continue;
                }
            }

            val *= ctx.getMultiplier();

            try {
                Attribute at = parseAttribute(attrName);
                if (at == null) continue;

                AttributeInstance inst = player.getAttribute(at);
                if (inst == null)
                    continue;

                double newBase = inst.getBaseValue() + val;
                if (maxCap != null) {
                    newBase = Math.min(newBase, maxCap);
                }

                inst.setBaseValue(newBase);

                if (player != null && !silent && !ctx.isGlobalSilent()) {
                    String translatedAttribute = "<translate:" + getMinecraftAttributeKey(at) + ">";
                    ctx.lang().sendMessage(player, "rewards.attribute.added", Map.of(
                            "attribute", translatedAttribute,
                            "value", String.valueOf(val)));
                }
            } catch (Exception ignored) {
            }
        }
    }

    private String getMinecraftAttributeKey(Attribute at) {
        String name = at.name().toLowerCase(Locale.ROOT);
        if (name.startsWith("generic_")) {
            return "attribute.name.generic." + name.substring(8);
        } else if (name.startsWith("player_")) {
            return "attribute.name.player." + name.substring(7);
        } else if (name.startsWith("zombie_")) {
            return "attribute.name.zombie." + name.substring(7);
        } else if (name.startsWith("horse_")) {
            return "attribute.name.horse." + name.substring(6);
        }
        return "attribute.name." + name;
    }

    private Attribute parseAttribute(String name) {
        String clean = name.toUpperCase(Locale.ROOT).trim();
        try {
            return Attribute.valueOf(clean);
        } catch (Exception ignored) {
        }
        if (!clean.startsWith("GENERIC_")) {
            try {
                return Attribute.valueOf("GENERIC_" + clean);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
