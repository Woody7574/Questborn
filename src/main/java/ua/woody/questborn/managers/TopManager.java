package ua.woody.questborn.managers;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.model.TopEntry;
import ua.woody.questborn.storage.PlayerDataStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TopManager {
    private final QuestbornPlugin plugin;
    private final PlayerDataStore playerData;
    private int topSize;
    private int updateInterval = 6000;

    private final List<TopEntry> cachedTop = new CopyOnWriteArrayList<>();
    private final Map<UUID, Integer> playerRanks = new ConcurrentHashMap<>();
    private com.tcoded.folialib.wrapper.task.WrappedTask updateTask;

    public TopManager(QuestbornPlugin plugin) {
        this.plugin = plugin;
        this.playerData = plugin.getPlayerDataStore();
        this.topSize = plugin.getTopConfig().getSize();
        startUpdateTask();
    }

    public void setTopSize(int topSize) {
        this.topSize = topSize;
    }

    public void setUpdateInterval(int ticks) {
        if (this.updateInterval != ticks) {
            this.updateInterval = ticks;
            startUpdateTask();
        }
    }

    public void startUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        updateTask = plugin.getFoliaLib().getImpl().runTimerAsync(this::refreshTop, 1L,
                (long) updateInterval);
    }

    public void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }
    }

    public void refreshTop() {
        try {
            Set<String> enabledTypeIds = new java.util.HashSet<>();
            try {
                for (ua.woody.questborn.model.QuestTypeConfig t : plugin.getQuestManager().getQuestTypeManager()
                        .getEnabledTypes()) {
                    enabledTypeIds.add(t.getId().toLowerCase(java.util.Locale.ROOT));
                }
            } catch (Exception ignored) {
                  }

            List<TopEntry> fileEntries = playerData.getStorageProvider()
                    .getTopPlayers(Integer.MAX_VALUE, enabledTypeIds.isEmpty() ? null : enabledTypeIds).get();

            Map<UUID, TopEntry> entryMap = new java.util.LinkedHashMap<>();
            for (TopEntry e : fileEntries) {
                entryMap.put(e.getUuid(), e);
            }

            for (Map.Entry<UUID, PlayerQuestProgress> ce : playerData.getAll().entrySet()) {
                UUID uuid = ce.getKey();
                PlayerQuestProgress progress = ce.getValue();
                int total = 0;
                for (Map.Entry<String, Integer> e : progress.getCompletedByType().entrySet()) {
                    if (enabledTypeIds.isEmpty() || enabledTypeIds.contains(e.getKey())) {
                        total += e.getValue();
                    }
                }
                if (total > 0) {
                    String name = Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName())
                            .orElse(uuid.toString().substring(0, 8));
                    String texture = progress.getSkinTexture();
                    String signature = progress.getSkinSignature();
                    entryMap.put(uuid, new TopEntry(uuid, name, total, texture, signature));
                }
            }

            List<TopEntry> filtered = new ArrayList<>(entryMap.values());
            Collections.sort(filtered);

            cachedTop.clear();
            playerRanks.clear();

            int size = Math.min(filtered.size(), topSize);
            for (int i = 0; i < size; i++) {
                TopEntry entry = filtered.get(i);
                cachedTop.add(entry);
                playerRanks.put(entry.getUuid(), i + 1);
            }
            for (int i = size; i < filtered.size(); i++) {
                playerRanks.put(filtered.get(i).getUuid(), i + 1);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Questborn] Failed to refresh top: " + e.getMessage());
        }
    }

    public List<TopEntry> getCachedTop() {
        return new ArrayList<>(cachedTop);
    }

    public int getPlayerRank(UUID uuid) {
        return playerRanks.getOrDefault(uuid, -1);
    }

    public int getTotalCompletedQuests(UUID playerId) {
        PlayerQuestProgress progress = playerData.get(playerId);
        if (progress == null)
            return 0;
        int total = 0;
        for (int count : progress.getCompletedByType().values())
            total += count;
        return total;
    }

    public Map<String, Integer> getCompletedByType(UUID playerId) {
        PlayerQuestProgress progress = playerData.get(playerId);
        return progress != null ? progress.getCompletedByType() : new HashMap<>();
    }
}
