package ua.woody.questborn.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class QuestTabCompleter implements TabCompleter {

    private final QuestbornPlugin plugin;

    public QuestTabCompleter(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {

        if (!cmd.getName().equalsIgnoreCase("quest"))
            return List.of();

        boolean admin = sender.hasPermission("quest.admin");

        /* =========================================================
         * ARG 1 — список сабкоманд
         * ========================================================= */
        if (args.length == 1) {

            List<String> base = new ArrayList<>();

            if (sender.hasPermission("quest.use")) {
                base.add("help");
                base.add("open");
            }

            if (admin || sender.hasPermission("quest.command.activate"))
                base.add("activate");

            if (admin || sender.hasPermission("quest.command.deactivate"))
                base.add("deactivate");

            if (admin || sender.hasPermission("quest.command.finish"))
                base.add("finish");

            if (admin || sender.hasPermission("quest.command.reset"))
                base.add("reset");

            if (admin || sender.hasPermission("quest.command.effect"))
                base.add("effect");

            if (admin || sender.hasPermission("quest.command.reload"))
                base.add("reload");

            return filter(base, args[0]);
        }

        /* =========================================================
         * ARG 2 — після сабкоманди
         * ========================================================= */
        if (args.length == 2) {

            String sub = args[0].toLowerCase();

            return switch (sub) {

                case "help" -> List.of();

                case "activate", "reset" ->
                        filter(plugin.getQuestManager().getAllIds(), args[1]);

                case "open" ->
                        filter(List.of("main", "top", "details", "list"), args[1]);

                case "deactivate", "finish" ->
                        filterPlayers(args[1]);

                case "effect" ->
                        filter(getEffectPresetIds(), args[1]);

                default -> List.of();
            };
        }

        /* =========================================================
         * ARG 3 — тут з’являються твої списки!
         * ========================================================= */
        if (args.length == 3) {

            String sub = args[0].toLowerCase();

            if (sub.equals("open")) {

                // ========== /quest open details <questId> <player?>
                if (args[1].equalsIgnoreCase("details")) {
                    return filter(plugin.getQuestManager().getAllIds(), args[2]);
                }

                // ========== /quest open list <typeId> <player?>
                if (args[1].equalsIgnoreCase("list")) {
                    return filter(getTypeIds(), args[2]);
                }

                // /quest open main/top <player>
                return filterPlayers(args[2]);
            }

            // activate/reset/effect <value> <player>
            return filterPlayers(args[2]);
        }

        /* =========================================================
         * ARG 4 — player selector
         * ========================================================= */
        if (args.length == 4) {

            String sub = args[0].toLowerCase();

            if (sub.equals("open")) {

                if (args[1].equalsIgnoreCase("details")) {
                    return filterPlayers(args[3]);
                }

                if (args[1].equalsIgnoreCase("list")) {
                    return filterPlayers(args[3]);
                }
            }
        }

        return List.of();
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private List<String> filter(Collection<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .sorted()
                .toList();
    }

    private List<String> filterPlayers(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
                .sorted()
                .toList();
    }

    /** Повертає список типів з QuestTypeManager */
    private List<String> getTypeIds() {
        return plugin.getQuestManager()
                .getQuestTypeManager()
                .getEnabledTypes()
                .stream()
                .map(type -> type.getId().toLowerCase())
                .toList();
    }

    private Set<String> getEffectPresetIds() {
        try {
            var managerField = plugin.getQuestManager().getClass()
                    .getDeclaredField("effectPresetManager");
            managerField.setAccessible(true);
            Object mgr = managerField.get(plugin.getQuestManager());

            var presetsField = mgr.getClass().getDeclaredField("presets");
            presetsField.setAccessible(true);
            Object raw = presetsField.get(mgr);

            if (raw instanceof Map<?, ?> map) {
                return map.keySet().stream()
                        .filter(k -> k instanceof String)
                        .map(k -> (String) k)
                        .collect(Collectors.toSet());
            }

        } catch (Exception ignored) {}

        return Set.of();
    }
}
