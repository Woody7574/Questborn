package ua.woody.questborn.storage.providers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ua.woody.questborn.QuestbornPlugin;

import java.sql.Connection;
import java.sql.SQLException;

public class PostgreSQLStorageProvider extends AbstractSqlStorageProvider {
    private static final String POSTGRESQL_UPSERT = "INSERT INTO player_data ("
            + "uuid, active_quest_id, active_quest_start, active_quest_progress, "
            + "current_stage, stage_progress, completed_stages, items_transferred, "
            + "travel_buffer, pending_quest_id, completed_by_type, completed_quests, "
            + "claimed_rewards, pending_rewards, cooldowns, last_type_completion, "
            + "skin_texture, skin_signature, assigned_rotation_quests, rotation_assigned_at, "
            + "active_quests, tracked_quest_id"
            + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
            + "ON CONFLICT (uuid) DO UPDATE SET "
            + "active_quest_id=EXCLUDED.active_quest_id, "
            + "active_quest_start=EXCLUDED.active_quest_start, "
            + "active_quest_progress=EXCLUDED.active_quest_progress, "
            + "current_stage=EXCLUDED.current_stage, "
            + "stage_progress=EXCLUDED.stage_progress, "
            + "completed_stages=EXCLUDED.completed_stages, "
            + "items_transferred=EXCLUDED.items_transferred, "
            + "travel_buffer=EXCLUDED.travel_buffer, "
            + "pending_quest_id=EXCLUDED.pending_quest_id, "
            + "completed_by_type=EXCLUDED.completed_by_type, "
            + "completed_quests=EXCLUDED.completed_quests, "
            + "claimed_rewards=EXCLUDED.claimed_rewards, "
            + "pending_rewards=EXCLUDED.pending_rewards, "
            + "cooldowns=EXCLUDED.cooldowns, "
            + "last_type_completion=EXCLUDED.last_type_completion, "
            + "skin_texture=EXCLUDED.skin_texture, "
            + "skin_signature=EXCLUDED.skin_signature, "
            + "assigned_rotation_quests=EXCLUDED.assigned_rotation_quests, "
            + "rotation_assigned_at=EXCLUDED.rotation_assigned_at, "
            + "active_quests=EXCLUDED.active_quests, "
            + "tracked_quest_id=EXCLUDED.tracked_quest_id";

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSsl;
    private final int poolSize;

    private HikariDataSource dataSource;

    public PostgreSQLStorageProvider(QuestbornPlugin plugin) {
        super(plugin);
        this.host = plugin.getConfig().getString("storage.postgresql.host", "localhost");
        this.port = plugin.getConfig().getInt("storage.postgresql.port", 5432);
        this.database = plugin.getConfig().getString("storage.postgresql.database", "questborn");
        this.username = plugin.getConfig().getString("storage.postgresql.username", "postgres");
        this.password = plugin.getConfig().getString("storage.postgresql.password", "");
        this.useSsl = plugin.getConfig().getBoolean("storage.postgresql.use-ssl", false);
        this.poolSize = plugin.getConfig().getInt("storage.postgresql.pool-size", 5);
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
        String sslParam = useSsl ? "?sslmode=require&sslfactory=org.postgresql.ssl.NonValidatingFactory" : "?sslmode=disable";
        config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database + sslParam);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setDriverClassName("org.postgresql.Driver");
        config.setPoolName("Questborn-PostgreSQL-Pool");

        this.dataSource = new HikariDataSource(config);

        createTable();
        plugin.getLogger().info(QuestbornPlugin.WHITE + "  ↳ PostgreSQL connected → "
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
        return POSTGRESQL_UPSERT;
    }

    @Override
    protected String upsertGlobalSql() {
        return "INSERT INTO global_quests (quest_id, data) VALUES (?,?) ON CONFLICT (quest_id) DO UPDATE SET data=EXCLUDED.data";
    }

    private void closeQuietly(Connection c) {
        try {
            if (!c.isClosed())
                c.close();
        } catch (SQLException ignored) {
        }
    }
}
