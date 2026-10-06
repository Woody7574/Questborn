package ua.woody.questborn.model.npc.mood;

import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.NpcConfig;
import ua.woody.questborn.model.PlayerQuestProgress;

public class QuestCompletedCondition implements MoodCondition {
    private final String questId;

    public QuestCompletedCondition(String questId) {
        this.questId = questId;
    }

    public String getQuestId() {
        return questId;
    }

    @Override
    public boolean check(Player player, NpcConfig npc) {
        if (player == null || questId == null) return false;
        QuestbornPlugin plugin = QuestbornPlugin.getInstance();
        if (plugin == null || plugin.getPlayerDataStore() == null) return false;

        PlayerQuestProgress progress = plugin.getPlayerDataStore().get(player.getUniqueId());
        if (progress == null) return false;

        boolean isCompletedForever = progress.getCompletedQuests().contains(questId.toLowerCase(java.util.Locale.ROOT));
        boolean isOnCooldown = progress.getQuestCooldownUntil(questId) > System.currentTimeMillis();

        return isCompletedForever || isOnCooldown;
    }
}
