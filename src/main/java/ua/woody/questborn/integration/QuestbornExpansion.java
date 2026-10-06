package ua.woody.questborn.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.managers.QuestManager;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.storage.PlayerDataStore;
import ua.woody.questborn.util.QuestDisplayBuilder;
import ua.woody.questborn.util.TimeFormatter;

public class QuestbornExpansion extends PlaceholderExpansion {
    private final QuestbornPlugin plugin;

    public QuestbornExpansion(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "questborn";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || !player.isOnline()) {
            return "";
        }

        PlayerDataStore dataStore = plugin.getPlayerDataStore();
        if (dataStore == null)
            return "";

        PlayerQuestProgress data = dataStore.get(player.getUniqueId());
        if (data == null)
            return "";

        QuestManager questManager = plugin.getQuestManager();
        if (questManager == null)
            return "";

        if (params.equalsIgnoreCase("quests_completed")) {
            return String.valueOf(data.getCompletedQuests().size());
        }

        if (params.startsWith("status_")) {
            String questId = params.substring("status_".length());
            QuestDefinition def = questManager.getQuest(questId);
            if (def == null)
                return "NOT_FOUND";

            Player p = player.getPlayer();
            if (p == null)
                return "NOT_FOUND";

            long cd = data.getQuestCooldownUntil(questId);
            if (cd > System.currentTimeMillis())
                return "COOLDOWN";
            if (questId.equalsIgnoreCase(data.getTrackedQuestId()))
                return "ACTIVE";
            if (data.isQuestCompleted(questId))
                return "COMPLETED";

            if (questManager.isQuestAvailable(p, def, data))
                return "AVAILABLE";
            return "UNAVAILABLE";
        }

        if (params.startsWith("cooldown_")) {
            String questId = params.substring("cooldown_".length());
            long cd = data.getQuestCooldownUntil(questId);
            long diff = (cd - System.currentTimeMillis()) / 1000L;
            if (diff > 0) {
                return TimeFormatter.format(diff);
            }
            return "";
        }

        String activeQuestId = data.getTrackedQuestId();
        if (activeQuestId == null || activeQuestId.isEmpty()) {
            return "";
        }

        QuestDefinition activeDef = questManager.getQuest(activeQuestId);
        if (activeDef == null) {
            return "";
        }

        if (params.equalsIgnoreCase("active_id")) {
            return activeQuestId;
        }

        if (params.equalsIgnoreCase("active_name")) {
            return activeDef.getDisplayName();
        }

        if (params.equalsIgnoreCase("active_description")) {
            return String.join("\n", activeDef.getDescription());
        }

        int currentStageIndex = (data.getTrackedQuestId() != null && data.getQuestData(data.getTrackedQuestId()) != null ? data.getQuestData(data.getTrackedQuestId()).getCurrentStage() : 1);

        if (params.equalsIgnoreCase("active_stage_current")) {
            return String.valueOf(currentStageIndex);
        }

        if (params.equalsIgnoreCase("active_stage_total")) {
            return String.valueOf(activeDef.getStageCount());
        }

        if (currentStageIndex > activeDef.getStageCount()) {
            return "";
        }

        Player p = player.getPlayer();
        if (p == null)
            return "";

        QuestObjective objective = questManager.resolveObjective(p, activeDef);

        if (params.equalsIgnoreCase("active_progress_target")) {
            return String.valueOf(questManager.getTargetValue(data, activeDef));
        }

        if (params.equalsIgnoreCase("active_progress_current")) {
            return String.valueOf(questManager.getProgressValue(data, activeDef));
        }

        if (params.equalsIgnoreCase("active_progressbar")) {
            int currentProg = questManager.getProgressValue(data, activeDef);
            int targetProg = questManager.getTargetValue(data, activeDef);
            return plugin.getGuiConfig().getProgressBar(currentProg, targetProg);
        }

        if (params.startsWith("active_objective")) {
            if (objective != null) {
                java.util.List<String> objectiveLines = QuestDisplayBuilder.build(
                        objective,
                        plugin.getLanguage());

                if (params.equalsIgnoreCase("active_objective")) {
                    return objectiveLines.stream().skip(1).findFirst().orElse("");
                }

                String suffix = params.substring("active_objective".length());
                if (suffix.startsWith("-") || suffix.startsWith("_")) {
                    try {
                        int lineNum = Integer.parseInt(suffix.substring(1));
                        if (lineNum >= 0 && lineNum < objectiveLines.size()) {
                            return objectiveLines.get(lineNum);
                        } else {
                            return "";
                        }
                    } catch (NumberFormatException e) {
                        return "";
                    }
                }
                return "";
            } else {
                if (params.equalsIgnoreCase("active_objective") || params.equals("active_objective-1")
                        || params.equals("active_objective_1")) {
                    return plugin.getLanguage().tr("gui.quest_details.info.required-items");
                }
                return "";
            }
        }

        return null;
    }
}
