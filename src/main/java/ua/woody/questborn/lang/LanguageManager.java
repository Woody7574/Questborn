package ua.woody.questborn.lang;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.woody.questborn.QuestbornPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LanguageManager {

    private final QuestbornPlugin plugin;
    private final Map<String, YamlConfiguration> languages = new HashMap<>();
    private final LocalizationService localizationService;

    private YamlConfiguration active;
    private String activeLanguageCode = "en_us";

    public LanguageManager(QuestbornPlugin plugin) {
        this.plugin = plugin;
        this.localizationService = new LocalizationService(this);
        loadLanguages();
        loadActiveLanguage();
    }

    /* =======================================================
     *                    ЗАВАНТАЖЕННЯ МОВ
     * ======================================================= */

    public void reload() {
        languages.clear();
        loadLanguages();
        loadActiveLanguage();
    }

    private void loadLanguages() {
        File folder = new File(plugin.getDataFolder(), "language");
        if (!folder.exists() || !folder.isDirectory()) {
            plugin.getLogger().warning("Language folder not found: " + folder.getPath()
                    + " (resources should be extracted by the main plugin class on first run)");
            // все одно спробуємо load(..), щоб логіка fallback працювала
        }

        load("en_us");
        load("uk_ua");
        load("de_de");
        load("es_es");
        load("ru_ru");
    }

    private void load(String code) {
        File file = new File(plugin.getDataFolder(), "language/" + code + ".yml");

        if (!file.exists()) {
            plugin.getLogger().warning("Language file missing: " + file.getPath()
                    + " (it should be extracted from resources by the main plugin class)");
            return;
        }

        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(file);
            languages.put(code.toLowerCase(Locale.ROOT), yaml);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().severe("Failed to load language: " + code + " (" + e.getMessage() + ")");
        }
    }

    private void loadActiveLanguage() {
        String code = plugin.getConfig().getString("language", "en_us").toLowerCase(Locale.ROOT);

        if (!languages.containsKey(code)) {
            plugin.getLogger().warning("Language " + code + " not found, using en_us");
            code = "en_us";
        }

        active = languages.get(code);
        this.activeLanguageCode = code;

        if (active == null) {
            plugin.getLogger().severe("Active language config is null. Check language files in /language/");
        }
    }

    /* =======================================================
     *                    ОСНОВНІ МЕТОДИ
     * ======================================================= */

    public String tr(String path) {
        if (active == null) return path;

        String value = active.getString(path);
        if (value == null) {
            plugin.getLogger().warning("[Lang] Missing key: " + path);
            return path;
        }

        return ColorFormatter.applyColors(value);
    }

    public String tr(String path, Map<String, String> params) {
        String text = tr(path);

        for (Map.Entry<String, String> entry : params.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return text;
    }

    public List<String> trList(String path) {
        if (active == null) return Collections.emptyList();

        List<String> rawList = active.getStringList(path);
        List<String> coloredList = new ArrayList<>(rawList.size());

        for (String line : rawList) {
            coloredList.add(ColorFormatter.applyColors(line));
        }

        return coloredList;
    }

    public List<String> trList(String path, Map<String, String> params) {
        List<String> list = trList(path);
        List<String> result = new ArrayList<>(list.size());

        for (String line : list) {
            String modifiedLine = line;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                modifiedLine = modifiedLine.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            result.add(modifiedLine);
        }

        return result;
    }

    /* =======================================================
     *                  ДОПОМІЖНІ МЕТОДИ
     * ======================================================= */

    /**
     * Отримує локалізоване значення з категорії
     */
    String getLocaleValue(String category, String rawKey) {
        if (active == null) return null;

        String key = normalizeKey(rawKey);
        String path = "locale." + category + "." + key;

        String value = active.getString(path);

        // ⛔ Якщо ключа немає — повертаємо null
        if (value == null) {
            return null;
        }

        return ColorFormatter.applyColors(value);
    }

    private String normalizeKey(String key) {
        if (key == null) return "";

        key = key.toLowerCase(Locale.ROOT);

        // Видаляємо префікс minecraft: якщо він є
        if (key.startsWith("minecraft:")) {
            key = key.substring(10);
        }

        return key;
    }

    /* =======================================================
     *                    ГЕТЕРИ
     * ======================================================= */

    public String getActiveLanguageCode() {
        return activeLanguageCode;
    }

    public LocalizationService getLocalizationService() {
        return localizationService;
    }

    // Делеговані методи для зворотної сумісності

    public String localizeItem(String itemId) {
        return localizationService.localizeItem(itemId);
    }

    public String localizeMaterial(org.bukkit.Material material) {
        return localizationService.localizeMaterial(material);
    }

    public String localizeEntity(org.bukkit.entity.EntityType entityType) {
        return localizationService.localizeEntity(entityType);
    }

    public String localizeEnchant(org.bukkit.enchantments.Enchantment enchantment) {
        return localizationService.localizeEnchantment(enchantment);
    }

    public String localizeBiome(org.bukkit.block.Biome biome) {
        return localizationService.localizeBiome(biome);
    }

    public String trQuestType(String typeId) {
        return localizationService.localizeQuestType(typeId);
    }

    public String trQuestType(ua.woody.questborn.model.QuestTypeConfig typeConfig) {
        return localizationService.localizeQuestType(typeConfig);
    }

    public String color(String input) {
        return ColorFormatter.applyColors(input);
    }

    public String trTime(String path) {
        return tr("time." + path);
    }
}
