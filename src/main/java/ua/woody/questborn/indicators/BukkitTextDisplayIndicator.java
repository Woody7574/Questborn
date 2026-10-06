package ua.woody.questborn.indicators;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.lang.ColorFormatter;

public class BukkitTextDisplayIndicator implements QuestIndicator {
    private final QuestbornPlugin plugin;
    private final Player receiver;
    private Entity displayEntity;
    private boolean valid = true;
    private boolean isTextDisplay;
    private boolean bobbing = true;

    public BukkitTextDisplayIndicator(QuestbornPlugin plugin, Player player, Location loc, String text) {
        this.plugin = plugin;
        this.receiver = player;
        spawn(loc, text);
    }

    private void spawn(Location loc, String text) {
        try {
            String formattedText = ColorFormatter.applyColors(text);

            if (isVersionAtLeast(20) || (isVersionAtLeast(19) && isMinorVersionAtLeast(4))) {
                isTextDisplay = true;
                EntityType textDisplayType = EntityType.valueOf("TEXT_DISPLAY");
                displayEntity = loc.getWorld().spawnEntity(loc, textDisplayType);

                displayEntity.getClass().getMethod("setText", String.class).invoke(displayEntity, formattedText);

                try {
                    Class<?> displayClass = displayEntity.getClass();
                    Class<?> billboardEnum = Class.forName("org.bukkit.entity.Display$Billboard");
                    Object center = null;
                    for (Object constant : billboardEnum.getEnumConstants()) {
                        if (constant.toString().equals("CENTER")) {
                            center = constant;
                            break;
                        }
                    }
                    if (center != null) {
                        displayClass.getMethod("setBillboard", billboardEnum).invoke(displayEntity, center);
                    }
                    displayClass.getMethod("setSeeThrough", Boolean.TYPE).invoke(displayEntity, false);
                    displayClass.getMethod("setShadowed", Boolean.TYPE).invoke(displayEntity, true);

                    try {
                        Class<?> colorClass = Class.forName("org.bukkit.Color");
                        Object transparent = colorClass.getMethod("fromARGB", Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE).invoke(null, 0, 0, 0, 0);
                        displayClass.getMethod("setBackgroundColor", colorClass).invoke(displayEntity, transparent);
                    } catch (Exception ignored) {}

                    try {
                        displayClass.getMethod("setTeleportDuration", Integer.TYPE).invoke(displayEntity, 5);
                    } catch (Exception ignored) {}

                    try {
                        Object transform = displayClass.getMethod("getTransformation").invoke(displayEntity);
                        Class<?> transformClass = transform.getClass();
                        Class<?> vector3fClass = Class.forName("org.joml.Vector3f");
                        Class<?> quaternionfClass = Class.forName("org.joml.Quaternionf");
                        Object translation = transformClass.getMethod("getTranslation").invoke(transform);
                        Object leftRot = transformClass.getMethod("getLeftRotation").invoke(transform);
                        Object rightRot = transformClass.getMethod("getRightRotation").invoke(transform);
                        Object scale = vector3fClass.getConstructor(Float.TYPE, Float.TYPE, Float.TYPE).newInstance(1.5f, 1.5f, 1.5f);
                        Object newTransform = transformClass.getConstructor(vector3fClass, quaternionfClass, vector3fClass, quaternionfClass)
                                .newInstance(translation, leftRot, scale, rightRot);
                        displayClass.getMethod("setTransformation", transformClass).invoke(displayEntity, newTransform);
                    } catch (Exception ignored) {}

                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed to apply TextDisplay styling: " + ex.getMessage());
                }

                displayEntity.getClass().getMethod("setVisibleByDefault", Boolean.TYPE).invoke(displayEntity, false);
                receiver.getClass().getMethod("showEntity", Plugin.class, Entity.class).invoke(receiver, plugin, displayEntity);

                try {
                    displayEntity.getClass().getMethod("setPersistent", Boolean.TYPE).invoke(displayEntity, false);
                    PersistentDataContainer pdc = (PersistentDataContainer) displayEntity.getClass().getMethod("getPersistentDataContainer").invoke(displayEntity);
                    NamespacedKey key = new NamespacedKey(plugin, "questborn_indicator");
                    pdc.set(key, PersistentDataType.STRING, "true");
                } catch (Exception ignored) {}
            } else {
                isTextDisplay = false;
                ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
                stand.setVisible(false);
                stand.setMarker(true);
                stand.setGravity(false);
                stand.setSmall(true);
                stand.setCustomName(formattedText);
                stand.setCustomNameVisible(true);

                try {
                    stand.setPersistent(false);
                    stand.getPersistentDataContainer().set(new NamespacedKey(plugin, "questborn_indicator"), PersistentDataType.STRING, "true");
                } catch (Exception ignored) {}

                displayEntity = stand;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.getUniqueId().equals(receiver.getUniqueId())) {
                        try {
                            p.getClass().getMethod("hideEntity", Plugin.class, Entity.class).invoke(p, plugin, stand);
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error spawning Bukkit indicator: " + e.getMessage());
            if (displayEntity != null) {
                displayEntity.remove();
            }
            valid = false;
        }
    }

    @Override
    public void setBobbing(boolean bobbing) {
        this.bobbing = bobbing;
    }

    @Override
    public void updateLocation(Location loc) {
        if (!isValid()) return;

        double yOffset = 0.0;
        if (isTextDisplay && bobbing) {
            long time = System.currentTimeMillis();
            yOffset = Math.sin(time * 0.0015) * 0.20;
        }

        Location targetLoc = loc.clone().add(0, yOffset, 0);

        if (displayEntity.getLocation().distanceSquared(targetLoc) > 0.001) {
            displayEntity.teleport(targetLoc);
        }
    }

    @Override
    public void updateName(String name) {
        if (!isValid()) return;
        String formattedName = ColorFormatter.applyColors(name);
        try {
            if (isTextDisplay) {
                displayEntity.getClass().getMethod("setText", String.class).invoke(displayEntity, formattedName);
            } else {
                ArmorStand stand = (ArmorStand) displayEntity;
                if (!formattedName.equals(stand.getCustomName())) {
                    stand.setCustomName(formattedName);
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void remove() {
        if (isValid()) {
            displayEntity.remove();
            displayEntity = null;
            valid = false;
        }
    }

    @Override
    public boolean isValid() {
        return valid && displayEntity != null && displayEntity.isValid() && receiver.isOnline();
    }

    @Override
    public Entity getBukkitEntity() {
        return displayEntity;
    }

    private boolean isVersionAtLeast(int minor) {
        try {
            String[] parts = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
            if (parts.length >= 2) {
                int major = Integer.parseInt(parts[0]);
                if (major > 1) return true;
                return Integer.parseInt(parts[1]) >= minor;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isMinorVersionAtLeast(int subMinor) {
        try {
            String[] parts = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
            if (parts.length > 0 && Integer.parseInt(parts[0]) > 1) return true;
            if (parts.length >= 3) {
                return Integer.parseInt(parts[2]) >= subMinor;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
