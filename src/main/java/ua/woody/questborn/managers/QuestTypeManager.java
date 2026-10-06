package ua.woody.questborn.managers;

import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestTypeConfig;

import java.io.File;
import java.util.*;

public class QuestTypeManager {
    private final QuestbornPlugin plugin;
    private final Map<String, QuestTypeConfig> questTypes = new HashMap<>();

    public QuestTypeManager(QuestbornPlugin plugin) {
        this.plugin = plugin;
        loadQuestTypes();
    }

    public void loadQuestTypes() {
        questTypes.clear();

        File typesFolder = new File(plugin.getDataFolder(), "types");
        if (!typesFolder.exists() || !typesFolder.isDirectory()) {
            plugin.getLogger().warning("Quest types folder not found: " + typesFolder.getPath()
                    + " (resources should be extracted by the main plugin class on first run)");
            return;
        }

        File[] typeFiles = typesFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (typeFiles == null || typeFiles.length == 0) {
            plugin.getLogger().warning("No quest type files found in " + typesFolder.getPath());
            return;
        }

        for (File file : typeFiles) {
            QuestTypeConfig typeConfig = QuestTypeConfig.loadFromFile(plugin, file);
            if (typeConfig != null) {
                questTypes.put(typeConfig.getId().toLowerCase(), typeConfig);
            }
        }

        if (questTypes.isEmpty()) {
            plugin.getLogger().warning("No valid quest types loaded! Check your type configuration files.");
        }
    }

    public QuestTypeConfig getType(String typeId) {
        if (typeId == null)
            return null;
        return questTypes.get(typeId.toLowerCase());
    }

    public Collection<QuestTypeConfig> getAllTypes() {
        return questTypes.values();
    }

    public List<QuestTypeConfig> getEnabledTypes() {
        return questTypes.values().stream()
                .filter(QuestTypeConfig::isEnabled)
                .sorted(Comparator.comparingInt(QuestTypeConfig::getGuiSlot))
                .toList();
    }

    public boolean typeExists(String typeId) {
        if (typeId == null)
            return false;
        return questTypes.containsKey(typeId.toLowerCase());
    }

    public boolean isTypeUnlocked(org.bukkit.entity.Player player, QuestTypeConfig type) {
        if (player == null || type == null)
            return false;

        List<ua.woody.questborn.model.Requirement> reqs = type.getRequirements();
        if (reqs == null || reqs.isEmpty())
            return true;

        var dataStore = plugin.getPlayerDataStore();

        for (ua.woody.questborn.model.Requirement req : reqs) {
            if (!req.check(player, dataStore)) {
                return false;
            }
        }
        return true;
    }

    public void reload() {
        loadQuestTypes();
    }
}
