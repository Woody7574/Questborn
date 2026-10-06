package ua.woody.questborn.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;

public class ObjectiveProgressEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final QuestDefinition quest;
    private final QuestObjective objective;
    private final int newProgress;
    private final int maxProgress;

    public ObjectiveProgressEvent(Player player, QuestDefinition quest, QuestObjective objective, int newProgress, int maxProgress) {
        this.player = player;
        this.quest = quest;
        this.objective = objective;
        this.newProgress = newProgress;
        this.maxProgress = maxProgress;
    }

    public Player getPlayer() {
        return player;
    }

    public QuestDefinition getQuest() {
        return quest;
    }

    public QuestObjective getObjective() {
        return objective;
    }

    public int getNewProgress() {
        return newProgress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
