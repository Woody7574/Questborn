package ua.woody.questborn.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.config.GuiItemConfig;
import ua.woody.questborn.config.GuiLayout;
import ua.woody.questborn.lang.ColorFormatter;
import ua.woody.questborn.lang.LanguageManager;
import ua.woody.questborn.managers.QuestManager;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestItem;
import ua.woody.questborn.model.QuestStage;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.util.QuestDisplayBuilder;
import ua.woody.questborn.util.ItemDisplayUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuestDetailsGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final QuestDefinition quest;
    private final LanguageManager lang;
    private final boolean simpleView;
    private final ItemStack npcIcon;
    private final String npcId;

    private final String returnDialogueId;
    private final String returnNodeId;
    private final boolean confirmCancel;
    private final boolean fromMainMenu;
    private final Gui gui;
    private final GuiLayout layout;
    private int returnPage = 1;

    public void setReturnPage(int page) {
        this.returnPage = page;
    }

    public QuestDetailsGui(QuestbornPlugin plugin, Player player, QuestDefinition quest) {
        this(plugin, player, quest, false, null, null, null, null, false, false);
    }

    public QuestDetailsGui(QuestbornPlugin plugin, Player player, QuestDefinition quest, boolean simpleView) {
        this(plugin, player, quest, simpleView, null, null, null, null, false, false);
    }

    public QuestDetailsGui(QuestbornPlugin plugin, Player player, QuestDefinition quest, boolean simpleView, ItemStack npcIcon) {
        this(plugin, player, quest, simpleView, npcIcon, null, null, null, false, false);
    }

    public QuestDetailsGui(QuestbornPlugin plugin, Player player, QuestDefinition quest, boolean simpleView, ItemStack npcIcon, String npcId) {
        this(plugin, player, quest, simpleView, npcIcon, npcId, null, null, false, false);
    }

    public QuestDetailsGui(QuestbornPlugin plugin, Player player, QuestDefinition quest, boolean simpleView, ItemStack npcIcon, String npcId, String returnDialogueId, String returnNodeId) {
        this(plugin, player, quest, simpleView, npcIcon, npcId, returnDialogueId, returnNodeId, false, false);
    }

    public QuestDetailsGui(QuestbornPlugin plugin, Player player, QuestDefinition quest, boolean simpleView, ItemStack npcIcon, String npcId, String returnDialogueId, String returnNodeId, boolean confirmCancel) {
        this(plugin, player, quest, simpleView, npcIcon, npcId, returnDialogueId, returnNodeId, confirmCancel, false);
    }

    public QuestDetailsGui(QuestbornPlugin plugin, Player player, QuestDefinition quest, boolean simpleView, ItemStack npcIcon, String npcId, String returnDialogueId, String returnNodeId, boolean confirmCancel, boolean fromMainMenu) {
        this.plugin = plugin;
        this.player = player;
        this.quest = quest;
        this.lang = plugin.getLanguage();
        this.simpleView = simpleView;
        this.npcIcon = npcIcon;
        this.npcId = npcId;
        this.returnDialogueId = returnDialogueId;
        this.returnNodeId = returnNodeId;
        this.confirmCancel = confirmCancel;
        this.fromMainMenu = fromMainMenu;
        this.layout = plugin.getMenuConfig().getLayout("quest-details");

        String title = calculateGuiTitle(plugin, quest, npcId);

        this.gui = Gui.gui()
                .title(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(title))
                .rows(layout != null ? layout.getSize() / 9 : 6)
                .disableAllInteractions()
                .create();

        build();
    }

    private static String calculateGuiTitle(QuestbornPlugin plugin, QuestDefinition quest, String npcId) {
        QuestTypeConfig typeConfig = plugin.getQuestManager().getQuestTypeManager().getType(quest.getTypeId());
        String questName = quest.getDisplayName();
        String typeName = typeConfig != null ? typeConfig.getDisplayName() : "";

        if (typeConfig != null && typeConfig.getGuiQuestTitle() != null) {
            String titleTemplate = typeConfig.getGuiQuestTitle();
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
                String npcName = plugin.getNpcManager().getDisplayName(npcId);
                String strippedNpcName = ColorFormatter.stripColors(npcName);
                String npcTitle = npcConfig.getGuiTitle().replace("{npc}", strippedNpcName);
                String titleWithPlaceholders = npcTitle.replace("{quest}", questName).replace("{type}", typeConfig != null ? ColorFormatter.stripColors(typeConfig.getDisplayName()) : "");
                return org.bukkit.ChatColor.translateAlternateColorCodes('&', titleWithPlaceholders);
            }
        }

        GuiLayout layout = plugin.getMenuConfig().getLayout("quest-details");
        if (layout != null && layout.getProperty("title") != null) {
            String layoutTitle = layout.getProperty("title").toString();
            if (layoutTitle.contains("&") || layoutTitle.contains("<#") || layoutTitle.contains("§")) {
                String titleWithPlaceholders = layoutTitle.replace("{quest}", questName).replace("{type}", typeConfig != null ? ColorFormatter.stripColors(typeConfig.getDisplayName()) : "");
                return org.bukkit.ChatColor.translateAlternateColorCodes('&', titleWithPlaceholders);
            } else {
                String titleWithPlaceholders = layoutTitle.replace("{quest}", questName).replace("{type}", typeConfig != null ? typeConfig.getDisplayName() : "");
                return org.bukkit.ChatColor.translateAlternateColorCodes('&', titleWithPlaceholders);
            }
        }

        return questName;
    }

    private void build() {
        GuiUtils.fillLayout(plugin, gui, layout);

        int iconSlot = 4;
        int objectiveSlot = 20;
        int rewardsSlot = 22;
        int transferSlot = 24;
        int backSlot = 38;
        int actionSlot = 42;

        if (layout != null) {
            if (layout.getItem("icon") != null && layout.getItem("icon").getSlot() >= 0) iconSlot = layout.getItem("icon").getSlot();
            if (layout.getItem("objective") != null && layout.getItem("objective").getSlot() >= 0) objectiveSlot = layout.getItem("objective").getSlot();
            if (layout.getItem("rewards") != null && layout.getItem("rewards").getSlot() >= 0) rewardsSlot = layout.getItem("rewards").getSlot();
            if (layout.getItem("transfer") != null && layout.getItem("transfer").getSlot() >= 0) transferSlot = layout.getItem("transfer").getSlot();
            if (layout.getItem("back") != null && layout.getItem("back").getSlot() >= 0) backSlot = layout.getItem("back").getSlot();
            if (layout.getItem("action") != null && layout.getItem("action").getSlot() >= 0) actionSlot = layout.getItem("action").getSlot();
        }

        PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());
        String activeId = data.getTrackedQuestId();
        boolean isActive = data.hasActiveQuest(quest.getId());
        boolean isTracked = quest.getId().equals(activeId);
        boolean pendingReward = data.hasPendingReward(quest.getId());

        int currentStageNum = 1;
        if (isActive) {
            currentStageNum = data.getQuestData(quest.getId()).getCurrentStage();
        }

        QuestTypeConfig typeC = plugin.getQuestManager().getQuestTypeManager().getType(quest.getTypeId());
        boolean isOneTime = typeC != null && typeC.getCooldownSeconds() == 0;
        boolean isCompletedOneTime = isOneTime && data.isQuestCompleted(quest.getId());
        boolean onCooldown = data.getQuestCooldownUntil(quest.getId()) > System.currentTimeMillis();
        boolean show100 = isCompletedOneTime || onCooldown;

        String stageDisplay = String.valueOf(currentStageNum);
        if (show100) {
            stageDisplay = String.valueOf(quest.getStageCount());
            currentStageNum = quest.getStageCount();
        }

        QuestStage currentStage = quest.getStage(currentStageNum);
        if (currentStage == null && isActive && data.getQuestData(quest.getId()).isStageCompleted(currentStageNum)) {
            currentStage = quest.getStage(quest.getStageCount());
        } else if (currentStage == null) {
            currentStage = quest.getStage(1);
        }

        boolean isGlobal = typeC != null && typeC.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL;
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

        boolean objectiveCompleted = isActive && data.getQuestData(quest.getId()).isStageCompleted(currentStageNum);
        boolean itemsTransferred = isActive && data.getQuestData(quest.getId()).isItemsTransferred();
        if (isGlobal && remainingGlobal <= 0) {
            itemsTransferred = true;
        }

        if (iconSlot >= 0 && iconSlot < gui.getRows() * 9) {
            ItemStack icon = null;
            if (quest.getIconCraftEngineId() != null && plugin.getCraftEngineIntegration() != null && plugin.getCraftEngineIntegration().isEnabled()) {
                ItemStack ce = plugin.getCraftEngineIntegration().getCustomItem(quest.getIconCraftEngineId());
                if (ce != null) icon = ce.clone();
            } else if (quest.getIconItemsAdderId() != null && plugin.getItemsAdderIntegration() != null && plugin.getItemsAdderIntegration().isEnabled()) {
                ItemStack ia = plugin.getItemsAdderIntegration().getCustomItem(quest.getIconItemsAdderId());
                if (ia != null) icon = ia.clone();
            }
            if (icon == null) {
                Material mat = quest.getIconMaterial();
                icon = new ItemStack(mat != null ? mat : Material.WRITABLE_BOOK);
            }
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(quest.getDisplayName());
                List<String> description = new ArrayList<>();
                for (String line : quest.getDescription()) {
                    description.add(lang.color(line));
                }
                meta.setLore(description);
                if (quest.getIconCustomModelData() != null) {
                    meta.setCustomModelData(quest.getIconCustomModelData());
                }
                icon.setItemMeta(meta);
                GuiUtils.applyBaseHead(icon, quest.getIconBaseHead());
                meta = icon.getItemMeta();
                GuiUtils.applyAllItemFlags(meta);
                icon.setItemMeta(meta);
            }
            gui.setItem(iconSlot, new dev.triumphteam.gui.guis.GuiItem(icon, event -> event.setCancelled(true)));
        }

        List<String> dynamicDetails = new ArrayList<>();

        if (currentStage != null) {
            if (currentStage.isObjective()) {
                dynamicDetails.addAll(QuestDisplayBuilder.build(currentStage.getObjective(), lang));

                if (!isGlobal && currentStage.getObjective().getAmount() > 0) {
                    int target = currentStage.getObjective().getAmount();
                    int current;
                    if (show100) {
                        current = target;
                    } else if (isActive) {
                        current = data.getQuestData(quest.getId()).getStageProgress();
                    } else {
                        current = 0;
                    }
                    String progressBar = plugin.getGuiConfig().getProgressBar(current, target);
                    if (!progressBar.isEmpty()) {
                        dynamicDetails.add(" ");
                        dynamicDetails.add(lang.color("<#ffaa00>" + lang.tr("gui.quest_details.info.progress.title")));
                        int percent = 0;
                        if (target > 0) percent = Math.min(100, (int) ((current / (double) target) * 100));
                        dynamicDetails.add(lang.color(lang.tr("gui.quest_details.info.progress.bar", Map.of("bar", progressBar, "percent", String.valueOf(percent)))));
                        dynamicDetails.add(lang.color(lang.tr("gui.quest_details.info.progress.value", Map.of("current", String.valueOf(current), "target", String.valueOf(target)))));
                    }
                }
            } else if (currentStage.isRequiredMaterials()) {
                if (currentStage.getRequiredItems() != null && !currentStage.getRequiredItems().isEmpty()) {
                    dynamicDetails.add(lang.color(lang.tr("quests.objectives.header")));
                    dynamicDetails.add(lang.color(lang.tr("gui.quest_details.info.required-items")));
                    for (Map.Entry<QuestItem, Integer> entry : currentStage.getRequiredItems().entrySet()) {
                        QuestItem qItem = entry.getKey();
                        int amount = entry.getValue();
                        if (isGlobal && quest.getPersonalLimit() > 0) {
                            amount = quest.getPersonalLimit();
                        }
                        String localizedName;
                        if (qItem.isItemsAdderItem()) {
                            localizedName = ItemDisplayUtil.getItemsAdderDisplayName(qItem.getItemsAdderId(), lang);
                        } else {
                            localizedName = ItemDisplayUtil.findLocalization(qItem.getMaterial(), lang, false);
                        }
                        dynamicDetails.add(lang.color(" <#a8a8a8>• <#ffffff>" + localizedName + " <#a8a8a8>x<#ffffff>" + amount));
                    }
                }

                int target = 0;
                if (currentStage.getRequiredItems() != null) {
                    for (int amount : currentStage.getRequiredItems().values()) {
                        target += amount;
                    }
                }
                if (target <= 0) target = 1;

                if (!isGlobal) {
                    int current = (show100 || itemsTransferred) ? target : 0;
                    String progressBar = plugin.getGuiConfig().getProgressBar(current, target);
                    if (!progressBar.isEmpty()) {
                        dynamicDetails.add(" ");
                        dynamicDetails.add(lang.color("<#ffaa00>" + lang.tr("gui.quest_details.info.progress.title")));
                        int percent = (current * 100) / target;
                        dynamicDetails.add(lang.color(lang.tr("gui.quest_details.info.progress.bar", Map.of("bar", progressBar, "percent", String.valueOf(percent)))));
                        dynamicDetails.add(lang.color(lang.tr("gui.quest_details.info.progress.value", Map.of("current", String.valueOf(current), "target", String.valueOf(target)))));
                    }
                }
            }
        }

        if (typeC != null && typeC.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL) {
            ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(quest.getId());

            if (isActive && gp != null && gp.isCompleted()) {
                data.removeActiveQuest(quest.getId());
                plugin.getPlayerDataStore().markDirty(player.getUniqueId());
                isActive = false;

                plugin.getQuestManager().getActionBarManager().sendForPlayer(player);
                plugin.getQuestManager().getBossBarManager().updateBar(player);
                plugin.getQuestManager().getScoreboardManager().updateBoard(player);
                plugin.getQuestManager().updateActiveQuestPlayer(player);
            }

            int maxP = quest.getMaxParticipants();
            int personalLimit = quest.getPersonalLimit();
            int myContribution = gp.getContribution(player.getUniqueId());

            dynamicDetails.add(" ");
            dynamicDetails.add(lang.color(lang.tr("gui.quest_details.info.your-contribution")));
            if (personalLimit > 0) {
                String pBar = plugin.getGuiConfig().getProgressBar(myContribution, personalLimit);
                int pPercent = (int) ((myContribution / (double) personalLimit) * 100);
                dynamicDetails.add(lang.color(lang.tr("gui.quest_details.info.progress.bar", Map.of("bar", pBar, "percent", String.valueOf(pPercent)))));
                dynamicDetails.add(lang.color(lang.tr("gui.quest_details.info.progress.value", Map.of("current", String.valueOf(myContribution), "target", String.valueOf(personalLimit)))));
            } else {
                dynamicDetails.add(lang.color("<#ffffff>" + myContribution));
            }
        }

        GuiItemConfig objConfig = (layout != null) ? layout.getItem("objective") : null;
        ItemStack objectiveItem;
        if (objConfig != null) {
            objectiveItem = objConfig.createItemStack(plugin);
            ItemMeta meta = objectiveItem.getItemMeta();
            if (meta != null && meta.hasLore()) {
                List<String> finalLore = new ArrayList<>();
                for (String line : meta.getLore()) {
                    String processedLine = line.replace("{stage}", stageDisplay).replace("{total_stages}", String.valueOf(quest.getStageCount()));
                    if (processedLine.contains("{objective_details}")) {
                        finalLore.addAll(dynamicDetails);
                    } else {
                        finalLore.add(processedLine);
                    }
                }
                meta.setLore(finalLore);
                objectiveItem.setItemMeta(meta);
            }
        } else {
            Material mat = Material.getMaterial("SPYGLASS");
            if (mat == null) mat = Material.WRITABLE_BOOK;
            objectiveItem = new ItemStack(mat);
            ItemMeta meta = objectiveItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(lang.color(lang.tr("gui.quest_details.objective_title")));
                meta.setLore(dynamicDetails);
                objectiveItem.setItemMeta(meta);
            }
        }

        if (objectiveSlot >= 0 && objectiveSlot < gui.getRows() * 9) {
            gui.setItem(objectiveSlot, new dev.triumphteam.gui.guis.GuiItem(objectiveItem, event -> event.setCancelled(true)));
        }

        List<String> rewardDescriptions = quest.getRewardsDescription();
        List<String> dynamicRewards = new ArrayList<>();
        if (rewardDescriptions != null) {
            for (String r : rewardDescriptions) {
                dynamicRewards.add(lang.color(r));
            }
        }

        GuiItemConfig rewardsConfig = (layout != null) ? layout.getItem("rewards") : null;
        ItemStack rewardItem;
        if (rewardsConfig != null) {
            rewardItem = rewardsConfig.createItemStack(plugin);
            ItemMeta meta = rewardItem.getItemMeta();
            if (meta != null && meta.hasLore()) {
                List<String> finalLore = new ArrayList<>();
                for (String line : meta.getLore()) {
                    if (line.contains("{reward}")) {
                        finalLore.addAll(dynamicRewards);
                    } else {
                        finalLore.add(line);
                    }
                }
                meta.setLore(finalLore);
                rewardItem.setItemMeta(meta);
            }
        } else {
            rewardItem = new ItemStack(Material.CHEST_MINECART);
            ItemMeta rewardMeta = rewardItem.getItemMeta();
            if (rewardMeta != null) {
                rewardMeta.setDisplayName(lang.color(lang.tr("gui.quest_details.rewards_title")));
                rewardMeta.setLore(dynamicRewards);
                rewardItem.setItemMeta(rewardMeta);
            }
        }

        if (rewardsSlot >= 0 && rewardsSlot < gui.getRows() * 9) {
            gui.setItem(rewardsSlot, new dev.triumphteam.gui.guis.GuiItem(rewardItem, event -> event.setCancelled(true)));
        }

        ItemStack transferItem;
        boolean stageRequiresTransfer = isActive && currentStage != null && currentStage.isRequiredMaterials() && !itemsTransferred;

        if (stageRequiresTransfer) {
            transferItem = GuiUtils.createActionButton(plugin, layout, "transfer", Material.MINECART, "gui.quest_details.info.required-items");
            ItemMeta trMeta = transferItem.getItemMeta();
            if (trMeta != null) {
                List<String> reqLore = new ArrayList<>();
                reqLore.add(lang.color(lang.tr("gui.quest_details.info.required-items")));

                for (Map.Entry<QuestItem, Integer> entry : currentStage.getRequiredItems().entrySet()) {
                    QuestItem qItem = entry.getKey();
                    int amount = entry.getValue();
                    if (isGlobal && quest.getPersonalLimit() > 0) {
                        amount = quest.getPersonalLimit();
                    }
                    String localizedName;
                    if (qItem.isItemsAdderItem()) {
                        localizedName = ItemDisplayUtil.getItemsAdderDisplayName(qItem.getItemsAdderId(), lang);
                    } else {
                        localizedName = ItemDisplayUtil.findLocalization(qItem.getMaterial(), lang, false);
                    }
                    reqLore.add(lang.color(" <#a8a8a8>• <#ffffff>" + localizedName + " <#a8a8a8>x<#ffffff>" + amount));
                }

                if (itemsTransferred) {
                    reqLore.add(" ");
                    reqLore.add(lang.tr("gui.quest_details.info.items-transferred"));
                } else {
                    reqLore.add(" ");
                    reqLore.add(lang.tr("gui.quest_details.info.awaiting-transfer"));
                }

                String customName = GuiUtils.getButtonName(layout, "transfer-required", "gui.quest_details.info.required-items");
                if (customName != null) {
                    trMeta.setDisplayName(customName);
                }

                if (!reqLore.isEmpty()) {
                    if (reqLore.size() > 1) {
                        trMeta.setLore(reqLore.subList(1, reqLore.size()));
                    } else {
                        trMeta.setLore(reqLore);
                    }
                }
                transferItem.setItemMeta(trMeta);
            }
        } else {
            transferItem = GuiUtils.createActionButton(plugin, layout, "transfer-none", Material.FURNACE_MINECART, "gui.details.transfer.transfer-no-items.title");
        }

        if (transferSlot >= 0 && transferSlot < gui.getRows() * 9) {
            GuiItem trGuiItem;
            if (stageRequiresTransfer && !itemsTransferred) {
                trGuiItem = new dev.triumphteam.gui.guis.GuiItem(transferItem, event -> {
                    GuiUtils.playClickSound(plugin, player, npcId);
                    plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> {
                        QuestTransferGui transferGui = new QuestTransferGui(plugin, player, quest, simpleView, npcIcon, npcId, returnDialogueId, returnNodeId);
                        transferGui.setReturnPage(returnPage);
                        transferGui.open();
                    });
                });
            } else {
                trGuiItem = new dev.triumphteam.gui.guis.GuiItem(transferItem, event -> event.setCancelled(true));
            }
            gui.setItem(transferSlot, trGuiItem);
        }

        if (!simpleView) {
            ItemStack back = GuiUtils.createActionButton(plugin, layout, "back", Material.ORANGE_DYE, "gui.details.back.title");
            if (backSlot >= 0 && backSlot < gui.getRows() * 9) {
                gui.setItem(backSlot, new dev.triumphteam.gui.guis.GuiItem(back, event -> {
                    GuiUtils.playClickSound(plugin, player, npcId);
                    plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> {
                        openBackGui();
                    });
                }));
            }
        }

        if (pendingReward) {
            boolean canClaim = plugin.getQuestManager().canClaimRewardFromNpc(player, quest, npcId);

            if (canClaim) {
                ItemStack claim = GuiUtils.createActionButton(plugin, layout, "claim-reward", Material.EMERALD, "gui.details.claim-reward");
                if (actionSlot >= 0 && actionSlot < gui.getRows() * 9) {
                    gui.setItem(actionSlot, new dev.triumphteam.gui.guis.GuiItem(claim, event -> {
                        boolean claimed = plugin.getQuestManager().claimReward(player, quest);
                        if (claimed) {
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                            plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> {
                                openBackGui();
                            });
                        }
                    }));
                }
            } else {
                ItemStack mustTalk = GuiUtils.createActionButton(plugin, layout, "claim-reward-other-npc", Material.COMPASS, "gui.details.claim-reward");
                org.bukkit.inventory.meta.ItemMeta meta = mustTalk.getItemMeta();
                if (meta != null) {
                    String targetNpcId = null;
                    if (plugin.getNpcManager() != null) {
                        for (ua.woody.questborn.model.NpcConfig nc : plugin.getNpcManager().getAll()) {
                            if (nc.getFinishesQuests().contains(quest.getId()) || nc.getFinishesTypes().contains(quest.getTypeId())) {
                                targetNpcId = nc.getNpcId();
                                break;
                            }
                        }
                        if (targetNpcId == null) {
                            for (ua.woody.questborn.model.NpcConfig nc : plugin.getNpcManager().getAll()) {
                                if (nc.getStartsQuests().contains(quest.getId()) || nc.getStartsTypes().contains(quest.getTypeId())) {
                                    targetNpcId = nc.getNpcId();
                                    break;
                                }
                            }
                        }
                    }
                    String npcName = plugin.getNpcManager() != null && targetNpcId != null ? plugin.getNpcManager().getDisplayName(targetNpcId) : targetNpcId;
                    if (npcName == null) npcName = "NPC";

                    if (layout == null || layout.getItem("claim-reward-other-npc") == null) {
                        meta.setDisplayName(lang.color(lang.tr("gui.quest_details.start_npc_title")));
                        java.util.List<String> lore = new java.util.ArrayList<>();
                        lore.add(lang.color(lang.tr("gui.quest_details.start_npc_lore1", java.util.Map.of("npc", npcName))));
                        lore.add(lang.color(lang.tr("gui.quest_details.start_npc_lore2")));
                        meta.setLore(lore);
                    } else {
                        if (meta.hasDisplayName()) {
                            meta.setDisplayName(lang.color(meta.getDisplayName().replace("{npc}", npcName)));
                        }
                        if (meta.hasLore()) {
                            java.util.List<String> lore = new java.util.ArrayList<>();
                            for (String l : meta.getLore()) {
                                lore.add(lang.color(l.replace("{npc}", npcName)));
                            }
                            meta.setLore(lore);
                        }
                    }
                    mustTalk.setItemMeta(meta);
                }
                if (actionSlot >= 0 && actionSlot < gui.getRows() * 9) {
                    gui.setItem(actionSlot, new dev.triumphteam.gui.guis.GuiItem(mustTalk, event -> {
                        event.setCancelled(true);
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }));
                }
            }
            return;
        }

        if (isActive) {
            ItemStack cancel = null;
            GuiItem cancelItem = null;

            if (isTracked) {
                if (confirmCancel) {
                    cancel = GuiUtils.createActionButton(plugin, layout, "action-confirm-cancel", Material.ORANGE_DYE, "gui.details.action.confirm-cancel.title");
                    cancelItem = new dev.triumphteam.gui.guis.GuiItem(cancel, event -> {
                        GuiUtils.playClickSound(plugin, player, npcId);
                        plugin.getQuestManager().cancelQuest(player, quest.getId());
                        plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> {
                            openBackGui();
                        });
                    });
                } else {
                    cancel = GuiUtils.createActionButton(plugin, layout, "action-cancel", Material.RED_DYE, "gui.details.action.cancel.title");
                    cancelItem = new dev.triumphteam.gui.guis.GuiItem(cancel, event -> {
                        GuiUtils.playClickSound(plugin, player, npcId);
                        reopenSelf(true);
                    });
                }
            } else {
                boolean canTrack = true;
                if (isGlobal && remainingGlobal <= 0) canTrack = false;

                if (canTrack) {
                    cancel = GuiUtils.createActionButton(plugin, layout, "action-track", Material.LIGHT_BLUE_DYE, "");
                    cancelItem = new dev.triumphteam.gui.guis.GuiItem(cancel, event -> {
                        GuiUtils.playClickSound(plugin, player, npcId);
                        plugin.getQuestManager().trackQuest(player, quest.getId());
                        reopenSelf(false);
                    });
                }
            }
            if (actionSlot >= 0 && actionSlot < gui.getRows() * 9 && cancelItem != null) {
                gui.setItem(actionSlot, cancelItem);
            }
            return;
        }

        String activeSameType = null;
        for (PlayerQuestProgress.ActiveQuestData qd : data.getActiveQuests().values()) {
            if (!data.hasPendingReward(qd.getQuestId())) {
                QuestDefinition activeDef = plugin.getQuestManager().getQuest(qd.getQuestId());
                if (activeDef != null && activeDef.getTypeId().equalsIgnoreCase(quest.getTypeId())) {
                    if (plugin.getQuestManager().getStateService().isGlobalMaxContributionReached(player, activeDef)) {
                        continue;
                    }
                    activeSameType = qd.getQuestId();
                    break;
                }
            }
        }

        if (activeSameType != null) {
            QuestManager.ActivationConflictMode mode = plugin.getQuestManager().getActivationConflictMode();
            GuiItem actionGuiItem = null;

            switch (mode) {
                case BLOCK -> {
                    ItemStack actionItem = GuiUtils.createActionButton(plugin, layout, "action-blocked", Material.GRAY_DYE, "gui.details.action.blocked.title");
                    actionGuiItem = new dev.triumphteam.gui.guis.GuiItem(actionItem, event -> event.setCancelled(true));
                }
                case REPLACE -> {
                    ItemStack actionItem = GuiUtils.createActionButton(plugin, layout, "action-replace", Material.YELLOW_DYE, "gui.details.action.replace.title");
                    actionGuiItem = new dev.triumphteam.gui.guis.GuiItem(actionItem, event -> {
                        GuiUtils.playClickSound(plugin, player, npcId);
                        boolean ok = plugin.getQuestManager().activateQuest(player, quest.getId(), false);
                        if (ok && isGlobal && !plugin.getQuestManager().getStateService().isWaitingGlobal(quest.getId())) {
                            player.closeInventory();
                        } else {
                            reopenSelf(false);
                        }
                    });
                }
                case CHANGE, CONFIRM -> {
                    String pendingId = data.getPendingQuestId();
                    if (quest.getId().equals(pendingId)) {
                        ItemStack actionItem = GuiUtils.createActionButton(plugin, layout, "action-confirm-change", Material.ORANGE_DYE, "gui.details.action.confirm-change.title");
                        actionGuiItem = new dev.triumphteam.gui.guis.GuiItem(actionItem, event -> {
                            GuiUtils.playClickSound(plugin, player, npcId);
                            boolean ok = plugin.getQuestManager().confirmQuestChange(player, quest.getId(), false);
                            if (ok && isGlobal && !plugin.getQuestManager().getStateService().isWaitingGlobal(quest.getId())) {
                                player.closeInventory();
                            } else {
                                reopenSelf(false);
                            }
                        });
                    } else {
                        ItemStack actionItem = GuiUtils.createActionButton(plugin, layout, "action-change", Material.YELLOW_DYE, "gui.details.action.change.title");
                        actionGuiItem = new dev.triumphteam.gui.guis.GuiItem(actionItem, event -> {
                            GuiUtils.playClickSound(plugin, player, npcId);
                            data.setPendingQuestId(quest.getId());
                            reopenSelf(confirmCancel);
                        });
                    }
                }
            }

            if (actionGuiItem != null && actionSlot >= 0 && actionSlot < gui.getRows() * 9) {
                gui.setItem(actionSlot, actionGuiItem);
            }
            return;
        }

        if (typeC != null && typeC.getCooldownSeconds() > 0) {
            long cooldownUntil = data.getQuestCooldownUntil(quest.getId());
            long now = System.currentTimeMillis();

            if (cooldownUntil > now) {
                ItemStack locked = GuiUtils.createActionButton(plugin, layout, "action-cooldown", Material.CLOCK, "gui.quest_details.status.cooldown");
                if (actionSlot >= 0 && actionSlot < gui.getRows() * 9) {
                    gui.setItem(actionSlot, new dev.triumphteam.gui.guis.GuiItem(locked, event -> event.setCancelled(true)));
                }
                return;
            }
        }

        if (this.npcId == null && typeC != null && !plugin.getQuestManager().getQuestTypeManager().isTypeUnlocked(player, typeC)) {
            ItemStack locked = GuiUtils.createActionButton(plugin, layout, "action-blocked", Material.BARRIER, "gui.quest_details.status.locked");
            if (actionSlot >= 0 && actionSlot < gui.getRows() * 9) {
                gui.setItem(actionSlot, new dev.triumphteam.gui.guis.GuiItem(locked, event -> event.setCancelled(true)));
            }
            return;
        }

        if (isCompletedOneTime) {
            ItemStack completed = GuiUtils.createActionButton(plugin, layout, "action-completed", Material.PURPLE_DYE, "gui.details.status.completed");
            if (actionSlot >= 0 && actionSlot < gui.getRows() * 9) {
                gui.setItem(actionSlot, new dev.triumphteam.gui.guis.GuiItem(completed, event -> event.setCancelled(true)));
            }
            return;
        }

        ItemStack start = GuiUtils.createActionButton(plugin, layout, "action-activate", Material.LIME_DYE, "gui.details.action.activate.title");
        GuiItem startGuiItem = new dev.triumphteam.gui.guis.GuiItem(start, event -> {
            GuiUtils.playClickSound(plugin, player, npcId);
            boolean ok = plugin.getQuestManager().activateQuest(player, quest.getId(), false);
            if (ok && isGlobal && !plugin.getQuestManager().getStateService().isWaitingGlobal(quest.getId())) {
                player.closeInventory();
            } else {
                reopenSelf(false);
            }
        });
        if (actionSlot >= 0 && actionSlot < gui.getRows() * 9) {
            gui.setItem(actionSlot, startGuiItem);
        }
    }
    private void reopenSelf(boolean confirmCancelOverride) {
        plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> {
            QuestDetailsGui gui = new QuestDetailsGui(plugin, player, quest, simpleView, npcIcon, npcId, returnDialogueId, returnNodeId, confirmCancelOverride, fromMainMenu);
            gui.setReturnPage(returnPage);
            gui.open();
        });
    }

    public void open() {
        gui.open(player);
    }

    public QuestDefinition getQuest() {
        return quest;
    }

    public ItemStack getNpcIcon() {
        return npcIcon;
    }

    public String getNpcId() {
        return npcId;
    }

    private void openBackGui() {
        if (simpleView) {
            player.closeInventory();
            return;
        }
        if (npcId != null && plugin.getNpcIntegrationManager() != null) {
            plugin.getNpcIntegrationManager().handleNpcClick(player, npcId, null, true, true, returnPage, returnDialogueId, returnNodeId);
            return;
        }
        QuestTypeConfig typeC = plugin.getQuestManager().getQuestTypeManager().getType(quest.getTypeId());
        new QuestListGui(plugin, player, typeC, returnPage, simpleView, null, npcIcon, npcId, returnDialogueId, returnNodeId, fromMainMenu).open();
    }
}
