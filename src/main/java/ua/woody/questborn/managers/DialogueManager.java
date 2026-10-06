package ua.woody.questborn.managers;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.lang.ColorFormatter;
import ua.woody.questborn.model.Dialogue;
import ua.woody.questborn.model.DialogueNode;
import ua.woody.questborn.model.DialogueOption;
import ua.woody.questborn.model.PlayerQuestProgress;

import java.io.File;
import java.util.*;

public class DialogueManager {
    private final QuestbornPlugin plugin;
    private final Map<String, Dialogue> dialogues = new HashMap<>();

    public DialogueManager(QuestbornPlugin plugin) {
        this.plugin = plugin;
        loadDialogues();
    }

    public Map<String, Dialogue> getDialogues() { return dialogues; }

    public void reload() {
        loadDialogues();
    }

    private void loadDialogues() {
        dialogues.clear();
        File dir = new File(plugin.getDataFolder(), "npc");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "dialogues.yml");
        if (!file.exists()) {
            plugin.saveResource("npc/dialogues.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection dialoguesSection = config.getConfigurationSection("dialogues");
        if (dialoguesSection == null) return;

        for (String id : dialoguesSection.getKeys(false)) {
            ConfigurationSection sec = dialoguesSection.getConfigurationSection(id);
            if (sec == null) continue;

            Map<String, DialogueNode> nodes = new HashMap<>();
            ConfigurationSection nodesSec = sec.getConfigurationSection("nodes");
            if (nodesSec != null) {
                for (String nodeId : nodesSec.getKeys(false)) {
                    ConfigurationSection nodeSec = nodesSec.getConfigurationSection(nodeId);
                    if (nodeSec == null) continue;

                    String sound = nodeSec.getString("sound");

                    List<String> text;
                    if (nodeSec.isList("text")) {
                        text = nodeSec.getStringList("text");
                    } else {
                        text = Collections.singletonList(nodeSec.getString("text"));
                    }

                    List<DialogueOption> options = new ArrayList<>();
                    if (nodeSec.contains("options")) {
                        for (Map<?, ?> optMap : nodeSec.getMapList("options")) {
                            String optText = (String) optMap.get("text");
                            String optMaterial = (String) optMap.get("material");
                            String optAction = (String) optMap.get("action");
                            String optGoto = (String) optMap.get("goto");
                            List<String> optConditions = new ArrayList<>();
                            if (optMap.containsKey("conditions")) {
                                Object rawConds = optMap.get("conditions");
                                if (rawConds instanceof List) optConditions.addAll((List<String>) rawConds);
                            } else if (optMap.containsKey("condition")) {
                                optConditions.add((String) optMap.get("condition"));
                            }
                            int optSlot = -1;
                            if (optMap.containsKey("slot")) {
                                Object slotObj = optMap.get("slot");
                                if (slotObj instanceof Number) {
                                    optSlot = ((Number) slotObj).intValue();
                                } else if (slotObj instanceof String) {
                                    try { optSlot = Integer.parseInt(slotObj.toString().trim()); } catch (NumberFormatException ignored) {}
                                }
                            }
                            String optSound = (String) optMap.get("sound");
                            List<String> optLore = optMap.containsKey("lore") ? (List<String>) optMap.get("lore") : null;

                            String finalAction = optAction != null ? optAction : (optGoto != null ? "goto:" + optGoto : "close");

                            options.add(new DialogueOption(optText, optLore, optMaterial, finalAction, optConditions, optSlot, optSound));
                        }
                    }

                    nodes.put(nodeId, new DialogueNode(nodeId, text, options, sound));
                }
            }

            int cpt = sec.getInt("cpt", 1);
            long updatePeriod = sec.getLong("update_period", 1L);

            Dialogue dialogue = new Dialogue(id, nodes, cpt, updatePeriod);
            dialogues.put(id, dialogue);
        }
        plugin.getLogger().info("Loaded " + dialogues.size() + " dialogues.");
    }

    public boolean startDialogue(Player player, String dialogueId, String npcId) {
        Dialogue dialogue = dialogues.get(dialogueId);
        if (dialogue == null) return false;

        ua.woody.questborn.api.events.DialogueStartEvent event = new ua.woody.questborn.api.events.DialogueStartEvent(player, dialogueId, npcId);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        String initialNode = "start";

        ua.woody.questborn.model.NpcConfig npc = plugin.getNpcManager().getConfigByNpcId(npcId);
        if (npc != null && plugin.getMoodManager() != null) {
            String mood = plugin.getMoodManager().calculateMood(player, npc);
            if (dialogue.getNode("start_" + mood) != null) {
                initialNode = "start_" + mood;
            }
        }

        if (ua.woody.questborn.gui.DialogueGui.hasRewardFromNpc(plugin, player, npcId) && dialogue.getNode("reward_ready") != null) {
            initialNode = "reward_ready";
        }

        return runNode(player, dialogue, initialNode, npcId);
    }

    public boolean runNode(Player player, Dialogue dialogue, String nodeId, String npcId) {
        if (dialogue == null || dialogue.getNode(nodeId) == null) return false;

        plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> {
            new ua.woody.questborn.gui.DialogueGui(plugin, player, dialogue.getId(), nodeId, npcId).open();
        });

        return true;
    }

    public void completeDialogue(Player player, String dialogueId, String npcId) {
        ua.woody.questborn.api.events.DialogueCompleteEvent event = new ua.woody.questborn.api.events.DialogueCompleteEvent(player, dialogueId, npcId);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
    }

    private boolean checkCondition(Player player, PlayerQuestProgress progress, String condition) {
        boolean invert = condition.startsWith("!");
        if (invert) condition = condition.substring(1).trim();

        boolean result = false;
        if (condition.startsWith("has_active_quest(")) {
            String qId = condition.substring(17, condition.length() - 1);
            result = progress.hasActiveQuest(qId);
        } else if (condition.startsWith("completed_quest(")) {
            String qId = condition.substring(16, condition.length() - 1);
            result = progress.getCompletedQuests().contains(qId);
        }

        return invert ? !result : result;
    }
}
