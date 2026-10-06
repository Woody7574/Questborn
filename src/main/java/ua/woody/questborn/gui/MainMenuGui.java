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
import ua.woody.questborn.config.TopConfig;
import ua.woody.questborn.lang.LanguageManager;
import ua.woody.questborn.model.EngineType;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.managers.QuestManager;

import java.util.ArrayList;
import java.util.List;

public class MainMenuGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final QuestManager questManager;
    private final LanguageManager lang;
    private final TopConfig topConfig;
    private final List<QuestTypeConfig> customTypes;
    private final ItemStack npcIcon;
    private final String npcId;
    private final Gui gui;
    private final GuiLayout layout;

    private final String returnDialogueId;
    private final String returnNodeId;

    public MainMenuGui(QuestbornPlugin plugin, Player player) {
        this(plugin, player, null, null, null, null);
    }

    public MainMenuGui(QuestbornPlugin plugin, Player player, List<QuestTypeConfig> customTypes) {
        this(plugin, player, customTypes, null, null, null);
    }

    public MainMenuGui(QuestbornPlugin plugin, Player player, List<QuestTypeConfig> customTypes,
            ItemStack npcIcon) {
        this(plugin, player, customTypes, npcIcon, null, null);
    }

    public MainMenuGui(QuestbornPlugin plugin, Player player, List<QuestTypeConfig> customTypes,
            ItemStack npcIcon, String npcId) {
        this(plugin, player, customTypes, npcIcon, npcId, null);
    }

    public MainMenuGui(QuestbornPlugin plugin, Player player, List<QuestTypeConfig> customTypes,
            ItemStack npcIcon, String npcId, String customTitle) {
        this(plugin, player, customTypes, npcIcon, npcId, customTitle, null, null);
    }

    public MainMenuGui(QuestbornPlugin plugin, Player player, List<QuestTypeConfig> customTypes,
            ItemStack npcIcon, String npcId, String customTitle, String returnDialogueId, String returnNodeId) {
        this.plugin = plugin;
        this.player = player;
        this.questManager = plugin.getQuestManager();
        this.lang = plugin.getLanguage();
        this.topConfig = plugin.getTopConfig();
        this.customTypes = customTypes;
        this.npcIcon = npcIcon;
        this.npcId = npcId;
        this.returnDialogueId = returnDialogueId;
        this.returnNodeId = returnNodeId;

        this.layout = plugin.getMenuConfig().getLayout("main");

        String title = customTitle != null ? customTitle : getMenuTitle(plugin);

        this.gui = Gui.gui()
                .title(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(title))
                .rows(layout != null ? layout.getSize() / 9 : 6)
                .disableAllInteractions()
                .create();

        build();
    }

    private static String getMenuTitle(QuestbornPlugin plugin) {
        GuiLayout layout = plugin.getMenuConfig().getLayout("main");
        if (layout != null && layout.getProperty("title") != null) {
            String layoutTitle = layout.getProperty("title").toString();
            if (layoutTitle.contains("&") || layoutTitle.contains("<#") || layoutTitle.contains("§")) {
                return org.bukkit.ChatColor.translateAlternateColorCodes('&', layoutTitle);
            } else {
                return org.bukkit.ChatColor.translateAlternateColorCodes('&', "<#1c1c1c>" + layoutTitle);
            }
        }
        return "Quests";
    }

    private void build() {
        GuiUtils.fillLayout(plugin, gui, layout);

        List<QuestTypeConfig> typesToShow;

        if (customTypes != null) {
            typesToShow = customTypes;
        } else {
            typesToShow = new ArrayList<>();
            List<QuestTypeConfig> enabledTypes = questManager.getQuestTypeManager().getEnabledTypes();

            java.util.Set<String> linkedTypes = new java.util.HashSet<>();
            if (plugin.getGuiConfig().isHideNpcBoundQuests() && plugin.getNpcManager() != null && plugin.getNpcIntegrationManager().getProvider() != null) {
                for (ua.woody.questborn.model.NpcConfig npc : plugin.getNpcManager().getAll()) {
                    for (String typeId : npc.getStartsTypes()) {
                        linkedTypes.add(typeId.toLowerCase());
                    }
                    for (String typeId : npc.getFinishesTypes()) {
                        linkedTypes.add(typeId.toLowerCase());
                    }
                }
            }

            for (QuestTypeConfig typeConfig : enabledTypes) {
                boolean hide = false;
                if (plugin.getGuiConfig().isHideNpcBoundQuests() && plugin.getNpcIntegrationManager().getProvider() != null) {
                    if (linkedTypes.contains(typeConfig.getId().toLowerCase())) {
                        hide = true;
                    }
                }
                if (!hide) {
                    typesToShow.add(typeConfig);
                }
            }
        }

        List<Integer> layoutSlots = layout != null ? layout.getSlots("types") : null;
        if (layoutSlots == null || layoutSlots.isEmpty()) {
            layoutSlots = new ArrayList<>();
            int rowsCount = layout != null ? layout.getSize() / 9 : 6;
            for (int r = 1; r < rowsCount - 1; r++) {
                for (int c = 1; c < 8; c++) {
                    layoutSlots.add(r * 9 + c);
                }
            }
        }
        boolean useLayoutSlots = true;
        boolean isNpcMode = customTypes != null;
        boolean centerItems = plugin.getGuiConfig().isCenterNpcTypes();

        if (isNpcMode && useLayoutSlots && !typesToShow.isEmpty() && centerItems) {
            java.util.Map<Integer, List<Integer>> rowsMap = new java.util.TreeMap<>();
            for (int slot : layoutSlots) {
                rowsMap.computeIfAbsent(slot / 9, k -> new ArrayList<>()).add(slot);
            }
            List<List<Integer>> rows = new ArrayList<>(rowsMap.values());

            int maxCols = rows.stream().mapToInt(List::size).max().orElse(1);

            List<List<QuestTypeConfig>> typeChunks = new ArrayList<>();
            for (int i = 0; i < typesToShow.size(); i += maxCols) {
                typeChunks.add(typesToShow.subList(i, Math.min(i + maxCols, typesToShow.size())));
            }

            int startRow = Math.max(0, (rows.size() - typeChunks.size()) / 2);

            for (int i = 0; i < typeChunks.size(); i++) {
                if (startRow + i >= rows.size()) break;
                List<Integer> rowSlots = rows.get(startRow + i);
                List<QuestTypeConfig> chunk = typeChunks.get(i);

                int startCol = Math.max(0, (rowSlots.size() - chunk.size()) / 2);
                for (int j = 0; j < chunk.size(); j++) {
                    if (startCol + j >= rowSlots.size()) break;
                    int slot = rowSlots.get(startCol + j);
                    addTypeButton(chunk.get(j), slot);
                }
            }
        } else {
            int index = 0;
            for (QuestTypeConfig typeConfig : typesToShow) {
                int slot;
                if (useLayoutSlots && ((isNpcMode && centerItems) || typeConfig.getGuiSlot() < 0)) {
                    if (index >= layoutSlots.size())
                        break;
                    slot = layoutSlots.get(index++);
                } else {
                    slot = typeConfig.getGuiSlot();
                }
                addTypeButton(typeConfig, slot);
            }
        }

        if (customTypes == null && topConfig.isEnabled()) {
            addTopButton();
        }

        addCloseButton();
    }

    private void addTypeButton(QuestTypeConfig typeConfig, int slot) {
        if (slot < 0 || slot >= gui.getRows() * 9) {
            plugin.getLogger().warning("Invalid GUI slot for type: " + typeConfig.getId() + " (slot: " + slot + ")");
            return;
        }

        boolean isNpcMode = customTypes != null;
        boolean isUnlocked = questManager.getQuestTypeManager().isTypeUnlocked(player, typeConfig);

        ItemStack item;

        if (isNpcMode) {
            if (typeConfig.getEngine() == EngineType.CHAIN && !isUnlocked) {
                GuiItemConfig lockedConfig = (layout != null) ? layout.getItem("locked") : null;
                if (lockedConfig != null) {
                    item = lockedConfig.createItemStack(plugin);
                } else {
                    item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                }
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(GuiUtils.getButtonName(layout, "locked", "gui.type.locked.title"));
                    meta.setLore(GuiUtils.getButtonLore(layout, "locked", "gui.type.locked.lore"));
                    item.setItemMeta(meta);
                }

                GuiItem guiItem = new dev.triumphteam.gui.guis.GuiItem(item, event -> {
                    GuiUtils.playClickSound(plugin, player, npcId);
                    player.sendMessage(lang.tr("gui.type.locked-message"));
                });
                gui.setItem(slot, guiItem);
                return;
            }
        }

        item = null;
        if (typeConfig.getCraftEngineId() != null && plugin.getCraftEngineIntegration() != null && plugin.getCraftEngineIntegration().isEnabled()) {
            ItemStack ceItem = plugin.getCraftEngineIntegration().getCustomItem(typeConfig.getCraftEngineId());
            if (ceItem != null) item = ceItem.clone();
        } else if (typeConfig.getItemsAdderId() != null && plugin.getItemsAdderIntegration() != null && plugin.getItemsAdderIntegration().isEnabled()) {
            ItemStack iaItem = plugin.getItemsAdderIntegration().getCustomItem(typeConfig.getItemsAdderId());
            if (iaItem != null) item = iaItem.clone();
        }

        if (item == null) {
            item = new ItemStack(typeConfig.getMaterial());
        }

        boolean usedLockedConfig = false;

        if (!isNpcMode && !isUnlocked) {
             GuiItemConfig lockedConfig = (layout != null) ? layout.getItem("locked") : null;
             if (lockedConfig != null) {
                 item = lockedConfig.createItemStack(plugin);
                 usedLockedConfig = true;
             }
        }

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (!usedLockedConfig && typeConfig.getCustomModelData() != null) {
                meta.setCustomModelData(typeConfig.getCustomModelData());
            }

            item.setItemMeta(meta);
            if (!usedLockedConfig) {
                GuiUtils.applyBaseHead(item, typeConfig.getBaseHead());
                meta = item.getItemMeta();
            }

            meta.setDisplayName(lang.color(typeConfig.getDisplayName()));

            List<String> lore = new ArrayList<>();

            if (!isNpcMode && !isUnlocked) {
                GuiItemConfig lockedConfig = (layout != null) ? layout.getItem("locked") : null;

                if (lockedConfig != null) {
                    ItemStack lockedItem = lockedConfig.createItemStack(plugin);
                    ItemMeta lockedMeta = lockedItem.getItemMeta();
                    if (lockedMeta != null && lockedMeta.hasLore()) {
                        List<String> lockedLore = lockedMeta.getLore();
                        if (typeConfig.getRequirementsLore() != null && !typeConfig.getRequirementsLore().isEmpty()) {
                            for (String line : lockedLore) {
                                if (line.contains("{requirements}")) {
                                    lore.addAll(typeConfig.getRequirementsLore());
                                } else {
                                    lore.add(line);
                                }
                            }
                        } else {
                            for (String line : lockedLore) {
                                if (!line.contains("{requirements}")) {
                                    lore.add(line);
                                }
                            }
                        }
                    }
                }
            } else {
                List<String> customLore = typeConfig.getLore();

                if (customLore != null && !customLore.isEmpty()) {
                    java.util.Collection<ua.woody.questborn.model.QuestDefinition> typeQuests = questManager.getByType(typeConfig);
                    java.util.Collection<ua.woody.questborn.model.QuestDefinition> questsToCheck = new java.util.ArrayList<>();
                    if (typeConfig.getEngine() == EngineType.ROTATION) {
                        questsToCheck = questManager.getOrAssignRotationQuests(player, typeConfig);
                    } else {
                        java.util.Set<String> npcBoundQuests = new java.util.HashSet<>();
                        if (plugin.getGuiConfig().isHideNpcBoundQuests() && plugin.getNpcManager() != null && plugin.getNpcIntegrationManager().getProvider() != null) {
                            for (ua.woody.questborn.model.NpcConfig npc : plugin.getNpcManager().getAll()) {
                                npcBoundQuests.addAll(npc.getStartsQuests());
                            }
                        }
                        for (ua.woody.questborn.model.QuestDefinition q : typeQuests) {
                            if (npcBoundQuests.contains(q.getId())) continue;
                            questsToCheck.add(q);
                        }
                    }

                    int totalQuests = questsToCheck.size();
                    int availableQuests = 0;
                    int cooldownQuests = 0;
                    long now = System.currentTimeMillis();

                    ua.woody.questborn.model.PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());

                    for (ua.woody.questborn.model.QuestDefinition q : questsToCheck) {
                        if (questManager.isQuestAvailable(player, q, data)) {
                            availableQuests++;
                        }

                        if (typeConfig.getEngine() == EngineType.ROTATION || typeConfig.getEngine() == EngineType.CHAIN) {
                            if (data.isQuestCompleted(q.getId())) {
                                cooldownQuests++;
                            }
                        } else {
                            boolean isCompleted = data.isQuestCompleted(q.getId());
                            long cooldownUntil = data.getQuestCooldownUntil(q.getId());
                            if ((isCompleted && typeConfig.getCooldownSeconds() <= 0) || (cooldownUntil > now)) {
                                cooldownQuests++;
                            }
                        }
                    }

                    for (String loreLine : customLore) {
                        String processedLine = loreLine
                                .replace("{count}", String.valueOf(totalQuests))
                                .replace("{total}", String.valueOf(totalQuests))
                                .replace("{available}", String.valueOf(availableQuests))
                                .replace("{cooldown}", String.valueOf(cooldownQuests))
                                .replace("{completed}", String.valueOf(cooldownQuests));
                        lore.add(lang.color(processedLine));
                    }
                }
            }

            meta.setLore(lore);
            GuiUtils.applyAllItemFlags(meta);

            item.setItemMeta(meta);
        }

        GuiItem guiItem;
        if (!isNpcMode && !isUnlocked) {
            guiItem = new dev.triumphteam.gui.guis.GuiItem(item, event -> {
                GuiUtils.playClickSound(plugin, player, npcId);
                player.sendMessage(lang.tr("gui.type.locked-message"));
            });
        } else {
            guiItem = new dev.triumphteam.gui.guis.GuiItem(item, event -> {
                GuiUtils.playClickSound(plugin, player, npcId);
                plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> new QuestListGui(plugin, player, typeConfig, 0, false, null, npcIcon, npcId, returnDialogueId, returnNodeId, true).open());
            });
        }

        gui.setItem(slot, guiItem);
    }

    private void addCloseButton() {
        if (returnDialogueId != null && returnNodeId != null) {
            int slot = 40;
            if (layout != null && layout.getItem("back-to-dialogue") != null && layout.getItem("back-to-dialogue").getSlot() >= 0) {
                slot = layout.getItem("back-to-dialogue").getSlot();
            } else if (layout != null && layout.getItem("close") != null && layout.getItem("close").getSlot() >= 0) {
                slot = layout.getItem("close").getSlot();
            }
            if (slot < 0 || slot >= gui.getRows() * 9) return;
            ItemStack item = ua.woody.questborn.gui.GuiUtils.createActionButton(plugin, layout, "back-to-dialogue", Material.BARRIER, "gui.button.back-to-dialogue");
            if (item.getType() == Material.BARRIER && item.getItemMeta() != null && item.getItemMeta().getDisplayName().equals("gui.button.back-to-dialogue")) {
                item = ua.woody.questborn.gui.GuiUtils.createActionButton(plugin, layout, "close", Material.BARRIER, "gui.button.close");
            }
            gui.setItem(slot, new dev.triumphteam.gui.guis.GuiItem(item, event -> {
                ua.woody.questborn.gui.GuiUtils.playClickSound(plugin, player, npcId);
                plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> {
                    new DialogueGui(plugin, player, returnDialogueId, returnNodeId, npcId, true).open();
                });
            }));
        }
    }

    private void addTopButton() {
        if (layout == null) return;

        GuiItemConfig itemConfig = layout.getItem("top");
        if (itemConfig == null) return;

        int slot = itemConfig.getSlot();
        if (slot < 0 || slot >= gui.getRows() * 9) return;

        ItemStack topItem = itemConfig.createItemStack(plugin);
        ItemMeta meta = topItem.getItemMeta();
        if (meta != null) {
            GuiUtils.applyAllItemFlags(meta);
            topItem.setItemMeta(meta);
        }

        GuiItem guiItem = new dev.triumphteam.gui.guis.GuiItem(topItem, event -> {
            GuiUtils.playClickSound(plugin, player, npcId);
            plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> new TopGui(plugin, player).open());
        });

        gui.setItem(slot, guiItem);
    }

    public ItemStack getNpcIcon() {
        return npcIcon;
    }

    public String getNpcId() {
        return npcId;
    }

    public void open() {
        gui.open(player);
    }
}
