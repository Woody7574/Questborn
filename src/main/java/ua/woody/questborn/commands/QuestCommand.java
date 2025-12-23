package ua.woody.questborn.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.commands.sub.*;

import ua.woody.questborn.gui.MainMenuGui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class QuestCommand implements CommandExecutor {

    private final QuestbornPlugin plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public QuestCommand(QuestbornPlugin plugin) {
        this.plugin = plugin;

        // Реєстрація сабкоманд
        register(new HelpCommand(plugin));
        register(new ActivateCommand(plugin));
        register(new DeactivateCommand(plugin));
        register(new FinishCommand(plugin));
        register(new OpenCommand(plugin));
        register(new ResetCommand(plugin));
        register(new EffectCommand(plugin));
        register(new ReloadCommand(plugin));
    }

    private void register(SubCommand cmd) {
        subCommands.put(cmd.getName().toLowerCase(Locale.ROOT), cmd);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        boolean isPlayer = sender instanceof Player;
        Player p = isPlayer ? (Player) sender : null;

        // ---------------------------------------------------------
        // /quest → просто відкрити головне меню
        // ---------------------------------------------------------
        if (args.length == 0) {

            if (!isPlayer) {
                sender.sendMessage(plugin.getLanguage().tr("command.player-only"));
                return true;
            }

            if (!p.hasPermission("quest.use")) {
                p.sendMessage(plugin.getLanguage().tr("command.no-permission"));
                return true;
            }

            new MainMenuGui(plugin, p).open();
            return true;
        }

        // ---------------------------------------------------------
        // /quest <subcommand> ...
        // ---------------------------------------------------------
        String subName = args[0].toLowerCase(Locale.ROOT);
        SubCommand handler = subCommands.get(subName);

        if (handler != null) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return handler.execute(sender, subArgs);
        }

        // Невідома команда → покажемо help, якщо є доступ
        if (sender.hasPermission("quest.use") || sender.hasPermission("quest.admin")) {
            SubCommand help = subCommands.get("help");
            if (help != null) {
                help.execute(sender, new String[0]);
                return true;
            }
        }

        // Fallback → відкрити меню як раніше
        if (isPlayer && p.hasPermission("quest.use")) {
            new MainMenuGui(plugin, p).open();
        }

        return true;
    }
}
