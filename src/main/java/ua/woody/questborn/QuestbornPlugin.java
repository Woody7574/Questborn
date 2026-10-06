package ua.woody.questborn;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ua.woody.questborn.commands.QuestCommands;
import co.aikar.commands.PaperCommandManager;
import ua.woody.questborn.config.GuiConfig;
import ua.woody.questborn.config.TopConfig;
import ua.woody.questborn.lang.LanguageManager;

import ua.woody.questborn.listeners.PlayerDataListener;
import ua.woody.questborn.listeners.QuestProgressListener;
import ua.woody.questborn.managers.QuestManager;
import ua.woody.questborn.rewards.RewardHandler;
import ua.woody.questborn.storage.PlayerDataStore;
import ua.woody.questborn.update.UpdateChecker;
import ua.woody.questborn.util.QuestDisplayBuilder;
import ua.woody.questborn.util.TimeFormatter;
import ua.woody.questborn.libs.bstats.Metrics;
import ua.woody.questborn.util.ActionLogger;
import ua.woody.questborn.util.SmartYamlUpdater;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.tcoded.folialib.FoliaLib;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class QuestbornPlugin extends JavaPlugin {
    private static QuestbornPlugin instance;
    private FoliaLib foliaLib;

    static {
        try {
            System.setProperty("com.mongodb.diagnostics.logging.level", "OFF");
            System.setProperty("org.mongodb.driver.logging.level", "OFF");

            java.util.logging.Logger mongoLogger = java.util.logging.Logger.getLogger("org.mongodb.driver");
            mongoLogger.setLevel(Level.OFF);
            mongoLogger.setFilter(record -> false);
            mongoLogger.setUseParentHandlers(false);

            silenceLog4j2();
        } catch (Exception ignored) {}
    }

    private static void silenceLog4j2() {
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();

            String name = "org.mongodb.driver";
            LoggerConfig loggerConfig = config.getLoggerConfig(name);

            if (!loggerConfig.getName().equals(name)) {
                LoggerConfig newConfig = new LoggerConfig(name, org.apache.logging.log4j.Level.OFF, false);
                config.addLogger(name, newConfig);
            } else {
                loggerConfig.setLevel(org.apache.logging.log4j.Level.OFF);
            }

            ctx.updateLoggers();
        } catch (Throwable ignored) {}
    }

    public static QuestbornPlugin getInstance() {
        return instance;
    }

    private PlayerDataStore playerDataStore;
    private ua.woody.questborn.storage.GlobalQuestDataStore globalQuestDataStore;
    private QuestManager questManager;
    private ActionLogger actionLogger;
    private LanguageManager language;
    private ua.woody.questborn.managers.DialogueManager dialogueManager;
    private TopConfig topConfig;
    private GuiConfig guiConfig;
    private ua.woody.questborn.config.MenuConfig menuConfig;
    private ua.woody.questborn.managers.NpcIndicatorManager npcIndicatorManager;
    private ua.woody.questborn.integration.npc.NpcIntegrationManager npcIntegrationManager;
    private ua.woody.questborn.managers.NpcManager npcManager;
    private ua.woody.questborn.managers.MoodManager moodManager;
    private ua.woody.questborn.gui.editor.ChatInputManager chatInputManager;
    private net.kyori.adventure.platform.bukkit.BukkitAudiences adventure;

    public net.kyori.adventure.platform.bukkit.BukkitAudiences getAdventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Cannot retrieve audience provider while plugin is not enabled");
        }
        return this.adventure;
    }

    public ua.woody.questborn.managers.NpcManager getNpcManager() {
        return npcManager;
    }

    public ua.woody.questborn.managers.MoodManager getMoodManager() {
        return moodManager;
    }

    public ua.woody.questborn.gui.editor.ChatInputManager getChatInputManager() {
        return chatInputManager;
    }

    public ua.woody.questborn.storage.GlobalQuestDataStore getGlobalQuestDataStore() {
        return globalQuestDataStore;
    }

    public ua.woody.questborn.integration.npc.NpcIntegrationManager getNpcIntegrationManager() {
        return npcIntegrationManager;
    }

    private ua.woody.questborn.integration.ItemsAdderIntegration itemsAdderIntegration;

    public ua.woody.questborn.integration.ItemsAdderIntegration getItemsAdderIntegration() {
        return itemsAdderIntegration;
    }

    private ua.woody.questborn.integration.CraftEngineIntegration craftEngineIntegration;

    public ua.woody.questborn.integration.CraftEngineIntegration getCraftEngineIntegration() {
        return craftEngineIntegration;
    }

    private ua.woody.questborn.integration.skin.SkinsRestorerProvider skinsRestorerProvider;

    public ua.woody.questborn.integration.skin.SkinsRestorerProvider getSkinsRestorerProvider() {
        return skinsRestorerProvider;
    }

    public ua.woody.questborn.managers.NpcIndicatorManager getNpcIndicatorManager() {
        return npcIndicatorManager;
    }

    private ua.woody.questborn.integration.QuestbornExpansion placeholderExpansion;
    private net.milkbowl.vault.economy.Economy econ = null;

    public boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public net.milkbowl.vault.economy.Economy getEconomy() { return econ; }
    public ua.woody.questborn.integration.QuestbornExpansion getPlaceholderExpansion() { return placeholderExpansion; }

    public LanguageManager getLanguageManager() { return language; }
    public void setLanguage(LanguageManager language) { this.language = language; }
    public void setGuiConfig(GuiConfig guiConfig) { this.guiConfig = guiConfig; }
    public void setMenuConfig(ua.woody.questborn.config.MenuConfig menuConfig) { this.menuConfig = menuConfig; }
    public void setTopConfig(TopConfig topConfig) { this.topConfig = topConfig; }
    public void setPlayerDataStore(PlayerDataStore playerDataStore) { this.playerDataStore = playerDataStore; }
    public void setGlobalQuestDataStore(ua.woody.questborn.storage.GlobalQuestDataStore globalQuestDataStore) { this.globalQuestDataStore = globalQuestDataStore; }
    public void setItemsAdderIntegration(ua.woody.questborn.integration.ItemsAdderIntegration itemsAdderIntegration) { this.itemsAdderIntegration = itemsAdderIntegration; }
    public void setCraftEngineIntegration(ua.woody.questborn.integration.CraftEngineIntegration craftEngineIntegration) { this.craftEngineIntegration = craftEngineIntegration; }
    public void setQuestManager(QuestManager questManager) { this.questManager = questManager; }
    public void setDialogueManager(ua.woody.questborn.managers.DialogueManager dialogueManager) { this.dialogueManager = dialogueManager; }
    public void setMoodManager(ua.woody.questborn.managers.MoodManager moodManager) { this.moodManager = moodManager; }
    public void setNpcManager(ua.woody.questborn.managers.NpcManager npcManager) { this.npcManager = npcManager; }
    public void setSkinsRestorerProvider(ua.woody.questborn.integration.skin.SkinsRestorerProvider skinsRestorerProvider) { this.skinsRestorerProvider = skinsRestorerProvider; }
    public void setPlaceholderExpansion(ua.woody.questborn.integration.QuestbornExpansion placeholderExpansion) { this.placeholderExpansion = placeholderExpansion; }

    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    public static final String BOLD = "\u001B[1m";
    public static final String DARK_GRAY = "\u001B[90m";
    public static final String RED = "\u001B[31m";
    public static final String GOLD = "\u001B[38;5;214m";
    public static final String LIGHT_BLUE = "\u001B[38;5;117m";
    public static final String ORANGE = "\u001B[38;5;208m";
    public static final String LIGHT_GREEN = "\u001B[38;5;46m";
    public static final String PURPLE = "\u001B[38;5;171m";

    private ua.woody.questborn.bootstrap.PluginBootstrap bootstrap;

    @Override
    public void onEnable() {
        ua.woody.questborn.api.QuestbornProvider.set(new ua.woody.questborn.api.DefaultQuestbornAPI());
        instance = this;
        this.foliaLib = new FoliaLib(this);
        this.adventure = net.kyori.adventure.platform.bukkit.BukkitAudiences.create(this);
        this.bootstrap = new ua.woody.questborn.bootstrap.PluginBootstrap(this);
        this.bootstrap.enable();
        this.actionLogger = new ActionLogger(this);
        this.chatInputManager = new ua.woody.questborn.gui.editor.ChatInputManager(this);
        this.setupMetrics();
    }

    public void preloadGuiClasses() {
        getFoliaLib().getImpl().runAsync(__task -> {
            try {
                Class.forName("ua.woody.questborn.gui.MainMenuGui");
                Class.forName("ua.woody.questborn.gui.QuestListGui");
                Class.forName("ua.woody.questborn.gui.QuestDetailsGui");
                Class.forName("ua.woody.questborn.gui.QuestTransferGui");
                Class.forName("ua.woody.questborn.gui.TopGui");
            } catch (ClassNotFoundException ignored) {}
        });
    }

    public String getServerType() {
        String serverName = getServer().getName();
        if (serverName.equalsIgnoreCase("Leaf") || serverName.equalsIgnoreCase("Leaves") ||
            serverName.equalsIgnoreCase("Purpur") || serverName.equalsIgnoreCase("Pufferfish")) {
            return serverName;
        }

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return "Folia";
        } catch (ClassNotFoundException e0) {
            try {
                Class.forName("org.purpurmc.purpur.PurpurConfig");
                return "Purpur";
            } catch (ClassNotFoundException ep) {
                try {
                    Class.forName("gg.pufferfish.pufferfish.PufferfishConfig");
                    return "Pufferfish";
                } catch (ClassNotFoundException ep2) {
                    try {
                        Class.forName("com.destroystokyo.paper.PaperConfig");
                        return "Paper";
                    } catch (ClassNotFoundException e) {
                        try {
                            Class.forName("org.spigotmc.SpigotConfig");
                            return "Spigot";
                        } catch (ClassNotFoundException e2) {
                            return "Bukkit";
                        }
                    }
                }
            }
        }
    }

    public void ensureLanguageResources() {
        File folder = new File(getDataFolder(), "language");

        if (!folder.exists() && !folder.mkdirs()) {
            getLogger().warning(ORANGE + "Failed to create language folder!" + RESET);
            return;
        }

        File mcFolder = new File(folder, "minecraft");
        File editorFolder = new File(folder, "editor");

        String[] langCodes = new String[] { "en_us", "uk_ua", "de_de", "es_es", "fr_fr", "pt_br", "pl_pl", "ru_ru", "kk_kz", "tr_tr" };

        boolean missing = !folder.exists() || !folder.isDirectory()
                || !mcFolder.exists() || !mcFolder.isDirectory()
                || !editorFolder.exists() || !editorFolder.isDirectory();

        if (!missing) {
            for (String code : langCodes) {
                if (!new File(folder, code + ".yml").exists()
                        || !new File(editorFolder, code + ".yml").exists()
                        || !new File(mcFolder, code + ".yml").exists()) {
                    missing = true;
                    break;
                }
            }
        }

        if (missing) {
            getLogger().info(ORANGE + "Missing language files detected, extracting from JAR..." + RESET);
            extractBundledFolderIfMissing("language");
        }
    }

    public void ensureRootConfigsResources() {
        File configFile = new File(getDataFolder(), "config.yml");
        SmartYamlUpdater.updateAndLoad(this, configFile, "config.yml");

        File effectsFile = new File(getDataFolder(), "quest-effects.yml");
        SmartYamlUpdater.updateAndLoad(this, effectsFile, "quest-effects.yml");

        File menusFile = new File(getDataFolder(), "menus.yml");
        SmartYamlUpdater.updateAndLoad(this, menusFile, "menus.yml");
    }

    @Override
    public void reloadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        SmartYamlUpdater.updateAndLoad(this, configFile, "config.yml");
        super.reloadConfig();
        org.bukkit.configuration.file.YamlConfiguration def = SmartYamlUpdater.loadJarResource(this, "config.yml");
        if (def != null) {
            getConfig().setDefaults(def);
        }
    }

    private void extractBundledFolderIfMissing(String folderName) {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            getLogger()
                    .warning(ORANGE + "Failed to create plugin data folder: " + dataFolder.getAbsolutePath() + RESET);
            return;
        }

        int extracted = 0;

        try {
            File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!jarFile.isFile())
                return;

            String prefix = folderName.toLowerCase(Locale.ROOT) + "/";

            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.isDirectory())
                        continue;

                    String name = entry.getName();
                    String lower = name.toLowerCase(Locale.ROOT);

                    if (!lower.startsWith(prefix))
                        continue;

                    if (name.contains("..") || name.startsWith("/") || name.startsWith("\\"))
                        continue;

                    File out = new File(dataFolder, name);
                    if (out.exists())
                        continue;

                    File parent = out.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs())
                        continue;

                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        extracted++;
                    }
                }
            }

            if (extracted > 0) {
                getLogger().info(LIGHT_BLUE + "Extracted " + WHITE + extracted + RESET + LIGHT_BLUE + " resources to /"
                        + folderName + RESET);
            }

        } catch (Exception e) {
            getLogger().warning(ORANGE + "Failed to restore " + folderName + " resources: " + e.getMessage() + RESET);
        }
    }

    private void extractBundledFileIfMissing(String relativePath) {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs())
            return;

        File out = new File(dataFolder, relativePath);
        if (out.exists())
            return;

        try {
            File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!jarFile.isFile())
                return;

            String target = relativePath.replace("\\", "/");
            String lowerTarget = target.toLowerCase(Locale.ROOT);

            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.isDirectory())
                        continue;

                    String name = entry.getName();
                    if (name == null)
                        continue;

                    if (!name.toLowerCase(Locale.ROOT).equals(lowerTarget))
                        continue;

                    if (name.contains("..") || name.startsWith("/") || name.startsWith("\\"))
                        return;

                    File parent = out.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs())
                        return;

                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    getLogger().info(LIGHT_BLUE + "Restored missing file: " + WHITE + relativePath + RESET);
                    return;
                }
            }

        } catch (Exception e) {
            getLogger().warning(ORANGE + "Failed to restore " + relativePath + ": " + e.getMessage() + RESET);
        }
    }

    private void reloadPlayerDataIfPresentNoCreate() {
        if (playerDataStore == null)
            return;
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            playerDataStore.loadPlayerSync(p.getUniqueId());
        }
    }

    public void installBundledResourcesOnFirstInstall() {
        File dataFolder = getDataFolder();

        boolean folderMissing = !dataFolder.exists();
        boolean folderEmpty = dataFolder.exists() && dataFolder.isDirectory()
                && Objects.requireNonNullElse(dataFolder.listFiles(), new File[0]).length == 0;

        if (!folderMissing && !folderEmpty) {
            return;
        }

        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            getLogger()
                    .warning(ORANGE + "Failed to create plugin data folder: " + dataFolder.getAbsolutePath() + RESET);
            return;
        }

        int extracted = 0;

        try {
            File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!jarFile.isFile()) {
                return;
            }

            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.isDirectory())
                        continue;

                    String name = entry.getName();
                    String lower = name.toLowerCase(Locale.ROOT);

                    if (name.startsWith("META-INF/"))
                        continue;
                    if (name.endsWith(".class"))
                        continue;
                    if (lower.equals("plugin.yml") || lower.equals("paper-plugin.yml") || lower.equals("bungee.yml"))
                        continue;

                    if (!isQuestbornBundledResource(name))
                        continue;

                    if (name.contains("..") || name.startsWith("/") || name.startsWith("\\"))
                        continue;

                    File out = new File(dataFolder, name);
                    if (out.exists())
                        continue;

                    File parent = out.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs())
                        continue;

                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        extracted++;
                    }
                }
            }

            if (extracted > 0) {
                getLogger().info(LIGHT_BLUE + "Installed default resources: " + WHITE + extracted + RESET);
            }

        } catch (URISyntaxException e) {
            getLogger().warning(ORANGE + "Failed to resolve plugin jar location: " + e.getMessage() + RESET);
        } catch (Exception e) {
            getLogger().warning(ORANGE + "Failed to install resources: " + e.getMessage() + RESET);
        }
    }

    private boolean isQuestbornBundledResource(String path) {
        String lower = path.toLowerCase(Locale.ROOT);

        if (lower.startsWith("language/"))
            return true;
        if (lower.startsWith("quests/"))
            return true;
        if (lower.startsWith("types/"))
            return true;
        if (lower.startsWith("npcs/"))
            return true;

        return lower.equals("config.yml")
                || lower.equals("quest-effects.yml");
    }

    public void applyOptimizationSettings() {
        boolean autoSave = getConfig().getBoolean("optimization.auto-save.enabled", true);
        if (autoSave) {
            int interval = getConfig().getInt("optimization.auto-save.interval-seconds", 30);
            int globalInterval = getConfig().getInt("optimization.auto-save.global-interval-seconds", interval * 2);

            if (playerDataStore != null) {
                playerDataStore.setAutoSaveEnabled(true);
                playerDataStore.setAutoSaveInterval(interval * 20);
            }
            if (globalQuestDataStore != null) {
                globalQuestDataStore.setAutoSaveEnabled(true);
                globalQuestDataStore.setAutoSaveInterval(globalInterval * 20);
            }
        } else {
            if (playerDataStore != null) {
                playerDataStore.setAutoSaveEnabled(false);
            }
            if (globalQuestDataStore != null) {
                globalQuestDataStore.setAutoSaveEnabled(false);
            }
        }
    }

    private void setupMetrics() {
        if (!getConfig().getBoolean("metrics.enabled", true)) {
            return;
        }

        try {
            int pluginId = 28356;
            Metrics metrics = new Metrics(this, pluginId);

            metrics.addCustomChart(new Metrics.SimplePie("language",
                    () -> getConfig().getString("language", "en_us")));

            metrics.addCustomChart(new Metrics.SimplePie("top_system_enabled",
                    () -> topConfig != null && topConfig.isEnabled() ? "Yes" : "No"));

            metrics.addCustomChart(new Metrics.SingleLineChart("total_quests",
                    () -> questManager != null ? questManager.getAll().size() : 0));

            metrics.addCustomChart(new Metrics.SingleLineChart("quest_types",
                    () -> questManager != null ? questManager.getQuestTypeManager().getEnabledTypes().size() : 0));

            metrics.addCustomChart(new Metrics.AdvancedPie("server_type", () -> {
                java.util.Map<String, Integer> valueMap = new java.util.HashMap<>();
                String serverType = getServerType();
                valueMap.put(serverType, 1);
                return valueMap;
            }));

            metrics.addCustomChart(new Metrics.SimplePie("optimization_mode",
                    () -> getConfig().getBoolean("optimization.distance-quests.enabled", true) ? "Optimized"
                            : "Standard"));

            getLogger().info(LIGHT_BLUE + "Metrics initialized successfully" + RESET);

        } catch (Exception e) {
            getLogger().warning(ORANGE + "Failed to initialize metrics: " + e.getMessage() + RESET);
        }
    }

    public int getEffectPresetCountSafe() {
        try {
            var effectPresetManager = questManager.getEffectPresetManager();
            var presetsField = effectPresetManager.getClass().getDeclaredField("presets");
            presetsField.setAccessible(true);
            Map<?, ?> presets = (Map<?, ?>) presetsField.get(effectPresetManager);
            return presets != null ? presets.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public void reloadAll() {
        ua.woody.questborn.gui.editor.EditorSessionManager.forceCloseAll();
        long startTime = System.currentTimeMillis();

        final String version = Objects.requireNonNullElse(getDescription().getVersion(), "dev");

        getLogger().info("");
        getLogger().info(GOLD + BOLD + "    Reloading Questborn" + WHITE + BOLD + " v" + version + RESET);
        getLogger().info("");

        Level originalLevel = getLogger().getLevel();

        String languageLoaded = "EN_US";

        int effectPresetsLoaded = 0;

        try {
            getLogger().setLevel(Level.WARNING);

            try {
                System.setProperty("com.mongodb.diagnostics.logging.level", "OFF");
                System.setProperty("org.mongodb.driver.logging.level", "OFF");
                java.util.logging.LogManager logManager = java.util.logging.LogManager.getLogManager();
                java.util.Enumeration<String> loggerNames = logManager.getLoggerNames();
                while (loggerNames.hasMoreElements()) {
                    String name = loggerNames.nextElement();
                    if (name.startsWith("org.mongodb.driver")) {
                        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(name);
                        logger.setLevel(Level.OFF);
                        logger.setFilter(record -> false);
                        logger.setUseParentHandlers(false);
                    }
                }
                silenceLog4j2();
            } catch (Exception ignored) {}

            ensureRootConfigsResources();
            ensureLanguageResources();

            reloadConfig();
            QuestDisplayBuilder.loadFormat(getConfig());

            languageLoaded = getConfig().getString("language", "en_us").toUpperCase(Locale.ROOT);

            getLogger().info(
                    LIGHT_BLUE + "Reloading language..." + RESET + " [" + CYAN + BOLD + languageLoaded + RESET + "]");

            if (language == null) {
                language = new LanguageManager(this);
            } else {
                language.reload();
            }

            RewardHandler.init(language, this);

            TimeFormatter.init(this);

            if (playerDataStore != null) {
                playerDataStore.save();
                playerDataStore.clearCache();
            }
            if (globalQuestDataStore != null) {
                globalQuestDataStore.reload();
            }
            reloadPlayerDataIfPresentNoCreate();

            if (questManager != null) {
                getLogger().info(LIGHT_BLUE + "Reloading quests..." + RESET);
                questManager.reload();
                if (dialogueManager != null) dialogueManager.reload();

                if (actionLogger != null) {
                    actionLogger.reloadConfig();
                }

                effectPresetsLoaded = getEffectPresetCountSafe();
            } else {
                effectPresetsLoaded = 0;
            }

            if (topConfig != null)
                topConfig.reload();
            if (guiConfig != null)
                guiConfig.reload();
            if (menuConfig != null)
                menuConfig.reload();

            if (npcManager != null) {
                npcManager.reload();
            }

            setupIntegration();

            this.itemsAdderIntegration = new ua.woody.questborn.integration.ItemsAdderIntegration(this);
            this.craftEngineIntegration = new ua.woody.questborn.integration.CraftEngineIntegration(this);

            if (this.itemsAdderIntegration != null && this.itemsAdderIntegration.isEnabled()) {
                getLogger().info(LIGHT_BLUE + "Hooked into " + WHITE + "ItemsAdder" + RESET);
            }
            if (this.craftEngineIntegration != null && this.craftEngineIntegration.isEnabled()) {
                getLogger().info(LIGHT_BLUE + "Hooked into " + WHITE + "CraftEngine" + RESET);
            }

            this.skinsRestorerProvider = new ua.woody.questborn.integration.skin.SkinsRestorerProvider(this);
            if (this.skinsRestorerProvider.isEnabled()) {
                getLogger().info(LIGHT_BLUE + "Hooked into " + WHITE + "SkinsRestorer" + RESET);
            }

            if (getConfig().getBoolean("integration.vault.enabled", true)
                    && getServer().getPluginManager().getPlugin("Vault") != null) {
                setupEconomy();
                getLogger().info(LIGHT_BLUE + "Hooked into " + WHITE + "Vault" + RESET);
            }

            if (getConfig().getBoolean("integration.worldguard.enabled", true)
                    && getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                getLogger().info(LIGHT_BLUE + "Hooked into " + WHITE + "WorldGuard" + RESET);
            }

            applyOptimizationSettings();

        } finally {
            getLogger().setLevel(Objects.requireNonNullElse(originalLevel, Level.INFO));
        }

        long reloadTime = System.currentTimeMillis() - startTime;

        getLogger().info(LIGHT_BLUE + "Language: " + RESET + "[" + CYAN + BOLD + languageLoaded + RESET + "]");

        if (questManager != null) {
            int questCount = questManager.getAll().size();
            int typeCount = questManager.getQuestTypeManager().getEnabledTypes().size();

            getLogger()
                    .info(LIGHT_BLUE + "Reloaded " + WHITE + typeCount + RESET + LIGHT_BLUE + " quest types" + RESET);
            getLogger()
                    .info(LIGHT_BLUE + "Reloaded " + WHITE + questCount + RESET + LIGHT_BLUE + " total quests" + RESET);
        }

        getLogger().info(LIGHT_BLUE + "Reloaded " + WHITE + effectPresetsLoaded + RESET + LIGHT_BLUE + " effect presets"
                + RESET);

        if (npcManager != null && getConfig().getBoolean("integration.npc.enabled", false)) {
            int npcCount = npcManager.getAll().size();
            getLogger().info(LIGHT_BLUE + "Reloaded " + WHITE + npcCount + RESET + LIGHT_BLUE + " NPC configs" + RESET);
        }

        if (topConfig != null) {
            String topStatus = topConfig.isEnabled() ? LIGHT_GREEN + "ENABLED" : ORANGE + "DISABLED";
            getLogger().info(LIGHT_BLUE + "Top system: " + topStatus + RESET);
        }

        getLogger().info(CYAN + "Reload complete." + RESET
                + DARK_GRAY + " (Took " + WHITE + reloadTime + "ms" + RESET + DARK_GRAY + ")" + RESET);
    }

    public ua.woody.questborn.managers.SoundManager getSoundManager() {
        return questManager != null ? questManager.getSoundManager() : null;
    }

    public GuiConfig getGuiConfig() {
        return guiConfig;
    }

    public ua.woody.questborn.config.MenuConfig getMenuConfig() {
        return menuConfig;
    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
        final String version = Objects.requireNonNullElse(getDescription().getVersion(), "dev");

        getLogger().info("");
        getLogger().info(YELLOW + BOLD + "    Disabling Questborn" + WHITE + BOLD + " v" + version + RESET);
        getLogger().info("");

        if (questManager != null) {
            getLogger().info(LIGHT_BLUE + "Shutting down quest manager..." + RESET);
            try {
                questManager.onDisable();
            } catch (Exception e) {
                getLogger().warning(ORANGE + "QuestManager shutdown error: " + e.getMessage() + RESET);
            }
        }

        if (globalQuestDataStore != null) {
            getLogger().info(LIGHT_BLUE + "Saving global quests data..." + RESET);
            try {
                globalQuestDataStore.onDisable();
            } catch (Exception e) {
                getLogger().warning(ORANGE + "Global quests save error: " + e.getMessage() + RESET);
            }
        }

        if (playerDataStore != null) {
            getLogger().info(LIGHT_BLUE + "Saving player data..." + RESET);
            try {
                playerDataStore.onDisable();
            } catch (Exception e) {
                getLogger().warning(ORANGE + "Player data save error: " + e.getMessage() + RESET);
            }
        }

        if (placeholderExpansion != null) {
            try {
                placeholderExpansion.unregister();
            } catch (Exception ignored) {
            }
        }

        if (npcIndicatorManager != null) {
            npcIndicatorManager.shutdown();
        }

        getLogger().info(CYAN + "Plugin successfully disabled" + RESET);
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public PlayerDataStore getPlayerDataStore() {
        return playerDataStore;
    }

    public ActionLogger getActionLogger() {
        return actionLogger;
    }

    public ua.woody.questborn.managers.DialogueManager getDialogueManager() {
        return dialogueManager;
    }

    public LanguageManager getLanguage() {
        return language;
    }

    public FoliaLib getFoliaLib() {
        return foliaLib;
    }

    public TopConfig getTopConfig() {
        return topConfig;
    }

    public void setupIntegration() {
        if (this.npcIntegrationManager == null) {
            this.npcIntegrationManager = new ua.woody.questborn.integration.npc.NpcIntegrationManager(this);
        }

        boolean enabled = getConfig().getBoolean("integration.npc.enabled",
                getConfig().getBoolean("integration.enabled", true));

        if (!enabled) {
            if (npcIntegrationManager.getProvider() != null) {
                getLogger().info(ORANGE + "NPC integration disabled in config. Unhooking..." + RESET);
                npcIntegrationManager.setProvider(null);
            }
            if (npcIndicatorManager != null) {
                npcIndicatorManager.shutdown();
                npcIndicatorManager = null;
            }
            return;
        }

        String npcPlugin = getConfig()
                .getString("integration.npc.plugin", getConfig().getString("integration.npc-plugin", "CITIZENS"))
                .toUpperCase(Locale.ROOT);

        boolean needsInit = npcIntegrationManager.getProvider() == null;

        if (!needsInit) {
            String currentType = (npcIntegrationManager
                    .getProvider() instanceof ua.woody.questborn.integration.npc.FancyNpcsProvider)
                            ? "FANCYNPCS"
                            : "CITIZENS";

            if (!currentType.equals(npcPlugin)) {
                getLogger()
                        .info(LIGHT_BLUE + "Switching NPC provider from " + currentType + " to " + npcPlugin + RESET);
                needsInit = true;
            }
        }

        if (needsInit) {
            if ("FANCYNPCS".equals(npcPlugin)) {
                if (getServer().getPluginManager().getPlugin("FancyNpcs") != null) {
                    npcIntegrationManager.setProvider(new ua.woody.questborn.integration.npc.FancyNpcsProvider(this));
                    getLogger().info(LIGHT_BLUE + "Hooked into " + WHITE + "FancyNpcs" + RESET);
                } else {
                    getLogger().warning(ORANGE + "FancyNpcs not found! NPC integration disabled." + RESET);
                    npcIntegrationManager.setProvider(null);
                }
            } else {
                if (getServer().getPluginManager().getPlugin("Citizens") != null) {
                    npcIntegrationManager.setProvider(new ua.woody.questborn.integration.npc.CitizensProvider(this));
                    getLogger().info(LIGHT_BLUE + "Hooked into " + WHITE + "Citizens" + RESET);
                } else {
                    npcIntegrationManager.setProvider(null);
                }
            }
        }

        if (npcIntegrationManager.getProvider() != null) {
            if (this.npcIndicatorManager == null) {
                this.npcIndicatorManager = new ua.woody.questborn.managers.NpcIndicatorManager(this);
            } else {
                this.npcIndicatorManager.reload();
            }
        } else {
            if (this.npcIndicatorManager != null) {
                this.npcIndicatorManager.shutdown();
                this.npcIndicatorManager = null;
            }
        }
    }
}
