package ua.woody.questborn.listeners.handlers;

import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.config.ActionBarMode;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;

public class LevelAndExperienceHandler extends AbstractQuestHandler {

    public LevelAndExperienceHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    // ==================== EXPERIENCE_ORB_PICKUP ====================
    @EventHandler
    public void onExperiencePickup(PlayerExpChangeEvent e) {
        Player p = e.getPlayer();
        int expGained = e.getAmount();

        if (expGained <= 0) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() == QuestObjectiveType.EXPERIENCE_ORB_PICKUP) {
            progress(p, q, expGained);
        }
    }

    // ==================== LEVEL_UP_REACH та LEVEL_UP_GAIN ====================
    public void onLevelUp(PlayerLevelChangeEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        var data = plugin.getPlayerDataStore().get(p.getUniqueId());

        // Перевірка, що активний квест справді цей
        if (!q.getId().equals(data.getActiveQuestId())) return;

        if (o.getType() == QuestObjectiveType.LEVEL_UP_REACH) {
            int currentLevel = e.getNewLevel();
            int oldLevel = e.getOldLevel();
            int targetLevel = o.getAmount();

            // Рівень не збільшився - нічого не робимо
            if (currentLevel <= oldLevel) {
                return;
            }

            int currentProgress = data.getActiveQuestProgress();
            int newProgress = Math.min(currentLevel, targetLevel);

            // Оновлюємо тільки якщо прогрес дійсно змінився
            if (newProgress > currentProgress) {
                updateQuestProgress(p, q, newProgress);

                // Якщо досягли цільового рівня - завершуємо квест
                if (newProgress >= targetLevel) {
                    plugin.getQuestManager().completeQuest(p, q);
                }
            }
        } else if (o.getType() == QuestObjectiveType.LEVEL_UP_GAIN) {
            int levelsGained = e.getNewLevel() - e.getOldLevel();
            if (levelsGained > 0) {
                progress(p, q, levelsGained);
            }
        }
    }

    private void updateQuestProgress(Player p, QuestDefinition q, int newProgress) {
        var data = plugin.getPlayerDataStore().get(p.getUniqueId());

        // Додаткова перевірка: оновлюємо тільки активний квест
        if (!q.getId().equalsIgnoreCase(data.getActiveQuestId())) return;

        // Оновлюємо прогрес
        data.setActiveQuestProgress(newProgress);
        plugin.getPlayerDataStore().save();

        // Оновлюємо ActionBar якщо потрібно
        if (plugin.getQuestManager().getActionBarMode() == ActionBarMode.ON_PROGRESS_CHANGE) {
            plugin.getQuestManager().sendActionBarForPlayer(p);
        }
    }
}