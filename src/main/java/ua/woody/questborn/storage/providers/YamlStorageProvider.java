package ua.woody.questborn.storage.providers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.GlobalQuestProgress;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.TopEntry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class YamlStorageProvider implements StorageProvider {
    private final QuestbornPlugin plugin;
    private final File playersFolder;
    private final File globalQuestsFile;
    private final Object globalFileLock = new Object();

    public YamlStorageProvider(QuestbornPlugin plugin) {
        this.plugin = plugin;
        this.playersFolder = new File(plugin.getDataFolder(), "playerdata");
        this.globalQuestsFile = new File(playersFolder, "global_quests.yml");
    }

    @Override
    public void init() throws Exception {
        if (!playersFolder.exists()) {
            if (!playersFolder.mkdirs()) {
                plugin.getLogger().severe("Could not create playerdata directory!");
            }
        }
        if (!globalQuestsFile.exists()) {
            globalQuestsFile.createNewFile();
        }
    }

    @Override
    public void close() throws Exception {
          }

    @Override
    public CompletableFuture<PlayerQuestProgress> loadPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(playersFolder, uuid.toString() + ".yml");
            if (!file.exists()) {
                return new PlayerQuestProgress();
            }
            YamlConfiguration sec = YamlConfiguration.loadConfiguration(file);
            return deserialize(sec);
        });
    }

    @Override
    public CompletableFuture<Boolean> savePlayer(UUID uuid, PlayerQuestProgress p) {
        return CompletableFuture.supplyAsync(() -> {
            File finalFile = new File(playersFolder, uuid.toString() + ".yml");
            File tmpFile = new File(playersFolder, uuid.toString() + ".yml.tmp");
            YamlConfiguration sec = new YamlConfiguration();

            if (!p.getActiveQuests().isEmpty()) {
                ConfigurationSection aqSec = sec.createSection("active-quests");
                for (Map.Entry<String, PlayerQuestProgress.ActiveQuestData> entry : p.getActiveQuests().entrySet()) {
                    ConfigurationSection qs = aqSec.createSection(entry.getKey());
                    PlayerQuestProgress.ActiveQuestData qd = entry.getValue();
                    qs.set("start", qd.getStartTime());
                    qs.set("progress", qd.getProgress());
                    qs.set("stage", qd.getCurrentStage());
                    qs.set("stage-progress", qd.getStageProgress());
                    qs.set("items", qd.isItemsTransferred());
                    qs.set("paused", qd.isPaused());
                    if (!qd.getCompletedStages().isEmpty()) {
                        ConfigurationSection cs = qs.createSection("stages");
                        for (Map.Entry<Integer, Boolean> e : qd.getCompletedStages().entrySet()) {
                            cs.set(e.getKey().toString(), e.getValue());
                        }
                    }
                }
            }
            sec.set("tracked-quest-id", p.getTrackedQuestId());

            sec.set("travel-buffer", p.getTravelBuffer());
            sec.set("pending-quest-id", p.getPendingQuestId());

            ConfigurationSection completedSec = sec.createSection("completed");
            for (Map.Entry<String, Integer> entry : p.getCompletedByType().entrySet()) {
                completedSec.set(entry.getKey(), entry.getValue());
            }

            sec.set("completed-quests", new ArrayList<>(p.getCompletedQuests()));

            sec.set("claimed-rewards", new ArrayList<>(p.getClaimedRewards()));
            sec.set("pending-rewards", new ArrayList<>(p.getPendingRewards()));

            ConfigurationSection cdSec = sec.createSection("cooldowns");
            for (Map.Entry<String, Long> entry : p.getQuestCooldowns().entrySet()) {
                cdSec.set(entry.getKey(), entry.getValue());
            }

            ConfigurationSection ltcSec = sec.createSection("last-type-completion");
            for (Map.Entry<String, Long> entry : p.getLastTypeCompletionMap().entrySet()) {
                ltcSec.set(entry.getKey(), entry.getValue());
            }

            sec.set("skin-texture", p.getSkinTexture());
            sec.set("skin-signature", p.getSkinSignature());

            if (!p.getAssignedRotationQuestsMap().isEmpty()) {
                ConfigurationSection rotQuestsSec = sec.createSection("rotation-quests");
                for (Map.Entry<String, List<String>> entry : p.getAssignedRotationQuestsMap().entrySet()) {
                    rotQuestsSec.set(entry.getKey(), entry.getValue());
                }
            }
            if (!p.getRotationAssignedAtMap().isEmpty()) {
                ConfigurationSection rotTimeSec = sec.createSection("rotation-assigned-at");
                for (Map.Entry<String, Long> entry : p.getRotationAssignedAtMap().entrySet()) {
                    rotTimeSec.set(entry.getKey(), entry.getValue());
                }
            }

            try {
                sec.save(tmpFile);
                Files.move(tmpFile.toPath(), finalFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                return true;
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save player data for " + uuid);
                e.printStackTrace();

                if (tmpFile.exists())
                    tmpFile.delete();
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<List<TopEntry>> getTopPlayers(int limit, Set<String> enabledTypeIds) {
        return CompletableFuture.supplyAsync(() -> {
            File[] files = playersFolder.listFiles(
                    f -> f.isFile() && f.getName().endsWith(".yml") && !f.getName().equals("global_quests.yml"));

            if (files == null || files.length == 0)
                return Collections.emptyList();

            List<TopEntry> list = new ArrayList<>();

            for (File file : files) {
                String name = file.getName().replace(".yml", "");
                UUID uuid;
                try {
                    uuid = UUID.fromString(name);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                try {
                    YamlConfiguration sec = YamlConfiguration.loadConfiguration(file);
                    PlayerQuestProgress p = deserialize(sec);

                    int total = 0;
                    for (Map.Entry<String, Integer> e : p.getCompletedByType().entrySet()) {
                        if (enabledTypeIds == null || enabledTypeIds.contains(e.getKey())) {
                            total += e.getValue();
                        }
                    }

                    if (total > 0) {
                        String playerName = Optional
                                .ofNullable(Bukkit.getOfflinePlayer(uuid).getName())
                                .orElse(name.substring(0, 8));
                        String texture = p.getSkinTexture();
                        String signature = p.getSkinSignature();
                        list.add(new TopEntry(uuid, playerName, total, texture, signature));
                    }
                } catch (Exception ignored) {
                }
            }

            Collections.sort(list);
            if (list.size() > limit)
                return list.subList(0, limit);
            return list;
        });
    }

    private static PlayerQuestProgress deserialize(YamlConfiguration sec) {
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

        ConfigurationSection rotQuestsSec = sec.getConfigurationSection("rotation-quests");
        if (rotQuestsSec != null) {
            for (String typeId : rotQuestsSec.getKeys(false)) {
                p.setAssignedRotationQuests(typeId, rotQuestsSec.getStringList(typeId));
            }
        }
        ConfigurationSection rotTimeSec = sec.getConfigurationSection("rotation-assigned-at");
        if (rotTimeSec != null) {
            for (String typeId : rotTimeSec.getKeys(false)) {
                p.setRotationAssignedAt(typeId, rotTimeSec.getLong(typeId, 0L));
            }
        }

        return p;
    }

    @Override
    public CompletableFuture<GlobalQuestProgress> loadGlobalQuest(String questId) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (globalFileLock) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(globalQuestsFile);
                ConfigurationSection sec = config.getConfigurationSection(questId);
                if (sec == null) {
                    return new GlobalQuestProgress(questId);
                }
                GlobalQuestProgress p = new GlobalQuestProgress(questId);
                p.setGlobalProgress(sec.getInt("progress", 0));
                p.setCompleted(sec.getBoolean("completed", false));
                p.setActivationReached(sec.getBoolean("activationReached", false));
                long startedAt = sec.getLong("startedAt", 0L);
                if (p.getGlobalProgress() == 0 || p.isCompleted()) {
                    startedAt = 0L;
                }
                p.setStartedAt(startedAt);

                ConfigurationSection contSec = sec.getConfigurationSection("contributions");
                if (contSec != null) {
                    for (String u : contSec.getKeys(false)) {
                        try {
                            p.setContribution(UUID.fromString(u), contSec.getInt(u));
                        } catch (Exception ignored) {}
                    }
                } else {
                    List<String> uuids = sec.getStringList("participants");
                    for (String u : uuids) {
                        try {
                            p.setContribution(UUID.fromString(u), 0);
                        } catch (Exception ignored) {}
                    }
                }

                ConfigurationSection timeSec = sec.getConfigurationSection("lastContributionTimes");
                if (timeSec != null) {
                    for (String u : timeSec.getKeys(false)) {
                        try {
                            p.setLastContributionTime(UUID.fromString(u), timeSec.getLong(u));
                        } catch (Exception ignored) {}
                    }
                }

                List<String> forfeitedList = sec.getStringList("forfeitedPlayers");
                if (forfeitedList != null) {
                    for (String u : forfeitedList) {
                        try {
                            p.setForfeited(UUID.fromString(u), true);
                        } catch (Exception ignored) {}
                    }
                }
                return p;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> saveGlobalQuest(String questId, GlobalQuestProgress progress) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (globalFileLock) {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(globalQuestsFile);
                    ConfigurationSection sec = config.createSection(questId);
                    sec.set("progress", progress.getGlobalProgress());
                    sec.set("completed", progress.isCompleted());
                    sec.set("activationReached", progress.isActivationReached());
                    sec.set("startedAt", progress.getStartedAt());

                    ConfigurationSection contSec = sec.createSection("contributions");
                    for (Map.Entry<UUID, Integer> entry : progress.getContributions().entrySet()) {
                        contSec.set(entry.getKey().toString(), entry.getValue());
                    }

                    ConfigurationSection timeSec = sec.createSection("lastContributionTimes");
                    for (Map.Entry<UUID, Long> entry : progress.getLastContributionTimes().entrySet()) {
                        timeSec.set(entry.getKey().toString(), entry.getValue());
                    }

                    List<String> forfeitedList = new ArrayList<>();
                    for (UUID u : progress.getForfeitedPlayers()) {
                        forfeitedList.add(u.toString());
                    }
                    sec.set("forfeitedPlayers", forfeitedList);

                    config.save(globalQuestsFile);
                    return true;
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to save global quest " + questId + ": " + e.getMessage());
                    return false;
                }
            }
        });
    }

    @Override
    public CompletableFuture<Map<String, GlobalQuestProgress>> loadAllGlobalQuests() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, GlobalQuestProgress> map = new HashMap<>();
            synchronized (globalFileLock) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(globalQuestsFile);
                for (String key : config.getKeys(false)) {
                    ConfigurationSection sec = config.getConfigurationSection(key);
                    if (sec != null) {
                        GlobalQuestProgress p = new GlobalQuestProgress(key);
                        p.setGlobalProgress(sec.getInt("progress", 0));
                        p.setCompleted(sec.getBoolean("completed", false));
                        p.setActivationReached(sec.getBoolean("activationReached", false));
                        long startedAt = sec.getLong("startedAt", 0L);
                        if (p.getGlobalProgress() == 0 || p.isCompleted()) {
                            startedAt = 0L;
                        }
                        p.setStartedAt(startedAt);

                        ConfigurationSection contSec = sec.getConfigurationSection("contributions");
                        if (contSec != null) {
                            for (String u : contSec.getKeys(false)) {
                                try {
                                    p.setContribution(UUID.fromString(u), contSec.getInt(u));
                                } catch (Exception ignored) {}
                            }
                        } else {
                            List<String> uuids = sec.getStringList("participants");
                            for (String u : uuids) {
                                try {
                                    p.setContribution(UUID.fromString(u), 0);
                                } catch (Exception ignored) {}
                            }
                        }

                        ConfigurationSection timeSec = sec.getConfigurationSection("lastContributionTimes");
                        if (timeSec != null) {
                            for (String u : timeSec.getKeys(false)) {
                                try {
                                    p.setLastContributionTime(UUID.fromString(u), timeSec.getLong(u));
                                } catch (Exception ignored) {}
                            }
                        }
                        map.put(key, p);
                    }
                }
            }
            return map;
        });
    }
}
