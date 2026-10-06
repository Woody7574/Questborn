package ua.woody.questborn.storage;

import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.GlobalQuestProgress;
import ua.woody.questborn.storage.providers.StorageProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class GlobalQuestDataStore {
    private final QuestbornPlugin plugin;
    private final StorageProvider provider;
    private final Map<String, GlobalQuestProgress> cache = new ConcurrentHashMap<>();

    private com.tcoded.folialib.wrapper.task.WrappedTask saveTask = null;
    private boolean autoSaveEnabled = true;
    private int autoSaveInterval = 20 * 60;

    public GlobalQuestDataStore(QuestbornPlugin plugin, StorageProvider provider) {
        this.plugin = plugin;
        this.provider = provider;

        loadAllSync();
        startAutoSaveTask();
    }

    public void reload() {
        saveAll();
        cache.clear();
        loadAllSync();
    }

    private void loadAllSync() {
        try {
            Map<String, GlobalQuestProgress> loaded = provider.loadAllGlobalQuests().get();
            cache.putAll(loaded);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load global quests", e);
        }
    }

    public GlobalQuestProgress get(String questId) {
        return cache.computeIfAbsent(questId, k -> new GlobalQuestProgress(k));
    }

    public void save(String questId) {
        GlobalQuestProgress p = cache.get(questId);
        if (p != null) {
            provider.saveGlobalQuest(questId, p);
        }
    }

    public void saveAll() {
        for (Map.Entry<String, GlobalQuestProgress> entry : cache.entrySet()) {
            provider.saveGlobalQuest(entry.getKey(), entry.getValue()).join();
        }
    }

    private void startAutoSaveTask() {
        if (saveTask != null) {
            saveTask.cancel();
        }

        saveTask = plugin.getFoliaLib().getImpl().runTimerAsync(() -> {
            if (!autoSaveEnabled) return;
            boolean debugLog = plugin.getConfig().getBoolean("optimization.debug.auto-save-log", false);
            int saved = 0;
            for (Map.Entry<String, GlobalQuestProgress> entry : cache.entrySet()) {
                provider.saveGlobalQuest(entry.getKey(), entry.getValue());
                saved++;
            }
            if (debugLog && saved > 0) {
                plugin.getLogger().info("[DEBUG] Auto-save: saved " + saved + " global quests.");
            }
        }, autoSaveInterval, autoSaveInterval);
    }

    public void setAutoSaveEnabled(boolean enabled) {
        this.autoSaveEnabled = enabled;
        if (enabled) {
            startAutoSaveTask();
        } else {
            if (saveTask != null) {
                saveTask.cancel();
                saveTask = null;
            }
        }
    }

    public void setAutoSaveInterval(int ticks) {
        this.autoSaveInterval = ticks;
        if (autoSaveEnabled) {
            startAutoSaveTask();
        }
    }

    public void onDisable() {
        if (saveTask != null) {
            saveTask.cancel();
        }
        saveAll();
    }
}
