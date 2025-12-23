package ua.woody.questborn.commands.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.MainMenuGui;
import ua.woody.questborn.gui.QuestDetailsGui;
import ua.woody.questborn.gui.QuestListGui;
import ua.woody.questborn.gui.TopGui;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestTypeConfig;

import java.util.Locale;
import java.util.Map;

public class OpenCommand extends BaseCommand {

    public OpenCommand(QuestbornPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "open";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        var lang = plugin.getLanguage();

        if (!has(sender, "quest.command.open")) {
            sender.sendMessage(lang.tr("command.no-permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(lang.tr("command.open.usage"));
            return true;
        }

        String menu = args[0].toLowerCase(Locale.ROOT);

        // =====================================================================
        // /quest open details <quest> <player?>
        // =====================================================================
        if (menu.equals("details")) {

            if (args.length < 2) {
                sender.sendMessage(lang.tr("command.open.usage"));
                return true;
            }

            String questId = args[1];
            QuestDefinition def = plugin.getQuestManager().getById(questId);

            if (def == null) {
                sender.sendMessage(lang.tr("command.errors.quest-not-found",
                        Map.of("quest", questId)));
                return true;
            }

            Player target = resolveTarget(sender, args, 2);
            if (target == null) return true;

            new QuestDetailsGui(plugin, target, def).open();

            // повідомляємо тільки якщо відкриваєш іншому
            sendOpenedMessage(sender, target, "details:" + questId);

            return true;
        }

        // =====================================================================
        // /quest open list <type> <player?>
        // =====================================================================
        if (menu.equals("list")) {

            if (args.length < 2) {
                sender.sendMessage("Usage: /quest open list <type> <player?>");
                return true;
            }

            String typeId = args[1].toLowerCase();
            QuestTypeConfig typeConfig =
                    plugin.getQuestManager().getQuestTypeManager().getType(typeId);

            if (typeConfig == null) {
                sender.sendMessage(lang.tr("command.errors.quest-not-found",
                        Map.of("quest", typeId)));
                return true;
            }

            Player target = resolveTarget(sender, args, 2);
            if (target == null) return true;

            new QuestListGui(plugin, target, typeConfig, 0).open();

            sendOpenedMessage(sender, target, "list:" + typeId);

            return true;
        }

        // =====================================================================
        // /quest open main/top <player?>
        // =====================================================================

        Player target = resolveTarget(sender, args, 1);
        if (target == null) return true;

        switch (menu) {
            case "main", "menu" -> new MainMenuGui(plugin, target).open();
            case "top"          -> new TopGui(plugin, target).open();
            default -> {
                sender.sendMessage(lang.tr("command.open.unknown-menu",
                        Map.of("menu", menu)));
                return true;
            }
        }

        sendOpenedMessage(sender, target, menu);

        return true;
    }

    // =====================================================================
    // HELPERS
    // =====================================================================

    private Player resolveTarget(CommandSender sender, String[] args, int index) {

        if (args.length > index) {
            Player target = Bukkit.getPlayer(args[index]);
            if (target == null) {
                sender.sendMessage(plugin.getLanguage().tr("command.errors.player-not-found",
                        Map.of("player", args[index])));
                return null;
            }
            return target;
        }

        if (sender instanceof Player p) return p;

        sender.sendMessage("Console must specify player.");
        return null;
    }

    /**
     * Відправляє повідомлення тільки якщо sender != target.
     */
    private void sendOpenedMessage(CommandSender sender, Player target, String menuName) {

        var lang = plugin.getLanguage();

        // Якщо гравець відкриває для себе — не повідомляємо
        if (sender instanceof Player p && p.getUniqueId().equals(target.getUniqueId()))
            return;

        // Повідомлення для відправника
        sender.sendMessage(lang.tr(
                "command.open.opened",
                Map.of("menu", menuName, "player", target.getName())
        ));
    }
}
