package ua.woody.questborn.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.effects.EffectPresetManager;
import ua.woody.questborn.gui.editor.QuestEditorListGui;
import ua.woody.questborn.gui.MainMenuGui;
import ua.woody.questborn.gui.QuestDetailsGui;
import ua.woody.questborn.gui.QuestListGui;
import ua.woody.questborn.gui.TopGui;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.storage.PlayerDataStore;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CommandAlias("quest|quests|q")
public class QuestCommands extends BaseCommand {
    private final QuestbornPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public QuestCommands(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    @Default
    @CommandPermission("questborn.use")
    public void defaultCommand(Player player) {
        String mode = plugin.getConfig().getString("integration.npc.mode", plugin.getConfig().getString("integration.mode", "MIXED")).toUpperCase();
        if ("NPC_ONLY".equals(mode)) {
            player.sendMessage(plugin.getLanguage().tr("commands.npc-only"));
            return;
        }
        new MainMenuGui(plugin, player).open();
    }

    @Subcommand("editor")
    @CommandPermission("questborn.command.editor")
    public void editorCommand(Player player) {
        new ua.woody.questborn.gui.editor.MainEditorSelectorGui(plugin, player).open();
    }

    @CatchUnknown
    public void onUnknown(CommandSender sender) {
        sender.sendMessage(plugin.getLanguage().tr("commands.errors.unknown-command"));
    }

    @HelpCommand
    @Syntax("[page]")
    @CommandPermission("questborn.use")
    public void doHelp(CommandSender sender, co.aikar.commands.CommandHelp help) {
        var lang = plugin.getLanguage();
        lang.sendMessage(sender, "commands.help.header");

        java.util.List<co.aikar.commands.HelpEntry> entries = new java.util.ArrayList<>(help.getHelpEntries());
        entries.sort((e1, e2) -> {
            String cmd1 = e1.getCommand();
            String cmd2 = e2.getCommand();
            if (cmd1.endsWith("help")) return -1;
            if (cmd2.endsWith("help")) return 1;
            if (cmd1.endsWith("reload")) return 1;
            if (cmd2.endsWith("reload")) return -1;
            return cmd1.compareToIgnoreCase(cmd2);
        });

        int perPage = help.getPerPage();
        if (perPage <= 0) perPage = 10;
        int total = entries.size();
        int totalPages = (int) Math.ceil((double) total / perPage);
        if (totalPages == 0) totalPages = 1;

        int page = help.getPage();
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, total);

        for (int i = start; i < end; i++) {
            co.aikar.commands.HelpEntry entry = entries.get(i);
            String syntax = entry.getParameterSyntax() != null ? entry.getParameterSyntax() : "";
            String desc = entry.getDescription() != null ? entry.getDescription() : "";

            String permission = "";
            try {
                for (java.lang.reflect.Method m : entry.getClass().getDeclaredMethods()) {
                    if (m.getReturnType().getSimpleName().equals("RegisteredCommand")) {
                        m.setAccessible(true);
                        co.aikar.commands.RegisteredCommand<?> reg = (co.aikar.commands.RegisteredCommand<?>) m.invoke(entry);
                        java.util.Set<String> perms = reg.getRequiredPermissions();
                        if (perms != null && !perms.isEmpty()) {
                            permission = String.join(", ", perms);
                        }
                        break;
                    }
                }
            } catch (Exception e) {}

            String hoverText = permission.isEmpty()
                    ? lang.tr("commands.help.hover-no-perm")
                    : lang.tr("commands.help.hover-with-perm", java.util.Map.of("permission", permission));

            String cmdString = "/" + entry.getCommand();

            net.kyori.adventure.text.Component cmdComp = net.kyori.adventure.text.Component.text(cmdString).color(net.kyori.adventure.text.format.TextColor.color(0xa7ff99));

            net.kyori.adventure.text.Component syntaxComp = net.kyori.adventure.text.Component.empty();
            if (!syntax.isEmpty()) {
                String[] parts = syntax.split(" ");
                for (String part : parts) {
                    if (part.startsWith("<") && part.endsWith(">")) {
                        syntaxComp = syntaxComp.append(net.kyori.adventure.text.Component.text(" " + part).color(net.kyori.adventure.text.format.TextColor.color(0xffd470)));
                    } else if (part.startsWith("[") && part.endsWith("]")) {
                        syntaxComp = syntaxComp.append(net.kyori.adventure.text.Component.text(" " + part).color(net.kyori.adventure.text.format.TextColor.color(0xa3a3a3)));
                    } else {
                        syntaxComp = syntaxComp.append(net.kyori.adventure.text.Component.text(" " + part).color(net.kyori.adventure.text.format.TextColor.color(0xa7ff99)));
                    }
                }
            }

            net.kyori.adventure.text.Component descComp = net.kyori.adventure.text.Component.empty();
            if (!desc.isBlank()) {
                descComp = net.kyori.adventure.text.Component.text(" - " + desc).color(net.kyori.adventure.text.format.TextColor.color(0xffffff));
            }

            net.kyori.adventure.text.Component msgComp = cmdComp.append(syntaxComp).append(descComp);

            net.kyori.adventure.text.Component hoverComp;
            try {
                hoverComp = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(hoverText.replace("\n", "<br>"));
            } catch (Exception e) {
                hoverComp = net.kyori.adventure.text.Component.text(hoverText);
            }

            msgComp = msgComp.clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand(cmdString))
                             .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(hoverComp));

            plugin.getAdventure().sender(sender).sendMessage(msgComp);
        }

        if (totalPages > 1) {
            lang.sendMessage(sender, "commands.help.footer", java.util.Map.of(
                "page", String.valueOf(page),
                "total_pages", String.valueOf(totalPages),
                "total_results", String.valueOf(total)
            ));
        }
    }

    @Subcommand("activate")
    @CommandPermission("questborn.command.activate")
    @CommandCompletion("@quests @players")
    public void activate(CommandSender sender, String questId, @Flags("other") @Optional Player player) {
        var lang = plugin.getLanguage();
        if (player == null) {
            if (sender instanceof Player p) {
                player = p;
            } else {
                lang.sendMessage(sender, "commands.errors.player-not-found", Map.of("player", "null"));
                return;
            }
        }

        QuestDefinition def = plugin.getQuestManager().getById(questId);
        if (def == null) {
            lang.sendMessage(sender, "commands.errors.quest-not-found", Map.of("quest", questId));
            return;
        }

        PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());
        String active = data.getTrackedQuestId();
        if (active != null && !active.equalsIgnoreCase(questId)) {
            data.setTrackedQuestId(null);

            data.clearPendingQuest();
            plugin.getPlayerDataStore().save();
        }

        boolean ok = plugin.getQuestManager().activateQuest(player, questId);
        lang.sendMessage(sender, ok ? "commands.activate.success" : "commands.activate.fail",
                Map.of("quest", questId, "player", player.getName()));
    }

    @Subcommand("deactivate")
    @CommandPermission("questborn.command.deactivate")
    @CommandCompletion("@players")
    public void deactivate(CommandSender sender, @Flags("other") @Optional Player player) {
        var lang = plugin.getLanguage();
        if (player == null) {
            if (sender instanceof Player p) player = p;
            else {
                lang.sendMessage(sender, "commands.errors.player-not-found", Map.of("player", "null"));
                return;
            }
        }

        PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());
        String activeId = data.getTrackedQuestId();
        if (activeId == null) {
            lang.sendMessage(sender, "commands.deactivate.no-active");
            return;
        }

        plugin.getQuestManager().cancelQuest(player, activeId);
        lang.sendMessage(sender, "commands.deactivate.success", Map.of("player", player.getName()));
    }

    @Subcommand("finish stage")
    @CommandPermission("questborn.command.finish")
    @CommandCompletion("@players")
    public void finishStage(CommandSender sender, @Flags("other") @Optional Player player) {
        var lang = plugin.getLanguage();
        if (player == null) {
            if (sender instanceof Player p) player = p;
            else {
                lang.sendMessage(sender, "commands.errors.player-not-found", Map.of("player", "null"));
                return;
            }
        }

        PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());
        String activeId = data.getTrackedQuestId();
        if (activeId == null && !data.getActiveQuests().isEmpty()) {
            activeId = data.getActiveQuests().keySet().iterator().next();
            data.setTrackedQuestId(activeId);
        }

        if (activeId == null) {
            lang.sendMessage(sender, "commands.finish.no-active");
            return;
        }

        boolean finished = plugin.getQuestManager().forceFinishStage(player);
        if (finished) {
            lang.sendMessage(sender, "commands.finish.stage.success", Map.of("player", player.getName()));
        } else {
            lang.sendMessage(sender, "commands.finish.stage.no-stages");
        }
    }

    @Subcommand("finish")
    @CommandPermission("questborn.command.finish")
    @CommandCompletion("@players")
    public void finish(CommandSender sender, @Flags("other") @Optional Player player) {
        var lang = plugin.getLanguage();
        if (player == null) {
            if (sender instanceof Player p) player = p;
            else {
                lang.sendMessage(sender, "commands.errors.player-not-found", Map.of("player", "null"));
                return;
            }
        }

        PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());
        String activeId = data.getTrackedQuestId();
        if (activeId == null && !data.getActiveQuests().isEmpty()) {
            activeId = data.getActiveQuests().keySet().iterator().next();
        }

        if (activeId == null) {
            lang.sendMessage(sender, "commands.finish.no-active");
            return;
        }

        QuestDefinition q = plugin.getQuestManager().getById(activeId);
        plugin.getQuestManager().complete(player, q);
        lang.sendMessage(sender, "commands.finish.success", Map.of("player", player.getName()));
    }

    @Subcommand("finish global")
    @CommandPermission("questborn.command.finish")
    @CommandCompletion("@activeGlobalQuests @nothing")
    public void finishGlobal(CommandSender sender, String questId) {
        var lang = plugin.getLanguage();
        QuestDefinition questDef = plugin.getQuestManager().getById(questId);
        if (questDef == null) {
            lang.sendMessage(sender, "commands.errors.quest-not-found", Map.of("quest", questId));
            return;
        }

        ua.woody.questborn.model.QuestTypeConfig typeC = plugin.getQuestManager().getQuestTypeManager().getType(questDef.getTypeId());
        if (typeC == null || typeC.getEngine() != ua.woody.questborn.model.EngineType.GLOBAL) {
            lang.sendMessage(sender, "commands.finish-global.not-global", Map.of("quest", questId));
            return;
        }

        ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(questId);
        if (gp.isCompleted() || gp.getParticipantCount() == 0) {
            lang.sendMessage(sender, "commands.finish-global.not-active", Map.of("quest", questId));
            return;
        }

        plugin.getQuestManager().forceCompleteGlobalQuest(questDef);
        lang.sendMessage(sender, "commands.finish-global.success", Map.of("quest", questId));
    }

    @Subcommand("open details")
    @CommandPermission("questborn.command.open")
    @CommandCompletion("@quests @players --simple")
    public void openDetails(CommandSender sender, String questId, @Flags("other") @Optional Player player, @Optional String flag) {
        boolean simpleView = "--simple".equalsIgnoreCase(flag);
        var lang = plugin.getLanguage();
        QuestDefinition def = plugin.getQuestManager().getById(questId);
        if (def == null) {
            lang.sendMessage(sender, "commands.errors.quest-not-found", Map.of("quest", questId));
            return;
        }

        if (player == null) {
            if (sender instanceof Player p) player = p;
            else {
                sender.sendMessage(plugin.getLanguage().tr("commands.errors.console-specify-player"));
                return;
            }
        }

        new QuestDetailsGui(plugin, player, def, simpleView).open();
        sendOpenedMessage(sender, player, "details:" + questId);
    }

    @Subcommand("open list")
    @CommandPermission("questborn.command.open")
    @CommandCompletion("@questTypes @players --simple")
    public void openList(CommandSender sender, String typeId, @Flags("other") @Optional Player player, @Optional String flag) {
        boolean simpleView = "--simple".equalsIgnoreCase(flag);
        var lang = plugin.getLanguage();
        QuestTypeConfig typeConfig = plugin.getQuestManager().getQuestTypeManager().getType(typeId);
        if (typeConfig == null) {
            lang.sendMessage(sender, "commands.errors.type-not-found", Map.of("type", typeId));
            return;
        }

        if (player == null) {
            if (sender instanceof Player p) player = p;
            else {
                sender.sendMessage(plugin.getLanguage().tr("commands.errors.console-specify-player"));
                return;
            }
        }

        new QuestListGui(plugin, player, typeConfig, 0, simpleView).open();
        sendOpenedMessage(sender, player, "list:" + typeId);
    }

    @Subcommand("open main")
    @co.aikar.commands.annotation.Private
    @CommandPermission("questborn.command.open")
    @CommandCompletion("@players")
    public void openMain(CommandSender sender, @Flags("other") @Optional Player player) {
        String mode = plugin.getConfig().getString("integration.npc.mode", plugin.getConfig().getString("integration.mode", "MIXED")).toUpperCase();
        if ("NPC_ONLY".equals(mode) && sender instanceof Player) {
            sender.sendMessage(plugin.getLanguage().tr("commands.npc-only"));
            return;
        }
        if (player == null) {
            if (sender instanceof Player p) player = p;
            else {
                sender.sendMessage(plugin.getLanguage().tr("commands.errors.console-specify-player"));
                return;
            }
        }
        new MainMenuGui(plugin, player).open();
        sendOpenedMessage(sender, player, "main");
    }

    @Subcommand("open menu")
    @CommandPermission("questborn.command.open")
    @CommandCompletion("@players")
    public void openMenu(CommandSender sender, @Flags("other") @Optional Player player) {
        openMain(sender, player);
    }

    @Subcommand("open top")
    @CommandPermission("questborn.command.open")
    @CommandCompletion("@players --simple")
    public void openTop(CommandSender sender, @Flags("other") @Optional Player player, @Optional String flag) {
        openTopImpl(sender, player, flag);
    }

    @Subcommand("top")
    @CommandPermission("questborn.command.open")
    @CommandCompletion("@players --simple")
    public void top(CommandSender sender, @Flags("other") @Optional Player player, @Optional String flag) {
        openTopImpl(sender, player, flag);
    }

    private void openTopImpl(CommandSender sender, Player player, String flag) {
        boolean simpleView = "--simple".equalsIgnoreCase(flag);
        String mode = plugin.getConfig().getString("integration.npc.mode", plugin.getConfig().getString("integration.mode", "MIXED")).toUpperCase();
        if ("NPC_ONLY".equals(mode)) {
            simpleView = true;
        }
        if (player == null) {
            if (sender instanceof Player p) player = p;
            else {
                sender.sendMessage(plugin.getLanguage().tr("commands.errors.console-specify-player"));
                return;
            }
        }
        new TopGui(plugin, player, 0, simpleView).open();
        sendOpenedMessage(sender, player, "top");
    }

    private void sendOpenedMessage(CommandSender sender, Player player, String menuName) {
        if (sender instanceof Player p && p.getUniqueId().equals(player.getUniqueId())) return;
        sender.sendMessage(plugin.getLanguage().tr("commands.open.opened", Map.of("menu", menuName, "player", player.getName())));
    }

    @Subcommand("internal dialogue")
    @co.aikar.commands.annotation.Private
    @CommandPermission("questborn.use")
    public void internalDialogue(Player player, String dialogueId, String action, String npcIdStr) {
        String npcId = "none".equals(npcIdStr) ? null : npcIdStr;

        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);

        if (action.equals("close")) {
            return;
        } else if (action.equals("open_quest_menu")) {
            if (npcId != null) {
                plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> plugin.getNpcIntegrationManager().handleNpcClick(player, npcId, null, true));
            } else {
                plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> new ua.woody.questborn.gui.MainMenuGui(plugin, player).open());
            }
        } else if (action.startsWith("give_quest:")) {
            String qId = action.substring(11);
            plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> plugin.getQuestManager().activateQuest(player, qId));
        } else if (action.startsWith("run_command:")) {
            String cmd = action.substring(12);
            plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> player.performCommand(cmd));
        } else if (action.startsWith("goto:")) {
            String nodeId = action.substring(5);
            ua.woody.questborn.model.Dialogue diag = plugin.getDialogueManager().getDialogues().get(dialogueId);
            if (diag != null) {
                plugin.getDialogueManager().runNode(player, diag, nodeId, npcId);
            }
        }
    }

    @Subcommand("details")
    @CommandPermission("questborn.command.details")
    public void detailsCommand(Player player) {
        var lang = plugin.getLanguage();
        if ("NPC_ONLY".equals(plugin.getConfig().getString("integration.npc.mode", plugin.getConfig().getString("integration.mode", "MIXED")).toUpperCase())) {
            lang.sendMessage(player, "commands.npc-only");
            return;
        }

        PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());
        if (data == null || data.getTrackedQuestId() == null) {
            lang.sendMessage(player, "commands.details.no-active-quest");
            return;
        }

        QuestDefinition questDef = plugin.getQuestManager().getById(data.getTrackedQuestId());
        if (questDef == null) {
            lang.sendMessage(player, "commands.errors.quest-not-found", Map.of("quest", data.getTrackedQuestId()));
            return;
        }
        new QuestDetailsGui(plugin, player, questDef, true).open();
    }

    @Subcommand("reload")
    @CommandPermission("questborn.command.reload")
    public void reload(CommandSender sender) {
        plugin.reloadAll();
        sender.sendMessage(plugin.getLanguage().tr("commands.reload.done"));
    }

    @Subcommand("reset rotation")
    @CommandPermission("questborn.command.reset")
    @CommandCompletion("@resetQuestTypes @players")
    public void resetRotation(CommandSender sender, String typeId, @Flags("other") @Optional Player player) {
        var lang = plugin.getLanguage();
        if (player == null) {
            if (sender instanceof Player p) player = p;
            else {
                lang.sendMessage(sender, "commands.errors.player-not-found", Map.of("player", "null"));
                return;
            }
        }

        var typeConfig = plugin.getQuestManager().getQuestTypeManager().getType(typeId);
        if (typeConfig == null) {
            lang.sendMessage(sender, "commands.errors.type-not-found", Map.of("type", typeId));
            return;
        }

        if (typeConfig.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL) {
             throw new co.aikar.commands.InvalidCommandArgument(true);
        }

        PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());
        if (typeConfig != null) {
            for (var q : plugin.getQuestManager().getByType(typeConfig)) {
                data.resetHistory(q.getId());
                data.resetCooldown(q.getId());
                data.resetReward(q.getId());
            }
        }

        data.setRotationAssignedAt(typeId, 0L);
        plugin.getQuestManager().getOrAssignRotationQuests(player, typeConfig);

        QuestTypeConfig.RotationAnnounce ann = typeConfig.getRotationAnnounce();
        if (ann != null && ann.isEnabled()) {
            if (ann.getSound() != null) {
                player.playSound(player.getLocation(), ann.getSound(), ann.getVolume(), ann.getPitch());
            }
            for (String line : ann.getChat()) {
                player.sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors(line));
            }
        }

        plugin.getPlayerDataStore().save();
        lang.sendMessage(sender, "commands.reset.rotation-success", Map.of("type", typeId, "player", player.getName()));
    }

    @Subcommand("reset top")
    @CommandPermission("questborn.command.reset")
    @CommandCompletion("@resetQuestTypes @playersOrAll")
    public void resetTop(CommandSender sender, String typeId, @Optional String target) {
        var lang = plugin.getLanguage();
        var typeConfig = plugin.getQuestManager().getQuestTypeManager().getType(typeId);
        if (typeConfig == null) {
            lang.sendMessage(sender, "commands.errors.type-not-found", Map.of("type", typeId));
            return;
        }

        if (target == null || target.equalsIgnoreCase("-all")) {
            lang.sendMessage(sender, "commands.reset.top-start", java.util.Map.of("type", typeId));
            ua.woody.questborn.utils.SchedulerUtils.runTaskAsynchronously(plugin, () -> {
                int count = 0;
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    PlayerQuestProgress data = plugin.getPlayerDataStore().get(p.getUniqueId());
                    if (data.getCompletedByType().containsKey(typeId.toLowerCase(java.util.Locale.ROOT))) {
                        data.resetCompleted(typeId);
                        plugin.getPlayerDataStore().markDirty(p.getUniqueId());
                        count++;
                    }
                }
                plugin.getPlayerDataStore().save();

                try {
                    java.util.List<ua.woody.questborn.model.TopEntry> entries = plugin.getPlayerDataStore().getStorageProvider()
                            .getTopPlayers(Integer.MAX_VALUE, Set.of(typeId)).get();
                    for (ua.woody.questborn.model.TopEntry entry : entries) {
                        java.util.UUID uuid = entry.getUuid();
                        if (plugin.getServer().getPlayer(uuid) != null) continue;

                        PlayerQuestProgress data = plugin.getPlayerDataStore().getStorageProvider().loadPlayer(uuid).get();
                        if (data.getCompletedByType().containsKey(typeId.toLowerCase(java.util.Locale.ROOT))) {
                            data.resetCompleted(typeId);
                            plugin.getPlayerDataStore().getStorageProvider().savePlayer(uuid, data).get();
                            count++;
                        }
                    }
                    lang.sendMessage(sender, "commands.reset.top-success-all", java.util.Map.of("count", String.valueOf(count)));
                } catch (Exception e) {
                    lang.sendMessage(sender, "commands.errors.top-reset-error", java.util.Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown"));
                    e.printStackTrace();
                }
            });
            return;
        }

        Player player = Bukkit.getPlayer(target);
        if (player == null) {
            lang.sendMessage(sender, "commands.errors.player-not-found", java.util.Map.of("player", target));
            return;
        }

        PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());
        data.resetCompleted(typeId);
        plugin.getPlayerDataStore().save();
        lang.sendMessage(sender, "commands.reset.top-success", java.util.Map.of("type", typeId, "player", player.getName()));
    }

    @Subcommand("reset type")
    @CommandPermission("questborn.command.reset")
    @CommandCompletion("@resetQuestTypes @players")
    public void resetType(CommandSender sender, String typeId, @Flags("other") @Optional Player player) {
        var lang = plugin.getLanguage();
        if (player == null) {
            if (sender instanceof Player p) player = p;
            else {
                lang.sendMessage(sender, "commands.errors.player-not-found", Map.of("player", "null"));
                return;
            }
        }

        var typeConfig = plugin.getQuestManager().getQuestTypeManager().getType(typeId);
        if (typeConfig == null) {
            lang.sendMessage(sender, "commands.errors.type-not-found", Map.of("type", typeId));
            return;
        }
        if (typeConfig.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL) {
             throw new co.aikar.commands.InvalidCommandArgument(true);
        }

        PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());
        String activeId = data.getTrackedQuestId();
        if (activeId != null) {
            QuestDefinition q = plugin.getQuestManager().getById(activeId);
            if (q != null && q.getTypeId().equalsIgnoreCase(typeId)) {
                data.removeActiveQuest(data.getTrackedQuestId());
            }
        }

        if (typeConfig != null) {
            for (var q : plugin.getQuestManager().getByType(typeConfig)) {
                data.resetHistory(q.getId());
                data.resetCooldown(q.getId());
                data.resetReward(q.getId());
            }
        }
        plugin.getPlayerDataStore().save();
        lang.sendMessage(sender, "commands.reset.type-success", Map.of("type", typeId, "player", player.getName()));
    }

    @Subcommand("reset cooldown")
    @CommandPermission("questborn.command.reset")
    @CommandCompletion("@resetQuestTypes @questsOfType @players")
    public void resetCooldown(CommandSender sender, String typeId, String questId, @Flags("other") @Optional Player player) {
        handleTargetedReset(sender, typeId, questId, player, "cooldown");
    }

    @Subcommand("reset history")
    @CommandPermission("questborn.command.reset")
    @CommandCompletion("@resetQuestTypes @questsOfType @players")
    public void resetHistory(CommandSender sender, String typeId, String questId, @Flags("other") @Optional Player player) {
        handleTargetedReset(sender, typeId, questId, player, "history");
    }

    @Subcommand("reset reward")
    @CommandPermission("questborn.command.reset")
    @CommandCompletion("@resetQuestTypes @questsOfType @players")
    public void resetReward(CommandSender sender, String typeId, String questId, @Flags("other") @Optional Player player) {
        handleTargetedReset(sender, typeId, questId, player, "reward");
    }

    @Subcommand("reset global")
    @CommandPermission("questborn.command.reset")
    @CommandCompletion("@completedGlobalQuests @nothing")
    public void resetGlobal(CommandSender sender, String questId) {
        var lang = plugin.getLanguage();
        QuestDefinition questDef = plugin.getQuestManager().getById(questId);
        if (questDef == null) {
             lang.sendMessage(sender, "commands.errors.quest-not-found", Map.of("quest", questId));
             return;
        }

        var typeConfig = plugin.getQuestManager().getQuestTypeManager().getType(questDef.getTypeId());
        if (typeConfig == null || typeConfig.getEngine() != ua.woody.questborn.model.EngineType.GLOBAL) {
             lang.sendMessage(sender, "commands.reset-global.not-global", Map.of("quest", questId));
             return;
        }

        ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(questId);
        if (gp != null) {
             gp.setCompleted(false);
             gp.getContributions().clear();
             gp.setGlobalProgress(0);
             gp.setStartedAt(0L);
             plugin.getGlobalQuestDataStore().save(questId);
        }

        for (Player p : plugin.getServer().getOnlinePlayers()) {
             PlayerQuestProgress data = plugin.getPlayerDataStore().get(p.getUniqueId());
             if (questId.equalsIgnoreCase(data.getTrackedQuestId())) data.removeActiveQuest(questId);
             data.resetCooldown(questId);
             data.resetHistory(questId);
             data.resetReward(questId);
        }
        plugin.getPlayerDataStore().save();

        lang.sendMessage(sender, "commands.reset-global.success", Map.of("quest", questId));
    }

    @Subcommand("reset quest")
    @CommandPermission("questborn.command.reset")
    @CommandCompletion("@resetQuestTypes @questsOfType @players")
    public void resetQuest(CommandSender sender, String typeId, String questId, @Flags("other") @Optional Player player) {
        handleTargetedReset(sender, typeId, questId, player, "quest");
    }

    private void handleTargetedReset(CommandSender sender, String typeId, String questId, Player player, String action) {
        var lang = plugin.getLanguage();
        if (player == null) {
            if (sender instanceof Player p) player = p;
            else {
                lang.sendMessage(sender, "commands.errors.player-not-found", Map.of("player", "null"));
                return;
            }
        }

        PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());
        QuestDefinition questDef = plugin.getQuestManager().getById(questId);
        if (questDef == null) {
             lang.sendMessage(sender, "commands.errors.quest-not-found", Map.of("quest", questId));
             return;
        }
        if (!questDef.getTypeId().equalsIgnoreCase(typeId)) {
            lang.sendMessage(sender, "commands.errors.quest-wrong-type", Map.of("quest", questId, "type", typeId));
            return;
        }

        switch (action) {
            case "cooldown" -> {
                data.resetCooldown(questId);
                lang.sendMessage(sender, "commands.reset.cooldown-success", Map.of("quest", questId, "player", player.getName()));
            }
            case "history" -> {
                data.resetHistory(questId);
                lang.sendMessage(sender, "commands.reset.history-success", Map.of("quest", questId, "player", player.getName()));
            }
            case "reward" -> {
                data.resetReward(questId);
                lang.sendMessage(sender, "commands.reset.reward-success", Map.of("quest", questId, "player", player.getName()));
            }
            case "quest" -> {
                if (questId.equalsIgnoreCase(data.getTrackedQuestId())) data.removeActiveQuest(data.getTrackedQuestId());
                data.resetCooldown(questId);
                data.resetHistory(questId);
                data.resetReward(questId);

                ua.woody.questborn.model.QuestTypeConfig typeC = plugin.getQuestManager().getQuestTypeManager().getType(questDef.getTypeId());
                if (typeC != null && typeC.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL) {
                    plugin.getQuestManager().getStateService().notifyGlobalQuestLeave(player, questDef);
                    ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(questId);
                    if (gp != null) {
                        int contribution = gp.getContribution(player.getUniqueId());
                        if (contribution > 0) {
                            gp.addGlobalProgress(-contribution);
                            if (gp.getGlobalProgress() < plugin.getQuestManager().getProgressProcessor().getGlobalGoal(questDef)) {
                                gp.setCompleted(false);
                            }
                        }
                        gp.getContributions().remove(player.getUniqueId());
                        plugin.getGlobalQuestDataStore().save(questId);
                    }
                }

                lang.sendMessage(sender, "commands.reset.quest-success", Map.of("quest", questId, "player", player.getName()));
            }
        }
        plugin.getPlayerDataStore().save();
    }

    @Subcommand("npc indicator")
    @CommandPermission("questborn.command.npc")
    @CommandCompletion("@npcIds @npcIndicatorOffsets")
    public void npcIndicator(CommandSender sender, String npcId, double offset) {
        var lang = plugin.getLanguage();
        ua.woody.questborn.model.NpcConfig npc = plugin.getNpcManager().getOrCreateConfig(npcId);
        npc.setIndicatorHeightOffset(offset);
        plugin.getNpcManager().saveNpcsAsync();
        plugin.getNpcIndicatorManager().reload();
        lang.sendMessage(sender, "commands.npc.indicator-set", java.util.Map.of("npc", npcId, "offset", String.valueOf(offset)));
    }

    @Subcommand("npc voice")
    @CommandPermission("questborn.command.npc")
    @CommandCompletion("@npcIds @voices @nothing")
    public void npcVoice(CommandSender sender, String npcId, String voice) {
        if (!isNpcEnabled()) return;
        var lang = plugin.getLanguage();
        npcId = resolveNpcId(sender, npcId);
        if (npcId == null) return;

        ua.woody.questborn.model.NpcConfig npc = plugin.getNpcManager().getOrCreateConfig(npcId);

        if (voice.equalsIgnoreCase("none") || voice.equalsIgnoreCase("remove") || voice.equalsIgnoreCase("clear") || voice.equalsIgnoreCase("null") || voice.equalsIgnoreCase("reset")) {
            npc.setVoice(null);
            plugin.getNpcManager().saveNpcsAsync();
            lang.sendMessage(sender, "commands.npc.voice-removed", java.util.Map.of("npc", npcId));
        } else {
            npc.setVoice(voice);
            plugin.getNpcManager().saveNpcsAsync();
            lang.sendMessage(sender, "commands.npc.voice-set", java.util.Map.of("npc", npcId, "voice", voice));
        }
    }

    @Subcommand("npc dialogue")
    @CommandPermission("questborn.command.npc")
    @CommandCompletion("@npcIds @dialogues @nothing")
    public void npcDialogue(CommandSender sender, String npcId, String dialogueId) {
        if (!isNpcEnabled()) return;
        var lang = plugin.getLanguage();
        npcId = resolveNpcId(sender, npcId);
        if (npcId == null) return;

        ua.woody.questborn.model.NpcConfig npc = plugin.getNpcManager().getOrCreateConfig(npcId);

        if (dialogueId.equalsIgnoreCase("none") || dialogueId.equalsIgnoreCase("remove") || dialogueId.equalsIgnoreCase("clear") || dialogueId.equalsIgnoreCase("null") || dialogueId.equalsIgnoreCase("reset")) {
            npc.setDialogueId(null);
            plugin.getNpcManager().saveNpcsAsync();
            lang.sendMessage(sender, "commands.npc.dialogue-removed", java.util.Map.of("npc", npcId));
            return;
        }

        if (!plugin.getDialogueManager().getDialogues().containsKey(dialogueId)) {
            lang.sendMessage(sender, "commands.npc.dialogue-not-found", java.util.Map.of("dialogue", dialogueId));
            return;
        }

        npc.setDialogueId(dialogueId);
        plugin.getNpcManager().saveNpcsAsync();
        lang.sendMessage(sender, "commands.npc.dialogue-set", java.util.Map.of("npc", npcId, "dialogue", dialogueId));
    }

    @Subcommand("npc link")
    @CommandPermission("questborn.command.npc")
    @CommandCompletion("quest|type start|finish @questsOrTypes @nothing")
    public void npcLink(CommandSender sender, String category, String linkType, String targetId) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguage().tr("commands.npc.console-must-specify-id"));
            return;
        }

        boolean isQuest = category.equalsIgnoreCase("quest");
        boolean isStart = linkType.equalsIgnoreCase("start");

        plugin.getNpcManager().setPendingAction(player.getUniqueId(), new ua.woody.questborn.managers.NpcManager.PendingNpcAction(isStart, isQuest, targetId, true));
        plugin.getLanguage().sendMessage(sender, "commands.npc.link-pending");
    }

    @Subcommand("npc unlink")
    @CommandPermission("questborn.command.npc")
    @CommandCompletion("quest|type start|finish @questsOrTypes @nothing")
    public void npcUnlink(CommandSender sender, String category, String linkType, String targetId) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguage().tr("commands.npc.console-must-specify-id"));
            return;
        }

        boolean isQuest = category.equalsIgnoreCase("quest");
        boolean isStart = linkType.equalsIgnoreCase("start");

        plugin.getNpcManager().setPendingAction(player.getUniqueId(), new ua.woody.questborn.managers.NpcManager.PendingNpcAction(isStart, isQuest, targetId, false));
        plugin.getLanguage().sendMessage(sender, "commands.npc.unlink-pending");
    }

    @Subcommand("npc list")
    @CommandPermission("questborn.command.npc")
    public void npcList(CommandSender sender) {
        if (!isNpcEnabled()) return;
        var lang = plugin.getLanguage();
        var all = plugin.getNpcManager().getAll();
        if (all.isEmpty()) {
            lang.sendMessage(sender, "commands.npc.list-empty");
            return;
        }
        lang.sendMessage(sender, "commands.npc.list-header");
        var provider = plugin.getNpcIntegrationManager().getProvider();
        for (var npc : all) {
            String npcId = npc.getNpcId();
            String npcName = plugin.getNpcManager().getDisplayName(npcId);
            String display = npcId.equals(npcName) ? npcId : npcId + " &7(" + npcName + "&7)";
            lang.sendMessage(sender, "commands.npc.list-item", java.util.Map.of("npc", display));
        }
    }

    @Subcommand("npc info")
    @CommandPermission("questborn.command.npc")
    @CommandCompletion("@npcIds")
    public void npcInfo(CommandSender sender, String npcId) {
        if (!isNpcEnabled()) return;
        var lang = plugin.getLanguage();
        npcId = resolveNpcId(sender, npcId);
        if (npcId == null) return;
        var npc = plugin.getNpcManager().getConfigByNpcId(npcId);
        if (npc == null) {
            lang.sendMessage(sender, "commands.npc.not-found", java.util.Map.of("npc", npcId));
            return;
        }
        var provider = plugin.getNpcIntegrationManager().getProvider();
        String npcName = plugin.getNpcManager().getDisplayName(npcId);
        String strippedNpcName = ua.woody.questborn.lang.ColorFormatter.stripColors(npcName);
        String title = npc.getGuiTitle() != null ? npc.getGuiTitle().replace("{npc}", strippedNpcName) : "none";

        lang.sendMessage(sender, "commands.npc.info.header", java.util.Map.of("npc", npcId));
        lang.sendMessage(sender, "commands.npc.info.title", java.util.Map.of("title", title));
        lang.sendMessage(sender, "commands.npc.info.dialogue", java.util.Map.of("dialogue", npc.getDialogueId() != null ? npc.getDialogueId() : "none"));
        lang.sendMessage(sender, "commands.npc.info.indicator", java.util.Map.of("offset", npc.getIndicatorHeightOffset() != null ? npc.getIndicatorHeightOffset().toString() : "0.0"));

        sender.sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors("&7Starts Quests: &f" + String.join(", ", npc.getStartsQuests())));
        sender.sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors("&7Finishes Quests: &f" + String.join(", ", npc.getFinishesQuests())));
        sender.sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors("&7Starts Types: &f" + String.join(", ", npc.getStartsTypes())));
        sender.sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors("&7Finishes Types: &f" + String.join(", ", npc.getFinishesTypes())));
    }

    @Subcommand("npc displayname")
    @CommandPermission("questborn.command.npc")
    @CommandCompletion("@npcIds @nothing")
    public void npcDisplayName(CommandSender sender, String npcId, String name) {
        if (!isNpcEnabled()) return;
        var lang = plugin.getLanguage();
        String resolvedNpcId = resolveNpcId(sender, npcId.equalsIgnoreCase("@target") ? null : npcId);
        if (resolvedNpcId == null) return;
        var npc = plugin.getNpcManager().getOrCreateConfig(resolvedNpcId);
        if (name.equalsIgnoreCase("@nothing") || name.equalsIgnoreCase("none") || name.equalsIgnoreCase("null")) {
            npc.setDisplayName(null);
            lang.sendMessage(sender, "commands.npc.displayname-removed", java.util.Map.of("npc", npcId));
        } else {
            npc.setDisplayName(name);
            lang.sendMessage(sender, "commands.npc.displayname-set", java.util.Map.of("npc", npcId, "name", name));
        }
        plugin.getNpcManager().saveNpcsAsync();
        plugin.getNpcIndicatorManager().reload();
    }

    @Subcommand("npc gui-title")
    @CommandPermission("questborn.command.npc")
    @CommandCompletion("@npcIds @nothing")
    public void npcGuiTitle(CommandSender sender, String npcId, String title) {
        if (!isNpcEnabled()) return;
        var lang = plugin.getLanguage();
        npcId = resolveNpcId(sender, npcId);
        if (npcId == null) return;
        var npc = plugin.getNpcManager().getOrCreateConfig(npcId);
        npc.setGuiTitle(title);
        plugin.getNpcManager().saveNpcsAsync();
        lang.sendMessage(sender, "commands.npc.gui-title-set", java.util.Map.of("npc", npcId, "title", title));
    }

    @Subcommand("npc remove")
    @CommandPermission("questborn.command.npc")
    @CommandCompletion("@npcIds")
    public void npcRemove(CommandSender sender, String npcId) {
        if (!isNpcEnabled()) return;
        var lang = plugin.getLanguage();
        npcId = resolveNpcId(sender, npcId);
        if (npcId == null) return;
        if (plugin.getNpcManager().getConfigByNpcId(npcId) == null) {
             lang.sendMessage(sender, "commands.npc.not-found", java.util.Map.of("npc", npcId));
             return;
        }
        plugin.getNpcManager().removeNpc(npcId);
        plugin.getNpcIndicatorManager().reload();
        lang.sendMessage(sender, "commands.npc.remove-success", java.util.Map.of("npc", npcId));
    }

    private boolean isNpcEnabled() {
        return plugin.getConfig().getBoolean("integration.npc.enabled", plugin.getConfig().getBoolean("integration.enabled", true));
    }

    private String resolveNpcId(CommandSender sender, String npcId) {
        if (npcId != null && !npcId.isEmpty()) return npcId;
        if (sender instanceof Player player) {
            var npcManager = plugin.getNpcIntegrationManager();
            if (npcManager != null && npcManager.getProvider() != null) {
                String targetId = npcManager.getProvider().getTargetNpcId(player, 5);
                if (targetId != null) return targetId;
            }
        } else {
            sender.sendMessage(plugin.getLanguage().tr("commands.npc.console-must-specify-id"));
            return null;
        }
        sender.sendMessage(plugin.getLanguage().tr("commands.npc.not-looking-at-npc"));
        return null;
    }

    @Subcommand("npc")
    @CommandPermission("questborn.command.npc")
    public void npcHelp(CommandSender sender) {
        var lang = plugin.getLanguage();
        lang.sendMessage(sender, "commands.npc.usage-header");
        lang.sendMessage(sender, "commands.npc.help.dialogue");
        lang.sendMessage(sender, "commands.npc.help.displayname");
        lang.sendMessage(sender, "commands.npc.help.gui-title");
        lang.sendMessage(sender, "commands.npc.help.indicator");
        lang.sendMessage(sender, "commands.npc.help.info");
        lang.sendMessage(sender, "commands.npc.help.link");
        lang.sendMessage(sender, "commands.npc.help.remove");
        lang.sendMessage(sender, "commands.npc.help.unlink");
        lang.sendMessage(sender, "commands.npc.help.voice");
    }

    @Subcommand("global")
    @CommandPermission("questborn.command.global")
    public void globalHelp(CommandSender sender) {
        var lang = plugin.getLanguage();
        lang.sendMessage(sender, "commands.global.usage-header");
        lang.sendMessage(sender, "commands.global.help.finish");
        lang.sendMessage(sender, "commands.global.help.reset");
        lang.sendMessage(sender, "commands.global.help.top");
    }

    @Subcommand("effect list")
    @CommandPermission("questborn.command.effect")
    @Syntax("[page]")
    public void effectList(CommandSender sender, @Optional Integer pageParam) {
        var lang = plugin.getLanguage();
        Set<String> idsSet = getEffectPresetIds();
        if (idsSet.isEmpty()) {
            lang.sendMessage(sender, "commands.effect.no-effects");
            return;
        }

        List<String> ids = idsSet.stream().sorted().toList();
        int total = ids.size();
        int perPage = 10;
        int totalPages = (int) Math.ceil((double) total / perPage);
        if (totalPages < 1) totalPages = 1;

        int page = pageParam != null ? pageParam : 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, total);

        lang.sendMessage(sender, "commands.effect.list-header");
        for (int i = start; i < end; i++) {
            sender.sendMessage(lang.color(String.format("  <#55ff55>%d. <#ffffff>%s", i + 1, ids.get(i))));
        }
        if (totalPages > 1) {
            lang.sendMessage(sender, "commands.effect.list-footer", Map.of(
                "page", String.valueOf(page),
                "total_pages", String.valueOf(totalPages),
                "total_results", String.valueOf(total)
            ));
        }
    }

    @Subcommand("effect")
    @CommandPermission("questborn.command.effect")
    @CommandCompletion("@effectPresets @players")
    @Syntax("[effect|list] [player|page]")
    public void effect(CommandSender sender, @Optional String effectId, @Flags("other") @Optional String secondArg) {
        var lang = plugin.getLanguage();

        if (effectId == null || effectId.isEmpty() || effectId.equalsIgnoreCase("list")) {
            Integer page = null;
            if (secondArg != null) {
                try { page = Integer.parseInt(secondArg); } catch (NumberFormatException ignored) {}
            }
            effectList(sender, page);
            return;
        }

        try {
            int pageNum = Integer.parseInt(effectId);
            effectList(sender, pageNum);
            return;
        } catch (NumberFormatException ignored) {}

        Player player = null;
        if (secondArg != null && !secondArg.isEmpty()) {
            player = Bukkit.getPlayerExact(secondArg);
            if (player == null) player = Bukkit.getPlayer(secondArg);
            if (player == null) {
                lang.sendMessage(sender, "commands.errors.player-not-found", Map.of("player", secondArg));
                return;
            }
        } else {
            if (sender instanceof Player p) player = p;
            else {
                lang.sendMessage(sender, "commands.errors.console-specify-player");
                return;
            }
        }

        if (!hasEffectPreset(effectId) || !playEffectPreset(player, effectId)) {
            lang.sendMessage(sender, "commands.effect.not-found", Map.of("effect", effectId));
            return;
        }

        if (sender.equals(player)) {
            lang.sendMessage(sender, "commands.effect.success-self", Map.of("effect", effectId));
        } else {
            lang.sendMessage(sender, "commands.effect.success-other", Map.of("effect", effectId, "player", player.getName()));
            lang.sendMessage(player, "commands.effect.received", Map.of("effect", effectId));
        }
    }

    private Set<String> getEffectPresetIds() {
        EffectPresetManager mgr = plugin.getQuestManager().getEffectPresetManager();
        return mgr != null ? mgr.getAllPresetIds() : Set.of();
    }

    private boolean hasEffectPreset(String id) {
        EffectPresetManager mgr = plugin.getQuestManager().getEffectPresetManager();
        return mgr != null && mgr.hasPreset(id);
    }

    private boolean playEffectPreset(Player player, String effectId) {
        EffectPresetManager mgr = plugin.getQuestManager().getEffectPresetManager();
        return mgr != null && mgr.playPreset(player, effectId);
    }
}
