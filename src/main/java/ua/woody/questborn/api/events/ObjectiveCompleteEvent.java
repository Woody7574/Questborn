package ua.woody.questborn.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;

public class ObjectiveCompleteEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final QuestDefinition quest;
    private final QuestObjective objective;

    public ObjectiveCompleteEvent(Player player, QuestDefinition quest, QuestObjective objective) {
        this.player = player;
        this.quest = quest;
        this.objective = objective;
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

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
