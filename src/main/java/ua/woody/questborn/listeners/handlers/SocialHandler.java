package ua.woody.questborn.listeners.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SocialHandler extends AbstractQuestHandler implements Listener {

    private final Map<UUID, Long> cooldownMap = new HashMap<>();
    private static final long COOLDOWN_MS = 500;

    public SocialHandler(QuestbornPlugin plugin) {
        super(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        handleTeleport(event.getPlayer(), event.getCause());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerPortal(PlayerPortalEvent event) {
        handleTeleport(event.getPlayer(), event.getCause());
    }

    private void handleTeleport(Player player, PlayerTeleportEvent.TeleportCause cause) {
        // Кулдаун для запобігання дублюванню
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldownMap.containsKey(playerId)) {
            long lastTime = cooldownMap.get(playerId);
            if (now - lastTime < COOLDOWN_MS) {
                return;
            }
        }

        cooldownMap.put(playerId, now);

        QuestDefinition quest = getActiveQuest(player);
        if (quest == null) {
            return;
        }

        var objective = quest.getObjective();
        if (objective.getType() != QuestObjectiveType.TELEPORT) {
            return;
        }

        String requiredCause = objective.getCause();

        // 🔥 Якщо причина не вказана - приймаємо ВСІ телепорти
        if (requiredCause == null || requiredCause.trim().isEmpty()) {
            progress(player, quest, 1);
            return;
        }

        requiredCause = requiredCause.trim().toUpperCase();

        // 🔥 ПЕРЕВІРКА ТІЛЬКИ ДОЗВОЛЕНИХ ПРИЧИН:
        switch (requiredCause) {
            case "COMMAND":
                // Приймаємо як COMMAND, так і PLUGIN (для зручності)
                if (cause == PlayerTeleportEvent.TeleportCause.COMMAND ||
                        cause == PlayerTeleportEvent.TeleportCause.PLUGIN) {
                    progress(player, quest, 1);
                }
                return;

            case "PLUGIN":
                // Тільки PLUGIN телепорти (CMI, Essentials)
                if (cause == PlayerTeleportEvent.TeleportCause.PLUGIN) {
                    progress(player, quest, 1);
                }
                return;

            case "NETHER_PORTAL":
                if (cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
                    progress(player, quest, 1);
                }
                return;

            case "END_PORTAL":
                if (cause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
                    progress(player, quest, 1);
                }
                return;

            default:
                // 🔥 ВАЖЛИВО: Якщо вказана інша причина - НЕ приймаємо її
                return;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldownMap.remove(event.getPlayer().getUniqueId());
    }
}