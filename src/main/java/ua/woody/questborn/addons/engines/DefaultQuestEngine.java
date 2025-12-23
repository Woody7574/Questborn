package ua.woody.questborn.addons.engines;

import org.bukkit.entity.Player;
import ua.woody.questborn.addons.api.AvailabilityResult;
import ua.woody.questborn.addons.api.QuestEngine;
import ua.woody.questborn.managers.QuestManager;
import ua.woody.questborn.model.QuestDefinition;

/**
 * Core fallback engine. Always allows quests.
 */
public final class DefaultQuestEngine implements QuestEngine {

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public AvailabilityResult checkAvailability(Player player, QuestDefinition quest, QuestManager questManager) {
        return AvailabilityResult.ok();
    }

    @Override
    public void onQuestCompleted(Player player, QuestDefinition quest, QuestManager questManager) {
        // no-op
    }
}
