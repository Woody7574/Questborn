package ua.woody.questborn.managers;

import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.addons.api.AvailabilityResult;
import ua.woody.questborn.addons.api.QuestEngine;
import ua.woody.questborn.config.ActionBarMode;
import ua.woody.questborn.effects.EffectPresetManager;
import ua.woody.questborn.lang.LanguageManager;
import ua.woody.questborn.listeners.QuestProgressListener;
import ua.woody.questborn.model.*;
import ua.woody.questborn.rewards.RewardHandler;
import ua.woody.questborn.storage.PlayerDataStore;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class QuestManager {

    public enum ActivationConflictMode {
        BLOCK,
        REPLACE,
        CHANGE
    }

    private final QuestbornPlugin plugin;
    private final PlayerDataStore playerData;
    private final QuestProgressListener questProgressListener;
    private final LanguageManager lang;
    private final QuestTypeManager questTypeManager;
    private final EffectPresetManager effectPresetManager;

    // Менеджери
    private final QuestParser questParser;
    private final ActionBarManager actionBarManager;
    private final SoundManager soundManager;
    private final QuestEffectsManager questEffectsManager;
    private final TopManager topManager;

    // Дані
    private final Map<String, QuestDefinition> questsById = new HashMap<>();
    private ActivationConflictMode activationConflictMode;

    public QuestManager(QuestbornPlugin plugin, PlayerDataStore playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
        this.lang = plugin.getLanguage();
        this.questProgressListener = new QuestProgressListener(plugin);
        this.questTypeManager = new QuestTypeManager(plugin);
        this.effectPresetManager = new EffectPresetManager(plugin);

        // Ініціалізація менеджерів
        this.questParser = new QuestParser(plugin);
        this.actionBarManager = new ActionBarManager(plugin, playerData);
        this.soundManager = new SoundManager(plugin);
        this.questEffectsManager = new QuestEffectsManager(plugin, effectPresetManager);
        this.topManager = new TopManager(playerData);

        loadConfig();
        loadQuests();
        startActionBarTask();
    }

    public void reload() {
        questsById.clear();
        plugin.reloadConfig();

        questTypeManager.reload();
        effectPresetManager.reload();

        loadConfig();
        loadQuests();
        startActionBarTask();
    }

    private void loadConfig() {
        var cfg = plugin.getConfig();

        // ActionBar
        ActionBarMode actionBarMode = ActionBarMode.fromString(
                cfg.getString("actionbar.mode", "ON_PROGRESS_CHANGE"));
        String actionBarFormat = lang.tr("actionbar.format");
        String actionBarCompleteMessage = lang.tr("actionbar.complete-message");
        int actionBarInterval = cfg.getInt("actionbar.update-interval-ticks", 40);

        actionBarManager.loadConfig(actionBarMode, actionBarFormat, actionBarCompleteMessage, actionBarInterval);

        // Top
        topManager.setTopSize(cfg.getInt("top.size", 10));

        // Conflict mode
        String modeStr = cfg.getString("activation.conflict-mode", "BLOCK");
        try {
            this.activationConflictMode = ActivationConflictMode.valueOf(modeStr.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            this.activationConflictMode = ActivationConflictMode.BLOCK;
        }

        // Sounds
        soundManager.loadConfig();
    }

    private void loadQuests() {
        for (QuestTypeConfig typeConfig : questTypeManager.getEnabledTypes()) {
            File folder = new File(plugin.getDataFolder(), typeConfig.getFolder());
            List<QuestDefinition> quests = questParser.loadQuestsFromFolder(folder, typeConfig.getId());

            for (QuestDefinition quest : quests) {
                questsById.put(quest.getId(), quest);
            }
        }

        plugin.getLogger().fine("Loaded " + questsById.size() + " quests from " +
                questTypeManager.getEnabledTypes().size() + " types");
    }

    private void startActionBarTask() {
        actionBarManager.startTask();
    }

    /* =========================================================
     *                  АКТИВАЦІЯ / СКАСУВАННЯ
     * ========================================================= */

    public boolean activateQuest(Player player, String questId) {
        QuestDefinition def = questsById.get(questId);
        if (def == null) return false;

        var data = playerData.get(player.getUniqueId());

        QuestTypeConfig typeConfig = questTypeManager.getType(def.getTypeId());
        if (typeConfig != null && typeConfig.getCooldownSeconds() == 0 && data.isQuestCompleted(questId)) {
            return false;
        }

        long cdUntil = data.getQuestCooldownUntil(questId);
        if (cdUntil > 0 && cdUntil > System.currentTimeMillis()) {
            return false;
        }

        // ✅ availability (engine)
        AvailabilityResult avail = checkAvailability(player, def);
        if (!avail.isAllowed()) return false;

        String currentActive = data.getActiveQuestId();
        if (currentActive != null && !currentActive.equalsIgnoreCase(questId)) {
            if (activationConflictMode == ActivationConflictMode.BLOCK) {
                return false;
            } else if (activationConflictMode == ActivationConflictMode.REPLACE) {
                // НЕ викликаємо clearActiveQuest(), щоб не залежати від наявності методу
                data.setActiveQuestId(null);
                data.setActiveQuestProgress(0);
                data.setActiveQuestStartTime(0L);
            } else if (activationConflictMode == ActivationConflictMode.CHANGE) {
                data.setPendingQuestId(questId);
                playerData.save();
                return true;
            }
        }

        if (questId.equalsIgnoreCase(currentActive)) {
            return true;
        }

        data.setActiveQuestId(questId);
        data.setActiveQuestStartTime(System.currentTimeMillis());
        data.clearPendingQuest();

        // Спеціальний кейс для LEVEL_UP_REACH — прогрес може бути вже виконаний
        if (def.getObjective().getType() == QuestObjectiveType.LEVEL_UP_REACH) {
            int currentLevel = player.getLevel();
            int targetLevel = def.getObjective().getAmount();
            int progress = Math.min(currentLevel, targetLevel);
            data.setActiveQuestProgress(progress);

            if (currentLevel >= targetLevel) {
                playerData.save();
                completeQuest(player, def);
                return true;
            }
        } else {
            data.setActiveQuestProgress(0);
        }

        playerData.save();

        // ✅ engine hook (activation) — optional через reflection, щоб не ламати компіляцію
        try {
            QuestEngine engine = resolveEngineForQuest(def);
            invokeOptionalEngineHook(engine, "onQuestActivated", player, def, this);
        } catch (Exception ignored) {}

        if (actionBarManager.getMode() == ActionBarMode.ON_PROGRESS_CHANGE) {
            actionBarManager.sendForPlayer(player);
        }

        questEffectsManager.playQuestActivateEffects(player, def);
        return true;
    }

    public boolean confirmQuestChange(Player player, String questId) {
        var data = playerData.get(player.getUniqueId());

        if (!questId.equals(data.getPendingQuestId())) {
            return false;
        }

        QuestDefinition def = questsById.get(questId);
        if (def == null) return false;

        QuestTypeConfig typeConfig = questTypeManager.getType(def.getTypeId());
        if (typeConfig != null && typeConfig.getCooldownSeconds() == 0 && data.isQuestCompleted(questId)) {
            return false;
        }

        long cdUntil = data.getQuestCooldownUntil(questId);
        if (cdUntil > 0 && cdUntil > System.currentTimeMillis()) {
            return false;
        }

        // ✅ availability (engine)
        AvailabilityResult avail = checkAvailability(player, def);
        if (!avail.isAllowed()) return false;

        String currentActive = data.getActiveQuestId();
        if (currentActive != null) {
            data.setActiveQuestId(null);
            data.setActiveQuestProgress(0);
            data.setActiveQuestStartTime(0L);
        }

        data.setActiveQuestId(questId);
        data.setActiveQuestProgress(0);
        data.setActiveQuestStartTime(System.currentTimeMillis());
        data.clearPendingQuest();
        playerData.save();

        // ✅ engine hook (activation) — optional через reflection
        try {
            QuestEngine engine = resolveEngineForQuest(def);
            invokeOptionalEngineHook(engine, "onQuestActivated", player, def, this);
        } catch (Exception ignored) {}

        if (actionBarManager.getMode() == ActionBarMode.ON_PROGRESS_CHANGE) {
            actionBarManager.sendForPlayer(player);
        }

        questEffectsManager.playQuestActivateEffects(player, def);
        return true;
    }

    public boolean cancelQuest(Player player, String questId) {
        var data = playerData.get(player.getUniqueId());
        String current = data.getActiveQuestId();
        if (current == null || !current.equalsIgnoreCase(questId)) {
            return false;
        }

        data.setActiveQuestId(null);
        data.setActiveQuestProgress(0);
        data.setActiveQuestStartTime(0L);
        data.clearPendingQuest();
        playerData.save();
        return true;
    }

    public void incrementProgress(Player player, QuestDefinition quest, int delta) {
        var data = playerData.get(player.getUniqueId());

        if (!quest.getId().equalsIgnoreCase(data.getActiveQuestId())) return;

        var o = quest.getObjective();
        int target;

        // 🎯 Визначення потрібного target
        if (o.getType() == QuestObjectiveType.TRAVEL_DISTANCE) {
            target = (int) o.getDistance();
        } else if (o.getType() == QuestObjectiveType.ENTITY_RIDE && o.getDistance() > 0) {
            target = (int) o.getDistance();
        } else {
            target = o.getAmount();
        }

        // 📈 Оновлюємо прогрес
        int newValue = data.getActiveQuestProgress() + delta;
        if (newValue > target) newValue = target;

        data.setActiveQuestProgress(newValue);
        playerData.save();

        // 🏁 Завершення або оновлення HUD
        if (newValue >= target) {
            completeQuest(player, quest);
        } else if (actionBarManager.getMode() == ActionBarMode.ON_PROGRESS_CHANGE) {
            actionBarManager.sendForPlayer(player);
        }
    }

    public void completeQuest(Player player, QuestDefinition quest) {
        var data = playerData.get(player.getUniqueId());

        // Лічильник виконаних (для топу / статистики)
        data.incrementCompleted(quest.getTypeId());

        QuestTypeConfig typeConfig = questTypeManager.getType(quest.getTypeId());

        // ✅ ВАЖЛИВО: зберігаємо "виконано хоч раз" для availability.after
        data.addCompletedQuest(quest.getId());

        // cooldown per quest
        if (typeConfig != null && typeConfig.getCooldownSeconds() > 0) {
            long cooldownUntil = System.currentTimeMillis() + typeConfig.getCooldownSeconds() * 1000L;
            data.setQuestCooldownUntil(quest.getId(), cooldownUntil);
        }

        // clear active (без залежності від clearActiveQuest())
        data.setActiveQuestId(null);
        data.setActiveQuestProgress(0);
        data.setActiveQuestStartTime(0L);
        data.clearPendingQuest();
        playerData.save();

        // UX + rewards
        actionBarManager.sendCompleteMessage(player, quest);
        questEffectsManager.playQuestFinishEffects(player, quest);
        RewardHandler.giveRewards(player, quest);

        // ✅ engine hook (completion) — ТІЛЬКИ ТЕ, що є в QuestEngine інтерфейсі:
        // onQuestCompleted(Player, QuestDefinition, QuestManager)
        try {
            QuestEngine engine = resolveEngineForQuest(quest);
            if (engine != null) {
                engine.onQuestCompleted(player, quest, this);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Викликається хендлерами напряму — завершення квесту вручну
     */
    public void complete(Player player, QuestDefinition quest) {
        if (player == null || quest == null) return;

        var data = playerData.get(player.getUniqueId());
        data.setActiveQuestProgress(quest.getObjective().getAmount());

        completeQuest(player, quest);
    }

    /**
     * ✅ Availability check через QuestEngine#checkAvailability(Player, QuestDefinition, QuestManager)
     */
    public AvailabilityResult checkAvailability(Player player, QuestDefinition quest) {
        QuestTypeConfig typeConfig = questTypeManager.getType(quest.getTypeId());

        QuestEngine engine = resolveEngine(typeConfig);
        if (engine == null) return AvailabilityResult.ok();

        try {
            return engine.checkAvailability(player, quest, this);
        } catch (Exception ex) {
            // якщо якийсь engine впав — не ламаємо активацію
            return AvailabilityResult.ok();
        }
    }

    /**
     * Збережений метод (може десь використовуватись).
     * Повертає engine за типом (typeConfig.engine), або null.
     */
    public QuestEngine resolveEngine(QuestTypeConfig typeConfig) {
        if (plugin.getAddonManager() == null) return null;
        if (typeConfig == null) return null;

        String engineId = null;
        try {
            engineId = typeConfig.getEngine();
        } catch (Exception ignored) {}

        if (engineId == null || engineId.isBlank()) engineId = "default";
        return plugin.getAddonManager().getEngine(engineId);
    }

    /**
     * Engine для конкретного квесту:
     * 1) якщо в QuestDefinition є engineId (з YAML "engine") -> беремо його
     * 2) legacy: addonData("engine")
     * 3) інакше беремо engine з QuestTypeConfig
     * 4) fallback: "default"
     *
     * ВАЖЛИВО: у тебе зараз немає методу getEngineId() у QuestDefinition (по скріну).
     * Тому беремо engine через reflection (підтримка двох варіантів без компіляційних помилок).
     */
    private QuestEngine resolveEngineForQuest(QuestDefinition quest) {
        if (plugin.getAddonManager() == null || quest == null) return null;

        String engineId = null;

        // (1) QuestDefinition#getEngineId() (якщо існує)
        engineId = readStringViaReflection(quest, "getEngineId");

        // (1b) QuestDefinition#getEngine() (інколи так називають)
        if (engineId == null || engineId.isBlank()) {
            engineId = readStringViaReflection(quest, "getEngine");
        }

        // (2) optional legacy: addonData("engine")
        if (engineId == null || engineId.isBlank()) {
            try {
                Object raw = quest.getAddonData("engine", Object.class);
                if (raw != null) engineId = String.valueOf(raw);
            } catch (Exception ignored) {}
        }

        // (3) type engine
        if (engineId == null || engineId.isBlank()) {
            QuestTypeConfig typeConfig = questTypeManager.getType(quest.getTypeId());
            if (typeConfig != null) {
                try {
                    engineId = typeConfig.getEngine();
                } catch (Exception ignored) {}
            }
        }

        // (4) default
        if (engineId == null || engineId.isBlank()) engineId = "default";

        return plugin.getAddonManager().getEngine(engineId);
    }

    private String readStringViaReflection(Object target, String methodName) {
        if (target == null || methodName == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            return v == null ? null : String.valueOf(v);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void sendActionBarForPlayer(Player player) {
        actionBarManager.sendForPlayer(player);
    }

    /* =========================================================
     *                    УТИЛІТИ / ТОП
     * ========================================================= */

    public Collection<QuestDefinition> getAll() {
        return questsById.values();
    }

    public Collection<QuestDefinition> getByType(String typeId) {
        return questsById.values().stream()
                .filter(q -> q.getTypeId().equalsIgnoreCase(typeId))
                .collect(Collectors.toList());
    }

    public Collection<QuestDefinition> getByType(QuestTypeConfig typeConfig) {
        return getByType(typeConfig.getId());
    }

    public QuestDefinition getById(String id) {
        return questsById.get(id);
    }

    public List<TopEntry> getTop(String typeId) {
        return topManager.getTop(typeId);
    }

    public List<TopEntry> getTop(QuestTypeConfig typeConfig) {
        return topManager.getTop(typeConfig);
    }

    public List<String> getAllIds() {
        return new ArrayList<>(questsById.keySet());
    }

    /* =========================================================
     *                    ГЕТЕРИ
     * ========================================================= */

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

    public void onDisable() {
        actionBarManager.stopTask();
    }

    public QuestbornPlugin getPlugin() {
        return plugin;
    }

    /* =========================================================
     *                OPTIONAL ENGINE HOOKS (REFLECTION)
     * ========================================================= */

    private void invokeOptionalEngineHook(QuestEngine engine, String methodName, Object... args) {
        if (engine == null || methodName == null) return;

        Method best = null;

        for (Method m : engine.getClass().getMethods()) {
            if (!m.getName().equals(methodName)) continue;

            Class<?>[] p = m.getParameterTypes();
            if (p.length != args.length) continue;

            boolean ok = true;
            for (int i = 0; i < p.length; i++) {
                Object a = args[i];
                if (a == null) continue;
                if (!wrap(p[i]).isAssignableFrom(a.getClass())) {
                    ok = false;
                    break;
                }
            }
            if (!ok) continue;

            best = m;
            break;
        }

        if (best == null) return;

        try {
            best.setAccessible(true);
            best.invoke(engine, args);
        } catch (Exception ignored) {}
    }

    private static Class<?> wrap(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == boolean.class) return Boolean.class;
        if (c == byte.class) return Byte.class;
        if (c == short.class) return Short.class;
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == float.class) return Float.class;
        if (c == double.class) return Double.class;
        if (c == char.class) return Character.class;
        return c;
    }
}
