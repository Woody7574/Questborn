package ua.woody.questborn.effects;

public class QuestEffects {

    private final String activatePresetId;
    private final String completePresetId;

    public QuestEffects(String activatePresetId, String completePresetId) {
        this.activatePresetId = activatePresetId;
        this.completePresetId = completePresetId;
    }

    public String getActivatePresetId() {
        return activatePresetId;
    }

    public String getCompletePresetId() {
        return completePresetId;
    }
}
