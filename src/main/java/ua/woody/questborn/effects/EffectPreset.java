package ua.woody.questborn.effects;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import ua.woody.questborn.QuestbornPlugin;

import java.util.*;
import java.util.function.Consumer;

public class EffectPreset {

    // ========== ВНУТРІШНІ КЛАСИ ==========

    public static class ParticleEffect {
        public Particle type;
        public int count;
        public double offsetX, offsetY, offsetZ;
        public double speed;
        public Object data;
        public Location location;

        public ParticleEffect(Particle type, int count,
                              double offsetX, double offsetY, double offsetZ,
                              double speed, Object data) {
            this.type = type;
            this.count = count;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.speed = speed;
            this.data = data;
        }
    }

    public static class SoundEffect {
        public Sound sound;
        public float volume;
        public float pitch;
        public int delay;
        public SoundCategory category;

        public SoundEffect(Sound sound, float volume, float pitch,
                           int delay, SoundCategory category) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
            this.delay = delay;
            this.category = category;
        }
    }

    public static class TitleEffect {
        public String title;
        public String subtitle;
        public int fadeIn;
        public int stay;
        public int fadeOut;

        public TitleEffect(String title, String subtitle,
                           int fadeIn, int stay, int fadeOut) {
            this.title = title;
            this.subtitle = subtitle;
            this.fadeIn = fadeIn;
            this.stay = stay;
            this.fadeOut = fadeOut;
        }
    }

    public static class FireworkEffect {
        public boolean enabled;
        public Type type;
        public List<String> colors; // Зберігаємо як String для парсингу
        public List<String> fadeColors;
        public boolean flicker;
        public boolean trail;
        public int power;

        public enum Type {
            BALL, BALL_LARGE, STAR, BURST, CREEPER
        }
    }

    public static class BossBarEffect {
        public boolean enabled;
        public String title;
        public BarColor color;
        public BarStyle style;
        public double progress;
        public int duration;
    }

    public static class FloatingText {
        public boolean enabled;
        public String text;
        public double offsetY;
        public int duration;
        public Animation animation;

        public enum Animation {
            FLOAT, RISE, SPIN, PULSE, NONE
        }
    }

    public static class EntitySpawn {
        public EntityType type;
        public int count;
        public double offsetY;
        public String customName;
        public boolean glowing;
        public boolean invulnerable;
        public int lifetime;
        public List<PotionEffect> effects;
    }

    public static class BlockEffect {
        public Type type;
        public Material material;
        public int radius;
        public int duration;
        public Particle particle;

        public enum Type {
            TEMPORARY, PARTICLE_BLOCK, CIRCLE, SPHERE
        }
    }

    public static class AmbientEffect {
        public Type type;
        public Particle particle;
        public double radius;
        public double height;
        public int points;
        public int duration;
        public int rotations;

        public enum Type {
            PARTICLE_CIRCLE, HELIX, SPIRAL, VORTEX, FOUNTAIN
        }
    }

    public static class SequenceStep {
        public int delay;
        public String effectRef;
        public Consumer<Player> customAction;
    }

    public static class Conditions {
        public String permission;
        public String world;
        public List<String> biomes; // Зберігаємо як String
        public Time time;
        public Weather weather;
        public int minPlayers;
        public int maxPlayers;

        public enum Time { DAY, NIGHT, SUNRISE, SUNSET, ANY }
        public enum Weather { CLEAR, RAIN, THUNDER, ANY }
    }

    public static class ExecutionSettings {
        public boolean async;
        public Priority priority;
        public boolean cancelOnDamage;
        public boolean cancelOnMove;
        public int cooldown; // в тиках

        public enum Priority {
            LOWEST, LOW, NORMAL, HIGH, HIGHEST, MONITOR
        }
    }

    // ========== ПОЛЯ КЛАСУ ==========

    private final String id;
    private final FireworkEffect firework;
    private final List<ParticleEffect> particles;
    private final List<SoundEffect> sounds;
    private final TitleEffect title;
    private final BossBarEffect bossBar;
    private final FloatingText floatingText;
    private final List<EntitySpawn> entitySpawns;
    private final List<BlockEffect> blockEffects;
    private final List<AmbientEffect> ambientEffects;
    private final List<SequenceStep> sequence;
    private final Conditions conditions;
    private final ExecutionSettings execution;

    private final QuestbornPlugin plugin;

    // ========== КОНСТРУКТОР ==========

    public EffectPreset(String id,
                        FireworkEffect firework,
                        List<ParticleEffect> particles,
                        List<SoundEffect> sounds,
                        TitleEffect title,
                        BossBarEffect bossBar,
                        FloatingText floatingText,
                        List<EntitySpawn> entitySpawns,
                        List<BlockEffect> blockEffects,
                        List<AmbientEffect> ambientEffects,
                        List<SequenceStep> sequence,
                        Conditions conditions,
                        ExecutionSettings execution,
                        QuestbornPlugin plugin) {
        this.id = id;
        this.firework = firework;
        this.particles = particles;
        this.sounds = sounds;
        this.title = title;
        this.bossBar = bossBar;
        this.floatingText = floatingText;
        this.entitySpawns = entitySpawns;
        this.blockEffects = blockEffects;
        this.ambientEffects = ambientEffects;
        this.sequence = sequence;
        this.conditions = conditions;
        this.execution = execution;
        this.plugin = plugin;
    }

    // ========== ОСНОВНИЙ МЕТОД ==========

    public void play(Player player) {
        // Перевірка умов
        if (!checkConditions(player)) {
            return;
        }

        // Виконання (синхронне/асинхронне)
        Runnable task = () -> executeEffects(player);

        if (execution.async) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    private boolean checkConditions(Player player) {
        if (conditions == null) return true;

        // Перевірка дозволу
        if (conditions.permission != null &&
                !player.hasPermission(conditions.permission)) {
            return false;
        }

        // Перевірка світу
        if (conditions.world != null &&
                !player.getWorld().getName().equalsIgnoreCase(conditions.world)) {
            return false;
        }

        // Перевірка біому
        if (conditions.biomes != null && !conditions.biomes.isEmpty()) {
            String currentBiome = player.getLocation().getBlock().getBiome().name();
            if (!conditions.biomes.contains(currentBiome.toUpperCase())) {
                return false;
            }
        }

        // Перевірка часу доби
        if (conditions.time != Conditions.Time.ANY) {
            long time = player.getWorld().getTime();
            boolean isDay = time < 13000 || time > 23000;
            boolean isNight = time >= 13000 && time <= 23000;

            switch (conditions.time) {
                case DAY:
                    if (!isDay) return false;
                    break;
                case NIGHT:
                    if (!isNight) return false;
                    break;
                case SUNRISE:
                    if (time < 23000 || time > 24000) return false;
                    break;
                case SUNSET:
                    if (time < 12000 || time > 13000) return false;
                    break;
            }
        }

        // Перевірка погоди
        if (conditions.weather != Conditions.Weather.ANY) {
            boolean isRaining = player.getWorld().hasStorm();
            boolean isThundering = player.getWorld().isThundering();

            switch (conditions.weather) {
                case CLEAR:
                    if (isRaining || isThundering) return false;
                    break;
                case RAIN:
                    if (!isRaining || isThundering) return false;
                    break;
                case THUNDER:
                    if (!isThundering) return false;
                    break;
            }
        }

        // Перевірка кількості гравців
        if (conditions.minPlayers > 0 || conditions.maxPlayers > 0) {
            int onlineCount = Bukkit.getOnlinePlayers().size();
            if (conditions.minPlayers > 0 && onlineCount < conditions.minPlayers) {
                return false;
            }
            if (conditions.maxPlayers > 0 && onlineCount > conditions.maxPlayers) {
                return false;
            }
        }

        return true;
    }

    private void executeEffects(Player player) {
        Location baseLoc = player.getLocation().clone().add(0, 1.0, 0);
        World world = player.getWorld();

        // 1. ФЕЙЕРВЕРКИ
        if (firework != null && firework.enabled) {
            spawnFirework(world, baseLoc.clone().add(0, 1.0, 0), firework);
        }

        // 2. ЧАСТИНКИ
        if (particles != null) {
            for (ParticleEffect effect : particles) {
                if (effect == null) continue;

                Location particleLoc = baseLoc.clone()
                        .add(effect.offsetX, effect.offsetY, effect.offsetZ);

                try {
                    if (effect.data != null && effect.type.getDataType() != Void.class) {
                        world.spawnParticle(effect.type, particleLoc, effect.count,
                                effect.offsetX, effect.offsetY, effect.offsetZ,
                                effect.speed, effect.data);
                    } else {
                        world.spawnParticle(effect.type, particleLoc, effect.count,
                                effect.offsetX, effect.offsetY, effect.offsetZ,
                                effect.speed);
                    }
                } catch (Exception e) {
                    // Ігноруємо помилки частинок
                }
            }
        }

        // 3. ЗВУКИ
        if (sounds != null) {
            for (SoundEffect sound : sounds) {
                if (sound == null) continue;

                if (sound.delay > 0) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        playSound(player, sound);
                    }, sound.delay);
                } else {
                    playSound(player, sound);
                }
            }
        }

        // 4. ТАЙТЛИ
        if (title != null) {
            player.sendTitle(
                    plugin.getLanguage().color(title.title),
                    plugin.getLanguage().color(title.subtitle),
                    title.fadeIn, title.stay, title.fadeOut
            );
        }

        // 5. БОСС БАР
        if (bossBar != null && bossBar.enabled) {
            BossBar bar = Bukkit.createBossBar(
                    plugin.getLanguage().color(bossBar.title),
                    bossBar.color, bossBar.style
            );
            bar.setProgress(bossBar.progress);
            bar.addPlayer(player);

            // Автоматичне видалення
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                bar.removePlayer(player);
            }, bossBar.duration);
        }

        // 6. ЛІТАЮЧИЙ ТЕКСТ
        if (floatingText != null && floatingText.enabled) {
            spawnFloatingText(player, baseLoc, floatingText);
        }

        // 7. СПАВН МОБІВ
        if (entitySpawns != null) {
            for (EntitySpawn spawn : entitySpawns) {
                if (spawn == null) continue;
                spawnEntities(world, baseLoc, spawn);
            }
        }

        // 8. ЕФЕКТИ БЛОКІВ
        if (blockEffects != null) {
            for (BlockEffect blockEffect : blockEffects) {
                if (blockEffect == null) continue;
                applyBlockEffect(world, baseLoc, blockEffect);
            }
        }

        // 9. АМБІЄНТ ЕФЕКТИ
        if (ambientEffects != null) {
            for (AmbientEffect ambient : ambientEffects) {
                if (ambient == null) continue;
                createAmbientEffect(player, baseLoc, ambient);
            }
        }

        // 10. ПОСЛІДОВНІСТЬ
        if (sequence != null && !sequence.isEmpty()) {
            executeSequence(player, baseLoc, sequence);
        }
    }

    // ========== ДОПОМІЖНІ МЕТОДИ ==========

    private void spawnFirework(World world, Location loc, FireworkEffect fw) {
        Firework firework = (Firework) world.spawnEntity(loc, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = firework.getFireworkMeta();

        // Тип фейерверку
        org.bukkit.FireworkEffect.Type type;
        switch (fw.type) {
            case BALL_LARGE: type = org.bukkit.FireworkEffect.Type.BALL_LARGE; break;
            case STAR: type = org.bukkit.FireworkEffect.Type.STAR; break;
            case BURST: type = org.bukkit.FireworkEffect.Type.BURST; break;
            case CREEPER: type = org.bukkit.FireworkEffect.Type.CREEPER; break;
            default: type = org.bukkit.FireworkEffect.Type.BALL;
        }

        // Кольори
        List<Color> colors = new ArrayList<>();
        for (String colorStr : fw.colors) {
            colors.add(parseColor(colorStr));
        }

        List<Color> fadeColors = new ArrayList<>();
        if (fw.fadeColors != null) {
            for (String colorStr : fw.fadeColors) {
                fadeColors.add(parseColor(colorStr));
            }
        }

        // Створення ефекту
        org.bukkit.FireworkEffect.Builder builder = org.bukkit.FireworkEffect.builder()
                .with(type)
                .withColor(colors)
                .flicker(fw.flicker)
                .trail(fw.trail);

        if (!fadeColors.isEmpty()) {
            builder.withFade(fadeColors);
        }

        org.bukkit.FireworkEffect effect = builder.build();

        meta.addEffect(effect);
        meta.setPower(Math.min(fw.power, 3)); // Максимум 3
        firework.setFireworkMeta(meta);
    }

    private void playSound(Player player, SoundEffect sound) {
        if (sound.category != null) {
            player.playSound(player.getLocation(), sound.sound,
                    sound.category, sound.volume, sound.pitch);
        } else {
            player.playSound(player.getLocation(), sound.sound,
                    sound.volume, sound.pitch);
        }
    }

    private void spawnFloatingText(Player player, Location baseLoc, FloatingText text) {
        // Створення ARMOR_STAND як носія тексту
        Location textLoc = baseLoc.clone().add(0, text.offsetY, 0);
        ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(textLoc, EntityType.ARMOR_STAND);

        stand.setCustomName(plugin.getLanguage().color(text.text));
        stand.setCustomNameVisible(true);
        stand.setGravity(false);
        stand.setVisible(false);
        stand.setInvulnerable(true);
        stand.setMarker(true);

        // Анімація
        switch (text.animation) {
            case FLOAT:
                animateFloat(stand, text.duration);
                break;
            case RISE:
                animateRise(stand, text.duration);
                break;
            case SPIN:
                animateSpin(stand, text.duration);
                break;
            case PULSE:
                animatePulse(stand, text.duration);
                break;
        }

        // Автовидалення
        Bukkit.getScheduler().runTaskLater(plugin, stand::remove, text.duration);
    }

    private void spawnEntities(World world, Location baseLoc, EntitySpawn spawn) {
        for (int i = 0; i < spawn.count; i++) {
            double radius = 0.5; // Фіксований радіус
            Location spawnLoc = baseLoc.clone()
                    .add(randomOffset(radius), spawn.offsetY, randomOffset(radius));

            Entity entity = world.spawnEntity(spawnLoc, spawn.type);

            if (spawn.customName != null) {
                entity.setCustomName(plugin.getLanguage().color(spawn.customName));
                entity.setCustomNameVisible(true);
            }

            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                living.setGlowing(spawn.glowing);
                living.setInvulnerable(spawn.invulnerable);

                if (spawn.effects != null) {
                    for (PotionEffect effect : spawn.effects) {
                        living.addPotionEffect(effect);
                    }
                }
            }

            // Автовидалення
            if (spawn.lifetime > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, entity::remove, spawn.lifetime);
            }
        }
    }

    private void applyBlockEffect(World world, Location center, BlockEffect effect) {
        // Базова реалізація - можна розширити
        // Наразі просто спавнимо частинки
        if (effect.particle != null) {
            for (int i = 0; i < effect.radius * 10; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double distance = Math.random() * effect.radius;
                double x = Math.cos(angle) * distance;
                double z = Math.sin(angle) * distance;

                Location particleLoc = center.clone().add(x, 0, z);
                world.spawnParticle(effect.particle, particleLoc, 1, 0, 0, 0, 0);
            }
        }
    }

    private void createAmbientEffect(Player player, Location center, AmbientEffect ambient) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= ambient.duration) {
                    cancel();
                    return;
                }

                switch (ambient.type) {
                    case PARTICLE_CIRCLE:
                        createParticleCircle(player, center, ambient);
                        break;
                    case HELIX:
                        createHelix(player, center, ambient, ticks);
                        break;
                    case SPIRAL:
                        createSpiral(player, center, ambient, ticks);
                        break;
                    case VORTEX:
                    case FOUNTAIN:
                        // Поки не реалізовано
                        break;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void createParticleCircle(Player player, Location center, AmbientEffect ambient) {
        for (int i = 0; i < ambient.points; i++) {
            double angle = 2 * Math.PI * i / ambient.points;
            double x = ambient.radius * Math.cos(angle);
            double z = ambient.radius * Math.sin(angle);

            Location particleLoc = center.clone().add(x, ambient.height, z);
            player.getWorld().spawnParticle(ambient.particle, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    private void createHelix(Player player, Location center, AmbientEffect ambient, int tick) {
        double height = ambient.height * tick / (double) ambient.duration;
        double angle = 2 * Math.PI * ambient.rotations * tick / ambient.duration;

        double x = ambient.radius * Math.cos(angle);
        double z = ambient.radius * Math.sin(angle);

        Location particleLoc = center.clone().add(x, height, z);
        player.getWorld().spawnParticle(ambient.particle, particleLoc, 1, 0, 0, 0, 0);
    }

    private void createSpiral(Player player, Location center, AmbientEffect ambient, int tick) {
        // Створення спіралі
        double progress = (double) tick / ambient.duration;
        double radius = ambient.radius * progress;
        double height = ambient.height * progress;
        double angle = 2 * Math.PI * ambient.rotations * tick / 10.0;

        double x = radius * Math.cos(angle);
        double z = radius * Math.sin(angle);

        Location particleLoc = center.clone().add(x, height, z);
        player.getWorld().spawnParticle(ambient.particle, particleLoc, 1, 0, 0, 0, 0);
    }

    private void executeSequence(Player player, Location baseLoc, List<SequenceStep> sequence) {
        for (SequenceStep step : sequence) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Виконання кроку послідовності
                if (step.customAction != null) {
                    step.customAction.accept(player);
                }
            }, step.delay);
        }
    }

    // ========== АНІМАЦІЇ ==========

    private void animateFloat(ArmorStand stand, int duration) {
        new BukkitRunnable() {
            double y = stand.getLocation().getY();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }

                double newY = y + 0.05 * Math.sin(ticks * 0.1);
                Location loc = stand.getLocation();
                loc.setY(newY);
                stand.teleport(loc);

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void animateRise(ArmorStand stand, int duration) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }

                Location loc = stand.getLocation();
                loc.add(0, 0.05, 0);
                stand.teleport(loc);

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void animateSpin(ArmorStand stand, int duration) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }

                Location loc = stand.getLocation();
                loc.setYaw(loc.getYaw() + 10);
                stand.teleport(loc);

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void animatePulse(ArmorStand stand, int duration) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }

                // Можна додати ефект пульсації через зміну видимості
                if (ticks % 10 == 0) {
                    stand.setCustomNameVisible(!stand.isCustomNameVisible());
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // ========== УТІЛІТИ ==========

    private Color parseColor(String colorStr) {
        try {
            String[] rgb = colorStr.split(",");
            if (rgb.length == 3) {
                int r = Math.min(255, Math.max(0, Integer.parseInt(rgb[0].trim())));
                int g = Math.min(255, Math.max(0, Integer.parseInt(rgb[1].trim())));
                int b = Math.min(255, Math.max(0, Integer.parseInt(rgb[2].trim())));
                return Color.fromRGB(r, g, b);
            }
        } catch (Exception e) {
            // ignore
        }
        return Color.WHITE;
    }

    private double randomOffset(double max) {
        return (Math.random() * 2 - 1) * max;
    }

    // ========== ГЕТЕРИ ==========

    public String getId() { return id; }
    public FireworkEffect getFirework() { return firework; }
    public List<ParticleEffect> getParticles() { return particles; }
    public List<SoundEffect> getSounds() { return sounds; }
    public TitleEffect getTitle() { return title; }
    public BossBarEffect getBossBar() { return bossBar; }
    public FloatingText getFloatingText() { return floatingText; }
    public List<EntitySpawn> getEntitySpawns() { return entitySpawns; }
    public List<BlockEffect> getBlockEffects() { return blockEffects; }
    public List<AmbientEffect> getAmbientEffects() { return ambientEffects; }
    public List<SequenceStep> getSequence() { return sequence; }
    public Conditions getConditions() { return conditions; }
    public ExecutionSettings getExecution() { return execution; }
}