package ua.woody.questborn.commands.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.PlayerQuestProgress;

import java.util.Map;

public class ResetCommand extends BaseCommand {

    public ResetCommand(QuestbornPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "reset";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        var lang = plugin.getLanguage();

        if (!has(sender, "quest.command.reset")) {
            sender.sendMessage(lang.tr("command.no-permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(lang.tr("command.reset.usage"));
            return true;
        }

        String questId = args[0];
        Player target = null;

        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
        } else if (sender instanceof Player p) {
            target = p;
        }

        if (target == null) {
            sender.sendMessage(lang.tr("command.errors.player-not-found",
                    Map.of("player", args.length >= 2 ? args[1] : "null")));
            return true;
        }

        PlayerQuestProgress data = plugin.getPlayerDataStore().get(target.getUniqueId());

        boolean progressReset = false;
        boolean cooldownReset = false;

        String active = data.getActiveQuestId();
        boolean isActive = questId.equalsIgnoreCase(active);
        boolean isCompleted = data.getCompletedQuests().contains(questId);

        // Якщо квест активний → скидаємо прогрес
        if (isActive) {
            data.setActiveQuestProgress(0);
            progressReset = true;
        }

        // Якщо квест виконаний → чіпаємо тільки cooldown
        if (isCompleted) {
            if (data.getQuestCooldowns().remove(questId) != null) {
                cooldownReset = true;
            }

            plugin.getPlayerDataStore().save();

            if (cooldownReset) {
                sender.sendMessage(lang.tr("command.reset.cooldown-reset",
                        Map.of("quest", questId, "player", target.getName())));
            } else {
                sender.sendMessage(lang.tr("command.reset.nothing"));
            }

            return true;
        }

        // Не активний + не виконаний → може бути тільки cooldown
        if (data.getQuestCooldowns().remove(questId) != null) {
            cooldownReset = true;
        }

        if (progressReset && cooldownReset) {
            sender.sendMessage(lang.tr("command.reset.active-and-cooldown-reset"));
        } else if (progressReset) {
            sender.sendMessage(lang.tr("command.reset.active-reset"));
        } else if (cooldownReset) {
            sender.sendMessage(lang.tr("command.reset.cooldown-reset",
                    Map.of("quest", questId, "player", target.getName())));
        } else {
            sender.sendMessage(lang.tr("command.reset.nothing"));
        }

        plugin.getPlayerDataStore().save();
        return true;
    }
}
