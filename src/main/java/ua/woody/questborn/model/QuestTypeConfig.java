package ua.woody.questborn.model;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.lang.ColorFormatter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public class QuestTypeConfig {
    private final String id;
    private final String displayName;
    private final long cooldownSeconds;
    private final String folder;
    private final int slot;
    private final Material material;
    private final Integer customModelData;
    private final boolean enabled;
    private final List<String> lore;

    // ✅ NEW
    private final String engine; // default | chains | ...

    public QuestTypeConfig(String id, String displayName, long cooldownSeconds,
                           String folder, int slot, Material material, Integer customModelData,
                           boolean enabled, List<String> lore,
                           String engine) {
        this.id = id;
        this.displayName = ColorFormatter.applyColors(displayName);
        this.cooldownSeconds = cooldownSeconds;
        this.folder = folder;
        this.slot = slot;
        this.material = material;
        this.customModelData = customModelData;
        this.enabled = enabled;
        this.lore = lore != null ? applyHexColorsToList(lore) : new ArrayList<>();
        this.engine = (engine == null || engine.isBlank()) ? "default" : engine.toLowerCase(Locale.ROOT);
    }

    private List<String> applyHexColorsToList(List<String> input) {
        if (input == null) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (String line : input) result.add(ColorFormatter.applyColors(line));
        return result;
    }

    public static QuestTypeConfig loadFromFile(QuestbornPlugin plugin, File file) {
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

            String id = yaml.getString("id");
            if (id == null) {
                id = file.getName().replace(".yml", "").replace("_type", "");
            }

            String displayName = yaml.getString("display-name", id);
            long cooldownSeconds = yaml.getLong("cooldown-seconds", 86400L);
            String folder = yaml.getString("folder", "quests/" + id);
            int slot = yaml.getInt("slot", 10);
            boolean enabled = yaml.getBoolean("enabled", true);

            Integer customModelData = null;
            if (yaml.contains("custom-model-data")) customModelData = yaml.getInt("custom-model-data");

            List<String> lore = yaml.isList("lore") ? yaml.getStringList("lore") : new ArrayList<>();

            Material material;
            try {
                String materialStr = yaml.getString("material", "PAPER");
                material = Material.valueOf(materialStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in " + file.getName() + ", using PAPER");
                material = Material.PAPER;
            }

            // ✅ NEW
            String engine = yaml.getString("engine", "default");

            return new QuestTypeConfig(
                    id, displayName, cooldownSeconds, folder, slot, material, customModelData, enabled, lore, engine
            );

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load quest type from " + file.getName(), e);
            return null;
        }
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public long getCooldownSeconds() { return cooldownSeconds; }
    public String getFolder() { return folder; }
    public int getGuiSlot() { return slot; }
    public int getSlot() { return slot; }
    public Material getGuiMaterial() { return material; }
    public Material getMaterial() { return material; }
    public Integer getCustomModelData() { return customModelData; }
    public boolean isEnabled() { return enabled; }
    public List<String> getLore() { return lore; }

    // ✅ NEW
    public String getEngine() { return engine; }

    @Override
    public String toString() {
        return "QuestTypeConfig{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", cooldownSeconds=" + cooldownSeconds +
                ", folder='" + folder + '\'' +
                ", slot=" + slot +
                ", material=" + material +
                ", customModelData=" + customModelData +
                ", enabled=" + enabled +
                ", engine=" + engine +
                ", lore=" + lore +
                '}';
    }
}
