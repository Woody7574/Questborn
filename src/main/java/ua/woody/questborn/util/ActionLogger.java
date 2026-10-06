package ua.woody.questborn.util;

import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

public class ActionLogger {
    private final QuestbornPlugin plugin;
    private final File logsDir;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    private boolean enabled;

    public ActionLogger(QuestbornPlugin plugin) {
        this.plugin = plugin;
        this.logsDir = new File(plugin.getDataFolder(), "logs");
        reloadConfig();
    }

    public void reloadConfig() {
        this.enabled = plugin.getConfig().getBoolean("logs.enabled", false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void logAction(Player player, String actionType, String details) {
        if (!enabled) return;

        final String playerName = player.getName();
        final long time = System.currentTimeMillis();

        plugin.getFoliaLib().getImpl().runAsync((task) -> {
            Date now = new Date(time);
            String fileName = dateFormat.format(now) + ".log";
            File logFile = new File(logsDir, fileName);

            synchronized (this) {
                try {
                    if (!logsDir.exists()) {
                        logsDir.mkdirs();
                    }
                    if (!logFile.exists()) {
                        logFile.createNewFile();
                    }

                    try (PrintWriter out = new PrintWriter(new FileWriter(logFile, true))) {
                        String logLine = String.format("[%s] [%s] %s: %s",
                                timeFormat.format(now), playerName, actionType, details);
                        out.println(logLine);
                    }
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to write to action log " + fileName, e);
                }
            }
        });
    }
}
