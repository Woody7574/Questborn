package ua.woody.questborn.commands.sub;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import ua.woody.questborn.QuestbornPlugin;

import java.util.List;

public class HelpCommand extends BaseCommand {

    private final MiniMessage mm = MiniMessage.miniMessage();

    public HelpCommand(QuestbornPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        var lang = plugin.getLanguage();

        // Потрібен хоча б quest.use
        if (!has(sender, "quest.use")) {
            sender.sendMessage(lang.tr("command.no-permission"));
            return true;
        }

        sender.sendMessage(lang.tr("command.help.header"));

        // {команда, ключ опису, permission}
        List<String[]> cmds = List.of(
                new String[]{"/quest",                            "open",       "quest.use"},
                new String[]{"/quest help",                       "help",       "quest.use"},
                new String[]{"/quest activate <quest> <player?>", "activate",   "quest.command.activate"},
                new String[]{"/quest deactivate <player?>",       "deactivate", "quest.command.deactivate"},
                new String[]{"/quest finish <player?>",           "finish",     "quest.command.finish"},
                new String[]{"/quest open <menu> <player?>",      "open-menu",  "quest.command.open"},
                new String[]{"/quest open details <quest> <player?>", "open-details", "quest.command.open"},
                new String[]{"/quest reset <quest> <player?>",    "reset",      "quest.command.reset"},
                new String[]{"/quest effect <id> <player?>",      "effect",     "quest.command.effect"},
                new String[]{"/quest reload",                     "reload",     "quest.command.reload"}
        );

        for (String[] c : cmds) {
            String command = c[0];
            String descKey = "command.help.desc." + c[1];
            String perm = c[2];

            if (!has(sender, perm)) {
                continue;
            }

            String desc = lang.tr(descKey);

            Component line = mm.deserialize("<#ffe999>" + command + " <gray>— " + desc)
                    .hoverEvent(HoverEvent.showText(
                            mm.deserialize("<white>Permission: <#ffffff>" + perm)
                    ))
                    .clickEvent(ClickEvent.suggestCommand(command));

            sender.sendMessage(line);
        }

        return true;
    }
}
