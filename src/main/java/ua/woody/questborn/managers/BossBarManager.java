package ua.woody.questborn.managers;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.config.ActionBarMode;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;
import ua.woody.questborn.model.QuestStage;
import ua.woody.questborn.storage.PlayerDataStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BossBarManager implements Listener {
    private final QuestbornPlugin plugin;
    private final PlayerDataStore playerData;

    private boolean enabled;
    private String titleFormat;
    private BarColor barColor;
    private BarStyle barStyle;
    private ua.woody.questborn.util.ProgressBarUtil progressBarUtil;

    private final Map<UUID, BossBar> activeBars = new ConcurrentHashMap<>();

    private final Map<UUID, com.tcoded.folialib.wrapper.task.WrappedTask> hideTasks = new ConcurrentHashMap<>();

    private final Map<String, BarColor> questColorCache = new ConcurrentHashMap<>();

    private ActionBarMode mode = ActionBarMode.ON_PROGRESS_CHANGE;
    private int updateInterval = 40;
    private int hideDelay = 60;
    private com.tcoded.folialib.wrapper.task.WrappedTask task = null;

    private boolean autoColor;

    public BossBarManager(QuestbornPlugin plugin, PlayerDataStore playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void loadConfig() {
        var cfg = plugin.getConfig();
        this.enabled = cfg.getBoolean("bossbar.enabled", false);
        this.titleFormat = cfg.getString("bossbar.title", "&e{quest_name} &7[{progress}/{target}]");
        this.hideDelay = cfg.getInt("bossbar.hide-delay-ticks", 60);

        String colorStr = cfg.getString("bossbar.color", "YELLOW").toUpperCase();
        if (colorStr.equals("AUTO")) {
            this.autoColor = true;
            this.barColor = BarColor.WHITE;
        } else {
            this.autoColor = false;
            try {
                this.barColor = BarColor.valueOf(colorStr);
            } catch (IllegalArgumentException e) {
                this.barColor = BarColor.YELLOW;
            }
        }

        try {
            this.barStyle = BarStyle.valueOf(cfg.getString("bossbar.style", "SOLID").toUpperCase());
        } catch (IllegalArgumentException e) {
            this.barStyle = BarStyle.SOLID;
        }

        if (cfg.isConfigurationSection("bossbar.progress-bar") &&
                cfg.getConfigurationSection("bossbar.progress-bar").contains("length")) {
            this.progressBarUtil = ua.woody.questborn.util.ProgressBarUtil
                    .fromConfig(cfg.getConfigurationSection("bossbar.progress-bar"));
        } else {
            this.progressBarUtil = ua.woody.questborn.util.ProgressBarUtil
                    .fromConfig(cfg.getConfigurationSection("progress-bar"));
        }

        String modeStr = cfg.getString("bossbar.mode", "ON_PROGRESS_CHANGE");
        this.mode = ActionBarMode.fromString(modeStr);
        this.updateInterval = cfg.getInt("bossbar.update-interval-ticks", 20);

        removeAllBars();

        if (enabled) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                refreshBar(p);
            }
        }

        if (mode == ActionBarMode.STATIC) {
            startTask();
        } else {
            stopTask();
        }
    }

    public void startTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        if (mode != ActionBarMode.STATIC || !enabled) {
            return;
        }

        long period = Math.max(1, updateInterval);

        task = plugin.getFoliaLib().getImpl().runTimerAsync(() -> {
            if (!plugin.isEnabled() || plugin.getQuestManager() == null) return;
            for (UUID uuid : plugin.getQuestManager().getActiveQuestPlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    plugin.getFoliaLib().getImpl().runAtEntity(p, __task -> {
                        refreshBar(p);
                    });
                }
            }
        }, 1L, period);
    }

    public void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void removeAllBars() {
        for (BossBar bar : activeBars.values()) {
            bar.removeAll();
        }
        activeBars.clear();

        for (com.tcoded.folialib.wrapper.task.WrappedTask tid : hideTasks.values()) {
            if (tid != null) tid.cancel();
        }
        hideTasks.clear();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeBar(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
    }

    public void removeBar(Player player) {
        BossBar bar = activeBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removePlayer(player);
        }

        com.tcoded.folialib.wrapper.task.WrappedTask tid = hideTasks.remove(player.getUniqueId());
        if (tid != null) {
            tid.cancel();
        }
    }

    public void updateBar(Player player) {
        if (mode == ActionBarMode.STATIC) {
            return;
        }
        refreshBar(player);
    }

    public void refreshBar(Player player) {
        if (!enabled || !plugin.getQuestManager().getQuestProgressListener().isValidGamemode(player)) {
            removeBar(player);
            return;
        }

        var data = playerData.get(player.getUniqueId());
        String questId = data.getTrackedQuestId();

        if (questId == null || data.hasPendingReward(questId)) {
            removeBar(player);
            return;
        }

        QuestDefinition def = plugin.getQuestManager().getById(questId);
        if (def == null) {
            removeBar(player);
            return;
        }

        ua.woody.questborn.model.QuestTypeConfig typeC = plugin.getQuestManager().getQuestTypeManager().getType(def.getTypeId());
        if (typeC != null && typeC.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL) {
            int minP = def.getMinParticipants();
            if (minP > 0) {
                ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(def.getId());
                if (gp == null || (!gp.isActivationReached() && gp.getParticipantCount() < minP)) {
                    removeBar(player);
                    return;
                }
            }
        }

        BossBar bar = activeBars.get(player.getUniqueId());
        if (bar == null) {
            bar = Bukkit.createBossBar("", barColor, barStyle);
            bar.addPlayer(player);
            activeBars.put(player.getUniqueId(), bar);
        }

        if (mode == ActionBarMode.ON_PROGRESS_CHANGE) {
            scheduleHide(player);
        }

        double progress = 0.0;
        int currentVal = 0;
        int maxVal = 1;

        boolean isGlobal = (typeC != null && typeC.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL);

        QuestStage stage = null;
        if (isGlobal) {
            int personalLimit = def.getPersonalLimit();
            if (personalLimit > 0) {
                ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(questId);
                currentVal = gp.getContribution(player.getUniqueId());
                maxVal = personalLimit;
            } else {
                ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(questId);
                currentVal = gp.getGlobalProgress();
                maxVal = plugin.getQuestManager().getProgressProcessor().getGlobalGoal(def);
            }
        } else {
            if (def.hasStages()) {
                stage = plugin.getQuestManager().getCurrentStage(player.getUniqueId(), def);
                currentVal = plugin.getQuestManager().getProgressValue(data, def);
            } else {
                currentVal = plugin.getQuestManager().getProgressValue(data, def);
            }

            ua.woody.questborn.model.QuestObjective objective = null;
            if (stage != null && stage.isObjective()) {
                objective = stage.getObjective();
            } else if (!def.hasStages()) {
                objective = def.getObjective();
            }

            maxVal = plugin.getQuestManager().getTargetValue(data, def);
        }

        if (maxVal > 0) {
            progress = (double) currentVal / maxVal;
        }

        if (progress < 0)
            progress = 0.0;
        if (progress > 1.0)
            progress = 1.0;

        bar.setProgress(progress);

        int stageNum = Math.max(1, (data.getTrackedQuestId() != null && data.getQuestData(data.getTrackedQuestId()) != null ? data.getQuestData(data.getTrackedQuestId()).getCurrentStage() : 1));
        int totalStages = def.getStageCount();
        if (totalStages < 1)
            totalStages = 1;

        String title = titleFormat
            .replace("{quest_name}", def.getDisplayName())
            .replace("{stage}", String.valueOf(stageNum))
            .replace("{total_stages}", String.valueOf(totalStages))
            .replace("{progress}", String.valueOf(currentVal))
            .replace("{target}", String.valueOf(maxVal));
            if (progressBarUtil != null) {
                title = title.replace("{progressbar}", progressBarUtil.getProgressBar(currentVal, maxVal));
            } else {
                title = title.replace("{progressbar}", "");
            }

        String coloredTitle = ua.woody.questborn.lang.ColorFormatter.applyColors(title);
        if (!coloredTitle.equals(bar.getTitle())) {
            bar.setTitle(coloredTitle);
        }

        BarColor targetColor = barColor;
        if (autoColor) {
            targetColor = questColorCache.computeIfAbsent(questId, k -> {
                String lastC = ua.woody.questborn.lang.ColorFormatter.getLastColor(def.getDisplayName());
                return getBestBarColor(lastC);
            });
        }

        if (bar.getColor() != targetColor) {
            bar.setColor(targetColor);
        }

        if (bar.getStyle() != barStyle) {
            bar.setStyle(barStyle);
        }

        if (Math.abs(bar.getProgress() - progress) > 0.001) {
            bar.setProgress(progress);
        }
    }

    private BarColor getBestBarColor(String colorCode) {
        if (colorCode == null || colorCode.isEmpty())
            return BarColor.WHITE;

        if (colorCode.startsWith("<#") && colorCode.endsWith(">")) {
            String hex = colorCode.substring(2, 8);
            try {
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);
                return getClosestBarColor(r, g, b);
            } catch (Exception e) {
                return BarColor.WHITE;
            }
        }

        if (colorCode.startsWith("§x") && colorCode.length() >= 14) {
            try {
                char[] chars = colorCode.toCharArray();
                String rS = "" + chars[3] + chars[5];
                String gS = "" + chars[7] + chars[9];
                String bS = "" + chars[11] + chars[13];

                int r = Integer.parseInt(rS, 16);
                int g = Integer.parseInt(gS, 16);
                int b = Integer.parseInt(bS, 16);
                return getClosestBarColor(r, g, b);
            } catch (Exception e) {
                return BarColor.WHITE;
            }
        }

        char code = colorCode.charAt(colorCode.length() - 1);
        return switch (code) {
            case '1', '9', '3', 'b' -> BarColor.BLUE;
            case '2', 'a' -> BarColor.GREEN;
            case '4', 'c' -> BarColor.RED;
            case '5' -> BarColor.PURPLE;
            case 'd' -> BarColor.PINK;
            case '6', 'e' -> BarColor.YELLOW;
            default -> BarColor.WHITE;
        };
    }

    private BarColor getClosestBarColor(int r, int g, int b) {
        float[] hsv = rgbToHsv(r, g, b);
        float h = hsv[0];
        float s = hsv[1];
        float v = hsv[2];

        if (s < 0.15f)
            return BarColor.WHITE;

        if (v < 0.15f)
            return BarColor.WHITE;

        if (h >= 345 || h < 25)
            return BarColor.RED;
        if (h >= 25 && h < 80)
            return BarColor.YELLOW;
        if (h >= 80 && h < 160)
            return BarColor.GREEN;
        if (h >= 160 && h < 260)
            return BarColor.BLUE;
        if (h >= 260 && h < 310)
            return BarColor.PURPLE;
        if (h >= 310 && h < 345)
            return BarColor.PINK;

        return BarColor.WHITE;
    }

    private float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        float h = 0;
        float s = max == 0 ? 0 : delta / max;
        float v = max;

        if (delta != 0) {
            if (max == rf) {
                h = (gf - bf) / delta;
            } else if (max == gf) {
                h = 2 + (bf - rf) / delta;
            } else {
                h = 4 + (rf - gf) / delta;
            }
            h *= 60;
            if (h < 0)
                h += 360;
        }
        return new float[] { h, s, v };
    }

    private void scheduleHide(Player player) {
        UUID uuid = player.getUniqueId();
        com.tcoded.folialib.wrapper.task.WrappedTask existing = hideTasks.remove(uuid);
        if (existing != null) {
            existing.cancel();
        }

        com.tcoded.folialib.wrapper.task.WrappedTask tid = plugin.getFoliaLib().getImpl().runAtEntityLater(player, () -> {
            removeBar(player);
            hideTasks.remove(uuid);
        }, hideDelay);
        hideTasks.put(uuid, tid);
    }

    private int getTargetAmount(ua.woody.questborn.model.QuestObjective o) {
        if (o == null)
            return 1;
        if (o.getType() == QuestObjectiveType.TRAVEL_DISTANCE ||
                (o.getType() == QuestObjectiveType.ENTITY_RIDE && o.getDistance() > 0)) {
            return (int) o.getDistance();
        }
        return o.getAmount();
    }
}
