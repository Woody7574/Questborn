package ua.woody.questborn.rewards.modules;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ua.woody.questborn.rewards.RewardExecutionContext;
import ua.woody.questborn.rewards.RewardHandler;
import ua.woody.questborn.rewards.RewardModule;

import java.util.List;
import java.util.Locale;

public class CommandRewardModule implements RewardModule {

    @Override
    public String getKey() {
        return "commands";
    }

    @Override
    public void execute(RewardExecutionContext ctx, Player player, Object config) {
        if (config == null) return;

        boolean silentType = false;
        Object listObj;

        if (config instanceof java.util.Map<?, ?> map) {
            silentType = RewardHandler.getSilent(map);
            listObj = map.get("list");
        } else {
            listObj = config;
        }

        if (!(listObj instanceof List<?> list)) return;

        for (Object o : list) {
            String raw = String.valueOf(o);
            boolean silentInline = raw.toUpperCase(Locale.ROOT).startsWith("[SILENT]");
            String cmd = silentInline ? raw.substring(8).trim() : raw;

            cmd = cmd.replace("%player%", player.getName());

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            // повідомлення ми і раніше не відправляли
        }
    }
}
