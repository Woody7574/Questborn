package ua.woody.questborn.api.managers;

import org.bukkit.entity.Player;
import ua.woody.questborn.model.Dialogue;

import java.util.Map;

public interface DialogueManagerAPI {
    boolean startDialogue(Player player, String dialogueId, String npcId);
    boolean runNode(Player player, Dialogue dialogue, String nodeId, String npcId);
    Map<String, Dialogue> getDialogues();
}
