package ua.woody.questborn.config;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestTypeConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TopConfig {

    private final QuestbornPlugin plugin;
    private boolean enabled;
    private int size;
    private Material guiMaterial;
    private int guiCustomModelData;
    private int guiSlot;
    private String guiDisplayName;
    private List<String> guiLore;
    private Material playerMaterial;
    private int playerCustomModelData;
    private String playerDisplayName;
    private List<String> playerLore;
    private Material statsMaterial;
    private int statsCustomModelData;
    private String statsDisplayName;
    private List<String> statsLore;

    public TopConfig(QuestbornPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        // ✅ НІЧОГО НЕ СТВОРЮЄМО ТУТ. Файл має бути витягнутий з resources головним класом.
        File file = new File(plugin.getDataFolder(), "top.yml");

        if (!file.exists()) {
            plugin.getLogger().warning("top.yml not found: " + file.getPath()
                    + " (it should be extracted from resources by the main plugin class on first run)");

            // Працюємо на дефолтах в пам'яті, щоб плагін не падав
            applyDefaults();
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Основні налаштування
        this.enabled = config.getBoolean("enabled", true);
        this.size = config.getInt("size", 100);

        // Налаштування GUI кнопки
        String guiMaterialStr = config.getString("gui-item.material", "JUNGLE_HANGING_SIGN");
        this.guiMaterial = Material.matchMaterial(guiMaterialStr);
        if (this.guiMaterial == null) {
            this.guiMaterial = Material.CHEST;
            plugin.getLogger().warning("Invalid material for top gui-item: " + guiMaterialStr + ", using CHEST");
        }

        this.guiCustomModelData = config.getInt("gui-item.custom-model-data", 0);
        this.guiSlot = config.getInt("gui-item.slot", 22);
        this.guiDisplayName = config.getString("gui-item.display-name", "<#ffe380>Player Top");
        this.guiLore = config.getStringList("gui-item.lore");
        if (this.guiLore.isEmpty()) {
            this.guiLore = List.of(
                    "<#cccccc>Top-100 players",
                    "<#cccccc>by completed quests"
            );
        }

        // Налаштування предмету гравця в топі
        String playerMaterialStr = config.getString("player-item.material", "PLAYER_HEAD");
        this.playerMaterial = Material.matchMaterial(playerMaterialStr);
        if (this.playerMaterial == null) {
            this.playerMaterial = Material.PLAYER_HEAD;
            plugin.getLogger().warning("Invalid material for top player-item: " + playerMaterialStr + ", using PLAYER_HEAD");
        }

        this.playerCustomModelData = config.getInt("player-item.custom-model-data", 0);
        this.playerDisplayName = config.getString("player-item.display-name", "<#ffe380>{player}");
        this.playerLore = config.getStringList("player-item.lore");
        if (this.playerLore.isEmpty()) {
            this.playerLore = List.of(
                    "<#cccccc>Position: &e#{position}",
                    "<#cccccc>Completed quests: &6{count}"
            );
        }

        // Налаштування предмету статистики гравця
        String statsMaterialStr = config.getString("stats-item.material", "PLAYER_HEAD");
        this.statsMaterial = Material.matchMaterial(statsMaterialStr);
        if (this.statsMaterial == null) {
            this.statsMaterial = Material.PLAYER_HEAD;
            plugin.getLogger().warning("Invalid material for top stats-item: " + statsMaterialStr + ", using PLAYER_HEAD");
        }

        this.statsCustomModelData = config.getInt("stats-item.custom-model-data", 0);
        this.statsDisplayName = config.getString("stats-item.display-name", "<#6966ff>Your Statistics");
        this.statsLore = config.getStringList("stats-item.lore");
        if (this.statsLore.isEmpty()) {
            this.statsLore = List.of(
                    "{type} <#cccccc>- &e{count}",
                    "",
                    "<#cccccc>Total: &6{total}"
            );
        }
    }

    /**
     * ✅ Дефолти "в пам'яті" (без створення файлу).
     * Використовується якщо top.yml відсутній.
     */
    private void applyDefaults() {
        this.enabled = true;
        this.size = 100;

        this.guiMaterial = Material.CHEST;
        this.guiCustomModelData = 0;
        this.guiSlot = 22;
        this.guiDisplayName = "<#ffe380>Player Top";
        this.guiLore = List.of(
                "<#cccccc>Top-100 players",
                "<#cccccc>by completed quests"
        );

        this.playerMaterial = Material.PLAYER_HEAD;
        this.playerCustomModelData = 0;
        this.playerDisplayName = "<#ffe380>{player}";
        this.playerLore = List.of(
                "<#cccccc>Position: &e#{position}",
                "<#cccccc>Completed quests: &6{count}"
        );

        this.statsMaterial = Material.PLAYER_HEAD;
        this.statsCustomModelData = 0;
        this.statsDisplayName = "<#6966ff>Your Statistics";
        this.statsLore = List.of(
                "{type} <#cccccc>- &e{count}",
                "",
                "<#cccccc>Total: &6{total}"
        );
    }

    /**
     * Створює ItemStack для кнопки топу в головному меню
     */
    public ItemStack createGuiItem() {
        ItemStack item = new ItemStack(guiMaterial);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(plugin.getLanguage().color(guiDisplayName));

            List<String> coloredLore = new ArrayList<>();
            for (String line : guiLore) {
                coloredLore.add(plugin.getLanguage().color(line));
            }
            meta.setLore(coloredLore);

            if (guiCustomModelData != 0) {
                meta.setCustomModelData(guiCustomModelData);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Створює ItemStack для гравця в топі з заміною плейсхолдерів та підтримкою голів
     */
    public ItemStack createPlayerItem(OfflinePlayer player, int position, int questCount) {
        ItemStack item;

        if (playerMaterial == Material.PLAYER_HEAD && player != null) {
            item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(player);
                item.setItemMeta(skullMeta);
            }
        } else {
            item = new ItemStack(playerMaterial);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String playerName = player != null ? player.getName() : "Unknown";
            String displayName = playerDisplayName
                    .replace("{player}", playerName)
                    .replace("{position}", String.valueOf(position))
                    .replace("{count}", String.valueOf(questCount));
            meta.setDisplayName(plugin.getLanguage().color(displayName));

            List<String> coloredLore = new ArrayList<>();
            for (String line : playerLore) {
                String processedLine = line
                        .replace("{player}", playerName)
                        .replace("{position}", String.valueOf(position))
                        .replace("{count}", String.valueOf(questCount));
                coloredLore.add(plugin.getLanguage().color(processedLine));
            }
            meta.setLore(coloredLore);

            if (playerCustomModelData != 0) {
                meta.setCustomModelData(playerCustomModelData);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Створює ItemStack для статистики гравця з автоматичним додаванням типів квестів
     */
    public ItemStack createStatsItem(String playerName, Map<String, Integer> questCountsByType) {
        ItemStack item = new ItemStack(statsMaterial);

        if (statsMaterial == Material.PLAYER_HEAD) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            if (player.hasPlayedBefore()) {
                SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
                if (skullMeta != null) {
                    skullMeta.setOwningPlayer(player);
                    item.setItemMeta(skullMeta);
                }
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = statsDisplayName.replace("{player}", playerName);
            meta.setDisplayName(plugin.getLanguage().color(displayName));

            List<String> coloredLore = new ArrayList<>();
            int totalQuests = 0;

            for (String line : statsLore) {
                if (line.contains("{type}") && line.contains("{count}")) {
                    for (Map.Entry<String, Integer> entry : questCountsByType.entrySet()) {
                        String typeId = entry.getKey();
                        int count = entry.getValue();

                        QuestTypeConfig typeConfig = plugin.getQuestManager().getQuestTypeManager().getType(typeId);
                        if (typeConfig == null || !typeConfig.isEnabled()) continue;

                        totalQuests += count;

                        if (count > 0) {
                            String typeName = typeConfig.getDisplayName();
                            String processedLine = line
                                    .replace("{type}", typeName)
                                    .replace("{count}", String.valueOf(count));
                            coloredLore.add(plugin.getLanguage().color(processedLine));
                        }
                    }
                } else {
                    String processedLine = line.replace("{player}", playerName);
                    coloredLore.add(plugin.getLanguage().color(processedLine));
                }
            }

            for (int i = 0; i < coloredLore.size(); i++) {
                String line = coloredLore.get(i);
                if (line.contains("{total}")) {
                    coloredLore.set(i, line.replace("{total}", String.valueOf(totalQuests)));
                }
            }

            meta.setLore(coloredLore);

            if (statsCustomModelData != 0) {
                meta.setCustomModelData(statsCustomModelData);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean isEnabled() { return enabled; }
    public int getSize() { return size; }
    public int getGuiSlot() { return guiSlot; }
    public Material getGuiMaterial() { return guiMaterial; }
    public int getGuiCustomModelData() { return guiCustomModelData; }
    public String getGuiDisplayName() { return guiDisplayName; }
    public List<String> getGuiLore() { return guiLore; }
    public Material getPlayerMaterial() { return playerMaterial; }
    public int getPlayerCustomModelData() { return playerCustomModelData; }
    public String getPlayerDisplayName() { return playerDisplayName; }
    public List<String> getPlayerLore() { return playerLore; }
    public Material getStatsMaterial() { return statsMaterial; }
    public int getStatsCustomModelData() { return statsCustomModelData; }
    public String getStatsDisplayName() { return statsDisplayName; }
    public List<String> getStatsLore() { return statsLore; }

    public void reload() {
        load();
    }

    public boolean isTopEnabled() {
        return enabled;
    }
}
