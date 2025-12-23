package ua.woody.questborn.rewards.modules;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import ua.woody.questborn.rewards.RewardExecutionContext;
import ua.woody.questborn.rewards.RewardHandler;
import ua.woody.questborn.rewards.RewardModule;

import java.util.Map;

public class AttributeRewardModule implements RewardModule {

    @Override
    public String getKey() {
        return "attributes";
    }

    @Override
    public void execute(RewardExecutionContext ctx, Player player, Object config) {
        if (!(config instanceof Map<?, ?> map)) return;

        boolean silent = RewardHandler.getSilent(map);

        for (var e : map.entrySet()) {
            if ("silent".equalsIgnoreCase(String.valueOf(e.getKey()))) continue;

            String attr = String.valueOf(e.getKey());
            double val;
            try {
                val = Double.parseDouble(String.valueOf(e.getValue()));
            } catch (Exception ignored) {
                continue;
            }

            try {
                Attribute at = Attribute.valueOf(attr.toUpperCase());
                AttributeInstance inst = player.getAttribute(at);
                if (inst == null) continue;

                inst.setBaseValue(inst.getBaseValue() + val);

                if (!silent && !ctx.isGlobalSilent()) {
                    ctx.send(player, "reward.attribute.added",
                            Map.of("attribute", attr, "value", String.valueOf(val)));
                }
            } catch (Exception ignored) {}
        }
    }
}
