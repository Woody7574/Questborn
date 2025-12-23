package ua.woody.questborn.effects;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ua.woody.questborn.QuestbornPlugin;

import org.bukkit.Particle.DustOptions;
import org.bukkit.Particle.DustTransition;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class EffectPresetManager {

    private final QuestbornPlugin plugin;
    private final Logger logger;
    private final Map<String, EffectPreset> presets = new HashMap<>();
    private File effectsFile;
    private YamlConfiguration yaml;

    public EffectPresetManager(QuestbornPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        reload();
    }

    public void reload() {
        presets.clear();

        effectsFile = new File(plugin.getDataFolder(), "quest-effects.yml");

        // ✅ НЕ СТВОРЮЄМО файл тут.
        // Він має бути розпакований з resources головним класом.
        if (!effectsFile.exists()) {
            logger.warning("quest-effects.yml not found: " + effectsFile.getPath()
                    + " (it should be extracted from resources by the main plugin class on first run)");
            return;
        }

        yaml = YamlConfiguration.loadConfiguration(effectsFile);

        ConfigurationSection root = yaml.getConfigurationSection("presets");
        if (root == null) {
            logger.warning("quest-effects.yml does not contain 'presets' section.");
            return;
        }

        for (String id : root.getKeys(false)) {
            try {
                ConfigurationSection sec = root.getConfigurationSection(id);
                if (sec == null) continue;

                EffectPreset preset = parsePreset(id, sec);
                if (preset != null) {
                    presets.put(id.toLowerCase(Locale.ROOT), preset);
                }
            } catch (Exception e) {
                logger.warning("Failed to load effect preset '" + id + "': " + e.getMessage());
                e.printStackTrace();
            }
        }

        logger.info("Loaded " + presets.size() + " effect preset(s) with full customization.");
    }

    private EffectPreset parsePreset(String id, ConfigurationSection sec) {
        try {
            // Фейерверки
            EffectPreset.FireworkEffect firework = parseFirework(sec);

            // Частинки
            List<EffectPreset.ParticleEffect> particles = parseParticles(sec);

            // Звуки
            List<EffectPreset.SoundEffect> sounds = parseSounds(sec);

            // Тайтли
            EffectPreset.TitleEffect title = parseTitle(sec);

            // Босс бар
            EffectPreset.BossBarEffect bossBar = parseBossBar(sec);

            // Літаючий текст
            EffectPreset.FloatingText floatingText = parseFloatingText(sec);

            // Сутності
            List<EffectPreset.EntitySpawn> entitySpawns = parseEntitySpawns(sec);

            // Блоки
            List<EffectPreset.BlockEffect> blockEffects = parseBlockEffects(sec);

            // Амбієнт ефекти
            List<EffectPreset.AmbientEffect> ambientEffects = parseAmbientEffects(sec);

            // Послідовність
            List<EffectPreset.SequenceStep> sequence = parseSequence(sec);

            // Умови
            EffectPreset.Conditions conditions = parseConditions(sec);

            // Налаштування виконання
            EffectPreset.ExecutionSettings execution = parseExecutionSettings(sec);

            return new EffectPreset(
                    id, firework, particles, sounds, title, bossBar, floatingText,
                    entitySpawns, blockEffects, ambientEffects, sequence,
                    conditions, execution, plugin
            );

        } catch (Exception e) {
            logger.warning("Error parsing preset '" + id + "': " + e.getMessage());
            return null;
        }
    }

    // ========== ПАРСЕРИ ДЛЯ КОЖНОГО КОМПОНЕНТА ==========

    private EffectPreset.FireworkEffect parseFirework(ConfigurationSection sec) {
        if (!sec.getBoolean("spawn-firework", false)) {
            return null;
        }

        EffectPreset.FireworkEffect fw = new EffectPreset.FireworkEffect();
        fw.enabled = true;

        // Тип
        String typeStr = sec.getString("firework-type", "BALL");
        try {
            fw.type = EffectPreset.FireworkEffect.Type.valueOf(typeStr.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            fw.type = EffectPreset.FireworkEffect.Type.BALL;
        }

        // Кольори
        fw.colors = new ArrayList<>();
        List<String> colorStrs = sec.getStringList("firework-colors");
        if (colorStrs.isEmpty()) {
            fw.colors.add("255,255,255");
        } else {
            fw.colors.addAll(colorStrs);
        }

        // Кольори зникнення
        fw.fadeColors = new ArrayList<>();
        List<String> fadeStrs = sec.getStringList("firework-fade-colors");
        if (!fadeStrs.isEmpty()) {
            fw.fadeColors.addAll(fadeStrs);
        }

        // Інші властивості
        fw.flicker = sec.getBoolean("firework-flicker", false);
        fw.trail = sec.getBoolean("firework-trail", false);
        fw.power = Math.min(sec.getInt("firework-power", 1), 3);

        return fw;
    }

    private List<EffectPreset.ParticleEffect> parseParticles(ConfigurationSection sec) {
        List<EffectPreset.ParticleEffect> particles = new ArrayList<>();

        if (sec.isList("particles")) {
            List<Map<?, ?>> particleMaps = sec.getMapList("particles");
            for (Map<?, ?> map : particleMaps) {
                try {
                    EffectPreset.ParticleEffect particle = parseParticle(map);
                    if (particle != null) {
                        particles.add(particle);
                    }
                } catch (Exception e) {
                    logger.warning("Failed to parse particle: " + e.getMessage());
                }
            }
        }

        return particles;
    }

    private EffectPreset.ParticleEffect parseParticle(Map<?, ?> map) {
        try {
            Map<String, Object> stringMap = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                stringMap.put(String.valueOf(entry.getKey()), entry.getValue());
            }

            String typeStr = String.valueOf(stringMap.get("type"));
            Particle type = Particle.valueOf(typeStr.toUpperCase(Locale.ROOT));

            int count = ((Number) stringMap.getOrDefault("count", 1)).intValue();
            double offsetX = ((Number) stringMap.getOrDefault("offset-x", 0.0)).doubleValue();
            double offsetY = ((Number) stringMap.getOrDefault("offset-y", 0.0)).doubleValue();
            double offsetZ = ((Number) stringMap.getOrDefault("offset-z", 0.0)).doubleValue();
            double speed = ((Number) stringMap.getOrDefault("speed", 0.0)).doubleValue();

            Object data = null;
            if (stringMap.containsKey("data") && stringMap.get("data") instanceof Map) {
                data = parseParticleData(type, (Map<?, ?>) stringMap.get("data"));
            }

            return new EffectPreset.ParticleEffect(type, count, offsetX, offsetY, offsetZ, speed, data);

        } catch (Exception e) {
            return null;
        }
    }

    private Object parseParticleData(Particle particle, Map<?, ?> dataMap) {
        try {
            Map<String, Object> stringMap = new HashMap<>();
            for (Map.Entry<?, ?> entry : dataMap.entrySet()) {
                stringMap.put(String.valueOf(entry.getKey()), entry.getValue());
            }

            switch (particle) {
                case DUST -> {
                    String colorStr = String.valueOf(stringMap.getOrDefault("color", "255,255,255"));
                    float size = ((Number) stringMap.getOrDefault("size", 1.0f)).floatValue();
                    return new DustOptions(parseColor(colorStr), size);
                }

                case DUST_COLOR_TRANSITION -> {
                    String fromStr = String.valueOf(stringMap.getOrDefault("from-color", "255,0,0"));
                    String toStr = String.valueOf(stringMap.getOrDefault("to-color", "0,0,255"));
                    float transSize = ((Number) stringMap.getOrDefault("size", 1.0f)).floatValue();
                    return new DustTransition(parseColor(fromStr), parseColor(toStr), transSize);
                }

                case BLOCK, BLOCK_MARKER, FALLING_DUST -> {
                    String blockStr = String.valueOf(stringMap.getOrDefault("block", "STONE"));
                    Material blockMat = Material.matchMaterial(blockStr.toUpperCase(Locale.ROOT));
                    if (blockMat != null && blockMat.isBlock()) {
                        return Bukkit.createBlockData(blockMat);
                    }
                }

                case ITEM -> {
                    String itemStr = String.valueOf(stringMap.getOrDefault("item", "STONE"));
                    Material itemMat = Material.matchMaterial(itemStr.toUpperCase(Locale.ROOT));
                    if (itemMat != null) {
                        return new ItemStack(itemMat);
                    }
                }

                case ENCHANTED_HIT -> {
                    String mobColorStr = String.valueOf(stringMap.getOrDefault("color", "255,255,255"));
                    return parseColor(mobColorStr);
                }

                case SCULK_CHARGE -> {
                    return ((Number) stringMap.getOrDefault("roll", 0.5f)).floatValue();
                }

                case NOTE -> {
                    return ((Number) stringMap.getOrDefault("note", 0)).floatValue() / 24.0f;
                }

                case VIBRATION -> {
                    // складний тип - пропускаємо
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private List<EffectPreset.SoundEffect> parseSounds(ConfigurationSection sec) {
        List<EffectPreset.SoundEffect> sounds = new ArrayList<>();

        if (sec.isList("sounds")) {
            List<Map<?, ?>> soundMaps = sec.getMapList("sounds");
            for (Map<?, ?> map : soundMaps) {
                try {
                    EffectPreset.SoundEffect sound = parseSound(map);
                    if (sound != null) {
                        sounds.add(sound);
                    }
                } catch (Exception e) {
                    logger.warning("Failed to parse sound: " + e.getMessage());
                }
            }
        }

        return sounds;
    }

    private EffectPreset.SoundEffect parseSound(Map<?, ?> map) {
        try {
            Map<String, Object> stringMap = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                stringMap.put(String.valueOf(entry.getKey()), entry.getValue());
            }

            String typeStr = String.valueOf(stringMap.get("type"));
            Sound sound = Sound.valueOf(typeStr.toUpperCase(Locale.ROOT));

            float volume = ((Number) stringMap.getOrDefault("volume", 1.0f)).floatValue();
            float pitch = ((Number) stringMap.getOrDefault("pitch", 1.0f)).floatValue();
            int delay = ((Number) stringMap.getOrDefault("delay", 0)).intValue();

            SoundCategory category = null;
            if (stringMap.containsKey("category")) {
                String categoryStr = String.valueOf(stringMap.get("category"));
                category = SoundCategory.valueOf(categoryStr.toUpperCase(Locale.ROOT));
            }

            return new EffectPreset.SoundEffect(sound, volume, pitch, delay, category);

        } catch (Exception e) {
            return null;
        }
    }

    private EffectPreset.TitleEffect parseTitle(ConfigurationSection sec) {
        if (!sec.isConfigurationSection("title")) {
            return null;
        }

        ConfigurationSection titleSec = sec.getConfigurationSection("title");
        if (titleSec == null) return null;

        return new EffectPreset.TitleEffect(
                titleSec.getString("main", ""),
                titleSec.getString("subtitle", ""),
                titleSec.getInt("fade-in", 10),
                titleSec.getInt("stay", 70),
                titleSec.getInt("fade-out", 20)
        );
    }

    private EffectPreset.BossBarEffect parseBossBar(ConfigurationSection sec) {
        if (!sec.isConfigurationSection("bossbar")) {
            return null;
        }

        ConfigurationSection bossSec = sec.getConfigurationSection("bossbar");
        if (bossSec == null || !bossSec.getBoolean("enabled", false)) {
            return null;
        }

        EffectPreset.BossBarEffect bossBar = new EffectPreset.BossBarEffect();
        bossBar.enabled = true;
        bossBar.title = bossSec.getString("title", "");

        try {
            bossBar.color = BarColor.valueOf(bossSec.getString("color", "RED").toUpperCase(Locale.ROOT));
            bossBar.style = BarStyle.valueOf(bossSec.getString("style", "SOLID").toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            bossBar.color = BarColor.RED;
            bossBar.style = BarStyle.SOLID;
        }

        bossBar.progress = Math.max(0.0, Math.min(1.0, bossSec.getDouble("progress", 1.0)));
        bossBar.duration = bossSec.getInt("duration", 100);

        return bossBar;
    }

    private EffectPreset.FloatingText parseFloatingText(ConfigurationSection sec) {
        if (!sec.isConfigurationSection("floating-text")) {
            return null;
        }

        ConfigurationSection textSec = sec.getConfigurationSection("floating-text");
        if (textSec == null || !textSec.getBoolean("enabled", false)) {
            return null;
        }

        EffectPreset.FloatingText text = new EffectPreset.FloatingText();
        text.enabled = true;
        text.text = textSec.getString("text", "");
        text.offsetY = textSec.getDouble("offset-y", 2.5);
        text.duration = textSec.getInt("duration", 100);

        try {
            text.animation = EffectPreset.FloatingText.Animation.valueOf(
                    textSec.getString("animation", "FLOAT").toUpperCase(Locale.ROOT)
            );
        } catch (Exception e) {
            text.animation = EffectPreset.FloatingText.Animation.FLOAT;
        }

        return text;
    }

    private List<EffectPreset.EntitySpawn> parseEntitySpawns(ConfigurationSection sec) {
        List<EffectPreset.EntitySpawn> spawns = new ArrayList<>();

        if (sec.isList("spawn-entities")) {
            List<Map<?, ?>> entityMaps = sec.getMapList("spawn-entities");
            for (Map<?, ?> map : entityMaps) {
                try {
                    EffectPreset.EntitySpawn spawn = parseEntitySpawn(map);
                    if (spawn != null) {
                        spawns.add(spawn);
                    }
                } catch (Exception e) {
                    logger.warning("Failed to parse entity spawn: " + e.getMessage());
                }
            }
        }

        return spawns;
    }

    private EffectPreset.EntitySpawn parseEntitySpawn(Map<?, ?> map) {
        try {
            Map<String, Object> stringMap = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                stringMap.put(String.valueOf(entry.getKey()), entry.getValue());
            }

            EffectPreset.EntitySpawn spawn = new EffectPreset.EntitySpawn();

            String typeStr = String.valueOf(stringMap.get("type"));
            spawn.type = EntityType.valueOf(typeStr.toUpperCase(Locale.ROOT));
            spawn.count = ((Number) stringMap.getOrDefault("count", 1)).intValue();
            spawn.offsetY = ((Number) stringMap.getOrDefault("offset-y", 0.0)).doubleValue();
            spawn.customName = (String) stringMap.get("name");
            spawn.glowing = Boolean.parseBoolean(String.valueOf(stringMap.getOrDefault("glowing", false)));
            spawn.invulnerable = Boolean.parseBoolean(String.valueOf(stringMap.getOrDefault("invulnerable", true)));
            spawn.lifetime = ((Number) stringMap.getOrDefault("lifetime", 100)).intValue();

            spawn.effects = new ArrayList<>();
            if (stringMap.containsKey("effects") && stringMap.get("effects") instanceof List) {
                List<?> effectList = (List<?>) stringMap.get("effects");
                for (Object effectObj : effectList) {
                    if (effectObj instanceof Map) {
                        PotionEffect effect = parsePotionEffect((Map<?, ?>) effectObj);
                        if (effect != null) {
                            spawn.effects.add(effect);
                        }
                    }
                }
            }

            return spawn;

        } catch (Exception e) {
            return null;
        }
    }

    private PotionEffect parsePotionEffect(Map<?, ?> map) {
        try {
            Map<String, Object> stringMap = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                stringMap.put(String.valueOf(entry.getKey()), entry.getValue());
            }

            String typeStr = String.valueOf(stringMap.get("type"));
            PotionEffectType type = PotionEffectType.getByName(typeStr.toUpperCase(Locale.ROOT));
            if (type == null) return null;

            int duration = ((Number) stringMap.getOrDefault("duration", 100)).intValue() * 20;
            int amplifier = ((Number) stringMap.getOrDefault("amplifier", 0)).intValue();
            boolean ambient = Boolean.parseBoolean(String.valueOf(stringMap.getOrDefault("ambient", false)));
            boolean particles = Boolean.parseBoolean(String.valueOf(stringMap.getOrDefault("particles", true)));
            boolean icon = Boolean.parseBoolean(String.valueOf(stringMap.getOrDefault("icon", true)));

            return new PotionEffect(type, duration, amplifier, ambient, particles, icon);

        } catch (Exception e) {
            return null;
        }
    }

    private List<EffectPreset.BlockEffect> parseBlockEffects(ConfigurationSection sec) {
        return new ArrayList<>();
    }

    private List<EffectPreset.AmbientEffect> parseAmbientEffects(ConfigurationSection sec) {
        return new ArrayList<>();
    }

    private List<EffectPreset.SequenceStep> parseSequence(ConfigurationSection sec) {
        return new ArrayList<>();
    }

    private EffectPreset.Conditions parseConditions(ConfigurationSection sec) {
        if (!sec.isConfigurationSection("conditions")) {
            return null;
        }

        ConfigurationSection condSec = sec.getConfigurationSection("conditions");
        if (condSec == null) return null;

        EffectPreset.Conditions conditions = new EffectPreset.Conditions();
        conditions.permission = condSec.getString("requires-permission");
        conditions.world = condSec.getString("world");

        conditions.biomes = new ArrayList<>();
        List<String> biomeStrs = condSec.getStringList("biome");
        for (String biomeStr : biomeStrs) {
            conditions.biomes.add(biomeStr.toUpperCase(Locale.ROOT));
        }

        try {
            conditions.time = EffectPreset.Conditions.Time.valueOf(
                    condSec.getString("time", "ANY").toUpperCase(Locale.ROOT)
            );
        } catch (Exception e) {
            conditions.time = EffectPreset.Conditions.Time.ANY;
        }

        try {
            conditions.weather = EffectPreset.Conditions.Weather.valueOf(
                    condSec.getString("weather", "ANY").toUpperCase(Locale.ROOT)
            );
        } catch (Exception e) {
            conditions.weather = EffectPreset.Conditions.Weather.ANY;
        }

        conditions.minPlayers = condSec.getInt("min-players", 0);
        conditions.maxPlayers = condSec.getInt("max-players", 0);

        return conditions;
    }

    private EffectPreset.ExecutionSettings parseExecutionSettings(ConfigurationSection sec) {
        if (!sec.isConfigurationSection("execution")) {
            EffectPreset.ExecutionSettings settings = new EffectPreset.ExecutionSettings();
            settings.async = false;
            settings.priority = EffectPreset.ExecutionSettings.Priority.NORMAL;
            settings.cancelOnDamage = false;
            settings.cancelOnMove = false;
            settings.cooldown = 0;
            return settings;
        }

        ConfigurationSection execSec = sec.getConfigurationSection("execution");
        if (execSec == null) {
            return new EffectPreset.ExecutionSettings();
        }

        EffectPreset.ExecutionSettings settings = new EffectPreset.ExecutionSettings();
        settings.async = execSec.getBoolean("async", false);

        try {
            settings.priority = EffectPreset.ExecutionSettings.Priority.valueOf(
                    execSec.getString("priority", "NORMAL").toUpperCase(Locale.ROOT)
            );
        } catch (Exception e) {
            settings.priority = EffectPreset.ExecutionSettings.Priority.NORMAL;
        }

        settings.cancelOnDamage = execSec.getBoolean("cancel-on-damage", false);
        settings.cancelOnMove = execSec.getBoolean("cancel-on-move", false);
        settings.cooldown = execSec.getInt("cooldown", 0);

        return settings;
    }

    private Color parseColor(String colorStr) {
        try {
            String[] rgb = colorStr.split(",");
            if (rgb.length == 3) {
                int r = Integer.parseInt(rgb[0].trim());
                int g = Integer.parseInt(rgb[1].trim());
                int b = Integer.parseInt(rgb[2].trim());
                return Color.fromRGB(r, g, b);
            }
        } catch (Exception ignored) {}
        return Color.WHITE;
    }

    // ✅ ВИДАЛЕНО: createDefaultConfig()

    public boolean playPreset(Player player, String presetId) {
        if (presetId == null) return false;
        EffectPreset preset = presets.get(presetId.toLowerCase(Locale.ROOT));
        if (preset == null) {
            logger.warning("Effect preset not found: " + presetId);
            return false;
        }

        preset.play(player);
        return true;
    }

    public boolean hasPreset(String id) {
        return id != null && presets.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public Set<String> getAllPresetIds() {
        return presets.keySet();
    }
}
