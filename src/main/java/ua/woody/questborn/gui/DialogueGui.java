package ua.woody.questborn.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.config.GuiLayout;
import ua.woody.questborn.lang.ColorFormatter;
import ua.woody.questborn.model.Dialogue;
import ua.woody.questborn.model.DialogueNode;
import ua.woody.questborn.model.DialogueOption;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DialogueGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String dialogueId;
    private final String nodeId;
    private final String npcId;
    private final Gui gui;
    private final GuiLayout layout;

    private com.tcoded.folialib.wrapper.task.WrappedTask animationTask;
    private boolean isFinished = false;
    private boolean instant = false;

    public DialogueGui(QuestbornPlugin plugin, Player player, String dialogueId, String nodeId, String npcId) {
        this(plugin, player, dialogueId, nodeId, npcId, false);
    }

    public DialogueGui(QuestbornPlugin plugin, Player player, String dialogueId, String nodeId, String npcId, boolean instant) {
        this.plugin = plugin;
        this.player = player;
        this.dialogueId = dialogueId;
        this.nodeId = nodeId;
        this.npcId = npcId;
        this.instant = instant;

        this.layout = plugin.getMenuConfig().getLayout("dialogue");

        int rows = layout != null ? layout.getRows() : 4;
        String title = layout != null && layout.getProperty("title") != null ? layout.getProperty("title").toString() : "Dialogue";

        if (title.contains("{npc}") || title.contains("{npc_clean}")) {
            String npcName = plugin.getNpcManager().getDisplayName(npcId);
            if (npcName != null) {
                if (title.contains("{npc_clean}")) {
                    title = title.replace("{npc_clean}", ColorFormatter.stripColors(npcName));
                }
                if (title.contains("{npc}")) {
                    title = title.replace("{npc}", npcName);
                }
            }
        }
        title = ua.woody.questborn.util.PlaceholderUtil.format(plugin, player, title);

        this.gui = Gui.gui()
                .title(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(ColorFormatter.applyColors(title)))
                .rows(rows)
                .disableAllInteractions()
                .create();

        this.gui.setCloseGuiAction(event -> {
            if (animationTask != null && !animationTask.isCancelled()) {
                animationTask.cancel();
            }
        });

        GuiUtils.fillLayout(plugin, gui, layout);

        populate();
    }

    private void populate() {
        Dialogue dialogue = plugin.getDialogueManager().getDialogues().get(dialogueId);
        if (dialogue == null) return;

        DialogueNode node = dialogue.getNodes().get(nodeId);
        if (node == null) return;

        int npcSlot = 13;
        if (layout != null && !layout.getSlots("npc").isEmpty()) {
            npcSlot = layout.getSlots("npc").get(0);
        }

        String npcDisp = plugin.getNpcManager().getDisplayName(npcId);
        if (npcDisp == null) npcDisp = npcId;
        String rawNpcName = npcDisp;

        String npcName = ua.woody.questborn.util.PlaceholderUtil.format(plugin, player, rawNpcName);
        if (npcName == null || npcName.isEmpty()) npcName = "NPC";

        ItemStack npcIcon = null;
        String textureBase64 = null;
        ua.woody.questborn.model.NpcConfig npcConfig = plugin.getNpcManager().getConfigByNpcId(npcId);

        if (npcConfig != null && npcConfig.getHeadTexture() != null && !npcConfig.getHeadTexture().isEmpty()) {
            textureBase64 = npcConfig.getHeadTexture();
        }

        else if (npcId != null && !npcId.equals("none")) {
            String dynamicSkin = plugin.getNpcIntegrationManager().getSkinTextureBase64(npcId);
            if (dynamicSkin != null && !dynamicSkin.isEmpty()) {
                textureBase64 = dynamicSkin;
            }
        }

        if (textureBase64 != null && !textureBase64.isEmpty()) {
            npcIcon = new ItemStack(Material.PLAYER_HEAD);
            try {
                SkullMeta sm = (SkullMeta) npcIcon.getItemMeta();
                UUID id = new UUID(textureBase64.hashCode(), textureBase64.hashCode());
                PlayerProfile profile = Bukkit.createProfile(id);
                profile.setProperty(new ProfileProperty("textures", textureBase64));
                sm.setPlayerProfile(profile);
                npcIcon.setItemMeta(sm);
            } catch (Exception ignored) {}
        } else if (plugin.getNpcIntegrationManager() != null && plugin.getNpcIntegrationManager().getProvider() != null && npcId != null && !npcId.equals("none")) {
            org.bukkit.entity.EntityType npcType = plugin.getNpcIntegrationManager().getProvider().getType(npcId);
            String providerName = plugin.getNpcIntegrationManager().getProvider().getName(npcId);
            npcIcon = plugin.getNpcIntegrationManager().createNpcIcon(npcType, providerName, npcName);
        } else {
            npcIcon = new ItemStack(Material.PLAYER_HEAD);
        }

        ItemMeta meta = npcIcon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorFormatter.applyColors(npcName));
            npcIcon.setItemMeta(meta);
        }

        List<String> fullLines = new ArrayList<>();
        List<String> nodeTextFormatted = ua.woody.questborn.util.PlaceholderUtil.format(plugin, player, node.getText());
        String displayName = plugin.getNpcManager().getDisplayName(npcId);
        if (displayName == null || displayName.isEmpty()) displayName = npcId != null ? npcId : "NPC";
        for (String line : nodeTextFormatted) {
            if (line != null && line.contains("{npc}")) {
                line = line.replace("{npc}", displayName);
            }
            fullLines.add(ColorFormatter.applyColors("<#a8a8a8>" + line));
        }

        List<String> cleanLines = new ArrayList<>();
        for (String line : fullLines) {
            cleanLines.add(line.replaceAll("(?i)</?shout>|</?whisper>|</?normal>", ""));
        }

        int finalNpcSlot = npcSlot;
        final ItemStack finalNpcIcon = npcIcon;
        gui.setItem(npcSlot, new GuiItem(finalNpcIcon.clone(), event -> {
            event.setCancelled(true);
            if (!isFinished) {
                skipAnimation(node, finalNpcSlot, finalNpcIcon, cleanLines);
            }
        }));

        List<Integer> optionSlotsFallback = new ArrayList<>();
        if (layout != null && !layout.getSlots("options").isEmpty()) {
            optionSlotsFallback = layout.getSlots("options");
        } else {
            optionSlotsFallback.add(28); optionSlotsFallback.add(29); optionSlotsFallback.add(30); optionSlotsFallback.add(31); optionSlotsFallback.add(32); optionSlotsFallback.add(33); optionSlotsFallback.add(34);
        }
        final List<Integer> optionSlots = optionSlotsFallback;

        int index = 0;
        for (DialogueOption option : getActiveOptions(node, optionSlots)) {
            if (!ua.woody.questborn.integration.npc.DialogueConditionParser.checkConditions(plugin, player, npcId, option.getConditions())) {
                index++;
                continue;
            }
            int slot = option.getSlot() != -1 ? option.getSlot() : (index < optionSlots.size() ? optionSlots.get(index) : -1);
            if (slot == -1) {
                index++;
                continue;
            }
            ItemStack waitItem = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta wm = waitItem.getItemMeta();
            if (wm != null) {
                wm.setDisplayName(ColorFormatter.applyColors("<#a8a8a8>..."));
                waitItem.setItemMeta(wm);
            }
            gui.setItem(slot, new GuiItem(waitItem, e -> e.setCancelled(true)));
            index++;
        }

        if (node.getSound() != null && !node.getSound().isEmpty()) {
            try {
                Sound sound = Sound.valueOf(node.getSound().toUpperCase(java.util.Locale.ROOT));
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (Exception ignored) {}
        }

        if (this.instant) {
            skipAnimation(node, npcSlot, npcIcon, cleanLines);
        } else {
            startTypewriter(dialogue, node, npcSlot, npcIcon, fullLines, cleanLines, optionSlots);
        }
    }

    private void startTypewriter(Dialogue dialogue, DialogueNode node, int npcSlot, ItemStack baseIcon, List<String> fullLines, List<String> cleanLines, List<Integer> optionSlots) {
        List<String> currentLines = new ArrayList<>();
        for (int i = 0; i < fullLines.size(); i++) {
            currentLines.add(" ");
        }

        long period = dialogue.getUpdatePeriod() > 0 ? dialogue.getUpdatePeriod() : 1L;
        int charsPerTick = dialogue.getCpt() > 0 ? dialogue.getCpt() : 1;

        final int[] readIndices = new int[fullLines.size()];
        final float[] voiceModifiers = new float[] {1.0f, 1.0f};

        animationTask = plugin.getFoliaLib().getImpl().runAtEntityTimer(player, () -> {
            if (isFinished) {
                if (animationTask != null && !animationTask.isCancelled()) {
                    animationTask.cancel();
                }
                return;
            }

            boolean advanced = false;
            int charsToAdvance = charsPerTick;

            for (int k = 0; k < charsToAdvance; k++) {
                int lineIndex = -1;
                for (int i = 0; i < fullLines.size(); i++) {
                    if (readIndices[i] < fullLines.get(i).length()) {
                        lineIndex = i;
                        break;
                    }
                }

                if (lineIndex == -1) {
                    skipAnimation(node, npcSlot, baseIcon, cleanLines);
                    return;
                }

                String fullLine = fullLines.get(lineIndex);
                int rIdx = readIndices[lineIndex];

                String remainingLine = fullLine.substring(rIdx).toLowerCase(java.util.Locale.ROOT);
                if (remainingLine.startsWith("<shout>")) {
                    voiceModifiers[0] = 0.7f;
                    voiceModifiers[1] = 1.5f;
                    readIndices[lineIndex] += 7;
                    k--;
                    continue;
                } else if (remainingLine.startsWith("<whisper>")) {
                    voiceModifiers[0] = 1.4f;
                    voiceModifiers[1] = 0.5f;
                    readIndices[lineIndex] += 9;
                    k--;
                    continue;
                } else if (remainingLine.startsWith("<normal>")) {
                    voiceModifiers[0] = 1.0f;
                    voiceModifiers[1] = 1.0f;
                    readIndices[lineIndex] += 8;
                    k--;
                    continue;
                } else if (remainingLine.startsWith("</shout>")) {
                    voiceModifiers[0] = 1.0f;
                    voiceModifiers[1] = 1.0f;
                    readIndices[lineIndex] += 8;
                    k--;
                    continue;
                } else if (remainingLine.startsWith("</whisper>")) {
                    voiceModifiers[0] = 1.0f;
                    voiceModifiers[1] = 1.0f;
                    readIndices[lineIndex] += 10;
                    k--;
                    continue;
                }

                if (rIdx + 1 < fullLine.length() && fullLine.charAt(rIdx) == '\u00a7') {
                    currentLines.set(lineIndex, currentLines.get(lineIndex) + fullLine.substring(rIdx, rIdx + 2));
                    readIndices[lineIndex] += 2;
                    k--;
                } else {
                    currentLines.set(lineIndex, currentLines.get(lineIndex) + fullLine.charAt(rIdx));
                    readIndices[lineIndex] += 1;
                }

                advanced = true;
            }

            if (advanced) {
                String currentVoice = "none";
                if (npcId != null && !npcId.equals("none")) {
                    var npcConfig = plugin.getNpcManager().getConfigByNpcId(npcId);
                    if (npcConfig != null && npcConfig.getVoice() != null) {
                        currentVoice = npcConfig.getVoice();
                    }
                }

                playVoiceSound(currentVoice, voiceModifiers[0], voiceModifiers[1]);

                ItemStack icon = baseIcon.clone();
                ItemMeta meta = icon.getItemMeta();
                if (meta != null) {
                    meta.setLore(currentLines);
                    icon.setItemMeta(meta);
                }
                gui.updateItem(npcSlot, icon);
            }
        }, 1L, period);
    }

    private void playVoiceSound(String voice, float pitchModifier, float volumeModifier) {
        if (voice == null) voice = "none";
        voice = voice.toLowerCase(java.util.Locale.ROOT);

        if (voice.equals("none")) return;

        if (Math.random() > 0.4) return;

        Sound sound = getSoundFromVoice(voice);
        if (sound == null) return;

        float pitch = 1.0f;
        float volume = 0.6f;

        switch (voice) {
            case "villager": pitch = 1.5f; break;
            case "deep": case "old": pitch = 0.5f; break;
            case "cute": case "fairy": pitch = 2.0f; volume = 0.4f; break;
            case "monster": case "zombie": pitch = 1.0f; volume = 0.3f; break;
            case "undead": case "infected": pitch = 1.0f; volume = 0.4f; break;
            case "orc": case "goblin": case "piglin": pitch = 1.0f; volume = 0.5f; break;
            case "spider": case "snake": pitch = 1.0f; volume = 0.3f; break;
            case "void": case "enderman": pitch = 1.0f; volume = 0.5f; break;
            case "ghost": case "spirit": pitch = 1.0f; volume = 0.4f; break;
            case "robot": pitch = 1.0f; volume = 0.5f; break;
            case "cat": case "pet": pitch = 1.0f; volume = 0.5f; break;
            case "magic": pitch = 1.0f; break;
            default:
                pitch = 1.0f;
                break;
        }

        player.playSound(player.getLocation(), sound, volume * volumeModifier, pitch * pitchModifier);
    }

    public static float getPitchFromVoice(String voice) {
        if (voice == null || voice.equalsIgnoreCase("none")) return 1.0f;
        voice = voice.toLowerCase(java.util.Locale.ROOT);
        switch (voice) {
            case "villager": return 1.5f;
            case "deep": case "old": return 0.5f;
            case "cute": case "fairy": return 2.0f;
            default: return 1.0f;
        }
    }

    private java.util.List<DialogueOption> getActiveOptions(DialogueNode node, java.util.List<Integer> optionSlots) {
        return new java.util.ArrayList<>(node.getOptions());
    }

    public static Sound getSoundFromVoice(String voice) {
        if (voice == null || voice.equalsIgnoreCase("none")) return null;
        voice = voice.toLowerCase(java.util.Locale.ROOT);

        switch (voice) {
            case "villager":
            case "deep":
            case "old":
                return Sound.ENTITY_VILLAGER_AMBIENT;
            case "trader":
            case "shaman":
                return Sound.ENTITY_WANDERING_TRADER_AMBIENT;
            case "guard":
            case "pillager":
                return Sound.ENTITY_PILLAGER_AMBIENT;
            case "witch":
            case "hag":
                return Sound.ENTITY_WITCH_AMBIENT;
            case "evoker":
            case "cultist":
                return Sound.ENTITY_EVOKER_AMBIENT;
            case "cute":
            case "fairy":
                return Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
            case "monster":
            case "zombie":
                return Sound.ENTITY_ZOMBIE_AMBIENT;
            case "undead":
            case "infected":
                return Sound.ENTITY_ZOMBIE_VILLAGER_AMBIENT;
            case "orc":
            case "goblin":
            case "piglin":
                return Sound.ENTITY_PIGLIN_AMBIENT;
            case "spider":
            case "snake":
                return Sound.ENTITY_SPIDER_AMBIENT;
            case "void":
            case "enderman":
                return Sound.ENTITY_ENDERMAN_AMBIENT;
            case "ghost":
            case "spirit":
                return Sound.ENTITY_GHAST_AMBIENT;
            case "robot":
                return Sound.ENTITY_IRON_GOLEM_REPAIR;
            case "cat":
            case "pet":
                return Sound.ENTITY_CAT_PURREOW;
            case "magic":
                return Sound.BLOCK_NOTE_BLOCK_BELL;
            default:
                try {
                    return Sound.valueOf(voice.toUpperCase(java.util.Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    return null;
                }
        }
    }

    private void skipAnimation(DialogueNode node, int npcSlot, ItemStack baseIcon, List<String> fullLines) {
        if (isFinished) return;

        if (animationTask != null) animationTask.cancel();

        ItemStack icon = baseIcon.clone();
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setLore(fullLines);
            icon.setItemMeta(meta);
        }
        gui.updateItem(npcSlot, icon);

        finishAnimation(node, layout != null && !layout.getSlots("options").isEmpty() ? layout.getSlots("options") : java.util.List.of(28, 29, 30, 31, 32, 33, 34));
    }

    private void finishAnimation(DialogueNode node, List<Integer> optionSlots) {
        if (isFinished) return;
        isFinished = true;

        if (animationTask != null && !animationTask.isCancelled()) {
            animationTask.cancel();
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);

        int index = 0;
        for (DialogueOption option : getActiveOptions(node, optionSlots)) {
            if (!ua.woody.questborn.integration.npc.DialogueConditionParser.checkConditions(plugin, player, npcId, option.getConditions())) {
                index++;
                continue;
            }
                    int slot = option.getSlot() != -1 ? option.getSlot() : (index < optionSlots.size() ? optionSlots.get(index) : -1);
            if (slot == -1) {
                index++;
                continue;
            }

            ItemStack optItem = null;
            if (option.getMaterial() != null) {
                String mat = option.getMaterial().trim();
                if ((mat.toLowerCase().startsWith("craftengine:") || mat.toLowerCase().startsWith("ce:")) && plugin.getCraftEngineIntegration() != null && plugin.getCraftEngineIntegration().isEnabled()) {
                    String ceId = mat.toLowerCase().startsWith("ce:") ? mat.substring(3) : mat.substring(12);
                    ItemStack ceItem = plugin.getCraftEngineIntegration().getCustomItem(ceId);
                    if (ceItem != null) optItem = ceItem.clone();
                } else if ((mat.toLowerCase().startsWith("itemsadder:") || mat.toLowerCase().startsWith("ia:")) && plugin.getItemsAdderIntegration() != null && plugin.getItemsAdderIntegration().isEnabled()) {
                    String iaId = mat.toLowerCase().startsWith("ia:") ? mat.substring(3) : mat.substring(11);
                    ItemStack iaItem = plugin.getItemsAdderIntegration().getCustomItem(iaId);
                    if (iaItem != null) optItem = iaItem.clone();
                } else {
                    try {
                        optItem = new ItemStack(Material.valueOf(mat.toUpperCase()));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            if (optItem == null) optItem = new ItemStack(Material.PAPER);

            ItemMeta optMeta = optItem.getItemMeta();
            if (optMeta != null) {
                String displayName = plugin.getNpcManager().getDisplayName(npcId);
                if (displayName == null || displayName.isEmpty()) displayName = npcId != null ? npcId : "NPC";

                String formattedText = ua.woody.questborn.util.PlaceholderUtil.format(plugin, player, option.getText());
                if (formattedText != null && formattedText.contains("{npc}")) {
                    formattedText = formattedText.replace("{npc}", displayName);
                }
                optMeta.setDisplayName(ColorFormatter.applyColors(formattedText));

                if (option.getLore() != null && !option.getLore().isEmpty()) {
                    List<String> coloredLore = new ArrayList<>();
                    for (String line : ua.woody.questborn.util.PlaceholderUtil.format(plugin, player, option.getLore())) {
                        if (line != null && line.contains("{npc}")) {
                            line = line.replace("{npc}", displayName);
                        }
                        coloredLore.add(ColorFormatter.applyColors(line));
                    }
                    optMeta.setLore(coloredLore);
                }
                optItem.setItemMeta(optMeta);
            }

            gui.updateItem(slot, new GuiItem(optItem, event -> {
                event.setCancelled(true);

                if (option.getSound() != null && !option.getSound().isEmpty()) {
                    try {
                        Sound s = Sound.valueOf(option.getSound().toUpperCase());
                        player.playSound(player.getLocation(), s, 1.0f, 1.0f);
                    } catch (Exception ignored) {}
                } else {
                    GuiUtils.playClickSound(plugin, player, npcId);
                }

                boolean shouldClose = true;
                if (option.getAction() != null) {
                    String[] actions = option.getAction().split(";");
                    for (String actionStr : actions) {
                        String action = actionStr.trim();
                        if (action.startsWith("goto:")) {
                            String targetNode = action.substring(5).trim();
                            boolean skip = false;
                            if (targetNode.endsWith(":skip")) {
                                skip = true;
                                targetNode = targetNode.substring(0, targetNode.length() - 5);
                            }
                            if (targetNode.contains(",")) {
                                String[] targets = targetNode.split(",");
                                targetNode = targets[new java.util.Random().nextInt(targets.length)].trim();
                            }
                            final String finalTarget = targetNode;
                            final boolean finalSkip = skip;
                            shouldClose = false;
                            if (!finalSkip) {
                                player.closeInventory();
                                plugin.getFoliaLib().getImpl().runAtEntityLater(player, () -> {
                                    new DialogueGui(plugin, player, dialogueId, finalTarget, npcId, finalSkip).open();
                                }, 1L);
                            } else {
                                plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> {
                                    new DialogueGui(plugin, player, dialogueId, finalTarget, npcId, finalSkip).open();
                                });
                            }
                        } else if (action.equals("open_quest_menu") || action.equals("give_items") || action.startsWith("open_quest:")) {
                            shouldClose = false;
                            ua.woody.questborn.integration.npc.NpcActionExecutor.executeAction(plugin, player, npcId, action, dialogueId, node.getId());
                        } else {
                            ua.woody.questborn.integration.npc.NpcActionExecutor.executeAction(plugin, player, npcId, action, dialogueId, node.getId());
                        }
                    }
                }
                if (shouldClose) {
                    plugin.getFoliaLib().getImpl().runNextTick(__task -> player.closeInventory());
                }
            }));

            index++;
        }
    }

    public void open() {
        gui.open(player);
    }
    public static boolean hasRewardFromNpc(ua.woody.questborn.QuestbornPlugin plugin, org.bukkit.entity.Player player, String npcId) {
        if (npcId == null || npcId.equals("none")) return false;
        var npcConfig = plugin.getNpcManager().getConfigByNpcId(npcId);
        var data = plugin.getPlayerDataStore().get(player.getUniqueId());
        if (data == null) return false;

        for (String qId : data.getPendingRewards()) {
            ua.woody.questborn.model.QuestDefinition q = plugin.getQuestManager().getQuest(qId);
            if (q != null && plugin.getQuestManager().canClaimRewardFromNpc(player, q, npcId)) {
                return true;
            }
        }

        return false;
    }

    public static boolean claimRewardFromNpc(ua.woody.questborn.QuestbornPlugin plugin, org.bukkit.entity.Player player, String npcId) {
        boolean claimed = false;
        if (npcId == null || npcId.equals("none")) return false;
        var data = plugin.getPlayerDataStore().get(player.getUniqueId());
        if (data == null) return false;

        for (String qId : new java.util.ArrayList<>(data.getPendingRewards())) {
            ua.woody.questborn.model.QuestDefinition q = plugin.getQuestManager().getQuest(qId);
            if (q != null && plugin.getQuestManager().canClaimRewardFromNpc(player, q, npcId)) {
                plugin.getQuestManager().claimReward(player, q);
                claimed = true;
            }
        }

        return claimed;
    }

    public static ua.woody.questborn.model.QuestDefinition getGiveItemQuestStatic(ua.woody.questborn.QuestbornPlugin plugin, Player player, String npcId) {
        if (npcId == null || npcId.equals("none")) return null;
        var data = plugin.getPlayerDataStore().get(player.getUniqueId());
        if (data == null) return null;

        for (String activeId : data.getActiveQuests().keySet()) {
            ua.woody.questborn.model.QuestDefinition q = plugin.getQuestManager().getQuest(activeId);
            if (q != null) {
                ua.woody.questborn.model.PlayerQuestProgress.ActiveQuestData qd = data.getQuestData(activeId);
                ua.woody.questborn.model.QuestObjective objective = null;
                if (q.hasStages()) {
                    ua.woody.questborn.model.QuestStage stage = q.getStage(qd.getCurrentStage());
                    if (stage != null && stage.isObjective()) objective = stage.getObjective();
                } else {
                    objective = q.getObjective();
                }

                if (objective != null && npcId.equalsIgnoreCase(objective.getNpc()) && objective.getType() == ua.woody.questborn.model.QuestObjectiveType.GIVE_ITEM_TO_NPC) {
                    int currentProgress = q.hasStages() ? qd.getStageProgress() : qd.getProgress();
                    if (currentProgress < plugin.getQuestManager().getTargetAmount(objective)) {
                        return q;
                    }
                }
            }
        }
        return null;
    }
}
