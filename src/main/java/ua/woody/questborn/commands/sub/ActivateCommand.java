package ua.woody.questborn.commands.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestDefinition;

import java.util.Map;

public class ActivateCommand extends BaseCommand {

    public ActivateCommand(QuestbornPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "activate";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        var lang = plugin.getLanguage();

        if (!has(sender, "quest.command.activate")) {
            sender.sendMessage(lang.tr("command.no-permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(lang.tr("command.activate.usage"));
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

        QuestDefinition def = plugin.getQuestManager().getById(questId);
        if (def == null) {
            sender.sendMessage(lang.tr("command.errors.quest-not-found",
                    Map.of("quest", questId)));
            return true;
        }

        // Форс-заміна активного квесту
        PlayerQuestProgress data = plugin.getPlayerDataStore().get(target.getUniqueId());
        String active = data.getActiveQuestId();
        if (active != null && !active.equalsIgnoreCase(questId)) {
            data.setActiveQuestId(null);
            data.setActiveQuestProgress(0);
            data.setActiveQuestStartTime(0L);
            data.clearPendingQuest();
            plugin.getPlayerDataStore().save();
        }

        boolean ok = plugin.getQuestManager().activateQuest(target, questId);

        sender.sendMessage(lang.tr(
                ok ? "command.activate.success" : "command.activate.fail",
                Map.of("quest", questId, "player", target.getName())
        ));

        return true;
    }
}
