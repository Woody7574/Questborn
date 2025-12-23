package ua.woody.questborn.managers;

import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.effects.EffectPresetManager;
import ua.woody.questborn.effects.QuestEffects;
import ua.woody.questborn.model.QuestDefinition;

public class QuestEffectsManager {

    private final QuestbornPlugin plugin;
    private final EffectPresetManager effectPresetManager;

    public QuestEffectsManager(QuestbornPlugin plugin, EffectPresetManager effectPresetManager) {
        this.plugin = plugin;
        this.effectPresetManager = effectPresetManager;
    }

    public void playQuestActivateEffects(Player player, QuestDefinition quest) {
        if (quest == null) return;

        QuestEffects effects = quest.getEffects();
        if (effects == null) return;

        String presetId = effects.getActivatePresetId();
        if (presetId != null && !presetId.trim().isEmpty()) {
            effectPresetManager.playPreset(player, presetId);
        }
    }

    public void playQuestFinishEffects(Player player, QuestDefinition quest) {
        if (quest == null) return;

        QuestEffects effects = quest.getEffects();
        if (effects == null) return;

        String presetId = effects.getCompletePresetId();
        if (presetId != null && !presetId.trim().isEmpty()) {
            effectPresetManager.playPreset(player, presetId);
        }
    }

    // Застарілі методи для зворотної сумісності
    public void playQuestActivateEffects(Player player) {
        // Не робимо нічого для зворотної сумісності
    }

    public void playQuestFinishEffects(Player player) {
        // Не робимо нічого для зворотної сумісності
    }
}