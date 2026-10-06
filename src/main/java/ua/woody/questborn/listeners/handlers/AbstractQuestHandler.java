package ua.woody.questborn.listeners.handlers;

import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;
import ua.woody.questborn.model.QuestStage;

public abstract class AbstractQuestHandler {
    public static final ThreadLocal<String> FORCE_ACTIVE_QUEST = new ThreadLocal<>();

    protected final QuestbornPlugin plugin;

    public AbstractQuestHandler(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    public QuestbornPlugin getPlugin() {
        return this.plugin;
    }

    protected PlayerQuestProgress getData(Player player) {
        return plugin.getPlayerDataStore().get(player.getUniqueId());
    }

    protected QuestDefinition getActiveQuest(Player player) {
        if (player == null) return null;

        if (plugin.getQuestManager() != null && plugin.getQuestManager().getQuestProgressListener() != null) {
            if (!plugin.getQuestManager().getQuestProgressListener().isValidGamemode(player)) {
                return null;
            }
        }

        var data = getData(player);
        String id = FORCE_ACTIVE_QUEST.get();
        if (id == null) {
            id = data.getTrackedQuestId();
        }
        if (id == null || id.isBlank()) return null;

        return plugin.getQuestManager().getById(id);
    }

    protected boolean isActiveQuest(Player player, QuestDefinition quest) {
        if (player == null || quest == null) return false;

        if (plugin.getQuestManager() != null && plugin.getQuestManager().getQuestProgressListener() != null) {
            if (!plugin.getQuestManager().getQuestProgressListener().isValidGamemode(player)) {
                return false;
            }
        }

        String activeId = getData(player).getTrackedQuestId();
        return activeId != null && quest.getId().equalsIgnoreCase(activeId);
    }

    protected int getCurrentStageNumber(Player player) {
        if (player == null) return 1;
        int stage = 1;
        if (getData(player) != null && getData(player).getTrackedQuestId() != null && getData(player).getQuestData(getData(player).getTrackedQuestId()) != null) {
            stage = getData(player).getQuestData(getData(player).getTrackedQuestId()).getCurrentStage();
        }
        return Math.max(1, stage);
    }

    protected QuestStage getCurrentStage(Player player, QuestDefinition quest) {
        if (player == null || quest == null) return null;
        if (!quest.hasStages()) return null;

        int stageNum = getCurrentStageNumber(player);
        return quest.getStage(stageNum);
    }

    protected QuestObjective getCurrentObjective(Player player, QuestDefinition quest) {
        if (player == null || quest == null) return null;

        return plugin.getQuestManager().resolveObjective(player, quest);
    }

    protected QuestObjectiveType getCurrentObjectiveType(Player player, QuestDefinition quest) {
        QuestObjective o = getCurrentObjective(player, quest);
        return o != null ? o.getType() : null;
    }

    protected boolean isCurrentObjectiveType(Player player, QuestDefinition quest, QuestObjectiveType type) {
        QuestObjectiveType current = getCurrentObjectiveType(player, quest);
        return current == type;
    }

    protected boolean isCurrentStageObjective(Player player, QuestDefinition quest) {
        if (player == null || quest == null) return false;

        QuestObjective objective = plugin.getQuestManager().resolveObjective(player, quest);
        return objective != null;
    }

    protected boolean isCurrentStageRequiredMaterials(Player player, QuestDefinition quest) {
        if (player == null || quest == null) return false;

        if (!quest.hasStages()) return false;

        QuestStage stage = getCurrentStage(player, quest);
        return stage != null && stage.isRequiredMaterials();
    }

    protected boolean progress(Player player, QuestDefinition quest, int amount) {
        if (player == null || quest == null) return false;
        if (amount <= 0) return false;

        plugin.getQuestManager().incrementProgress(player, quest, amount);
        return true;
    }

    protected boolean progress(Player player, QuestDefinition quest, double add) {
        if (add <= 0) return false;
        return progress(player, quest, (int) Math.ceil(add));
    }

    protected void complete(Player player, QuestDefinition quest) {
        if (player == null || quest == null) return;
        plugin.getQuestManager().completeQuest(player, quest);
    }

    protected void sendActionBarForPlayer(Player player) {
        if (player == null) return;
        plugin.getQuestManager().sendActionBarForPlayer(player);
    }
}
