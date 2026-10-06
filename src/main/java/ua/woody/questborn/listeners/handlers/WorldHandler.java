package ua.woody.questborn.listeners.handlers;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WorldHandler extends AbstractQuestHandler {
    private final Map<UUID, Long> playTimeStart = new ConcurrentHashMap<>();
    private final Map<UUID, Long> sleepStartTime = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastKnownWorld = new ConcurrentHashMap<>();

    public WorldHandler(QuestbornPlugin plugin) {
        super(plugin);

        plugin.getFoliaLib().getImpl().runTimerAsync(() -> {
            if (!plugin.isEnabled()) return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();

                String currentWorld = player.getWorld().getName();
                String lastWorld = lastKnownWorld.get(uuid);

                if (lastWorld != null && !lastWorld.equals(currentWorld)) {
                    plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> {
                        onChangeDimension(player, player.getWorld());
                    });
                }
                lastKnownWorld.put(uuid, currentWorld);

                if (!playTimeStart.containsKey(uuid))
                    continue;

                QuestDefinition q = getActiveQuest(player);
                if (q == null)
                    continue;

                QuestObjective o = getCurrentObjective(player, q);
                if (o == null)
                    continue;

                if (o.getType() == QuestObjectiveType.PLAY_TIME) {
                    progress(player, q, 1);
                }
            }
        }, 20L, 20L);
    }

    public void onEnterBed(PlayerBedEnterEvent e) {
        if (e.isCancelled())
            return;

        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() == QuestObjectiveType.ENTER_BED) {
            progress(p, q, 1);
        }

        if (o.getType() == QuestObjectiveType.SLEEP_IN_BED) {
            sleepStartTime.put(p.getUniqueId(), System.currentTimeMillis());
        }
    }

    public void onLeaveBed(PlayerBedLeaveEvent e) {
        Player p = e.getPlayer();

        Long start = sleepStartTime.get(p.getUniqueId());
        if (start == null)
            return;

        long sleepTime = (System.currentTimeMillis() - start) / 1000;

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() == QuestObjectiveType.SLEEP_IN_BED) {
            if (sleepTime >= 5) {
                progress(p, q, 1);
            }
        }
    }

    public void onChangeDimension(PlayerChangedWorldEvent e) {
        onChangeDimension(e.getPlayer(), e.getPlayer().getWorld());
    }

    public void onChangeDimension(Player p, World toWorld) {
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null || o.getType() != QuestObjectiveType.CHANGE_DIMENSION) return;

        if (o.getMessage() != null && !o.getMessage().isEmpty()) {
            String target = o.getMessage().trim().toLowerCase();
            String worldName = toWorld.getName().toLowerCase();
            String envName = toWorld.getEnvironment().name().toLowerCase();

            if (worldName.equals(target) || envName.equals(target) ||
                worldName.contains(target) || envName.contains(target) || target.contains(envName) ||
                (target.contains("nether") && envName.equals("nether")) ||
                (target.contains("end") && envName.equals("the_end"))) {
                progress(p, q, 1);
            }
        } else {
            progress(p, q, 1);
        }
    }

    public void onJoinServer(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        playTimeStart.put(p.getUniqueId(), System.currentTimeMillis());

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() == QuestObjectiveType.JOIN_SERVER) {
            progress(p, q, 1);
        }
    }

    public void onQuitServer(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        playTimeStart.remove(uuid);
        sleepStartTime.remove(uuid);
        lastKnownWorld.remove(uuid);
    }

    public long getPlayTimeSeconds(Player player) {
        UUID uuid = player.getUniqueId();
        Long start = playTimeStart.get(uuid);
        if (start == null)
            return 0;
        return (System.currentTimeMillis() - start) / 1000;
    }
}
