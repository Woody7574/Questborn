package ua.woody.questborn.indicators;

import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.NpcConfig;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestStage;
import ua.woody.questborn.model.QuestTypeConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class IndicatorStatusCalculator {
    private final QuestbornPlugin plugin;

    private String iconAvailable;
    private String iconInProgress;
    private String iconCompleted;
    private String iconCooldown;
    private String iconLocked;
    private String iconRewardAvailable;
    private String iconObjective;

    public IndicatorStatusCalculator(QuestbornPlugin plugin) {
        this.plugin = plugin;
        loadIcons();
    }

    public void loadIcons() {
        this.iconAvailable = ua.woody.questborn.lang.ColorFormatter.applyColors(plugin.getConfig().getString("npc-indicators.icons.available", "<#ffcc66>&l!"));
        this.iconInProgress = ua.woody.questborn.lang.ColorFormatter.applyColors(plugin.getConfig().getString("npc-indicators.icons.in-progress", "<#99ccff>&l?"));
        this.iconCompleted = ua.woody.questborn.lang.ColorFormatter.applyColors(plugin.getConfig().getString("npc-indicators.icons.completed", "<#00ff94>&l?"));
        this.iconCooldown = ua.woody.questborn.lang.ColorFormatter.applyColors(plugin.getConfig().getString("npc-indicators.icons.cooldown", "<#cccccc>&l⏳"));
        this.iconLocked = ua.woody.questborn.lang.ColorFormatter.applyColors(plugin.getConfig().getString("npc-indicators.icons.locked", "<#ff6666>&l🔒"));
        this.iconRewardAvailable = ua.woody.questborn.lang.ColorFormatter.applyColors(plugin.getConfig().getString("npc-indicators.icons.reward-available", "<#a7ff99>&l?"));
        this.iconObjective = ua.woody.questborn.lang.ColorFormatter.applyColors(plugin.getConfig().getString("npc-indicators.icons.objective", "<#7dd3ff>&l?"));
    }

    public boolean shouldBob(String icon) {
        if (icon == null) return false;
        if (icon.equals(iconCompleted) || icon.equals(iconCooldown)) {
            return false;
        }
        return true;
    }

    public List<String> calculateStatus(Player player, PlayerQuestProgress data, String npcId) {
        NpcConfig npcConfig = plugin.getNpcManager().getConfigByNpcId(npcId);

        if (npcConfig != null && plugin.getMoodManager() != null) {
            String currentMood = plugin.getMoodManager().calculateMood(player, npcConfig);
            if (plugin.getConfig().contains("npc-indicators.hide-on-moods")) {
                java.util.List<String> hideMoods = plugin.getConfig().getStringList("npc-indicators.hide-on-moods");
                for (String mood : hideMoods) {
                    if (mood.equalsIgnoreCase(currentMood)) {
                        return null;
                    }
                }
            }
        }

        List<QuestTypeConfig> matchedTypes = new ArrayList<>();
        List<QuestDefinition> matchedQuests = new ArrayList<>();

        if (npcConfig != null) {
            for (String typeId : npcConfig.getStartsTypes()) {
                QuestTypeConfig t = plugin.getQuestManager().getQuestTypeManager().getType(typeId);
                if (t != null && !matchedTypes.contains(t)) matchedTypes.add(t);
            }
            for (String qId : npcConfig.getStartsQuests()) {
                QuestDefinition q = plugin.getQuestManager().getQuest(qId);
                if (q != null && !matchedQuests.contains(q)) matchedQuests.add(q);
            }
        }

        if (!matchedTypes.isEmpty()) {
            matchedQuests.clear();
        }

        boolean hasPendingReward = false;
        boolean hasObjective = false;
        boolean hasActive = false;
        boolean hasAvailable = false;
        boolean hasCooldown = false;
        boolean allLocked = true;
        long now = System.currentTimeMillis();

        for (String qId : data.getPendingRewards()) {
            QuestDefinition q = plugin.getQuestManager().getQuest(qId);
            if (q != null && plugin.getQuestManager().canClaimRewardFromNpc(player, q, npcId)) {
                hasPendingReward = true;
                break;
            }
        }

        if (data.getTrackedQuestId() != null) {
            QuestDefinition activeQ = plugin.getQuestManager().getQuest(data.getTrackedQuestId());
            if (activeQ != null && !data.hasPendingReward(activeQ.getId())) {
                PlayerQuestProgress.ActiveQuestData qd = data.getQuestData(activeQ.getId());
                QuestObjective obj = null;
                if (activeQ.hasStages()) {
                    int currentStage = qd.getCurrentStage();
                    if (currentStage > 0 && currentStage <= activeQ.getStageCount()) {
                        QuestStage stage = activeQ.getStage(currentStage);
                        if (stage != null) obj = stage.getObjective();
                    }
                } else {
                    obj = activeQ.getObjective();
                }

                if (obj != null && ("NPC_INTERACT".equals(obj.getType().name()) || "GIVE_ITEM_TO_NPC".equals(obj.getType().name()))) {
                    if (npcId.equalsIgnoreCase(obj.getNpc())) {
                        hasObjective = true;
                    }
                }
            }
        }

        for (QuestTypeConfig type : matchedTypes) {
            boolean typeUnlocked = plugin.getQuestManager().getQuestTypeManager().isTypeUnlocked(player, type);
            if (typeUnlocked) allLocked = false;

            boolean hasActiveOfSameType = false;
            for (String activeQuestId : data.getActiveQuests().keySet()) {
                QuestDefinition aq = plugin.getQuestManager().getById(activeQuestId);
                if (aq != null && aq.getTypeId().equals(type.getId())) {
                    hasActiveOfSameType = true;
                    break;
                }
            }
            if (!hasActiveOfSameType) {
                for (String pendingId : data.getPendingRewards()) {
                    QuestDefinition aq = plugin.getQuestManager().getById(pendingId);
                    if (aq != null && aq.getTypeId().equals(type.getId())) {
                        hasActiveOfSameType = true;
                        break;
                    }
                }
            }

            Collection<QuestDefinition> questsToCheck;
            if (type.getEngine() == ua.woody.questborn.model.EngineType.ROTATION) {
                if (typeUnlocked) {
                    questsToCheck = plugin.getQuestManager().getOrAssignRotationQuests(player, type);
                } else {
                    questsToCheck = Collections.emptyList();
                }
            } else {
                questsToCheck = plugin.getQuestManager().getByType(type);
            }

            for (QuestDefinition quest : questsToCheck) {
                boolean qStart = npcConfig != null && (npcConfig.getStartsQuests().contains(quest.getId()) || npcConfig.getStartsTypes().contains(type.getId()));
                boolean qFinish = npcConfig != null && (npcConfig.getFinishesQuests().contains(quest.getId()) || npcConfig.getFinishesTypes().contains(type.getId()));
                boolean isOnlyFinish = qFinish && !qStart;

                if (data.hasPendingReward(quest.getId())) {
                    if (qFinish) {
                        hasPendingReward = true;
                    } else if (!isOnlyFinish) {
                        hasActive = true;
                    }
                } else if (!isOnlyFinish && data.hasActiveQuest(quest.getId())) {
                    PlayerQuestProgress.ActiveQuestData qd = data.getQuestData(quest.getId());
                    if (quest.hasStages()) {
                        if (qd.getCurrentStage() >= quest.getStageCount() && qd.isStageCompleted(quest.getStageCount()) && qFinish) {
                            hasPendingReward = true;
                        }
                    } else if (quest.getObjective() != null && qd.getProgress() >= plugin.getQuestManager().getTargetAmount(quest.getObjective()) && qFinish) {
                        hasPendingReward = true;
                    }
                    hasActive = true;
                }
                if (!isOnlyFinish && !hasActiveOfSameType && typeUnlocked && !data.hasActiveQuest(quest.getId()) && !data.hasPendingReward(quest.getId()) && plugin.getQuestManager().isQuestAvailable(player, quest, data)) {
                    hasAvailable = true;
                }
                long cdUntil = data.getQuestCooldownUntil(quest.getId());
                if (cdUntil > now) {
                    hasCooldown = true;
                } else if (type.getEngine() == ua.woody.questborn.model.EngineType.ROTATION) {
                    if (data.getCompletedQuests().contains(quest.getId())) {
                        hasCooldown = true;
                    }
                }
            }
        }

        for (QuestDefinition quest : matchedQuests) {
            allLocked = false;
            QuestTypeConfig type = plugin.getQuestManager().getQuestTypeManager().getType(quest.getTypeId());
            boolean qStart = npcConfig != null && (npcConfig.getStartsQuests().contains(quest.getId()) || (type != null && npcConfig.getStartsTypes().contains(type.getId())));
            boolean qFinish = npcConfig != null && (npcConfig.getFinishesQuests().contains(quest.getId()) || (type != null && npcConfig.getFinishesTypes().contains(type.getId())));
            boolean isOnlyFinish = qFinish && !qStart;

            boolean hasActiveOfSameType = false;
            if (type != null) {
                for (String activeQuestId : data.getActiveQuests().keySet()) {
                    QuestDefinition aq = plugin.getQuestManager().getById(activeQuestId);
                    if (aq != null && aq.getTypeId().equals(type.getId())) {
                        hasActiveOfSameType = true;
                        break;
                    }
                }
                if (!hasActiveOfSameType) {
                    for (String pendingId : data.getPendingRewards()) {
                        QuestDefinition aq = plugin.getQuestManager().getById(pendingId);
                        if (aq != null && aq.getTypeId().equals(type.getId())) {
                            hasActiveOfSameType = true;
                            break;
                        }
                    }
                }
            }

            if (data.hasPendingReward(quest.getId())) {
                if (qFinish) {
                    hasPendingReward = true;
                } else if (!isOnlyFinish) {
                    hasActive = true;
                }
            } else if (!isOnlyFinish && data.hasActiveQuest(quest.getId())) {
                PlayerQuestProgress.ActiveQuestData qd = data.getQuestData(quest.getId());
                if (quest.hasStages()) {
                    if (qd.getCurrentStage() >= quest.getStageCount() && qd.isStageCompleted(quest.getStageCount()) && qFinish) {
                        hasPendingReward = true;
                    }
                } else if (quest.getObjective() != null && qd.getProgress() >= plugin.getQuestManager().getTargetAmount(quest.getObjective()) && qFinish) {
                    hasPendingReward = true;
                }
                hasActive = true;
            }
            if (!isOnlyFinish && !hasActiveOfSameType && !data.hasActiveQuest(quest.getId()) && !data.hasPendingReward(quest.getId()) && plugin.getQuestManager().isQuestAvailable(player, quest, data)) {
                hasAvailable = true;
            }
            long cdUntil = data.getQuestCooldownUntil(quest.getId());
            if (cdUntil > now) {
                hasCooldown = true;
            } else if (type != null && type.getEngine() == ua.woody.questborn.model.EngineType.ROTATION) {
                if (data.getCompletedQuests().contains(quest.getId()) && data.getAssignedRotationQuests(type.getId()).contains(quest.getId())) {
                    hasCooldown = true;
                }
            }
        }

        boolean isQuestGiver = !matchedTypes.isEmpty() || !matchedQuests.isEmpty();

        if (!isQuestGiver && !hasPendingReward && !hasObjective) {
            return null;
        }

        List<String> icons = new ArrayList<>();

        if (hasPendingReward || hasObjective) {
            if (hasPendingReward) icons.add(iconRewardAvailable);
            if (hasObjective) icons.add(iconObjective);
            return icons;
        }

        if (hasAvailable) {
            icons.add(iconAvailable);
        }
        if (hasActive) {
            icons.add(iconInProgress);
        }
        if (hasCooldown && !hasAvailable && !hasActive) {
            icons.add(iconCooldown);
        }
        if (allLocked && isQuestGiver && !hasAvailable && !hasActive && !hasCooldown) {
            icons.add(iconLocked);
        }
        if (isQuestGiver && !hasAvailable && !hasActive && !hasCooldown && !allLocked) {
            icons.add(iconCompleted);
        }

        return icons.isEmpty() ? null : icons;
    }
}
