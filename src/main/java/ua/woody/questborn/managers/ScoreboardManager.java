package ua.woody.questborn.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;
import ua.woody.questborn.model.QuestStage;
import ua.woody.questborn.storage.PlayerDataStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager implements Listener {
    private final QuestbornPlugin plugin;
    private final PlayerDataStore playerData;

    private boolean enabled;
    private String title;
    private List<String> lines;
    private ua.woody.questborn.util.ProgressBarUtil progressBarUtil;

    private final Map<UUID, ua.woody.questborn.util.QuestBoard> activeBoards = new ConcurrentHashMap<>();

    private ua.woody.questborn.config.ActionBarMode mode = ua.woody.questborn.config.ActionBarMode.ON_PROGRESS_CHANGE;
    private int updateInterval = 20;
    private int hideDelay = 60;
    private boolean hideScores = true;
    private com.tcoded.folialib.wrapper.task.WrappedTask task = null;

    private final Map<UUID, com.tcoded.folialib.wrapper.task.WrappedTask> hideTasks = new ConcurrentHashMap<>();

    public ScoreboardManager(QuestbornPlugin plugin, PlayerDataStore playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void loadConfig() {
        var cfg = plugin.getConfig();
        this.enabled = cfg.getBoolean("scoreboard.enabled", false);
        this.title = cfg.getString("scoreboard.title", "&6&lQUESTBORN");
        this.lines = cfg.getStringList("scoreboard.lines");

        String modeStr = cfg.getString("scoreboard.mode", "ON_PROGRESS_CHANGE");
        this.mode = ua.woody.questborn.config.ActionBarMode.fromString(modeStr);
        this.updateInterval = cfg.getInt("scoreboard.update-interval-ticks", 20);
        this.hideDelay = cfg.getInt("scoreboard.hide-delay-ticks", 60);
        this.hideScores = cfg.getBoolean("scoreboard.hide-scores", true);

        if (cfg.isConfigurationSection("scoreboard.progress-bar") &&
                cfg.getConfigurationSection("scoreboard.progress-bar").contains("length")) {
            this.progressBarUtil = ua.woody.questborn.util.ProgressBarUtil
                    .fromConfig(cfg.getConfigurationSection("scoreboard.progress-bar"));
        } else {
            this.progressBarUtil = ua.woody.questborn.util.ProgressBarUtil
                    .fromConfig(cfg.getConfigurationSection("progress-bar"));
        }

        removeAllBoards();
        if (enabled) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                refreshBoard(p);
            }
        }

        if (mode == ua.woody.questborn.config.ActionBarMode.STATIC) {
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

        if (mode != ua.woody.questborn.config.ActionBarMode.STATIC || !enabled) {
            return;
        }

        long period = Math.max(1, updateInterval);

        task = plugin.getFoliaLib().getImpl().runTimerAsync(() -> {
            if (!plugin.isEnabled() || plugin.getQuestManager() == null) return;
            for (UUID uuid : plugin.getQuestManager().getActiveQuestPlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    plugin.getFoliaLib().getImpl().runAtEntity(p, __task -> {
                        refreshBoard(p);
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

    public void removeAllBoards() {
        for (ua.woody.questborn.util.QuestBoard board : activeBoards.values()) {
            if (board != null && !board.isDeleted()) {
                board.delete();
            }
        }
        activeBoards.clear();

        for (com.tcoded.folialib.wrapper.task.WrappedTask tid : hideTasks.values()) {
            if (tid != null) tid.cancel();
        }
        hideTasks.clear();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeBoard(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
    }

    public void removeBoard(Player player) {
        ua.woody.questborn.util.QuestBoard board = activeBoards.remove(player.getUniqueId());
        if (board != null && !board.isDeleted()) {
            board.delete();
        }

        com.tcoded.folialib.wrapper.task.WrappedTask tid = hideTasks.remove(player.getUniqueId());
        if (tid != null) {
            tid.cancel();
        }
    }

    public void updateBoard(Player player) {
        if (mode == ua.woody.questborn.config.ActionBarMode.STATIC) {
            return;
        }
        refreshBoard(player);
    }

    public void refreshBoard(Player player) {
        if (!enabled || !plugin.getQuestManager().getQuestProgressListener().isValidGamemode(player)) {
            removeBoard(player);
            return;
        }

        var data = playerData.get(player.getUniqueId());
        String questId = data.getTrackedQuestId();

        if (questId == null) {
            removeBoard(player);
            return;
        }

        if (data.hasPendingReward(questId)) {
            removeBoard(player);
            return;
        }

        QuestDefinition def = plugin.getQuestManager().getById(questId);
        if (def == null) {
            removeBoard(player);
            return;
        }

        ua.woody.questborn.model.QuestTypeConfig typeC = plugin.getQuestManager().getQuestTypeManager().getType(def.getTypeId());
        if (typeC != null && typeC.getEngine() == ua.woody.questborn.model.EngineType.GLOBAL) {
            int minP = def.getMinParticipants();
            if (minP > 0) {
                ua.woody.questborn.model.GlobalQuestProgress gp = plugin.getGlobalQuestDataStore().get(def.getId());
                if (gp == null || (!gp.isActivationReached() && gp.getParticipantCount() < minP)) {
                    removeBoard(player);
                    return;
                }
            }
        }

        ua.woody.questborn.util.QuestBoard board = activeBoards.get(player.getUniqueId());
        if (board == null || board.isDeleted()) {
            if (plugin.getFoliaLib().isFolia()) {
                board = new ua.woody.questborn.util.FastQuestBoard(player, this.hideScores);
            } else {
                board = new ua.woody.questborn.util.SimpleBoard(player, this.hideScores);
            }
            board.updateTitle(ua.woody.questborn.lang.ColorFormatter.applyColors(title));
            activeBoards.put(player.getUniqueId(), board);
        }

        if (mode == ua.woody.questborn.config.ActionBarMode.ON_PROGRESS_CHANGE) {
            scheduleHide(player);
        }

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

            ua.woody.questborn.model.QuestObjective obj = null;
            if (stage != null && stage.isObjective()) {
                obj = stage.getObjective();
            } else if (!def.hasStages()) {
                obj = def.getObjective();
            }

            maxVal = plugin.getQuestManager().getTargetValue(data, def);
        }

        int stageNum = Math.max(1, (data.getTrackedQuestId() != null && data.getQuestData(data.getTrackedQuestId()) != null ? data.getQuestData(data.getTrackedQuestId()).getCurrentStage() : 1));
        int totalStages = def.getStageCount();
        if (totalStages < 1)
            totalStages = 1;

        List<String> processedLines = new ArrayList<>();
        List<String> objectiveDetails = null;

        for (String line : lines) {
            if (line.contains("{objective_details}")) {
                if (objectiveDetails == null) {
                    objectiveDetails = new ArrayList<>();
                                        if (data.hasPendingReward(questId)) {
                    } else {
                        ua.woody.questborn.model.QuestObjective resolvedObjective = plugin.getQuestManager()
                                .resolveObjective(player, def);

                        if (resolvedObjective != null) {
                            List<String> details = ua.woody.questborn.util.QuestDisplayBuilder.build(resolvedObjective,
                                    plugin.getLanguage());

                            int limit = Math.min(details.size(), 5);
                            for (int i = 0; i < limit; i++) {
                                objectiveDetails.add(details.get(i));
                            }
                        } else {
                            Map<ua.woody.questborn.model.QuestItem, Integer> materials = plugin.getQuestManager()
                                    .resolveRequiredMaterials(player, def);
                            if (materials != null && !materials.isEmpty()) {
                                objectiveDetails.add(plugin.getLanguage().color(plugin.getLanguage().tr("quests.objectives.header")));
                                objectiveDetails.add(plugin.getLanguage().color(plugin.getLanguage().tr("gui.quest_details.info.required-items")));
                                int count = 0;
                                for (Map.Entry<ua.woody.questborn.model.QuestItem, Integer> entry : materials.entrySet()) {
                                    if (count >= 3)
                                        break;

                                    ua.woody.questborn.model.QuestItem item = entry.getKey();
                                    String materialName;
                                    if (!item.isItemsAdderItem()) {
                                        materialName = ua.woody.questborn.util.ItemDisplayUtil
                                                .findLocalization(item.getMaterial(), plugin.getLanguage(), false);
                                    } else {
                                        materialName = ua.woody.questborn.util.ItemDisplayUtil
                                                .getItemsAdderDisplayName(item.getItemsAdderId(), plugin.getLanguage());
                                    }

                                    int amount = entry.getValue();
                                    if ("global".equalsIgnoreCase(def.getTypeId()) && def.getPersonalLimit() > 0) {
                                        amount = def.getPersonalLimit();
                                    }

                                    objectiveDetails.add(plugin.getLanguage()
                                            .color(" &7  &f" + materialName + " &7x&f" + amount));
                                    count++;
                                }
                            }
                        }
                    }
                }

                if (objectiveDetails.isEmpty()) {
                    continue;
                }
                processedLines.addAll(objectiveDetails);
            } else {
                processedLines.add(line);
            }
        }

        if (processedLines.size() > 15) {
            processedLines = processedLines.subList(0, 15);
        }

        List<String> finalLines = new ArrayList<>();
        for (String line : processedLines) {
            String formatted = line
                    .replace("{quest_name}", def.getDisplayName())
                    .replace("{progress}", String.valueOf(currentVal))
                    .replace("{target}", String.valueOf(maxVal))
                    .replace("{stage}", String.valueOf(stageNum))
                    .replace("{total_stages}", String.valueOf(totalStages));

            if (progressBarUtil != null) {
                formatted = formatted.replace("{progressbar}", progressBarUtil.getProgressBar(currentVal, maxVal));
            } else {
                formatted = formatted.replace("{progressbar}", "");
            }

            formatted = ua.woody.questborn.lang.ColorFormatter.applyColors(formatted);
            if (formatted.isEmpty())
                formatted = " ";

            finalLines.add(formatted);
        }

        board.updateLines(finalLines);
    }

    private void scheduleHide(Player player) {
        UUID uuid = player.getUniqueId();
        com.tcoded.folialib.wrapper.task.WrappedTask existing = hideTasks.remove(uuid);
        if (existing != null) {
            existing.cancel();
        }

        com.tcoded.folialib.wrapper.task.WrappedTask tid = plugin.getFoliaLib().getImpl().runAtEntityLater(player, () -> {
            removeBoard(player);
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
