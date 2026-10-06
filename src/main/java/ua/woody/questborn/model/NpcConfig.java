package ua.woody.questborn.model;

import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.lang.ColorFormatter;
import ua.woody.questborn.managers.QuestTypeManager;
import ua.woody.questborn.managers.QuestManager;

import java.util.ArrayList;
import java.util.List;
import ua.woody.questborn.model.npc.mood.MoodRule;

public class NpcConfig {
    private final String npcId;
    private String guiTitle;
    private String displayName;
    private final List<String> startsQuests = new ArrayList<>();
    private final List<String> finishesQuests = new ArrayList<>();
    private final List<String> startsTypes = new ArrayList<>();
    private final List<String> finishesTypes = new ArrayList<>();
    private Double indicatorHeightOffset;
    private String dialogueId;
    private String voice;
    private String headTexture;
    private String defaultMood = "NEUTRAL";
    private List<MoodRule> moodRules = new ArrayList<>();

    public NpcConfig(String npcId, String guiTitle) {
        this.npcId = npcId;
        this.guiTitle = guiTitle != null ? ColorFormatter.applyColors(guiTitle) : null;
    }

    public String getNpcId() {
        return npcId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName != null ? ColorFormatter.applyColors(displayName) : null;
    }

    public String getGuiTitle() {
        return guiTitle;
    }

    public void setGuiTitle(String guiTitle) {
        this.guiTitle = guiTitle != null ? ColorFormatter.applyColors(guiTitle) : null;
    }

    public List<String> getStartsTypes() {
        return startsTypes;
    }

    public List<String> getFinishesTypes() {
        return finishesTypes;
    }

    public String getDialogueId() {
        return dialogueId;
    }

    public void setDialogueId(String dialogueId) {
        this.dialogueId = dialogueId;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public String getHeadTexture() {
        return headTexture;
    }

    public void setHeadTexture(String headTexture) {
        this.headTexture = headTexture;
    }

    public String getDefaultMood() {
        return defaultMood;
    }

    public void setDefaultMood(String defaultMood) {
        this.defaultMood = defaultMood != null ? defaultMood : "NEUTRAL";
    }

    public List<MoodRule> getMoodRules() {
        return moodRules;
    }

    public void setMoodRules(List<MoodRule> moodRules) {
        this.moodRules = moodRules != null ? moodRules : new ArrayList<>();
        if (!this.moodRules.isEmpty()) {
            this.moodRules.sort(java.util.Comparator.comparingInt(MoodRule::getPriority).reversed());
        }
    }

    public Double getIndicatorHeightOffset() {
        return indicatorHeightOffset;
    }

    public void setIndicatorHeightOffset(Double indicatorHeightOffset) {
        this.indicatorHeightOffset = indicatorHeightOffset;
    }

    public List<String> getStartsQuests() {
        return startsQuests;
    }

    public List<String> getFinishesQuests() {
        return finishesQuests;
    }

    public boolean startsQuest(String questId, String typeId) {
        if (!startsTypes.isEmpty()) {
            return typeId != null && startsTypes.contains(typeId);
        }
        return startsQuests.contains(questId);
    }

    public boolean finishesQuest(String questId, String typeId) {
        if (!finishesTypes.isEmpty()) {
            return typeId != null && finishesTypes.contains(typeId);
        }
        return finishesQuests.contains(questId);
    }

    public boolean isEmpty() {
        return startsQuests.isEmpty() && finishesQuests.isEmpty() && startsTypes.isEmpty() && finishesTypes.isEmpty() && dialogueId == null && voice == null && (indicatorHeightOffset == null || indicatorHeightOffset == 0.0) && displayName == null && headTexture == null && moodRules.isEmpty();
    }
}
