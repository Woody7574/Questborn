package ua.woody.questborn.commands.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.PlayerQuestProgress;

import java.util.Map;

public class DeactivateCommand extends BaseCommand {

    public DeactivateCommand(QuestbornPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "deactivate";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        var lang = plugin.getLanguage();

        if (!has(sender, "quest.command.deactivate")) {
            sender.sendMessage(lang.tr("command.no-permission"));
            return true;
        }

        Player target = null;

        if (args.length >= 1) {
            target = Bukkit.getPlayer(args[0]);
        } else if (sender instanceof Player p) {
            target = p;
        }

        if (target == null) {
            sender.sendMessage(lang.tr("command.errors.player-not-found",
                    Map.of("player", args.length >= 1 ? args[0] : "null")));
            return true;
        }

        PlayerQuestProgress data = plugin.getPlayerDataStore().get(target.getUniqueId());
        String activeId = data.getActiveQuestId();

        if (activeId == null) {
            sender.sendMessage(lang.tr("command.deactivate.no-active"));
            return true;
        }

        plugin.getQuestManager().cancelQuest(target, activeId);

        sender.sendMessage(lang.tr("command.deactivate.success",
                Map.of("player", target.getName())));

        return true;
    }
}
