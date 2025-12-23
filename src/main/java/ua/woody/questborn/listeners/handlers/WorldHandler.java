package ua.woody.questborn.listeners.handlers;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldHandler extends AbstractQuestHandler {

    private final Map<UUID, Long> playTimeStart = new HashMap<>();
    private final Map<UUID, Long> sleepStartTime = new HashMap<>();

    public WorldHandler(QuestbornPlugin plugin) {
        super(plugin);

        // Запускаємо таймер для відстеження часу гри
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                if (playTimeStart.containsKey(uuid)) {
                    long startTime = playTimeStart.get(uuid);
                    long playedTime = (System.currentTimeMillis() - startTime) / 1000; // в секундах

                    // Кожну секунду перевіряємо квест PLAY_TIME
                    QuestDefinition q = getActiveQuest(player);
                    if (q != null) {
                        var o = q.getObjective();
                        if (o.getType() == QuestObjectiveType.PLAY_TIME) {
                            // Кожну секунду додаємо 1 секунду до прогресу
                            progress(player, q, 1);
                        }
                    }
                }
            }
        }, 20L, 20L); // Кожну секунду
    }

    // ==================== ENTER_BED ====================
    @EventHandler
    public void onEnterBed(PlayerBedEnterEvent e) {
        // Гравець просто клікав по ліжку → ігноруємо
        if (e.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) return;

        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();

        // Квест "Лягти в ліжко"
        if (o.getType() == QuestObjectiveType.ENTER_BED) {
            progress(p, q, 1);
        }

        // Якщо квест на "спати X секунд" — старт трекінгу тільки якщо він реально ліг
        if (o.getType() == QuestObjectiveType.SLEEP_IN_BED) {
            sleepStartTime.put(p.getUniqueId(), System.currentTimeMillis());
        }
    }


    // ==================== SLEEP_IN_BED ====================
    @EventHandler
    public void onLeaveBed(PlayerBedLeaveEvent e) {
        Player p = e.getPlayer();

        if (sleepStartTime.containsKey(p.getUniqueId())) {
            long sleepStart = sleepStartTime.get(p.getUniqueId());
            long sleepTime = (System.currentTimeMillis() - sleepStart) / 1000; // в секундах

            QuestDefinition q = getActiveQuest(p);
            if (q != null) {
                var o = q.getObjective();
                if (o.getType() == QuestObjectiveType.SLEEP_IN_BED) {
                    // Перевіряємо, чи гравець проспав достатньо довго (наприклад, мінімум 5 секунд)
                    if (sleepTime >= 5) {
                        progress(p, q, 1);
                    }
                }
            }

            sleepStartTime.remove(p.getUniqueId());
        }
    }

    // ==================== CHANGE_DIMENSION ====================
    @EventHandler
    public void onChangeDimension(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() == QuestObjectiveType.CHANGE_DIMENSION) {
            World.Environment fromEnv = e.getFrom().getEnvironment();
            World.Environment toEnv = p.getWorld().getEnvironment();

            // Якщо потрібно фільтрувати за конкретними вимірами
            if (o.getMessage() != null && !o.getMessage().isEmpty()) {
                String targetDimension = o.getMessage().toLowerCase();
                String actualDimension = toEnv.name().toLowerCase();

                if (actualDimension.contains(targetDimension) || targetDimension.contains(actualDimension)) {
                    progress(p, q, 1);
                }
            } else {
                // Будь-яка зміна виміру
                progress(p, q, 1);
            }
        }
    }

    // ==================== JOIN_SERVER ====================
    @EventHandler
    public void onJoinServer(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        // Записуємо час початку гри
        playTimeStart.put(p.getUniqueId(), System.currentTimeMillis());

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() == QuestObjectiveType.JOIN_SERVER) {
            progress(p, q, 1);
        }
    }

    // ==================== PLAY_TIME (обробляється в таймері вище) ====================

    @EventHandler
    public void onQuitServer(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        // Видаляємо дані про час гри при виході
        playTimeStart.remove(uuid);
        sleepStartTime.remove(uuid);
    }

    // ==================== Допоміжний метод для перевірки часу гри ====================
    public long getPlayTimeSeconds(Player player) {
        UUID uuid = player.getUniqueId();
        if (playTimeStart.containsKey(uuid)) {
            return (System.currentTimeMillis() - playTimeStart.get(uuid)) / 1000;
        }
        return 0;
    }
}