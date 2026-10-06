package ua.woody.questborn.effects;

public class QuestEffects {
    private final String activatePresetId;
    private final String completePresetId;
    private final String stageChangePresetId;

    public QuestEffects(String activatePresetId, String completePresetId, String stageChangePresetId) {
        this.activatePresetId = activatePresetId;
        this.completePresetId = completePresetId;
        this.stageChangePresetId = stageChangePresetId;
    }

    public String getActivatePresetId() {
        return activatePresetId;
    }

    public String getCompletePresetId() {
        return completePresetId;
    }

    public String getStageChangePresetId() {
        return stageChangePresetId;
    }
}
