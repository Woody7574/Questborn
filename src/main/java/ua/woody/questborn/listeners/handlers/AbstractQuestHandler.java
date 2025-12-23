package ua.woody.questborn.listeners.handlers;

import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;

import java.lang.reflect.Method;

public abstract class AbstractQuestHandler {

    protected final QuestbornPlugin plugin;

    public AbstractQuestHandler(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    public QuestbornPlugin getPlugin() {
        return this.plugin;
    }

    /* ======================== ACTIVE QUEST ======================== */
    protected QuestDefinition getActiveQuest(Player player) {
        var data = plugin.getPlayerDataStore().get(player.getUniqueId());
        String id = data.getActiveQuestId();
        if (id == null) return null;
        return plugin.getQuestManager().getById(id);
    }

    /* ======================== PROGRESS INT ======================== */
    protected boolean progress(Player player, QuestDefinition quest, int amount) {
        if (quest == null) return false;
        plugin.getQuestManager().incrementProgress(player, quest, amount);
        return true;
    }

    /* ======================== PROGRESS DOUBLE ======================== */
    protected boolean progress(Player player, QuestDefinition quest, double add) {
        return progress(player, quest, (int) Math.ceil(add));
    }

    /* ======================== COMPLETE QUEST ======================== */
    protected void complete(Player player, QuestDefinition quest) {
        if (quest == null) return;
        plugin.getQuestManager().completeQuest(player, quest); // <-- FIXED
    }

    /* ======================== ACTIONBAR OUTPUT ======================== */
    protected void sendActionBarForPlayer(Player player) {
        try {
            var questManager = plugin.getQuestManager();
            if (questManager == null) return;

            var field = questManager.getClass().getDeclaredField("actionBarManager");
            field.setAccessible(true);
            var actionBarManager = field.get(questManager);

            Method method = actionBarManager.getClass().getMethod("sendForPlayer", Player.class);
            method.invoke(actionBarManager, player);

        } catch (Exception ignored) {}
    }
}
