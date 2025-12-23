package ua.woody.questborn.addons;

import org.bukkit.configuration.file.YamlConfiguration;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.addons.api.AddonContext;
import ua.woody.questborn.addons.api.QuestEngine;
import ua.woody.questborn.addons.api.QuestbornAddon;
import ua.woody.questborn.addons.engines.DefaultQuestEngine;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

public class AddonManager {

    private final QuestbornPlugin plugin;

    /** engineId -> engine */
    private final Map<String, QuestEngine> engines = new HashMap<>();

    /** addonId -> loaded addon */
    private final Map<String, LoadedAddon> loadedAddons = new HashMap<>();

    private final File addonsFolder;
    private final File addonsConfigFile;

    /* =========================================================
     *                      INIT
     * ========================================================= */

    public AddonManager(QuestbornPlugin plugin) {
        this.plugin = plugin;
        this.addonsFolder = new File(plugin.getDataFolder(), "addons");
        this.addonsConfigFile = new File(plugin.getDataFolder(), "addons.yml");

        // 🔒 ALWAYS register fallback engine
        registerEngine(new DefaultQuestEngine());
    }

    /* =========================================================
     *                  LOAD / DISABLE
     * ========================================================= */

    public void loadAll() {
        if (!addonsFolder.exists() && !addonsFolder.mkdirs()) {
            plugin.getLogger().warning(
                    "[Addons] Failed to create addons folder: " + addonsFolder.getAbsolutePath()
            );
            return;
        }

        Set<String> enabled = readEnabledAddonIds();

        File[] jars = addonsFolder.listFiles((dir, name) ->
                name.toLowerCase(Locale.ROOT).endsWith(".jar")
        );

        if (jars == null || jars.length == 0) return;

        for (File jar : jars) {
            try {
                loadOne(jar, enabled);
            } catch (Exception e) {
                plugin.getLogger().log(
                        Level.WARNING,
                        "[Addons] Failed to load " + jar.getName(),
                        e
                );
            }
        }

        // enable addons
        for (LoadedAddon a : loadedAddons.values()) {
            try {
                a.addon.onEnable();
            } catch (Exception e) {
                plugin.getLogger().log(
                        Level.WARNING,
                        "[Addons] Failed to enable addon '" + a.id + "'",
                        e
                );
            }
        }
    }

    public void disableAll() {
        for (LoadedAddon a : loadedAddons.values()) {
            try { a.addon.onDisable(); } catch (Exception ignored) {}
            try { a.classLoader.close(); } catch (Exception ignored) {}
        }

        loadedAddons.clear();

        // 🧹 keep ONLY default engine
        engines.keySet().removeIf(id -> !id.equals("default"));
    }

    /* =========================================================
     *                      ENGINES
     * ========================================================= */

    /**
     * Called ONLY by addons (or core during init)
     */
    public void registerEngine(QuestEngine engine) {
        if (engine == null || engine.getId() == null || engine.getId().isBlank()) {
            return;
        }

        String id = engine.getId().toLowerCase(Locale.ROOT);
        engines.put(id, engine);

        plugin.getLogger().info(
                "[Addons] Registered quest engine: " + id
        );
    }

    /**
     * 🔑 NEVER returns null.
     * Falls back to default engine.
     */
    public QuestEngine getEngine(String id) {
        if (id == null || id.isBlank()) {
            return engines.get("default");
        }

        return engines.getOrDefault(
                id.toLowerCase(Locale.ROOT),
                engines.get("default")
        );
    }

    public boolean hasEngine(String id) {
        if (id == null) return false;
        return engines.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public Set<String> getLoadedAddonIds() {
        return Collections.unmodifiableSet(loadedAddons.keySet());
    }

    /* =========================================================
     *                  ADDON LOADING
     * ========================================================= */

    private void loadOne(File jarFile, Set<String> enabledIds) throws Exception {
        AddonDescriptorResult res = readAddonDescriptor(jarFile);
        if (res == null || res.desc == null) {
            plugin.getLogger().warning(
                    "[Addons] Skipping " + jarFile.getName() + " (no addon.yml)"
            );
            return;
        }

        String id = res.desc.id.toLowerCase(Locale.ROOT);

        if (!enabledIds.isEmpty() && !enabledIds.contains(id)) {
            plugin.getLogger().info(
                    "[Addons] Skipping addon '" + id + "' (disabled in addons.yml)"
            );
            return;
        }

        if (loadedAddons.containsKey(id)) {
            plugin.getLogger().warning(
                    "[Addons] Duplicate addon id '" + id + "'"
            );
            return;
        }

        URLClassLoader cl = new URLClassLoader(
                new URL[]{jarFile.toURI().toURL()},
                plugin.getClass().getClassLoader()
        );

        try {
            Class<?> mainClass = Class.forName(res.desc.main, true, cl);
            Object obj = mainClass.getDeclaredConstructor().newInstance();

            if (!(obj instanceof QuestbornAddon addon)) {
                throw new IllegalStateException(
                        "Main class does not implement QuestbornAddon: " + res.desc.main
                );
            }

            File addonDataFolder = new File(addonsFolder, id);
            if (!addonDataFolder.exists()) {
                addonDataFolder.mkdirs();
            }

            addon.onLoad(
                    new AddonContext(
                            plugin,
                            plugin.getLogger(),
                            addonDataFolder
                    )
            );

            loadedAddons.put(id, new LoadedAddon(id, addon, cl));

            plugin.getLogger().info(
                    "[Addons] Loaded addon '" + id + "'"
            );

        } catch (Exception ex) {
            try { cl.close(); } catch (Exception ignored) {}
            throw ex;
        }
    }

    /* =========================================================
     *                  DESCRIPTOR
     * ========================================================= */

    private AddonDescriptorResult readAddonDescriptor(File jarFile) throws Exception {
        try (JarFile jar = new JarFile(jarFile)) {

            JarEntry entry = jar.getJarEntry("addon.yml");
            if (entry == null) entry = jar.getJarEntry("addon.yaml");

            if (entry == null) {
                Enumeration<JarEntry> en = jar.entries();
                while (en.hasMoreElements()) {
                    JarEntry e = en.nextElement();
                    if (!e.isDirectory()) {
                        String name = e.getName().toLowerCase(Locale.ROOT);
                        if (name.endsWith("addon.yml") || name.endsWith("addon.yaml")) {
                            entry = e;
                            break;
                        }
                    }
                }
            }

            if (entry == null) return null;

            try (InputStream in = jar.getInputStream(entry);
                 InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {

                YamlConfiguration y = new YamlConfiguration();
                y.load(r);

                String id = y.getString("id");
                String main = y.getString("main");

                if (id == null || id.isBlank() || main == null || main.isBlank()) {
                    return null;
                }

                return new AddonDescriptorResult(
                        new AddonDescriptor(id, main)
                );
            }
        }
    }

    /* =========================================================
     *                  CONFIG
     * ========================================================= */

    private Set<String> readEnabledAddonIds() {
        if (!addonsConfigFile.exists()) return Collections.emptySet();

        try {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(addonsConfigFile);
            Set<String> out = new HashSet<>();

            for (String s : y.getStringList("enabled")) {
                if (s != null && !s.isBlank()) {
                    out.add(s.toLowerCase(Locale.ROOT));
                }
            }
            return out;

        } catch (Exception e) {
            plugin.getLogger().warning(
                    "[Addons] Failed to read addons.yml: " + e.getMessage()
            );
            return Collections.emptySet();
        }
    }

    /* =========================================================
     *                  INTERNAL
     * ========================================================= */

    private record AddonDescriptor(String id, String main) {}

    private static final class LoadedAddon {
        final String id;
        final QuestbornAddon addon;
        final URLClassLoader classLoader;

        LoadedAddon(String id, QuestbornAddon addon, URLClassLoader classLoader) {
            this.id = id;
            this.addon = addon;
            this.classLoader = classLoader;
        }
    }

    private record AddonDescriptorResult(AddonDescriptor desc) {}
}
