package ua.woody.questborn.storage;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.storage.providers.MySQLStorageProvider;
import ua.woody.questborn.storage.providers.SqliteStorageProvider;
import ua.woody.questborn.storage.providers.StorageProvider;
import ua.woody.questborn.storage.providers.YamlStorageProvider;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class PlayerDataStore {
    private final QuestbornPlugin plugin;
    private final StorageProvider provider;

    private final Map<UUID, PlayerQuestProgress> cache = new ConcurrentHashMap<>();

    private final Map<UUID, AtomicBoolean> dirtyFlags = new ConcurrentHashMap<>();

    private final Set<UUID> pendingLoads = ConcurrentHashMap.newKeySet();

    private com.tcoded.folialib.wrapper.task.WrappedTask saveTask = null;
    private boolean autoSaveEnabled = true;
    private int autoSaveInterval = 20 * 30;

    public PlayerDataStore(QuestbornPlugin plugin, File dataFolder) {
        this.plugin = plugin;
        this.provider = createProvider(plugin);

        try {
            provider.init();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize StorageProvider!", e);
        }

        migrateLegacyFile(new File(dataFolder, "playerdata.yml"));
        migrateLegacyGlobalQuestsFile(new File(dataFolder, "global_quests.yml"));

        for (Player p : Bukkit.getOnlinePlayers()) {
            loadPlayerSync(p.getUniqueId());
        }

        startAutoSaveTask();
    }

    private StorageProvider createProvider(QuestbornPlugin plugin) {
        String type = plugin.getConfig().getString("storage.type", "H2").toUpperCase(Locale.ROOT).trim();
        switch (type) {
            case "MONGODB" -> {
                plugin.getLogger().info(QuestbornPlugin.WHITE + "  ↳ Storage: " + QuestbornPlugin.LIGHT_BLUE + "MONGODB"
                        + QuestbornPlugin.WHITE + " (MongoDB)" + QuestbornPlugin.RESET);
                return new ua.woody.questborn.storage.providers.MongoStorageProvider(plugin);
            }
            case "MYSQL" -> {
                plugin.getLogger().info(QuestbornPlugin.WHITE + "  ↳ Storage: " + QuestbornPlugin.LIGHT_BLUE + "MYSQL"
                        + QuestbornPlugin.WHITE + " (MySQL / MariaDB)" + QuestbornPlugin.RESET);
                return new MySQLStorageProvider(plugin);
            }
            case "POSTGRESQL" -> {
                plugin.getLogger()
                        .info(QuestbornPlugin.WHITE + "  ↳ Storage: " + QuestbornPlugin.LIGHT_BLUE + "POSTGRESQL"
                                + QuestbornPlugin.WHITE + " (PostgreSQL)" + QuestbornPlugin.RESET);
                return new ua.woody.questborn.storage.providers.PostgreSQLStorageProvider(plugin);
            }
            case "SQLITE" -> {
                plugin.getLogger().info(QuestbornPlugin.WHITE + "  ↳ Storage: " + QuestbornPlugin.LIGHT_BLUE + "SQLITE"
                        + QuestbornPlugin.WHITE + " (file-based SQLite)" + QuestbornPlugin.RESET);
                return new SqliteStorageProvider(plugin);
            }
            case "YAML" -> {
                plugin.getLogger().info(QuestbornPlugin.WHITE + "  ↳ Storage: " + QuestbornPlugin.LIGHT_BLUE + "YAML"
                        + QuestbornPlugin.WHITE + " (per-player files)" + QuestbornPlugin.RESET);
                return new YamlStorageProvider(plugin);
            }
            default -> {
                if (!type.equals("H2")) {
                    plugin.getLogger().warning(QuestbornPlugin.ORANGE + "  Unknown storage.type '" + type
                            + "', using H2." + QuestbornPlugin.RESET);
                }
                plugin.getLogger().info(QuestbornPlugin.WHITE + "  ↳ Storage: " + QuestbornPlugin.LIGHT_BLUE + "H2"
                        + QuestbornPlugin.WHITE + " (file-based H2)" + QuestbornPlugin.RESET);
                return new ua.woody.questborn.storage.providers.H2StorageProvider(plugin);
            }
        }
    }

    private void migrateLegacyFile(File legacyFile) {
        if (!legacyFile.exists())
            return;

        plugin.getLogger().info(QuestbornPlugin.WHITE + "  ↳ Migrating playerdata.yml → new database..."
                + QuestbornPlugin.RESET);

        YamlConfiguration legacyYaml = YamlConfiguration.loadConfiguration(legacyFile);
        ConfigurationSection playersSec = legacyYaml.getConfigurationSection("players");

        if (playersSec == null) {
            plugin.getLogger().info(QuestbornPlugin.WHITE
                    + "  ↳ playerdata.yml has no 'players' section – skipping migration." + QuestbornPlugin.RESET);
            renameLegacy(legacyFile);
            return;
        }

        int migrated = 0;
        for (String key : playersSec.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }

            ConfigurationSection sec = playersSec.getConfigurationSection(key);
            if (sec == null)
                continue;

            PlayerQuestProgress p = deserializeSection(sec);

            try {
                provider.savePlayer(uuid, p).get();
                migrated++;
            } catch (Exception e) {
                plugin.getLogger().warning(QuestbornPlugin.ORANGE + "Could not migrate data for UUID " + uuid + ": "
                        + e.getMessage() + QuestbornPlugin.RESET);
            }
        }

        plugin.getLogger()
                .info(QuestbornPlugin.WHITE + "  ↳ Migration complete: " + QuestbornPlugin.LIGHT_BLUE + migrated
                        + QuestbornPlugin.WHITE + " players migrated." + QuestbornPlugin.RESET);
        renameLegacy(legacyFile);
    }

    private void renameLegacy(File legacyFile) {
        File backup = new File(legacyFile.getParentFile(), "playerdata.yml.backup");
        if (legacyFile.renameTo(backup)) {
            plugin.getLogger().info(QuestbornPlugin.WHITE + "  ↳ Old playerdata.yml renamed to "
                    + QuestbornPlugin.LIGHT_BLUE + "playerdata.yml.backup" + QuestbornPlugin.RESET);
        } else {
            plugin.getLogger().warning(QuestbornPlugin.ORANGE
                    + "Could not rename playerdata.yml – please remove it manually." + QuestbornPlugin.RESET);
        }
    }

    private void migrateLegacyGlobalQuestsFile(File legacyFile) {
        if (!legacyFile.exists()) return;

        plugin.getLogger().info(QuestbornPlugin.WHITE + "  ↳ Migrating global_quests.yml → new database..." + QuestbornPlugin.RESET);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(legacyFile);
        int migrated = 0;
        for (String key : config.getKeys(false)) {
            ConfigurationSection sec = config.getConfigurationSection(key);
            if (sec != null) {
                ua.woody.questborn.model.GlobalQuestProgress p = new ua.woody.questborn.model.GlobalQuestProgress(key);
                p.setGlobalProgress(sec.getInt("progress", 0));
                p.setCompleted(sec.getBoolean("completed", false));
                p.setStartedAt(sec.getLong("startedAt", 0L));
                ConfigurationSection contSec = sec.getConfigurationSection("contributions");
                if (contSec != null) {
                    for (String u : contSec.getKeys(false)) {
                        try { p.setContribution(UUID.fromString(u), contSec.getInt(u)); } catch (Exception ignored) {}
                    }
                } else {
                    List<String> uuids = sec.getStringList("participants");
                    for (String u : uuids) {
                        try { p.setContribution(UUID.fromString(u), 0); } catch (Exception ignored) {}
                    }
                }
                ConfigurationSection timeSec = sec.getConfigurationSection("lastContributionTimes");
                if (timeSec != null) {
                    for (String u : timeSec.getKeys(false)) {
                        try { p.setLastContributionTime(UUID.fromString(u), timeSec.getLong(u)); } catch (Exception ignored) {}
                    }
                }
                try {
                    provider.saveGlobalQuest(key, p).get();
                    migrated++;
                } catch (Exception e) {
                    plugin.getLogger().warning(QuestbornPlugin.ORANGE + "Could not migrate global quest " + key + ": " + e.getMessage() + QuestbornPlugin.RESET);
                }
            }
        }
        plugin.getLogger().info(QuestbornPlugin.WHITE + "  ↳ Migration complete: " + QuestbornPlugin.LIGHT_BLUE + migrated + QuestbornPlugin.WHITE + " global quests migrated." + QuestbornPlugin.RESET);
        File backup = new File(legacyFile.getParentFile(), "global_quests.yml.backup");
        legacyFile.renameTo(backup);
    }

    public CompletableFuture<Void> loadPlayerAsync(UUID uuid) {
        if (cache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(null);
        }
        pendingLoads.add(uuid);
        return provider.loadPlayer(uuid).thenAccept(progress -> {
            if (pendingLoads.remove(uuid)) {
                cache.put(uuid, progress);
                dirtyFlags.put(uuid, new AtomicBoolean(false));

                org.bukkit.entity.Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    plugin.getFoliaLib().getImpl().runAtEntity(player, __ -> {
                        checkOfflineGlobalQuestCompletions(player, progress);
                        if (plugin.getQuestManager() != null) {
                            plugin.getQuestManager().refreshPlayerUI(player);
                        }
                    });
                }
            }

        }).exceptionally(ex -> {
            pendingLoads.remove(uuid);
            plugin.getLogger().log(Level.SEVERE, "Failed to load data for " + uuid, ex);

            if (cache.get(uuid) == null) {
                cache.put(uuid, new PlayerQuestProgress());
                dirtyFlags.put(uuid, new AtomicBoolean(false));
            }
            return null;
        });
    }

    public void loadPlayerSync(UUID uuid) {
        try {
            PlayerQuestProgress progress = provider.loadPlayer(uuid).get();
            cache.put(uuid, progress);
            dirtyFlags.put(uuid, new AtomicBoolean(false));

            org.bukkit.entity.Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                checkOfflineGlobalQuestCompletions(player, progress);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load data for " + uuid, e);
            cache.put(uuid, new PlayerQuestProgress());
            dirtyFlags.put(uuid, new AtomicBoolean(false));
        }
    }

    private void checkOfflineGlobalQuestCompletions(Player player, PlayerQuestProgress data) {
        if (plugin.getGlobalQuestDataStore() == null || plugin.getQuestManager() == null) return;
        List<String> completedToProcess = new ArrayList<>();

        for (String activeQuestId : new ArrayList<>(data.getActiveQuests().keySet())) {
            ua.woody.questborn.model.QuestDefinition def = plugin.getQuestManager().getQuest(activeQuestId);
            if (def == null) continue;

            ua.woody.questborn.model.QuestTypeConfig tc = plugin.getQuestManager().getQuestTypeManager().getType(def.getTypeId());
            if (tc != null && tc.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL) {
                ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(activeQuestId);
                if (gp != null && gp.isCompleted()) {
                    completedToProcess.add(activeQuestId);
                }
            }
        }

        for (String questId : completedToProcess) {
            ua.woody.questborn.model.QuestDefinition def = plugin.getQuestManager().getQuest(questId);
            if (def != null) {
                ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(questId);
                int contribution = gp != null ? gp.getContribution(player.getUniqueId()) : 0;

                if (contribution > 0) {
                    Map<String, Object> allRewards = def.getRewards();
                    Map<String, Object> maxContributionBonus = null;
                    if (allRewards != null) {
                        Object obj = allRewards.get("max-contribution-bonus");
                        if (obj instanceof Map) {
                            maxContributionBonus = (Map<String, Object>) obj;
                        } else if (obj instanceof org.bukkit.configuration.ConfigurationSection) {
                            maxContributionBonus = ((org.bukkit.configuration.ConfigurationSection) obj).getValues(false);
                        }
                    }
                    if (maxContributionBonus != null && def.getPersonalLimit() > 0 && contribution >= def.getPersonalLimit()) {
                        ua.woody.questborn.rewards.RewardHandler.giveCustomRewards(player, def, maxContributionBonus);
                    }

                    plugin.getQuestManager().getProgressProcessor().completeQuest(player, def);
                } else {
                    data.removeActiveQuest(questId);
                    markDirty(player.getUniqueId());
                }
            }
        }
    }

    public void evictPlayer(UUID uuid) {
        PlayerQuestProgress snapshot = cache.remove(uuid);
        AtomicBoolean flag = dirtyFlags.remove(uuid);

        boolean wasLoading = pendingLoads.remove(uuid);

        if (snapshot != null && flag != null && flag.get()) {
            boolean debugLog = plugin.getConfig().getBoolean("optimization.debug.auto-save-log", false);
            provider.savePlayer(uuid, snapshot).thenRun(() -> {
                if (debugLog) {
                    plugin.getLogger().info("[DEBUG] Player quit: saved data for " + uuid);
                }
            }).exceptionally(ex -> {
                plugin.getLogger().log(Level.SEVERE, "Failed to save data on quit for " + uuid, ex);
                return null;
            });
        } else if (snapshot != null && (flag == null || !flag.get())) {
        } else if (wasLoading) {
            plugin.getLogger().fine("Player " + uuid + " quit before data loaded — nothing to save.");
        }
    }

    public PlayerQuestProgress get(UUID uuid) {
        PlayerQuestProgress cached = cache.get(uuid);
        if (cached != null) {
            return cached;
        }

        return new PlayerQuestProgress();
    }

    public PlayerQuestProgress getCachedOrNull(UUID uuid) {
        return cache.get(uuid);
    }

    public String[] loadSkinFromFile(UUID uuid) {
        PlayerQuestProgress cached = cache.get(uuid);
        if (cached != null) {
            String texture = cached.getSkinTexture();
            if (texture == null)
                return null;
            return new String[] { texture, cached.getSkinSignature() };
        }

        File file = new File(plugin.getDataFolder(), "playerdata/" + uuid + ".yml");
        if (!file.exists())
            return null;
        try {
            YamlConfiguration sec = YamlConfiguration.loadConfiguration(file);
            String texture = sec.getString("skin-texture", null);
            if (texture == null)
                return null;
            String signature = sec.getString("skin-signature", null);
            return new String[] { texture, signature };
        } catch (Exception e) {
            return null;
        }
    }

    public Map<UUID, PlayerQuestProgress> getAll() {
        return cache;
    }

    public void markDirty(UUID uuid) {
        if (!cache.containsKey(uuid))
            return;
        dirtyFlags.computeIfAbsent(uuid, k -> new AtomicBoolean(false)).set(true);
    }

    public void updatePlayerData(UUID uuid, PlayerQuestProgress progress) {
        cache.put(uuid, progress);
        markDirty(uuid);
    }

    public void saveIfDirty(UUID uuid) {
        AtomicBoolean flag = dirtyFlags.get(uuid);
        if (flag != null && flag.compareAndSet(true, false)) {
            PlayerQuestProgress progress = cache.get(uuid);
            if (progress != null) {
                provider.savePlayer(uuid, progress).exceptionally(ex -> {
                    plugin.getLogger().log(Level.SEVERE, "Failed to save data for " + uuid, ex);
                    flag.set(true);
                    return false;
                });
            }
        }
    }

    public void save() {
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (Map.Entry<UUID, PlayerQuestProgress> entry : cache.entrySet()) {
            UUID uuid = entry.getKey();
            AtomicBoolean flag = dirtyFlags.get(uuid);

            if (flag != null && flag.get()) {
                futures.add(provider.savePlayer(uuid, entry.getValue()));
            }
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during bulk save!", e);
        }

        dirtyFlags.values().forEach(f -> f.set(false));
    }

    public void clearCache() {
        cache.clear();
        dirtyFlags.clear();
        pendingLoads.clear();
    }

    private void startAutoSaveTask() {
        if (saveTask != null) {
            saveTask.cancel();
        }

        saveTask = plugin.getFoliaLib().getImpl().runTimerAsync(() -> {
            if (!autoSaveEnabled)
                return;

            boolean debugLog = plugin.getConfig().getBoolean("optimization.debug.auto-save-log", false);
            int saved = 0;

            for (UUID uuid : new HashSet<>(dirtyFlags.keySet())) {
                AtomicBoolean flag = dirtyFlags.get(uuid);
                if (flag != null && flag.compareAndSet(true, false)) {
                    PlayerQuestProgress progress = cache.get(uuid);
                    if (progress != null) {
                        final UUID finalUuid = uuid;
                        provider.savePlayer(uuid, progress).exceptionally(ex -> {
                            plugin.getLogger().log(Level.WARNING, "Auto-save failed for " + finalUuid, ex);
                            flag.set(true);
                            return false;
                        });
                        saved++;
                    }
                }
            }

            if (debugLog && saved > 0) {
                plugin.getLogger().info("[DEBUG] Auto-save: saved " + saved + " players.");
            }
        }, autoSaveInterval, autoSaveInterval);
    }

    public void stopAutoSave() {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
    }

    public void setAutoSaveEnabled(boolean enabled) {
        this.autoSaveEnabled = enabled;
        if (enabled) {
            startAutoSaveTask();
        } else {
            stopAutoSave();
        }
    }

    public void setAutoSaveInterval(int ticks) {
        this.autoSaveInterval = ticks;
        if (autoSaveEnabled) {
            startAutoSaveTask();
        }
    }

    public void onDisable() {
        stopAutoSave();
        save();
        try {
            provider.close();
        } catch (Exception e) {
            plugin.getLogger().warning("Error closing storage provider: " + e.getMessage());
        }
    }

    public StorageProvider getStorageProvider() {
        return provider;
    }

    private static PlayerQuestProgress deserializeSection(ConfigurationSection sec) {
        PlayerQuestProgress p = new PlayerQuestProgress();
        ConfigurationSection aqSec = sec.getConfigurationSection("active-quests");
        if (aqSec != null) {
            for (String qid : aqSec.getKeys(false)) {
                ConfigurationSection qs = aqSec.getConfigurationSection(qid);
                PlayerQuestProgress.ActiveQuestData qd = new PlayerQuestProgress.ActiveQuestData(qid, qs.getLong("start"));
                qd.setProgress(qs.getInt("progress"));
                qd.setCurrentStage(qs.getInt("stage"));
                qd.setStageProgress(qs.getInt("stage-progress"));
                qd.setItemsTransferred(qs.getBoolean("items"));
                qd.setPaused(qs.getBoolean("paused", false));
                ConfigurationSection cs = qs.getConfigurationSection("stages");
                if (cs != null) {
                    for (String s : cs.getKeys(false)) {
                        try { qd.setStageCompleted(Integer.parseInt(s), cs.getBoolean(s)); } catch (Exception ignored) {}
                    }
                }
                p.addActiveQuest(qd);
            }
        } else {
            String legacyId = sec.getString("active-quest-id");
            if (legacyId != null && !legacyId.isEmpty()) {
                PlayerQuestProgress.ActiveQuestData qd = new PlayerQuestProgress.ActiveQuestData(legacyId, sec.getLong("active-quest-start"));
                qd.setProgress(sec.getInt("active-quest-progress"));
                qd.setCurrentStage(sec.getInt("current-stage", 1));
                qd.setStageProgress(sec.getInt("stage-progress"));
                qd.setItemsTransferred(sec.getBoolean("items-transferred"));
                ConfigurationSection cs = sec.getConfigurationSection("completed-stages");
                if (cs != null) {
                    for (String s : cs.getKeys(false)) {
                        try { qd.setStageCompleted(Integer.parseInt(s), cs.getBoolean(s)); } catch (Exception ignored) {}
                    }
                }
                p.addActiveQuest(qd);
                p.setTrackedQuestId(legacyId);
            }
        }

        p.setTrackedQuestId(sec.getString("tracked-quest-id", null));
        p.setTravelBuffer(sec.getDouble("travel-buffer", 0.0));
        p.setPendingQuestId(sec.getString("pending-quest-id", null));

        ConfigurationSection completedSec = sec.getConfigurationSection("completed");
        if (completedSec != null) {
            for (String typeId : completedSec.getKeys(false)) {
                try {
                    p.getCompletedByType().put(typeId.toLowerCase(Locale.ROOT), completedSec.getInt(typeId, 0));
                } catch (Exception ignored) {
                }
            }
        }

        List<String> completedIds = sec.getStringList("completed-quests");
        completedIds.forEach(p::addCompletedQuest);

        if (sec.contains("claimed-rewards")) {
            sec.getStringList("claimed-rewards").forEach(qid -> p.setRewardClaimed(qid, true));
        } else {
            completedIds.forEach(qid -> p.setRewardClaimed(qid, true));
        }

        sec.getStringList("pending-rewards").forEach(p::addPendingReward);

        ConfigurationSection cdSec = sec.getConfigurationSection("cooldowns");
        if (cdSec != null) {
            for (String qid : cdSec.getKeys(false))
                p.setQuestCooldownUntil(qid, cdSec.getLong(qid, 0L));
        }

        ConfigurationSection ltcSec = sec.getConfigurationSection("last-type-completion");
        if (ltcSec != null) {
            for (String typeId : ltcSec.getKeys(false))
                p.setLastTypeCompletion(typeId, ltcSec.getLong(typeId, 0L));
        }

        p.setSkinTexture(sec.getString("skin-texture", null));
        p.setSkinSignature(sec.getString("skin-signature", null));
        return p;
    }
}
