package ua.woody.questborn.api;

import ua.woody.questborn.api.objective.ObjectiveRegistry;
import ua.woody.questborn.api.requirement.RequirementRegistry;
import ua.woody.questborn.rewards.RewardRegistry;
import ua.woody.questborn.model.QuestObjectiveType;
import ua.woody.questborn.model.PlayerQuestProgress;
import java.util.Locale;

public class DefaultQuestbornAPI implements QuestbornAPI {
    private final ObjectiveRegistry objectiveRegistry = new ObjectiveRegistry();
    private final RewardRegistry rewardRegistry = new RewardRegistry();
    private final RequirementRegistry requirementRegistry = new RequirementRegistry();

    public DefaultQuestbornAPI() {
        registerDefaultObjectives();
        registerDefaultRequirements();
    }

    private void registerDefaultObjectives() {
        for (QuestObjectiveType type : QuestObjectiveType.values()) {
            objectiveRegistry.register(type.name(), ua.woody.questborn.model.QuestObjective.class);
            objectiveRegistry.register(type.canonicalKey(), ua.woody.questborn.model.QuestObjective.class);
        }
    }

    private void registerDefaultRequirements() {
        requirementRegistry.register("PERMISSION", (player, dataStore, value) -> {
            return player.hasPermission(value);
        });

        requirementRegistry.register("QUEST_COMPLETED", (player, dataStore, value) -> {
            if (dataStore == null) return false;
            return dataStore.get(player.getUniqueId()).isQuestCompleted(value);
        });

        requirementRegistry.register("QUEST_TYPE_COMPLETED", (player, dataStore, value) -> {
            if (dataStore == null) return false;
            PlayerQuestProgress progress = dataStore.get(player.getUniqueId());
            Integer count = progress.getCompletedByType().get(value.toLowerCase(Locale.ROOT));
            return count != null && count > 0;
        });
    }

    @Override
    public ObjectiveRegistry getObjectiveRegistry() {
        return objectiveRegistry;
    }

    @Override
    public RewardRegistry getRewardRegistry() {
        return rewardRegistry;
    }

    @Override
    public RequirementRegistry getRequirementRegistry() {
        return requirementRegistry;
    }
}
