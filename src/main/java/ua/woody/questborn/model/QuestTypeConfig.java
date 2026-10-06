package ua.woody.questborn.model;

import org.bukkit.Material;
import org.bukkit.Sound;
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
    private final String itemsAdderId;
    private final String craftEngineId;
    private final Material material;
    private final Integer customModelData;
    private final String baseHead;
    private final boolean enabled;
    private final List<String> lore;
    private final EngineType engine;
    private final List<Requirement> requirements;
    private final File sourceFile;
    private final boolean autoActivateNext;

    private final List<Integer> rotationSlots;
    private final long resetSeconds;
    private final String resetTime;
    private final int resetAnchorDay;
    private final java.util.Map<String, Integer> rotationPools;
    private final java.util.Map<String, List<Integer>> rotationPoolSlots;
    private final String guiTypeTitle;
    private final String guiQuestTitle;
    private final String guiTransferTitle;
    private final List<String> requirementsLore;
    private final RotationAnnounce rotationAnnounce;

    public QuestTypeConfig(String id, String displayName, long cooldownSeconds,
            String folder, int slot, Material material, String itemsAdderId, String craftEngineId, Integer customModelData, String baseHead,
            boolean enabled, List<String> lore) {
        this(id, displayName, cooldownSeconds, folder, slot, material, itemsAdderId, craftEngineId, customModelData, baseHead,
                enabled, lore, EngineType.DEFAULT, null, null, false, new ArrayList<>(), 0L, null, 1, new java.util.LinkedHashMap<>(), new java.util.LinkedHashMap<>(), null, null, null, null, null);
    }

    public QuestTypeConfig(String id, String displayName, long cooldownSeconds,
            String folder, int slot, Material material, String itemsAdderId, String craftEngineId, Integer customModelData, String baseHead,
            boolean enabled, List<String> lore, EngineType engine) {
        this(id, displayName, cooldownSeconds, folder, slot, material, itemsAdderId, craftEngineId, customModelData, baseHead,
                enabled, lore, engine, null, null, false, new ArrayList<>(), 0L, null, 1, new java.util.LinkedHashMap<>(), new java.util.LinkedHashMap<>(), null, null, null, null, null);
    }

    public QuestTypeConfig(String id, String displayName, long cooldownSeconds,
            String folder, int slot, Material material, String itemsAdderId, String craftEngineId, Integer customModelData, String baseHead,
            boolean enabled, List<String> lore, EngineType engine,
            List<Requirement> requirements, File sourceFile, boolean autoActivateNext) {
        this(id, displayName, cooldownSeconds, folder, slot, material, itemsAdderId, craftEngineId, customModelData, baseHead,
                enabled, lore, engine, requirements, sourceFile, autoActivateNext,
                new ArrayList<>(), 0L, null, 1, new java.util.LinkedHashMap<>(), new java.util.LinkedHashMap<>(), null, null, null, null, null);
    }

    public QuestTypeConfig(String id, String displayName, long cooldownSeconds,
            String folder, int slot, Material material, String itemsAdderId, String craftEngineId, Integer customModelData, String baseHead,
            boolean enabled, List<String> lore, EngineType engine,
            List<Requirement> requirements, File sourceFile, boolean autoActivateNext,
            List<Integer> rotationSlots, long resetSeconds, String resetTime, int resetAnchorDay,
            java.util.Map<String, Integer> rotationPools, java.util.Map<String, List<Integer>> rotationPoolSlots) {
        this(id, displayName, cooldownSeconds, folder, slot, material, itemsAdderId, craftEngineId, customModelData, baseHead, enabled, lore, engine,
                requirements, sourceFile, autoActivateNext, rotationSlots, resetSeconds, resetTime, resetAnchorDay,
                rotationPools, rotationPoolSlots, null, null, null, null, null);
    }

    public QuestTypeConfig(String id, String displayName, long cooldownSeconds,
            String folder, int slot, Material material, String itemsAdderId, String craftEngineId, Integer customModelData, String baseHead,
            boolean enabled, List<String> lore, EngineType engine,
            List<Requirement> requirements, File sourceFile, boolean autoActivateNext,
            List<Integer> rotationSlots, long resetSeconds, String resetTime, int resetAnchorDay,
            java.util.Map<String, Integer> rotationPools, java.util.Map<String, List<Integer>> rotationPoolSlots, String guiTypeTitle, String guiQuestTitle, String guiTransferTitle,
            List<String> requirementsLore, RotationAnnounce rotationAnnounce) {
        this.id = id;
        this.displayName = applyHexColor(displayName);

        if (engine == EngineType.CHAIN || engine == EngineType.ROTATION) {
            this.cooldownSeconds = 0;
        } else {
            this.cooldownSeconds = cooldownSeconds;
        }

        this.folder = folder;
        this.slot = slot;
        this.material = material;
        this.itemsAdderId = itemsAdderId;
        this.craftEngineId = craftEngineId;
        this.customModelData = customModelData;
        this.baseHead = baseHead;
        this.enabled = enabled;
        this.lore = lore != null ? applyHexColorsToList(lore) : new ArrayList<>();
        this.engine = engine;
        this.requirements = requirements != null ? requirements : new ArrayList<>();
        this.sourceFile = sourceFile;
        this.autoActivateNext = autoActivateNext;
        this.rotationSlots = rotationSlots != null ? rotationSlots : new ArrayList<>();
        this.resetSeconds = resetSeconds;
        this.resetTime = resetTime;
        this.resetAnchorDay = resetAnchorDay;
        this.rotationPools = rotationPools != null ? rotationPools : new java.util.LinkedHashMap<>();
        this.rotationPoolSlots = rotationPoolSlots != null ? rotationPoolSlots : new java.util.LinkedHashMap<>();
        this.guiTypeTitle = guiTypeTitle != null ? applyHexColor(guiTypeTitle) : null;
        this.guiQuestTitle = guiQuestTitle != null ? applyHexColor(guiQuestTitle) : null;
        this.guiTransferTitle = guiTransferTitle != null ? applyHexColor(guiTransferTitle) : null;
        this.requirementsLore = requirementsLore != null ? applyHexColorsToList(requirementsLore) : new ArrayList<>();
        this.rotationAnnounce = rotationAnnounce;
    }

    private List<String> applyHexColorsToList(List<String> input) {
        if (input == null)
            return new ArrayList<>();

        List<String> result = new ArrayList<>();
        for (String line : input) {
            result.add(applyHexColor(line));
        }
        return result;
    }

    private String applyHexColor(String input) {
        return ColorFormatter.applyColors(input);
    }

    public static QuestTypeConfig loadFromFile(QuestbornPlugin plugin, File file) {
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

            String id = yaml.getString("id");
            if (id == null) {
                id = file.getName().replace(".yml", "").replace("_type", "");
            }

            String displayName = yaml.getString("display-name", id);

            String cooldownRaw = yaml.getString("cooldown", yaml.getString("cooldown-seconds", "86400"));
            long cooldownSeconds = ua.woody.questborn.util.TimeFormatter.parseDuration(cooldownRaw);

            String folder = yaml.getString("folder", "quests/" + id);
            int slot = yaml.getInt("slot", -1);
            boolean enabled = yaml.getBoolean("enabled", true);
            boolean autoActivateNext = yaml.getBoolean("auto-activate-next", false);

            Integer customModelData = null;
            if (yaml.contains("custom-model-data")) {
                customModelData = yaml.getInt("custom-model-data");
            }

            String baseHead = yaml.getString("base-head", null);
            if (baseHead == null) {
                baseHead = yaml.getString("base_head", null);
            }

            List<String> lore = new ArrayList<>();
            if (yaml.isList("lore")) {
                lore = yaml.getStringList("lore");
            }

            Material material = Material.PAPER;
            String itemsAdderId = null;
            String craftEngineId = null;
            try {
                String materialStr = yaml.getString("material", "PAPER");
                if (materialStr.toLowerCase().startsWith("ce:") || materialStr.toLowerCase().startsWith("craftengine:")) {
                    craftEngineId = materialStr.toLowerCase().startsWith("ce:") ? materialStr.substring(3) : materialStr.substring(12);
                } else if (materialStr.toLowerCase().startsWith("ia:") || materialStr.toLowerCase().startsWith("itemsadder:")) {
                    itemsAdderId = materialStr.toLowerCase().startsWith("ia:") ? materialStr.substring(3) : materialStr.substring(11);
                } else {
                    material = Material.valueOf(materialStr.toUpperCase());
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in " + file.getName() + ", using PAPER");
                material = Material.PAPER;
            }

            EngineType engine = EngineType.DEFAULT;
            try {
                String engineStr = yaml.getString("engine", "DEFAULT").toUpperCase(Locale.ROOT);
                engine = EngineType.valueOf(engineStr);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid engine in " + file.getName() + ", using DEFAULT");
            }

            List<Requirement> requirements = new ArrayList<>();
            if (yaml.isConfigurationSection("requirements")) {
                org.bukkit.configuration.ConfigurationSection reqSec = yaml.getConfigurationSection("requirements");
                if (reqSec != null) {
                    for (String key : reqSec.getKeys(false)) {
                        String value = reqSec.getString(key);
                        requirements.add(Requirement.fromString(key, value));
                    }
                }
            }

            List<Integer> legacyRotationSlots = new ArrayList<>();
            if (yaml.isList("rotation-slots")) {
                legacyRotationSlots = yaml.getIntegerList("rotation-slots");
            }

            String resetPeriodRaw = yaml.getString("reset-period", yaml.getString("reset-seconds", "0"));
            long resetSeconds = ua.woody.questborn.util.TimeFormatter.parseDuration(resetPeriodRaw);

            String resetTime = yaml.getString("reset-time", null);

            int resetAnchorDay = yaml.getInt("reset-anchor-day", 1);

            java.util.Map<String, Integer> rotationPools = new java.util.LinkedHashMap<>();
            java.util.Map<String, List<Integer>> rotationPoolSlots = new java.util.LinkedHashMap<>();
            List<Integer> finalRotationSlots = new ArrayList<>();

            if (yaml.isConfigurationSection("rotation-pools")) {
                org.bukkit.configuration.ConfigurationSection poolSec = yaml.getConfigurationSection("rotation-pools");
                if (poolSec != null) {
                    for (String key : poolSec.getKeys(false)) {
                        if (poolSec.isList(key)) {
                            List<Integer> slots = poolSec.getIntegerList(key);
                            rotationPoolSlots.put(key, slots);
                            rotationPools.put(key, slots.size());
                            finalRotationSlots.addAll(slots);
                        } else {
                            int count = poolSec.getInt(key, 1);
                            rotationPools.put(key, count);
                        }
                    }
                }
            }

            if (!legacyRotationSlots.isEmpty() && finalRotationSlots.isEmpty()) {
                finalRotationSlots.addAll(legacyRotationSlots);
                int currentSlotIndex = 0;
                for (String poolName : rotationPools.keySet()) {
                    if (!rotationPoolSlots.containsKey(poolName)) {
                        int count = rotationPools.get(poolName);
                        List<Integer> poolSlots = new ArrayList<>();
                        for (int i = 0; i < count; i++) {
                            if (currentSlotIndex < legacyRotationSlots.size()) {
                                poolSlots.add(legacyRotationSlots.get(currentSlotIndex));
                                currentSlotIndex++;
                            }
                        }
                        rotationPoolSlots.put(poolName, poolSlots);
                    }
                }
            }

            if (rotationPools.isEmpty() && engine == EngineType.ROTATION && !finalRotationSlots.isEmpty()) {
                rotationPools.put("default", finalRotationSlots.size());
                rotationPoolSlots.put("default", new ArrayList<>(finalRotationSlots));
            }

            String guiTypeTitle = yaml.getString("gui-type-title", yaml.getString("gui-title", null));
            String guiQuestTitle = yaml.getString("gui-quest-title", null);
            String guiTransferTitle = yaml.getString("gui-transfer-title", null);

            List<String> requirementsLore = new ArrayList<>();
            if (yaml.isList("requirements-lore")) {
                requirementsLore = yaml.getStringList("requirements-lore");
            }

            RotationAnnounce rotationAnnounce = null;
            if (yaml.isConfigurationSection("rotation-announce")) {
                org.bukkit.configuration.ConfigurationSection annSec = yaml.getConfigurationSection("rotation-announce");
                boolean annEnabled = annSec.getBoolean("enabled", true);
                Sound annSound = null;
                if (annSec.isString("sound")) {
                    try {
                        annSound = Sound.valueOf(annSec.getString("sound").toUpperCase());
                    } catch (Exception ignored) {}
                }
                float annVolume = (float) annSec.getDouble("volume", 1.0);
                float annPitch = (float) annSec.getDouble("pitch", 1.0);
                List<String> annChat = annSec.isList("chat") ? annSec.getStringList("chat") : new ArrayList<>();
                rotationAnnounce = new RotationAnnounce(annEnabled, annSound, annVolume, annPitch, annChat);
            }

            return new QuestTypeConfig(
                    id, displayName, cooldownSeconds, folder, slot,
                    material, itemsAdderId, craftEngineId, customModelData, baseHead, enabled, lore, engine,
                    requirements, file, autoActivateNext,
                    finalRotationSlots, resetSeconds, resetTime, resetAnchorDay, rotationPools, rotationPoolSlots, guiTypeTitle, guiQuestTitle, guiTransferTitle, requirementsLore, rotationAnnounce);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load quest type from " + file.getName(), e);
            return null;
        }
    }

    public static class RotationAnnounce {
        private final boolean enabled;
        private final Sound sound;
        private final float volume;
        private final float pitch;
        private final List<String> chat;

        public RotationAnnounce(boolean enabled, Sound sound, float volume, float pitch, List<String> chat) {
            this.enabled = enabled;
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
            this.chat = chat;
        }

        public boolean isEnabled() { return enabled; }
        public Sound getSound() { return sound; }
        public float getVolume() { return volume; }
        public float getPitch() { return pitch; }
        public List<String> getChat() { return chat; }
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public String getFolder() {
        return folder;
    }

    public int getGuiSlot() {
        return slot;
    }

    public int getSlot() {
        return slot;
    }

    public Material getGuiMaterial() {
        return material;
    }

    public Material getMaterial() {
        return material;
    }

    public String getItemsAdderId() {
        return itemsAdderId;
    }

    public String getCraftEngineId() {
        return craftEngineId;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public String getBaseHead() {
        return baseHead;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getLore() {
        return lore;
    }

    public EngineType getEngine() {
        return engine;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public boolean isAutoActivateNext() {
        return autoActivateNext;
    }

    public List<Integer> getRotationSlots() {
        return rotationSlots;
    }

    public int getRotationCount() {
        return rotationSlots.size();
    }

    public long getResetSeconds() {
        return resetSeconds;
    }

    public String getGuiTypeTitle() {
        return guiTypeTitle;
    }

    public String getGuiQuestTitle() {
        return guiQuestTitle;
    }

    public String getGuiTransferTitle() {
        return guiTransferTitle;
    }

    public List<String> getRequirementsLore() {
        return requirementsLore;
    }

    public String getResetTime() {
        return resetTime;
    }

    public int getResetAnchorDay() {
        return resetAnchorDay;
    }

    public boolean hasResetTime() {
        return resetTime != null && !resetTime.isEmpty();
    }

    public java.util.Map<String, Integer> getRotationPools() {
        return rotationPools;
    }

    public java.util.Map<String, List<Integer>> getRotationPoolSlots() {
        return rotationPoolSlots;
    }

    public RotationAnnounce getRotationAnnounce() {
        return rotationAnnounce;
    }

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
                ", autoActivateNext=" + autoActivateNext +
                '}';
    }
}
