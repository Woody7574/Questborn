package ua.woody.questborn.storage.providers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.GlobalQuestProgress;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.TopEntry;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractSqlStorageProvider implements StorageProvider {
    protected static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private static final Type MAP_STR_INT = new TypeToken<Map<String, Integer>>() {
    }.getType();
    private static final Type MAP_STR_LONG = new TypeToken<Map<String, Long>>() {
    }.getType();
    private static final Type LIST_STR = new TypeToken<List<String>>() {
    }.getType();

    protected final QuestbornPlugin plugin;

    protected AbstractSqlStorageProvider(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    protected abstract Connection getConnection() throws SQLException;

    protected void releaseConnection(Connection con) {
    }

    protected abstract String upsertSql();

    protected abstract String upsertGlobalSql();

    protected void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS player_data ("
                + "uuid                      VARCHAR(36)  NOT NULL,"
                + "active_quest_id            VARCHAR(255),"
                + "active_quest_start         BIGINT       NOT NULL DEFAULT 0,"
                + "active_quest_progress      INT          NOT NULL DEFAULT 0,"
                + "current_stage              INT          NOT NULL DEFAULT 1,"
                + "stage_progress             INT          NOT NULL DEFAULT 0,"
                + "completed_stages           TEXT,"
                + "items_transferred          SMALLINT     NOT NULL DEFAULT 0,"
                + "travel_buffer              DOUBLE PRECISION NOT NULL DEFAULT 0,"
                + "pending_quest_id           VARCHAR(255),"
                + "active_quests              TEXT,"
                + "tracked_quest_id           VARCHAR(255),"
                + "completed_by_type          TEXT,"
                + "completed_quests           TEXT,"
                + "claimed_rewards            TEXT,"
                + "pending_rewards            TEXT,"
                + "cooldowns                  TEXT,"
                + "last_type_completion       TEXT,"
                + "skin_texture               TEXT,"
                + "skin_signature             TEXT,"
                + "assigned_rotation_quests   TEXT,"
                + "rotation_assigned_at       TEXT,"
                + "PRIMARY KEY (uuid)"
                + ")";
        String sqlGlobal = "CREATE TABLE IF NOT EXISTS global_quests ("
                + "quest_id VARCHAR(255) PRIMARY KEY, "
                + "data TEXT"
                + ")";
        Connection con = getConnection();
        try (Statement st = con.createStatement()) {
            st.executeUpdate(sql);
            st.executeUpdate(sqlGlobal);

            tryAddColumn(con, "assigned_rotation_quests", "TEXT");
            tryAddColumn(con, "rotation_assigned_at", "TEXT");
            tryAddColumn(con, "active_quests", "TEXT");
            tryAddColumn(con, "tracked_quest_id", "VARCHAR(255)");
        } finally {
            releaseConnection(con);
        }
    }

    protected void tryAddColumn(Connection con, String columnName, String columnType) {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("ALTER TABLE player_data ADD COLUMN "
                    + columnName + " " + columnType);
        } catch (SQLException e) {
        }
    }

    @Override
    public CompletableFuture<PlayerQuestProgress> loadPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM player_data WHERE uuid = ?";
            Connection con = null;
            try {
                con = getConnection();
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return deserialize(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load player " + uuid + ": " + e.getMessage());
            } finally {
                releaseConnection(con);
            }
            return new PlayerQuestProgress();
        });
    }

    @Override
    public CompletableFuture<Boolean> savePlayer(UUID uuid, PlayerQuestProgress p) {
        return CompletableFuture.supplyAsync(() -> {
            Connection con = null;
            try {
                con = getConnection();
                try (PreparedStatement ps = con.prepareStatement(upsertSql())) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, null);
                    ps.setLong(3, 0);
                    ps.setInt(4, 0);
                    ps.setInt(5, 1);
                    ps.setInt(6, 0);
                    ps.setString(7, null);
                    ps.setInt(8, 0);
                    ps.setDouble(9, p.getTravelBuffer());
                    ps.setString(10, p.getPendingQuestId());
                    ps.setString(11, GSON.toJson(p.getCompletedByType()));
                    ps.setString(12, GSON.toJson(new ArrayList<>(p.getCompletedQuests())));
                    ps.setString(13, GSON.toJson(new ArrayList<>(p.getClaimedRewards())));
                    ps.setString(14, GSON.toJson(new ArrayList<>(p.getPendingRewards())));
                    ps.setString(15, GSON.toJson(p.getQuestCooldowns()));
                    ps.setString(16, GSON.toJson(p.getLastTypeCompletionMap()));
                    ps.setString(17, p.getSkinTexture());
                    ps.setString(18, p.getSkinSignature());
                    ps.setString(19, GSON.toJson(p.getAssignedRotationQuestsMap()));
                    ps.setString(20, GSON.toJson(p.getRotationAssignedAtMap()));
                    ps.setString(21, GSON.toJson(p.getActiveQuests()));
                    ps.setString(22, p.getTrackedQuestId());
                    ps.executeUpdate();
                    return true;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save player " + uuid + ": " + e.getMessage());
                return false;
            } finally {
                releaseConnection(con);
            }
        });
    }

    @Override
    public CompletableFuture<List<TopEntry>> getTopPlayers(int limit, Set<String> enabledTypeIds) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT uuid, completed_by_type, skin_texture, skin_signature FROM player_data";
            List<TopEntry> list = new ArrayList<>();
            Connection con = null;
            try {
                con = getConnection();
                try (PreparedStatement ps = con.prepareStatement(sql);
                        ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String uuidStr = rs.getString("uuid");
                        UUID uuid;
                        try {
                            uuid = UUID.fromString(uuidStr);
                        } catch (IllegalArgumentException e) {
                            continue;
                        }

                        Map<String, Integer> byType = parseMapStrInt(rs.getString("completed_by_type"));
                        int total = 0;
                        for (Map.Entry<String, Integer> e : byType.entrySet()) {
                            if (enabledTypeIds == null || enabledTypeIds.contains(e.getKey())) {
                                total += e.getValue();
                            }
                        }
                        if (total > 0) {
                            String name = Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName())
                                    .orElse(uuidStr.substring(0, 8));
                            String texture = rs.getString("skin_texture");
                            String signature = rs.getString("skin_signature");
                            list.add(new TopEntry(uuid, name, total, texture, signature));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load top players: " + e.getMessage());
            } finally {
                releaseConnection(con);
            }

            Collections.sort(list);
            return list.size() > limit ? list.subList(0, limit) : list;
        });
    }

    @Override
    public CompletableFuture<GlobalQuestProgress> loadGlobalQuest(String questId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT data FROM global_quests WHERE quest_id = ?";
            Connection con = null;
            try {
                con = getConnection();
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, questId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String json = rs.getString("data");
                            if (json != null && !json.isEmpty()) {
                                GlobalQuestProgress p = GSON.fromJson(json, GlobalQuestProgress.class);
                                if (p != null) return p;
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load global quest " + questId + ": " + e.getMessage());
            } finally {
                releaseConnection(con);
            }
            return new GlobalQuestProgress(questId);
        });
    }

    @Override
    public CompletableFuture<Boolean> saveGlobalQuest(String questId, GlobalQuestProgress progress) {
        return CompletableFuture.supplyAsync(() -> {
            Connection con = null;
            try {
                con = getConnection();
                try (PreparedStatement ps = con.prepareStatement(upsertGlobalSql())) {
                    ps.setString(1, questId);
                    ps.setString(2, GSON.toJson(progress));
                    ps.executeUpdate();
                    return true;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save global quest " + questId + ": " + e.getMessage());
                return false;
            } finally {
                releaseConnection(con);
            }
        });
    }

    @Override
    public CompletableFuture<Map<String, GlobalQuestProgress>> loadAllGlobalQuests() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT quest_id, data FROM global_quests";
            Map<String, GlobalQuestProgress> map = new HashMap<>();
            Connection con = null;
            try {
                con = getConnection();
                try (PreparedStatement ps = con.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String id = rs.getString("quest_id");
                        String json = rs.getString("data");
                        if (json != null && !json.isEmpty()) {
                            GlobalQuestProgress p = GSON.fromJson(json, GlobalQuestProgress.class);
                            if (p != null) {
                                map.put(id, p);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load all global quests: " + e.getMessage());
            } finally {
                releaseConnection(con);
            }
            return map;
        });
    }

    private PlayerQuestProgress deserialize(ResultSet rs) throws SQLException {
        PlayerQuestProgress p = new PlayerQuestProgress();

        String activeQuestsJson = null;
        try { activeQuestsJson = rs.getString("active_quests"); } catch(SQLException ignored) {}

        if (activeQuestsJson != null && !activeQuestsJson.isEmpty()) {
            java.lang.reflect.Type type = new TypeToken<Map<String, PlayerQuestProgress.ActiveQuestData>>() {}.getType();
            Map<String, PlayerQuestProgress.ActiveQuestData> parsed = GSON.fromJson(activeQuestsJson, type);
            if (parsed != null) {
                parsed.values().forEach(p::addActiveQuest);
            }
        } else {
            String legacyId = rs.getString("active_quest_id");
            if (legacyId != null && !legacyId.isEmpty()) {
                PlayerQuestProgress.ActiveQuestData data = new PlayerQuestProgress.ActiveQuestData(legacyId, rs.getLong("active_quest_start"));
                data.setProgress(rs.getInt("active_quest_progress"));
                data.setCurrentStage(rs.getInt("current_stage"));
                data.setStageProgress(rs.getInt("stage_progress"));
                data.setItemsTransferred(rs.getInt("items_transferred") != 0);

                String stagesStr = rs.getString("completed_stages");
                if (stagesStr != null && !stagesStr.isEmpty()) {
                    data.setCompletedStages(parseMapIntBool(stagesStr));
                }
                p.addActiveQuest(data);
                p.setTrackedQuestId(legacyId);
            }
        }

        try { p.setTrackedQuestId(rs.getString("tracked_quest_id")); } catch(SQLException ignored) {}

        p.setTravelBuffer(rs.getDouble("travel_buffer"));
        p.setPendingQuestId(rs.getString("pending_quest_id"));
        p.setSkinTexture(rs.getString("skin_texture"));
        p.setSkinSignature(rs.getString("skin_signature"));

        parseMapStrInt(rs.getString("completed_by_type"))
                .forEach((k, v) -> p.getCompletedByType().put(k.toLowerCase(Locale.ROOT), v));

        List<String> completedIds = parseListStr(rs.getString("completed_quests"));
        completedIds.forEach(p::addCompletedQuest);

        List<String> claimed = parseListStr(rs.getString("claimed_rewards"));
        if (!claimed.isEmpty()) {
            claimed.forEach(qid -> p.setRewardClaimed(qid, true));
        } else {
            completedIds.forEach(qid -> p.setRewardClaimed(qid, true));
        }

        parseListStr(rs.getString("pending_rewards")).forEach(p::addPendingReward);

        parseMapStrLong(rs.getString("cooldowns"))
                .forEach((questId, until) -> p.setQuestCooldownUntil(questId, until != null ? until : 0L));

        parseMapStrLong(rs.getString("last_type_completion"))
                .forEach((typeId, time) -> p.setLastTypeCompletion(typeId, time != null ? time : 0L));

        parseMapStrListStr(rs.getString("assigned_rotation_quests"))
                .forEach(p::setAssignedRotationQuests);
        parseMapStrLong(rs.getString("rotation_assigned_at"))
                .forEach((typeId, ts) -> p.setRotationAssignedAt(typeId, ts != null ? ts : 0L));

        return p;
    }

    private Map<String, Integer> parseMapStrInt(String json) {
        if (json == null || json.isEmpty())
            return new HashMap<>();
        try {
            Map<String, Integer> m = GSON.fromJson(json, MAP_STR_INT);
            return m != null ? m : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private Map<String, Long> parseMapStrLong(String json) {
        if (json == null || json.isEmpty())
            return new HashMap<>();
        try {
            Map<String, Long> m = GSON.fromJson(json, MAP_STR_LONG);
            return m != null ? m : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private Map<Integer, Boolean> parseMapIntBool(String json) {
        if (json == null || json.isEmpty())
            return new HashMap<>();
        try {
            Map<String, Boolean> raw = GSON.fromJson(json,
                    new TypeToken<Map<String, Boolean>>() {
                    }.getType());
            if (raw == null)
                return new HashMap<>();
            Map<Integer, Boolean> result = new HashMap<>();
            raw.forEach((k, v) -> {
                try {
                    result.put(Integer.parseInt(k), v);
                } catch (NumberFormatException ignored) {
                }
            });
            return result;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private List<String> parseListStr(String json) {
        if (json == null || json.isEmpty())
            return new ArrayList<>();
        try {
            List<String> l = GSON.fromJson(json, LIST_STR);
            return l != null ? l : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Map<String, List<String>> parseMapStrListStr(String json) {
        if (json == null || json.isEmpty())
            return new HashMap<>();
        try {
            java.lang.reflect.Type type = new TypeToken<Map<String, List<String>>>() {
            }.getType();
            Map<String, List<String>> m = GSON.fromJson(json, type);
            return m != null ? m : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
