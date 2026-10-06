package ua.woody.questborn.lang;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import ua.woody.questborn.model.QuestObjectiveType;
import ua.woody.questborn.model.QuestTypeConfig;

import java.util.Locale;

public class LocalizationService {
    private final LanguageManager languageManager;

    public LocalizationService(LanguageManager manager) {
        this.languageManager = manager;
    }

    public String localizeObjectiveType(QuestObjectiveType type) {
        if (type == null)
            return "Unknown";

        String key = "quest.objective-type." + type.name().toLowerCase(Locale.ROOT);
        String translated = languageManager.tr(key);

        return translated.equals(key) ? type.canonicalKey() : translated;
    }

    private String localized(String category, String key) {
        if (key == null)
            return null;
        String value = languageManager.getLocaleValue(category, key);
        if (value == null)
            return null;
        if (value.equals(key))
            return null;
        return value;
    }

    public String localizeItem(String itemId) {
        return localized("items", itemId);
    }

    public String localizeMaterial(Material material) {
        if (material == null)
            return null;
        String key = material.name();
        String v = localized("blocks", key);
        if (v != null)
            return v;
        v = localized("items", key);
        if (v != null)
            return v;
        v = localized(material.isBlock() ? "blocks" : "items", key.toLowerCase(Locale.ROOT));
        if (v != null)
            return v;
        return null;
    }

    public String localizePotionType(String potionTypeKey) {
        if (potionTypeKey == null)
            return null;
        String v = localized("potions", potionTypeKey);
        if (v != null)
            return v;
        v = localized("effects", potionTypeKey);
        if (v != null)
            return v;
        return null;
    }

    public String localizeEntity(EntityType type) {
        if (type == null)
            return null;
        String lower = type.getKey().getKey();
        String v = localized("entities", lower);
        if (v != null)
            return v;
        String upper = type.name();
        v = localized("entities", upper.toLowerCase(Locale.ROOT));
        if (v != null)
            return v;
        v = localized("entities", upper);
        if (v != null)
            return v;
        String compact = lower.replace("_", "");
        v = localized("entities", compact);
        if (v != null)
            return v;
        v = localized("entities", "unknown");
        if (v != null)
            return v;
        return formatDisplayName(type.name());
    }

    public String localizeEnchantment(Enchantment enchantment) {
        if (enchantment == null)
            return null;
        NamespacedKey key = enchantment.getKey();
        if (key == null)
            return null;
        String raw = key.getKey();
        String v = localized("enchantments", raw);
        if (v != null)
            return v;
        v = localized("enchantments", raw.toLowerCase(Locale.ROOT));
        if (v != null)
            return v;
        return null;
    }

    public String localizeBiome(Biome biome) {
        if (biome == null)
            return null;
        return localizeBiome(biome.name());
    }

    public String localizeBiome(String biomeId) {
        if (biomeId == null)
            return null;
        String v = localized("biomes", biomeId);
        if (v != null)
            return v;
        v = localized("biomes", biomeId.toLowerCase(Locale.ROOT));
        if (v != null)
            return v;
        String raw = biomeId.toLowerCase(Locale.ROOT);
        if (raw.startsWith("minecraft:")) raw = raw.substring(10);
        return formatDisplayName(raw);
    }

    public String localizeQuestType(String typeId) {
        if (typeId == null)
            return "";
        String key = "quest.type." + typeId.toLowerCase(Locale.ROOT);
        String translated = languageManager.tr(key);
        return translated.equals(key) ? formatDisplayName(typeId) : translated;
    }

    public String localizeQuestType(QuestTypeConfig config) {
        if (config == null)
            return "";
        String key = "quest.type." + config.getId().toLowerCase(Locale.ROOT);
        String translated = languageManager.tr(key);
        return translated.equals(key) ? config.getDisplayName() : translated;
    }

    private String formatDisplayName(String raw) {
        if (raw == null || raw.isEmpty())
            return "Unknown";
        StringBuilder out = new StringBuilder();
        for (String s : raw.toLowerCase(Locale.ROOT).split("_")) {
            if (!s.isEmpty()) {
                if (out.length() > 0)
                    out.append(" ");
                out.append(Character.toUpperCase(s.charAt(0)))
                        .append(s.substring(1));
            }
        }
        return out.toString();
    }
}
