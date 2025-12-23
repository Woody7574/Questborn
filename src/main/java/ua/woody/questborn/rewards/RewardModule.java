package ua.woody.questborn.rewards;

import org.bukkit.entity.Player;

public interface RewardModule {

    /**
     * Ключ типу нагороди, напр.:
     * "money", "items", "commands", "xp", "attributes", "effects"
     */
    String getKey();

    /**
     * Виконати нагороду.
     *
     * @param ctx    контекст (плагін, локалізація, квест, гравець, черга)
     * @param player гравець
     * @param config сирі дані з quest.getRewards().get(getKey())
     */
    void execute(RewardExecutionContext ctx, Player player, Object config);
}
