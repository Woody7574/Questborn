package ua.woody.questborn.model;

import org.bukkit.Material;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class QuestStage {
    public enum StageType {
        OBJECTIVE,
        REQUIRED_MATERIALS
    }

    private final StageType type;
    private final QuestObjective objective;
    private final Map<QuestItem, Integer> requiredItems;

    public QuestStage(QuestObjective objective) {
        this.type = StageType.OBJECTIVE;
        this.objective = Objects.requireNonNull(objective, "objective");
        this.requiredItems = Collections.emptyMap();
    }

    public QuestStage(Map<QuestItem, Integer> requiredItems) {
        this.type = StageType.REQUIRED_MATERIALS;
        this.objective = null;

        if (requiredItems == null || requiredItems.isEmpty()) {
            this.requiredItems = Collections.emptyMap();
        } else {
            this.requiredItems = Collections.unmodifiableMap(new LinkedHashMap<>(requiredItems));
        }
    }

    public StageType getType() {
        return type;
    }

    public QuestObjective getObjective() {
        return objective;
    }

    public Map<QuestItem, Integer> getRequiredItems() {
        return requiredItems;
    }

    public boolean isObjective() {
        return type == StageType.OBJECTIVE;
    }

    public boolean isRequiredMaterials() {
        return type == StageType.REQUIRED_MATERIALS;
    }

    @Override
    public String toString() {
        if (isObjective()) {
            return "QuestStage{type=OBJECTIVE, objective=" + objective + "}";
        }
        return "QuestStage{type=REQUIRED_MATERIALS, requiredItems=" + requiredItems + "}";
    }
}
