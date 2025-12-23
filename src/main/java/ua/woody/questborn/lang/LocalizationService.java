package ua.woody.questborn.lang;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import ua.woody.questborn.model.QuestTypeConfig;

import java.util.Locale;

public class LocalizationService {

    private final LanguageManager languageManager;

    public LocalizationService(LanguageManager manager) {
        this.languageManager = manager;
    }

    // =======================================================================
    // 🧠 Universal fallback-checker
    // Якщо локалізація відсутня → повертає null
    // =======================================================================
    private String localized(String category, String key) {
        if (key == null) return null;

        String value = languageManager.getLocaleValue(category, key);

        if (value == null) return null;

        // ВАЖЛИВО: перевіряємо строгий equals, а не equalsIgnoreCase
        if (value.equals(key)) return null;

        return value;
    }


    // =======================================================================
    // ITEMS
    // =======================================================================
    public String localizeItem(String itemId) {
        return localized("items", itemId);
    }

    // =======================================================================
    // MATERIALS
    // =======================================================================
    public String localizeMaterial(Material material) {
        if (material == null) return null;

        String key = material.name();

        // 1️⃣ Спроба знайти як блок
        String v = localized("blocks", key);
        if (v != null) return v;

        // 2️⃣ Спроба знайти як предмет
        v = localized("items", key);
        if (v != null) return v;

        // 3️⃣ Спроба lowercase
        v = localized(material.isBlock() ? "blocks" : "items",
                key.toLowerCase(Locale.ROOT));
        if (v != null) return v;

        return null;
    }

    // =======================================================================
    // POTIONS
    // =======================================================================
    public String localizePotionType(String potionTypeKey) {
        if (potionTypeKey == null) return null;

        // 1) окрема секція для типів зіль
        String v = localized("potions", potionTypeKey);
        if (v != null) return v;

        // 2) якщо захочеш тримати це в effects (опціонально)
        v = localized("effects", potionTypeKey);
        if (v != null) return v;

        return null;
    }

    // =======================================================================
    // ENTITY — розширена підтримка YAML ключів
    // =======================================================================
    public String localizeEntity(EntityType type) {
        if (type == null) return null;

        // --- Найчастіший формат ключів у YAML ---
        String lower = type.getKey().getKey(); // example: "skeleton_horse"

        String v = localized("entities", lower);
        if (v != null) return v;

        // --- Варіант №2: name() у верхньому регістрі ---
        String upper = type.name(); // SKELETON_HORSE
        v = localized("entities", upper.toLowerCase(Locale.ROOT));
        if (v != null) return v;

        v = localized("entities", upper);
        if (v != null) return v;

        // --- Варіант №3: спрощений ключ без "_" ---
        String compact = lower.replace("_", "");
        v = localized("entities", compact);
        if (v != null) return v;

        // --- Варіант №4: fallback до unknown ---
        v = localized("entities", "unknown");
        if (v != null) return v;

        // --- Останній fallback ---
        return formatDisplayName(type.name());
    }


    // =======================================================================
    // ENCHANTMENTS
    // =======================================================================
    public String localizeEnchantment(Enchantment enchantment) {
        if (enchantment == null) return null;

        NamespacedKey key = Registry.ENCHANTMENT.getKey(enchantment);
        if (key == null) return null;

        String raw = key.getKey(); // minecraft id

        String v = localized("enchantments", raw);
        if (v != null) return v;

        v = localized("enchantments", raw.toLowerCase(Locale.ROOT));
        if (v != null) return v;

        return null;
    }

    // =======================================================================
    // BIOMES
    // =======================================================================
    public String localizeBiome(Biome biome) {
        if (biome == null) return null;

        String raw = biome.name();

        String v = localized("biomes", raw);
        if (v != null) return v;

        v = localized("biomes", raw.toLowerCase(Locale.ROOT));
        if (v != null) return v;

        return null;
    }

    // =======================================================================
    // QUEST TYPES
    // =======================================================================
    public String localizeQuestType(String typeId) {
        if (typeId == null) return "";

        String key = "quest.type." + typeId.toLowerCase(Locale.ROOT);
        String translated = languageManager.tr(key);

        // якщо переклад не знайдено — fallback у красивий текст
        return translated.equals(key) ? formatDisplayName(typeId) : translated;
    }

    public String localizeQuestType(QuestTypeConfig config) {
        if (config == null) return "";

        String key = "quest.type." + config.getId().toLowerCase(Locale.ROOT);
        String translated = languageManager.tr(key);

        return translated.equals(key) ? config.getDisplayName() : translated;
    }

    // =======================================================================
    // DISPLAY NAME FALLBACK (STONE_BUTTON → Stone Button)
    // =======================================================================
    private String formatDisplayName(String raw) {
        if (raw == null || raw.isEmpty()) return "Unknown";

        StringBuilder out = new StringBuilder();

        for (String s : raw.toLowerCase(Locale.ROOT).split("_")) {
            if (!s.isEmpty()) {
                if (out.length() > 0) out.append(" ");
                out.append(Character.toUpperCase(s.charAt(0)))
                        .append(s.substring(1));
            }
        }

        return out.toString();
    }
}
