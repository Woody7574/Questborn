package ua.woody.questborn.managers;

import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.config.ActionBarMode;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;
import ua.woody.questborn.model.QuestStage;
import ua.woody.questborn.storage.PlayerDataStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QuestStateService {
    private final QuestbornPlugin plugin;
    private final QuestManager questManager;
    private final PlayerDataStore playerData;

    private final Map<String, Long> globalRejoinCooldowns = new ConcurrentHashMap<>();

    public QuestStateService(QuestbornPlugin plugin, QuestManager questManager, PlayerDataStore playerData) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.playerData = playerData;
    }

    public boolean activateQuest(Player player, String questId, boolean ignoreRequirements) {
        if (player == null) return false;

        UUID playerId = player.getUniqueId();
        QuestDefinition def = questManager.getQuest(questId);
        if (def == null) return false;

        var data = playerData.get(playerId);

        if (!questManager.getValidator().isQuestAvailable(player, def, data, ignoreRequirements)) {
            return false;
        }

        ua.woody.questborn.model.QuestTypeConfig typeConfigObj = questManager.getQuestTypeManager().getType(def.getTypeId());
        if (typeConfigObj != null && typeConfigObj.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL) {
            int maxP = def.getMaxParticipants();
            if (maxP > 0) {
                ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(questId);
                if (gp.getParticipantCount() >= maxP && !gp.hasParticipant(playerId)) {
                    String msg = plugin.getLanguage().tr("quests.global.full", java.util.Map.of("quest", def.getDisplayName()));
                    if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
                    return false;
                }
            }

            int cooldownSec = plugin.getConfig().getInt("gameplay.global-quests.rejoin-cooldown-seconds", 900);
            if (cooldownSec > 0 && !player.hasPermission("questborn.admin")) {
                String cooldownKey = playerId.toString();
                Long leftAt = globalRejoinCooldowns.get(cooldownKey);
                if (leftAt != null) {
                    long elapsed = (System.currentTimeMillis() - leftAt) / 1000;
                    if (elapsed < cooldownSec) {
                        long remaining = cooldownSec - elapsed;
                        String timeStr = ua.woody.questborn.util.TimeFormatter.format(remaining);
                        String msg = plugin.getLanguage().tr("quests.global.rejoin_cooldown", java.util.Map.of("time", timeStr));
                        if (msg != null && !msg.isEmpty()) player.sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors(msg));
                        return false;
                    } else {
                        globalRejoinCooldowns.remove(cooldownKey);
                    }
                }
            }
        }

        String typeId = def.getTypeId();
        String currentActiveSameType = null;
        for (PlayerQuestProgress.ActiveQuestData qd : data.getActiveQuests().values()) {
            if (!data.hasPendingReward(qd.getQuestId())) {
                QuestDefinition activeDef = questManager.getQuest(qd.getQuestId());
                if (activeDef != null && activeDef.getTypeId().equalsIgnoreCase(typeId)) {
                    if (isGlobalMaxContributionReached(player, activeDef)) {
                        continue;
                    }
                    currentActiveSameType = qd.getQuestId();
                    break;
                }
            }
        }

        if (currentActiveSameType != null && !currentActiveSameType.equalsIgnoreCase(questId)) {
            QuestManager.ActivationConflictMode mode = questManager.getActivationConflictMode();
            if (mode == QuestManager.ActivationConflictMode.BLOCK) {
                return false;
            } else if (mode == QuestManager.ActivationConflictMode.REPLACE) {
                QuestDefinition oldDef = questManager.getQuest(currentActiveSameType);
                notifyGlobalQuestLeave(player, oldDef);
                data.removeActiveQuest(currentActiveSameType);
                playerData.markDirty(playerId);
            } else if (mode == QuestManager.ActivationConflictMode.CHANGE) {
                data.setPendingQuestId(questId);
                playerData.markDirty(playerId);
                return true;
            }
        }

        if (data.hasActiveQuest(questId)) {
            return true;
        }

        ua.woody.questborn.api.events.QuestStartEvent event = new ua.woody.questborn.api.events.QuestStartEvent(player, def);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        PlayerQuestProgress.ActiveQuestData newQuest = new PlayerQuestProgress.ActiveQuestData(questId, System.currentTimeMillis());
        newQuest.setProgress(0);
        newQuest.setCurrentStage(1);
        newQuest.setStageProgress(0);
        newQuest.setItemsTransferred(false);
        newQuest.setPaused(false);

        data.addActiveQuest(newQuest);
        data.clearPendingQuest();

        boolean isGlobal = (typeConfigObj != null && typeConfigObj.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL);
        ua.woody.questborn.model.GlobalQuestProgress gp = null;
        if (isGlobal) {
            gp = plugin.getGlobalQuestDataStore().get(questId);
            if (gp != null && !gp.hasParticipant(playerId)) {
                gp.setContribution(playerId, 0);
                plugin.getGlobalQuestDataStore().save(questId);
            }
        }

        int minP = isGlobal ? def.getMinParticipants() : 0;
        int currentP = (isGlobal && gp != null) ? gp.getParticipantCount() : 0;
        boolean isWaiting = isGlobal && minP > 0 && (gp == null || (!gp.isActivationReached() && currentP < minP));

        if (isGlobal && !isWaiting) {
            globalRejoinCooldowns.put(playerId.toString(), System.currentTimeMillis());
        }

        if (!isWaiting) {
            if (data.getTrackedQuestId() == null || isWaitingGlobal(data.getTrackedQuestId())) {
                data.setTrackedQuestId(questId);
            }
        } else {
            if (data.getTrackedQuestId() == null) {
                data.setTrackedQuestId(questId);
            }
        }

        QuestStage st1 = def.getStage(1);
        QuestObjective firstObj = (st1 != null && st1.isObjective()) ? st1.getObjective() : def.getObjective();

        if (firstObj != null && firstObj.getType() == QuestObjectiveType.LEVEL_UP_REACH) {
            int currentLevel = player.getLevel();
            int targetLevel = firstObj.getAmount();
            int progress = Math.min(currentLevel, targetLevel);

            if (def.hasStages() && st1 != null && st1.isObjective()) {
                newQuest.setStageProgress(progress);
            } else {
                newQuest.setProgress(progress);
            }

            if (currentLevel >= targetLevel) {
                playerData.markDirty(playerId);
                questManager.getProgressProcessor().completeQuest(player, def);
                return true;
            }
        }

        playerData.markDirty(playerId);
        updateVisuals(player, def);

        if (isGlobal) {
            if (minP > 0) {
                if (currentP < minP) {
                    notifyGlobalQuestJoin(player, def);
                } else if (currentP == minP) {
                    if (gp != null) {
                        gp.setActivationReached(true);
                        plugin.getGlobalQuestDataStore().save(def.getId());
                    }
                    triggerGlobalQuestActivationForParticipants(def);
                    notifyGlobalQuestJoin(player, def);
                } else {
                    notifyGlobalQuestJoin(player, def);
                    questManager.getQuestEffectsManager().playQuestActivateEffects(player, def);
                    updateVisuals(player, def);
                }
            } else {
                if (currentP == 1) {
                    broadcastGlobalQuestStart(def);
                }
                notifyGlobalQuestJoin(player, def);
                questManager.getQuestEffectsManager().playQuestActivateEffects(player, def);
                updateVisuals(player, def);
            }
        } else {
            questManager.getNotificationService().sendQuestActivationMessage(player, def, questManager.isShowActivationDetails());
            questManager.getQuestEffectsManager().playQuestActivateEffects(player, def);
            updateVisuals(player, def);
        }

        questManager.getQuestProgressListener().updatePlayerCache(player);
        questManager.updateActiveQuestPlayer(player);
        plugin.getActionLogger().logAction(player, "STARTED_QUEST", questId);

        return true;
    }

    public boolean isWaitingGlobal(String questId) {
        if (questId == null) return false;
        QuestDefinition def = questManager.getQuest(questId);
        if (def == null) return false;

        ua.woody.questborn.model.QuestTypeConfig typeC = questManager.getQuestTypeManager().getType(def.getTypeId());
        if (typeC != null && typeC.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL) {
            int minP = def.getMinParticipants();
            if (minP > 0) {
                ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(def.getId());
                if (gp == null || (!gp.isActivationReached() && gp.getParticipantCount() < minP)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean trackQuest(Player player, String questId) {
        if (player == null) return false;
        var data = playerData.get(player.getUniqueId());
        if (data == null || !data.hasActiveQuest(questId)) return false;

        data.setTrackedQuestId(questId);
        playerData.markDirty(player.getUniqueId());

        updateVisuals(player, questManager.getQuest(questId));
        questManager.getQuestProgressListener().updatePlayerCache(player);
        questManager.updateActiveQuestPlayer(player);
        plugin.getActionLogger().logAction(player, "TRACKED_QUEST", questId);
        return true;
    }

    private void updateVisuals(Player player, QuestDefinition def) {
        if (questManager.getActionBarManager().isEnabled()) {
            if (questManager.getActionBarMode() == ActionBarMode.STATIC) {
                questManager.getActionBarManager().refreshForPlayer(player);
            }
            questManager.getActionBarManager().sendForPlayer(player);
        }
        questManager.getBossBarManager().refreshBar(player);
        questManager.getScoreboardManager().refreshBoard(player);
        if (questManager.getActionBarManager().isEnabled()) {
            questManager.getActionBarManager().refreshForPlayer(player);
            questManager.getActionBarManager().sendForPlayer(player);
        }
    }

    public boolean confirmQuestChange(Player player, String questId, boolean ignoreRequirements) {
        if (player == null) return false;

        UUID playerId = player.getUniqueId();
        var data = playerData.get(playerId);

        if (!questId.equals(data.getPendingQuestId())) {
            return false;
        }

        QuestDefinition def = questManager.getQuest(questId);
        if (def == null) return false;

        if (!questManager.getValidator().isQuestAvailable(player, def, data, ignoreRequirements)) {
            return false;
        }

        ua.woody.questborn.model.QuestTypeConfig typeConfigObj = questManager.getQuestTypeManager().getType(def.getTypeId());
        if (typeConfigObj != null && typeConfigObj.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL) {
            int maxP = def.getMaxParticipants();
            if (maxP > 0) {
                ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(questId);
                if (gp != null && gp.getParticipantCount() >= maxP && !gp.hasParticipant(playerId)) {
                    String msg = plugin.getLanguage().tr("quests.global.full", java.util.Map.of("quest", def.getDisplayName()));
                    if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
                    return false;
                }
            }

            int cooldownSec = plugin.getConfig().getInt("gameplay.global-quests.rejoin-cooldown-seconds", 900);
            if (cooldownSec > 0 && !player.hasPermission("questborn.admin")) {
                String cooldownKey = playerId.toString();
                Long leftAt = globalRejoinCooldowns.get(cooldownKey);
                if (leftAt != null) {
                    long elapsed = (System.currentTimeMillis() - leftAt) / 1000;
                    if (elapsed < cooldownSec) {
                        long remaining = cooldownSec - elapsed;
                        String timeStr = ua.woody.questborn.util.TimeFormatter.format(remaining);
                        String msg = plugin.getLanguage().tr("quests.global.rejoin_cooldown", java.util.Map.of("time", timeStr));
                        if (msg != null && !msg.isEmpty()) player.sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors(msg));
                        return false;
                    }
                }
            }
        }

        String typeId = def.getTypeId();
        String currentActiveSameType = null;
        for (PlayerQuestProgress.ActiveQuestData qd : data.getActiveQuests().values()) {
            if (!data.hasPendingReward(qd.getQuestId())) {
                QuestDefinition activeDef = questManager.getQuest(qd.getQuestId());
                if (activeDef != null && activeDef.getTypeId().equalsIgnoreCase(typeId)) {
                    if (isGlobalMaxContributionReached(player, activeDef)) {
                        continue;
                    }
                    currentActiveSameType = qd.getQuestId();
                    break;
                }
            }
        }

        if (currentActiveSameType != null) {
            QuestDefinition oldDef = questManager.getQuest(currentActiveSameType);
            if (oldDef != null) {
                notifyGlobalQuestLeave(player, oldDef);
            }
            data.removeActiveQuest(currentActiveSameType);
        }

        return activateQuest(player, questId, ignoreRequirements);
    }

    public boolean isGlobalMaxContributionReached(Player player, QuestDefinition quest) {
        if (player == null || quest == null) return false;
        ua.woody.questborn.model.QuestTypeConfig typeC = questManager.getQuestTypeManager().getType(quest.getTypeId());
        if (typeC == null || typeC.getEngine() != ua.woody.questborn.model.EngineType.GLOBAL) return false;
        if (quest.getPersonalLimit() <= 0) return false;
        ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(quest.getId());
        if (gp == null) return false;
        return gp.getContribution(player.getUniqueId()) >= quest.getPersonalLimit();
    }

    public boolean cancelQuest(Player player, String questId) {
        if (player == null) return false;

        UUID playerId = player.getUniqueId();
        var data = playerData.get(playerId);

        if (!data.hasActiveQuest(questId)) {
            return false;
        }

        QuestDefinition def = questManager.getQuest(questId);

        ua.woody.questborn.api.events.QuestCancelEvent event = new ua.woody.questborn.api.events.QuestCancelEvent(player, def);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        ua.woody.questborn.model.QuestTypeConfig typeConfigObj = def != null ? questManager.getQuestTypeManager().getType(def.getTypeId()) : null;
        boolean isGlobal = typeConfigObj != null && typeConfigObj.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL;

        if (isGlobal && def != null) {
            ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(questId);
            int contrib = gp != null ? gp.getContribution(playerId) : 0;
            String cancelMode = plugin.getConfig().getString("gameplay.global-quests.cancel-mode", "ALLOW_WITH_CONTRIBUTION_RETAINED");

            if ("BLOCK_IF_CONTRIBUTED".equalsIgnoreCase(cancelMode) && contrib > 0) {
                String msg = plugin.getLanguage().tr("quests.global.cancel_blocked_contributed");
                if (msg != null && !msg.isEmpty()) {
                    player.sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors(msg));
                }
                return false;
            }

            if ("ALLOW_WITH_CONTRIBUTION_RETAINED".equalsIgnoreCase(cancelMode) && contrib > 0) {
                if (gp != null) {
                    gp.setForfeited(playerId, true);
                    plugin.getGlobalQuestDataStore().save(questId);
                }
            }
        }

        notifyGlobalQuestLeave(player, def);

        data.removeActiveQuest(questId);
        playerData.markDirty(playerId);

        questManager.getBossBarManager().refreshBar(player);
        questManager.getScoreboardManager().refreshBoard(player);
        if (questManager.getActionBarManager().isEnabled()) {
            questManager.getActionBarManager().refreshForPlayer(player);
            questManager.getActionBarManager().sendForPlayer(player);
        }

        questManager.getQuestProgressListener().updatePlayerCache(player);
        questManager.updateActiveQuestPlayer(player);
        plugin.getActionLogger().logAction(player, "CANCELLED_QUEST", questId);

        return true;
    }

    public void notifyGlobalQuestJoin(Player player, QuestDefinition quest) {
        if (quest == null || player == null) return;
        ua.woody.questborn.model.QuestTypeConfig typeConfigObj = questManager.getQuestTypeManager().getType(quest.getTypeId());
        if (typeConfigObj == null || typeConfigObj.getEngine() != ua.woody.questborn.model.EngineType.GLOBAL) return;

        int minP = quest.getMinParticipants();
        ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(quest.getId());
        int currentP = gp != null ? gp.getParticipantCount() : 0;

        String key = (minP > 1 && currentP < minP) ? "quests.global.broadcast.join-waiting" : "quests.global.broadcast.join";
        int remaining = Math.max(0, minP - currentP);

        String msg = plugin.getLanguage().tr(key, java.util.Map.of(
                "player", player.getName(),
                "quest", quest.getDisplayName(),
                "remaining", String.valueOf(remaining)
        ));
        if (msg == null || msg.isEmpty()) return;
        String formatted = ua.woody.questborn.lang.ColorFormatter.applyColors(msg);

        boolean triggeredActivation = (minP > 0 && currentP == minP) || (minP == 0 && currentP == 1);
        if (!triggeredActivation) {
            String personalKey = (minP > 1 && currentP < minP) ? "quests.global.personal.join-waiting" : "quests.global.personal.join";
            String personalMsg = plugin.getLanguage().tr(personalKey, java.util.Map.of(
                    "quest", quest.getDisplayName(),
                    "remaining", String.valueOf(remaining)
            ));
            if (personalMsg != null && !personalMsg.isEmpty()) {
                player.sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors(personalMsg));
            }
        }

        String questId = quest.getId();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getUniqueId().equals(player.getUniqueId())) continue;

            var pData = playerData.get(p.getUniqueId());
            if (pData != null && pData.hasActiveQuest(questId)) {
                p.sendMessage(formatted);
            }
        }
    }

    public void notifyGlobalQuestLeave(Player player, QuestDefinition quest) {
        if (quest == null || player == null) return;
        ua.woody.questborn.model.QuestTypeConfig typeConfigObj = questManager.getQuestTypeManager().getType(quest.getTypeId());
        if (typeConfigObj == null || typeConfigObj.getEngine() != ua.woody.questborn.model.EngineType.GLOBAL) return;

        String msg = plugin.getLanguage().tr("quests.global.broadcast.leave", java.util.Map.of(
                "player", player.getName(),
                "quest", quest.getDisplayName()
        ));
        if (msg == null || msg.isEmpty()) return;
        String formatted = ua.woody.questborn.lang.ColorFormatter.applyColors(msg);

        String questId = quest.getId();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getUniqueId().equals(player.getUniqueId())) continue;
            var pData = playerData.get(p.getUniqueId());
            if (pData != null && pData.hasActiveQuest(questId)) {
                p.sendMessage(formatted);
            }
        }

        ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(questId);
        int minP = quest.getMinParticipants();
        boolean isWaiting = (minP > 0) && (gp == null || !gp.isActivationReached());
        String personalKey = isWaiting ? "quests.global.personal.leave-waiting" : "quests.global.personal.leave";
        String personalMsg = plugin.getLanguage().tr(personalKey, java.util.Map.of("quest", quest.getDisplayName()));
        if (personalMsg != null && !personalMsg.isEmpty()) {
            player.sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors(personalMsg));
        }

        if (gp != null && gp.getContribution(player.getUniqueId()) == 0) {
            gp.getContributions().remove(player.getUniqueId());
            plugin.getGlobalQuestDataStore().save(questId);
        }

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            var pData = playerData.get(p.getUniqueId());
            if (pData != null && pData.hasActiveQuest(questId) && !p.getUniqueId().equals(player.getUniqueId())) {
                updateVisuals(p, quest);
            }
        }
    }

    public void triggerGlobalQuestActivationForParticipants(QuestDefinition quest) {
        if (quest == null) return;
        String questId = quest.getId();

        ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(questId);
        if (gp != null) {
            long now = System.currentTimeMillis();
            for (UUID participantId : gp.getContributions().keySet()) {
                globalRejoinCooldowns.put(participantId.toString(), now);
            }
        }

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            var pData = playerData.get(p.getUniqueId());
            if (pData != null && pData.hasActiveQuest(questId)) {
                questManager.getQuestEffectsManager().playQuestActivateEffects(p, quest);
                updateVisuals(p, quest);
            }
        }
        broadcastGlobalQuestStart(quest);
    }

    public void broadcastGlobalQuestStart(QuestDefinition quest) {
        if (quest == null) return;
        java.util.List<String> lines = plugin.getLanguage().trList("quests.global.broadcast.start", java.util.Map.of(
                "quest", quest.getDisplayName()
        ));
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (lines != null && !lines.isEmpty()) {
                for (String line : lines) {
                    p.sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors(line));
                }
            }
            plugin.getSoundManager().playGlobalQuestStart(p);
        }
    }
}
