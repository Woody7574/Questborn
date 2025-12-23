package ua.woody.questborn.managers;

import org.bukkit.Bukkit;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.model.TopEntry;
import ua.woody.questborn.storage.PlayerDataStore;

import java.util.*;

public class TopManager {

    private final PlayerDataStore playerData;
    private int topSize;

    public TopManager(PlayerDataStore playerData) {
        this.playerData = playerData;
    }

    public void setTopSize(int topSize) {
        this.topSize = topSize;
    }

    public List<TopEntry> getTop(String typeId) {
        List<TopEntry> list = new ArrayList<>();

        for (var entry : playerData.getAll().entrySet()) {
            UUID uuid = entry.getKey();
            PlayerQuestProgress progress = entry.getValue();
            int value = progress.getCompleted(typeId);
            if (value <= 0) continue;

            String name = Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName())
                    .orElse(uuid.toString().substring(0, 8));
            list.add(new TopEntry(uuid, name, value));
        }

        Collections.sort(list);

        if (list.size() > topSize) {
            return list.subList(0, topSize);
        }

        return list;
    }

    public List<TopEntry> getTop(QuestTypeConfig typeConfig) {
        return getTop(typeConfig.getId());
    }

    public int getTotalCompletedQuests(UUID playerId) {
        PlayerQuestProgress progress = playerData.get(playerId);
        if (progress == null) return 0;

        int total = 0;
        for (int count : progress.getCompletedByType().values()) {
            total += count;
        }

        return total;
    }

    public Map<String, Integer> getCompletedByType(UUID playerId) {
        PlayerQuestProgress progress = playerData.get(playerId);
        return progress != null ? progress.getCompletedByType() : new HashMap<>();
    }
}