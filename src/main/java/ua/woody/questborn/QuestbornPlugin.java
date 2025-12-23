package ua.woody.questborn;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ua.woody.questborn.commands.QuestCommand;
import ua.woody.questborn.commands.QuestTabCompleter;
import ua.woody.questborn.config.GuiConfig;
import ua.woody.questborn.config.TopConfig;
import ua.woody.questborn.lang.LanguageManager;
import ua.woody.questborn.listeners.GuiClickListener;
import ua.woody.questborn.listeners.QuestProgressListener;
import ua.woody.questborn.managers.QuestManager;
import ua.woody.questborn.rewards.RewardHandler;
import ua.woody.questborn.storage.PlayerDataStore;
import ua.woody.questborn.update.UpdateChecker;
import ua.woody.questborn.util.ObjectiveLoreBuilder;
import ua.woody.questborn.util.TimeFormatter;
import ua.woody.questborn.libs.bstats.Metrics;

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

public class QuestbornPlugin extends JavaPlugin {

    private PlayerDataStore playerDataStore;
    private QuestManager questManager;
    private LanguageManager language;
    private TopConfig topConfig;
    private GuiConfig guiConfig;

    private ua.woody.questborn.addons.AddonManager addonManager;
    public ua.woody.questborn.addons.AddonManager getAddonManager() { return addonManager; }

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        // ✅ Витягуємо дефолтні ресурси ТІЛЬКИ на першій установці (або коли папка порожня на Paper)
        installBundledResourcesOnFirstInstall();

        // config.yml (не перезапише, якщо вже є)
        saveDefaultConfig();

        // Формати лору для цілей
        ObjectiveLoreBuilder.loadFormat(getConfig());

        int questTypesLoaded = 0;
        int questsLoaded = 0;
        int effectPresetsLoaded = 0;
        String languageLoaded = getConfig().getString("language", "en_us");

        // HEADER
        getLogger().info("╔══════════════════════════════════════════════╗");
        getLogger().info("║             Questborn v1.1.0                 ║");
        getLogger().info("║           Initializing plugin...             ║");
        getLogger().info("╚══════════════════════════════════════════════╝");

        // 1. LANGUAGE
        getLogger().info("│ 1. Language System");
        getLogger().info("│   ↳ Loading translations...");
        ensureLanguageResources();
        this.language = new LanguageManager(this);

        // 2. REWARDS
        getLogger().info("│ 2. Reward System");
        getLogger().info("│   ↳ Initializing rewards handler...");
        RewardHandler.init(language, this);

        // 3. TIME FORMATTER
        getLogger().info("│ 3. Time Formatter");
        getLogger().info("│   ↳ Setting up time formatting...");
        TimeFormatter.init(this);

        // 4. CONFIGS
        getLogger().info("│ 4. Configurations");
        getLogger().info("│   ↳ Loading GUI configuration...");
        this.guiConfig = new GuiConfig(this);

        getLogger().info("│   ↳ Loading Top configuration...");
        this.topConfig = new TopConfig(this);

        // 5. DATA STORAGE
        getLogger().info("│ 5. Data Storage");
        getLogger().info("│   ↳ Setting up player data storage...");
        this.playerDataStore = new PlayerDataStore(getDataFolder());

        // 5.5 ADDONS (до QuestManager!)
        getLogger().info("│ 5.5 Addons");
        getLogger().info("│   ↳ Loading addons from /Questborn/addons ...");
        this.addonManager = new ua.woody.questborn.addons.AddonManager(this);
        this.addonManager.loadAll();


        // 6. QUEST SYSTEM
        getLogger().info("│ 6. Quest System");
        getLogger().info("│   ↳ Loading quest types and quests...");

        Level originalLevel = getLogger().getLevel();
        try {
            // Прихововуємо INFO-логі саме на період завантаження квестів
            getLogger().setLevel(Level.WARNING);

            this.questManager = new QuestManager(this, playerDataStore);

            questTypesLoaded = questManager.getQuestTypeManager().getEnabledTypes().size();
            questsLoaded = questManager.getAll().size();
            effectPresetsLoaded = getEffectPresetCountSafe();

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize QuestManager", e);
        } finally {
            getLogger().setLevel(Objects.requireNonNullElse(originalLevel, Level.INFO));
        }

        // 7. EVENTS
        getLogger().info("│ 7. Event Registration");
        getLogger().info("│   ↳ Registering GUI click listener...");
        getServer().getPluginManager().registerEvents(new GuiClickListener(this), this);

        if (questManager != null) {
            getLogger().info("│   ↳ Registering quest progress listener...");
            QuestProgressListener questProgressListener = questManager.getQuestProgressListener();
            getServer().getPluginManager().registerEvents(questProgressListener, this);
        } else {
            getLogger().warning("│   ✗ QuestManager is null, quest progress listener not registered!");
        }

        // 8. COMMANDS
        getLogger().info("│ 8. Commands");
        getLogger().info("│   ↳ Setting up /quest command...");

        PluginCommand questCommand = getCommand("quest");
        if (questCommand != null) {
            questCommand.setExecutor(new QuestCommand(this));
            questCommand.setTabCompleter(new QuestTabCompleter(this));
            getLogger().info("│   ✓ Command registered successfully");
        } else {
            getLogger().warning("│   ✗ Failed to register /quest command!");
        }

        // 9. METRICS (bStats)
        getLogger().info("│ 9. Metrics");
        getLogger().info("│   ↳ Setting up bStats metrics...");
        setupMetrics();

        // FINAL STATS
        long endTime = System.currentTimeMillis();
        long loadTime = endTime - startTime;

        getLogger().info("├──────────────────────────────────────────────┤");
        getLogger().info("│           INITIALIZATION COMPLETE            │");
        getLogger().info("├──────────────────────────────────────────────┤");
        getLogger().info("│ • Language: " + languageLoaded);
        getLogger().info("│ • Quest Types: " + questTypesLoaded);
        getLogger().info("│ • Total Quests: " + questsLoaded);
        getLogger().info("│ • Effect Presets: " + effectPresetsLoaded);
        getLogger().info("│ • Top System: " + (topConfig != null && topConfig.isEnabled() ? "✓ ENABLED" : "✗ DISABLED"));
        getLogger().info("│ • Load Time: " + loadTime + "ms");
        getLogger().info("╚══════════════════════════════════════════════╝");

        new UpdateChecker(this, this.language, 130817).checkOnStartup();
    }

    /* ==========================================================
     *   RESTORE MISSING RESOURCES (ON RELOAD)
     * ========================================================== */

    /**
     * ✅ Переконуємось, що language файли існують.
     * Якщо адмін видалив /language або *.yml — витягнемо їх з jar (тільки language/).
     */
    private void ensureLanguageResources() {
        File folder = new File(getDataFolder(), "language");
        File en = new File(folder, "en_us.yml");
        File uk = new File(folder, "uk_ua.yml");
        File de = new File(folder, "de_de.yml");
        File es = new File(folder, "es_es.yml");
        File ru = new File(folder, "ru_ru.yml");

        boolean missing = !folder.exists() || !folder.isDirectory() || !en.exists() || !uk.exists() || !de.exists() || !es.exists() || !ru.exists();
        if (!missing) return;

        extractBundledFolderIfMissing("language");
    }

    /**
     * ✅ Відновлює кореневі конфіги, якщо їх видалили (НЕ перезаписує, якщо існують)
     */
    private void ensureRootConfigsResources() {
        extractBundledFileIfMissing("config.yml");
        extractBundledFileIfMissing("top.yml");
        extractBundledFileIfMissing("quest-effects.yml");
    }

    /**
     * Витягує файли з папки resources/{folderName}/ якщо вони відсутні.
     */
    private void extractBundledFolderIfMissing(String folderName) {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            getLogger().warning("Failed to create plugin data folder: " + dataFolder.getAbsolutePath());
            return;
        }

        int extracted = 0;

        try {
            File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!jarFile.isFile()) return; // IDE run

            String prefix = folderName.toLowerCase(Locale.ROOT) + "/";

            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.isDirectory()) continue;

                    String name = entry.getName();
                    String lower = name.toLowerCase(Locale.ROOT);

                    if (!lower.startsWith(prefix)) continue;

                    // zip-slip protect
                    if (name.contains("..") || name.startsWith("/") || name.startsWith("\\")) continue;

                    File out = new File(dataFolder, name);
                    if (out.exists()) continue;

                    File parent = out.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) continue;

                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        extracted++;
                    }
                }
            }

            if (extracted > 0) {
                getLogger().info("│   ✓ Restored missing resources in /" + folderName + ": " + extracted);
            }

        } catch (Exception e) {
            getLogger().warning("│   ✗ Failed to restore " + folderName + " resources: " + e.getMessage());
        }
    }

    /**
     * ✅ Витягує 1 файл з jar, якщо його нема в dataFolder.
     * Напр: "top.yml" -> {pluginDir}/top.yml
     */
    private void extractBundledFileIfMissing(String relativePath) {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) return;

        File out = new File(dataFolder, relativePath);
        if (out.exists()) return;

        try {
            File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!jarFile.isFile()) return; // IDE run

            String target = relativePath.replace("\\", "/");
            String lowerTarget = target.toLowerCase(Locale.ROOT);

            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.isDirectory()) continue;

                    String name = entry.getName();
                    if (name == null) continue;

                    if (!name.toLowerCase(Locale.ROOT).equals(lowerTarget)) continue;

                    // zip-slip protect
                    if (name.contains("..") || name.startsWith("/") || name.startsWith("\\")) return;

                    File parent = out.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) return;

                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    getLogger().info("│   ✓ Restored missing file: " + relativePath);
                    return;
                }
            }

        } catch (Exception e) {
            getLogger().warning("│   ✗ Failed to restore " + relativePath + ": " + e.getMessage());
        }
    }

    /**
     * ✅ Перезавантажує playerdata.yml ТІЛЬКИ якщо файл існує.
     * Якщо файлу нема — нічого не створює.
     *
     * Працює через reflection (шукає метод reload() або load()).
     */
    private void reloadPlayerDataIfPresentNoCreate() {
        if (playerDataStore == null) return;

        File f = new File(getDataFolder(), "playerdata.yml");
        if (!f.exists()) return;

        boolean invoked = false;

        invoked |= invokeNoArgIfExists(playerDataStore, "reload");
        invoked |= invokeNoArgIfExists(playerDataStore, "load");

        //if (!invoked) {
        //    getLogger().warning("│   ! playerdata.yml exists, but PlayerDataStore has no reload/load method. Restart required to reload it.");
        //}
    }

    private boolean invokeNoArgIfExists(Object target, String methodName) {
        try {
            var m = target.getClass().getMethod(methodName);
            m.invoke(target);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        } catch (Exception e) {
            getLogger().warning("│   ! Failed to call PlayerDataStore#" + methodName + "(): " + e.getMessage());
            return false;
        }
    }

    /* ==========================================================
     *   FIRST INSTALL RESOURCES
     * ========================================================== */

    private void installBundledResourcesOnFirstInstall() {
        File dataFolder = getDataFolder();

        boolean folderMissing = !dataFolder.exists();
        boolean folderEmpty = dataFolder.exists() && dataFolder.isDirectory()
                && Objects.requireNonNullElse(dataFolder.listFiles(), new File[0]).length == 0;

        if (!folderMissing && !folderEmpty) {
            return;
        }

        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            getLogger().warning("Failed to create plugin data folder: " + dataFolder.getAbsolutePath());
            return;
        }

        int extracted = 0;

        try {
            File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!jarFile.isFile()) {
                return; // запуск не з .jar (IDE)
            }

            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.isDirectory()) continue;

                    String name = entry.getName();
                    String lower = name.toLowerCase(Locale.ROOT);

                    if (name.startsWith("META-INF/")) continue;
                    if (name.endsWith(".class")) continue;
                    if (lower.equals("plugin.yml") || lower.equals("paper-plugin.yml") || lower.equals("bungee.yml")) continue;

                    if (!isQuestbornBundledResource(name)) continue;

                    if (name.contains("..") || name.startsWith("/") || name.startsWith("\\")) continue;

                    File out = new File(dataFolder, name);
                    if (out.exists()) continue;

                    File parent = out.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) continue;

                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        extracted++;
                    }
                }
            }

            getLogger().info("│   ✓ Installed default resources: " + extracted);

        } catch (URISyntaxException e) {
            getLogger().warning("│   ✗ Failed to resolve plugin jar location: " + e.getMessage());
        } catch (Exception e) {
            getLogger().warning("│   ✗ Failed to install resources: " + e.getMessage());
        }
    }

    private boolean isQuestbornBundledResource(String path) {
        String lower = path.toLowerCase(Locale.ROOT);

        if (lower.startsWith("language/")) return true;
        if (lower.startsWith("quests/")) return true;
        if (lower.startsWith("types/")) return true;

        return lower.equals("config.yml")
                || lower.equals("top.yml")
                || lower.equals("quest-effects.yml");
    }

    /* ==========================================================
     *   OTHER
     * ========================================================== */

    private void setupMetrics() {
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
                String serverType;

                try {
                    Class.forName("com.destroystokyo.paper.PaperConfig");
                    serverType = "Paper";
                } catch (ClassNotFoundException e) {
                    try {
                        Class.forName("org.spigotmc.SpigotConfig");
                        serverType = "Spigot";
                    } catch (ClassNotFoundException e2) {
                        serverType = "Bukkit";
                    }
                }

                valueMap.put(serverType, 1);
                return valueMap;
            }));

            getLogger().info("│   ✓ Metrics initialized successfully");

        } catch (Exception e) {
            getLogger().warning("│   ✗ Failed to initialize metrics: " + e.getMessage());
        }
    }

    private int getEffectPresetCountSafe() {
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
        getLogger().info("╔══════════════════════════════════════════════╗");
        getLogger().info("║            Reloading Questborn...            ║");
        getLogger().info("╚══════════════════════════════════════════════╝");

        Level originalLevel = getLogger().getLevel();
        try {
            getLogger().setLevel(Level.WARNING);

            // ✅ якщо адмін видалив config/top/quest-effects — відновимо з jar
            ensureRootConfigsResources();

            // ✅ якщо language файли видалили — відновимо
            ensureLanguageResources();

            // ✅ тепер можна reloadConfig() без ризику "порожнього" конфігу
            reloadConfig();
            ObjectiveLoreBuilder.loadFormat(getConfig());

            // ✅ LanguageManager: reload або create
            if (language == null) {
                language = new LanguageManager(this);
            } else {
                language.reload();
            }

            // ✅ RewardHandler має знати нову/оновлену language
            RewardHandler.init(language, this);

            // ✅ перезавантажити playerdata.yml, але НЕ створювати якщо нема
            reloadPlayerDataIfPresentNoCreate();

            if (questManager != null) {
                questManager.reload();
            }

            TimeFormatter.init(this);

            if (topConfig != null) {
                topConfig.reload();
            }

            if (guiConfig != null) {
                guiConfig.reload();
            }

        } finally {
            getLogger().setLevel(Objects.requireNonNullElse(originalLevel, Level.INFO));
        }

        // ✅ reload addons
        if (addonManager != null) {
            addonManager.disableAll();
            addonManager.loadAll();
        }

        getLogger().info("│ ✓ Reload complete!");
        if (questManager != null && topConfig != null) {
            getLogger().info("│ • Quest Types: " + questManager.getQuestTypeManager().getEnabledTypes().size());
            getLogger().info("│ • Total Quests: " + questManager.getAll().size());
            getLogger().info("│ • Top System: " + (topConfig.isEnabled() ? "✓ ENABLED" : "✗ DISABLED"));
        }
        getLogger().info("╚══════════════════════════════════════════════╝");
    }

    public ua.woody.questborn.managers.SoundManager getSoundManager() {
        return questManager != null ? questManager.getSoundManager() : null;
    }

    public GuiConfig getGuiConfig() {
        return guiConfig;
    }

    @Override
    public void onDisable() {
        getLogger().info("╔══════════════════════════════════════════════╗");
        getLogger().info("║            Disabling Questborn...            ║");
        getLogger().info("╚══════════════════════════════════════════════╝");

        if (playerDataStore != null) {
            getLogger().info("│ ↳ Saving player data...");
            playerDataStore.save();
        }

        if (questManager != null) {
            getLogger().info("│ ↳ Shutting down quest manager...");
            questManager.onDisable();
        }

        if (addonManager != null) addonManager.disableAll();

        getLogger().info("│ ✓ Plugin successfully disabled");
        getLogger().info("╚══════════════════════════════════════════════╝");
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public PlayerDataStore getPlayerDataStore() {
        return playerDataStore;
    }

    public LanguageManager getLanguage() {
        return language;
    }

    public TopConfig getTopConfig() {
        return topConfig;
    }
}
