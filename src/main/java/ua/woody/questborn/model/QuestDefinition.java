package ua.woody.questborn.model;

import ua.woody.questborn.effects.QuestEffects;
import ua.woody.questborn.lang.ColorFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestDefinition {

    /* =========================================================
     *                      CORE FIELDS
     * ========================================================= */

    private final String id;
    private final String displayName;
    private final String typeId;

    /** Quest engine id (null / "default" = default engine) */
    private final String engine;

    private final List<String> description;
    private final List<String> rewardsDescription;
    private final QuestObjective objective;

    private final long timeLimitSeconds;
    private final String seasonTag;
    private final QuestEffects effects;
    private final Map<String, Object> rewards;

    /** Availability rules (after, permission, etc.) */
    private final QuestAvailability availability;

    /** Slot / order in GUI (-1 = not set) */
    private final int slot;

    /* =========================================================
     *                      RUNTIME
     * ========================================================= */

    private long startedAt = System.currentTimeMillis();

    /** Addon-specific runtime data */
    private final Map<String, Object> addonData = new HashMap<>();

    /* =========================================================
     *                      CONSTRUCTORS
     * ========================================================= */

    /**
     * Backward-compatible constructor (engine = default)
     */
    public QuestDefinition(
            String id,
            String name,
            String typeId,
            List<String> description,
            List<String> rewardsDescription,
            QuestObjective objective,
            long timeLimitSeconds,
            String seasonTag,
            QuestEffects effects,
            Map<String, Object> rewards
    ) {
        this(
                id,
                name,
                typeId,
                null,
                description,
                rewardsDescription,
                objective,
                timeLimitSeconds,
                seasonTag,
                effects,
                rewards,
                null,
                -1
        );
    }

    /**
     * Main constructor
     */
    public QuestDefinition(
            String id,
            String name,
            String typeId,
            String engine,
            List<String> description,
            List<String> rewardsDescription,
            QuestObjective objective,
            long timeLimitSeconds,
            String seasonTag,
            QuestEffects effects,
            Map<String, Object> rewards,
            QuestAvailability availability,
            int slot
    ) {
        this.id = id;
        this.displayName = ColorFormatter.applyColors(name);
        this.typeId = typeId;
        this.engine = (engine == null || engine.isBlank())
                ? null
                : engine.toLowerCase();

        this.description = description;
        this.rewardsDescription = rewardsDescription;
        this.objective = objective;
        this.timeLimitSeconds = timeLimitSeconds;
        this.seasonTag = seasonTag;
        this.effects = effects;
        this.rewards = rewards;
        this.availability = availability;
        this.slot = slot;
    }

    /* =========================================================
     *                      RUNTIME API
     * ========================================================= */

    public void markStarted() {
        this.startedAt = System.currentTimeMillis();
    }

    public boolean startedRecently(long millis) {
        return System.currentTimeMillis() - startedAt < millis;
    }

    public boolean startedRecently() {
        return startedRecently(1500);
    }

    /* =========================================================
     *                      GETTERS
     * ========================================================= */

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTypeId() {
        return typeId;
    }

    /** @return engine id or null (default engine) */
    public String getEngine() {
        return engine;
    }

    public List<String> getDescription() {
        return description;
    }

    public List<String> getRewardsDescription() {
        return rewardsDescription;
    }

    public QuestObjective getObjective() {
        return objective;
    }

    public long getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public String getSeasonTag() {
        return seasonTag;
    }

    public Map<String, Object> getRewards() {
        return rewards;
    }

    public QuestEffects getEffects() {
        return effects;
    }

    public QuestAvailability getAvailability() {
        return availability;
    }

    public int getSlot() {
        return slot;
    }

    /* =========================================================
     *                  ADDON DATA API
     * ========================================================= */

    public void setAddonData(String addonId, Object data) {
        if (addonId == null) return;
        addonData.put(addonId.toLowerCase(), data);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAddonData(String addonId, Class<T> type) {
        if (addonId == null || type == null) return null;
        Object data = addonData.get(addonId.toLowerCase());
        return type.isInstance(data) ? (T) data : null;
    }
}
