package ua.woody.questborn.commands.sub;

import org.bukkit.command.CommandSender;
import ua.woody.questborn.QuestbornPlugin;

public abstract class BaseCommand implements SubCommand {

    protected final QuestbornPlugin plugin;

    protected BaseCommand(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    protected boolean has(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission("quest.admin");
    }
}
