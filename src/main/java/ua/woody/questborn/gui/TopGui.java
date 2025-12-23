package ua.woody.questborn.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.config.TopConfig;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestTypeConfig;

import java.util.*;

public class TopGui extends BaseGui {

    private final TopConfig topConfig;
    private final int currentPage;
    private final List<TopPlayer> topPlayers;
    private final org.bukkit.entity.Player viewer;

    private static final int[] PLAYER_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private static final int PLAYERS_PER_PAGE = PLAYER_SLOTS.length;

    public TopGui(QuestbornPlugin plugin, org.bukkit.entity.Player player) {
        this(plugin, player, 0);
    }

    public TopGui(QuestbornPlugin plugin, org.bukkit.entity.Player player, int page) {
        super(plugin, player, 45, plugin.getLanguage().tr("gui.top.title"));
        this.topConfig = plugin.getTopConfig();
        this.currentPage = page;
        this.viewer = player;
        this.topPlayers = calculateTopPlayers();
        build();
    }

    private List<TopPlayer> calculateTopPlayers() {
        List<TopPlayer> players = new ArrayList<>();
        Map<UUID, PlayerQuestProgress> allData = plugin.getPlayerDataStore().getAll();

        for (Map.Entry<UUID, PlayerQuestProgress> entry : allData.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerQuestProgress data = entry.getValue();

            // Рахуємо загальну кількість виконаних квестів (сума по всіх ВВІМКНЕНИХ типах)
            int totalQuests = 0;
            for (Map.Entry<String, Integer> typeEntry : data.getCompletedByType().entrySet()) {
                String typeId = typeEntry.getKey();
                int count = typeEntry.getValue();

                // Перевіряємо, чи тип ввімкнений
                QuestTypeConfig typeConfig = plugin.getQuestManager().getQuestTypeManager().getType(typeId);
                if (typeConfig != null && typeConfig.isEnabled()) {
                    totalQuests += count;
                }
            }

            if (totalQuests > 0) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                players.add(new TopPlayer(offlinePlayer, totalQuests));
            }
        }

        // Сортуємо за кількістю квестів (спадання)
        players.sort((p1, p2) -> Integer.compare(p2.getQuestCount(), p1.getQuestCount()));

        // Обрізаємо до розміру топу
        if (players.size() > topConfig.getSize()) {
            players = players.subList(0, topConfig.getSize());
        }

        return players;
    }

    private void build() {
        fillFrame();
        addPlayerStats(); // Додаємо статистику поточного гравця в слот 4
        addPlayers();
        addNavigationButtons();
        addMainMenuButton(40);
    }

    private void addPlayerStats() {
        ItemStack statsItem = createStatsItem();
        inventory.setItem(4, statsItem); // Слот 4 (рахуємо з 0)
    }

    private ItemStack createStatsItem() {
        // Отримуємо прогрес поточного гравця
        PlayerQuestProgress data = plugin.getPlayerDataStore().get(viewer.getUniqueId());

        // Отримуємо всі типи квестів з їх кількістю (тільки ввімкнені)
        Map<String, Integer> questCountsByType = new HashMap<>();
        for (String typeId : data.getCompletedByType().keySet()) {
            // Перевіряємо, чи тип ввімкнений
            QuestTypeConfig typeConfig = plugin.getQuestManager().getQuestTypeManager().getType(typeId);
            if (typeConfig != null && typeConfig.isEnabled()) {
                int count = data.getCompleted(typeId);
                if (count > 0) {
                    questCountsByType.put(typeId, count);
                }
            }
        }

        // Створюємо предмет статистики з автоматичним додаванням типів
        return topConfig.createStatsItem(viewer.getName(), questCountsByType);
    }

    private void addPlayers() {
        int startIndex = currentPage * PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PLAYERS_PER_PAGE, topPlayers.size());

        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex >= PLAYER_SLOTS.length) break;

            TopPlayer topPlayer = topPlayers.get(i);
            int slot = PLAYER_SLOTS[slotIndex];
            int position = i + 1;

            // ВИПРАВЛЕННЯ: передаємо об'єкт OfflinePlayer замість імені
            ItemStack playerItem = topConfig.createPlayerItem(
                    topPlayer.getPlayer(), // передаємо OfflinePlayer
                    position,
                    topPlayer.getQuestCount()
            );
            inventory.setItem(slot, playerItem);
        }
    }

    private void addNavigationButtons() {
        int totalPages = (int) Math.ceil((double) topPlayers.size() / PLAYERS_PER_PAGE);

        if (currentPage > 0) {
            addPrevPageButton(38);
        }

        if (currentPage < totalPages - 1) {
            addNextPageButton(42);
        }
    }

    private void addMainMenuButton(int slot) {
        ItemStack item = new ItemStack(Material.GUSTER_BANNER_PATTERN);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(plugin.getLanguage().tr("gui.button.main"));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "top-action"),
                PersistentDataType.STRING,
                "main"
        );

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    private void addPrevPageButton(int slot) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(plugin.getLanguage().tr("gui.button.prev"));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "top-action"),
                PersistentDataType.STRING,
                "prev"
        );

        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "page"),
                PersistentDataType.INTEGER,
                currentPage - 1
        );

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    private void addNextPageButton(int slot) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(plugin.getLanguage().tr("gui.button.next"));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "top-action"),
                PersistentDataType.STRING,
                "next"
        );

        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "page"),
                PersistentDataType.INTEGER,
                currentPage + 1
        );

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    private static class TopPlayer {
        private final OfflinePlayer player;
        private final int questCount;

        public TopPlayer(OfflinePlayer player, int questCount) {
            this.player = player;
            this.questCount = questCount;
        }

        public OfflinePlayer getPlayer() { return player; }
        public int getQuestCount() { return questCount; }
    }

    public int getCurrentPage() {
        return currentPage;
    }
}