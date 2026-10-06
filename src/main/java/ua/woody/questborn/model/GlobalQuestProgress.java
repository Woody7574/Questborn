package ua.woody.questborn.model;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GlobalQuestProgress {
    private final String questId;
    private final AtomicInteger globalProgress = new AtomicInteger(0);
    private final Map<UUID, Integer> contributions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastContributionTime = new ConcurrentHashMap<>();
    private boolean completed = false;
    private boolean activationReached = false;
    private long startedAt;

    private final java.util.Set<UUID> forfeitedPlayers = ConcurrentHashMap.newKeySet();

    public GlobalQuestProgress(String questId) {
        this.questId = questId;
        this.startedAt = 0L;
    }

    public java.util.Set<UUID> getForfeitedPlayers() {
        return forfeitedPlayers;
    }

    public boolean isForfeited(UUID uuid) {
        return forfeitedPlayers.contains(uuid);
    }

    public void setForfeited(UUID uuid, boolean forfeited) {
        if (forfeited) {
            forfeitedPlayers.add(uuid);
        } else {
            forfeitedPlayers.remove(uuid);
        }
    }

    public String getQuestId() {
        return questId;
    }

    public int getGlobalProgress() {
        return globalProgress.get();
    }

    public void setGlobalProgress(int progress) {
        this.globalProgress.set(progress);
    }

    public int addGlobalProgress(int amount) {
        return this.globalProgress.addAndGet(amount);
    }

    public Map<UUID, Integer> getContributions() {
        return contributions;
    }

    public void setContribution(UUID uuid, int amount) {
        contributions.put(uuid, amount);
    }

    public int getContribution(UUID uuid) {
        return contributions.getOrDefault(uuid, 0);
    }

    public Map<UUID, Long> getLastContributionTimes() {
        return lastContributionTime;
    }

    public void setLastContributionTime(UUID uuid, long time) {
        lastContributionTime.put(uuid, time);
    }

    public long getLastContributionTime(UUID uuid) {
        return lastContributionTime.getOrDefault(uuid, 0L);
    }

    public int getParticipantCount() {
        return contributions.size();
    }

    public boolean hasParticipant(UUID uuid) {
        return contributions.containsKey(uuid);
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public boolean isActivationReached() {
        return activationReached;
    }

    public void setActivationReached(boolean activationReached) {
        this.activationReached = activationReached;
    }
}
