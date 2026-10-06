package ua.woody.questborn.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.config.GuiLayout;
import ua.woody.questborn.lang.ColorFormatter;
import ua.woody.questborn.lang.LanguageManager;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestItem;
import ua.woody.questborn.model.QuestStage;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.util.ItemDisplayUtil;

import java.util.*;

public class QuestTransferGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final QuestDefinition quest;
    private final LanguageManager lang;
    private final boolean simpleView;
    private final ItemStack npcIcon;
    private final String npcId;
    private final Gui gui;
    private final GuiLayout layout;

    private final String returnDialogueId;
    private final String returnNodeId;
    private int returnPage = 1;

    public void setReturnPage(int page) {
        this.returnPage = page;
    }

    private final List<Integer> inputSlots;
    private int infoSlot = 4;
    private int cancelSlot = 38;
    private int transferSlot = 42;

    private boolean isTransferring = false;

    public QuestTransferGui(QuestbornPlugin plugin, Player player, QuestDefinition quest) {
        this(plugin, player, quest, false, null, null, null, null);
    }

    public QuestTransferGui(QuestbornPlugin plugin, Player player, QuestDefinition quest, boolean simpleView) {
        this(plugin, player, quest, simpleView, null, null, null, null);
    }

    public QuestTransferGui(QuestbornPlugin plugin, Player player, QuestDefinition quest, boolean simpleView, ItemStack npcIcon, String npcId) {
        this(plugin, player, quest, simpleView, npcIcon, npcId, null, null);
    }

    public QuestTransferGui(QuestbornPlugin plugin, Player player, QuestDefinition quest, boolean simpleView, ItemStack npcIcon, String npcId, String returnDialogueId, String returnNodeId) {
        this.plugin = plugin;
        this.player = player;
        this.quest = quest;
        this.lang = plugin.getLanguage();
        this.simpleView = simpleView;
        this.npcIcon = npcIcon;
        this.npcId = npcId;
        this.returnDialogueId = returnDialogueId;
        this.returnNodeId = returnNodeId;
        this.layout = plugin.getMenuConfig().getLayout("quest-transfer");

        this.inputSlots = new ArrayList<>();
        if (layout != null) {
            List<Integer> slots = layout.getSlots("input");
            if (slots != null) this.inputSlots.addAll(slots);
            if (this.inputSlots.isEmpty()) {
                this.inputSlots.addAll(List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34));
            }
            if (layout.getItem("info") != null && layout.getItem("info").getSlot() >= 0) infoSlot = layout.getItem("info").getSlot();
            if (layout.getItem("cancel") != null && layout.getItem("cancel").getSlot() >= 0) cancelSlot = layout.getItem("cancel").getSlot();
            if (layout.getItem("confirm") != null && layout.getItem("confirm").getSlot() >= 0) transferSlot = layout.getItem("confirm").getSlot();
        } else {
            this.inputSlots.addAll(List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34));
        }

        String title = calculateGuiTitle(plugin, quest, npcId);

        this.gui = Gui.gui()
                .title(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(title))
                .rows(layout != null ? layout.getSize() / 9 : 6)
                .create();

        gui.setDefaultTopClickAction(event -> {
            if (inputSlots.contains(event.getSlot())) {
                event.setCancelled(false);
                plugin.getFoliaLib().getImpl().runNextTick(__task -> this.updateTransferButton());
            } else {
                event.setCancelled(true);
            }
        });

        gui.setPlayerInventoryAction(event -> {
            event.setCancelled(false);
            if (event.isShiftClick()) {
                plugin.getFoliaLib().getImpl().runNextTick(__task -> this.updateTransferButton());
            }
        });

        gui.setDragAction(event -> {
            for (int slot : event.getRawSlots()) {
                if (slot < gui.getRows() * 9 && !inputSlots.contains(slot)) {
                    event.setCancelled(true);
                    return;
                }
            }
            plugin.getFoliaLib().getImpl().runNextTick(__task -> this.updateTransferButton());
        });

        gui.setCloseGuiAction(event -> {
            if (!isTransferring) {
                returnItemsToPlayer();
            }
        });

        build();
    }

    private static String calculateGuiTitle(QuestbornPlugin plugin, QuestDefinition quest, String npcId) {
        QuestTypeConfig typeConfig = plugin.getQuestManager().getQuestTypeManager().getType(quest.getTypeId());
        String questName = quest.getDisplayName();
        String typeName = typeConfig != null ? typeConfig.getDisplayName() : "";

        if (typeConfig != null && typeConfig.getGuiTransferTitle() != null) {
            String titleTemplate = typeConfig.getGuiTransferTitle();
            if (titleTemplate.contains("&") || titleTemplate.contains("<#") || titleTemplate.contains("§")) {
                questName = ColorFormatter.stripColors(questName);
                typeName = ColorFormatter.stripColors(typeName);
            }
            String title = titleTemplate.replace("{quest}", questName).replace("{type}", typeName);
            return ColorFormatter.applyColors(title);
        }

        questName = ColorFormatter.stripColors(quest.getDisplayName());

        if (npcId != null && plugin.getNpcManager() != null) {
            ua.woody.questborn.model.NpcConfig npcConfig = plugin.getNpcManager().getConfigByNpcId(npcId);
            if (npcConfig != null && npcConfig.getGuiTitle() != null) {
                String layoutTitle = npcConfig.getGuiTitle();
                String titleWithPlaceholder = layoutTitle.replace("{quest}", questName);
                if (layoutTitle.contains("&") || layoutTitle.contains("<#") || layoutTitle.contains("§")) {
                    return org.bukkit.ChatColor.translateAlternateColorCodes('&', titleWithPlaceholder);
                } else {
                    return org.bukkit.ChatColor.translateAlternateColorCodes('&', "<#1c1c1c>" + titleWithPlaceholder);
                }
            }
        }

        GuiLayout layout = plugin.getMenuConfig().getLayout("quest-transfer");
        if (layout != null && layout.getProperty("title") != null) {
            String layoutTitle = layout.getProperty("title").toString();
            String titleWithPlaceholder = layoutTitle.replace("{quest}", questName);
            if (layoutTitle.contains("&") || layoutTitle.contains("<#") || layoutTitle.contains("§")) {
                return org.bukkit.ChatColor.translateAlternateColorCodes('&', titleWithPlaceholder);
            } else {
                return org.bukkit.ChatColor.translateAlternateColorCodes('&', "<#1c1c1c>" + titleWithPlaceholder);
            }
        }

        return questName;
    }

    private void build() {
        GuiUtils.fillLayout(plugin, gui, layout);

        for (int slot : inputSlots) {
            gui.removeItem(slot);
            gui.getInventory().setItem(slot, null);
        }

        addInfoItem();
        addCancelButton();
        addTransferButton();
    }

    private void addInfoItem() {
        if (infoSlot < 0 || infoSlot >= gui.getRows() * 9) return;

        Material iconMaterial = quest.getIconMaterial();
        if (iconMaterial == null || iconMaterial == Material.AIR) iconMaterial = Material.BOOK;

        ItemStack infoItem = new ItemStack(iconMaterial);
        ItemMeta meta = infoItem.getItemMeta();

        if (quest.getIconCustomModelData() != null) {
            meta.setCustomModelData(quest.getIconCustomModelData());
        }

        String customName = (layout != null && layout.getItem("info") != null) ? layout.getItem("info").getName() : null;
        if (customName != null) {
            meta.setDisplayName(lang.color(customName));
        } else {
            meta.setDisplayName(lang.color(quest.getDisplayName()));
        }

        List<String> lore = new ArrayList<>();
        for (String line : quest.getDescription()) {
            lore.add(lang.color(line));
        }

        meta.setLore(lore);
        GuiUtils.applyAllItemFlags(meta);
        infoItem.setItemMeta(meta);

        gui.setItem(infoSlot, new dev.triumphteam.gui.guis.GuiItem(infoItem, event -> event.setCancelled(true)));
    }

    private void addCancelButton() {
        if (cancelSlot < 0 || cancelSlot >= gui.getRows() * 9) return;

        ItemStack cancelItem = GuiUtils.createActionButton(plugin, layout, "cancel", Material.ORANGE_DYE, "gui.transfer.cancel.title");
        gui.setItem(cancelSlot, new dev.triumphteam.gui.guis.GuiItem(cancelItem, event -> {
            GuiUtils.playClickSound(plugin, player);
            isTransferring = true;
            returnItemsToPlayer();
            if (returnDialogueId != null && returnNodeId != null) {
                plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> new DialogueGui(plugin, player, returnDialogueId, returnNodeId, npcId).open());
            } else {
                plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> {
                    QuestDetailsGui detailsGui = new QuestDetailsGui(plugin, player, quest, simpleView, npcIcon, npcId);
                    detailsGui.setReturnPage(returnPage);
                    detailsGui.open();
                });
            }
        }));
    }

    public void updateTransferButton() {
        addTransferButton();
        GuiItem guiItem = gui.getGuiItem(transferSlot);
        if (guiItem != null) {
            gui.getInventory().setItem(transferSlot, guiItem.getItemStack());
        }
    }

    private void addTransferButton() {
        if (transferSlot < 0 || transferSlot >= gui.getRows() * 9) return;

        Map<QuestItem, Integer> required = getRequiredItemsForCurrentStage();

        QuestTypeConfig typeConfig = plugin.getQuestManager().getQuestTypeManager().getType(quest.getTypeId());
        boolean isGlobal = (typeConfig != null && typeConfig.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL);
        int remainingGlobal = 0;
        if (isGlobal) {
            ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(quest.getId());
            int globalGoal = plugin.getQuestManager().getProgressProcessor().getGlobalGoal(quest);
            int currentGlobal = gp != null ? gp.getGlobalProgress() : 0;
            int personalProgress = 0;
            if (gp != null && gp.hasParticipant(player.getUniqueId())) {
                personalProgress = gp.getContribution(player.getUniqueId());
            }
            remainingGlobal = globalGoal - currentGlobal;
            if (quest.getPersonalLimit() > 0) {
                int personalRemaining = quest.getPersonalLimit() - personalProgress;
                if (personalRemaining < remainingGlobal) remainingGlobal = personalRemaining;
            }
        }

        ua.woody.questborn.integration.ItemsAdderIntegration ia = plugin.getItemsAdderIntegration();
        boolean hasAnyItem = false;
        List<String> formattedItemLines = new ArrayList<>();

        for (Map.Entry<QuestItem, Integer> entry : required.entrySet()) {
            QuestItem questItem = entry.getKey();
            int requiredAmount = entry.getValue();
            if (isGlobal && quest.getPersonalLimit() > 0) {
                requiredAmount = remainingGlobal;
            }
            int currentAmount = countInInventory(questItem);

            String localizedName;
            if (questItem.isItemsAdderItem()) {
                ItemStack stack = questItem.toItemStack(ia, 1);
                localizedName = (stack != null && stack.hasItemMeta() && stack.getItemMeta().hasDisplayName())
                        ? stack.getItemMeta().getDisplayName()
                        : (stack != null ? prettify(stack.getType().name()) : questItem.getItemsAdderId());
            } else {
                localizedName = ItemDisplayUtil.findLocalization(questItem.getMaterial(), lang, false);
            }

            String formatStr = (currentAmount >= requiredAmount)
                ? plugin.getConfig().getString("gui.transfer.item-format-complete", "<#6e6e6e>[<#79d679>✔<#6e6e6e>] <#ffd470>{item} <#a8a8a8>(<#79d679>{current} <#6e6e6e>/ <#a8a8a8>{required}<#a8a8a8>)")
                : plugin.getConfig().getString("gui.transfer.item-format", "<#6e6e6e>[<#ed6868>✖<#6e6e6e>] <#ffd470>{item} <#a8a8a8>(<#ed6868>{current} <#6e6e6e>/ <#a8a8a8>{required}<#a8a8a8>)");

            String formattedLine = formatStr
                .replace("{item}", localizedName)
                .replace("{current}", String.valueOf(currentAmount))
                .replace("{required}", String.valueOf(requiredAmount))
                .replace("{status}", (currentAmount >= requiredAmount) ? "✔" : "✖");

            formattedItemLines.add(lang.color(formattedLine));

            if (currentAmount > 0) {
                hasAnyItem = true;
            }
        }

        boolean allComplete = hasAnyItem;
        if (isGlobal && remainingGlobal <= 0) {
            allComplete = false;
        }

        boolean hasWrongItems = false;
        for (int slot : inputSlots) {
            ItemStack item = gui.getInventory().getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                boolean matchesAny = false;
                for (QuestItem reqItem : required.keySet()) {
                    ua.woody.questborn.integration.CraftEngineIntegration ce = plugin.getCraftEngineIntegration();
                    if (reqItem.matches(item, ia, ce)) {
                        matchesAny = true;
                        break;
                    }
                }
                if (!matchesAny) {
                    hasWrongItems = true;
                    break;
                }
            }
        }

        String buttonKey = "confirm";
        Material defaultMat = Material.LIME_DYE;

        if (!allComplete) {
            buttonKey = "confirm-empty";
            defaultMat = Material.GRAY_DYE;
        } else if (hasWrongItems) {
            buttonKey = "confirm-wrong";
            defaultMat = Material.ORANGE_DYE;
        }

        ItemStack transferItem = GuiUtils.createActionButton(plugin, layout, buttonKey, defaultMat, "gui.transfer.transfer.title");
        ItemMeta meta = transferItem.getItemMeta();
        if (meta == null) return;

        List<String> rawLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        List<String> finalLore = new ArrayList<>();

        boolean itemsReplaced = false;
        for (String line : rawLore) {
            if (line.contains("{items}")) {
                finalLore.addAll(formattedItemLines);
                itemsReplaced = true;
            } else {
                finalLore.add(line);
            }
        }

        if (!itemsReplaced) {
            finalLore.add(" ");
            finalLore.addAll(formattedItemLines);
        }

        meta.setLore(finalLore);
        transferItem.setItemMeta(meta);

        boolean finalAllComplete = allComplete;
        gui.setItem(transferSlot, new dev.triumphteam.gui.guis.GuiItem(transferItem, event -> {
            if (!finalAllComplete) {
                GuiUtils.playClickSound(plugin, player);
                return;
            }

            if (isGlobal) {
                ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(quest.getId());
                int minP = quest.getMinParticipants();
                int currentP = gp != null ? gp.getParticipantCount() : 0;
                if (minP > 0 && currentP < minP && (gp == null || !gp.isActivationReached())) {
                    GuiUtils.playClickSound(plugin, player);
                    isTransferring = true;
                    returnItemsToPlayer();
                    player.closeInventory();
                    String msg = plugin.getLanguage().tr("quests.global.not_enough_participants", Map.of(
                            "quest", quest.getDisplayName(),
                            "min", String.valueOf(minP),
                            "current", String.valueOf(currentP)
                    ));
                    if (msg != null && !msg.isEmpty()) {
                        player.sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors(msg));
                    }
                    return;
                }
            }

            GuiUtils.playClickSound(plugin, player);
            isTransferring = true;
            Map<QuestItem, Integer> taken = takeRequiredItems();
            returnAllExcessItems();

            int totalTaken = 0;
            for (int amount : taken.values()) totalTaken += amount;

            if (totalTaken > 0) {
                boolean isGiveItem = false;
                PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());
                ua.woody.questborn.model.PlayerQuestProgress.ActiveQuestData qd = data != null ? data.getQuestData(quest.getId()) : null;

                if (quest.hasStages()) {
                    if (qd != null) {
                        int currentStageNum = Math.max(1, qd.getCurrentStage());
                        QuestStage stage = quest.getStage(currentStageNum);
                        if (stage != null && stage.isObjective() && stage.getObjective().getType() == ua.woody.questborn.model.QuestObjectiveType.GIVE_ITEM_TO_NPC) {
                            if (!stage.isRequiredMaterials()) {
                                isGiveItem = true;
                            }
                        }
                    }
                } else {
                    ua.woody.questborn.model.QuestObjective obj = quest.getObjective();
                    if (obj != null && obj.getType() == ua.woody.questborn.model.QuestObjectiveType.GIVE_ITEM_TO_NPC) {
                        if (!quest.hasRequiredMaterials()) {
                            isGiveItem = true;
                        }
                    }
                }

                int preStage = 1;
                if (qd != null) preStage = qd.getCurrentStage();

                if (isGlobal) {
                    plugin.getQuestManager().getProgressProcessor().incrementGlobalProgress(player, quest, totalTaken);

                    ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(quest.getId());
                    int globalGoal = plugin.getQuestManager().getProgressProcessor().getGlobalGoal(quest);
                    int currentGlobal = gp != null ? gp.getGlobalProgress() : 0;
                    int personalProgress = gp != null ? gp.getContribution(player.getUniqueId()) : 0;

                    int remainingGlobalCalc = globalGoal - currentGlobal;
                    if (quest.getPersonalLimit() > 0) {
                        int personalRemaining = quest.getPersonalLimit() - personalProgress;
                        if (personalRemaining < remainingGlobalCalc) remainingGlobalCalc = personalRemaining;
                    }

                    if (remainingGlobalCalc > 0) {
                        plugin.getFoliaLib().getImpl().runNextTick(__task -> new QuestTransferGui(plugin, player, quest, simpleView, npcIcon, npcId).open());
                        return;
                    }
                } else {
                    if (isGiveItem) {
                        plugin.getQuestManager().getProgressProcessor().incrementProgress(player, quest, totalTaken, true);
                    } else {
                        plugin.getQuestManager().transferItems(player, quest, taken);
                    }

                    PlayerQuestProgress postData = plugin.getPlayerDataStore().get(player.getUniqueId());
                    ua.woody.questborn.model.PlayerQuestProgress.ActiveQuestData postQd = postData != null ? postData.getQuestData(quest.getId()) : null;

                    if (postQd != null && postQd.getCurrentStage() == preStage) {
                        Map<QuestItem, Integer> remaining = getRequiredItemsForCurrentStage();
                        if (!remaining.isEmpty()) {
                            plugin.getFoliaLib().getImpl().runNextTick(__task -> new QuestTransferGui(plugin, player, quest, simpleView, npcIcon, npcId).open());
                            return;
                        }
                    }
                }

                plugin.getFoliaLib().getImpl().runNextTick(__task -> player.closeInventory());
                return;
            }

            if (returnDialogueId != null && returnNodeId != null) {
                plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> new DialogueGui(plugin, player, returnDialogueId, returnNodeId, npcId).open());
            } else {
                plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> {
                    QuestDetailsGui detailsGui = new QuestDetailsGui(plugin, player, quest, simpleView, npcIcon, npcId);
                    detailsGui.setReturnPage(returnPage);
                    detailsGui.open();
                });
            }
        }));
    }

    private int countInInventory(QuestItem questItem) {
        int count = 0;
        ua.woody.questborn.integration.ItemsAdderIntegration ia = plugin.getItemsAdderIntegration();
        for (int slot : inputSlots) {
            ItemStack item = gui.getInventory().getItem(slot);
            ua.woody.questborn.integration.CraftEngineIntegration ce = plugin.getCraftEngineIntegration();
            if (item != null && item.getType() != Material.AIR && questItem.matches(item, ia, ce)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public Map<QuestItem, Integer> getRequiredItemsForCurrentStage() {
        Map<QuestItem, Integer> required = new HashMap<>();

        PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());
        ua.woody.questborn.model.PlayerQuestProgress.ActiveQuestData qd = data != null ? data.getQuestData(quest.getId()) : null;

        if (quest.hasStages()) {
            if (qd != null) {
                int currentStageNum = Math.max(1, qd.getCurrentStage());
                QuestStage stage = quest.getStage(currentStageNum);

                if (stage != null) {
                    if (stage.isRequiredMaterials()) {
                        Map<QuestItem, Integer> stageReq = stage.getRequiredItems();
                        if (stageReq != null) {
                            QuestTypeConfig typeConfig = plugin.getQuestManager().getQuestTypeManager().getType(quest.getTypeId());
                            boolean isGlobal = (typeConfig != null && typeConfig.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL);
                            for (Map.Entry<QuestItem, Integer> entry : stageReq.entrySet()) {
                                int needed = entry.getValue();
                                if (!isGlobal && qd != null) {
                                    needed -= qd.getTransferredAmount(entry.getKey());
                                }
                                if (needed > 0) {
                                    required.put(entry.getKey(), needed);
                                }
                            }
                        }
                    } else if (stage.isObjective() && stage.getObjective().getType() == ua.woody.questborn.model.QuestObjectiveType.GIVE_ITEM_TO_NPC) {
                        ua.woody.questborn.model.QuestObjective obj = stage.getObjective();
                        int total = plugin.getQuestManager().getTargetAmount(obj);
                        int needed = total - qd.getStageProgress();
                        if (needed > 0) addObjectiveRequirement(required, obj, needed);
                    }
                }
            }
        } else {
            ua.woody.questborn.model.QuestObjective obj = quest.getObjective();
            if (obj != null && obj.getType() == ua.woody.questborn.model.QuestObjectiveType.GIVE_ITEM_TO_NPC) {
                int total = plugin.getQuestManager().getTargetAmount(obj);
                int needed = total - (qd != null ? qd.getProgress() : 0);
                if (needed > 0) addObjectiveRequirement(required, obj, needed);
            }
        }

        return required;
    }

    private void addObjectiveRequirement(Map<QuestItem, Integer> required, ua.woody.questborn.model.QuestObjective obj, int needed) {
        if (obj.getTargetMaterials() != null && !obj.getTargetMaterials().isEmpty()) {
            required.put(new QuestItem(obj.getTargetMaterials().get(0)), needed);
        } else if (obj.getTargetItems() != null && !obj.getTargetItems().isEmpty()) {
            required.put(new QuestItem(obj.getTargetItems().get(0)), needed);
        } else if (obj.getTargetBlockIds() != null && !obj.getTargetBlockIds().isEmpty()) {
            String blockId = obj.getTargetBlockIds().get(0);
            String lowerBlockId = blockId.toLowerCase(Locale.ROOT);
            if (lowerBlockId.startsWith("itemsadder:")) {
                required.put(new QuestItem(blockId.substring(11)), needed);
            } else if (lowerBlockId.startsWith("ia:")) {
                required.put(new QuestItem(blockId.substring(3)), needed);
            } else if (lowerBlockId.startsWith("craftengine:")) {
                required.put(new QuestItem(blockId.substring(12), true), needed);
            } else if (lowerBlockId.startsWith("ce:")) {
                required.put(new QuestItem(blockId.substring(3), true), needed);
            } else {
                try {
                    required.put(new QuestItem(Material.valueOf(blockId.toUpperCase(Locale.ROOT))), needed);
                } catch (Exception ignored) {}
            }
        } else if (obj.getItem() != null && !obj.getItem().isEmpty()) {
            String itemStr = obj.getItem();
            String lowerItemStr = itemStr.toLowerCase(Locale.ROOT);
            if (lowerItemStr.startsWith("itemsadder:")) {
                required.put(new QuestItem(itemStr.substring(11)), needed);
            } else if (lowerItemStr.startsWith("ia:")) {
                required.put(new QuestItem(itemStr.substring(3)), needed);
            } else if (lowerItemStr.startsWith("craftengine:")) {
                required.put(new QuestItem(itemStr.substring(12), true), needed);
            } else if (lowerItemStr.startsWith("ce:")) {
                required.put(new QuestItem(itemStr.substring(3), true), needed);
            } else {
                try {
                    required.put(new QuestItem(Material.valueOf(itemStr.toUpperCase(Locale.ROOT))), needed);
                } catch (Exception ignored) {}
            }
        }
    }

    public Map<QuestItem, Integer> takeRequiredItems() {
        Map<QuestItem, Integer> required = getRequiredItemsForCurrentStage();

        QuestTypeConfig typeConfig = plugin.getQuestManager().getQuestTypeManager().getType(quest.getTypeId());
        boolean isGlobal = (typeConfig != null && typeConfig.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL);
        int remainingGlobal = 0;
        if (isGlobal) {
            ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(quest.getId());
            int globalGoal = plugin.getQuestManager().getProgressProcessor().getGlobalGoal(quest);
            int currentGlobal = gp != null ? gp.getGlobalProgress() : 0;
            int personalProgress = 0;
            if (gp != null && gp.hasParticipant(player.getUniqueId())) {
                personalProgress = gp.getContribution(player.getUniqueId());
            }
            remainingGlobal = globalGoal - currentGlobal;
            if (quest.getPersonalLimit() > 0) {
                int personalRemaining = quest.getPersonalLimit() - personalProgress;
                if (personalRemaining < remainingGlobal) remainingGlobal = personalRemaining;
            }
        }

        Map<QuestItem, Integer> taken = new HashMap<>();
        ua.woody.questborn.integration.ItemsAdderIntegration ia = plugin.getItemsAdderIntegration();

        Map<QuestItem, Integer> remainingNeeded = new HashMap<>(required);
        if (isGlobal) {
            for (Map.Entry<QuestItem, Integer> entry : remainingNeeded.entrySet()) {
                entry.setValue(remainingGlobal);
            }
        }
        List<ItemStack> itemsToReturn = new ArrayList<>();

        for (int slot : inputSlots) {
            ItemStack item = gui.getInventory().getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            QuestItem matchedReq = null;
            for (QuestItem req : remainingNeeded.keySet()) {
                ua.woody.questborn.integration.CraftEngineIntegration ce = plugin.getCraftEngineIntegration();
                if (req.matches(item, ia, ce)) {
                    matchedReq = req;
                    break;
                }
            }

            if (matchedReq == null) {
                itemsToReturn.add(item.clone());
                gui.getInventory().setItem(slot, null);
                continue;
            }

            int needed = remainingNeeded.get(matchedReq);
            int available = item.getAmount();

            if (needed > 0) {
                int take = Math.min(available, needed);
                taken.merge(matchedReq, take, Integer::sum);
                remainingNeeded.put(matchedReq, needed - take);

                if (take == available) {
                    gui.getInventory().setItem(slot, null);
                } else {
                    ItemStack leftover = item.clone();
                    leftover.setAmount(available - take);
                    itemsToReturn.add(leftover);
                    gui.getInventory().setItem(slot, null);
                }
            } else {
                itemsToReturn.add(item.clone());
                gui.getInventory().setItem(slot, null);
            }
        }

        if (!itemsToReturn.isEmpty()) {
            returnItemsToPlayer(itemsToReturn);
        }

        return taken;
    }

    public void returnAllExcessItems() {
        Map<QuestItem, Integer> required = getRequiredItemsForCurrentStage();

        QuestTypeConfig typeConfig = plugin.getQuestManager().getQuestTypeManager().getType(quest.getTypeId());
        boolean isGlobal = (typeConfig != null && typeConfig.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL);
        int remainingGlobal = 0;
        if (isGlobal) {
            ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(quest.getId());
            int globalGoal = plugin.getQuestManager().getProgressProcessor().getGlobalGoal(quest);
            int currentGlobal = gp != null ? gp.getGlobalProgress() : 0;
            int personalProgress = 0;
            if (gp != null && gp.hasParticipant(player.getUniqueId())) {
                personalProgress = gp.getContribution(player.getUniqueId());
            }
            remainingGlobal = globalGoal - currentGlobal;
            if (quest.getPersonalLimit() > 0) {
                int personalRemaining = quest.getPersonalLimit() - personalProgress;
                if (personalRemaining < remainingGlobal) remainingGlobal = personalRemaining;
            }
        }

        ua.woody.questborn.integration.ItemsAdderIntegration ia = plugin.getItemsAdderIntegration();

        Map<QuestItem, Integer> currentCounts = new HashMap<>();
        for (QuestItem qi : required.keySet()) currentCounts.put(qi, 0);

        List<ItemStack> excessItems = new ArrayList<>();

        for (int slot : inputSlots) {
            ItemStack item = gui.getInventory().getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            QuestItem matched = null;
            for (QuestItem qi : required.keySet()) {
                ua.woody.questborn.integration.CraftEngineIntegration ce = plugin.getCraftEngineIntegration();
                if (qi.matches(item, ia, ce)) {
                    matched = qi;
                    break;
                }
            }

            if (matched == null) {
                excessItems.add(item.clone());
                gui.getInventory().setItem(slot, null);
            } else {
                int cur = currentCounts.get(matched);
                int req = required.get(matched);
                int itemAmt = item.getAmount();

                if (cur >= req) {
                    excessItems.add(item.clone());
                    gui.getInventory().setItem(slot, null);
                } else {
                    int canTake = req - cur;
                    if (itemAmt <= canTake) {
                        currentCounts.put(matched, cur + itemAmt);
                    } else {
                        int keep = canTake;
                        int returnAmt = itemAmt - keep;

                        currentCounts.put(matched, cur + keep);

                        ItemStack toReturn = item.clone();
                        toReturn.setAmount(returnAmt);
                        excessItems.add(toReturn);

                        item.setAmount(keep);
                        gui.getInventory().setItem(slot, item);
                    }
                }
            }
        }

        if (!excessItems.isEmpty()) {
            returnItemsToPlayer(excessItems);
        }
    }

    public void returnItemsToPlayer() {
        List<ItemStack> itemsToReturn = new ArrayList<>();
        for (int slot : inputSlots) {
            ItemStack item = gui.getInventory().getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                itemsToReturn.add(item.clone());
                gui.getInventory().setItem(slot, null);
            }
        }

        if (!itemsToReturn.isEmpty()) {
            returnItemsToPlayer(itemsToReturn);
        }
    }

    public void returnItemsToPlayer(List<ItemStack> items) {
        if (player == null) {
            plugin.getLogger().warning("Player not found when returning items.");
            return;
        }

        Inventory playerInv = player.getInventory();
        boolean itemsDropped = false;

        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                Map<Integer, ItemStack> leftover = playerInv.addItem(item);
                if (!leftover.isEmpty()) {
                    for (ItemStack leftoverItem : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftoverItem);
                        itemsDropped = true;
                    }
                }
            }
        }

        if (itemsDropped) {
            player.sendMessage(lang.tr("gui.delivery.items-dropped"));
        }
    }

    private String prettify(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String part : s.toLowerCase().split("_")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public void open() {
        gui.open(player);
    }
}
