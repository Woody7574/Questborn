package ua.woody.questborn.storage.providers;

import ua.woody.questborn.QuestbornPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class H2StorageProvider extends AbstractSqlStorageProvider {
    private static final String H2_UPSERT = "MERGE INTO player_data ("
            + "uuid, active_quest_id, active_quest_start, active_quest_progress, "
            + "current_stage, stage_progress, completed_stages, items_transferred, "
            + "travel_buffer, pending_quest_id, completed_by_type, completed_quests, "
            + "claimed_rewards, pending_rewards, cooldowns, last_type_completion, "
            + "skin_texture, skin_signature, assigned_rotation_quests, rotation_assigned_at, "
            + "active_quests, tracked_quest_id"
            + ") KEY(uuid) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private Connection connection;
    private final File dataFolder;

    public H2StorageProvider(QuestbornPlugin plugin) {
        super(plugin);
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
    }

    @Override
    public void init() throws Exception {
        Class.forName("org.h2.Driver");

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, "database");
        String url = "jdbc:h2:" + dbFile.getAbsolutePath() + ";MODE=MySQL";

        connection = DriverManager.getConnection(url);

        createTable();

        plugin.getLogger().info(QuestbornPlugin.WHITE + "  ↳ H2 Local Database connected → "
                + QuestbornPlugin.LIGHT_BLUE + dbFile.getName() + ".mv.db"
                + QuestbornPlugin.RESET);
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }

    @Override
    protected Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            File dbFile = new File(dataFolder, "database");
            String url = "jdbc:h2:" + dbFile.getAbsolutePath() + ";MODE=MySQL";
            connection = DriverManager.getConnection(url);
        }
        return connection;
    }

    @Override
    protected String upsertSql() {
        return H2_UPSERT;
    }

    @Override
    protected String upsertGlobalSql() {
        return "MERGE INTO global_quests (quest_id, data) KEY(quest_id) VALUES (?,?)";
    }
}
