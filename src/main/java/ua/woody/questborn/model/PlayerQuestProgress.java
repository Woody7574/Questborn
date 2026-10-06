package ua.woody.questborn.model;

import java.util.*;

public class PlayerQuestProgress {
    public static class ActiveQuestData {
        private String questId;
        private long startTime;
        private int progress;
        private int currentStage = 1;
        private int stageProgress = 0;
        private final Map<Integer, Boolean> completedStages = new HashMap<>();
        private boolean itemsTransferred = false;
        private boolean isPaused = false;

        public ActiveQuestData() {}

        public ActiveQuestData(String questId, long startTime) {
            this.questId = questId;
            this.startTime = startTime;
        }

        public String getQuestId() { return questId; }
        public void setQuestId(String questId) { this.questId = questId; }

        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }

        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }

        public int getCurrentStage() { return currentStage; }
        public void setCurrentStage(int currentStage) { this.currentStage = currentStage; }

        public int getStageProgress() { return stageProgress; }
        public void setStageProgress(int stageProgress) { this.stageProgress = stageProgress; }
        public void incrementStageProgress(int delta) { this.stageProgress += delta; }

        public boolean isStageCompleted(int stage) { return completedStages.getOrDefault(stage, false); }
        public void setStageCompleted(int stage, boolean completed) { completedStages.put(stage, completed); }
        public Map<Integer, Boolean> getCompletedStages() { return completedStages; }
        public void setCompletedStages(Map<Integer, Boolean> stages) {
            this.completedStages.clear();
            if (stages != null) this.completedStages.putAll(stages);
        }

        public boolean isItemsTransferred() { return itemsTransferred; }
        public void setItemsTransferred(boolean itemsTransferred) { this.itemsTransferred = itemsTransferred; }

        private final Map<String, Integer> transferredItemsData = new HashMap<>();
        public Map<String, Integer> getTransferredItemsData() { return transferredItemsData; }
        public void setTransferredItemsData(Map<String, Integer> items) {
            this.transferredItemsData.clear();
            if (items != null) this.transferredItemsData.putAll(items);
        }
        public void addTransferredItem(QuestItem item, int amount) {
            if (item != null) {
                String key = item.serialize();
                this.transferredItemsData.put(key, this.transferredItemsData.getOrDefault(key, 0) + amount);
            }
        }
        public int getTransferredAmount(QuestItem item) {
            if (item == null) return 0;
            return this.transferredItemsData.getOrDefault(item.serialize(), 0);
        }

        public boolean isPaused() { return isPaused; }
        public void setPaused(boolean paused) { isPaused = paused; }
    }

    private final Map<String, ActiveQuestData> activeQuests = new HashMap<>();
    private String trackedQuestId;

    public Map<String, ActiveQuestData> getActiveQuests() { return activeQuests; }

    public ActiveQuestData getQuestData(String questId) {
        if (questId == null) return null;
        return activeQuests.get(questId.toLowerCase(Locale.ROOT));
    }

    public void addActiveQuest(ActiveQuestData data) {
        if (data != null && data.getQuestId() != null) {
            activeQuests.put(data.getQuestId().toLowerCase(Locale.ROOT), data);
        }
    }

    public void removeActiveQuest(String questId) {
        if (questId == null) return;
        activeQuests.remove(questId.toLowerCase(Locale.ROOT));
        if (questId.equalsIgnoreCase(trackedQuestId)) {
            trackedQuestId = null;
        }
    }

    public boolean hasActiveQuest(String questId) {
        if (questId == null) return false;
        return activeQuests.containsKey(questId.toLowerCase(Locale.ROOT));
    }

    public String getTrackedQuestId() { return trackedQuestId; }
    public void setTrackedQuestId(String trackedQuestId) { this.trackedQuestId = trackedQuestId; }

    public boolean isTracked(String questId) {
        return trackedQuestId != null && trackedQuestId.equalsIgnoreCase(questId);
    }

    private final Map<String, Integer> completedByType = new HashMap<>();

    private final Map<String, Long> lastTypeCompletion = new HashMap<>();
    private final Set<String> completedQuests = new HashSet<>();

    private final Set<String> claimedRewards = new HashSet<>();

    private final Set<String> pendingRewards = new HashSet<>();
    private final Map<String, Long> questCooldowns = new HashMap<>();
    private String pendingQuestId;
    private double travelBuffer = 0;

    private final Map<String, List<String>> assignedRotationQuests = new HashMap<>();

    private final Map<String, Long> rotationAssignedAt = new HashMap<>();

    public Map<String, Integer> getCompletedByType() { return completedByType; }
    public void incrementCompleted(String typeId) {
        if (typeId == null) return;
        String key = typeId.toLowerCase(Locale.ROOT);
        completedByType.put(key, completedByType.getOrDefault(key, 0) + 1);
    }

    public boolean isQuestCompleted(String questId) {
        if (questId == null) return false;
        return completedQuests.contains(questId.toLowerCase(Locale.ROOT));
    }

    public long getQuestCooldownUntil(String questId) {
        if (questId == null) return 0L;
        return questCooldowns.getOrDefault(questId.toLowerCase(Locale.ROOT), 0L);
    }

    public int getCompleted(String typeId) {
        if (typeId == null) return 0;
        return completedByType.getOrDefault(typeId.toLowerCase(Locale.ROOT), 0);
    }

    public Set<String> getCompletedQuests() { return completedQuests; }
    public void addCompletedQuest(String questId) {
        if (questId == null) return;
        completedQuests.add(questId.toLowerCase(Locale.ROOT));
    }

    public Set<String> getClaimedRewards() { return claimedRewards; }
    public boolean isRewardClaimed(String questId) {
        if (questId == null) return false;
        return claimedRewards.contains(questId.toLowerCase(Locale.ROOT));
    }
    public void setRewardClaimed(String questId, boolean claimed) {
        if (questId == null) return;
        if (claimed) claimedRewards.add(questId.toLowerCase(Locale.ROOT));
        else claimedRewards.remove(questId.toLowerCase(Locale.ROOT));
    }

    public Map<String, Long> getQuestCooldowns() { return questCooldowns; }
    public void setQuestCooldownUntil(String questId, long time) {
        if (questId == null) return;
        questCooldowns.put(questId.toLowerCase(Locale.ROOT), time);
    }

    public String getPendingQuestId() { return pendingQuestId; }
    public void setPendingQuestId(String pendingQuestId) { this.pendingQuestId = pendingQuestId; }
    public void clearPendingQuest() { this.pendingQuestId = null; }

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

    public long getLastTypeCompletion(String typeId) {
        if (typeId == null)
            return 0L;
        return lastTypeCompletion.getOrDefault(typeId.toLowerCase(Locale.ROOT), 0L);
    }

    public void setLastTypeCompletion(String typeId, long time) {
        if (typeId == null)
            return;
        lastTypeCompletion.put(typeId.toLowerCase(Locale.ROOT), time);
    }

    public Map<String, Long> getLastTypeCompletionMap() {
        return lastTypeCompletion;
    }

    private String skinTexture;
    private String skinSignature;

    public String getSkinTexture() {
        return skinTexture;
    }

    public void setSkinTexture(String skinTexture) {
        this.skinTexture = skinTexture;
    }

    public String getSkinSignature() {
        return skinSignature;
    }

    public void setSkinSignature(String skinSignature) {
        this.skinSignature = skinSignature;
    }

    public Set<String> getPendingRewards() {
        return pendingRewards;
    }

    public boolean hasPendingReward(String questId) {
        if (questId == null)
            return false;
        return pendingRewards.contains(questId.toLowerCase(Locale.ROOT));
    }

    public void addPendingReward(String questId) {
        if (questId == null)
            return;
        pendingRewards.add(questId.toLowerCase(Locale.ROOT));
    }

    public void removePendingReward(String questId) {
        if (questId == null)
            return;
        pendingRewards.remove(questId.toLowerCase(Locale.ROOT));
    }

    public List<String> getAssignedRotationQuests(String typeId) {
        if (typeId == null)
            return new ArrayList<>();
        return assignedRotationQuests.getOrDefault(typeId.toLowerCase(Locale.ROOT), new ArrayList<>());
    }

    public void setAssignedRotationQuests(String typeId, List<String> questIds) {
        if (typeId == null)
            return;
        assignedRotationQuests.put(typeId.toLowerCase(Locale.ROOT),
                questIds != null ? questIds : new ArrayList<>());
    }

    public long getRotationAssignedAt(String typeId) {
        if (typeId == null)
            return 0L;
        return rotationAssignedAt.getOrDefault(typeId.toLowerCase(Locale.ROOT), 0L);
    }

    public void setRotationAssignedAt(String typeId, long timestamp) {
        if (typeId == null)
            return;
        rotationAssignedAt.put(typeId.toLowerCase(Locale.ROOT), timestamp);
    }

    public Map<String, List<String>> getAssignedRotationQuestsMap() {
        return assignedRotationQuests;
    }

    public Map<String, Long> getRotationAssignedAtMap() {
        return rotationAssignedAt;
    }

    public void clearRotationData(String typeId) {
        if (typeId == null) return;
        String key = typeId.toLowerCase(Locale.ROOT);
        assignedRotationQuests.remove(key);
        rotationAssignedAt.remove(key);
    }

    public void resetCooldown(String questId) {
        if (questId == null) return;
        questCooldowns.remove(questId.toLowerCase(Locale.ROOT));
    }

    public void resetHistory(String questId) {
        if (questId == null) return;
        completedQuests.remove(questId.toLowerCase(Locale.ROOT));
    }

    public void resetReward(String questId) {
        if (questId == null) return;
        claimedRewards.remove(questId.toLowerCase(Locale.ROOT));
        pendingRewards.remove(questId.toLowerCase(Locale.ROOT));
    }

    public void resetCompleted(String typeId) {
        if (typeId == null) return;
        completedByType.remove(typeId.toLowerCase(Locale.ROOT));
    }
}
