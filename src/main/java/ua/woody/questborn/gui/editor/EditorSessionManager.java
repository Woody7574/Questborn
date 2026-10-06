package ua.woody.questborn.gui.editor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class EditorSessionManager {
    private static final Map<UUID, Runnable> sessions = new HashMap<UUID, Runnable>();
    private static final Map<UUID, Runnable> returnActions = new HashMap<UUID, Runnable>();
    private static final java.util.Set<UUID> unsavedChanges = new java.util.HashSet<>();
    public static boolean isForceClosing = false;

    public static void setReturnAction(UUID playerId, Runnable returnAction) {
        returnActions.put(playerId, returnAction);
    }

    public static Runnable getReturnAction(UUID playerId) {
        return returnActions.get(playerId);
    }

    public static void setSession(UUID playerId, Runnable closeAction) {
        sessions.put(playerId, closeAction);
    }

    public static Runnable getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    public static void markUnsaved(UUID playerId) { unsavedChanges.add(playerId); }

    public static void clearUnsaved(UUID playerId) { unsavedChanges.remove(playerId); }

    public static boolean hasUnsaved(UUID playerId) { return unsavedChanges.contains(playerId); }

    public static void clearSession(UUID playerId) {
        sessions.remove(playerId);
        unsavedChanges.remove(playerId);
        returnActions.remove(playerId);
    }

    public static void forceCloseAll() {
        isForceClosing = true;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!sessions.containsKey(p.getUniqueId()) && !returnActions.containsKey(p.getUniqueId())) continue;
            p.closeInventory();
        }
        sessions.clear();
        unsavedChanges.clear();
        returnActions.clear();
        ua.woody.questborn.utils.SchedulerUtils.runTaskLaterGlobal(Bukkit.getPluginManager().getPlugin("Questborn"), () -> {
            isForceClosing = false;
        }, 2L);
    }
}
