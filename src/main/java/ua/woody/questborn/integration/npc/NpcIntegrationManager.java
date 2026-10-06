package ua.woody.questborn.integration.npc;

import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.MainMenuGui;
import ua.woody.questborn.gui.QuestDetailsGui;
import ua.woody.questborn.gui.QuestListGui;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestTypeConfig;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.Material;

import java.util.List;
import java.util.Locale;

public class NpcIntegrationManager {
    private final QuestbornPlugin plugin;
    private NpcProvider activeProvider;

    public NpcIntegrationManager(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    public void setProvider(NpcProvider provider) {
        if (this.activeProvider != null) {
            this.activeProvider.shutdown();
        }
        this.activeProvider = provider;
        if (this.activeProvider != null) {
            this.activeProvider.registerListeners();
        }
    }

    public NpcProvider getProvider() {
        return activeProvider;
    }

    public java.util.Collection<String> getAllNpcIds() {
        if (activeProvider != null) {
            return activeProvider.getAllNpcIds();
        }
        return java.util.Collections.emptyList();
    }

    public String getSkinTextureBase64(String npcId) {
        if (activeProvider != null) {
            return activeProvider.getSkinTextureBase64(npcId);
        }
        return null;
    }

    public void handleNpcClick(Player player, String npcId, Entity npcEntity) {
        handleNpcClick(player, npcId, npcEntity, false, false);
    }

    public void handleNpcClick(Player player, String npcId, Entity npcEntity, boolean skipDialogue) {
        handleNpcClick(player, npcId, npcEntity, skipDialogue, false);
    }

    private void handleNpcObjectives(Player player, String npcId) {
        ua.woody.questborn.storage.PlayerDataStore dataStore = plugin.getPlayerDataStore();
        if (dataStore == null) return;
        ua.woody.questborn.model.PlayerQuestProgress data = dataStore.get(player.getUniqueId());
        if (data == null) return;

        for (String questId : data.getActiveQuests().keySet()) {
            ua.woody.questborn.model.QuestDefinition q = plugin.getQuestManager().getQuest(questId);
            if (q != null) {
                ua.woody.questborn.model.PlayerQuestProgress.ActiveQuestData qd = data.getQuestData(questId);
                if (qd != null) {
                    ua.woody.questborn.model.QuestObjective objective = null;
                    if (q.hasStages()) {
                        ua.woody.questborn.model.QuestStage stage = q.getStage(qd.getCurrentStage());
                        if (stage != null && stage.isObjective()) objective = stage.getObjective();
                    } else {
                        objective = q.getObjective();
                    }

                    if (objective != null && objective.getNpc() != null && npcId.equalsIgnoreCase(objective.getNpc())) {
                        if (objective.getType() == ua.woody.questborn.model.QuestObjectiveType.NPC_INTERACT) {
                            plugin.getQuestManager().getProgressProcessor().incrementProgress(player, q, 1);
                        }
                    }
                }
            }
        }
    }

    public void handleNpcClick(Player player, String npcId, Entity npcEntity, boolean skipDialogue, boolean ignoreActiveQuest) {
        handleNpcClick(player, npcId, npcEntity, skipDialogue, ignoreActiveQuest, 0);
    }

    public void handleNpcClick(Player player, String npcId, Entity npcEntity, boolean skipDialogue, boolean ignoreActiveQuest, int returnPage) {
        handleNpcClick(player, npcId, npcEntity, skipDialogue, ignoreActiveQuest, returnPage, null, null);
    }

    public void handleNpcClick(Player player, String npcId, Entity npcEntity, boolean skipDialogue, boolean ignoreActiveQuest, int returnPage, String dialogueId, String nodeId) {
        if (activeProvider == null) return;

        ua.woody.questborn.managers.NpcManager.PendingNpcAction pendingAction = plugin.getNpcManager().getPendingAction(player.getUniqueId());
        if (pendingAction != null) {
            if (pendingAction.isLink) {
                plugin.getNpcManager().addLink(npcId, pendingAction.isStart, pendingAction.isQuest, pendingAction.targetId);
                plugin.getLanguage().sendMessage(player, "commands.npc.bind-success", java.util.Map.of("target", pendingAction.targetId, "npc", npcId));
            } else {
                plugin.getNpcManager().removeLink(npcId, pendingAction.isStart, pendingAction.isQuest, pendingAction.targetId);
                plugin.getLanguage().sendMessage(player, "commands.npc.unbind-success", java.util.Map.of("target", pendingAction.targetId, "npc", npcId));
            }
            plugin.getNpcManager().setPendingAction(player.getUniqueId(), null);
            return;
        }

        handleNpcObjectives(player, npcId);

        String mode = plugin.getConfig().getString("integration.npc.mode", plugin.getConfig().getString("integration.citizens.mode", "MIXED")).toUpperCase(Locale.ROOT);
        if ("MENU_ONLY".equals(mode)) return;

        ua.woody.questborn.model.NpcConfig npcConfig = plugin.getNpcManager().getConfigByNpcId(npcId);

        var data = plugin.getPlayerDataStore().get(player.getUniqueId());

        List<QuestTypeConfig> matchedTypes = new java.util.ArrayList<>();
        List<QuestDefinition> matchedQuests = new java.util.ArrayList<>();

        if (npcConfig != null) {
            for (String tId : npcConfig.getStartsTypes()) {
                QuestTypeConfig t = plugin.getQuestManager().getQuestTypeManager().getType(tId);
                if (t != null && !matchedTypes.contains(t)) matchedTypes.add(t);
            }
            for (String tId : npcConfig.getFinishesTypes()) {
                QuestTypeConfig t = plugin.getQuestManager().getQuestTypeManager().getType(tId);
                if (t != null) {
                    for (QuestDefinition q : plugin.getQuestManager().getByType(t)) {
                        if (!matchedQuests.contains(q) && data != null && (data.hasPendingReward(q.getId()) || data.hasActiveQuest(q.getId()))) {
                            matchedQuests.add(q);
                        }
                    }
                }
            }
            for (String qId : npcConfig.getStartsQuests()) {
                QuestDefinition q = plugin.getQuestManager().getQuest(qId);
                if (q != null && !matchedQuests.contains(q)) matchedQuests.add(q);
            }
            for (String qId : npcConfig.getFinishesQuests()) {
                QuestDefinition q = plugin.getQuestManager().getQuest(qId);
                if (q != null && !matchedQuests.contains(q)) {
                    if (data != null && (data.hasPendingReward(q.getId()) || data.hasActiveQuest(q.getId()))) {
                        matchedQuests.add(q);
                    }
                }
            }
        }

        if (!matchedTypes.isEmpty()) {
            matchedQuests.removeIf(q -> {
                for (QuestTypeConfig t : matchedTypes) {
                    if (t.getId().equalsIgnoreCase(q.getTypeId())) return true;
                }
                return false;
            });
        }

        if (npcConfig != null && npcConfig.getVoice() != null && !npcConfig.getVoice().equalsIgnoreCase("none")) {
            org.bukkit.Sound voiceSound = ua.woody.questborn.gui.DialogueGui.getSoundFromVoice(npcConfig.getVoice());
            if (voiceSound != null) {
                float pitch = ua.woody.questborn.gui.DialogueGui.getPitchFromVoice(npcConfig.getVoice());
                player.playSound(player.getLocation(), voiceSound, 0.6f, pitch);
            }
        }

        String configDialogueId = (npcConfig != null && npcConfig.getDialogueId() != null) ? npcConfig.getDialogueId() : null;

        if (!skipDialogue) {
            if (configDialogueId != null) {
                if (plugin.getDialogueManager().startDialogue(player, configDialogueId, npcId)) {
                    return;
                } else {
                    plugin.getLanguage().sendMessage(player, "commands.npc.dialogue-not-found", java.util.Map.of("dialogue", configDialogueId));
                }
            } else {
                if (plugin.getDialogueManager().startDialogue(player, npcId, npcId)) {
                    return;
                }

                if (plugin.getDialogueManager().startDialogue(player, "default", npcId)) {
                    return;
                }
            }
        }

        if (matchedTypes.isEmpty() && matchedQuests.isEmpty()) return;

        String npcName = activeProvider.getName(npcId);
        String guiTitle = npcConfig != null ? npcConfig.getGuiTitle() : null;
        if (guiTitle != null) {
            String strippedNpcName = ua.woody.questborn.lang.ColorFormatter.stripColors(npcName);
            guiTitle = guiTitle.replace("{npc}", strippedNpcName);
        }
        org.bukkit.entity.EntityType npcType = activeProvider.getType(npcId);
        ItemStack npcIcon = createNpcIcon(npcType, npcName, guiTitle != null ? guiTitle : npcName);

        if (matchedTypes.size() == 1 && matchedQuests.isEmpty()) {
            new ua.woody.questborn.gui.QuestListGui(plugin, player, matchedTypes.get(0), returnPage, true, null, npcIcon, npcId, dialogueId, nodeId).open();
        } else if (!matchedTypes.isEmpty()) {
            new ua.woody.questborn.gui.MainMenuGui(plugin, player, matchedTypes, npcIcon, npcId, guiTitle, dialogueId, nodeId).open();
        } else if (matchedQuests.size() == 1) {
            ua.woody.questborn.model.QuestDefinition q = matchedQuests.get(0);
            boolean isActive = false;
            ua.woody.questborn.model.PlayerQuestProgress questData = plugin.getPlayerDataStore().get(player.getUniqueId());
            if (questData != null && (questData.hasActiveQuest(q.getId()) || questData.hasPendingReward(q.getId())) && !ignoreActiveQuest) {
                isActive = true;
            }
            if (isActive) {
                new ua.woody.questborn.gui.QuestDetailsGui(plugin, player, q, true, npcIcon, npcId, dialogueId, nodeId).open();
            } else {
                new ua.woody.questborn.gui.QuestListGui(plugin, player, null, returnPage, true, matchedQuests, npcIcon, npcId, dialogueId, nodeId).open();
            }
        } else {
            new ua.woody.questborn.gui.QuestListGui(plugin, player, null, returnPage, true, matchedQuests, npcIcon, npcId, dialogueId, nodeId).open();
        }
    }

    public ItemStack createNpcIcon(org.bukkit.entity.EntityType type, String npcName, String customDisplayName) {
        ItemStack item;
        if (type == org.bukkit.entity.EntityType.PLAYER) {
            item = new ItemStack(Material.PLAYER_HEAD);
        } else {
            switch (type) {
                case ZOMBIE: item = new ItemStack(Material.ZOMBIE_HEAD); break;
                case SKELETON: item = new ItemStack(Material.SKELETON_SKULL); break;
                case CREEPER: item = new ItemStack(Material.CREEPER_HEAD); break;
                case WITHER_SKELETON: item = new ItemStack(Material.WITHER_SKELETON_SKULL); break;
                default:
                    try {
                        item = new ItemStack(Material.valueOf(type.name() + "_SPAWN_EGG"));
                    } catch (Exception e) {
                        item = new ItemStack(Material.ARMOR_STAND);
                    }
                    break;
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = (customDisplayName != null && !customDisplayName.isEmpty()) ? customDisplayName : npcName;
            meta.setDisplayName(ua.woody.questborn.lang.ColorFormatter.applyColors(name));
            ua.woody.questborn.gui.GuiUtils.applyAllItemFlags(meta);
            item.setItemMeta(meta);
        }
        return item;
    }
}
