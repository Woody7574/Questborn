package ua.woody.questborn.api.managers;

import org.bukkit.entity.Player;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestTypeConfig;

import java.util.Collection;
import java.util.List;

public interface QuestManagerAPI {
    boolean activateQuest(Player player, String questId, boolean ignoreRequirements);
    boolean activateQuest(Player player, String questId);
    boolean trackQuest(Player player, String questId);
    boolean confirmQuestChange(Player player, String questId, boolean ignoreRequirements);
    boolean confirmQuestChange(Player player, String questId);
    boolean cancelQuest(Player player, String questId);
    void completeQuest(Player player, QuestDefinition quest);
    boolean claimReward(Player player, QuestDefinition quest);
    boolean canClaimRewardFromNpc(Player player, QuestDefinition quest, String npcId);
    void complete(Player player, QuestDefinition quest);
    boolean forceFinishStage(Player player);
    List<QuestDefinition> getAvailableQuestsForPlayer(Player player, QuestTypeConfig typeConfig, boolean ignoreRequirements);
    List<QuestDefinition> getAvailableQuestsForPlayer(Player player, QuestTypeConfig typeConfig);
    Collection<QuestDefinition> getAll();
    QuestDefinition getById(String id);
}
