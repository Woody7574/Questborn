package ua.woody.questborn.storage.providers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.connection.SocketSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bukkit.Bukkit;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.GlobalQuestProgress;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.TopEntry;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MongoStorageProvider implements StorageProvider {
    private final QuestbornPlugin plugin;
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    private MongoClient client;
    private MongoCollection<Document> collection;
    private MongoCollection<Document> globalCollection;

    public MongoStorageProvider(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() throws Exception {
        try {
            java.util.logging.Logger.getLogger("org.mongodb.driver").setLevel(java.util.logging.Level.OFF);
            java.util.logging.Logger.getLogger("org.mongodb.driver").setFilter(r -> false);
        } catch (Exception ignored) {}

        String uri = plugin.getConfig().getString("storage.mongodb.uri", "mongodb://localhost:27017");
        String dbName = plugin.getConfig().getString("storage.mongodb.database", "questborn");
        String collName = plugin.getConfig().getString("storage.mongodb.collection", "player_data");
        int connectTimeout = plugin.getConfig().getInt("storage.mongodb.connect-timeout", 5000);
        int readTimeout = plugin.getConfig().getInt("storage.mongodb.read-timeout", 5000);

        ConnectionString cs = new ConnectionString(uri);

        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                .applyConnectionString(cs)
                .applyToSocketSettings(builder ->
                    builder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                           .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                )
                .applyToClusterSettings(builder ->
                    builder.serverSelectionTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                );

        client = MongoClients.create(settingsBuilder.build());
        MongoDatabase db = client.getDatabase(dbName);
        collection = db.getCollection(collName);

        try {
            db.runCommand(new Document("ping", 1));
            plugin.getLogger().info("  ↳ Successfully connected.");
        } catch (Exception e) {
            plugin.getLogger().severe("Could not connect to MongoDB: " + e.getMessage());
            throw e;
        }

        collection.createIndex(new Document("uuid", 1), new IndexOptions().unique(true));

        String globalCollName = plugin.getConfig().getString("storage.mongodb.global-collection", "global_quests");
        globalCollection = db.getCollection(globalCollName);
        globalCollection.createIndex(new Document("quest_id", 1), new IndexOptions().unique(true));
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
            client = null;
            collection = null;
            globalCollection = null;
        }
    }

    @Override
    public CompletableFuture<PlayerQuestProgress> loadPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (collection == null) return new PlayerQuestProgress();
            Document doc = collection.find(Filters.eq("uuid", uuid.toString())).first();
            if (doc == null) return new PlayerQuestProgress();
            String json = doc.getString("data");
            if (json == null || json.isEmpty()) return new PlayerQuestProgress();
            try {
                PlayerQuestProgress p = gson.fromJson(json, PlayerQuestProgress.class);
                return p != null ? p : new PlayerQuestProgress();
            } catch (Exception e) {
                return new PlayerQuestProgress();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> savePlayer(UUID uuid, PlayerQuestProgress p) {
        return CompletableFuture.supplyAsync(() -> {
            if (collection == null) return false;
            String json = gson.toJson(p);
            int total = 0;
            for (int v : p.getCompletedByType().values()) total += v;
            Document doc = new Document("uuid", uuid.toString())
                    .append("data", json)
                    .append("totalCompleted", total);
            ReplaceOptions opts = new ReplaceOptions().upsert(true);
            collection.replaceOne(Filters.eq("uuid", uuid.toString()), doc, opts);
            return true;
        });
    }

    @Override
    public CompletableFuture<List<TopEntry>> getTopPlayers(int limit, Set<String> enabledTypeIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<TopEntry> list = new ArrayList<>();
            if (collection == null) return list;
            for (Document doc : collection.find()) {
                String uuidStr = doc.getString("uuid");
                if (uuidStr == null) continue;
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                String json = doc.getString("data");
                if (json == null) continue;
                PlayerQuestProgress p;
                try {
                    p = gson.fromJson(json, PlayerQuestProgress.class);
                } catch (Exception e) {
                    continue;
                }
                int total = 0;
                if (p != null) {
                    for (Map.Entry<String, Integer> e : p.getCompletedByType().entrySet()) {
                        if (enabledTypeIds == null || enabledTypeIds.contains(e.getKey())) {
                            total += e.getValue();
                        }
                    }
                }
                if (total > 0) {
                    String name = Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName())
                            .orElse(uuidStr.substring(0, 8));
                    String texture = p != null ? p.getSkinTexture() : null;
                    String signature = p != null ? p.getSkinSignature() : null;
                    list.add(new TopEntry(uuid, name, total, texture, signature));
                }
            }
            Collections.sort(list);
            return list.size() > limit ? list.subList(0, limit) : list;
        });
    }

    @Override
    public CompletableFuture<GlobalQuestProgress> loadGlobalQuest(String questId) {
        return CompletableFuture.supplyAsync(() -> {
            if (globalCollection == null) return new GlobalQuestProgress(questId);
            Document doc = globalCollection.find(Filters.eq("quest_id", questId)).first();
            if (doc == null) return new GlobalQuestProgress(questId);
            String json = doc.getString("data");
            if (json == null || json.isEmpty()) return new GlobalQuestProgress(questId);
            try {
                GlobalQuestProgress p = gson.fromJson(json, GlobalQuestProgress.class);
                return p != null ? p : new GlobalQuestProgress(questId);
            } catch (Exception e) {
                return new GlobalQuestProgress(questId);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> saveGlobalQuest(String questId, GlobalQuestProgress progress) {
        return CompletableFuture.supplyAsync(() -> {
            if (globalCollection == null) return false;
            String json = gson.toJson(progress);
            Document doc = new Document("quest_id", questId)
                    .append("data", json);
            ReplaceOptions opts = new ReplaceOptions().upsert(true);
            globalCollection.replaceOne(Filters.eq("quest_id", questId), doc, opts);
            return true;
        });
    }

    @Override
    public CompletableFuture<Map<String, GlobalQuestProgress>> loadAllGlobalQuests() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, GlobalQuestProgress> map = new HashMap<>();
            if (globalCollection == null) return map;
            for (Document doc : globalCollection.find()) {
                String id = doc.getString("quest_id");
                if (id == null) continue;
                String json = doc.getString("data");
                if (json == null || json.isEmpty()) continue;
                try {
                    GlobalQuestProgress p = gson.fromJson(json, GlobalQuestProgress.class);
                    if (p != null) {
                        map.put(id, p);
                    }
                } catch (Exception ignored) {
                }
            }
            return map;
        });
    }
}
