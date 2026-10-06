package ua.woody.questborn.effects;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.util.PlaceholderUtil;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EffectPreset {
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
        public List<String> colors;
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

    public static class ActionBarEffect {
        public boolean enabled;
        public String text;
    }

    public static class Conditions {
        public String permission;
        public String world;
        public List<String> biomes;
        public Time time;
        public Weather weather;
        public int minPlayers;
        public int maxPlayers;

        public enum Time {
            DAY, NIGHT, SUNRISE, SUNSET, ANY
        }

        public enum Weather {
            CLEAR, RAIN, THUNDER, ANY
        }
    }

    public static class ExecutionSettings {
        public boolean async;
        public Priority priority;
        public boolean cancelOnDamage;
        public boolean cancelOnMove;
        public int cooldown;

        public enum Priority {
            LOWEST, LOW, NORMAL, HIGH, HIGHEST, MONITOR
        }
    }

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
    private final ActionBarEffect actionBar;
    private final List<PotionEffect> screenEffects;
    private final Conditions conditions;
    private final ExecutionSettings execution;

    private final QuestbornPlugin plugin;

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
            ActionBarEffect actionBar,
            List<PotionEffect> screenEffects,
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
        this.actionBar = actionBar;
        this.screenEffects = screenEffects;
        this.conditions = conditions;
        this.execution = execution;
        this.plugin = plugin;
    }

    public void play(Player player) {
        if (!checkConditions(player)) {
            return;
        }

        java.util.function.Consumer<com.tcoded.folialib.wrapper.task.WrappedTask> task = __ -> executeEffects(player);

        if (execution.async) {
            plugin.getFoliaLib().getImpl().runAsync(task);
        } else {
            plugin.getFoliaLib().getImpl().runAtEntity(player, task);
        }
    }

    private boolean checkConditions(Player player) {
        if (conditions == null)
            return true;

        if (conditions.permission != null &&
                !player.hasPermission(conditions.permission)) {
            return false;
        }

        if (conditions.world != null &&
                !player.getWorld().getName().equalsIgnoreCase(conditions.world)) {
            return false;
        }

        if (conditions.biomes != null && !conditions.biomes.isEmpty()) {
            String currentBiome = player.getLocation().getBlock().getBiome().name();
            if (!conditions.biomes.contains(currentBiome.toUpperCase())) {
                return false;
            }
        }

        if (conditions.time != Conditions.Time.ANY) {
            long time = player.getWorld().getTime();
            boolean isDay = time < 13000 || time > 23000;
            boolean isNight = time >= 13000 && time <= 23000;

            switch (conditions.time) {
                case DAY:
                    if (!isDay)
                        return false;
                    break;
                case NIGHT:
                    if (!isNight)
                        return false;
                    break;
                case SUNRISE:
                    if (time < 23000 || time > 24000)
                        return false;
                    break;
                case SUNSET:
                    if (time < 12000 || time > 13000)
                        return false;
                    break;
            }
        }

        if (conditions.weather != Conditions.Weather.ANY) {
            boolean isRaining = player.getWorld().hasStorm();
            boolean isThundering = player.getWorld().isThundering();

            switch (conditions.weather) {
                case CLEAR:
                    if (isRaining || isThundering)
                        return false;
                    break;
                case RAIN:
                    if (!isRaining || isThundering)
                        return false;
                    break;
                case THUNDER:
                    if (!isThundering)
                        return false;
                    break;
            }
        }

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

        if (firework != null && firework.enabled) {
            spawnFirework(world, baseLoc.clone().add(0, 1.0, 0), firework);
        }

        if (particles != null) {
            for (ParticleEffect effect : particles) {
                if (effect == null)
                    continue;

                Location particleLoc = baseLoc.clone().add(0, 1.0, 0);

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
                }
            }
        }

        if (sounds != null) {
            for (SoundEffect sound : sounds) {
                if (sound == null)
                    continue;

                if (sound.delay > 0) {
                    plugin.getFoliaLib().getImpl().runAtLocationLater(baseLoc, () -> {
                        playSound(player, sound);
                    }, sound.delay);
                } else {
                    playSound(player, sound);
                }
            }
        }

        if (title != null) {
            player.sendTitle(
                    plugin.getLanguage().color(PlaceholderUtil.format(plugin, player, title.title)),
                    plugin.getLanguage().color(PlaceholderUtil.format(plugin, player, title.subtitle)),
                    title.fadeIn, title.stay, title.fadeOut);
        }

        if (bossBar != null && bossBar.enabled) {
            BossBar bar = Bukkit.createBossBar(
                    plugin.getLanguage().color(PlaceholderUtil.format(plugin, player, bossBar.title)),
                    bossBar.color, bossBar.style);
            bar.setProgress(bossBar.progress);
            bar.addPlayer(player);

            plugin.getFoliaLib().getImpl().runAtEntityLater(player, () -> {
                bar.removePlayer(player);
            }, bossBar.duration);
        }

        if (floatingText != null && floatingText.enabled) {
            spawnFloatingText(player, baseLoc, floatingText);
        }

        if (entitySpawns != null) {
            for (EntitySpawn spawn : entitySpawns) {
                if (spawn == null)
                    continue;
                spawnEntities(world, baseLoc, spawn);
            }
        }

        if (blockEffects != null) {
            for (BlockEffect blockEffect : blockEffects) {
                if (blockEffect == null)
                    continue;
                applyBlockEffect(world, baseLoc, blockEffect);
            }
        }

        if (ambientEffects != null) {
            for (AmbientEffect ambient : ambientEffects) {
                if (ambient == null)
                    continue;
                createAmbientEffect(player, baseLoc, ambient);
            }
        }

        if (actionBar != null && actionBar.enabled) {
            String text = PlaceholderUtil.format(plugin, player, actionBar.text);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(plugin.getLanguage().color(text)));
        }

        if (screenEffects != null) {
            for (PotionEffect effect : screenEffects) {
                player.addPotionEffect(effect);
            }
        }
    }

    private void spawnFirework(World world, Location loc, FireworkEffect fw) {
        Firework firework = (Firework) world.spawnEntity(loc, EntityType.FIREWORK);
        FireworkMeta meta = firework.getFireworkMeta();

        org.bukkit.FireworkEffect.Type type;
        switch (fw.type) {
            case BALL_LARGE:
                type = org.bukkit.FireworkEffect.Type.BALL_LARGE;
                break;
            case STAR:
                type = org.bukkit.FireworkEffect.Type.STAR;
                break;
            case BURST:
                type = org.bukkit.FireworkEffect.Type.BURST;
                break;
            case CREEPER:
                type = org.bukkit.FireworkEffect.Type.CREEPER;
                break;
            default:
                type = org.bukkit.FireworkEffect.Type.BALL;
        }

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
        meta.setPower(Math.min(fw.power, 3));
        firework.setFireworkMeta(meta);

        firework.setMetadata("questborn_no_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
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
        Location textLoc = baseLoc.clone().add(0, text.offsetY, 0);
        String formattedText = plugin.getLanguage().color(PlaceholderUtil.format(plugin, player, text.text));

        Entity displayEntity = null;
        try {
            Class.forName("org.bukkit.entity.TextDisplay");

            displayEntity = player.getWorld().spawnEntity(textLoc, EntityType.valueOf("TEXT_DISPLAY"));
            Method setText = displayEntity.getClass().getMethod("setText", String.class);
            setText.invoke(displayEntity, formattedText);

            Class<?> billboardClass = Class.forName("org.bukkit.entity.Display$Billboard");
            Method setBillboard = displayEntity.getClass().getMethod("setBillboard", billboardClass);
            setBillboard.invoke(displayEntity, Enum.valueOf((Class<Enum>)billboardClass, "CENTER"));

            Method setBg = displayEntity.getClass().getMethod("setDefaultBackground", boolean.class);
            setBg.invoke(displayEntity, false);

        } catch (Exception e) {
            ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(textLoc, EntityType.ARMOR_STAND);
            stand.setCustomName(formattedText);
            stand.setCustomNameVisible(true);
            stand.setGravity(false);
            stand.setVisible(false);
            stand.setMarker(true);
            displayEntity = stand;
        }

        displayEntity.setInvulnerable(true);

        switch (text.animation) {
            case FLOAT:
                animateFloat(displayEntity, text.duration);
                break;
            case RISE:
                animateRise(displayEntity, text.duration);
                break;
            case SPIN:
                animateSpin(displayEntity, text.duration);
                break;
            case PULSE:
                animatePulse(displayEntity, text.duration);
                break;
        }

        Entity finalEntity = displayEntity;
        plugin.getFoliaLib().getImpl().runAtEntityLater(finalEntity, finalEntity::remove, text.duration);
    }

    private void spawnEntities(World world, Location baseLoc, EntitySpawn spawn) {
        for (int i = 0; i < spawn.count; i++) {
            double radius = 0.5;
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

            if (spawn.lifetime > 0) {
                plugin.getFoliaLib().getImpl().runAtEntityLater(entity, entity::remove, spawn.lifetime);
            }
        }
    }

    private void applyBlockEffect(World world, Location center, BlockEffect effect) {
        if (effect.type == BlockEffect.Type.TEMPORARY && effect.material != null) {
            List<org.bukkit.block.BlockState> oldStates = new ArrayList<>();
            for (int x = -effect.radius; x <= effect.radius; x++) {
                for (int y = -effect.radius; y <= effect.radius; y++) {
                    for (int z = -effect.radius; z <= effect.radius; z++) {
                        if (x * x + y * y + z * z <= effect.radius * effect.radius) {
                            org.bukkit.block.Block b = center.clone().add(x, y, z).getBlock();
                            if (b.getType() != Material.AIR) {
                                oldStates.add(b.getState());
                                b.setType(effect.material);
                            }
                        }
                    }
                }
            }
            if (effect.duration > 0) {
                plugin.getFoliaLib().getImpl().runAtLocationLater(center, () -> {
                    for (org.bukkit.block.BlockState state : oldStates) {
                        state.update(true, false);
                    }
                }, effect.duration);
            }
        } else if (effect.particle != null) {
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
        final int[] ticks = {0};
        plugin.getFoliaLib().getImpl().runAtLocationTimer(center, (task) -> {
            if (ticks[0] >= ambient.duration) {
                task.cancel();
                return;
            }

            switch (ambient.type) {
                case PARTICLE_CIRCLE:
                    createParticleCircle(player, center, ambient);
                    break;
                case HELIX:
                    createHelix(player, center, ambient, ticks[0]);
                    break;
                case SPIRAL:
                    createSpiral(player, center, ambient, ticks[0]);
                    break;
                case VORTEX:
                    createVortex(player, center, ambient, ticks[0]);
                    break;
                case FOUNTAIN:
                    createFountain(player, center, ambient, ticks[0]);
                    break;
            }

            ticks[0]++;
        }, 1L, 1L);
    }

    private void createVortex(Player player, Location center, AmbientEffect ambient, int tick) {
        double radius = ambient.radius * (1.0 - (double) tick / ambient.duration);
        if (radius < 0) radius = 0;
        double angle = 2 * Math.PI * ambient.rotations * tick / 20.0;
        double height = ambient.height * tick / ambient.duration;

        double x = radius * Math.cos(angle);
        double z = radius * Math.sin(angle);
        player.getWorld().spawnParticle(ambient.particle, center.clone().add(x, height, z), 1, 0, 0, 0, 0);
    }

    private void createFountain(Player player, Location center, AmbientEffect ambient, int tick) {
        for (int i = 0; i < 3; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double x = Math.cos(angle) * (Math.random() * ambient.radius);
            double z = Math.sin(angle) * (Math.random() * ambient.radius);
            double y = ambient.height * Math.sin(Math.PI * (tick / (double) ambient.duration));
            player.getWorld().spawnParticle(ambient.particle, center.clone().add(x, y, z), 1, 0, 0, 0, 0);
        }
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
        double progress = (double) tick / ambient.duration;
        double radius = ambient.radius * progress;
        double height = ambient.height * progress;
        double angle = 2 * Math.PI * ambient.rotations * tick / 10.0;

        double x = radius * Math.cos(angle);
        double z = radius * Math.sin(angle);

        Location particleLoc = center.clone().add(x, height, z);
        player.getWorld().spawnParticle(ambient.particle, particleLoc, 1, 0, 0, 0, 0);
    }

    private void animateFloat(Entity stand, int duration) {
        final double[] y = {stand.getLocation().getY()};
        final int[] ticks = {0};
        plugin.getFoliaLib().getImpl().runAtEntityTimer(stand, (task) -> {
            if (ticks[0] >= duration) {
                task.cancel();
                return;
            }

            double newY = y[0] + 0.05 * Math.sin(ticks[0] * 0.1);
            Location loc = stand.getLocation();
            loc.setY(newY);
            stand.teleport(loc);

            ticks[0]++;
        }, 1L, 1L);
    }

    private void animateRise(Entity stand, int duration) {
        final int[] ticks = {0};
        plugin.getFoliaLib().getImpl().runAtEntityTimer(stand, (task) -> {
            if (ticks[0] >= duration) {
                task.cancel();
                return;
            }

            Location loc = stand.getLocation();
            loc.add(0, 0.05, 0);
            stand.teleport(loc);

            ticks[0]++;
        }, 1L, 1L);
    }

    private void animateSpin(Entity stand, int duration) {
        final int[] ticks = {0};
        plugin.getFoliaLib().getImpl().runAtEntityTimer(stand, (task) -> {
            if (ticks[0] >= duration) {
                task.cancel();
                return;
            }

            Location loc = stand.getLocation();
            loc.setYaw(loc.getYaw() + 10);
            stand.teleport(loc);

            ticks[0]++;
        }, 1L, 1L);
    }

    private void animatePulse(Entity stand, int duration) {
        final int[] ticks = {0};
        plugin.getFoliaLib().getImpl().runAtEntityTimer(stand, (task) -> {
            if (ticks[0] >= duration) {
                task.cancel();
                return;
            }

            if (ticks[0] % 10 == 0) {
                stand.setCustomNameVisible(ticks[0] % 20 < 10);
            }

            ticks[0]++;
        }, 1L, 1L);
    }

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
        }
        return Color.WHITE;
    }

    private double randomOffset(double max) {
        return (Math.random() * 2 - 1) * max;
    }

    public String getId() {
        return id;
    }

    public FireworkEffect getFirework() {
        return firework;
    }

    public List<ParticleEffect> getParticles() {
        return particles;
    }

    public List<SoundEffect> getSounds() {
        return sounds;
    }

    public TitleEffect getTitle() {
        return title;
    }

    public BossBarEffect getBossBar() {
        return bossBar;
    }

    public FloatingText getFloatingText() {
        return floatingText;
    }

    public List<EntitySpawn> getEntitySpawns() {
        return entitySpawns;
    }

    public List<BlockEffect> getBlockEffects() {
        return blockEffects;
    }

    public List<AmbientEffect> getAmbientEffects() {
        return ambientEffects;
    }

    public ActionBarEffect getActionBar() {
        return actionBar;
    }

    public List<PotionEffect> getScreenEffects() {
        return screenEffects;
    }

    public Conditions getConditions() {
        return conditions;
    }

    public ExecutionSettings getExecution() {
        return execution;
    }
}
