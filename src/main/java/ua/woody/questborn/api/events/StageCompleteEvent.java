package ua.woody.questborn.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ua.woody.questborn.model.QuestDefinition;

public class StageCompleteEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final QuestDefinition quest;
    private final int stageId;

    public StageCompleteEvent(Player player, QuestDefinition quest, int stageId) {
        this.player = player;
        this.quest = quest;
        this.stageId = stageId;
    }

    public Player getPlayer() {
        return player;
    }

    public QuestDefinition getQuest() {
        return quest;
    }

    public int getStageId() {
        return stageId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
