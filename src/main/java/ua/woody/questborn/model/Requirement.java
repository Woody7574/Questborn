package ua.woody.questborn.model;

import org.bukkit.entity.Player;
import ua.woody.questborn.storage.PlayerDataStore;
import ua.woody.questborn.api.QuestbornProvider;
import ua.woody.questborn.api.requirement.RequirementHandler;

public class Requirement {
    private final String type;
    private final String value;

    public Requirement(String type, String value) {
        this.type = type.toUpperCase();
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public boolean check(Player player, PlayerDataStore dataStore) {
        if (player == null)
            return false;

        RequirementHandler handler = QuestbornProvider.get().getRequirementRegistry().get(type);
        if (handler != null) {
            return handler.check(player, dataStore, value);
        }

        return true;
    }

    public static Requirement fromString(String key, String value) {
        String type = key.toUpperCase().replace("-", "_");
        if (key.equalsIgnoreCase("permission"))
            type = "PERMISSION";
        else if (key.equalsIgnoreCase("quest") || key.equalsIgnoreCase("quest-completed") || key.equalsIgnoreCase("completed-quest"))
            type = "QUEST_COMPLETED";
        else if (key.equalsIgnoreCase("type") || key.equalsIgnoreCase("type-completed") || key.equalsIgnoreCase("completed-type"))
            type = "QUEST_TYPE_COMPLETED";

        return new Requirement(type, value);
    }
}
