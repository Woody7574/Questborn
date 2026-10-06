package ua.woody.questborn.api.requirement;

import org.bukkit.entity.Player;
import ua.woody.questborn.storage.PlayerDataStore;

public interface RequirementHandler {
    boolean check(Player player, PlayerDataStore dataStore, String value);
}
