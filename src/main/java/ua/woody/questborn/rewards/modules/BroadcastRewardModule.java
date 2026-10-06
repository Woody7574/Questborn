package ua.woody.questborn.rewards.modules;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ua.woody.questborn.rewards.RewardExecutionContext;
import ua.woody.questborn.rewards.RewardHandler;
import ua.woody.questborn.rewards.RewardModule;
import ua.woody.questborn.util.PlaceholderUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BroadcastRewardModule implements RewardModule {
    private final String key;

    public BroadcastRewardModule() {
        this("broadcast");
    }

    public BroadcastRewardModule(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void execute(RewardExecutionContext ctx, Player player, Object config) {
        if (config == null || player == null || ctx.isGlobalSilent())
            return;

        boolean silent = false;
        Object rawTextObj;

        if (config instanceof Map<?, ?> map) {
            silent = RewardHandler.getSilent(map);
            if (map.containsKey("text")) {
                rawTextObj = map.get("text");
            } else if (map.containsKey("messages")) {
                rawTextObj = map.get("messages");
            } else if (map.containsKey("list")) {
                rawTextObj = map.get("list");
            } else if (map.containsKey("broadcast")) {
                rawTextObj = map.get("broadcast");
            } else {
                rawTextObj = null;
            }
        } else {
            rawTextObj = config;
        }

        if (silent || rawTextObj == null)
            return;

        List<String> lines = new ArrayList<>();
        if (rawTextObj instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) lines.add(String.valueOf(item));
            }
        } else {
            lines.add(String.valueOf(rawTextObj));
        }

        for (String rawLine : lines) {
            if (rawLine == null) continue;

            String formatted = rawLine
                    .replace("%player%", player.getName())
                    .replace("%player_name%", player.getName())
                    .replace("{player}", player.getName());

            if (ctx.getQuest() != null) {
                formatted = formatted
                        .replace("{quest}", ctx.getQuest().getDisplayName())
                        .replace("{quest_name}", ctx.getQuest().getDisplayName())
                        .replace("{quest_id}", ctx.getQuest().getId());
            }

            formatted = PlaceholderUtil.format(ctx.getPlugin(), player, formatted);

            for (Player recipient : Bukkit.getOnlinePlayers()) {
                ctx.getLang().sendRawMessage(recipient, ctx.getLang().convertLegacyToMiniMessage(formatted));
            }
        }
    }
}
