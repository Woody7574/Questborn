package ua.woody.questborn.bootstrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.Optional;

import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.commands.QuestCommands;
import ua.woody.questborn.config.GuiConfig;
import ua.woody.questborn.config.MenuConfig;
import ua.woody.questborn.config.TopConfig;
import ua.woody.questborn.integration.ItemsAdderIntegration;
import ua.woody.questborn.integration.QuestbornExpansion;
import ua.woody.questborn.integration.skin.SkinsRestorerProvider;
import ua.woody.questborn.lang.LanguageManager;
import ua.woody.questborn.listeners.CommandListener;
import ua.woody.questborn.listeners.FireworkDamageListener;
import ua.woody.questborn.listeners.PaperTradeListener;
import ua.woody.questborn.listeners.PlayerDataListener;
import ua.woody.questborn.listeners.QuestProgressListener;
import ua.woody.questborn.listeners.SkinUpdateListener;
import ua.woody.questborn.listeners.categories.BlockEventListener;
import ua.woody.questborn.listeners.categories.EntityEventListener;
import ua.woody.questborn.listeners.categories.InteractionEventListener;
import ua.woody.questborn.listeners.categories.ItemEventListener;
import ua.woody.questborn.listeners.categories.MovementEventListener;
import ua.woody.questborn.listeners.categories.PlayerStateEventListener;
import ua.woody.questborn.managers.DialogueManager;
import ua.woody.questborn.managers.MoodManager;
import ua.woody.questborn.managers.NpcIndicatorManager;
import ua.woody.questborn.managers.NpcManager;
import ua.woody.questborn.managers.QuestManager;
import ua.woody.questborn.model.EngineType;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.rewards.RewardHandler;
import ua.woody.questborn.storage.GlobalQuestDataStore;
import ua.woody.questborn.storage.PlayerDataStore;
import ua.woody.questborn.util.QuestDisplayBuilder;
import ua.woody.questborn.util.TimeFormatter;

public class PluginBootstrap {
    private final QuestbornPlugin plugin;

    public PluginBootstrap(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        long startTime = System.currentTimeMillis();
        final String version = Objects.requireNonNullElse(plugin.getDescription().getVersion(), "dev");
        final String serverType = plugin.getServerType();

        plugin.getLogger().info("");
        plugin.getLogger().info(QuestbornPlugin.GOLD + QuestbornPlugin.BOLD + "    Questborn" + QuestbornPlugin.WHITE + QuestbornPlugin.BOLD + " v" + version + QuestbornPlugin.RESET);
        plugin.getLogger().info(QuestbornPlugin.DARK_GRAY + "    Running on " + serverType + " - " + plugin.getServer().getVersion() + QuestbornPlugin.RESET);
        plugin.getLogger().info("");

        plugin.getLogger().info(QuestbornPlugin.LIGHT_BLUE + "Preparing files..." + QuestbornPlugin.RESET);
        plugin.installBundledResourcesOnFirstInstall();
        plugin.ensureRootConfigsResources();
        plugin.ensureLanguageResources();

        plugin.getLogger().info(QuestbornPlugin.LIGHT_BLUE + "Loading config..." + QuestbornPlugin.RESET);
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        QuestDisplayBuilder.setPlugin(plugin);
        QuestDisplayBuilder.loadFormat(plugin.getConfig());

        String languageLoaded = plugin.getConfig().getString("language", "en_us").toUpperCase(Locale.ROOT);
        plugin.getLogger().info(QuestbornPlugin.LIGHT_BLUE + "Loading language..." + QuestbornPlugin.RESET + " [" + QuestbornPlugin.CYAN + QuestbornPlugin.BOLD + languageLoaded + QuestbornPlugin.RESET + "]");
        plugin.setLanguage(new LanguageManager(plugin));

        TimeFormatter.init(plugin);

        plugin.getLogger().info(QuestbornPlugin.LIGHT_BLUE + "Loading reward system..." + QuestbornPlugin.RESET);
        RewardHandler.init(plugin.getLanguage(), plugin);

        plugin.getLogger().info(QuestbornPlugin.LIGHT_BLUE + "Loading configurations..." + QuestbornPlugin.RESET);
        plugin.setGuiConfig(new GuiConfig(plugin));
        plugin.setMenuConfig(new MenuConfig(plugin));
        plugin.setTopConfig(new TopConfig(plugin));

        plugin.getLogger().info(QuestbornPlugin.LIGHT_BLUE + "Setting up player data storage..." + QuestbornPlugin.RESET);
        plugin.setPlayerDataStore(new PlayerDataStore(plugin, plugin.getDataFolder()));
        plugin.setGlobalQuestDataStore(new GlobalQuestDataStore(plugin, plugin.getPlayerDataStore().getStorageProvider()));

        int questTypesLoaded = 0;
        int questsLoaded = 0;
        int effectPresetsLoaded = 0;

        plugin.getLogger().info(QuestbornPlugin.LIGHT_BLUE + "Loading quest types and quests..." + QuestbornPlugin.RESET);

        Level originalLevel = plugin.getLogger().getLevel();
        try {
            plugin.getLogger().setLevel(Level.WARNING);

            plugin.setItemsAdderIntegration(new ItemsAdderIntegration(plugin));
            plugin.setCraftEngineIntegration(new ua.woody.questborn.integration.CraftEngineIntegration(plugin));
            plugin.setQuestManager(new QuestManager(plugin, plugin.getPlayerDataStore()));
            plugin.setDialogueManager(new DialogueManager(plugin));

            if (plugin.getQuestManager() != null) {
                questTypesLoaded = plugin.getQuestManager().getQuestTypeManager().getEnabledTypes().size();
                questsLoaded = plugin.getQuestManager().getAll().size();
                effectPresetsLoaded = plugin.getEffectPresetCountSafe();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, QuestbornPlugin.RED + "Failed to initialize QuestManager" + QuestbornPlugin.RESET, e);
        } finally {
            plugin.getLogger().setLevel(Objects.requireNonNullElse(originalLevel, Level.INFO));
        }

        plugin.applyOptimizationSettings();

        plugin.getLogger().info(QuestbornPlugin.LIGHT_BLUE + "Registering listeners..." + QuestbornPlugin.RESET);
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(new PlayerDataListener(plugin), plugin);
        pm.registerEvents(new FireworkDamageListener(), plugin);

        if (plugin.getQuestManager() != null) {
            QuestProgressListener listener = plugin.getQuestManager().getQuestProgressListener();
            pm.registerEvents(new BlockEventListener(listener), plugin);
            pm.registerEvents(new EntityEventListener(listener), plugin);
            pm.registerEvents(new ItemEventListener(listener), plugin);
            pm.registerEvents(new InteractionEventListener(listener), plugin);
            pm.registerEvents(new MovementEventListener(listener), plugin);
            pm.registerEvents(new PlayerStateEventListener(listener), plugin);

            try {
                Class.forName("io.papermc.paper.event.player.PlayerTradeEvent");
                pm.registerEvents(new PaperTradeListener(plugin, listener.getTradeHandler()), plugin);
            } catch (ClassNotFoundException ignored) {}
        } else {
            plugin.getLogger().warning(QuestbornPlugin.ORANGE + "QuestManager is null, quest progress listener not registered!" + QuestbornPlugin.RESET);
        }

        pm.registerEvents(new SkinUpdateListener(plugin), plugin);
        pm.registerEvents(new CommandListener(plugin), plugin);
        plugin.setMoodManager(new MoodManager(plugin));
        plugin.setNpcManager(new NpcManager(plugin));

        plugin.setupIntegration();

        plugin.setSkinsRestorerProvider(new SkinsRestorerProvider(plugin));
        if (plugin.getSkinsRestorerProvider().isEnabled()) {
            plugin.getLogger().info(QuestbornPlugin.LIGHT_BLUE + "Hooked into " + QuestbornPlugin.WHITE + "SkinsRestorer" + QuestbornPlugin.RESET);
        }

        if (plugin.getItemsAdderIntegration() != null && plugin.getItemsAdderIntegration().isEnabled()) {
            plugin.getLogger().info(QuestbornPlugin.LIGHT_BLUE + "Hooked into " + QuestbornPlugin.WHITE + "ItemsAdder" + QuestbornPlugin.RESET);
        }

        if (plugin.getCraftEngineIntegration() != null && plugin.getCraftEngineIntegration().isEnabled()) {
            plugin.getLogger().info(QuestbornPlugin.LIGHT_BLUE + "Hooked into " + QuestbornPlugin.WHITE + "CraftEngine" + QuestbornPlugin.RESET);
        }

        if (plugin.getConfig().getBoolean("integration.vault.enabled", true) && pm.getPlugin("Vault") != null) {
            plugin.setupEconomy();
            plugin.getLogger().info(QuestbornPlugin.LIGHT_BLUE + "Hooked into " + QuestbornPlugin.WHITE + "Vault" + QuestbornPlugin.RESET);
        }

        if (plugin.getConfig().getBoolean("integration.worldguard.enabled", true) && pm.getPlugin("WorldGuard") != null) {
            plugin.getLogger().info(QuestbornPlugin.LIGHT_BLUE + "Hooked into " + QuestbornPlugin.WHITE + "WorldGuard" + QuestbornPlugin.RESET);
        }

        if (plugin.getConfig().getBoolean("integration.placeholderapi.enabled", true) && pm.getPlugin("PlaceholderAPI") != null) {
            plugin.setPlaceholderExpansion(new QuestbornExpansion(plugin));
            plugin.getLogger().info(QuestbornPlugin.LIGHT_BLUE + "Hooked into " + QuestbornPlugin.WHITE + "PlaceholderAPI" + QuestbornPlugin.RESET);
            plugin.getFoliaLib().getImpl().runNextTick(__ -> {
                if (plugin.getPlaceholderExpansion() != null) {
                    plugin.getPlaceholderExpansion().register();
                }
            });
        }

        registerCommands();

        long loadTime = System.currentTimeMillis() - startTime;
        plugin.getLogger().info("");
        plugin.getLogger().info(QuestbornPlugin.GREEN + QuestbornPlugin.BOLD + "    Questborn is now ENABLED!" + QuestbornPlugin.RESET);
        plugin.getLogger().info(QuestbornPlugin.DARK_GRAY + "    Loaded " + QuestbornPlugin.CYAN + questTypesLoaded + QuestbornPlugin.DARK_GRAY + " quest types, " + QuestbornPlugin.CYAN + questsLoaded + QuestbornPlugin.DARK_GRAY + " quests, " + QuestbornPlugin.CYAN + effectPresetsLoaded + QuestbornPlugin.DARK_GRAY + " effect presets in " + QuestbornPlugin.YELLOW + loadTime + "ms" + QuestbornPlugin.RESET);
        plugin.getLogger().info("");
    }

    private void registerCommands() {
        plugin.getLogger().info(QuestbornPlugin.LIGHT_BLUE + "Registering commands..." + QuestbornPlugin.RESET);
        try {
            Logger pluginLogger = plugin.getLogger();
            Filter oldFilter = pluginLogger.getFilter();
            pluginLogger.setFilter(record -> {
                if (record.getMessage() != null && record.getMessage().contains("[ACF] Enabled Asynchronous Tab Completion")) {
                    return false;
                }
                return oldFilter == null || oldFilter.isLoggable(record);
            });

            PaperCommandManager manager = new PaperCommandManager(plugin);
            pluginLogger.setFilter(oldFilter);
            manager.enableUnstableAPI("help");

            String syntaxMsg = plugin.getLanguage().tr("commands.errors.invalid-syntax");
            manager.getLocales().addMessage(Locale.ENGLISH, co.aikar.commands.MessageKeys.INVALID_SYNTAX, syntaxMsg);

            manager.setDefaultExceptionHandler((command, registeredCommand, sender, args, t) -> {
                boolean isSyntaxError = false;
                if (t instanceof InvalidCommandArgument ex) {
                    try {
                        java.lang.reflect.Field f = InvalidCommandArgument.class.getDeclaredField("showSyntax");
                        f.setAccessible(true);
                        isSyntaxError = f.getBoolean(ex);
                    } catch (Exception ignored) {
                        isSyntaxError = true;
                    }
                } else if (t != null && (t.getClass().getName().contains("Syntax") || t.getClass().getName().contains("InvalidCommand") || t.getClass().getName().contains("Help"))) {
                    isSyntaxError = true;
                }

                if (isSyntaxError && registeredCommand != null) {
                    CommandSender bukkitSender = sender.getIssuer();
                    String syntax = registeredCommand.getSyntaxText();
                    String cmdLabel = "/" + registeredCommand.getCommand();
                    plugin.getLanguage().sendMessage(bukkitSender, "commands.errors.invalid-syntax", Map.of(
                        "command", cmdLabel.trim(),
                        "syntax", syntax != null ? syntax : ""
                    ));
                    return true;
                }
                return false;
            });

            QuestManager qm = plugin.getQuestManager();

            manager.getCommandCompletions().registerCompletion("quests", c -> {
                if (qm == null) return Collections.emptyList();
                return qm.getAll().stream().map(QuestDefinition::getId).toList();
            });
            manager.getCommandCompletions().registerCompletion("questsOfType", c -> {
                if (qm == null) return Collections.emptyList();
                String typeId = null;
                try { typeId = c.getContextValueByName(String.class, "typeId"); } catch (Exception ignored) {}
                if (typeId == null) {
                    try { typeId = c.getContextValue(String.class); } catch (Exception ignored) {}
                }
                if (typeId != null && !typeId.isEmpty()) {
                    final String targetType = typeId;
                    return qm.getAll().stream().filter(q -> targetType.equalsIgnoreCase(q.getTypeId())).map(QuestDefinition::getId).toList();
                }
                return qm.getAll().stream().map(QuestDefinition::getId).toList();
            });
            manager.getCommandCompletions().registerCompletion("questsOrTypes", c -> {
                if (qm == null) return Collections.emptyList();
                String category = null;
                try { category = c.getContextValueByName(String.class, "category"); } catch (Exception ignored) {}
                if ("quest".equalsIgnoreCase(category)) {
                    return qm.getAll().stream().map(QuestDefinition::getId).toList();
                } else if ("type".equalsIgnoreCase(category)) {
                    return qm.getQuestTypeManager().getEnabledTypes().stream().map(QuestTypeConfig::getId).toList();
                }
                List<String> combined = new ArrayList<>();
                combined.addAll(qm.getAll().stream().map(QuestDefinition::getId).toList());
                combined.addAll(qm.getQuestTypeManager().getEnabledTypes().stream().map(QuestTypeConfig::getId).toList());
                return combined;
            });
            manager.getCommandCompletions().registerCompletion("globalQuests", c -> {
                if (qm == null) return Collections.emptyList();
                return qm.getAll().stream().filter(q -> {
                    var t = qm.getQuestTypeManager().getType(q.getTypeId());
                    return t != null && t.getEngine() == EngineType.GLOBAL;
                }).map(QuestDefinition::getId).toList();
            });
            manager.getCommandCompletions().registerCompletion("activeGlobalQuests", c -> {
                if (qm == null) return Collections.emptyList();
                return qm.getAll().stream().filter(q -> {
                    var t = qm.getQuestTypeManager().getType(q.getTypeId());
                    if (t == null || t.getEngine() != EngineType.GLOBAL) return false;
                    var gp = plugin.getGlobalQuestDataStore().get(q.getId());
                    return !gp.isCompleted() && gp.getParticipantCount() > 0;
                }).map(QuestDefinition::getId).toList();
            });
            manager.getCommandCompletions().registerCompletion("completedGlobalQuests", c -> {
                if (qm == null) return Collections.emptyList();
                return qm.getAll().stream().filter(q -> {
                    var t = qm.getQuestTypeManager().getType(q.getTypeId());
                    if (t == null || t.getEngine() != EngineType.GLOBAL) return false;
                    return plugin.getGlobalQuestDataStore().get(q.getId()).isCompleted();
                }).map(QuestDefinition::getId).toList();
            });
            manager.getCommandCompletions().registerCompletion("questTypes", c -> {
                if (qm == null) return Collections.emptyList();
                return qm.getQuestTypeManager().getEnabledTypes().stream().map(QuestTypeConfig::getId).toList();
            });
            manager.getCommandCompletions().registerCompletion("resetQuestTypes", c -> {
                if (qm == null) return Collections.emptyList();
                return qm.getQuestTypeManager().getEnabledTypes().stream().filter(t -> t.getEngine() != EngineType.GLOBAL).map(QuestTypeConfig::getId).toList();
            });
            manager.getCommandCompletions().registerCompletion("playersOrAll", c -> {
                java.util.List<String> list = new java.util.ArrayList<>();
                list.add("-all");
                org.bukkit.Bukkit.getOnlinePlayers().forEach(p -> list.add(p.getName()));
                return list;
            });
            manager.getCommandContexts().registerIssuerAwareContext(Player.class, c -> {
                boolean isOptional = c.hasAnnotation(Optional.class);
                String arg = c.popFirstArg();
                if (arg == null || arg.isEmpty()) {
                    if (c.getSender() instanceof Player p) {
                        return p;
                    }
                    if (isOptional) {
                        return null;
                    }
                    plugin.getLanguage().sendMessage(c.getSender(), "commands.errors.console-specify-player");
                    throw new InvalidCommandArgument(false);
                }
                Player p = Bukkit.getPlayerExact(arg);
                if (p == null) {
                    p = Bukkit.getPlayer(arg);
                }
                if (p == null) {
                    plugin.getLanguage().sendMessage(c.getSender(), "commands.errors.player-not-found", Map.of("player", arg));
                    throw new InvalidCommandArgument(false);
                }
                return p;
            });

            manager.getCommandCompletions().registerCompletion("effectPresets", c -> {
                if (qm == null || qm.getEffectPresetManager() == null) return List.of("list");
                List<String> list = new ArrayList<>();
                list.add("list");
                list.addAll(qm.getEffectPresetManager().getAllPresetIds().stream()
                        .filter(id -> !"list".equalsIgnoreCase(id))
                        .sorted()
                        .toList());
                return list;
            });
            manager.getCommandCompletions().registerCompletion("npcIds", c -> {
                if (plugin.getNpcManager() == null) return Collections.emptyList();
                return new ArrayList<>(plugin.getNpcManager().getAllNpcIds());
            });
            manager.getCommandCompletions().registerCompletion("dialogues", c -> {
                if (plugin.getDialogueManager() == null) return Collections.emptyList();
                return new ArrayList<>(plugin.getDialogueManager().getDialogues().keySet());
            });
            manager.getCommandCompletions().registerCompletion("voices", c ->
                List.of("villager", "deep", "old", "cute", "fairy", "monster", "zombie", "robot", "goblin", "ender", "witch", "none")
            );
            manager.getCommandCompletions().registerCompletion("npcIndicatorOffsets", c ->
                List.of("0.0", "0.5", "1.0", "1.5", "2.0", "2.5", "3.0")
            );

            manager.registerCommand(new QuestCommands(plugin));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, QuestbornPlugin.RED + "Failed to register commands" + QuestbornPlugin.RESET, e);
        }
    }
}
