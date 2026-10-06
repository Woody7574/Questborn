package ua.woody.questborn.util;

import org.bukkit.configuration.file.YamlConfiguration;
import ua.woody.questborn.QuestbornPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

public class SmartYamlUpdater {
    private static final DateTimeFormatter BACKUP_FOLDER_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static File currentSessionBackupFolder = null;
    private static long lastBackupTime = 0;

    public static YamlConfiguration loadJarResource(QuestbornPlugin plugin, String resourcePath) {
        if (plugin == null || resourcePath == null) return null;
        String path = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        try (InputStream is = plugin.getResource(path)) {
            if (is == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[SmartYamlUpdater] Failed to read JAR resource: " + path, e);
            return null;
        }
    }

    private static synchronized File getSessionBackupFolder(QuestbornPlugin plugin) {
        long now = System.currentTimeMillis();
        if (currentSessionBackupFolder == null || (now - lastBackupTime > 5000)) {
            String timestamp = LocalDateTime.now().format(BACKUP_FOLDER_FORMATTER);
            currentSessionBackupFolder = new File(new File(plugin.getDataFolder(), "backups"), timestamp);
        }
        lastBackupTime = now;
        if (!currentSessionBackupFolder.exists()) {
            currentSessionBackupFolder.mkdirs();
        }
        return currentSessionBackupFolder;
    }

    public static void createBackup(QuestbornPlugin plugin, File file) {
        if (plugin == null || file == null || !file.exists()) return;
        try {
            File sessionFolder = getSessionBackupFolder(plugin);
            String relativePath = plugin.getDataFolder().toPath().relativize(file.toPath()).toString();
            File destFile = new File(sessionFolder, relativePath);
            File parent = destFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[SmartYamlUpdater] Failed to create backup for " + file.getName(), e);
        }
    }

    public static YamlConfiguration updateAndLoad(QuestbornPlugin plugin, File file, String resourcePath) {
        if (plugin == null || file == null) {
            return new YamlConfiguration();
        }

        String path = (resourcePath != null && resourcePath.startsWith("/")) ? resourcePath.substring(1) : resourcePath;
        YamlConfiguration jarDefaults = (path != null) ? loadJarResource(plugin, path) : null;

        if (!file.exists()) {
            if (path != null) {
                try {
                    plugin.saveResource(path, false);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "[SmartYamlUpdater] Failed to save default resource: " + path, e);
                }
            }
        }

        YamlConfiguration diskConfig = new YamlConfiguration();
        if (file.exists()) {
            try {
                diskConfig.load(file);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "[SmartYamlUpdater] Failed to load YAML file: " + file.getPath(), e);
                if (jarDefaults != null) {
                    return jarDefaults;
                }
                return diskConfig;
            }
        }

        if (jarDefaults != null) {
            diskConfig.setDefaults(jarDefaults);

            int missingCount = 0;
            for (String key : jarDefaults.getKeys(true)) {
                if (jarDefaults.isConfigurationSection(key)) {
                    continue;
                }
                if (!diskConfig.contains(key, true)) {
                    diskConfig.set(key, jarDefaults.get(key));
                    missingCount++;
                }
            }

            if (missingCount > 0 && file.exists()) {
                try {
                    createBackup(plugin, file);
                    diskConfig.save(file);
                    plugin.getLogger().info(QuestbornPlugin.ORANGE + "Synced " + QuestbornPlugin.WHITE + missingCount + QuestbornPlugin.ORANGE + " missing keys into " + QuestbornPlugin.WHITE + file.getName() + QuestbornPlugin.RESET);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "[SmartYamlUpdater] Failed to sync missing keys for " + file.getName(), e);
                }
            }
        }

        return diskConfig;
    }
}
