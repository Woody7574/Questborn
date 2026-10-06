package ua.woody.questborn.utils;

import com.tcoded.folialib.FoliaLib;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SchedulerUtils {
    private static FoliaLib foliaLib;

    private static FoliaLib getFoliaLib(Plugin plugin) {
        if (foliaLib == null) {
            foliaLib = new FoliaLib(plugin);
        }
        return foliaLib;
    }

    public static void runTask(Plugin plugin, Player player, Runnable runnable) {
        if (player != null && player.isOnline()) {
            getFoliaLib(plugin).getImpl().runAtEntity(player, t -> runnable.run());
        } else {
            getFoliaLib(plugin).getImpl().runNextTick(t -> runnable.run());
        }
    }

    public static void runTaskGlobal(Plugin plugin, Runnable runnable) {
        getFoliaLib(plugin).getImpl().runNextTick(t -> runnable.run());
    }

    public static void runTaskLater(Plugin plugin, Player player, Runnable runnable, long delayTicks) {
        if (player != null && player.isOnline()) {
            getFoliaLib(plugin).getImpl().runAtEntityLater(player, t -> runnable.run(), delayTicks);
        } else {
            getFoliaLib(plugin).getImpl().runLater(t -> runnable.run(), delayTicks);
        }
    }

    public static void runTaskLaterGlobal(Plugin plugin, Runnable runnable, long delayTicks) {
        getFoliaLib(plugin).getImpl().runLater(t -> runnable.run(), delayTicks);
    }

    public static void runTaskAsynchronously(Plugin plugin, Runnable runnable) {
        getFoliaLib(plugin).getImpl().runAsync(t -> runnable.run());
    }
}
