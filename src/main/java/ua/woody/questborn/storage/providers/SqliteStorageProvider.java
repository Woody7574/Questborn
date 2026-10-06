package ua.woody.questborn.storage.providers;

import ua.woody.questborn.QuestbornPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

public class SqliteStorageProvider extends AbstractSqlStorageProvider {
    private static final String SQLITE_UPSERT = "INSERT OR REPLACE INTO player_data ("
            + "uuid, active_quest_id, active_quest_start, active_quest_progress, "
            + "current_stage, stage_progress, completed_stages, items_transferred, "
            + "travel_buffer, pending_quest_id, completed_by_type, completed_quests, "
            + "claimed_rewards, pending_rewards, cooldowns, last_type_completion, "
            + "skin_texture, skin_signature, assigned_rotation_quests, rotation_assigned_at, "
            + "active_quests, tracked_quest_id"
            + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private final File dbFile;
    private Connection connection;

    public SqliteStorageProvider(QuestbornPlugin plugin) {
        super(plugin);
        String fileName = plugin.getConfig().getString("storage.sqlite.file", "questborn.db");

        File playersDir = new File(plugin.getDataFolder(), "playerdata");
        this.dbFile = new File(playersDir, fileName);
    }

    @Override
    public void init() throws Exception {
        if (!dbFile.getParentFile().exists() && !dbFile.getParentFile().mkdirs()) {
            throw new IllegalStateException("Cannot create data folder: " + dbFile.getParentFile());
        }

        Class.forName("org.sqlite.JDBC");

        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

        try (var st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
        }

        createTable();
        plugin.getLogger().info(QuestbornPlugin.WHITE + "  ↳ SQLite database initialized: "
                + QuestbornPlugin.LIGHT_BLUE + dbFile.getName() + QuestbornPlugin.RESET);
    }

    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Override
    protected synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
                try (var st = connection.createStatement()) {
                    st.execute("PRAGMA journal_mode=WAL");
                    st.execute("PRAGMA synchronous=NORMAL");
                }
                createTable();
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite JDBC driver not found", e);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to recreate SQLite connection", e);
            }
        }
        return connection;
    }

    @Override
    protected String upsertSql() {
        return SQLITE_UPSERT;
    }

    @Override
    protected String upsertGlobalSql() {
        return "INSERT OR REPLACE INTO global_quests (quest_id, data) VALUES (?,?)";
    }
}
