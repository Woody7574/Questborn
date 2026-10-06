package ua.woody.questborn.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class DialogueCompleteEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String dialogueId;
    private final String npcId;

    public DialogueCompleteEvent(Player player, String dialogueId, String npcId) {
        this.player = player;
        this.dialogueId = dialogueId;
        this.npcId = npcId;
    }

    public Player getPlayer() {
        return player;
    }

    public String getDialogueId() {
        return dialogueId;
    }

    public String getNpcId() {
        return npcId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
