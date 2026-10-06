package ua.woody.questborn.indicators;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.lang.ColorFormatter;

import java.lang.reflect.Constructor;

public class NMSPacketIndicator implements QuestIndicator {
    private final QuestbornPlugin plugin;
    private final Player receiver;
    private int entityId;
    private boolean valid = true;
    private Location lastLoc;
    private Object nmsStand;

    private static Class<?> WrapperPlayServerSpawnEntityLiving;
    private static Class<?> WrapperPlayServerEntityMetadata;
    private static Class<?> WrapperPlayServerEntityTeleport;
    private static Class<?> WrapperPlayServerEntityDestroy;
    private static Class<?> EntityArmorStandClass;
    private static Class<?> CraftWorldClass;
    private static Class<?> CraftPlayerClass;
    private static Class<?> PacketClass;
    private static Class<?> ChatSerializerClass;
    private static Class<?> IChatBaseComponentClass;
    private static boolean reflectionLoaded;

    public NMSPacketIndicator(QuestbornPlugin plugin, Player player, Location loc, String text) {
        this.plugin = plugin;
        this.receiver = player;
        this.lastLoc = loc;

        if (!reflectionLoaded) {
            loadReflection();
        }

        if (reflectionLoaded) {
            spawn(loc, text);
        } else {
            this.valid = false;
        }
    }

    private void loadReflection() {
        try {
            String pkgName = Bukkit.getServer().getClass().getPackage().getName();
            String version = pkgName.substring(pkgName.lastIndexOf('.') + 1);
            if (!version.equals("v1_16_R3")) {
                return;
            }
            EntityArmorStandClass = Class.forName("net.minecraft.server." + version + ".EntityArmorStand");
            WrapperPlayServerSpawnEntityLiving = Class.forName("net.minecraft.server." + version + ".PacketPlayOutSpawnEntityLiving");
            WrapperPlayServerEntityMetadata = Class.forName("net.minecraft.server." + version + ".PacketPlayOutEntityMetadata");
            WrapperPlayServerEntityTeleport = Class.forName("net.minecraft.server." + version + ".PacketPlayOutEntityTeleport");
            WrapperPlayServerEntityDestroy = Class.forName("net.minecraft.server." + version + ".PacketPlayOutEntityDestroy");
            PacketClass = Class.forName("net.minecraft.server." + version + ".Packet");
            IChatBaseComponentClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            ChatSerializerClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            CraftWorldClass = Class.forName("org.bukkit.craftbukkit." + version + ".CraftWorld");
            CraftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            reflectionLoaded = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void spawn(Location loc, String text) {
        try {
            Object craftWorld = CraftWorldClass.cast(loc.getWorld());
            Object worldServer = CraftWorldClass.getMethod("getHandle").invoke(craftWorld);
            Constructor<?> constructor = EntityArmorStandClass.getConstructor(Class.forName("net.minecraft.server.v1_16_R3.World"), Double.TYPE, Double.TYPE, Double.TYPE);
            this.nmsStand = constructor.newInstance(worldServer, loc.getX(), loc.getY(), loc.getZ());
            this.entityId = (Integer) EntityArmorStandClass.getMethod("getId").invoke(this.nmsStand);

            EntityArmorStandClass.getMethod("setInvisible", Boolean.TYPE).invoke(this.nmsStand, true);
            String formattedText = ColorFormatter.applyColors(text);
            LegacyComponentSerializer serializer = LegacyComponentSerializer.builder().character('\u00a7').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
            TextComponent component = serializer.deserialize(formattedText);
            String json = GsonComponentSerializer.gson().serialize(component);
            Object chatComp = ChatSerializerClass.getMethod("a", String.class).invoke(null, json);
            EntityArmorStandClass.getMethod("setCustomName", IChatBaseComponentClass).invoke(this.nmsStand, chatComp);
            EntityArmorStandClass.getMethod("setCustomNameVisible", Boolean.TYPE).invoke(this.nmsStand, true);
            EntityArmorStandClass.getMethod("setSmall", Boolean.TYPE).invoke(this.nmsStand, true);
            EntityArmorStandClass.getMethod("setMarker", Boolean.TYPE).invoke(this.nmsStand, true);
            EntityArmorStandClass.getMethod("setNoGravity", Boolean.TYPE).invoke(this.nmsStand, true);

            Object spawnPacket = WrapperPlayServerSpawnEntityLiving.getConstructor(Class.forName("net.minecraft.server.v1_16_R3.EntityLiving")).newInstance(this.nmsStand);
            Object dataWatcher = EntityArmorStandClass.getMethod("getDataWatcher").invoke(this.nmsStand);
            Object metaPacket = WrapperPlayServerEntityMetadata.getConstructor(Integer.TYPE, Class.forName("net.minecraft.server.v1_16_R3.DataWatcher"), Boolean.TYPE).newInstance(this.entityId, dataWatcher, true);

            sendPacket(spawnPacket);
            sendPacket(metaPacket);
        } catch (Exception e) {
            plugin.getLogger().warning("Reflection error spawning indicator: " + e.getMessage());
            this.valid = false;
        }
    }

    @Override
    public void setBobbing(boolean bobbing) {
    }

    @Override
    public void updateLocation(Location loc) {
        if (!isValid() || !loc.equals(this.lastLoc)) {
            this.lastLoc = loc;
            try {
                EntityArmorStandClass.getMethod("setLocation", Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE)
                        .invoke(this.nmsStand, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                Object teleportPacket = WrapperPlayServerEntityTeleport.getConstructor(Class.forName("net.minecraft.server.v1_16_R3.Entity")).newInstance(this.nmsStand);
                sendPacket(teleportPacket);
            } catch (Exception e) {
                plugin.getLogger().warning("Reflection error updating location: " + e.getMessage());
            }
        }
    }

    @Override
    public void updateName(String name) {
        if (!isValid()) return;
        try {
            LegacyComponentSerializer serializer = LegacyComponentSerializer.builder().character('\u00a7').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
            String formattedName = ColorFormatter.applyColors(name);
            TextComponent component = serializer.deserialize(formattedName);
            String json = GsonComponentSerializer.gson().serialize(component);
            Object chatComp = ChatSerializerClass.getMethod("a", String.class).invoke(null, json);
            EntityArmorStandClass.getMethod("setCustomName", IChatBaseComponentClass).invoke(this.nmsStand, chatComp);
            Object dataWatcher = EntityArmorStandClass.getMethod("getDataWatcher").invoke(this.nmsStand);
            Object metaPacket = WrapperPlayServerEntityMetadata.getConstructor(Integer.TYPE, Class.forName("net.minecraft.server.v1_16_R3.DataWatcher"), Boolean.TYPE).newInstance(this.entityId, dataWatcher, true);
            sendPacket(metaPacket);
        } catch (Exception e) {
            plugin.getLogger().warning("Reflection error updating name: " + e.getMessage());
        }
    }

    @Override
    public void remove() {
        if (!isValid()) return;
        try {
            Object destroyPacket = WrapperPlayServerEntityDestroy.getConstructor(int[].class).newInstance((Object) new int[]{this.entityId});
            sendPacket(destroyPacket);
            this.valid = false;
        } catch (Exception e) {
            plugin.getLogger().warning("Reflection error removing indicator: " + e.getMessage());
        }
    }

    @Override
    public boolean isValid() {
        return this.valid && this.receiver.isOnline();
    }

    @Override
    public Entity getBukkitEntity() {
        return null;
    }

    private void sendPacket(Object packet) {
        try {
            Object craftPlayer = CraftPlayerClass.cast(this.receiver);
            Object entityPlayer = CraftPlayerClass.getMethod("getHandle").invoke(craftPlayer);
            Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);
            playerConnection.getClass().getMethod("sendPacket", PacketClass).invoke(playerConnection, packet);
        } catch (Exception e) {
            plugin.getLogger().warning("Reflection error sending packet: " + e.getMessage());
        }
    }
}
