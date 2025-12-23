package ua.woody.questborn.addons.chains;

import org.bukkit.entity.Player;
import ua.woody.questborn.addons.api.AvailabilityResult;
import ua.woody.questborn.addons.api.QuestEngine;
import ua.woody.questborn.managers.QuestManager;
import ua.woody.questborn.model.QuestDefinition;

/**
 * Chains quest engine.
 * Marker engine: chaining logic is handled by core (QuestManager).
 * Never blocks availability.
 */
public final class ChainsEngine implements QuestEngine {

    @Override
    public String getId() {
        return "chains";
    }

    @Override
    public AvailabilityResult checkAvailability(Player player, QuestDefinition quest, QuestManager questManager) {
        return AvailabilityResult.ok();
    }

    @Override
    public void onQuestCompleted(Player player, QuestDefinition quest, QuestManager questManager) {
        // optional (usually core handles chaining)
    }
}
