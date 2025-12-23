package ua.woody.questborn.model;

import ua.woody.questborn.QuestbornPlugin;

import java.io.File;
import java.util.*;

public class QuestTypeManager {

    private final QuestbornPlugin plugin;
    private final Map<String, QuestTypeConfig> types = new HashMap<>();

    /* =========================================================
     *                      INIT
     * ========================================================= */

    public QuestTypeManager(QuestbornPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /* =========================================================
     *                      LOAD
     * ========================================================= */

    public void reload() {
        types.clear();

        File typesFolder = new File(plugin.getDataFolder(), "types");
        if (!typesFolder.exists() || !typesFolder.isDirectory()) {
            plugin.getLogger().warning(
                    "[QuestType] Types folder not found: " + typesFolder.getPath()
            );
            return;
        }

        File[] files = typesFolder.listFiles((dir, name) ->
                name.toLowerCase(Locale.ROOT).endsWith(".yml")
        );

        if (files == null || files.length == 0) {
            plugin.getLogger().warning(
                    "[QuestType] No quest type files found in " + typesFolder.getPath()
            );
            return;
        }

        for (File file : files) {
            QuestTypeConfig cfg = QuestTypeConfig.loadFromFile(plugin, file);
            if (cfg == null) continue;
            if (!cfg.isEnabled()) continue;

            types.put(cfg.getId().toLowerCase(Locale.ROOT), cfg);
        }

        if (types.isEmpty()) {
            plugin.getLogger().warning(
                    "[QuestType] No enabled quest types loaded"
            );
        }
    }

    /* =========================================================
     *                      ACCESS
     * ========================================================= */

    public QuestTypeConfig getType(String typeId) {
        if (typeId == null) return null;
        return types.get(typeId.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String typeId) {
        if (typeId == null) return false;
        return types.containsKey(typeId.toLowerCase(Locale.ROOT));
    }

    public Collection<QuestTypeConfig> getAll() {
        return Collections.unmodifiableCollection(types.values());
    }

    public List<QuestTypeConfig> getEnabledTypes() {
        return types.values().stream()
                .sorted(Comparator.comparingInt(QuestTypeConfig::getSlot))
                .toList();
    }
}
