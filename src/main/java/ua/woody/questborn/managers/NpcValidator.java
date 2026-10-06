package ua.woody.questborn.managers;

import org.bukkit.Location;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.NpcConfig;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestTypeConfig;

import java.util.ArrayList;
import java.util.List;

public class NpcValidator {
    private final QuestbornPlugin plugin;

    public NpcValidator(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    public static class ValidationResult {
        public final boolean hasErrors;
        public final boolean hasWarnings;
        public final List<String> messages;

        public ValidationResult(boolean hasErrors, boolean hasWarnings, List<String> messages) {
            this.hasErrors = hasErrors;
            this.hasWarnings = hasWarnings;
            this.messages = messages;
        }
    }

    public ValidationResult validate(String npcId) {
        List<String> messages = new ArrayList<>();
        boolean hasErrors = false;
        boolean hasWarnings = false;

        NpcConfig npcConfig = plugin.getNpcManager().getConfigByNpcId(npcId);
        if (npcConfig == null) {
            messages.add("&c❌ Critical: Config not found for this NPC.");
            return new ValidationResult(true, false, messages);
        }

        if (plugin.getNpcIntegrationManager().getProvider() != null) {
            Location loc = plugin.getNpcIntegrationManager().getProvider().getStoredLocation(npcId);
            if (loc == null) {
                messages.add("&e⚠️ Warning: Physical NPC not found in the world! Integration ("
                        + plugin.getNpcIntegrationManager().getProvider().getClass().getSimpleName().replace("Provider", "")
                        + ") doesn't recognize this ID.");
                hasWarnings = true;
            } else {
                messages.add("&a✔️ Physical NPC is valid and located at X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ());
            }
        } else {
            messages.add("&7ℹ️ NPC Integration is disabled or not found.");
        }

        List<String> startsQuests = npcConfig.getStartsQuests();
        List<String> finishesQuests = npcConfig.getFinishesQuests();

        for (String qId : startsQuests) {
            QuestDefinition q = plugin.getQuestManager().getQuest(qId);
            if (q == null) {
                messages.add("&c❌ Error: NPC starts quest '" + qId + "', but this quest does not exist.");
                hasErrors = true;
            } else {
                messages.add("&a✔️ Linked start quest: " + qId);
            }
        }
        for (String qId : finishesQuests) {
            QuestDefinition q = plugin.getQuestManager().getQuest(qId);
            if (q == null) {
                messages.add("&c❌ Error: NPC finishes quest '" + qId + "', but this quest does not exist.");
                hasErrors = true;
            } else {
                messages.add("&a✔️ Linked finish quest: " + qId);
            }
        }

        List<String> startsTypes = npcConfig.getStartsTypes();
        List<String> finishesTypes = npcConfig.getFinishesTypes();

        for (String typeId : startsTypes) {
            QuestTypeConfig t = plugin.getQuestManager().getQuestTypeManager().getType(typeId);
            if (t == null) {
                messages.add("&c❌ Error: NPC starts type '" + typeId + "', but this type does not exist.");
                hasErrors = true;
            } else {
                messages.add("&a✔️ Linked start type: " + typeId);
            }
        }
        for (String typeId : finishesTypes) {
            QuestTypeConfig t = plugin.getQuestManager().getQuestTypeManager().getType(typeId);
            if (t == null) {
                messages.add("&c❌ Error: NPC finishes type '" + typeId + "', but this type does not exist.");
                hasErrors = true;
            } else {
                messages.add("&a✔️ Linked finish type: " + typeId);
            }
        }

        String dialogueId = npcConfig.getDialogueId();
        if (dialogueId != null && !dialogueId.equalsIgnoreCase("none")) {
            if (!plugin.getDialogueManager().getDialogues().containsKey(dialogueId)) {
                messages.add("&c❌ Error: Linked dialogue '" + dialogueId + "' does not exist.");
                hasErrors = true;
            } else {
                messages.add("&a✔️ Linked dialogue is valid.");
            }
        } else {
            messages.add("&7ℹ️ No dialogue linked (default fallback will be used if available).");
        }

        if (startsQuests.isEmpty() && finishesQuests.isEmpty() && startsTypes.isEmpty() && finishesTypes.isEmpty() && (dialogueId == null || dialogueId.equalsIgnoreCase("none"))) {
            messages.add("&e⚠️ Warning: This NPC has no quests, types, or dialogues linked. It won't do anything.");
            hasWarnings = true;
        }

        return new ValidationResult(hasErrors, hasWarnings, messages);
    }
}
