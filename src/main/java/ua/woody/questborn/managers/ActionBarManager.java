package ua.woody.questborn.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.config.ActionBarMode;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;
import ua.woody.questborn.model.QuestStage;
import ua.woody.questborn.storage.PlayerDataStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ActionBarManager implements Listener {
    private final QuestbornPlugin plugin;
    private final PlayerDataStore playerData;

    private boolean enabled;
    private ActionBarMode mode;
    private String stageFormat;
    private String defaultFormat;
    private String materialsFormat;
    private String legacyMaterialsFormat;
    private String completeMessage;
    private int updateInterval;
    private Set<QuestObjectiveType> disabledForTypes = new HashSet<>();
    private com.tcoded.folialib.wrapper.task.WrappedTask task = null;

    private ua.woody.questborn.util.ProgressBarUtil progressBarUtil;

    private final Map<UUID, CachedActionBar> cachedBars = new ConcurrentHashMap<>();
    private final Map<UUID, Long> completeMessageLock = new ConcurrentHashMap<>();

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private static class CachedActionBar {
        Component component;
        long lastUpdateTime;
        long lastSendTime;
    }

    public ActionBarManager(QuestbornPlugin plugin, PlayerDataStore playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void loadConfig(ActionBarMode mode, int interval, List<String> disabledTypes) {
        this.enabled = plugin.getConfig().getBoolean("actionbar.enabled", true);
        this.mode = mode;
        this.updateInterval = interval;

        var cfg = plugin.getConfig();
        this.stageFormat = cfg.getString("actionbar.formats.stage",
                "{quest_name}&7: &f{progress}&7/&f{target} &7| &fStage: {stage}/{total_stages} {progressbar}");
        this.defaultFormat = cfg.getString("actionbar.formats.default",
                "{quest_name}&7: &f{progress}&7/&f{target} {progressbar}");
        this.materialsFormat = cfg.getString("actionbar.formats.materials",
                "{quest_name}&7: &fDeliver items &7| &fStage: {stage}/{total_stages}");
        this.legacyMaterialsFormat = cfg.getString("actionbar.formats.legacy-materials",
                "{quest_name}&7: &fDeliver items");

        this.completeMessage = plugin.getLanguage().tr("hud.actionbar.complete-message");

        this.disabledForTypes.clear();

        if (disabledTypes == null || disabledTypes.isEmpty()) {
            return;
        }

        for (String raw : disabledTypes) {
            if (raw == null)
                continue;

            String s = raw.trim();
            if (s.isEmpty())
                continue;

            if (s.equalsIgnoreCase("NONE"))
                continue;

            QuestObjectiveType type = null;

            try {
                type = QuestObjectiveType.fromStringStrict(s);
            } catch (Throwable ignored) {
            }

            if (type == null) {
                try {
                    type = QuestObjectiveType.valueOf(s.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (type != null) {
                disabledForTypes.add(type);
            } else {
                plugin.getLogger().warning("Unknown quest objective type in actionbar.disabled-for-types: " + raw);
            }
        }
    }

    public void configureProgressBar() {
        this.progressBarUtil = ua.woody.questborn.util.ProgressBarUtil
                .fromConfig(plugin.getConfig().getConfigurationSection("progress-bar"));
    }

    public void startTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        if (!enabled || mode != ActionBarMode.STATIC) {
            return;
        }

        long period = Math.min(40, Math.max(1, updateInterval));

        task = plugin.getFoliaLib().getImpl().runTimerAsync(() -> {
            if (!plugin.isEnabled() || plugin.getQuestManager() == null) return;
            long currentTime = System.currentTimeMillis();

            for (UUID uuid : plugin.getQuestManager().getActiveQuestPlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) continue;

                UUID playerId = p.getUniqueId();
                CachedActionBar cached = cachedBars.get(playerId);

                if (cached == null || currentTime - cached.lastUpdateTime >= updateInterval * 50L) {
                    sendStaticActionBar(p, true, currentTime);
                } else {
                    sendStaticActionBar(p, false, currentTime);
                }
            }
        }, 1L, period);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cachedBars.remove(event.getPlayer().getUniqueId());
        completeMessageLock.remove(event.getPlayer().getUniqueId());
    }

    public void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        cachedBars.clear();
    }

    private void sendStaticActionBar(Player player, boolean refreshData, long currentTime) {
        if (!enabled || mode != ActionBarMode.STATIC)
            return;

        if (!plugin.getQuestManager().getQuestProgressListener().isValidGamemode(player)) {
            cachedBars.remove(player.getUniqueId());
            plugin.getAdventure().player(player).sendActionBar(Component.empty());
            return;
        }

        Long lockTime = completeMessageLock.get(player.getUniqueId());
        if (lockTime != null) {
            if (currentTime < lockTime) {
                return;
            } else {
                completeMessageLock.remove(player.getUniqueId());
            }
        }

        CachedActionBar cached = cachedBars.get(player.getUniqueId());

        if (refreshData || cached == null) {
            var data = playerData.get(player.getUniqueId());
            String questId = data.getTrackedQuestId();

            if (questId == null) {
                cachedBars.remove(player.getUniqueId());
                plugin.getAdventure().player(player).sendActionBar(Component.empty());
                return;
            }

            QuestDefinition def = plugin.getQuestManager().getById(questId);
            if (def == null) {
                cachedBars.remove(player.getUniqueId());
                plugin.getAdventure().player(player).sendActionBar(Component.empty());
                return;
            }

            String message = formatActionBarMessage(def, data, player);

            if (message == null) {
                cachedBars.remove(player.getUniqueId());
                plugin.getAdventure().player(player).sendActionBar(Component.empty());
                return;
            }

            if (cached == null) {
                cached = new CachedActionBar();
                cachedBars.put(player.getUniqueId(), cached);
            }
            cached.component = LEGACY.deserialize(ua.woody.questborn.lang.ColorFormatter.applyColors(message));
            cached.lastUpdateTime = currentTime;
            cached.lastSendTime = currentTime;

            plugin.getAdventure().player(player).sendActionBar(cached.component);
        } else {
            if (currentTime - cached.lastSendTime >= 1900) {
                if (cached.component != null) {
                    plugin.getAdventure().player(player).sendActionBar(cached.component);
                }
                cached.lastSendTime = currentTime;
            }
        }
    }

    public void sendForPlayer(Player player) {
        if (!enabled)
            return;

        if (!plugin.getQuestManager().getQuestProgressListener().isValidGamemode(player)) {
            plugin.getAdventure().player(player).sendActionBar(Component.empty());
            return;
        }

        Long lockTime = completeMessageLock.get(player.getUniqueId());
        if (lockTime != null) {
            if (System.currentTimeMillis() < lockTime) {
                return;
            } else {
                completeMessageLock.remove(player.getUniqueId());
            }
        }

        if (mode == ActionBarMode.STATIC) {
            CachedActionBar cached = cachedBars.get(player.getUniqueId());
            if (cached != null) {
                cached.lastUpdateTime = 0;
            }
            return;
        }

        var data = playerData.get(player.getUniqueId());
        String questId = data.getTrackedQuestId();
        if (questId == null)
            return;

        QuestDefinition def = plugin.getQuestManager().getById(questId);
        if (def == null)
            return;

        String message = formatActionBarMessage(def, data, player);
        if (message == null) {
            return;
        }

        Component component = LEGACY.deserialize(ua.woody.questborn.lang.ColorFormatter.applyColors(message));
        plugin.getAdventure().player(player).sendActionBar(component);
    }

    private String formatActionBarMessage(QuestDefinition def, ua.woody.questborn.model.PlayerQuestProgress data,
            Player player) {
        String message = null;

        if (data.hasPendingReward(def.getId())) {
            return null;
        }

        ua.woody.questborn.model.QuestTypeConfig typeC = plugin.getQuestManager().getQuestTypeManager().getType(def.getTypeId());
        boolean isGlobal = (typeC != null && typeC.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL);

        if (isGlobal) {
            int minP = def.getMinParticipants();
            if (minP > 0) {
                ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(def.getId());
                if (gp == null || (!gp.isActivationReached() && gp.getParticipantCount() < minP)) {
                    return null;
                }
            }

            int progress;
            int target;
            int personalLimit = def.getPersonalLimit();
            if (personalLimit > 0) {
                ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(def.getId());
                progress = gp.getContribution(player.getUniqueId());
                target = personalLimit;
            } else {
                ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(def.getId());
                progress = gp.getGlobalProgress();
                target = plugin.getQuestManager().getProgressProcessor().getGlobalGoal(def);
            }

            message = defaultFormat
                    .replace("{quest_name}", def.getDisplayName())
                    .replace("{progress}", String.valueOf(progress))
                    .replace("{target}", String.valueOf(target))
                    .replace("{progressbar}", getProgressBar(progress, target));
            return message;
        }

        if (def.hasStages()) {
            int currentStage = (data.getTrackedQuestId() != null && data.getQuestData(data.getTrackedQuestId()) != null ? data.getQuestData(data.getTrackedQuestId()).getCurrentStage() : 1);
            int totalStages = def.getStageCount();
            QuestStage stage = def.getStage(currentStage);

            if (stage == null) {
                return null;
            }

            if (stage.isObjective()) {
                if (disabledForTypes.contains(stage.getObjective().getType())) {
                    return null;
                }

                var objective = stage.getObjective();

                int progress = plugin.getQuestManager().getProgressValue(data, def);
                int target = plugin.getQuestManager().getTargetValue(data, def);

                String formatToUse = (totalStages > 1) ? stageFormat : defaultFormat;

                message = formatToUse
                        .replace("{quest_name}", def.getDisplayName())
                        .replace("{progress}", String.valueOf(progress))
                        .replace("{target}", String.valueOf(target))
                        .replace("{stage}", String.valueOf(currentStage))
                        .replace("{total_stages}", String.valueOf(totalStages))
                        .replace("{progressbar}", getProgressBar(progress, target));
            } else if (stage.isRequiredMaterials()) {
                message = materialsFormat
                        .replace("{quest_name}", def.getDisplayName())
                        .replace("{stage}", String.valueOf(currentStage))
                        .replace("{total_stages}", String.valueOf(totalStages))
                        .replace("{progressbar}", "");
            }
        } else {
            if (def.hasRequiredMaterials()) {
                message = legacyMaterialsFormat
                        .replace("{quest_name}", def.getDisplayName())
                        .replace("{progressbar}", "");
            } else if (def.getObjective() != null) {
                if (disabledForTypes.contains(def.getObjective().getType())) {
                    return null;
                }

                var objective = def.getObjective();

                int progress = plugin.getQuestManager().getProgressValue(data, def);
                int target = plugin.getQuestManager().getTargetValue(data, def);

                message = defaultFormat
                        .replace("{quest_name}", def.getDisplayName())
                        .replace("{progress}", String.valueOf(progress))
                        .replace("{target}", String.valueOf(target))
                        .replace("{progressbar}", getProgressBar(progress, target));
            }
        }

        return message;
    }

    private int getTargetAmount(ua.woody.questborn.model.QuestObjective objective) {
        var o = objective;

        if (o.getType() == QuestObjectiveType.TRAVEL_DISTANCE ||
                (o.getType() == QuestObjectiveType.ENTITY_RIDE && o.getDistance() > 0)) {
            return (int) o.getDistance();
        }

        return o.getAmount();
    }

    public void sendCompleteMessage(Player player, QuestDefinition quest) {
        if (!enabled)
            return;

        cachedBars.remove(player.getUniqueId());

        String message = completeMessage.replace("{quest_name}", quest.getDisplayName());
        Component component = LEGACY.deserialize(ua.woody.questborn.lang.ColorFormatter.applyColors(message));
        plugin.getAdventure().player(player).sendActionBar(component);

        completeMessageLock.put(player.getUniqueId(), System.currentTimeMillis() + 2500L);
    }

    private String getProgressBar(int current, int max) {
        if (progressBarUtil == null) {
            return "";
        }
        return progressBarUtil.getProgressBar(current, max);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ActionBarMode getMode() {
        return mode;
    }

    public void setMode(ActionBarMode mode) {
        this.mode = mode;
        if (enabled && mode == ActionBarMode.STATIC) {
            startTask();
        } else {
            stopTask();
        }
    }

    public Set<QuestObjectiveType> getDisabledTypes() {
        return new HashSet<>(disabledForTypes);
    }

    public boolean isDisabledForType(QuestObjectiveType type) {
        return disabledForTypes.contains(type);
    }

    public void refreshForPlayer(Player player) {
        if (mode == ActionBarMode.STATIC) {
            CachedActionBar cached = cachedBars.get(player.getUniqueId());
            if (cached != null) {
                cached.lastUpdateTime = 0;
            }
        }
    }
}
