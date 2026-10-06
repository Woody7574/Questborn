package ua.woody.questborn.integration.skin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Optional;

public class SkinsRestorerProvider {
    private final boolean enabled;
    private Object skinsRestorerAPI;

    public SkinsRestorerProvider(ua.woody.questborn.QuestbornPlugin mainPlugin) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("SkinsRestorer");
        if (mainPlugin.getConfig().getBoolean("integration.skinsrestorer.enabled", true) && plugin != null
                && plugin.isEnabled()) {
            this.enabled = true;
            try {
                Class<?> providerClass = Class.forName("net.skinsrestorer.api.SkinsRestorerProvider");
                Method getMethod = providerClass.getMethod("get");
                this.skinsRestorerAPI = getMethod.invoke(null);
            } catch (Exception e) {
                this.skinsRestorerAPI = null;
            }
        } else {
            this.enabled = false;
        }
    }

    public boolean isEnabled() {
        return enabled && skinsRestorerAPI != null;
    }

    public Optional<String[]> getSkinData(java.util.UUID uuid, String name) {
        if (!isEnabled())
            return Optional.empty();

        try {
            Method getSkinStorageMethod = skinsRestorerAPI.getClass().getMethod("getSkinStorage");
            Object skinStorage = getSkinStorageMethod.invoke(skinsRestorerAPI);

            Method getSkinMethod = null;
            Object skinResult = null;

            if (name != null) {
                try {
                    getSkinMethod = skinStorage.getClass().getMethod("getSkin", String.class);
                    skinResult = getSkinMethod.invoke(skinStorage, name);
                } catch (NoSuchMethodException e) {
                    try {
                        getSkinMethod = skinStorage.getClass().getMethod("getSkinOfUser", String.class);
                        skinResult = getSkinMethod.invoke(skinStorage, name);
                    } catch (NoSuchMethodException ignored) {
                    }
                }
            }

            if ((skinResult == null || (skinResult instanceof Optional && ((Optional<?>) skinResult).isEmpty()))
                    && uuid != null) {
                try {
                    getSkinMethod = skinStorage.getClass().getMethod("getSkin", java.util.UUID.class);
                    skinResult = getSkinMethod.invoke(skinStorage, uuid);
                } catch (NoSuchMethodException ignored) {
                }
            }

            if (skinResult instanceof Optional) {
                skinResult = ((Optional<?>) skinResult).orElse(null);
            }

            if (skinResult != null) {
                Object skinProperty = skinResult;

                try {
                    Method getPropertyMethod = skinResult.getClass().getMethod("getProperty");
                    skinProperty = getPropertyMethod.invoke(skinResult);
                } catch (NoSuchMethodException ignored) {
                }

                if (skinProperty != null) {
                    Method getValueMethod = skinProperty.getClass().getMethod("getValue");
                    String value = (String) getValueMethod.invoke(skinProperty);

                    String signature = null;
                    try {
                        Method getSignatureMethod = skinProperty.getClass().getMethod("getSignature");
                        signature = (String) getSignatureMethod.invoke(skinProperty);
                    } catch (NoSuchMethodException ignored) {
                    }

                    if (value != null) {
                        return Optional.of(new String[] { value, signature });
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger()
                    .warning("[Questborn] Error fetching skin data for " + name + "/" + uuid + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<String> getSkinTexture(java.util.UUID uuid, String name) {
        return getSkinData(uuid, name).map(data -> data[0]);
    }

    public Optional<String> getSkinTexture(String playerName) {
        return getSkinData(null, playerName).map(data -> data[0]);
    }
}
