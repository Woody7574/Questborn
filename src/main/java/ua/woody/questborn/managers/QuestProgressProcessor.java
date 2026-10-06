package ua.woody.questborn.managers;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.*;
import ua.woody.questborn.storage.PlayerDataStore;

import java.util.*;

public class QuestProgressProcessor {
    private final QuestbornPlugin plugin;
    private final QuestManager questManager;
    private final PlayerDataStore playerData;

    private final Set<UUID> completingPlayers = new HashSet<>();

    public QuestProgressProcessor(QuestbornPlugin plugin, QuestManager questManager, PlayerDataStore playerData) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.playerData = playerData;
    }

    public void incrementProgress(Player player, QuestDefinition quest, int delta) {
        incrementProgress(player, quest, delta, false);
    }

    public void incrementProgress(Player player, QuestDefinition quest, int delta, boolean ignoreTrackingMode) {
        if (delta <= 0 || player == null || quest == null) return;

        QuestTypeConfig typeConfig = questManager.getQuestTypeManager().getType(quest.getTypeId());
        if (typeConfig != null && typeConfig.getEngine() == EngineType.GLOBAL) {
            incrementGlobalProgress(player, quest, delta);
            return;
        }

        UUID playerId = player.getUniqueId();
        var data = playerData.get(playerId);

        PlayerQuestProgress.ActiveQuestData qd = data.getQuestData(quest.getId());
        if (qd == null) return;

        if (qd.isPaused()) return;

        if (!ignoreTrackingMode) {
            String mode = plugin.getConfig().getString("gameplay.quest-tracking-mode", "STRICT_FOCUS");
            if ("STRICT_FOCUS".equalsIgnoreCase(mode)) {
                if (!data.isTracked(quest.getId())) return;
            }
        }

        int currentStageNum = Math.max(1, qd.getCurrentStage());
        if (qd.getCurrentStage() != currentStageNum)
            qd.setCurrentStage(currentStageNum);

        QuestStage currentStage = quest.getStage(currentStageNum);

        if (quest.hasStages()) {
            if (currentStage == null || !currentStage.isObjective()) return;
        } else {
            if (quest.getObjective() == null) return;
        }

        QuestObjective objective = quest.hasStages() ? currentStage.getObjective() : quest.getObjective();
        if (objective == null) return;

        boolean isDistanceQuest = questManager.isDistanceQuestType(objective.getType());
        int target = questManager.getTargetAmount(objective);
        if (target <= 0) return;

        if (completingPlayers.contains(playerId)) {
            return;
        }

        int currentVal = quest.hasStages() ? qd.getStageProgress() : qd.getProgress();

        if (questManager.isOptimizeDistanceQuests() && isDistanceQuest && delta < questManager.getMinDistanceSave() && (currentVal + delta < target)) {
            return;
        }

        if (currentVal >= target) {
            if (quest.hasStages()) completeCurrentStage(player, quest, data);
            else handleQuestEndReached(player, quest, data);
            return;
        }

        int newValue = currentVal + delta;
        if (newValue > target) newValue = target;

        ua.woody.questborn.api.events.ObjectiveProgressEvent progressEvent = new ua.woody.questborn.api.events.ObjectiveProgressEvent(player, quest, objective, newValue, target);
        org.bukkit.Bukkit.getPluginManager().callEvent(progressEvent);

        if (quest.hasStages()) qd.setStageProgress(newValue);
        else qd.setProgress(newValue);

        playerData.markDirty(playerId);

        if (newValue >= target) {
            ua.woody.questborn.api.events.ObjectiveCompleteEvent completeEvent = new ua.woody.questborn.api.events.ObjectiveCompleteEvent(player, quest, objective);
            org.bukkit.Bukkit.getPluginManager().callEvent(completeEvent);

            completingPlayers.add(playerId);

            questManager.getActionBarManager().sendForPlayer(player);
            questManager.getBossBarManager().updateBar(player);
            questManager.getScoreboardManager().updateBoard(player);

            plugin.getFoliaLib().getImpl().runAtEntityLater(player, () -> {
                completingPlayers.remove(playerId);

                Player p = plugin.getServer().getPlayer(playerId);
                if (p == null || !p.isOnline()) return;

                var currentData = playerData.get(playerId);
                if (currentData == null) return;

                PlayerQuestProgress.ActiveQuestData currentQd = currentData.getQuestData(quest.getId());
                if (currentQd == null) return;

                if (quest.hasStages() && currentQd.getCurrentStage() != currentStageNum) return;

                if (quest.hasStages()) completeCurrentStage(p, quest, currentData);
                else handleQuestEndReached(p, quest, currentData);
            }, quest.hasStages() && currentStageNum < quest.getStageCount() ? questManager.getStageCompleteDelay() : questManager.getQuestCompleteDelay());
        } else {
            ua.woody.questborn.config.ActionBarMode abm = questManager.getActionBarMode();
            if (abm == ua.woody.questborn.config.ActionBarMode.ON_PROGRESS_CHANGE) {
                questManager.getActionBarManager().sendForPlayer(player);
            }

            questManager.getBossBarManager().updateBar(player);
            questManager.getScoreboardManager().updateBoard(player);
        }
    }

    public void incrementGlobalProgress(Player player, QuestDefinition quest, int delta) {
        UUID playerId = player.getUniqueId();
        var data = playerData.get(playerId);
        PlayerQuestProgress.ActiveQuestData qd = data.getQuestData(quest.getId());
        if (qd == null || qd.isPaused()) return;

        String mode = plugin.getConfig().getString("gameplay.quest-tracking-mode", "STRICT_FOCUS");
        if ("STRICT_FOCUS".equalsIgnoreCase(mode)) {
            if (!data.isTracked(quest.getId())) return;
        }

        GlobalQuestProgress globalProgress = plugin.getGlobalQuestDataStore().get(quest.getId());
        if (globalProgress.isCompleted()) return;

        int minParticipants = quest.getMinParticipants();
        if (minParticipants > 0 && !globalProgress.isActivationReached() && globalProgress.getParticipantCount() < minParticipants) {
            String msg = plugin.getLanguage().tr("quests.global.not_enough_participants", Map.of(
                    "quest", quest.getDisplayName(),
                    "min", String.valueOf(minParticipants),
                    "current", String.valueOf(globalProgress.getParticipantCount())
            ));
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors(msg));
            }
            return;
        }

        int globalGoal = getGlobalGoal(quest);
        if (globalGoal <= 0) return;

        int personalLimit = quest.getPersonalLimit();
        if (personalLimit > 0 && qd.getProgress() >= personalLimit) {
            return;
        }

        int actualDelta = delta;
        int oldPersonal;
        int newPersonal;
        boolean justCompleted = false;

        synchronized (globalProgress) {
            if (globalProgress.isCompleted()) return;

            if (personalLimit > 0 && qd.getProgress() + actualDelta > personalLimit) {
                actualDelta = personalLimit - qd.getProgress();
            }

            int currentGlobal = globalProgress.getGlobalProgress();
            if (currentGlobal + actualDelta > globalGoal) {
                actualDelta = globalGoal - currentGlobal;
            }

            if (actualDelta <= 0) return;

            oldPersonal = qd.getProgress();
            newPersonal = oldPersonal + actualDelta;
            qd.setProgress(newPersonal);

            int oldContribution = globalProgress.getContribution(playerId);
            globalProgress.setContribution(playerId, oldContribution + actualDelta);
            globalProgress.setLastContributionTime(playerId, System.currentTimeMillis());

            if (globalProgress.getStartedAt() == 0L) {
                globalProgress.setStartedAt(System.currentTimeMillis());
            }

            int updatedGlobal = globalProgress.addGlobalProgress(actualDelta);
            if (updatedGlobal >= globalGoal && !globalProgress.isCompleted()) {
                globalProgress.setCompleted(true);
                justCompleted = true;
            }
        }

        playerData.markDirty(playerId);

        int rewardStep = quest.getRewardStep();
        if (rewardStep > 0) {
            int oldSteps = oldPersonal / rewardStep;
            int newSteps = newPersonal / rewardStep;
            int stepsEarned = newSteps - oldSteps;
            if (stepsEarned > 0) {
                Map<String, Object> allRewards = quest.getRewards();
                if (allRewards != null && allRewards.containsKey("step-rewards")) {
                    Map<String, Object> stepRewardsMap = null;
                    Object obj = allRewards.get("step-rewards");
                    if (obj instanceof Map) {
                        stepRewardsMap = (Map<String, Object>) obj;
                    } else if (obj instanceof org.bukkit.configuration.ConfigurationSection) {
                        stepRewardsMap = ((org.bukkit.configuration.ConfigurationSection) obj).getValues(false);
                    }
                    if (stepRewardsMap != null) {
                        ua.woody.questborn.rewards.RewardHandler.giveCustomRewards(player, quest, stepRewardsMap, stepsEarned);
                    }
                }
            }
        }

        if (personalLimit > 0 && newPersonal >= personalLimit && oldPersonal < personalLimit) {
            plugin.getFoliaLib().getImpl().runAtEntityLater(player, () -> {
                if (player.isOnline()) {
                    String msg = plugin.getLanguage().tr("quests.global.max_contribution", Map.of("quest", quest.getDisplayName()));
                    player.sendMessage(msg);

                    questManager.getQuestEffectsManager().playQuestFinishEffects(player, quest);

                    ua.woody.questborn.model.PlayerQuestProgress currentData = plugin.getPlayerDataStore().get(player.getUniqueId());
                    if (currentData != null && quest.getId().equals(currentData.getTrackedQuestId())) {
                        currentData.setTrackedQuestId(null);
                        plugin.getPlayerDataStore().markDirty(player.getUniqueId());
                        questManager.getBossBarManager().updateBar(player);
                        questManager.getScoreboardManager().updateBoard(player);
                    }
                }
            }, questManager.getStageCompleteDelay());

            plugin.getActionLogger().logAction(player, "GLOBAL_MAX_CONTRIBUTION", quest.getId());
        }

        plugin.getGlobalQuestDataStore().save(quest.getId());

        ua.woody.questborn.config.ActionBarMode abm = questManager.getActionBarMode();
        if (abm == ua.woody.questborn.config.ActionBarMode.ON_PROGRESS_CHANGE) {
            questManager.getActionBarManager().sendForPlayer(player);
        }

        questManager.getBossBarManager().updateBar(player);
        questManager.getScoreboardManager().updateBoard(player);

        if (justCompleted) {
            plugin.getFoliaLib().getImpl().runLater(() -> {
                completeGlobalQuest(quest, globalProgress);
            }, questManager.getQuestCompleteDelay());
        }
    }

    public int getGlobalGoal(QuestDefinition quest) {
        if (!quest.hasStages()) return 0;
        QuestStage stage = quest.getStage(1);
        if (stage == null) return 0;
        if (stage.isObjective()) {
            return questManager.getTargetAmount(stage.getObjective());
        } else if (stage.isRequiredMaterials()) {
            int total = 0;
            for (int amount : stage.getRequiredItems().values()) {
                total += amount;
            }
            return total;
        }
        return 0;
    }

    public void forceCompleteGlobalQuest(QuestDefinition quest) {
        QuestTypeConfig typeConfig = questManager.getQuestTypeManager().getType(quest.getTypeId());
        if (typeConfig == null || typeConfig.getEngine() != EngineType.GLOBAL) return;
        GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(quest.getId());
        if (gp == null || gp.isCompleted()) return;

        int globalGoal = getGlobalGoal(quest);
        gp.setGlobalProgress(globalGoal);
        gp.setCompleted(true);
        plugin.getGlobalQuestDataStore().save(quest.getId());
        plugin.getFoliaLib().getImpl().runLater(() -> {
            completeGlobalQuest(quest, gp);
        }, questManager.getQuestCompleteDelay());
    }

    private void completeGlobalQuest(QuestDefinition quest, GlobalQuestProgress globalProgress) {
        List<Map.Entry<UUID, Integer>> topPlayers = new ArrayList<>(globalProgress.getContributions().entrySet());
        topPlayers.sort((a, b) -> {
            int cmp = b.getValue().compareTo(a.getValue());
            if (cmp != 0) return cmp;
            long timeA = globalProgress.getLastContributionTime(a.getKey());
            long timeB = globalProgress.getLastContributionTime(b.getKey());
            return Long.compare(timeA, timeB);
        });

        int goal = getGlobalGoal(quest);
        String timeStr;
        if (globalProgress.getStartedAt() > 0) {
            long timeTaken = (System.currentTimeMillis() - globalProgress.getStartedAt()) / 1000;
            timeStr = ua.woody.questborn.util.TimeFormatter.format(timeTaken);
        } else {
            timeStr = plugin.getLanguage().tr("quests.global.broadcast.time-unknown");
            if (timeStr == null || timeStr.isEmpty()) timeStr = "unknown";
        }

        List<String> broadcastLines = new ArrayList<>();

        List<String> headerLines = plugin.getLanguage().trList("quests.global.broadcast.header", Map.of("quest", quest.getDisplayName()));
        if (headerLines != null && !headerLines.isEmpty()) {
            broadcastLines.addAll(headerLines);
        }

        String topEntryFormat = plugin.getLanguage().tr("quests.global.broadcast.top-entry");
        if (topEntryFormat != null && !topEntryFormat.isEmpty()) {
            for (int i = 0; i < Math.min(3, topPlayers.size()); i++) {
                Map.Entry<UUID, Integer> entry = topPlayers.get(i);
                org.bukkit.OfflinePlayer op = plugin.getServer().getOfflinePlayer(entry.getKey());
                String name = op.getName() != null ? op.getName() : "Unknown";
                int perc = (int) Math.round((entry.getValue() * 100.0) / Math.max(1, goal));

                String entryLine = topEntryFormat
                        .replace("{rank}", String.valueOf(i + 1))
                        .replace("{player}", name)
                        .replace("{contribution}", String.valueOf(entry.getValue()))
                        .replace("{percent}", String.valueOf(perc));
                broadcastLines.add(entryLine);
            }
        }

        List<String> footerLines = plugin.getLanguage().trList("quests.global.broadcast.footer",
                Map.of("participants", String.valueOf(globalProgress.getParticipantCount()),
                       "time", timeStr));
        if (footerLines != null && !footerLines.isEmpty()) {
            broadcastLines.addAll(footerLines);
        }

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            for (String line : broadcastLines) {
                p.sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors(line));
            }
            if (globalProgress.hasParticipant(p.getUniqueId())) {
                int pCont = globalProgress.getContribution(p.getUniqueId());
                String yourContrib = plugin.getLanguage().tr("quests.global.broadcast.your-contribution", Map.of("amount", String.valueOf(pCont)));
                if (yourContrib != null && !yourContrib.isEmpty()) {
                    p.sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors(yourContrib));
                }
            }
            p.sendMessage(" ");
            questManager.getSoundManager().playGlobalQuestComplete(p);
        }

        for (String line : broadcastLines) {
            plugin.getServer().getConsoleSender().sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors(line));
        }
        plugin.getServer().getConsoleSender().sendMessage(" ");

        Map<String, Object> allRewards = quest.getRewards();
        Map<String, Object> maxContributionBonus = null;
        if (allRewards != null) {
            Object obj = allRewards.get("max-contribution-bonus");
            if (obj instanceof Map) {
                maxContributionBonus = (Map<String, Object>) obj;
            } else if (obj instanceof org.bukkit.configuration.ConfigurationSection) {
                maxContributionBonus = ((org.bukkit.configuration.ConfigurationSection) obj).getValues(false);
            }
        }

        for (Map.Entry<UUID, Integer> entry : globalProgress.getContributions().entrySet()) {
            UUID uuid = entry.getKey();
            int contribution = entry.getValue();
            if (contribution <= 0) continue;

            PlayerQuestProgress data = playerData.get(uuid);
            if (data == null) continue;

            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                if (maxContributionBonus != null && quest.getPersonalLimit() > 0 && contribution >= quest.getPersonalLimit()) {
                    ua.woody.questborn.rewards.RewardHandler.giveCustomRewards(player, quest, maxContributionBonus);
                }
                completeQuest(player, quest);
            } else {
            }
        }

        for (Map.Entry<UUID, PlayerQuestProgress> mapEntry : playerData.getAll().entrySet()) {
            UUID pUuid = mapEntry.getKey();
            PlayerQuestProgress data = mapEntry.getValue();
            if (data.hasActiveQuest(quest.getId())) {
                data.removeActiveQuest(quest.getId());
                playerData.markDirty(pUuid);

                Player p = plugin.getServer().getPlayer(pUuid);
                if (p != null && p.isOnline()) {
                    questManager.getActionBarManager().sendForPlayer(p);
                    questManager.getBossBarManager().refreshBar(p);
                    questManager.getScoreboardManager().refreshBoard(p);
                    questManager.updateActiveQuestPlayer(p);
                }
            }
        }
    }

    public void completeCurrentStage(Player player, QuestDefinition quest, PlayerQuestProgress data) {
        PlayerQuestProgress.ActiveQuestData qd = data.getQuestData(quest.getId());
        if (qd == null) return;
        int currentStageNum = Math.max(1, qd.getCurrentStage());
        QuestStage currentStage = quest.getStage(currentStageNum);

        if (currentStage == null) return;

        ua.woody.questborn.api.events.StageCompleteEvent event = new ua.woody.questborn.api.events.StageCompleteEvent(player, quest, currentStageNum);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);

        qd.setStageCompleted(currentStageNum, true);

        if (currentStageNum >= quest.getStageCount()) {
            handleQuestEndReached(player, quest, data);
            return;
        }

        qd.setCurrentStage(currentStageNum + 1);
        qd.setStageProgress(0);
        qd.setItemsTransferred(false);

        String trackingMode = plugin.getConfig().getString("gameplay.quest-tracking-mode", "STRICT_FOCUS");
        boolean isBackground = "BACKGROUND_PROGRESS".equalsIgnoreCase(trackingMode)
                && data.getTrackedQuestId() != null
                && !data.getTrackedQuestId().equalsIgnoreCase(quest.getId());

        if (isBackground) {
            String bgMsg = plugin.getLanguage().tr("quests.background.stage_complete",
                    Map.of("stage", String.valueOf(currentStageNum), "quest", String.valueOf(quest.getDisplayName())));
            if (bgMsg != null && !bgMsg.isEmpty() && !bgMsg.equals("quests.background.stage_complete")) {
                if (quest.getStageCount() > 1) player.sendMessage(bgMsg);
            }
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        } else {
            String stageCompleteMsg = plugin.getLanguage().tr("quests.stage.complete",
                    Map.of("stage", String.valueOf(currentStageNum), "quest", String.valueOf(quest.getDisplayName())));

            if (stageCompleteMsg != null && !stageCompleteMsg.isEmpty()) {
                if (quest.getStageCount() > 1) player.sendMessage(stageCompleteMsg);
            }

            questManager.getQuestEffectsManager().playQuestStageChangeEffects(player, quest);

            questManager.getNotificationService().sendNewStageMessage(player, quest, currentStageNum + 1, questManager.isShowActivationDetails());
        }

        QuestStage nextStage = quest.getStage(currentStageNum + 1);
        if (nextStage != null && questManager.getActionBarManager().isEnabled()) {
            questManager.getActionBarManager().sendForPlayer(player);
        }

        questManager.getBossBarManager().updateBar(player);
        questManager.getScoreboardManager().updateBoard(player);

        playerData.markDirty(player.getUniqueId());
        questManager.getQuestProgressListener().updatePlayerCache(player);
        plugin.getActionLogger().logAction(player, "ADVANCED_STAGE", quest.getId() + " (stage " + (currentStageNum + 1) + ")");
    }

    public boolean forceFinishStage(Player player) {
        if (player == null) return false;

        UUID playerId = player.getUniqueId();
        PlayerQuestProgress data = playerData.get(playerId);

        String activeId = data.getTrackedQuestId();
        if (activeId == null) {
            if (!data.getActiveQuests().isEmpty()) {
                activeId = data.getActiveQuests().keySet().iterator().next();
            } else {
                return false;
            }
        }

        QuestDefinition quest = questManager.getQuest(activeId);
        if (quest == null) return false;

        PlayerQuestProgress.ActiveQuestData qd = data.getQuestData(activeId);
        if (qd == null) return false;

        if (!quest.hasStages()) {
            complete(player, quest);
            return true;
        }

        int currentStageNum = Math.max(1, qd.getCurrentStage());
        QuestStage currentStage = quest.getStage(currentStageNum);

        if (currentStage != null) {
            if (currentStage.isObjective()) {
                int t = questManager.getTargetAmount(currentStage.getObjective());
                qd.setStageProgress(Math.max(0, t));
            } else if (currentStage.isRequiredMaterials()) {
                qd.setItemsTransferred(true);
            }
        }

        playerData.markDirty(playerId);

        questManager.getActionBarManager().sendForPlayer(player);
        questManager.getBossBarManager().updateBar(player);
        questManager.getScoreboardManager().updateBoard(player);

        completingPlayers.add(playerId);

        plugin.getFoliaLib().getImpl().runAtEntityLater(player, () -> {
            completingPlayers.remove(playerId);
            Player p = plugin.getServer().getPlayer(playerId);
            if (p == null || !p.isOnline()) return;

            var currentData = playerData.get(playerId);
            if (currentData == null) return;

            if (!currentData.hasActiveQuest(quest.getId())) return;

            if (currentStageNum >= quest.getStageCount()) {
                handleQuestEndReached(p, quest, currentData);
            } else {
                completeCurrentStage(p, quest, currentData);
            }
        }, currentStageNum < quest.getStageCount() ? questManager.getStageCompleteDelay() : questManager.getQuestCompleteDelay());

        return true;
    }

    public boolean transferItems(Player player, QuestDefinition quest, Map<QuestItem, Integer> takenItems) {
        if (player == null || quest == null) return false;

        UUID playerId = player.getUniqueId();
        var data = playerData.get(playerId);
        PlayerQuestProgress.ActiveQuestData qd = data.getQuestData(quest.getId());
        if (qd == null) return false;

        int currentStageNum = Math.max(1, qd.getCurrentStage());
        QuestStage currentStage = quest.getStage(currentStageNum);

        if (currentStage == null || !currentStage.isRequiredMaterials()) {
            return false;
        }

        Map<QuestItem, Integer> required = currentStage.getRequiredItems();
        boolean allMet = true;

        for (Map.Entry<QuestItem, Integer> entry : required.entrySet()) {
            QuestItem item = entry.getKey();
            int requiredAmount = entry.getValue();
            int takenAmount = takenItems.getOrDefault(item, 0);

            if (takenAmount > 0) {
                qd.addTransferredItem(item, takenAmount);
            }

            int totalTransferred = qd.getTransferredAmount(item);
            if (totalTransferred < requiredAmount) {
                allMet = false;
            }
        }

        playerData.markDirty(playerId);

        if (allMet) {
            qd.setItemsTransferred(true);
            String successMsg = plugin.getLanguage().tr("quests.transfer.success");
            if (successMsg != null && !successMsg.isEmpty()) {
                player.sendMessage(successMsg);
            }
            completingPlayers.add(playerId);
            questManager.getActionBarManager().sendForPlayer(player);
            questManager.getBossBarManager().updateBar(player);
            questManager.getScoreboardManager().updateBoard(player);

            plugin.getFoliaLib().getImpl().runAtEntityLater(player, () -> {
                completingPlayers.remove(playerId);
                Player p = plugin.getServer().getPlayer(playerId);
                if (p == null || !p.isOnline()) return;

                var currentData = playerData.get(playerId);
                if (currentData == null) return;

                PlayerQuestProgress.ActiveQuestData currentQd = currentData.getQuestData(quest.getId());
                if (currentQd == null) return;

                if (quest.hasStages() && currentQd.getCurrentStage() != currentStageNum) return;

                completeCurrentStage(p, quest, currentData);
            }, currentStageNum < quest.getStageCount() ? questManager.getStageCompleteDelay() : questManager.getQuestCompleteDelay());
        }

        return true;
    }

    public void completeQuest(Player player, QuestDefinition quest) {
        completeQuest(player, quest, false);
    }

    public void completeQuest(Player player, QuestDefinition quest, boolean isClaimingReward) {
        if (player == null || quest == null) return;

        ua.woody.questborn.api.events.QuestCompleteEvent event = new ua.woody.questborn.api.events.QuestCompleteEvent(player, quest);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);

        UUID playerId = player.getUniqueId();
        var data = playerData.get(playerId);

        data.incrementCompleted(quest.getTypeId());
        playerData.markDirty(playerId);

        QuestTypeConfig typeConfig = questManager.getQuestTypeManager().getType(quest.getTypeId());
        boolean hasNpc = false;
        String finishNpcId = null;
        if (plugin.getNpcManager() != null) {
            for (ua.woody.questborn.model.NpcConfig npcVal : plugin.getNpcManager().getAll()) {
                if (npcVal.getFinishesQuests().contains(quest.getId()) || npcVal.getFinishesTypes().contains(quest.getTypeId())) {
                    hasNpc = true;
                    finishNpcId = npcVal.getNpcId();
                    break;
                }
            }
        }

        if (typeConfig == null) {
            plugin.getLogger().warning("[QuestManager] Quest completed " + quest.getId() + " but TypeConfig is NULL for " + quest.getTypeId());
        }

        String trackingMode = plugin.getConfig().getString("gameplay.quest-tracking-mode", "STRICT_FOCUS");
        boolean isBackground = "BACKGROUND_PROGRESS".equalsIgnoreCase(trackingMode)
                && data.getTrackedQuestId() != null
                && !data.getTrackedQuestId().equalsIgnoreCase(quest.getId());

        if (isBackground) {
            String bgMsg = plugin.getLanguage().tr("quests.background.complete",
                    Map.of("quest", String.valueOf(quest.getDisplayName())));
            if (bgMsg != null && !bgMsg.isEmpty() && !bgMsg.equals("quests.background.complete")) {
                player.sendMessage(bgMsg);
            }
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        } else {
            questManager.getBossBarManager().removeBar(player);
            questManager.getScoreboardManager().removeBoard(player);
            questManager.getActionBarManager().sendCompleteMessage(player, quest);

            if (typeConfig == null || typeConfig.getEngine() != EngineType.GLOBAL) {
                questManager.getQuestEffectsManager().playQuestFinishEffects(player, quest);
            }
        }

        boolean autoClaim = plugin.getConfig().getBoolean("rewards.auto-claim", false);
        if (hasNpc) {
            autoClaim = false;
        }

        if (isClaimingReward) {
            autoClaim = true;
        }

        if (!autoClaim) {
            data.addPendingReward(quest.getId());
            data.addCompletedQuest(quest.getId());

            if (hasNpc) {
                String npcName = finishNpcId != null && plugin.getNpcManager() != null ? plugin.getNpcManager().getDisplayName(finishNpcId) : finishNpcId;
                String msg = plugin.getLanguage().tr("quests.reward.waiting-npc", java.util.Map.of("npc", npcName != null ? npcName : ""));
                if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
            } else {
                String msg = plugin.getLanguage().tr("quests.reward.waiting-gui");
                if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
            }

        } else {
            if (!isClaimingReward) {
                ua.woody.questborn.rewards.RewardHandler.giveRewards(player, quest);
            }
            data.setRewardClaimed(quest.getId(), true);
        }

        if (typeConfig != null) {
            if (typeConfig.getCooldownSeconds() > 0) {
                long cooldownUntil = System.currentTimeMillis() + typeConfig.getCooldownSeconds() * 1000L;
                data.setQuestCooldownUntil(quest.getId(), cooldownUntil);
            }

            if (typeConfig.getCooldownSeconds() == 0) {
                data.addCompletedQuest(quest.getId());
            }

            data.setLastTypeCompletion(quest.getTypeId(), System.currentTimeMillis());

            data.removeActiveQuest(quest.getId());
            playerData.markDirty(playerId);

            if (typeConfig.getEngine() == EngineType.CHAIN && typeConfig.isAutoActivateNext()) {
                    Collection<QuestDefinition> typeQuests = questManager.getByType(typeConfig);
                    for (QuestDefinition q : typeQuests) {
                        if (q.getRequiredQuests() != null && q.getRequiredQuests().contains(quest.getId())) {
                            if (questManager.getValidator().isQuestAvailable(player, q, data, false)) {
                                if (questManager.getAutoActivateNextDelay() > 0) {
                                    plugin.getFoliaLib().getImpl().runAtEntityLater(player, () -> {
                                        if (!player.isOnline()) return;
                                        questManager.getStateService().activateQuest(player, q.getId(), false);
                                        String msg = plugin.getLanguage().tr("quests.chain.auto-activated", Map.of("quest", q.getDisplayName()));
                                        if (msg != null) player.sendMessage(msg);
                                    }, questManager.getAutoActivateNextDelay());
                                } else {
                                    questManager.getStateService().activateQuest(player, q.getId(), false);
                                    String msg = plugin.getLanguage().tr("quests.chain.auto-activated", Map.of("quest", q.getDisplayName()));
                                    if (msg != null) player.sendMessage(msg);
                                }
                                break;
                            }
                        }
                    }
                }
        } else {
            data.removeActiveQuest(quest.getId());
            playerData.markDirty(playerId);
        }

        questManager.getQuestProgressListener().updatePlayerCache(player);
        questManager.updateActiveQuestPlayer(player);
        plugin.getActionLogger().logAction(player, "COMPLETED_QUEST", quest.getId());
    }

    public boolean claimReward(Player player, QuestDefinition quest) {
        if (player == null || quest == null) return false;

        UUID playerId = player.getUniqueId();
        var data = playerData.get(playerId);

        if (!data.hasPendingReward(quest.getId())) {
            return false;
        }

        ua.woody.questborn.rewards.RewardHandler.giveRewards(player, quest);

        data.removePendingReward(quest.getId());

        if (data.hasActiveQuest(quest.getId())) {
            completeQuest(player, quest, true);
        } else {
            data.setRewardClaimed(quest.getId(), true);
        }

        playerData.markDirty(playerId);
        plugin.getActionLogger().logAction(player, "CLAIMED_REWARD", quest.getId());

        return true;
    }

    public void complete(Player player, QuestDefinition quest) {
        if (player == null || quest == null) return;

        UUID playerId = player.getUniqueId();
        var data = playerData.get(playerId);

        PlayerQuestProgress.ActiveQuestData qd = data.getQuestData(quest.getId());
        if (qd == null) return;

        QuestTypeConfig typeConfig = questManager.getQuestTypeManager().getType(quest.getTypeId());
        if (typeConfig != null && typeConfig.getEngine() == EngineType.GLOBAL) {
            int globalGoal = getGlobalGoal(quest);
            int personalLimit = quest.getPersonalLimit();
            int limit = personalLimit > 0 ? personalLimit : globalGoal;
            int remaining = limit - qd.getProgress();

            if (remaining > 0) {
                incrementGlobalProgress(player, quest, remaining);
            }
            return;
        }

        if (quest.hasStages()) {
            for (int i = 1; i <= quest.getStageCount(); i++) {
                QuestStage st = quest.getStage(i);
                if (st == null) continue;

                qd.setStageCompleted(i, true);

                if (i == quest.getStageCount()) {
                    if (st.isObjective()) {
                        int t = questManager.getTargetAmount(st.getObjective());
                        qd.setStageProgress(Math.max(0, t));
                    } else if (st.isRequiredMaterials()) {
                        qd.setItemsTransferred(true);
                    }
                }
            }
            qd.setCurrentStage(quest.getStageCount());
        } else {
            int target = questManager.getTargetAmount(quest.getObjective());
            qd.setProgress(Math.max(0, target));
        }

        playerData.markDirty(playerId);

        questManager.getActionBarManager().sendForPlayer(player);
        questManager.getBossBarManager().updateBar(player);
        questManager.getScoreboardManager().updateBoard(player);

        completingPlayers.add(playerId);

        plugin.getFoliaLib().getImpl().runAtEntityLater(player, () -> {
            completingPlayers.remove(playerId);
            Player p = plugin.getServer().getPlayer(playerId);
            if (p == null || !p.isOnline()) return;

            var currentData = playerData.get(playerId);
            if (currentData != null) {
                handleQuestEndReached(p, quest, currentData);
            }
        }, questManager.getQuestCompleteDelay());
    }

    private void handleQuestEndReached(Player player, QuestDefinition quest, PlayerQuestProgress data) {
        if (quest.hasStages()) {
            PlayerQuestProgress.ActiveQuestData qd = data.getQuestData(quest.getId());
            if (qd != null) {
                int currentStageNum = Math.max(1, qd.getCurrentStage());
                String trackingMode = plugin.getConfig().getString("gameplay.quest-tracking-mode", "STRICT_FOCUS");
                boolean isBackground = "BACKGROUND_PROGRESS".equalsIgnoreCase(trackingMode)
                        && data.getTrackedQuestId() != null
                        && !data.getTrackedQuestId().equalsIgnoreCase(quest.getId());

                if (isBackground) {
                    String bgMsg = plugin.getLanguage().tr("quests.background.stage_complete",
                            Map.of("stage", String.valueOf(currentStageNum), "quest", String.valueOf(quest.getDisplayName())));
                    if (bgMsg != null && !bgMsg.isEmpty() && !bgMsg.equals("quests.background.stage_complete")) {
                        if (quest.getStageCount() > 1) player.sendMessage(bgMsg);
                    }
                } else {
                    String stageCompleteMsg = plugin.getLanguage().tr("quests.stage.complete",
                            Map.of("stage", String.valueOf(currentStageNum), "quest", String.valueOf(quest.getDisplayName())));
                    if (stageCompleteMsg != null && !stageCompleteMsg.isEmpty()) {
                        if (quest.getStageCount() > 1) player.sendMessage(stageCompleteMsg);
                    }
                }
            }
        }

        boolean hasFinishNpc = false;
        String finishNpcId = null;
        if (plugin.getNpcManager() != null) {
            for (ua.woody.questborn.model.NpcConfig npcVal : plugin.getNpcManager().getAll()) {
                if (npcVal.getFinishesQuests().contains(quest.getId()) || npcVal.getFinishesTypes().contains(quest.getTypeId())) {
                    hasFinishNpc = true;
                    finishNpcId = npcVal.getNpcId();
                    break;
                }
            }
        }

        if (hasFinishNpc) {
            if (!data.hasPendingReward(quest.getId())) {
                data.addPendingReward(quest.getId());

                if (quest.getId().equalsIgnoreCase(data.getTrackedQuestId())) {
                    data.setTrackedQuestId(null);
                }

                playerData.markDirty(player.getUniqueId());
                String npcName = plugin.getNpcManager() != null ? plugin.getNpcManager().getDisplayName(finishNpcId) : finishNpcId;
                String msg = plugin.getLanguage().tr("quests.reward.waiting-npc", java.util.Map.of("npc", npcName != null ? npcName : ""));
                if (msg != null && !msg.isEmpty()) player.sendMessage(msg);

                questManager.getBossBarManager().updateBar(player);
                questManager.getScoreboardManager().updateBoard(player);
                if (questManager.getActionBarManager().isEnabled()) {
                    questManager.getActionBarManager().sendForPlayer(player);
                }
                questManager.getQuestEffectsManager().playQuestStageChangeEffects(player, quest);
            }
        } else {
            completeQuest(player, quest);
        }
    }

    public void updateTravelBuffer(UUID playerId, double distance) {
        if (distance <= 0) return;

        var data = playerData.get(playerId);

        if (questManager.isOptimizeDistanceQuests() && distance < questManager.getMinDistanceSave()) {
            return;
        }

        String activeQuestId = data.getTrackedQuestId();
        if (activeQuestId != null) {
            QuestDefinition activeQuest = questManager.getQuest(activeQuestId);
            if (activeQuest != null) {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null) {
                    incrementProgress(player, activeQuest, (int) distance);
                }
            }
        }
    }
}
