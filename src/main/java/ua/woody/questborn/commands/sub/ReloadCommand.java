package ua.woody.questborn.commands.sub;

import org.bukkit.command.CommandSender;
import ua.woody.questborn.QuestbornPlugin;

public class ReloadCommand extends BaseCommand {

    public ReloadCommand(QuestbornPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        var lang = plugin.getLanguage();

        if (!has(sender, "quest.command.reload")) {
            sender.sendMessage(lang.tr("command.admin.no-permission"));
            return true;
        }

        plugin.reloadAll();
        sender.sendMessage(lang.tr("command.reload.done"));
        return true;
    }
}
