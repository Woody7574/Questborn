package ua.woody.questborn.storage.providers;

import ua.woody.questborn.model.GlobalQuestProgress;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.TopEntry;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageProvider {
    void init() throws Exception;

    void close() throws Exception;

    CompletableFuture<PlayerQuestProgress> loadPlayer(UUID uuid);

    CompletableFuture<Boolean> savePlayer(UUID uuid, PlayerQuestProgress progress);

    CompletableFuture<List<TopEntry>> getTopPlayers(int limit, Set<String> enabledTypeIds);

    CompletableFuture<GlobalQuestProgress> loadGlobalQuest(String questId);

    CompletableFuture<Boolean> saveGlobalQuest(String questId, GlobalQuestProgress progress);

    CompletableFuture<Map<String, GlobalQuestProgress>> loadAllGlobalQuests();
}
