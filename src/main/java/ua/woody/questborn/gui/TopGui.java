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
import ua.woody.questborn.config.GuiLayout;
import ua.woody.questborn.config.TopConfig;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.model.TopEntry;

import java.util.*;

public class TopGui {
    private final QuestbornPlugin plugin;
    private final Player viewer;
    private final TopConfig topConfig;
    private final int currentPage;
    private final boolean simpleView;
    private final List<TopEntry> topPlayers;
    private final Gui gui;
    private final GuiLayout layout;

    private final List<Integer> playerSlots;
    private final int playersPerPage;
    private int statsSlot = 4;
    private int backSlot = 40;
    private int prevSlot = 38;
    private int nextSlot = 42;

    public TopGui(QuestbornPlugin plugin, Player player) {
        this(plugin, player, 0, false);
    }

    public TopGui(QuestbornPlugin plugin, Player player, int page) {
        this(plugin, player, page, false);
    }

    public TopGui(QuestbornPlugin plugin, Player player, int page, boolean simpleView) {
        this.plugin = plugin;
        this.viewer = player;
        this.topConfig = plugin.getTopConfig();
        this.currentPage = page;
        this.simpleView = simpleView;
        this.topPlayers = calculateTopPlayers();
        this.layout = plugin.getMenuConfig().getLayout("top");

        this.playerSlots = new ArrayList<>();
        if (layout != null) {
            List<Integer> slots = layout.getSlots("players");
            if (slots != null) {
                this.playerSlots.addAll(slots);
            }
            if (this.playerSlots.isEmpty()) {
                this.playerSlots.addAll(List.of(
                        10, 11, 12, 13, 14, 15, 16,
                        19, 20, 21, 22, 23, 24, 25,
                        28, 29, 30, 31, 32, 33, 34));
            }

            if (layout.getItem("stats") != null && layout.getItem("stats").getSlot() >= 0)
                statsSlot = layout.getItem("stats").getSlot();
            if (layout.getItem("back") != null && layout.getItem("back").getSlot() >= 0)
                backSlot = layout.getItem("back").getSlot();
            if (layout.getItem("prev") != null && layout.getItem("prev").getSlot() >= 0)
                prevSlot = layout.getItem("prev").getSlot();
            if (layout.getItem("next") != null && layout.getItem("next").getSlot() >= 0)
                nextSlot = layout.getItem("next").getSlot();
        } else {
            this.playerSlots.addAll(List.of(
                    10, 11, 12, 13, 14, 15, 16,
                    19, 20, 21, 22, 23, 24, 25,
                    28, 29, 30, 31, 32, 33, 34));
        }
        this.playersPerPage = this.playerSlots.size();

        String title = getFormattedTitle(plugin, page, topPlayers.size(), playersPerPage);

        this.gui = Gui.gui()
                .title(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(title))
                .rows(layout != null ? layout.getSize() / 9 : 6)
                .disableAllInteractions()
                .create();

        build();
    }

    private static String getFormattedTitle(QuestbornPlugin plugin, int page, int topSize, int playersPerPage) {
        int totalPages = (int) Math.ceil((double) topSize / playersPerPage);
        if (totalPages == 0) totalPages = 1;

        String title = "";
        GuiLayout layout = plugin.getMenuConfig().getLayout("top");
        if (layout != null && layout.getProperty("title") != null) {
            String layoutTitle = layout.getProperty("title").toString();
            if (layoutTitle.contains("&") || layoutTitle.contains("<#") || layoutTitle.contains("§")) {
                title = org.bukkit.ChatColor.translateAlternateColorCodes('&', layoutTitle);
            } else {
                title = org.bukkit.ChatColor.translateAlternateColorCodes('&', "<#1c1c1c>" + layoutTitle);
            }
        }

        return title
                .replace("{page}", String.valueOf(page + 1))
                .replace("{total}", String.valueOf(totalPages));
    }

    private List<TopEntry> calculateTopPlayers() {
        return new ArrayList<>(plugin.getQuestManager().getTopManager().getCachedTop());
    }

    private void build() {
        GuiUtils.fillLayout(plugin, gui, layout);
        addPlayerStats();
        addPlayers();
        addNavigationButtons();

        if (!simpleView) {
            addMainMenuButton();
        }
    }

    private void addPlayerStats() {
        if (statsSlot < 0 || statsSlot >= gui.getRows() * 9) return;

        ItemStack statsItem = createStatsItem();
        GuiItem guiItem = new dev.triumphteam.gui.guis.GuiItem(statsItem, event -> event.setCancelled(true));
        gui.setItem(statsSlot, guiItem);
    }

    private ItemStack createStatsItem() {
        PlayerQuestProgress data = plugin.getPlayerDataStore().get(viewer.getUniqueId());
        Map<String, Integer> questCountsByType = new HashMap<>();

        for (String typeId : data.getCompletedByType().keySet()) {
            QuestTypeConfig typeConfig = plugin.getQuestManager().getQuestTypeManager().getType(typeId);
            if (typeConfig != null && typeConfig.isEnabled()) {
                int count = data.getCompleted(typeId);
                if (count > 0) {
                    questCountsByType.put(typeId, count);
                }
            }
        }

        int rank = plugin.getQuestManager().getTopManager().getPlayerRank(viewer.getUniqueId());
        return topConfig.createStatsItem(viewer.getName(), questCountsByType, rank);
    }

    private void addPlayers() {
        if (playersPerPage <= 0) return;

        int startIndex = currentPage * playersPerPage;
        int endIndex = Math.min(startIndex + playersPerPage, topPlayers.size());

        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex >= playerSlots.size()) break;

            TopEntry topEntry = topPlayers.get(i);
            int slot = playerSlots.get(slotIndex);
            int position = i + 1;

            ItemStack playerItem = topConfig.createPlayerItem(topEntry, position, topEntry.getValue());
            GuiItem guiItem = new dev.triumphteam.gui.guis.GuiItem(playerItem, event -> event.setCancelled(true));
            gui.setItem(slot, guiItem);
        }
    }

    private void addNavigationButtons() {
        int totalPages = (int) Math.ceil((double) topPlayers.size() / playersPerPage);
        if (totalPages == 0) totalPages = 1;

        if (currentPage > 0) {
            addPrevPageButton();
        } else {
            clearButton("prev");
        }

        if (currentPage < totalPages - 1) {
            addNextPageButton();
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

    private void addMainMenuButton() {
        if (backSlot < 0 || backSlot >= gui.getRows() * 9) return;

        ItemStack item = new ItemStack(Material.PAINTING);
        if (layout != null && layout.getItem("back") != null) {
            item = layout.getItem("back").createItemStack(plugin);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(GuiUtils.getButtonName(layout, "back", "gui.button.main"));
            meta.setLore(GuiUtils.getButtonLore(layout, "back", "gui.button.main.lore"));
            GuiUtils.applyAllItemFlags(meta);
            item.setItemMeta(meta);
        }

        GuiItem guiItem = new dev.triumphteam.gui.guis.GuiItem(item, event -> {
            GuiUtils.playClickSound(plugin, viewer);
            plugin.getFoliaLib().getImpl().runAtEntity(viewer, __task -> new MainMenuGui(plugin, viewer).open());
        });

        gui.setItem(backSlot, guiItem);
    }

    private void addPrevPageButton() {
        if (prevSlot < 0 || prevSlot >= gui.getRows() * 9) return;

        ItemStack item = new ItemStack(Material.ARROW);
        if (layout != null && layout.getItem("prev") != null) {
            item = layout.getItem("prev").createItemStack(plugin);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(GuiUtils.getButtonName(layout, "prev", "gui.button.prev"));
            meta.setLore(GuiUtils.getButtonLore(layout, "prev", "gui.button.prev.lore"));
            GuiUtils.applyAllItemFlags(meta);
            item.setItemMeta(meta);
        }

        GuiItem guiItem = new dev.triumphteam.gui.guis.GuiItem(item, event -> {
            GuiUtils.playClickSound(plugin, viewer);
            plugin.getFoliaLib().getImpl().runAtEntity(viewer, __task -> new TopGui(plugin, viewer, currentPage - 1, simpleView).open());
        });

        gui.setItem(prevSlot, guiItem);
    }

    private void addNextPageButton() {
        if (nextSlot < 0 || nextSlot >= gui.getRows() * 9) return;

        ItemStack item = new ItemStack(Material.ARROW);
        if (layout != null && layout.getItem("next") != null) {
            item = layout.getItem("next").createItemStack(plugin);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(GuiUtils.getButtonName(layout, "next", "gui.button.next"));
            meta.setLore(GuiUtils.getButtonLore(layout, "next", "gui.button.next.lore"));
            GuiUtils.applyAllItemFlags(meta);
            item.setItemMeta(meta);
        }

        GuiItem guiItem = new dev.triumphteam.gui.guis.GuiItem(item, event -> {
            GuiUtils.playClickSound(plugin, viewer);
            plugin.getFoliaLib().getImpl().runAtEntity(viewer, __task -> new TopGui(plugin, viewer, currentPage + 1, simpleView).open());
        });

        gui.setItem(nextSlot, guiItem);
    }

    public void open() {
        gui.open(viewer);
    }

    public boolean isSimpleView() {
        return simpleView;
    }
}
