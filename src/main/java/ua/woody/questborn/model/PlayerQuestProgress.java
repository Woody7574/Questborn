package ua.woody.questborn.model;

import java.util.*;

public class PlayerQuestProgress {

    private String activeQuestId;
    private long activeQuestStartTime; // millis
    private int activeQuestProgress;

    // ЗМІНА: замінюємо EnumMap<QuestType, Integer> на Map<String, Integer>
    private final Map<String, Integer> completedByType = new HashMap<>();

    // ✅ Список завершених квестів (по ID)
    private final Set<String> completedQuests = new HashSet<>();

    // ✅ Перезарядка квестів: questId -> timestamp (millis, коли КД закінчується)
    private final Map<String, Long> questCooldowns = new HashMap<>();

    // ==========================
    // PENDING QUEST (для режиму CHANGE)
    // ==========================
    private String pendingQuestId; // ID квесту, який очікує підтвердження заміни

    // ==========================
    // TRAVEL BUFFER
    // ==========================
    private double travelBuffer = 0;

    // ✅ Додано гетер та сетер для travelBuffer
    public double getTravelBuffer() {
        return travelBuffer;
    }

    public void setTravelBuffer(double travelBuffer) {
        this.travelBuffer = travelBuffer;
    }

    public void addTravel(double dist) {
        travelBuffer += dist;
    }

    public int consumeTravel() {
        int whole = (int) travelBuffer;
        travelBuffer -= whole;
        return whole;
    }

    public String getActiveQuestId() {
        return activeQuestId;
    }

    public void setActiveQuestId(String activeQuestId) {
        this.activeQuestId = activeQuestId;
    }

    public long getActiveQuestStartTime() {
        return activeQuestStartTime;
    }

    public void setActiveQuestStartTime(long activeQuestStartTime) {
        this.activeQuestStartTime = activeQuestStartTime;
    }

    public int getActiveQuestProgress() {
        return activeQuestProgress;
    }

    public void setActiveQuestProgress(int activeQuestProgress) {
        this.activeQuestProgress = activeQuestProgress;
    }

    // ЗМІНА: оновлюємо гетер для нової мапи
    public Map<String, Integer> getCompletedByType() {
        return completedByType;
    }

    // ЗМІНА: тепер приймає typeId (String) замість QuestType
    public int getCompleted(String typeId) {
        if (typeId == null) return 0;
        return completedByType.getOrDefault(typeId.toLowerCase(Locale.ROOT), 0);
    }

    // ЗМІНА: тепер приймає typeId (String) замість QuestType
    public void incrementCompleted(String typeId) {
        if (typeId == null) return;
        String key = typeId.toLowerCase(Locale.ROOT);
        completedByType.put(key, getCompleted(typeId) + 1);
    }

    // ==========================
    //  COMPLETED QUEST IDS
    // ==========================
    public Set<String> getCompletedQuests() {
        return completedQuests;
    }

    public boolean isQuestCompleted(String questId) {
        if (questId == null) return false;
        return completedQuests.contains(questId.toLowerCase(Locale.ROOT));
    }

    public void addCompletedQuest(String questId) {
        if (questId == null) return;
        completedQuests.add(questId.toLowerCase(Locale.ROOT));
    }

    // ==========================
    //  COOLDOWNS
    // ==========================
    public Map<String, Long> getQuestCooldowns() {
        return questCooldowns;
    }

    public long getQuestCooldownUntil(String questId) {
        if (questId == null) return 0L;
        return questCooldowns.getOrDefault(questId.toLowerCase(Locale.ROOT), 0L);
    }

    public void setQuestCooldownUntil(String questId, long until) {
        if (questId == null) return;
        String key = questId.toLowerCase(Locale.ROOT);
        if (until <= 0) {
            questCooldowns.remove(key);
        } else {
            questCooldowns.put(key, until);
        }
    }

    public void clearActiveQuest() {
        this.activeQuestId = null;
        this.activeQuestProgress = 0;
        this.activeQuestStartTime = 0;
    }

    // ==========================
    //  PENDING QUEST (для режиму CHANGE)
    // ==========================
    public String getPendingQuestId() {
        return pendingQuestId;
    }

    public void setPendingQuestId(String pendingQuestId) {
        this.pendingQuestId = pendingQuestId;
    }

    public boolean hasPendingQuest() {
        return pendingQuestId != null && !pendingQuestId.isEmpty();
    }

    public void clearPendingQuest() {
        this.pendingQuestId = null;
    }

    // ==========================
    //  ДОДАТКОВІ УТІЛІТИ
    // ==========================

    /**
     * Перевіряє, чи вказаний квест очікує підтвердження
     */
    public boolean isPendingQuest(String questId) {
        return pendingQuestId != null && pendingQuestId.equalsIgnoreCase(questId);
    }

    /**
     * Встановлює квест на підтвердження та повертає попередній активний квест
     */
    public String setQuestPending(String newQuestId) {
        String previousActive = this.activeQuestId;
        this.pendingQuestId = newQuestId;
        return previousActive;
    }

    /**
     * Підтверджує заміну квесту - очищає pending та повертає ID квесту для активації
     */
    public String confirmPendingQuest() {
        String questToActivate = this.pendingQuestId;
        this.pendingQuestId = null;
        return questToActivate;
    }
}