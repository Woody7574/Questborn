package ua.woody.questborn.addons.api;

import org.bukkit.entity.Player;
import ua.woody.questborn.managers.QuestManager;
import ua.woody.questborn.model.QuestDefinition;

public interface QuestEngine {

    String getId();

    /**
     * Availability check hook
     */
    AvailabilityResult checkAvailability(
            Player player,
            QuestDefinition quest,
            QuestManager questManager
    );

    /**
     * Called after quest completion
     */
    default void onQuestCompleted(
            Player player,
            QuestDefinition quest,
            QuestManager questManager
    ) {
        // optional
    }

    default void onQuestActivated(Player player, QuestDefinition quest, QuestManager questManager) {
        // optional
    }

}
