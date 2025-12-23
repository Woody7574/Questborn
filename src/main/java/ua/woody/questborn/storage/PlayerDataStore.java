package ua.woody.questborn.storage;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.woody.questborn.model.PlayerQuestProgress;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataStore {

    private final File file;
    private final Map<UUID, PlayerQuestProgress> cache = new ConcurrentHashMap<>();
    private YamlConfiguration yaml;

    public PlayerDataStore(File dataFolder) {
        this.file = new File(dataFolder, "playerdata.yml");

        // ✅ НІЧОГО НЕ СТВОРЮЄМО ТУТ.
        // Якщо файлу немає — просто стартуємо з порожньою конфігурацією.
        this.yaml = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();

        loadAll();
    }

    private void loadAll() {
        ConfigurationSection playersSec = yaml.getConfigurationSection("players");
        if (playersSec == null) return;

        for (String key : playersSec.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }

            ConfigurationSection sec = playersSec.getConfigurationSection(key);
            if (sec == null) continue;

            PlayerQuestProgress p = new PlayerQuestProgress();
            p.setActiveQuestId(sec.getString("active-quest-id", null));
            p.setActiveQuestStartTime(sec.getLong("active-quest-start", 0L));
            p.setActiveQuestProgress(sec.getInt("active-quest-progress", 0));

            // ✅ travel buffer
            p.setTravelBuffer(sec.getDouble("travel-buffer", 0.0));

            // ✅ pending quest id
            p.setPendingQuestId(sec.getString("pending-quest-id", null));

            // ✅ completed by type (String -> int)
            ConfigurationSection completedSec = sec.getConfigurationSection("completed");
            if (completedSec != null) {
                Map<String, Integer> map = p.getCompletedByType();
                for (String typeId : completedSec.getKeys(false)) {
                    try {
                        map.put(typeId.toLowerCase(Locale.ROOT), completedSec.getInt(typeId, 0));
                    } catch (Exception ignored) {}
                }
            }

            // ✅ completed quest ids
            List<String> completedIds = sec.getStringList("completed-quests");
            for (String qid : completedIds) {
                p.addCompletedQuest(qid);
            }

            // ✅ cooldowns
            ConfigurationSection cdSec = sec.getConfigurationSection("cooldowns");
            if (cdSec != null) {
                for (String qid : cdSec.getKeys(false)) {
                    long until = cdSec.getLong(qid, 0L);
                    p.setQuestCooldownUntil(qid, until);
                }
            }

            cache.put(uuid, p);
        }
    }

    public PlayerQuestProgress get(UUID uuid) {
        return cache.computeIfAbsent(uuid, u -> new PlayerQuestProgress());
    }

    public Map<UUID, PlayerQuestProgress> getAll() {
        return cache;
    }

    public void save() {
        yaml = new YamlConfiguration();
        ConfigurationSection playersSec = yaml.createSection("players");

        for (Map.Entry<UUID, PlayerQuestProgress> e : cache.entrySet()) {
            UUID uuid = e.getKey();
            PlayerQuestProgress p = e.getValue();

            ConfigurationSection sec = playersSec.createSection(uuid.toString());
            sec.set("active-quest-id", p.getActiveQuestId());
            sec.set("active-quest-start", p.getActiveQuestStartTime());
            sec.set("active-quest-progress", p.getActiveQuestProgress());

            sec.set("travel-buffer", p.getTravelBuffer());
            sec.set("pending-quest-id", p.getPendingQuestId());

            ConfigurationSection completedSec = sec.createSection("completed");
            for (Map.Entry<String, Integer> entry : p.getCompletedByType().entrySet()) {
                completedSec.set(entry.getKey(), entry.getValue());
            }

            sec.set("completed-quests", new ArrayList<>(p.getCompletedQuests()));

            ConfigurationSection cdSec = sec.createSection("cooldowns");
            for (Map.Entry<String, Long> entry : p.getQuestCooldowns().entrySet()) {
                cdSec.set(entry.getKey(), entry.getValue());
            }
        }

        try {
            // ✅ якщо файлу/папок нема — збереження створить файл (якщо dataFolder існує)
            yaml.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().severe("[Questborn] Could not save playerdata.yml");
            e.printStackTrace();
        }
    }
}
