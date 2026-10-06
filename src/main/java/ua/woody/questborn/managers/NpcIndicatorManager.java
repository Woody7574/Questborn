package ua.woody.questborn.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.indicators.BukkitTextDisplayIndicator;
import ua.woody.questborn.indicators.IndicatorStatusCalculator;
import ua.woody.questborn.indicators.NMSPacketIndicator;
import ua.woody.questborn.indicators.QuestIndicator;
import ua.woody.questborn.integration.npc.NpcIntegrationManager;
import ua.woody.questborn.integration.npc.NpcProvider;

import java.util.List;
import ua.woody.questborn.model.PlayerQuestProgress;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NpcIndicatorManager implements Listener {
    private final QuestbornPlugin plugin;

    private final Map<UUID, Map<String, QuestIndicator>> playerIndicators = new ConcurrentHashMap<>();
    private final Map<String, NpcInfo> activeNpcs = new ConcurrentHashMap<>();

    private final IndicatorStatusCalculator calculator;

    private boolean enabled = true;
    private double viewDistanceSq = 400.0;
    private double heightOffset = 0.8;
    private int tickCounter = 0;

    public NpcIndicatorManager(QuestbornPlugin plugin) {
        this.plugin = plugin;
        this.calculator = new IndicatorStatusCalculator(plugin);

        loadConfig();
        calculator.loadIcons();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        plugin.getFoliaLib().getImpl().runTimerAsync(() -> this.asyncTick(), 20L, 10L);

        plugin.getFoliaLib().getImpl().runTimer(() -> this.syncTick(), 20L, 4L);

        cleanupOrphanedIndicators();
    }

    public void cleanupOrphanedIndicators() {
        if (!isVersionAtLeast(17)) return;

        plugin.getFoliaLib().getImpl().runNextTick(__ -> {
            NamespacedKey key = new NamespacedKey(plugin, "questborn_indicator");
            for (World world : Bukkit.getWorlds()) {
                try {
                    for (Entity entity : world.getEntities()) {
                        if (!(entity instanceof ArmorStand) && !entity.getType().name().equals("TEXT_DISPLAY")) continue;
                        try {
                            PersistentDataContainer pdc = (PersistentDataContainer) entity.getClass().getMethod("getPersistentDataContainer").invoke(entity);
                            if (pdc.has(key, PersistentDataType.STRING)) {
                                entity.remove();
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    public void reload() {
        loadConfig();
        calculator.loadIcons();
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeAllIndicatorsSync(player);
        }
    }

    private void loadConfig() {
        this.enabled = plugin.getConfig().getBoolean("npc-indicators.enabled", true);
        double viewDistance = plugin.getConfig().getDouble("npc-indicators.view-distance", 20.0);
        this.viewDistanceSq = viewDistance * viewDistance;
        this.heightOffset = plugin.getConfig().getDouble("npc-indicators.offset-y", 0.8);
    }

    public void shutdown() {
        for (Map<String, QuestIndicator> map : playerIndicators.values()) {
            for (QuestIndicator display : map.values()) {
                if (display != null) {
                    display.remove();
                }
            }
        }
        playerIndicators.clear();
        pendingUpdates.clear();
        activeNpcs.clear();
        org.bukkit.event.HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeAllIndicatorsSync(event.getPlayer());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        removeAllIndicatorsSync(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!isVersionAtLeast(17) || (isVersionAtLeast(19) && isMinorVersionAtLeast(4))) {
            return;
        }
        Player newPlayer = event.getPlayer();
        for (Map<String, QuestIndicator> map : playerIndicators.values()) {
            for (QuestIndicator indicator : map.values()) {
                if (indicator instanceof BukkitTextDisplayIndicator) {
                    Entity entity = indicator.getBukkitEntity();
                    if (entity != null && entity.isValid()) {
                        try {
                            newPlayer.getClass().getMethod("hideEntity", Plugin.class, Entity.class).invoke(newPlayer, plugin, entity);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    private void removeAllIndicatorsSync(Player player) {
        pendingUpdates.remove(player.getUniqueId());
        Map<String, QuestIndicator> indicators = playerIndicators.remove(player.getUniqueId());
        if (indicators != null) {
            plugin.getFoliaLib().getImpl().runNextTick(__ -> {
                for (QuestIndicator display : indicators.values()) {
                    if (display != null) display.remove();
                }
            });
        }
    }

    private final Map<UUID, Map<String, IndicatorUpdateTask>> pendingUpdates = new ConcurrentHashMap<>();

    private static class IndicatorUpdateTask {
        public final Location targetLoc;
        public final List<String> icons;
        public final boolean validNpc;

        public IndicatorUpdateTask(Location targetLoc, List<String> icons, boolean validNpc) {
            this.targetLoc = targetLoc;
            this.icons = icons;
            this.validNpc = validNpc;
        }
    }

    private void asyncTick() {
        if (!plugin.isEnabled() || !enabled) return;

        if (tickCounter++ % 5 == 0) {
            updateNpcLocations();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            PlayerQuestProgress data = plugin.getPlayerDataStore().get(uuid);
            if (data == null) continue;

            Location playerLoc = player.getLocation();
            Map<String, IndicatorUpdateTask> playerUpdates = new HashMap<>();

            for (Map.Entry<String, NpcInfo> entry : activeNpcs.entrySet()) {
                String npcId = entry.getKey();
                NpcInfo info = entry.getValue();
                Location npcLoc = info.location;

                if (npcLoc.getWorld() == null || !npcLoc.getWorld().equals(playerLoc.getWorld()) || npcLoc.distanceSquared(playerLoc) > viewDistanceSq) {
                    playerUpdates.put(npcId, new IndicatorUpdateTask(null, null, false));
                    continue;
                }

                List<String> statusIcons = calculator.calculateStatus(player, data, npcId);

                if (statusIcons == null || statusIcons.isEmpty()) {
                    playerUpdates.put(npcId, new IndicatorUpdateTask(null, null, false));
                    continue;
                }

                double actualOffset = info.offset != null ? info.offset : heightOffset;
                Location targetLoc = npcLoc.clone().add(0, info.height + actualOffset, 0);

                playerUpdates.put(npcId, new IndicatorUpdateTask(targetLoc, statusIcons, true));
            }

            pendingUpdates.put(uuid, playerUpdates);
        }
    }

    private void syncTick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Map<String, IndicatorUpdateTask> updates = pendingUpdates.remove(uuid);
            Map<String, QuestIndicator> indicators = playerIndicators.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

            if (updates != null) {
                indicators.entrySet().removeIf(indEntry -> {
                    if (!updates.containsKey(indEntry.getKey())) {
                        if (indEntry.getValue() != null) indEntry.getValue().remove();
                        return true;
                    }
                    return false;
                });

                for (Map.Entry<String, IndicatorUpdateTask> entry : updates.entrySet()) {
                    String npcId = entry.getKey();
                    IndicatorUpdateTask task = entry.getValue();
                    QuestIndicator display = indicators.get(npcId);

                    if (!task.validNpc) {
                        if (display != null) {
                            display.remove();
                            indicators.remove(npcId);
                        }
                        continue;
                    }

                    if (task.icons != null && !task.icons.isEmpty()) {
                        long time = System.currentTimeMillis() + Math.abs(npcId.hashCode());
                        int cycle = (int) ((time / 2000) % task.icons.size());
                        String currentIcon = task.icons.get(cycle);
                        boolean bob = calculator.shouldBob(currentIcon);

                        if (display == null || !display.isValid()) {
                            if (display != null) display.remove();
                            display = isVersionAtLeast(17) ?
                                new BukkitTextDisplayIndicator(plugin, player, task.targetLoc, currentIcon) :
                                new NMSPacketIndicator(plugin, player, task.targetLoc, currentIcon);
                            indicators.put(npcId, display);
                        } else {
                            display.updateName(currentIcon);
                        }
                        display.setBobbing(bob);
                        display.updateLocation(task.targetLoc);
                    }
                }
            } else {
                for (Map.Entry<String, QuestIndicator> entry : indicators.entrySet()) {
                    String npcId = entry.getKey();
                    QuestIndicator display = entry.getValue();
                    if (display != null && display.isValid() && display.getBukkitEntity() != null) {
                        NpcInfo info = activeNpcs.get(npcId);
                        if (info != null) {
                            double actualOffset = info.offset != null ? info.offset : heightOffset;
                            Location targetBaseLoc = info.location.clone().add(0, info.height + actualOffset, 0);
                            display.updateLocation(targetBaseLoc);
                        }
                    }
                }
            }
        }
    }

    private void updateNpcLocations() {
        activeNpcs.clear();
        try {
            NpcIntegrationManager npcManager = plugin.getNpcIntegrationManager();
            if (npcManager == null || npcManager.getProvider() == null) return;

            NpcProvider provider = npcManager.getProvider();
            for (String id : provider.getAllNpcIds()) {
                Location loc = provider.getStoredLocation(id);
                if (loc != null) {
                    double height = provider.getHeight(id);
                    NpcInfo info = new NpcInfo(loc, height);
                    ua.woody.questborn.model.NpcConfig config = plugin.getNpcManager().getConfigByNpcId(id);
                    if (config != null) {
                        info.offset = config.getIndicatorHeightOffset();
                    }
                    activeNpcs.put(id, info);
                }
            }
        } catch (Exception ignored) {}
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

    private static class NpcInfo {
        Location location;
        double height;
        Double offset;

        NpcInfo(Location location, double height) {
            this.location = location;
            this.height = height;
        }
    }
}
