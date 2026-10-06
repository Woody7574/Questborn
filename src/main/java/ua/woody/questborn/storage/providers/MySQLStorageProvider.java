package ua.woody.questborn.storage.providers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ua.woody.questborn.QuestbornPlugin;

import java.sql.Connection;
import java.sql.SQLException;

public class MySQLStorageProvider extends AbstractSqlStorageProvider {
    private static final String MYSQL_UPSERT = "INSERT INTO player_data ("
            + "uuid, active_quest_id, active_quest_start, active_quest_progress, "
            + "current_stage, stage_progress, completed_stages, items_transferred, "
            + "travel_buffer, pending_quest_id, completed_by_type, completed_quests, "
            + "claimed_rewards, pending_rewards, cooldowns, last_type_completion, "
            + "skin_texture, skin_signature, assigned_rotation_quests, rotation_assigned_at, "
            + "active_quests, tracked_quest_id"
            + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE "
            + "active_quest_id=VALUES(active_quest_id), "
            + "active_quest_start=VALUES(active_quest_start), "
            + "active_quest_progress=VALUES(active_quest_progress), "
            + "current_stage=VALUES(current_stage), "
            + "stage_progress=VALUES(stage_progress), "
            + "completed_stages=VALUES(completed_stages), "
            + "items_transferred=VALUES(items_transferred), "
            + "travel_buffer=VALUES(travel_buffer), "
            + "pending_quest_id=VALUES(pending_quest_id), "
            + "completed_by_type=VALUES(completed_by_type), "
            + "completed_quests=VALUES(completed_quests), "
            + "claimed_rewards=VALUES(claimed_rewards), "
            + "pending_rewards=VALUES(pending_rewards), "
            + "cooldowns=VALUES(cooldowns), "
            + "last_type_completion=VALUES(last_type_completion), "
            + "skin_texture=VALUES(skin_texture), "
            + "skin_signature=VALUES(skin_signature), "
            + "assigned_rotation_quests=VALUES(assigned_rotation_quests), "
            + "rotation_assigned_at=VALUES(rotation_assigned_at), "
            + "active_quests=VALUES(active_quests), "
            + "tracked_quest_id=VALUES(tracked_quest_id)";

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSsl;
    private final int poolSize;

    private HikariDataSource dataSource;

    public MySQLStorageProvider(QuestbornPlugin plugin) {
        super(plugin);
        this.host = plugin.getConfig().getString("storage.mysql.host", "localhost");
        this.port = plugin.getConfig().getInt("storage.mysql.port", 3306);
        this.database = plugin.getConfig().getString("storage.mysql.database", "questborn");
        this.username = plugin.getConfig().getString("storage.mysql.username", "root");
        this.password = plugin.getConfig().getString("storage.mysql.password", "");
        this.useSsl = plugin.getConfig().getBoolean("storage.mysql.use-ssl", false);
        this.poolSize = plugin.getConfig().getInt("storage.mysql.pool-size", 5);
    }

    @Override
    public void init() throws Exception {
        try {
            Class<?> configuratorClass = Class.forName("org.apache.logging.log4j.core.config.Configurator");
            Class<?> levelClass = Class.forName("org.apache.logging.log4j.Level");
            Object warnLevel = levelClass.getDeclaredField("WARN").get(null);
            java.lang.reflect.Method setLevelMethod = configuratorClass.getMethod("setLevel", String.class, levelClass);
            setLevelMethod.invoke(null, "ua.woody.questborn.libs.hikari", warnLevel);
            setLevelMethod.invoke(null, "com.zaxxer.hikari", warnLevel);
        } catch (Throwable ignored) {
            java.util.logging.Logger.getLogger("ua.woody.questborn.libs.hikari").setLevel(java.util.logging.Level.WARNING);
            java.util.logging.Logger.getLogger("com.zaxxer.hikari").setLevel(java.util.logging.Level.WARNING);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + useSsl
                + "&autoReconnect=true"
                + "&useUnicode=true"
                + "&characterEncoding=UTF-8"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setPoolName("Questborn-MySQL-Pool");

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        this.dataSource = new HikariDataSource(config);

        createTable();
        plugin.getLogger().info(QuestbornPlugin.WHITE + "  ↳ MySQL connected → "
                + QuestbornPlugin.LIGHT_BLUE + host + ":" + port + "/" + database
                + QuestbornPlugin.WHITE + " (Hikari pool=" + QuestbornPlugin.LIGHT_BLUE + poolSize + QuestbornPlugin.WHITE
                + ")"
                + QuestbornPlugin.RESET);
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    protected void releaseConnection(Connection c) {
        closeQuietly(c);
    }

    @Override
    protected String upsertSql() {
        return MYSQL_UPSERT;
    }

    @Override
    protected String upsertGlobalSql() {
        return "INSERT INTO global_quests (quest_id, data) VALUES (?,?) ON DUPLICATE KEY UPDATE data=VALUES(data)";
    }

    private void closeQuietly(Connection c) {
        try {
            if (!c.isClosed())
                c.close();
        } catch (SQLException ignored) {
        }
    }
}
