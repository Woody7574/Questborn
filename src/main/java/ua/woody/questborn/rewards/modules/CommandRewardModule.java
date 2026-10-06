package ua.woody.questborn.rewards.modules;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ua.woody.questborn.rewards.RewardExecutionContext;
import ua.woody.questborn.rewards.RewardHandler;
import ua.woody.questborn.rewards.RewardModule;
import ua.woody.questborn.util.PlaceholderUtil;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommandRewardModule implements RewardModule {
    @Override
    public String getKey() {
        return "commands";
    }

    @Override
    public void execute(RewardExecutionContext ctx, Player player, Object config) {
        if (config == null || player == null)
            return;

        Object listObj;

        if (config instanceof Map<?, ?> map) {
            listObj = map.get("list");
        } else {
            listObj = config;
        }

        if (!(listObj instanceof List<?> list))
            return;

        for (int i = 0; i < ctx.getMultiplier(); i++) {
            for (Object o : list) {
                if (o == null) continue;
                String raw = String.valueOf(o);

                boolean isPlayerCommand = false;

                boolean prefixFound = true;
                while (prefixFound) {
                    prefixFound = false;
                    String upper = raw.toUpperCase(Locale.ROOT).trim();

                    if (upper.startsWith("[SILENT_PLAYER]") || upper.startsWith("[PLAYER_SILENT]")) {
                        isPlayerCommand = true;
                        raw = raw.substring(15).trim();
                        prefixFound = true;
                    } else if (upper.startsWith("[SILENT_CONSOLE]") || upper.startsWith("[CONSOLE_SILENT]")) {
                        isPlayerCommand = false;
                        raw = raw.substring(16).trim();
                        prefixFound = true;
                    } else if (upper.startsWith("[PLAYER]")) {
                        isPlayerCommand = true;
                        raw = raw.substring(8).trim();
                        prefixFound = true;
                    } else if (upper.startsWith("[CONSOLE]")) {
                        isPlayerCommand = false;
                        raw = raw.substring(9).trim();
                        prefixFound = true;
                    } else if (upper.startsWith("[SILENT]")) {
                        raw = raw.substring(8).trim();
                        prefixFound = true;
                    }
                }

                String cmd = raw.replace("%player%", player.getName())
                        .replace("%player_name%", player.getName())
                        .replace("{player}", player.getName());

                if (ctx.getQuest() != null) {
                    cmd = cmd.replace("{quest}", ctx.getQuest().getDisplayName())
                            .replace("{quest_name}", ctx.getQuest().getDisplayName())
                            .replace("{quest_id}", ctx.getQuest().getId());
                }

                cmd = PlaceholderUtil.format(ctx.getPlugin(), player, cmd);

                if (cmd.startsWith("/")) {
                    cmd = cmd.substring(1);
                }

                final String finalCmd = cmd;
                final boolean finalIsPlayer = isPlayerCommand;

                ctx.runSync(player, () -> {
                    try {
                        if (finalIsPlayer) {
                            player.performCommand(finalCmd);
                        } else {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                        }
                    } catch (Throwable t) {
                        ctx.getPlugin().getLogger().warning("Error executing reward command '" + finalCmd + "': " + t.getMessage());
                    }
                });
            }
        }
    }
}
