package ua.woody.questborn.commands.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.effects.EffectPresetManager;

import java.util.Map;
import java.util.Set;

public class EffectCommand extends BaseCommand {

    public EffectCommand(QuestbornPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "effect";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        var lang = plugin.getLanguage();

        if (!has(sender, "quest.command.effect")) {
            sender.sendMessage(lang.tr("command.no-permission"));
            return true;
        }

        // /quest effect → список ефектів
        if (args.length == 0) {
            Set<String> ids = getEffectPresetIds();
            if (ids.isEmpty()) {
                sender.sendMessage(lang.tr("command.effect.no-effects"));
                return true;
            }

            sender.sendMessage(lang.tr("command.effect.list-header"));
            int i = 1;
            for (String id : ids) {
                sender.sendMessage(lang.color(
                        String.format("  <#55ff55>%d. <#ffffff>%s", i++, id)
                ));
            }
            sender.sendMessage(lang.tr("command.effect.usage"));
            return true;
        }

        String effectId = args[0];
        Player target;

        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(lang.tr("command.errors.player-not-found",
                        Map.of("player", args[1])));
                return true;
            }
        } else {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Console must specify target: /quest effect <id> <player>");
                return true;
            }
            target = p;
        }

        if (!hasEffectPreset(effectId)) {
            sender.sendMessage(lang.tr("command.effect.not-found",
                    Map.of("effect", effectId)));
            sender.sendMessage(lang.tr("command.effect.usage"));
            return true;
        }

        boolean ok = playEffectPreset(target, effectId);
        if (!ok) {
            sender.sendMessage(lang.tr("command.effect.not-found",
                    Map.of("effect", effectId)));
            return true;
        }

        if (sender.equals(target)) {
            sender.sendMessage(lang.tr("command.effect.success-self",
                    Map.of("effect", effectId)));
        } else {
            sender.sendMessage(lang.tr("command.effect.success-other",
                    Map.of("effect", effectId, "player", target.getName())));
            target.sendMessage(lang.tr("command.effect.received",
                    Map.of("effect", effectId)));
        }

        return true;
    }

    // ----------------- helpers via reflection -----------------

    private Set<String> getEffectPresetIds() {
        try {
            var f = plugin.getQuestManager().getClass().getDeclaredField("effectPresetManager");
            f.setAccessible(true);
            EffectPresetManager mgr = (EffectPresetManager) f.get(plugin.getQuestManager());

            var mapField = mgr.getClass().getDeclaredField("presets");
            mapField.setAccessible(true);
            Map<String, ?> presets = (Map<String, ?>) mapField.get(mgr);

            return presets.keySet();
        } catch (Exception e) {
            return Set.of();
        }
    }

    private boolean hasEffectPreset(String id) {
        try {
            var f = plugin.getQuestManager().getClass().getDeclaredField("effectPresetManager");
            f.setAccessible(true);
            EffectPresetManager mgr = (EffectPresetManager) f.get(plugin.getQuestManager());
            return mgr.hasPreset(id);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean playEffectPreset(Player player, String effectId) {
        try {
            var f = plugin.getQuestManager().getClass().getDeclaredField("effectPresetManager");
            f.setAccessible(true);
            EffectPresetManager mgr = (EffectPresetManager) f.get(plugin.getQuestManager());
            return mgr.playPreset(player, effectId);
        } catch (Exception e) {
            return false;
        }
    }
}
