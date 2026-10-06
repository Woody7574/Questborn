package ua.woody.questborn.managers;

import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestTypeConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

public class QuestRegistry {
    private final QuestbornPlugin plugin;
    private final QuestTypeManager questTypeManager;
    private final QuestParser questParser;

    private final Map<String, QuestDefinition> questsById = new HashMap<>();

    public QuestRegistry(QuestbornPlugin plugin, QuestTypeManager questTypeManager, QuestParser questParser) {
        this.plugin = plugin;
        this.questTypeManager = questTypeManager;
        this.questParser = questParser;
    }

    public void loadQuests() {
        questsById.clear();
        for (QuestTypeConfig typeConfig : questTypeManager.getEnabledTypes()) {
            File folder = new File(plugin.getDataFolder(), typeConfig.getFolder());
            List<QuestDefinition> quests = questParser.loadQuestsFromFolder(folder, typeConfig.getId());

            for (QuestDefinition quest : quests) {
                questsById.put(quest.getId(), quest);
            }
        }

        plugin.getLogger().fine("Loaded " + questsById.size() + " quests from " +
                questTypeManager.getEnabledTypes().size() + " types");
    }

    public void clear() {
        questsById.clear();
    }

    public QuestDefinition getQuest(String id) {
        return questsById.get(id);
    }

    public Collection<QuestDefinition> getAll() {
        return questsById.values();
    }

    public Collection<QuestDefinition> getByType(String typeId) {
        return questsById.values().stream()
                .filter(q -> q.getTypeId().equalsIgnoreCase(typeId))
                .collect(Collectors.toList());
    }

    public Collection<QuestDefinition> getByType(QuestTypeConfig typeConfig) {
        return getByType(typeConfig.getId());
    }

    public List<String> getAllIds() {
        return new ArrayList<>(questsById.keySet());
    }
}
