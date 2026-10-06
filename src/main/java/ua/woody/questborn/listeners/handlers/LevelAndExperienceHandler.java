package ua.woody.questborn.listeners.handlers;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

public class LevelAndExperienceHandler extends AbstractQuestHandler {
    public LevelAndExperienceHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onExperiencePickup(PlayerExpChangeEvent e) {
        Player p = e.getPlayer();
        int expGained = e.getAmount();
        if (expGained <= 0) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null) return;

        if (o.getType() == QuestObjectiveType.EXPERIENCE_ORB_PICKUP) {
            progress(p, q, expGained);
        }
    }

    public void onLevelUp(PlayerLevelChangeEvent e) {
        Player p = e.getPlayer();

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null) return;

        var data = plugin.getPlayerDataStore().get(p.getUniqueId());
        if (!q.getId().equals(data.getTrackedQuestId())) return;

        if (o.getType() == QuestObjectiveType.LEVEL_UP_REACH) {
            int currentLevel = e.getNewLevel();
            int oldLevel = e.getOldLevel();
            int targetLevel = o.getAmount();

            if (currentLevel <= oldLevel) return;

            int currentProgress = plugin.getQuestManager().getProgressValue(data, q);
            int newProgress = Math.min(currentLevel, targetLevel);

            if (newProgress > currentProgress) {
                plugin.getQuestManager().setProgressValue(data, q, newProgress);
                data.getQuestData(data.getTrackedQuestId()).setProgress(newProgress);

                if (newProgress >= targetLevel) {
                    plugin.getQuestManager().completeQuest(p, q);
                }
            }
        } else if (o.getType() == QuestObjectiveType.LEVEL_UP_GAIN) {
            int gained = e.getNewLevel() - e.getOldLevel();
            if (gained > 0) progress(p, q, gained);
        }
    }
}
