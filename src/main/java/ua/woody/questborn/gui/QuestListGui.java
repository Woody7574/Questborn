package ua.woody.questborn.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.config.GuiLayout;
import ua.woody.questborn.lang.LanguageManager;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.model.EngineType;
import ua.woody.questborn.util.TimeFormatter;
import ua.woody.questborn.util.ItemsAdderFontUtil;

import java.util.*;

public class QuestListGui {
    private EngineType getEngine() {
        if (typeConfig != null) return typeConfig.getEngine();
        if (quests != null && !quests.isEmpty()) {
            QuestTypeConfig type = plugin.getQuestManager().getQuestTypeManager().getType(quests.get(0).getTypeId());
            if (type != null) return type.getEngine();
        }
        return EngineType.DEFAULT;
    }

    private final QuestbornPlugin plugin;
    private final Player player;
    private final QuestTypeConfig typeConfig;
    private final LanguageManager lang;
    private final int currentPage;
    private final boolean simpleView;
    private final List<QuestDefinition> quests;
    private final List<QuestDefinition> explicitQuests;
    private final ItemStack npcIcon;
    private final String npcId;

    private final String returnDialogueId;
    private final String returnNodeId;
    private final boolean fromMainMenu;
    private final List<Integer> questSlots;
    private final int questsPerPage;
    private final Gui gui;
    private final GuiLayout layout;

    public QuestListGui(QuestbornPlugin plugin, Player player, QuestTypeConfig typeConfig) {
        this(plugin, player, typeConfig, 0, false, null, null, null, null, null);
    }

    public QuestListGui(QuestbornPlugin plugin, Player player, QuestTypeConfig typeConfig, ItemStack npcIcon, String npcId) {
        this(plugin, player, typeConfig, 0, false, null, npcIcon, npcId, null, null);
    }

    public QuestListGui(QuestbornPlugin plugin, Player player, QuestTypeConfig typeConfig, ItemStack npcIcon) {
        this(plugin, player, typeConfig, 0, false, null, npcIcon, null, null, null);
    }

    public QuestListGui(QuestbornPlugin plugin, Player player, QuestTypeConfig typeConfig, int page) {
        this(plugin, player, typeConfig, page, false, null, null, null, null, null);
    }

    public QuestListGui(QuestbornPlugin plugin, Player player, QuestTypeConfig typeConfig, int page, boolean simpleView) {
        this(plugin, player, typeConfig, page, simpleView, null, null, null, null, null);
    }

    public QuestListGui(QuestbornPlugin plugin, Player player, QuestTypeConfig typeConfig, int page, boolean simpleView, List<QuestDefinition> explicitQuests) {
        this(plugin, player, typeConfig, page, simpleView, explicitQuests, null, null, null, null);
    }

    public QuestListGui(QuestbornPlugin plugin, Player player, QuestTypeConfig typeConfig, int page, boolean simpleView, List<QuestDefinition> explicitQuests, String npcIcon, String npcId) {
        this(plugin, player, typeConfig, page, simpleView, explicitQuests, null, npcId, null, null);
    }

    public QuestListGui(QuestbornPlugin plugin, Player player, QuestTypeConfig typeConfig, int page, boolean simpleView, List<QuestDefinition> explicitQuests, ItemStack npcIconStack, String npcId) {
        this(plugin, player, typeConfig, page, simpleView, explicitQuests, npcIconStack, npcId, null, null);
    }

    public QuestListGui(QuestbornPlugin plugin, Player player, QuestTypeConfig typeConfig, int page, boolean simpleView, List<QuestDefinition> explicitQuests, ItemStack npcIconStack, String npcId, String returnDialogueId, String returnNodeId) {
        this(plugin, player, typeConfig, page, simpleView, explicitQuests, npcIconStack, npcId, returnDialogueId, returnNodeId, false);
    }

    public QuestListGui(QuestbornPlugin plugin, Player player, QuestTypeConfig typeConfig, int page, boolean simpleView, List<QuestDefinition> explicitQuests, ItemStack npcIconStack, String npcId, String returnDialogueId, String returnNodeId, boolean fromMainMenu) {
        this.plugin = plugin;
        this.player = player;
        this.typeConfig = typeConfig;
        this.lang = plugin.getLanguage();
        this.simpleView = simpleView;
        this.explicitQuests = explicitQuests;
        this.npcIcon = npcIconStack;
        this.npcId = npcId;
        this.returnDialogueId = returnDialogueId;
        this.returnNodeId = returnNodeId;
        this.fromMainMenu = fromMainMenu;
        this.layout = plugin.getMenuConfig().getLayout("quest-list");

        if (explicitQuests != null) {
            this.quests = explicitQuests;
        } else {
            this.quests = getSortedQuests(plugin, player, typeConfig);
        }

        if (getEngine() == EngineType.ROTATION && (typeConfig != null ? typeConfig.getRotationSlots() : null) != null && !(typeConfig != null ? typeConfig.getRotationSlots() : null).isEmpty()) {
            this.questSlots = new ArrayList<>((typeConfig != null ? typeConfig.getRotationSlots() : null));
        } else if (layout != null && layout.getSlots("quests") != null && !layout.getSlots("quests").isEmpty()) {
            this.questSlots = new ArrayList<>(layout.getSlots("quests"));
        } else {
            this.questSlots = List.of(
                    10, 11, 12, 13, 14, 15, 16,
                    19, 20, 21, 22, 23, 24, 25,
                    28, 29, 30, 31, 32, 33, 34);
        }
        this.questsPerPage = this.questSlots.size();

        if (getEngine() == EngineType.CHAIN || getEngine() == EngineType.GLOBAL) {
            boolean pageExists = false;
            for (QuestDefinition q : quests) {
                Integer questPage = q.getPage();
                int effectivePage = (questPage == null) ? 0 : questPage;
                if (effectivePage == page) {
                    pageExists = true;
                    break;
                }
            }

            if (!pageExists) {
                this.currentPage = findFirstPageWithQuests();
            } else {
                this.currentPage = page;
            }
        } else {
            this.currentPage = page;
        }

        int totalPages;
        int displayPage;
        if (getEngine() == EngineType.CHAIN || getEngine() == EngineType.GLOBAL) {
            Set<Integer> pages = new HashSet<>();
            for (QuestDefinition q : quests) {
                Integer qp = q.getPage();
                pages.add(qp == null ? 0 : qp);
            }
            List<Integer> sortedPages = new ArrayList<>(pages);
            Collections.sort(sortedPages);
            totalPages = sortedPages.isEmpty() ? 1 : sortedPages.size();
            displayPage = sortedPages.indexOf(currentPage);
            if (displayPage == -1) displayPage = 0;
        } else {
            totalPages = Math.max(1, (int) Math.ceil((double) quests.size() / questsPerPage));
            displayPage = Math.min(currentPage, Math.max(0, totalPages - 1));
        }

        String title = calculateGuiTitle(plugin, typeConfig, displayPage, npcId, totalPages);

        this.gui = Gui.gui()
                .title(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(title))
                .rows(layout != null ? layout.getSize() / 9 : 6)
                .disableAllInteractions()
                .create();

        build();
    }

    private static String calculateGuiTitle(QuestbornPlugin plugin, QuestTypeConfig typeConfig, int displayPage, String npcId, int totalPages) {
        String typeName = typeConfig != null ? stripColors(typeConfig.getDisplayName()) : "";
        String pageStr = String.valueOf(displayPage + 1);
        String totalStr = String.valueOf(totalPages);

        if (typeConfig != null && typeConfig.getGuiTypeTitle() != null) {
            String typeGuiTitle = typeConfig.getGuiTypeTitle();
            if (typeGuiTitle.contains("&") || typeGuiTitle.contains("<#") || typeGuiTitle.contains("§")) {
                String title = typeGuiTitle.replace("{type}", stripColors(typeConfig.getDisplayName())).replace("{page}", pageStr).replace("{total}", totalStr);
                title = ItemsAdderFontUtil.processFontFormats(title, plugin);
                return org.bukkit.ChatColor.translateAlternateColorCodes('&', title);
            } else {
                String title = typeGuiTitle.replace("{type}", typeConfig.getDisplayName()).replace("{page}", pageStr).replace("{total}", totalStr);
                title = ItemsAdderFontUtil.processFontFormats(title, plugin);
                return org.bukkit.ChatColor.translateAlternateColorCodes('&', title);
            }
        }

        if (npcId != null && plugin.getNpcManager() != null) {
            ua.woody.questborn.model.NpcConfig npcConfig = plugin.getNpcManager().getConfigByNpcId(npcId);
            if (npcConfig != null && npcConfig.getGuiTitle() != null) {
                String npcName = plugin.getNpcManager().getDisplayName(npcId);
                String strippedNpcName = ua.woody.questborn.lang.ColorFormatter.stripColors(npcName);
                String title = npcConfig.getGuiTitle().replace("{npc}", strippedNpcName).replace("{type}", typeName).replace("{page}", pageStr).replace("{total}", totalStr);
                title = ItemsAdderFontUtil.processFontFormats(title, plugin);
                return org.bukkit.ChatColor.translateAlternateColorCodes('&', title);
            }
        }

        ua.woody.questborn.config.GuiLayout layout = plugin.getMenuConfig().getLayout("quest-list");
        if (layout != null && layout.getProperty("title") != null) {
            String layoutTitle = layout.getProperty("title").toString();
            if (layoutTitle.contains("&") || layoutTitle.contains("<#") || layoutTitle.contains("§")) {
                String title = layoutTitle.replace("{type}", typeConfig != null ? stripColors(typeConfig.getDisplayName()) : "").replace("{page}", pageStr).replace("{total}", totalStr);
                title = ItemsAdderFontUtil.processFontFormats(title, plugin);
                return org.bukkit.ChatColor.translateAlternateColorCodes('&', title);
            } else {
                String title = layoutTitle.replace("{type}", typeConfig != null ? typeConfig.getDisplayName() : "").replace("{page}", pageStr).replace("{total}", totalStr);
                title = ItemsAdderFontUtil.processFontFormats(title, plugin);
                return org.bukkit.ChatColor.translateAlternateColorCodes('&', title);
            }
        }

        return typeName;
    }

    private static String stripColors(String input) {
        if (input == null) return "";
        String result = input.replaceAll("<#([A-Fa-f0-9]{6})>", "");
        result = result.replaceAll("§[0-9A-FK-ORXa-fk-orx]", "");
        result = result.replaceAll("&[0-9A-FK-ORXa-fk-orx]", "");
        return result;
    }

    private int findFirstPageWithQuests() {
        Set<Integer> pagesWithQuests = new HashSet<>();
        for (QuestDefinition q : quests) {
            Integer page = q.getPage();
            if (page == null) pagesWithQuests.add(0);
            else pagesWithQuests.add(page);
        }
        if (pagesWithQuests.isEmpty()) return 0;
        return Collections.min(pagesWithQuests);
    }

    private List<QuestDefinition> getSortedQuests(QuestbornPlugin plugin, Player player, QuestTypeConfig typeConfig) {
        if (getEngine() == EngineType.CHAIN || getEngine() == EngineType.GLOBAL) {
            List<QuestDefinition> chainQuests = new ArrayList<>(plugin.getQuestManager().getByType(typeConfig));
            chainQuests.removeIf(q -> shouldHideQuestInMenu(q, player));
            chainQuests.sort((q1, q2) -> {
                Integer page1 = q1.getPage() != null ? q1.getPage() : 0;
                Integer page2 = q2.getPage() != null ? q2.getPage() : 0;
                if (!page1.equals(page2)) return Integer.compare(page1, page2);
                Integer slot1 = q1.getSlot() != null ? q1.getSlot() : 999;
                Integer slot2 = q2.getSlot() != null ? q2.getSlot() : 999;
                return Integer.compare(slot1, slot2);
            });
            return chainQuests;
        }

        if (getEngine() == EngineType.ROTATION) {
            List<QuestDefinition> rotationQuests = new ArrayList<>(plugin.getQuestManager().getOrAssignRotationQuests(player, typeConfig));
            rotationQuests.removeIf(q -> shouldHideQuestInMenu(q, player));
            return rotationQuests;
        }

        PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());
        long now = System.currentTimeMillis();
        List<QuestDefinition> allQuests = new ArrayList<>(plugin.getQuestManager().getByType(typeConfig));
        allQuests.removeIf(q -> shouldHideQuestInMenu(q, player));
        allQuests.sort((q1, q2) -> {
            int priority1 = getQuestPriority(q1, data, now, typeConfig);
            int priority2 = getQuestPriority(q2, data, now, typeConfig);

            if (priority1 != priority2) return Integer.compare(priority1, priority2);

            if (priority1 == 4) {
                long cd1 = data.getQuestCooldownUntil(q1.getId());
                long cd2 = data.getQuestCooldownUntil(q2.getId());
                return Long.compare(cd1, cd2);
            }
            return 0;
        });
        return allQuests;
    }

    private boolean shouldHideQuestInMenu(QuestDefinition q, Player player) {
        PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());
        if (data.hasActiveQuest(q.getId()) || data.hasPendingReward(q.getId())) {
            return false;
        }

        if (this.npcId == null) {
            if (!plugin.getGuiConfig().isHideNpcBoundQuests() || plugin.getNpcIntegrationManager().getProvider() == null) return false;

            if (plugin.getNpcManager() != null) {
                for (ua.woody.questborn.model.NpcConfig nc : plugin.getNpcManager().getAll()) {
                    if (!nc.getStartsQuests().isEmpty() && nc.getStartsQuests().contains(q.getId())) {
                        return true;
                    }
                }
            }
            return false;
        } else {
            if (plugin.getNpcManager() != null) {
                ua.woody.questborn.model.NpcConfig nc = plugin.getNpcManager().getConfigByNpcId(this.npcId);
                if (nc != null) {
                    if (nc.getStartsQuests().contains(q.getId()) || nc.getStartsTypes().contains(q.getTypeId())) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private int getQuestPriority(QuestDefinition quest, PlayerQuestProgress data, long now, QuestTypeConfig typeConfig) {
        QuestTypeConfig qType = typeConfig != null ? typeConfig : plugin.getQuestManager().getQuestTypeManager().getType(quest.getTypeId());
        EngineType qEngine = qType != null ? qType.getEngine() : EngineType.DEFAULT;

        String qid = quest.getId();
        boolean isActive = data.hasActiveQuest(qid);
        boolean isTracked = qid.equalsIgnoreCase(data.getTrackedQuestId());
        boolean isCompletedOnce = data.isQuestCompleted(qid);

        long cdUntil;
        if (qEngine == EngineType.ROTATION && isCompletedOnce) {
            cdUntil = plugin.getQuestManager().getNextRotationResetTimestamp(qType, data.getRotationAssignedAt((qType != null ? qType.getId() : "")));
        } else {
            cdUntil = data.getQuestCooldownUntil(qid);
        }

        boolean onCooldown = cdUntil > 0 && cdUntil > now;
        boolean hasPendingReward = data.hasPendingReward(qid);
        boolean isOneTime = (qType != null ? qType.getCooldownSeconds() : 0L) == 0 && qEngine != EngineType.ROTATION;

        if (hasPendingReward) return 1;
        if (isActive) return 2;
        if (!onCooldown && (!isCompletedOnce || !isOneTime)) return 3;
        if (onCooldown) return 4;
        if (isCompletedOnce && isOneTime) return 5;
        return 6;
    }

    private void build() {
        GuiUtils.fillLayout(plugin, gui, layout);

        PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());
        long now = System.currentTimeMillis();

        List<QuestDefinition> availableQuests;
        if (typeConfig != null) {
            availableQuests = plugin.getQuestManager().getAvailableQuestsForPlayer(player, typeConfig, false);
        } else {
            availableQuests = new ArrayList<>();
            for (QuestDefinition q : quests) {
                if (plugin.getQuestManager().isQuestAvailable(player, q, data, false)) {
                    availableQuests.add(q);
                }
            }
        }
        Set<String> availableQuestIds = new HashSet<>();
        for (QuestDefinition quest : availableQuests) {
            availableQuestIds.add(quest.getId());
        }

        boolean isNpcMode = (npcId != null);
        boolean centerItems = isNpcMode && plugin.getGuiConfig().isCenterNpcQuests();

        if (getEngine() == EngineType.CHAIN || getEngine() == EngineType.GLOBAL) {
            List<QuestDefinition> questsForCurrentPage = new ArrayList<>();
            for (QuestDefinition q : quests) {
                Integer questPage = q.getPage();
                int effectivePage = (questPage == null) ? 0 : questPage;
                if (effectivePage == currentPage) {
                    questsForCurrentPage.add(q);
                }
            }

            if (questsForCurrentPage.isEmpty() && currentPage != 0) {
                new QuestListGui(plugin, player, typeConfig, 0, simpleView, typeConfig == null ? quests : null, npcIcon, npcId, returnDialogueId, returnNodeId, fromMainMenu).open();
                return;
            }

            if (typeConfig == null || !isNpcMode) {
                if (centerItems) {
                    placeQuestsCentered(questsForCurrentPage, data, now, availableQuestIds);
                } else {
                    placeQuestsBySlotOrSequential(questsForCurrentPage, data, now, availableQuestIds);
                }
            } else {
                if (centerItems) {
                    placeQuestsCentered(questsForCurrentPage, data, now, availableQuestIds);
                } else {
                    Set<String> placedQuestIds = new HashSet<>();
                    boolean[] occupiedSlots = new boolean[100];

                    for (QuestDefinition q : questsForCurrentPage) {
                        Integer questSlot = q.getSlot();
                        if (questSlot != null && questSlot >= 0 && questSlot < gui.getRows() * 9) {
                            if (!occupiedSlots[questSlot]) {
                                createQuestItem(q, questSlot, data, now, availableQuestIds, typeConfig);
                                occupiedSlots[questSlot] = true;
                                placedQuestIds.add(q.getId());
                            }
                        }
                    }

                    for (QuestDefinition q : questsForCurrentPage) {
                        if (placedQuestIds.contains(q.getId())) continue;

                        for (int slot : questSlots) {
                            if (!occupiedSlots[slot]) {
                                createQuestItem(q, slot, data, now, availableQuestIds, typeConfig);
                                occupiedSlots[slot] = true;
                                placedQuestIds.add(q.getId());
                                break;
                            }
                        }
                    }
                }
            }

            if (!simpleView || returnDialogueId != null) {
                addMainMenuButton();
            }
            addEngineInfoButton();
            addNavigationButtonsForChain();
            return;
        }

        int startIndex = currentPage * questsPerPage;
        int endIndex = Math.min(startIndex + questsPerPage, quests.size());

        if (startIndex >= quests.size() && startIndex > 0) {
            new QuestListGui(plugin, player, typeConfig, 0, simpleView, typeConfig == null ? quests : null, npcIcon, npcId, returnDialogueId, returnNodeId, fromMainMenu).open();
            return;
        }

        List<QuestDefinition> questsToPlace = new ArrayList<>();
        for (int i = startIndex; i < endIndex; i++) {
            questsToPlace.add(quests.get(i));
        }

        if (!centerItems) {
            placeQuestsBySlotOrSequential(questsToPlace, data, now, availableQuestIds);
        } else {
            placeQuestsCentered(questsToPlace, data, now, availableQuestIds);
        }

        if (!simpleView || returnDialogueId != null) {
            addMainMenuButton();
        }
        addEngineInfoButton();
        addNavigationButtons();
    }

    private void placeQuestsCentered(List<QuestDefinition> questsToPlace, PlayerQuestProgress data, long now, Set<String> availableQuestIds) {
        if (questsToPlace.isEmpty() || questSlots.isEmpty()) return;

        Map<Integer, List<Integer>> rowsMap = new TreeMap<>();
        for (int slot : questSlots) {
            rowsMap.computeIfAbsent(slot / 9, k -> new ArrayList<>()).add(slot);
        }
        List<List<Integer>> rows = new ArrayList<>(rowsMap.values());

        int maxCols = rows.stream().mapToInt(List::size).max().orElse(1);

        List<List<QuestDefinition>> questChunks = new ArrayList<>();
        for (int i = 0; i < questsToPlace.size(); i += maxCols) {
            questChunks.add(questsToPlace.subList(i, Math.min(i + maxCols, questsToPlace.size())));
        }

        int startRow = Math.max(0, (rows.size() - questChunks.size()) / 2);

        for (int i = 0; i < questChunks.size(); i++) {
            if (startRow + i >= rows.size()) break;
            List<Integer> rowSlots = rows.get(startRow + i);
            List<QuestDefinition> chunk = questChunks.get(i);

            int startCol = Math.max(0, (rowSlots.size() - chunk.size()) / 2);
            for (int j = 0; j < chunk.size(); j++) {
                if (startCol + j >= rowSlots.size()) break;
                int slot = rowSlots.get(startCol + j);
                createQuestItem(chunk.get(j), slot, data, now, availableQuestIds, typeConfig);
            }
        }
    }

    private void placeQuestsBySlotOrSequential(List<QuestDefinition> questsToPlace, PlayerQuestProgress data, long now, Set<String> availableQuestIds) {
        Set<String> placedQuestIds = new HashSet<>();
        boolean[] occupiedSlots = new boolean[100];

        for (QuestDefinition q : questsToPlace) {
            Integer questSlot = q.getSlot();
            if (questSlot != null && questSlot >= 0 && questSlot < gui.getRows() * 9) {
                if (!occupiedSlots[questSlot]) {
                    createQuestItem(q, questSlot, data, now, availableQuestIds, typeConfig);
                    occupiedSlots[questSlot] = true;
                    placedQuestIds.add(q.getId());
                }
            }
        }

        for (QuestDefinition q : questsToPlace) {
            if (placedQuestIds.contains(q.getId())) continue;

            for (int slot : questSlots) {
                if (!occupiedSlots[slot]) {
                    createQuestItem(q, slot, data, now, availableQuestIds, typeConfig);
                    occupiedSlots[slot] = true;
                    placedQuestIds.add(q.getId());
                    break;
                }
            }
        }
    }

    private void addNavigationButtonsForChain() {
        Set<Integer> allPages = new HashSet<>();
        for (QuestDefinition q : quests) {
            Integer page = q.getPage();
            allPages.add(page == null ? 0 : page);
        }

        if (allPages.isEmpty()) return;

        List<Integer> sortedPages = new ArrayList<>(allPages);
        Collections.sort(sortedPages);

        int currentIndex = -1;
        for (int i = 0; i < sortedPages.size(); i++) {
            if (sortedPages.get(i) == currentPage) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) return;

        if (currentIndex > 0) {
            addPrevPageButton(sortedPages.get(currentIndex - 1));
        } else {
            clearButton("prev");
        }

        if (currentIndex < sortedPages.size() - 1) {
            addNextPageButton(sortedPages.get(currentIndex + 1));
        } else {
            clearButton("next");
        }
    }

    private void addNavigationButtons() {
        int totalPages = (int) Math.ceil((double) quests.size() / questsPerPage);

        if (currentPage > 0) {
            addPrevPageButton(currentPage - 1);
        } else {
            clearButton("prev");
        }

        if (currentPage < totalPages - 1) {
            addNextPageButton(currentPage + 1);
        } else {
            clearButton("next");
        }
    }

    private void clearButton(String key) {
        if (layout != null && layout.getItem(key) != null) {
            int slot = layout.getItem(key).getSlot();
            if (slot >= 0 && slot < gui.getRows() * 9) {
                ua.woody.questborn.config.GuiItemConfig fillerConfig = layout.getItem("filler");
                if (fillerConfig != null) {
                    gui.setItem(slot, new dev.triumphteam.gui.guis.GuiItem(fillerConfig.createItemStack(plugin), e -> e.setCancelled(true)));
                } else {
                    gui.removeItem(slot);
                }
            }
        }
    }

    private void createQuestItem(QuestDefinition q, int slot, PlayerQuestProgress data, long now, Set<String> availableQuestIds, QuestTypeConfig typeConfig) {
        QuestTypeConfig qType = typeConfig != null ? typeConfig : plugin.getQuestManager().getQuestTypeManager().getType(q.getTypeId());
        EngineType qEngine = qType != null ? qType.getEngine() : EngineType.DEFAULT;

        String qid = q.getId();
        boolean isActive = data.hasActiveQuest(qid);
        boolean isTracked = qid.equalsIgnoreCase(data.getTrackedQuestId());
        boolean isCompletedOnce = data.isQuestCompleted(qid);
        boolean isAvailable = availableQuestIds.contains(qid);

        long cdUntil;
        if (qEngine == EngineType.ROTATION && isCompletedOnce) {
            cdUntil = plugin.getQuestManager().getNextRotationResetTimestamp(qType, data.getRotationAssignedAt((qType != null ? qType.getId() : "")));
            isAvailable = false;
        } else {
            cdUntil = data.getQuestCooldownUntil(qid);
        }

        boolean onCooldown = cdUntil > now;
        boolean hasPendingReward = data.hasPendingReward(qid);
        boolean isOneTime = (qType != null ? qType.getCooldownSeconds() : 0L) == 0 && qEngine != EngineType.ROTATION;

        boolean usingStatusIcon = false;
        ItemStack item = null;
        if (hasPendingReward) {
            ua.woody.questborn.config.GuiItemConfig conf = layout != null ? layout.getItem("status-reward") : null;
            item = conf != null ? conf.createItemStack(plugin) : new ItemStack(Material.CHEST_MINECART);
            usingStatusIcon = true;
        } else if (onCooldown) {
            ua.woody.questborn.config.GuiItemConfig conf = layout != null ? layout.getItem("status-cooldown") : null;
            item = conf != null ? conf.createItemStack(plugin) : new ItemStack(Material.CLOCK);
            usingStatusIcon = true;
        } else if (!isAvailable && !isActive && !isCompletedOnce) {
            ua.woody.questborn.config.GuiItemConfig conf = layout != null ? layout.getItem("status-locked") : null;
            item = conf != null ? conf.createItemStack(plugin) : new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            usingStatusIcon = true;
        } else {
            if (q.getIconCraftEngineId() != null && plugin.getCraftEngineIntegration() != null && plugin.getCraftEngineIntegration().isEnabled()) {
                ItemStack ce = plugin.getCraftEngineIntegration().getCustomItem(q.getIconCraftEngineId());
                if (ce != null) item = ce.clone();
            } else if (q.getIconItemsAdderId() != null && plugin.getItemsAdderIntegration() != null && plugin.getItemsAdderIntegration().isEnabled()) {
                ItemStack ia = plugin.getItemsAdderIntegration().getCustomItem(q.getIconItemsAdderId());
                if (ia != null) item = ia.clone();
            }
            if (item == null) {
                Material icon = q.getIconMaterial();
                if (icon == null) icon = Material.WRITABLE_BOOK;
                item = new ItemStack(icon);
            }
        }

        ItemMeta meta = item.getItemMeta();
        String displayName = lang.color(q.getDisplayName());
        if (isTracked) {
            displayName += lang.color(lang.tr("gui.quest_list.status.tracked-suffix"));
        }
        meta.setDisplayName(displayName);

        if (!usingStatusIcon && q.getIconCustomModelData() != null) {
            meta.setCustomModelData(q.getIconCustomModelData());
        }

        item.setItemMeta(meta);
        if (!usingStatusIcon) {
            GuiUtils.applyBaseHead(item, q.getIconBaseHead());
        }
        meta = item.getItemMeta();

        GuiUtils.applyAllItemFlags(meta);

        if (isActive || hasPendingReward) {
            meta.addEnchant(Enchantment.LURE, 1, true);
        }

        List<String> lore = new ArrayList<>();
        boolean printedCooldownStatus = false;

        if (hasPendingReward) {
            lore.add(lang.tr("gui.quest_list.status.reward-pending"));
        } else if (isOneTime && isCompletedOnce) {
            lore.add(lang.tr("gui.quest_list.status.completed-onetime"));
        } else if (isActive) {
            int minP = q.getMinParticipants();
            ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(qid);
            int currentP = gp != null ? gp.getParticipantCount() : 0;
            if (qEngine == EngineType.GLOBAL && minP > 0 && currentP < minP && (gp == null || !gp.isActivationReached())) {
                lore.add(lang.tr("gui.quest_list.status.waiting-participants", Map.of(
                        "current", String.valueOf(currentP),
                        "min", String.valueOf(minP)
                )));
            } else {
                lore.add(lang.tr("gui.quest_list.status.active"));
            }
        } else if (onCooldown) {
            long secs = Math.max(0, (cdUntil - now) / 1000L);
            if (qEngine == EngineType.ROTATION) {
                lore.add(lang.tr("gui.quest_list.status.rotation-cooldown", Map.of("time", TimeFormatter.format(secs))));
            } else {
                lore.add(lang.tr("gui.quest_list.status.cooldown", Map.of("time", TimeFormatter.format(secs))));
            }
            printedCooldownStatus = true;
        } else if (isAvailable) {
            lore.add(lang.tr("gui.quest_list.status.available"));
        } else {
            lore.add(lang.tr("gui.quest_list.status.locked"));
        }

        if (qEngine != EngineType.GLOBAL) {
            if (isActive && q.hasStages()) {
                lore.add(lang.tr("gui.quest_list.stage", Map.of("current", String.valueOf(data.getQuestData(q.getId()).getCurrentStage()), "total", String.valueOf(q.getStageCount()))));
            } else if (q.hasStages() && q.getStageCount() > 1) {
                lore.add(lang.tr("gui.quest_list.stages", Map.of("count", String.valueOf(q.getStageCount()))));
            }
        }

        if (qEngine == EngineType.GLOBAL) {
            ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(qid);

            if (isActive && gp != null && gp.isCompleted()) {
                data.removeActiveQuest(qid);
                plugin.getPlayerDataStore().markDirty(player.getUniqueId());
                isActive = false;

                plugin.getQuestManager().getActionBarManager().sendForPlayer(player);
                plugin.getQuestManager().getBossBarManager().updateBar(player);
                plugin.getQuestManager().getScoreboardManager().updateBoard(player);
                plugin.getQuestManager().updateActiveQuestPlayer(player);
            }

            boolean shouldShowDescGlobal = hasPendingReward || isActive || onCooldown || isAvailable || isCompletedOnce;
            if (shouldShowDescGlobal && q.getDescription() != null && !q.getDescription().isEmpty()) {
                lore.add(" ");
                for (String line : q.getDescription()) lore.add(lang.color(line));
            }

            boolean shouldShowRewardsGlobal = plugin.getGuiConfig().isShowRewardsInList() && (hasPendingReward || isActive || (isAvailable && !onCooldown && !(isOneTime && isCompletedOnce)));
            if (shouldShowRewardsGlobal) {
                lore.add(" ");
                String header = lang.tr("gui.quest_list.rewards-header");

                lore.add(header);
                for (String line : q.getRewardsDescription()) lore.add(lang.color(" <#ffffff>" + line));

                if (!printedCooldownStatus && q.getTimeLimitSeconds() > 0) {
                    lore.add(" ");
                    lore.add(lang.tr("gui.quest_list.cooldown-time", Map.of("time", TimeFormatter.format(q.getTimeLimitSeconds()))));
                }
            }

            int globalGoal = plugin.getQuestManager().getProgressProcessor().getGlobalGoal(q);
            int currentGlobal = gp.getGlobalProgress();
            if ((isOneTime && isCompletedOnce) || onCooldown) {
                currentGlobal = globalGoal;
            }
            int maxP = q.getMaxParticipants();
            int currentP = gp.getParticipantCount();

            lore.add(" ");
            lore.add(lang.color(lang.tr("gui.quest_list.global-progress")));
            String gBar = plugin.getGuiConfig().getProgressBar(currentGlobal, globalGoal);
            int gPercent = globalGoal > 0 ? (int) ((currentGlobal / (double) globalGoal) * 100) : 0;
            lore.add(lang.color(lang.tr("gui.quest_details.info.progress.bar", Map.of("bar", gBar, "percent", String.valueOf(gPercent)))));
            lore.add(lang.color(lang.tr("gui.quest_details.info.progress.value", Map.of("current", String.valueOf(currentGlobal), "target", String.valueOf(globalGoal)))));

            lore.add(" ");
            int minP = q.getMinParticipants();
            if (maxP > 0) {
                if (minP > 0) {
                    lore.add(lang.color(lang.tr("gui.quest_list.global-participants-min", Map.of("current", String.valueOf(currentP), "max", String.valueOf(maxP), "min", String.valueOf(minP)))));
                } else {
                    lore.add(lang.color(lang.tr("gui.quest_list.global-participants", Map.of("current", String.valueOf(currentP), "max", String.valueOf(maxP)))));
                }
            } else {
                if (minP > 0) {
                    lore.add(lang.color(lang.tr("gui.quest_list.global-participants-no-max-min", Map.of("current", String.valueOf(currentP), "min", String.valueOf(minP)))));
                } else {
                    lore.add(lang.color(lang.tr("gui.quest_list.global-participants-no-max", Map.of("current", String.valueOf(currentP)))));
                }
            }
            int personalLimit = q.getPersonalLimit();
            int myContribution = gp.getContribution(player.getUniqueId());
            if (personalLimit > 0) {
                lore.add(lang.color(lang.tr("gui.quest_list.global-contribution", Map.of("current", String.valueOf(myContribution), "max", String.valueOf(personalLimit)))));
            } else {
                lore.add(lang.color(lang.tr("gui.quest_list.global-contribution-no-max", Map.of("current", String.valueOf(myContribution)))));
            }
        }

        boolean isGlobal = qEngine == EngineType.GLOBAL;
        boolean shouldShowDescription = !isGlobal && (hasPendingReward || isActive || onCooldown || isAvailable || isCompletedOnce);
        boolean shouldShowRewards = plugin.getGuiConfig().isShowRewardsInList() && !isGlobal && (hasPendingReward || isActive || (isAvailable && !onCooldown && !(isOneTime && isCompletedOnce)));
        boolean shouldShowOpenDetails = isActive || onCooldown || (isAvailable && !onCooldown) || isCompletedOnce;

        if (shouldShowDescription && q.getDescription() != null && !q.getDescription().isEmpty()) {
            lore.add(" ");
            for (String line : q.getDescription()) lore.add(lang.color(line));
        }

        if (!isAvailable && q.getRequiredQuests() != null && !q.getRequiredQuests().isEmpty()) {
            lore.add(" ");
            lore.add(lang.tr("gui.quest_list.chain.required-quests"));
            for (String reqId : q.getRequiredQuests()) {
                QuestDefinition reqQuest = plugin.getQuestManager().getById(reqId);
                if (reqQuest != null) {
                    boolean isReqCompleted = data.isQuestCompleted(reqId);
                    String statusIcon = isReqCompleted ? "<#79d679>\u2713" : "<#ed6868>\u2717";
                    lore.add(lang.color(" <#a8a8a8>" + statusIcon + " <#ffffff>" + reqQuest.getDisplayName()));
                }
            }
        }

        if (shouldShowRewards) {
            lore.add(" ");
            String header = lang.tr("gui.quest_list.rewards-header");

            lore.add(header);
            for (String line : q.getRewardsDescription()) lore.add(lang.color(" <#ffffff>" + line));

            if (!printedCooldownStatus && q.getTimeLimitSeconds() > 0) {
                lore.add(" ");
                if (qEngine == EngineType.ROTATION) {
                    lore.add(lang.tr("gui.quest_list.rotation-cooldown-time", Map.of("time", TimeFormatter.format(q.getTimeLimitSeconds()))));
                } else {
                    lore.add(lang.tr("gui.quest_list.cooldown-time", Map.of("time", TimeFormatter.format(q.getTimeLimitSeconds()))));
                }
            }
        }

        boolean canClaim = plugin.getQuestManager().canClaimRewardFromNpc(player, q, npcId);

        if (hasPendingReward) {
            lore.add(" ");
            if (canClaim) {
                lore.add(lang.tr("gui.quest_list.claim-reward"));
            }
            lore.add(lang.tr("gui.quest_list.open-details"));
        } else if (shouldShowOpenDetails) {
            boolean canTrack = isActive && !q.getId().equals(data.getTrackedQuestId());
            if (isGlobal && q.getPersonalLimit() > 0) {
                ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(q.getId());
                if (gp != null && gp.getContribution(player.getUniqueId()) >= q.getPersonalLimit()) {
                    canTrack = false;
                }
            }

            lore.add(" ");
            lore.add(lang.tr("gui.quest_list.open-details"));
            if (canTrack) {
                lore.add(lang.tr("gui.quest_list.track-quest"));
            }
            boolean quickActionsEnabled = plugin.getConfig().getBoolean("gui.quick-actions-enabled", true);
            if (isActive) {
                if (quickActionsEnabled) {
                    String qHint = lang.tr("gui.quest_list.cancel-quest-quick");
                    if (qHint != null && !qHint.equals("gui.quest_list.cancel-quest-quick")) lore.add(qHint);
                }
            } else if (isAvailable && !onCooldown && !(isOneTime && isCompletedOnce)) {
                boolean hasOtherActive = false;
                for (ua.woody.questborn.model.PlayerQuestProgress.ActiveQuestData qd : data.getActiveQuests().values()) {
                    if (!data.hasPendingReward(qd.getQuestId())) {
                        ua.woody.questborn.model.QuestDefinition activeDef = plugin.getQuestManager().getQuest(qd.getQuestId());
                        if (activeDef != null && activeDef.getTypeId().equalsIgnoreCase(q.getTypeId())) {
                            if (!plugin.getQuestManager().getStateService().isGlobalMaxContributionReached(player, activeDef)) {
                                hasOtherActive = true;
                                break;
                            }
                        }
                    }
                }

                if (quickActionsEnabled) {
                    if (hasOtherActive) {
                        String fHintRep = lang.tr("gui.quest_list.activate-quest-quick-replace");
                        if (fHintRep != null && !fHintRep.equals("gui.quest_list.activate-quest-quick-replace")) {
                            lore.add(fHintRep);
                        } else {
                            String fHint = lang.tr("gui.quest_list.activate-quest-quick");
                            if (fHint != null && !fHint.equals("gui.quest_list.activate-quest-quick")) lore.add(fHint);
                        }
                    } else {
                        String fHint = lang.tr("gui.quest_list.activate-quest-quick");
                        if (fHint != null && !fHint.equals("gui.quest_list.activate-quest-quick")) lore.add(fHint);
                    }
                }
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        final boolean finalIsActive = isActive;
        final boolean finalCanClaim = canClaim;
        GuiItem guiItem = new dev.triumphteam.gui.guis.GuiItem(item, event -> {
            org.bukkit.event.inventory.ClickType click = event.getClick();

            if (click.isRightClick()) {
                if (hasPendingReward) {
                    if (!finalCanClaim) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return;
                    }
                    boolean claimed = plugin.getQuestManager().claimReward(player, q);
                    if (claimed) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                        plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> new QuestListGui(plugin, player, typeConfig, currentPage, simpleView, typeConfig == null ? QuestListGui.this.quests : null, npcIcon, npcId, returnDialogueId, returnNodeId, fromMainMenu).open());
                    }
                    return;
                } else if (shouldShowOpenDetails) {
                    boolean canTrack = finalIsActive && !q.getId().equals(data.getTrackedQuestId());
                    if (isGlobal && q.getPersonalLimit() > 0) {
                        ua.woody.questborn.model.GlobalQuestProgress globalP = plugin.getGlobalQuestDataStore().get(q.getId());
                        if (globalP != null && globalP.getContribution(player.getUniqueId()) >= q.getPersonalLimit()) {
                            canTrack = false;
                        }
                    }
                    if (canTrack) {
                        GuiUtils.playClickSound(plugin, player, npcId);
                        plugin.getQuestManager().trackQuest(player, q.getId());
                        plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> new QuestListGui(plugin, player, typeConfig, currentPage, simpleView, typeConfig == null ? QuestListGui.this.quests : null, npcIcon, npcId, returnDialogueId, returnNodeId, fromMainMenu).open());
                        return;
                    }
                }
            }

            if (click == org.bukkit.event.inventory.ClickType.DROP || click == org.bukkit.event.inventory.ClickType.CONTROL_DROP) {
                if (plugin.getConfig().getBoolean("gui.quick-actions-enabled", true)) {
                    if (finalIsActive) {
                        GuiUtils.playClickSound(plugin, player, npcId);
                        plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> {
                            QuestDetailsGui detailsGui = new QuestDetailsGui(plugin, player, q, false, npcIcon, npcId, returnDialogueId, returnNodeId, true, fromMainMenu);
                            detailsGui.setReturnPage(currentPage);
                            detailsGui.open();
                        });
                    } else {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                }
                return;
            }

            if (click == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND) {
                if (plugin.getConfig().getBoolean("gui.quick-actions-enabled", true)) {
                    if (finalIsActive) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return;
                    }

                    GuiUtils.playClickSound(plugin, player, npcId);
                    plugin.getQuestManager().activateQuest(player, q.getId(), false);
                    ua.woody.questborn.model.PlayerQuestProgress currentData = plugin.getPlayerDataStore().get(player.getUniqueId());

                    if (currentData.hasActiveQuest(q.getId())) {
                        plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> new QuestListGui(plugin, player, typeConfig, currentPage, simpleView, typeConfig == null ? QuestListGui.this.quests : null, npcIcon, npcId, returnDialogueId, returnNodeId, fromMainMenu).open());
                    } else if (q.getId().equals(currentData.getPendingQuestId())) {
                        plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> {
                            QuestDetailsGui detailsGui = new QuestDetailsGui(plugin, player, q, false, npcIcon, npcId, returnDialogueId, returnNodeId, false, fromMainMenu);
                            detailsGui.setReturnPage(currentPage);
                            detailsGui.open();
                        });
                    } else {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                }
                return;
            }

            if (hasPendingReward || shouldShowOpenDetails) {
                GuiUtils.playClickSound(plugin, player, npcId);
                plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> {
                    QuestDetailsGui detailsGui = new QuestDetailsGui(plugin, player, q, false, npcIcon, npcId, returnDialogueId, returnNodeId, false, fromMainMenu);
                    detailsGui.setReturnPage(currentPage);
                    detailsGui.open();
                });
            } else {
                GuiUtils.playClickSound(plugin, player, npcId);
            }
        });

        gui.setItem(slot, guiItem);
    }

    private void addMainMenuButton() {
        if (!fromMainMenu && returnDialogueId != null && returnNodeId != null) {
            int slot = 40;
            if (layout != null && layout.getItem("back-to-dialogue") != null && layout.getItem("back-to-dialogue").getSlot() >= 0) {
                slot = layout.getItem("back-to-dialogue").getSlot();
            } else if (layout != null && layout.getItem("main") != null && layout.getItem("main").getSlot() >= 0) {
                slot = layout.getItem("main").getSlot();
            }
            if (slot < 0 || slot >= gui.getRows() * 9) return;
            ItemStack item = GuiUtils.createActionButton(plugin, layout, "back-to-dialogue", Material.BARRIER, "gui.button.back-to-dialogue");
            if (item.getType() == Material.BARRIER && item.getItemMeta() != null && item.getItemMeta().getDisplayName().equals("gui.button.back-to-dialogue")) {
                item = GuiUtils.createActionButton(plugin, layout, "main", Material.BARRIER, "gui.button.main");
            }
            gui.setItem(slot, new dev.triumphteam.gui.guis.GuiItem(item, event -> {
                GuiUtils.playClickSound(plugin, player, npcId);
                plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> {
                    new DialogueGui(plugin, player, returnDialogueId, returnNodeId, npcId, true).open();
                });
            }));
            return;
        }

        int slot = 40;
        if (layout != null && layout.getItem("main") != null && layout.getItem("main").getSlot() >= 0) {
            slot = layout.getItem("main").getSlot();
        }

        if (slot < 0 || slot >= gui.getRows() * 9) return;

        Material material = Material.BARRIER;
        if (layout != null && layout.getItem("main") != null && layout.getItem("main").getMaterial() != null) {
            material = layout.getItem("main").getMaterial();
        }

        ItemStack item = GuiUtils.createActionButton(plugin, layout, "main", material, "gui.button.main");

        GuiItem guiItem = new dev.triumphteam.gui.guis.GuiItem(item, event -> {
            GuiUtils.playClickSound(plugin, player, npcId);
            plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> {
                if (npcId != null) {
                    plugin.getNpcIntegrationManager().handleNpcClick(player, npcId, null, true, true, 0, returnDialogueId, returnNodeId);
                } else {
                    new MainMenuGui(plugin, player).open();
                }
            });
        });

        gui.setItem(slot, guiItem);
    }

    private void addPrevPageButton(int targetPage) {
        int slot = 38;
        if (layout != null && layout.getItem("back") != null && layout.getItem("back").getSlot() >= 0) {
            slot = layout.getItem("back").getSlot();
        }

        if (slot < 0 || slot >= gui.getRows() * 9) return;

        ItemStack item = GuiUtils.createActionButton(plugin, layout, "back", Material.ARROW, "gui.button.prev");

        GuiItem guiItem = new dev.triumphteam.gui.guis.GuiItem(item, event -> {
            GuiUtils.playClickSound(plugin, player, npcId);
            plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> new QuestListGui(plugin, player, typeConfig, targetPage, simpleView, typeConfig == null ? QuestListGui.this.quests : null, npcIcon, npcId, returnDialogueId, returnNodeId, fromMainMenu).open());
        });

        gui.setItem(slot, guiItem);
    }

    private void addNextPageButton(int targetPage) {
        int slot = 42;
        if (layout != null && layout.getItem("next") != null && layout.getItem("next").getSlot() >= 0) {
            slot = layout.getItem("next").getSlot();
        }

        if (slot < 0 || slot >= gui.getRows() * 9) return;

        ItemStack item = GuiUtils.createActionButton(plugin, layout, "next", Material.ARROW, "gui.button.next");

        GuiItem guiItem = new dev.triumphteam.gui.guis.GuiItem(item, event -> {
            GuiUtils.playClickSound(plugin, player, npcId);
            plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> new QuestListGui(plugin, player, typeConfig, targetPage, simpleView, typeConfig == null ? QuestListGui.this.quests : null, npcIcon, npcId, returnDialogueId, returnNodeId, fromMainMenu).open());
        });

        gui.setItem(slot, guiItem);
    }

    private void addEngineInfoButton() {
        if (layout == null || typeConfig == null) return;
        String engineKey = "engine-info-" + getEngine().name().toLowerCase();

        ua.woody.questborn.config.GuiItemConfig itemConfig = layout.getItem(engineKey);
        if (itemConfig == null || itemConfig.getSlot() < 0) return;

        int slot = itemConfig.getSlot();
        if (slot >= gui.getRows() * 9) return;

        org.bukkit.inventory.ItemStack item = GuiUtils.createActionButton(plugin, layout, engineKey, org.bukkit.Material.PAPER, "");
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        if (meta != null && meta.hasLore()) {
            java.util.List<String> lore = meta.getLore();
            boolean changed = false;
            long remainingTimeSeconds = 0;

            if (getEngine() == EngineType.ROTATION) {
                long assignedAt = plugin.getPlayerDataStore().get(player.getUniqueId()).getRotationAssignedAt(typeConfig.getId());
                long nextReset = plugin.getQuestManager().getNextRotationResetTimestamp(typeConfig, assignedAt);
                remainingTimeSeconds = Math.max(0, (nextReset - System.currentTimeMillis()) / 1000);
            } else if (getEngine() == EngineType.GLOBAL) {
                long nextReset = plugin.getQuestManager().getNextRotationResetTimestamp(typeConfig, 0);
                remainingTimeSeconds = Math.max(0, (nextReset - System.currentTimeMillis()) / 1000);
            }

            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                if (line.contains("{time}")) {
                    String formattedTime = ua.woody.questborn.util.TimeFormatter.format(remainingTimeSeconds);
                    lore.set(i, line.replace("{time}", formattedTime));
                    changed = true;
                }
            }

            if (changed) {
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }

        gui.setItem(slot, new dev.triumphteam.gui.guis.GuiItem(item, event -> {
            GuiUtils.playClickSound(plugin, player, npcId);
        }));
    }

    public void open() {
        gui.open(player);
    }

    public QuestTypeConfig getTypeConfig() {
        return typeConfig;
    }

    public String getTypeId() {
        return (typeConfig != null ? typeConfig.getId() : "");
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public boolean isSimpleView() {
        return simpleView;
    }
}
