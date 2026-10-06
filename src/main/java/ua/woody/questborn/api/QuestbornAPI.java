package ua.woody.questborn.api;

import ua.woody.questborn.rewards.RewardRegistry;
import ua.woody.questborn.api.objective.ObjectiveRegistry;
import ua.woody.questborn.api.requirement.RequirementRegistry;

public interface QuestbornAPI {
    ObjectiveRegistry getObjectiveRegistry();
    RewardRegistry getRewardRegistry();
    RequirementRegistry getRequirementRegistry();
}
