package ua.woody.questborn.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class DialogueStartEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private final Player player;
    private final String dialogueId;
    private final String npcId;

    public DialogueStartEvent(Player player, String dialogueId, String npcId) {
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
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
