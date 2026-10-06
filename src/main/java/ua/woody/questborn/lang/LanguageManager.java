package ua.woody.questborn.lang;

import org.bukkit.configuration.file.YamlConfiguration;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.util.SmartYamlUpdater;

import java.io.File;
import java.util.*;

public class LanguageManager {
    private final QuestbornPlugin plugin;
    private final Map<String, YamlConfiguration> languages = new HashMap<>();
    private final Map<String, YamlConfiguration> jarLanguages = new HashMap<>();
    private final LocalizationService localizationService;

    private YamlConfiguration active;
    private YamlConfiguration jarActive;
    private YamlConfiguration jarEn;

    private YamlConfiguration activeMinecraft;
    private YamlConfiguration jarActiveMinecraft;
    private YamlConfiguration jarEnMinecraft;

    private YamlConfiguration activeEditor;
    private YamlConfiguration jarActiveEditor;
    private YamlConfiguration jarEnEditor;

    private String activeLanguageCode = "en_us";

    public LanguageManager(QuestbornPlugin plugin) {
        this.plugin = plugin;
        this.localizationService = new LocalizationService(this);
        loadLanguages();
        loadActiveLanguage();
    }

    public void reload() {
        languages.clear();
        jarLanguages.clear();
        loadLanguages();
        loadActiveLanguage();
    }

    public static final String[] ALL_LANGUAGES = new String[] {
        "en_us", "uk_ua", "de_de", "es_es", "fr_fr", "pt_br", "pl_pl", "ru_ru", "kk_kz", "tr_tr"
    };

    private void loadLanguages() {
        File folder = new File(plugin.getDataFolder(), "language");
        if (!folder.exists() || !folder.isDirectory()) {
            plugin.getLogger().warning("Language folder not found: " + folder.getPath()
                    + " (resources should be extracted by the main plugin class on first run)");
        }

        for (String code : ALL_LANGUAGES) {
            load(code);

            File editorFile = new File(plugin.getDataFolder(), "language/editor/" + code + ".yml");
            SmartYamlUpdater.updateAndLoad(plugin, editorFile, "language/editor/" + code + ".yml");

            File mcFile = new File(plugin.getDataFolder(), "language/minecraft/" + code + ".yml");
            SmartYamlUpdater.updateAndLoad(plugin, mcFile, "language/minecraft/" + code + ".yml");
        }
    }

    private void load(String code) {
        String resPath = "language/" + code + ".yml";
        File file = new File(plugin.getDataFolder(), resPath);

        try {
            YamlConfiguration yaml = SmartYamlUpdater.updateAndLoad(plugin, file, resPath);

            if (yaml.contains("locale")) {
                plugin.getLogger().info(QuestbornPlugin.ORANGE + "Migrating old language file: " + QuestbornPlugin.WHITE + file.getName() + QuestbornPlugin.RESET);
                SmartYamlUpdater.createBackup(plugin, file);

                if (yaml.contains("locale.blocks.any")) yaml.set("quests.types.blocks.any", yaml.get("locale.blocks.any"));
                if (yaml.contains("locale.blocks.unknown")) yaml.set("quests.types.blocks.unknown", yaml.get("locale.blocks.unknown"));
                if (yaml.contains("locale.containers.any")) yaml.set("quests.types.containers.any", yaml.get("locale.containers.any"));
                if (yaml.contains("locale.containers.unknown")) yaml.set("quests.types.containers.unknown", yaml.get("locale.containers.unknown"));
                if (yaml.contains("locale.entities.any")) yaml.set("quests.types.entities.any", yaml.get("locale.entities.any"));
                if (yaml.contains("locale.entities.unknown")) yaml.set("quests.types.entities.unknown", yaml.get("locale.entities.unknown"));
                if (yaml.contains("locale.fluids.any")) yaml.set("quests.types.fluids.any", yaml.get("locale.fluids.any"));
                if (yaml.contains("locale.fluids.unknown")) yaml.set("quests.types.fluids.unknown", yaml.get("locale.fluids.unknown"));
                if (yaml.contains("locale.items.any")) yaml.set("quests.types.items.any", yaml.get("locale.items.any"));
                if (yaml.contains("locale.items.any_fuel")) yaml.set("quests.types.items.any_fuel", yaml.get("locale.items.any_fuel"));
                if (yaml.contains("locale.items.unknown")) yaml.set("quests.types.items.unknown", yaml.get("locale.items.unknown"));
                if (yaml.contains("locale.villagers.any")) yaml.set("quests.types.villagers.any", yaml.get("locale.villagers.any"));
                if (yaml.contains("locale.villagers.unknown")) yaml.set("quests.types.villagers.unknown", yaml.get("locale.villagers.unknown"));
                if (yaml.isString("locale.unknown")) yaml.set("quests.types.general.unknown", yaml.get("locale.unknown"));

                yaml.set("locale", null);
                yaml.save(file);
                plugin.getLogger().info(QuestbornPlugin.ORANGE + "Migration completed. The 'locale' section was moved to " + QuestbornPlugin.WHITE + "language/minecraft/" + QuestbornPlugin.RESET);
            }

            languages.put(code.toLowerCase(Locale.ROOT), yaml);

            YamlConfiguration jarYaml = SmartYamlUpdater.loadJarResource(plugin, resPath);
            if (jarYaml != null) {
                jarLanguages.put(code.toLowerCase(Locale.ROOT), jarYaml);
            }
        } catch (Exception e) {
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
        jarActive = jarLanguages.get(code);
        jarEn = jarLanguages.get("en_us");
        if (jarEn == null) {
            jarEn = SmartYamlUpdater.loadJarResource(plugin, "language/en_us.yml");
        }
        this.activeLanguageCode = code;

        if (active == null) {
            plugin.getLogger().severe("Active language config is null. Check language files in /language/");
        }

        File mcFile = new File(plugin.getDataFolder(), "language/minecraft/" + code + ".yml");
        activeMinecraft = SmartYamlUpdater.updateAndLoad(plugin, mcFile, "language/minecraft/" + code + ".yml");
        jarActiveMinecraft = SmartYamlUpdater.loadJarResource(plugin, "language/minecraft/" + code + ".yml");
        jarEnMinecraft = SmartYamlUpdater.loadJarResource(plugin, "language/minecraft/en_us.yml");

        File editorFile = new File(plugin.getDataFolder(), "language/editor/" + code + ".yml");
        activeEditor = SmartYamlUpdater.updateAndLoad(plugin, editorFile, "language/editor/" + code + ".yml");
        jarActiveEditor = SmartYamlUpdater.loadJarResource(plugin, "language/editor/" + code + ".yml");
        jarEnEditor = SmartYamlUpdater.loadJarResource(plugin, "language/editor/en_us.yml");
    }

    public String trEditor(String path) {
        return trEditor(path, Collections.emptyMap());
    }

    public String trEditor(String path, Map<String, String> placeholders) {
        String value = null;
        if (activeEditor != null) {
            value = activeEditor.getString(path);
        }
        if (value == null && jarActiveEditor != null) {
            value = jarActiveEditor.getString(path);
        }
        if (value == null && jarEnEditor != null) {
            value = jarEnEditor.getString(path);
        }

        if (value == null) {
            plugin.getLogger().warning("[EditorLang] Missing key: " + path);
            return path;
        }

        if (value.contains("{prefix}")) {
            String prefixStr = activeEditor != null ? activeEditor.getString("prefix") : null;
            if (prefixStr == null && activeEditor != null) {
                prefixStr = activeEditor.getString("common_editor.prefix");
            }
            if (prefixStr == null && jarActiveEditor != null) {
                prefixStr = jarActiveEditor.getString("prefix", jarActiveEditor.getString("common_editor.prefix", ""));
            }
            if (prefixStr == null && jarEnEditor != null) {
                prefixStr = jarEnEditor.getString("prefix", jarEnEditor.getString("common_editor.prefix", ""));
            }
            if (prefixStr == null) prefixStr = "";
            value = value.replace("{prefix}", prefixStr);
        }

        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                value = value.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
            }
        }

        return ColorFormatter.applyColors(value);
    }

    public List<String> trEditorList(String path) {
        return trEditorList(path, Collections.emptyMap());
    }

    public List<String> trEditorList(String path, Map<String, String> params) {
        List<String> rawList = null;
        if (activeEditor != null) {
            rawList = activeEditor.getStringList(path);
        }
        if ((rawList == null || rawList.isEmpty()) && jarActiveEditor != null) {
            rawList = jarActiveEditor.getStringList(path);
        }
        if ((rawList == null || rawList.isEmpty()) && jarEnEditor != null) {
            rawList = jarEnEditor.getStringList(path);
        }

        if (rawList == null || rawList.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> coloredList = new ArrayList<>(rawList.size());
        for (String line : rawList) {
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    line = line.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "null");
                }
            }
            coloredList.add(ColorFormatter.format(line));
        }
        return coloredList;
    }

    public String tr(String path) {
        String value = null;
        if (active != null) {
            value = active.getString(path);
        }
        if (value == null && jarActive != null) {
            value = jarActive.getString(path);
        }
        if (value == null && jarEn != null) {
            value = jarEn.getString(path);
        }

        if (value == null) {
            plugin.getLogger().warning("[Lang] Missing key: " + path);
            return path;
        }

        if (value.contains("{prefix}")) {
            String prefix = active != null ? active.getString("prefix") : null;
            if (prefix == null && jarActive != null) prefix = jarActive.getString("prefix");
            if (prefix == null && jarEn != null) prefix = jarEn.getString("prefix");
            if (prefix == null) prefix = "";
            value = value.replace("{prefix}", prefix);
        }

        return ColorFormatter.applyColors(value);
    }

    public String tr(String path, Map<String, String> params) {
        String text = tr(path);

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = "{" + entry.getKey() + "}";
            int idx = text.indexOf(key);
            while (idx != -1) {
                String prefix = text.substring(0, idx);
                String suffix = text.substring(idx + key.length());
                String lastColor = ColorFormatter.getLastColor(prefix);
                text = prefix + entry.getValue() + lastColor + suffix;
                idx = text.indexOf(key, prefix.length() + entry.getValue().length() + lastColor.length());
            }
        }

        return text;
    }

    public void sendMessage(org.bukkit.command.CommandSender sender, String path) {
        sendMessage(sender, path, Collections.emptyMap());
    }

    public static String convertLegacyToMiniMessage(String text) {
        if (text == null) return null;
        text = text.replace("&0", "<black>")
                   .replace("&1", "<dark_blue>")
                   .replace("&2", "<dark_green>")
                   .replace("&3", "<dark_aqua>")
                   .replace("&4", "<dark_red>")
                   .replace("&5", "<dark_purple>")
                   .replace("&6", "<gold>")
                   .replace("&7", "<gray>")
                   .replace("&8", "<dark_gray>")
                   .replace("&9", "<blue>")
                   .replace("&a", "<green>")
                   .replace("&b", "<aqua>")
                   .replace("&c", "<red>")
                   .replace("&d", "<light_purple>")
                   .replace("&e", "<yellow>")
                   .replace("&f", "<white>")
                   .replace("&k", "<obfuscated>")
                   .replace("&l", "<bold>")
                   .replace("&m", "<strikethrough>")
                   .replace("&n", "<underlined>")
                   .replace("&o", "<italic>")
                   .replace("&r", "<reset>");
        return text;
    }

    public void sendMessage(org.bukkit.command.CommandSender sender, String path, Map<String, String> params) {
        String value = null;
        if (active != null) {
            value = active.getString(path);
        }
        if (value == null && jarActive != null) {
            value = jarActive.getString(path);
        }
        if (value == null && jarEn != null) {
            value = jarEn.getString(path);
        }

        if (value == null) {
            plugin.getLogger().warning("[Lang] Missing key: " + path);
            sender.sendMessage(path);
            return;
        }

        if (value.contains("{prefix}")) {
            String prefix = active != null ? active.getString("prefix") : null;
            if (prefix == null && jarActive != null) prefix = jarActive.getString("prefix");
            if (prefix == null && jarEn != null) prefix = jarEn.getString("prefix");
            if (prefix == null) prefix = "";
            value = value.replace("{prefix}", prefix);
        }

        for (Map.Entry<String, String> entry : params.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        String withTags = convertLegacyToMiniMessage(value);
        sendRawMessage(sender, withTags);
    }

    public void sendRawMessage(org.bukkit.command.CommandSender sender, String value) {
        try {
            String safeValue = value.replaceAll("§[0-9a-fk-or]", "");
            net.kyori.adventure.text.Component comp = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(safeValue);
            plugin.getAdventure().sender(sender).sendMessage(comp);
        } catch (Throwable t) {
            t.printStackTrace();
            sender.sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors(value));
        }
    }

    public List<String> trTemplate(String path, Map<String, String> simpleParams,
            Map<String, List<String>> listParams) {
        List<String> template = trList(path, simpleParams);
        List<String> result = new ArrayList<>();

        for (String line : template) {
            if (line.contains("{objective_details}") && listParams.containsKey("objective_details")) {
                result.addAll(listParams.get("objective_details"));
            } else if (line.contains("{clickable_details}") && listParams.containsKey("clickable_details")) {
                result.addAll(listParams.get("clickable_details"));
            } else {
                result.add(line);
            }
        }

        return result;
    }

    public List<String> trList(String path) {
        List<String> rawList = null;
        if (active != null) {
            rawList = active.getStringList(path);
        }
        if ((rawList == null || rawList.isEmpty()) && jarActive != null) {
            rawList = jarActive.getStringList(path);
        }
        if ((rawList == null || rawList.isEmpty()) && jarEn != null) {
            rawList = jarEn.getStringList(path);
        }

        if (rawList == null || rawList.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> coloredList = new ArrayList<>(rawList.size());
        String prefix = active != null ? active.getString("prefix", "") : "";
        if (prefix.isEmpty() && jarActive != null) prefix = jarActive.getString("prefix", "");

        for (String line : rawList) {
            if (line.contains("{prefix}") && !prefix.isEmpty()) {
                line = line.replace("{prefix}", prefix);
            }
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
                String key = "{" + entry.getKey() + "}";
                int idx = modifiedLine.indexOf(key);
                while (idx != -1) {
                    String prefix = modifiedLine.substring(0, idx);
                    String suffix = modifiedLine.substring(idx + key.length());
                    String lastColor = ColorFormatter.getLastColor(prefix);
                    modifiedLine = prefix + entry.getValue() + lastColor + suffix;
                    idx = modifiedLine.indexOf(key, prefix.length() + entry.getValue().length() + lastColor.length());
                }
            }
            result.add(modifiedLine);
        }

        return result;
    }

    String getLocaleValue(String category, String rawKey) {
        String key = normalizeKey(rawKey);
        String path = "locale." + category + "." + key;

        String value = null;
        if (activeMinecraft != null) {
            value = activeMinecraft.getString(path);
        }
        if (value == null && jarActiveMinecraft != null) {
            value = jarActiveMinecraft.getString(path);
        }
        if (value == null && jarEnMinecraft != null) {
            value = jarEnMinecraft.getString(path);
        }

        if (value == null) {
            return null;
        }

        return ColorFormatter.applyColors(value);
    }

    private String normalizeKey(String key) {
        if (key == null)
            return "";

        key = key.toLowerCase(Locale.ROOT);

        if (key.startsWith("minecraft:")) {
            key = key.substring(10);
        }

        return key;
    }

    public String getActiveLanguageCode() {
        return activeLanguageCode;
    }

    public LocalizationService getLocalizationService() {
        return localizationService;
    }

    public QuestbornPlugin getPlugin() {
        return plugin;
    }

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

    public String localizeBiome(String biomeId) {
        return localizationService.localizeBiome(biomeId);
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
        return tr("system.time." + path);
    }
}
