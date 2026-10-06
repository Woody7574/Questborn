package ua.woody.questborn.managers;

import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.EngineType;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestTypeConfig;

import java.util.ArrayList;
import java.util.List;

public class QuestValidator {
    private final QuestbornPlugin plugin;
    private final QuestManager questManager;
    private final QuestTypeManager questTypeManager;

    public QuestValidator(QuestbornPlugin plugin, QuestManager questManager, QuestTypeManager questTypeManager) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.questTypeManager = questTypeManager;
    }

    public boolean isQuestAvailable(Player player, QuestDefinition quest, PlayerQuestProgress data, boolean ignoreRequirements) {
        QuestTypeConfig typeConfig = questTypeManager.getType(quest.getTypeId());
        if (typeConfig == null) {
            return false;
        }

        if (typeConfig.getEngine() == EngineType.ROTATION) {
            boolean isNpcBound = false;
            if (plugin.getNpcManager() != null) {
                for (ua.woody.questborn.model.NpcConfig nc : plugin.getNpcManager().getAll()) {
                    if (!nc.getStartsQuests().isEmpty() && nc.getStartsQuests().contains(quest.getId())) {
                        isNpcBound = true;
                        break;
                    }
                }
            }

            if (!isNpcBound) {
                List<QuestDefinition> assigned = questManager.getOrAssignRotationQuests(player, typeConfig);
                boolean isAssigned = false;
                for (QuestDefinition q : assigned) {
                    if (q.getId().equals(quest.getId())) {
                        isAssigned = true;
                        break;
                    }
                }
                if (!isAssigned) {
                    return false;
                }
            }
            return isQuestAvailableDefault(player, quest, data, ignoreRequirements);
        }

        if (typeConfig.getEngine() == EngineType.DEFAULT) {
            return isQuestAvailableDefault(player, quest, data, ignoreRequirements);
        }

        if (typeConfig.getEngine() == EngineType.CHAIN) {
            return isQuestAvailableChain(player, quest, data, ignoreRequirements);
        }

        if (typeConfig.getEngine() == EngineType.GLOBAL) {
            if (typeConfig.getCooldownSeconds() > 0) {
                ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(quest.getId());
                if (gp != null && gp.isCompleted()) {
                    long completedAt = gp.getLastContributionTimes().values().stream().mapToLong(v -> v).max().orElse(0L);
                    if (completedAt > 0 && System.currentTimeMillis() >= completedAt + (typeConfig.getCooldownSeconds() * 1000L)) {
                        gp.setCompleted(false);
                        gp.setGlobalProgress(0);
                        gp.getContributions().clear();
                        gp.getLastContributionTimes().clear();
                        gp.setStartedAt(0L);
                        plugin.getGlobalQuestDataStore().save(quest.getId());
                    }
                }
            }
            return isQuestAvailableDefault(player, quest, data, ignoreRequirements);
        }

        return false;
    }

    private boolean isQuestAvailableDefault(Player player, QuestDefinition quest, PlayerQuestProgress data, boolean ignoreRequirements) {
        QuestTypeConfig typeConfig = questTypeManager.getType(quest.getTypeId());
        if (typeConfig != null && typeConfig.getCooldownSeconds() == 0 && data.isQuestCompleted(quest.getId())) {
            return false;
        }

        long cdUntil = data.getQuestCooldownUntil(quest.getId());
        if (cdUntil > 0 && cdUntil > System.currentTimeMillis()) {
            return false;
        }

        if (!ignoreRequirements) {
            if (quest.getRequiredQuests() != null && !quest.getRequiredQuests().isEmpty()) {
                for (String requiredQuestId : quest.getRequiredQuests()) {
                    if (!data.isQuestCompleted(requiredQuestId)) {
                        return false;
                    }
                }
            }

            if (quest.getRequiredPermission() != null && !quest.getRequiredPermission().isEmpty()) {
                if (!player.hasPermission(quest.getRequiredPermission())) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isQuestAvailableChain(Player player, QuestDefinition quest, PlayerQuestProgress data, boolean ignoreRequirements) {
        if (data.isQuestCompleted(quest.getId())) {
            return false;
        }

        if (!ignoreRequirements) {
            if (quest.getRequiredQuests() != null && !quest.getRequiredQuests().isEmpty()) {
                for (String requiredQuestId : quest.getRequiredQuests()) {
                    if (!data.isQuestCompleted(requiredQuestId)) {
                        return false;
                    }
                }
            }

            if (quest.getRequiredPermission() != null && !quest.getRequiredPermission().isEmpty()) {
                if (!player.hasPermission(quest.getRequiredPermission())) {
                    return false;
                }
            }
        }

        return true;
    }

    public List<QuestDefinition> getAvailableQuestsForPlayer(Player player, QuestTypeConfig typeConfig, PlayerQuestProgress data, boolean ignoreRequirements) {
        List<QuestDefinition> availableQuests = new ArrayList<>();

        for (QuestDefinition quest : questManager.getByType(typeConfig)) {
            if (isQuestAvailable(player, quest, data, ignoreRequirements)) {
                availableQuests.add(quest);
            }
        }

        if (typeConfig.getEngine() == EngineType.CHAIN) {
            availableQuests.sort((q1, q2) -> {
                Integer slot1 = q1.getSlot() != null ? q1.getSlot() : 0;
                Integer slot2 = q2.getSlot() != null ? q2.getSlot() : 0;
                return slot1.compareTo(slot2);
            });
        }

        return availableQuests;
    }
}
