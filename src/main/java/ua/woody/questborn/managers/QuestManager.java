package ua.woody.questborn.managers;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.config.ActionBarMode;
import ua.woody.questborn.effects.EffectPresetManager;
import ua.woody.questborn.lang.ColorFormatter;
import ua.woody.questborn.lang.LanguageManager;
import ua.woody.questborn.listeners.QuestProgressListener;
import ua.woody.questborn.model.*;

import ua.woody.questborn.storage.PlayerDataStore;
import ua.woody.questborn.util.QuestDisplayBuilder;
import ua.woody.questborn.util.ItemDisplayUtil;
import ua.woody.questborn.managers.QuestRegistry;
import ua.woody.questborn.managers.QuestValidator;
import ua.woody.questborn.managers.QuestNotificationService;
import ua.woody.questborn.managers.QuestProgressProcessor;
import ua.woody.questborn.managers.QuestStateService;

import java.io.File;
import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import com.tcoded.folialib.wrapper.task.WrappedTask;

public class QuestManager implements ua.woody.questborn.api.managers.QuestManagerAPI {
    public enum ActivationConflictMode {
        BLOCK,
        REPLACE,
        CHANGE,
        CONFIRM
    }

    private final QuestbornPlugin plugin;
    private final PlayerDataStore playerData;
    private final QuestProgressListener questProgressListener;
    private final LanguageManager lang;
    private final QuestTypeManager questTypeManager;
    private final EffectPresetManager effectPresetManager;
    private final QuestParser questParser;
    private final ActionBarManager actionBarManager;
    private final SoundManager soundManager;
    private final QuestEffectsManager questEffectsManager;
    private final TopManager topManager;
    private final BossBarManager bossBarManager;
    private final ScoreboardManager scoreboardManager;

    private QuestRegistry registry;
    private QuestValidator validator;
    private QuestNotificationService notificationService;
    private QuestProgressProcessor progressProcessor;
    private QuestStateService stateService;

    private final Set<UUID> activeQuestPlayers = ConcurrentHashMap.newKeySet();

    private final Map<String, Long> lastGlobalBroadcasts = new ConcurrentHashMap<>();
    private WrappedTask rotationAnnounceTask;

    private ActivationConflictMode activationConflictMode;
    private boolean optimizeDistanceQuests = true;
    private int minDistanceSave = 1;

    private boolean showActivationDetails = true;

    private long stageCompleteDelay = 50L;
    private long questCompleteDelay = 50L;
    private long autoActivateNextDelay = 20L;

    public QuestManager(QuestbornPlugin plugin, PlayerDataStore playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
        this.lang = plugin.getLanguage();
        this.questProgressListener = new QuestProgressListener(plugin);
        this.questTypeManager = new QuestTypeManager(plugin);
        this.effectPresetManager = new EffectPresetManager(plugin);

        this.questParser = new QuestParser(plugin);
        this.questParser.setQuestTypeManager(questTypeManager);

        this.actionBarManager = new ActionBarManager(plugin, playerData);
        this.soundManager = new SoundManager(plugin);
        this.questEffectsManager = new QuestEffectsManager(plugin, effectPresetManager);
        this.topManager = new TopManager(plugin);
        int topInterval = plugin.getConfig().getInt("top.update-interval-ticks", 6000);
        this.topManager.setUpdateInterval(topInterval);

        this.bossBarManager = new BossBarManager(plugin, playerData);
        this.scoreboardManager = new ScoreboardManager(plugin, playerData);

        this.registry = new QuestRegistry(plugin, questTypeManager, questParser);
        this.validator = new QuestValidator(plugin, this, questTypeManager);
        this.notificationService = new QuestNotificationService(plugin, this);
        this.progressProcessor = new QuestProgressProcessor(plugin, this, playerData);
        this.stateService = new QuestStateService(plugin, this, playerData);

        loadConfig();
        loadQuests();
        startActionBarTask();
        startRotationAnnounceTask();
    }

    public void reload() {
        registry.clear();
        plugin.reloadConfig();

        questTypeManager.reload();
        effectPresetManager.reload();

        actionBarManager.stopTask();

        loadConfig();
        loadQuests();
        questProgressListener.reload();

        startActionBarTask();
        startRotationAnnounceTask();

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            questProgressListener.updatePlayerCache(p);
            actionBarManager.sendForPlayer(p);
            bossBarManager.updateBar(p);
            scoreboardManager.updateBoard(p);
        }
    }

    private void loadConfig() {
        var cfg = plugin.getConfig();

        ActionBarMode actionBarMode = ActionBarMode.fromString(
                cfg.getString("actionbar.mode", "ON_PROGRESS_CHANGE"));

        int actionBarInterval = cfg.getInt("actionbar.update-interval-ticks", 40);

        List<String> disabledActionBarTypes = cfg.getStringList("actionbar.disabled-for-types");

        actionBarManager.loadConfig(actionBarMode, actionBarInterval, disabledActionBarTypes);

        actionBarManager.configureProgressBar();

        topManager.setUpdateInterval(cfg.getInt("top.update-interval-ticks", 6000));

        String modeStr = cfg.getString("activation.conflict-mode", "BLOCK");
        try {
            this.activationConflictMode = ActivationConflictMode.valueOf(modeStr.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            this.activationConflictMode = ActivationConflictMode.BLOCK;
        }

        this.optimizeDistanceQuests = cfg.getBoolean("optimization.distance-quests.enabled", true);
        this.minDistanceSave = cfg.getInt("optimization.distance-quests.min-save-blocks", 1);

        soundManager.loadConfig();

        this.showActivationDetails = cfg.getBoolean("chat.show-details", true);

        bossBarManager.loadConfig();
        scoreboardManager.loadConfig();

        this.stageCompleteDelay = cfg.getLong("delays.stage-complete", 50L);
        this.questCompleteDelay = cfg.getLong("delays.quest-complete", 50L);
        this.autoActivateNextDelay = cfg.getLong("delays.auto-activate-next", 20L);
    }

    private void loadQuests() { registry.loadQuests(); }

    private void startActionBarTask() {
        actionBarManager.startTask();
    }

    public void restartRotationAnnounceTask() {
        startRotationAnnounceTask();
    }

    private void startRotationAnnounceTask() {
        if (rotationAnnounceTask != null) {
            rotationAnnounceTask.cancel();
            rotationAnnounceTask = null;
        }

        for (QuestTypeConfig typeConfig : questTypeManager.getEnabledTypes()) {
            if (typeConfig.getEngine() == EngineType.ROTATION && typeConfig.getRotationAnnounce() != null) {
                long currentReset = typeConfig.hasResetTime() && typeConfig.getResetSeconds() > 0
                    ? getLastGlobalResetTimestamp(typeConfig.getResetTime(), typeConfig.getResetSeconds(), typeConfig.getResetAnchorDay())
                    : (typeConfig.hasResetTime() ? getLastDailyResetTimestamp(typeConfig.getResetTime()) : 0L);
                lastGlobalBroadcasts.put(typeConfig.getId(), currentReset);
            }
        }

        rotationAnnounceTask = plugin.getFoliaLib().getImpl().runTimerAsync(() -> {
            if (!plugin.isEnabled()) return;
            for (QuestTypeConfig typeConfig : questTypeManager.getEnabledTypes()) {
                if (typeConfig.getEngine() != EngineType.ROTATION) continue;
                QuestTypeConfig.RotationAnnounce ann = typeConfig.getRotationAnnounce();

                long currentReset = typeConfig.hasResetTime() && typeConfig.getResetSeconds() > 0
                    ? getLastGlobalResetTimestamp(typeConfig.getResetTime(), typeConfig.getResetSeconds(), typeConfig.getResetAnchorDay())
                    : (typeConfig.hasResetTime() ? getLastDailyResetTimestamp(typeConfig.getResetTime()) : 0L);

                if (currentReset == 0L) continue;

                long lastBroadcast = lastGlobalBroadcasts.getOrDefault(typeConfig.getId(), currentReset);
                if (currentReset > lastBroadcast) {
                    lastGlobalBroadcasts.put(typeConfig.getId(), currentReset);

                    Collection<? extends Player> online = plugin.getServer().getOnlinePlayers();
                    if (!online.isEmpty()) {
                        boolean doAnnounce = ann != null && ann.isEnabled();
                        for (Player p : online) {
                            if (doAnnounce) {
                                if (ann.getSound() != null) {
                                    p.playSound(p.getLocation(), ann.getSound(), ann.getVolume(), ann.getPitch());
                                }
                                for (String line : ann.getChat()) {
                                    p.sendMessage(ColorFormatter.applyColors(line));
                                }
                            }
                        }

                        plugin.getFoliaLib().getImpl().runNextTick((task) -> {
                            for (Player p : plugin.getServer().getOnlinePlayers()) {
                                getOrAssignRotationQuests(p, typeConfig);
                            }
                        });
                    }
                }
            }
        }, 20L * 10L, 20L * 10L);
    }

    public QuestRegistry getRegistry() { return registry; }
    public QuestValidator getValidator() { return validator; }
    public QuestNotificationService getNotificationService() { return notificationService; }
    public QuestProgressProcessor getProgressProcessor() { return progressProcessor; }
    public QuestStateService getStateService() { return stateService; }

    public ActionBarManager getActionBarManager() { return actionBarManager; }
    public BossBarManager getBossBarManager() { return bossBarManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public QuestEffectsManager getQuestEffectsManager() { return questEffectsManager; }

    public long getStageCompleteDelay() { return stageCompleteDelay; }
    public long getQuestCompleteDelay() { return questCompleteDelay; }
    public long getAutoActivateNextDelay() { return autoActivateNextDelay; }
    public boolean isShowActivationDetails() { return showActivationDetails; }

    public boolean isOptimizeDistanceQuests() {
        return optimizeDistanceQuests;
    }

    public Set<UUID> getActiveQuestPlayers() {
        return activeQuestPlayers;
    }

    public TopManager getTopManager() {
        return topManager;
    }

    public int getMinDistanceSave() {
        return minDistanceSave;
    }

    public QuestStage getCurrentStage(UUID playerId, QuestDefinition quest) {
        if (playerId == null || quest == null)
            return null;
        PlayerQuestProgress data = playerData.get(playerId);
        PlayerQuestProgress.ActiveQuestData qd = data.getQuestData(quest.getId());
        int stageNum = Math.max(1, (qd != null ? qd.getCurrentStage() : 1));
        return quest.getStage(stageNum);
    }

    public QuestObjective getCurrentStageObjective(UUID playerId, QuestDefinition quest) {
        QuestStage st = getCurrentStage(playerId, quest);
        return (st != null && st.isObjective()) ? st.getObjective() : null;
    }

    public Map<QuestItem, Integer> getCurrentStageMaterials(UUID playerId, QuestDefinition quest) {
        QuestStage st = getCurrentStage(playerId, quest);
        return (st != null && st.isRequiredMaterials()) ? st.getRequiredItems() : null;
    }

    public QuestObjective resolveObjective(Player player, QuestDefinition quest) {
        if (player == null || quest == null)
            return null;

        if (quest.hasStages()) {
            QuestObjective o = getCurrentStageObjective(player.getUniqueId(), quest);
            return o;
        }

        return quest.getObjective();
    }

    public Map<QuestItem, Integer> resolveRequiredMaterials(Player player, QuestDefinition quest) {
        if (player == null || quest == null)
            return null;

        if (quest.hasStages()) {
            return getCurrentStageMaterials(player.getUniqueId(), quest);
        }

        if (quest.hasRequiredMaterials())
            return quest.getRequiredItems();
        return null;
    }

    public int getProgressValue(PlayerQuestProgress data, QuestDefinition quest) {
        if (data == null || quest == null)
            return 0;

        boolean isTransferStage = false;
        if (quest.hasStages()) {
            PlayerQuestProgress.ActiveQuestData qd = data.getQuestData(quest.getId());
            int currentStageNum = Math.max(1, (qd != null ? qd.getCurrentStage() : 1));
            ua.woody.questborn.model.QuestStage stage = quest.getStage(currentStageNum);
            if (stage != null && stage.isRequiredMaterials()) {
                isTransferStage = true;
            }
        } else if (quest.hasRequiredMaterials()) {
            isTransferStage = true;
        }

        PlayerQuestProgress.ActiveQuestData qd = data.getQuestData(quest.getId());
        if (isTransferStage) {
            if (qd != null && qd.isItemsTransferred()) return getTargetValue(data, quest);
            if (qd != null) {
                int total = 0;
                for (int amt : qd.getTransferredItemsData().values()) {
                    total += amt;
                }
                return total;
            }
            return 0;
        }

        return quest.hasStages() ? (qd != null ? qd.getStageProgress() : 0) : (qd != null ? qd.getProgress() : 0);
    }

    public int getTargetValue(PlayerQuestProgress data, QuestDefinition quest) {
        if (quest == null) return 0;

        if (quest.hasStages()) {
            PlayerQuestProgress.ActiveQuestData qd = data != null ? data.getQuestData(quest.getId()) : null;
            int currentStageNum = Math.max(1, (qd != null ? qd.getCurrentStage() : 1));
            ua.woody.questborn.model.QuestStage stage = quest.getStage(currentStageNum);
            if (stage != null) {
                if (stage.isObjective()) {
                    return getTargetAmount(stage.getObjective());
                } else if (stage.isRequiredMaterials()) {
                    int t = 0;
                    if (stage.getRequiredItems() != null) {
                        for (int a : stage.getRequiredItems().values()) t += a;
                    }
                    return t <= 0 ? 1 : t;
                }
            }
        } else {
            if (quest.getObjective() != null) {
                return getTargetAmount(quest.getObjective());
            } else if (quest.hasRequiredMaterials()) {
                int t = 0;
                if (quest.getRequiredItems() != null) {
                    for (int a : quest.getRequiredItems().values()) t += a;
                }
                return t <= 0 ? 1 : t;
            }
        }
        return 1;
    }

    public void setProgressValue(PlayerQuestProgress data, QuestDefinition quest, int value) {
        if (data == null || quest == null)
            return;
        PlayerQuestProgress.ActiveQuestData qd = data.getQuestData(quest.getId());
        if (qd != null) {
            if (quest.hasStages()) {
                qd.setStageProgress(value);
            } else {
                qd.setProgress(value);
            }
        }
    }

    public boolean isDistanceQuestType(QuestObjectiveType type) {
        return type == QuestObjectiveType.TRAVEL_DISTANCE
                || type == QuestObjectiveType.SPRINT_DISTANCE
                || type == QuestObjectiveType.CROUCH_DISTANCE
                || type == QuestObjectiveType.SWIM_DISTANCE
                || type == QuestObjectiveType.ELYTRA_FLY
                || type == QuestObjectiveType.BOAT_TRAVEL
                || type == QuestObjectiveType.MINECART_TRAVEL
                || type == QuestObjectiveType.FALL_DISTANCE;
    }

    public int getTargetAmount(QuestObjective o) {
        if (o == null)
            return 0;

        QuestObjectiveType type = o.getType();

        if (isDistanceQuestType(type)) {
            if (o.getDistance() > 0)
                return (int) o.getDistance();
            return o.getAmount();
        }

        if (type == QuestObjectiveType.ENTITY_RIDE) {
            if (o.getDistance() > 0)
                return (int) o.getDistance();
            return o.getAmount();
        }

        return o.getAmount();
    }

    private void sendQuestActivationMessage(Player player, QuestDefinition quest) { notificationService.sendQuestActivationMessage(player, quest, showActivationDetails); }

    private void sendNewStageMessage(Player player, QuestDefinition quest, int newStageNum) { notificationService.sendNewStageMessage(player, quest, newStageNum, showActivationDetails); }

    private String convertHexForBungee(String text) { return ""; }

    private TextComponent createComponentWithColors(String text) { return null; }

    private TextComponent createMultiLineComponent(String text) { return null; }

    private void sendClickableLink(Player player, QuestDefinition quest) { }

    private List<String> getLimitedObjectiveDetails(Player player, QuestDefinition quest) { return new ArrayList<>(); }

    public void updateActiveQuestPlayer(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        PlayerQuestProgress data = playerData.get(uuid);
        if (data != null && data.getTrackedQuestId() != null) {
            activeQuestPlayers.add(uuid);
        } else {
            activeQuestPlayers.remove(uuid);
        }
    }

    public void refreshPlayerUI(Player player) {
        if (player == null) return;
        updateActiveQuestPlayer(player);
        if (scoreboardManager != null) scoreboardManager.updateBoard(player);
        if (bossBarManager != null) bossBarManager.updateBar(player);
        if (actionBarManager != null) actionBarManager.sendForPlayer(player);
        if (questProgressListener != null) questProgressListener.updatePlayerCache(player);
    }

    public boolean activateQuest(Player player, String questId, boolean ignoreRequirements) { return stateService.activateQuest(player, questId, ignoreRequirements); }
    public boolean activateQuest(Player player, String questId) { return stateService.activateQuest(player, questId, false); }

    public boolean trackQuest(Player player, String questId) { return stateService.trackQuest(player, questId); }

    public boolean confirmQuestChange(Player player, String questId, boolean ignoreRequirements) { return stateService.confirmQuestChange(player, questId, ignoreRequirements); }
    public boolean confirmQuestChange(Player player, String questId) { return stateService.confirmQuestChange(player, questId, false); }

    public boolean cancelQuest(Player player, String questId) { return stateService.cancelQuest(player, questId); }

    public void incrementProgress(Player player, QuestDefinition quest, int delta) { progressProcessor.incrementProgress(player, quest, delta); }

    private void completeCurrentStage(Player player, QuestDefinition quest, PlayerQuestProgress data) { progressProcessor.completeCurrentStage(player, quest, data); }

    public boolean forceFinishStage(Player player) { return progressProcessor.forceFinishStage(player); }

    public boolean transferItems(Player player, QuestDefinition quest, Map<QuestItem, Integer> takenItems) { return progressProcessor.transferItems(player, quest, takenItems); }

    public boolean transferItems(Player player, QuestDefinition quest) { return progressProcessor.transferItems(player, quest, new HashMap<>()); }

    private boolean hasRequiredItems(Player player, Map<QuestItem, Integer> required) { return true;   }

    private void removeRequiredItems(Player player, Map<QuestItem, Integer> required) {   }

    public void completeQuest(Player player, QuestDefinition quest) { progressProcessor.completeQuest(player, quest); }

    public boolean claimReward(Player player, QuestDefinition quest) { return progressProcessor.claimReward(player, quest); }

        public boolean canClaimRewardFromNpc(Player player, QuestDefinition quest, String npcId) {
        var data = playerData.get(player.getUniqueId());
        if (data == null || !data.hasPendingReward(quest.getId())) return false;

        if (npcId == null) {
            return true;
        }

        var npcConfig = plugin.getNpcManager().getConfigByNpcId(npcId);
        if (npcConfig != null) {
            if (npcConfig.finishesQuest(quest.getId(), quest.getTypeId())) return true;

            if (npcConfig.startsQuest(quest.getId(), quest.getTypeId())) {
                if (!plugin.getNpcManager().hasExplicitFinisher(quest.getId(), quest.getTypeId())) {
                    return true;
                }
            }
        }

        return false;
    }

    public void complete(Player player, QuestDefinition quest) { progressProcessor.complete(player, quest); }
    public void forceCompleteGlobalQuest(QuestDefinition quest) { progressProcessor.forceCompleteGlobalQuest(quest); }

    public void sendActionBarForPlayer(Player player) {
        actionBarManager.sendForPlayer(player);
        bossBarManager.updateBar(player);
        scoreboardManager.updateBoard(player);
    }

    public void updateTravelBuffer(UUID playerId, double distance) { progressProcessor.updateTravelBuffer(playerId, distance); }

    public boolean isQuestAvailable(Player player, QuestDefinition quest, PlayerQuestProgress data, boolean ignoreRequirements) { return validator.isQuestAvailable(player, quest, data, ignoreRequirements); }
    public boolean isQuestAvailable(Player player, QuestDefinition quest, PlayerQuestProgress data) { return validator.isQuestAvailable(player, quest, data, false); }

    private boolean isQuestAvailableDefault(Player player, QuestDefinition quest, PlayerQuestProgress data) { return true;   }

    private boolean isQuestAvailableChain(Player player, QuestDefinition quest, PlayerQuestProgress data) { return true;   }

    public List<QuestDefinition> getAvailableQuestsForPlayer(Player player, QuestTypeConfig typeConfig, boolean ignoreRequirements) { return validator.getAvailableQuestsForPlayer(player, typeConfig, playerData.get(player.getUniqueId()), ignoreRequirements); }
    public List<QuestDefinition> getAvailableQuestsForPlayer(Player player, QuestTypeConfig typeConfig) { return validator.getAvailableQuestsForPlayer(player, typeConfig, playerData.get(player.getUniqueId()), false); }

    public Collection<QuestDefinition> getAll() { return registry.getAll(); }

    public Collection<QuestDefinition> getByType(String typeId) { return registry.getByType(typeId); }

    public Collection<QuestDefinition> getByType(QuestTypeConfig typeConfig) { return registry.getByType(typeConfig); }

    public QuestDefinition getById(String id) { return registry.getQuest(id); }

    public List<String> getAllIds() { return registry.getAllIds(); }

    public QuestTypeManager getQuestTypeManager() {
        return questTypeManager;
    }

    public QuestProgressListener getQuestProgressListener() {
        return questProgressListener;
    }

    public ActionBarMode getActionBarMode() {
        return actionBarManager.getMode();
    }

    public EffectPresetManager getEffectPresetManager() {
        return effectPresetManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public ActivationConflictMode getActivationConflictMode() {
        return activationConflictMode;
    }

    public QuestDefinition getQuest(String id) { return registry.getQuest(id); }

    public List<QuestDefinition> getOrAssignRotationQuests(Player player, QuestTypeConfig typeConfig) {
        if (player == null || typeConfig == null)
            return Collections.emptyList();
        if (typeConfig.getEngine() != EngineType.ROTATION)
            return Collections.emptyList();

        UUID playerId = player.getUniqueId();
        var data = playerData.get(playerId);
        String typeId = typeConfig.getId();

        List<String> assignedIds = new ArrayList<>(data.getAssignedRotationQuests(typeId));
        if (assignedIds.isEmpty() || isRotationExpired(typeConfig, data.getRotationAssignedAt(typeId))) {
            assignNewRotation(playerId, data, typeConfig);
            assignedIds = new ArrayList<>(data.getAssignedRotationQuests(typeId));
        }
        List<QuestDefinition> result = new ArrayList<>();
        boolean needsCleanup = false;
        for (String questId : assignedIds) {
            QuestDefinition def = registry.getQuest(questId);
            if (def != null) {
                boolean isNpcBound = false;
                if (plugin.getNpcManager() != null) {
                    for (ua.woody.questborn.model.NpcConfig nc : plugin.getNpcManager().getAll()) {
                        if (!nc.getStartsQuests().isEmpty() && nc.getStartsQuests().contains(def.getId())) {
                            isNpcBound = true;
                            break;
                        }
                    }
                }
                if (isNpcBound) {
                    needsCleanup = true;
                    continue;
                }
                result.add(def);
            }
        }

        if (needsCleanup) {
            List<String> cleanedIds = new ArrayList<>();
            for (QuestDefinition q : result) cleanedIds.add(q.getId());
            data.setAssignedRotationQuests(typeId, cleanedIds);
            playerData.markDirty(playerId);
        }

        return result;
    }

    private void assignNewRotation(UUID playerId, ua.woody.questborn.model.PlayerQuestProgress data,
            QuestTypeConfig typeConfig) {
        String typeId = typeConfig.getId();

        List<String> oldAssigned = data.getAssignedRotationQuests(typeId);
        if (oldAssigned != null) {
            org.bukkit.entity.Player player = plugin.getServer().getPlayer(playerId);
            for (String oldQuest : oldAssigned) {
                if (data.hasActiveQuest(oldQuest)) {
                    if (player != null) {
                        stateService.cancelQuest(player, oldQuest);
                    } else {
                        data.removeActiveQuest(oldQuest);
                        playerData.markDirty(playerId);
                    }
                }
                data.resetHistory(oldQuest);
                data.resetReward(oldQuest);
                data.resetCooldown(oldQuest);
            }
        }

        List<QuestDefinition> allQuests = new ArrayList<>();
        for (QuestDefinition q : getByType(typeId)) {
            boolean isNpcBound = false;
            if (plugin.getNpcManager() != null) {
                for (ua.woody.questborn.model.NpcConfig nc : plugin.getNpcManager().getAll()) {
                    if (!nc.getStartsQuests().isEmpty() && nc.getStartsQuests().contains(q.getId())) {
                        isNpcBound = true;
                        break;
                    }
                }
            }
            if (isNpcBound) {
                continue;
            }
            allQuests.add(q);
        }
        Map<String, List<QuestDefinition>> pools = new HashMap<>();
        for (QuestDefinition q : allQuests) {
            String pool = q.getRotationPool();
            if (pool == null || pool.equalsIgnoreCase("none")) {
                continue;
            }
            pools.computeIfAbsent(pool, k -> new ArrayList<>()).add(q);
        }

        List<String> chosen = new ArrayList<>();
        Map<String, Integer> requiredPools = typeConfig.getRotationPools();

        boolean syncRotation = plugin.getConfig().getBoolean("gameplay.sync-rotation", false);
        java.util.Random random;
        if (syncRotation) {
            long seed = java.time.LocalDate.now().toEpochDay() + typeId.hashCode();
            random = new java.util.Random(seed);
        } else {
            random = new java.util.Random();
        }

        for (Map.Entry<String, Integer> entry : requiredPools.entrySet()) {
            String poolName = entry.getKey();
            int requestedCount = entry.getValue();

            if (requestedCount <= 0) continue;

            List<QuestDefinition> poolQuests = pools.get(poolName);
            if (poolQuests == null || poolQuests.isEmpty()) {
                plugin.getLogger().warning("[ROTATION] Not enough quests in pool '" + poolName + "' for type '" + typeId + "'");
                continue;
            }

            Collections.shuffle(poolQuests, random);
            int takeCount = Math.min(requestedCount, poolQuests.size());
            for (int i = 0; i < takeCount; i++) {
                chosen.add(poolQuests.get(i).getId());
            }

            if (requestedCount > poolQuests.size()) {
                plugin.getLogger().warning("[ROTATION] Requested " + requestedCount + " quests from pool '" + poolName + "', but only " + poolQuests.size() + " exist in type '" + typeId + "'.");
            }
        }

        data.setAssignedRotationQuests(typeId, chosen);
        data.setRotationAssignedAt(typeId, System.currentTimeMillis());
        playerData.markDirty(playerId);

        plugin.getLogger().fine("[ROTATION] Assigned " + chosen.size() + " quests to player "
                + playerId + " for type " + typeId + " from pools: " + requiredPools);
    }

    private boolean isRotationExpired(QuestTypeConfig typeConfig, long assignedAt) {
        if (assignedAt <= 0)
            return true;

        long now = System.currentTimeMillis();

        if (typeConfig.hasResetTime() && typeConfig.getResetSeconds() > 0) {
            long lastGlobalReset = getLastGlobalResetTimestamp(typeConfig.getResetTime(), typeConfig.getResetSeconds(),
                    typeConfig.getResetAnchorDay());
            return assignedAt < lastGlobalReset;
        } else if (typeConfig.hasResetTime()) {
            long lastResetTimestamp = getLastDailyResetTimestamp(typeConfig.getResetTime());
            return assignedAt < lastResetTimestamp;
        } else if (typeConfig.getResetSeconds() > 0) {
            long elapsed = (now - assignedAt) / 1000L;
            return elapsed >= typeConfig.getResetSeconds();
        }

        return false;
    }

    public long getNextRotationResetTimestamp(QuestTypeConfig typeConfig, long assignedAt) {
        if (typeConfig.hasResetTime() && typeConfig.getResetSeconds() > 0) {
            long lastGlobalReset = getLastGlobalResetTimestamp(typeConfig.getResetTime(), typeConfig.getResetSeconds(),
                    typeConfig.getResetAnchorDay());
            return lastGlobalReset + (typeConfig.getResetSeconds() * 1000L);
        } else if (typeConfig.hasResetTime()) {
            long lastReset = getLastDailyResetTimestamp(typeConfig.getResetTime());

            return lastReset + 86400000L;
        } else if (typeConfig.getResetSeconds() > 0) {
            return assignedAt + (typeConfig.getResetSeconds() * 1000L);
        }
        return 0L;
    }

    private long getLastGlobalResetTimestamp(String resetTime, long periodSeconds, int anchorDay) {
        try {
            String[] parts = resetTime.trim().split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

            int dayOffset = Math.max(0, anchorDay - 1);
            java.time.LocalDateTime anchorDate = java.time.LocalDateTime.of(2024, 1, 1, hour, minute, 0).plusDays(dayOffset);

            if (periodSeconds <= 0) return 0L;

            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            long diffSeconds = java.time.Duration.between(anchorDate, now).getSeconds();

            if (diffSeconds >= 0) {
                long periods = diffSeconds / periodSeconds;
                java.time.LocalDateTime lastReset = anchorDate.plusSeconds(periods * periodSeconds);
                return lastReset.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            } else {
                long periods = (Math.abs(diffSeconds) + periodSeconds - 1) / periodSeconds;
                java.time.LocalDateTime lastReset = anchorDate.minusSeconds(periods * periodSeconds);
                return lastReset.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
        } catch (Exception e) {
            return 0L;
        }
    }

    private long getLastDailyResetTimestamp(String resetTime) {
        try {
            String[] parts = resetTime.trim().split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

            java.time.LocalDateTime cal = java.time.LocalDateTime.now();
            cal = cal.withHour(hour).withMinute(minute).withSecond(0).withNano(0);

            if (cal.isAfter(java.time.LocalDateTime.now())) {
                cal = cal.minusDays(1);
            }

            return cal.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            plugin.getLogger().warning("[ROTATION] Invalid reset-time format: " + resetTime + " (expected HH:mm)");
            return 0L;
        }
    }

    public List<QuestDefinition> getAssignedRotationQuests(Player player, QuestTypeConfig typeConfig) {
        if (player == null || typeConfig == null)
            return Collections.emptyList();
        var data = playerData.get(player.getUniqueId());
        List<String> assignedIds = data.getAssignedRotationQuests(typeConfig.getId());
        List<QuestDefinition> result = new ArrayList<>();
        for (String questId : assignedIds) {
            QuestDefinition def = registry.getQuest(questId);
            if (def != null)
                result.add(def);
        }
        return result;
    }

    public void onDisable() {
        if (rotationAnnounceTask != null) {
            rotationAnnounceTask.cancel();
            rotationAnnounceTask = null;
        }
        actionBarManager.stopTask();
        bossBarManager.removeAllBars();
        scoreboardManager.removeAllBoards();
        playerData.stopAutoSave();

        boolean debugLog = plugin.getConfig().getBoolean("optimization.debug.auto-save-log", false);
        if (debugLog) {
            plugin.getLogger().info("[DEBUG] QuestManager disabled");
        }
    }
}
