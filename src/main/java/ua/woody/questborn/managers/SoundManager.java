package ua.woody.questborn.managers;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;

public class SoundManager {

    private final QuestbornPlugin plugin;

    private Sound guiClickSound;
    private float guiClickVolume;
    private float guiClickPitch;

    private Sound questCompleteSound;
    private float questCompleteVolume;
    private float questCompletePitch;

    public SoundManager(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        var cfg = plugin.getConfig();

        try {
            guiClickSound = Sound.valueOf(cfg.getString("sounds.gui-click.sound", "UI_BUTTON_CLICK"));
        } catch (Exception e) {
            guiClickSound = Sound.UI_BUTTON_CLICK;
        }
        guiClickVolume = (float) cfg.getDouble("sounds.gui-click.volume", 0.4);
        guiClickPitch = (float) cfg.getDouble("sounds.gui-click.pitch", 1.1);

        try {
            questCompleteSound = Sound.valueOf(cfg.getString("sounds.quest-complete.sound", "UI_TOAST_CHALLENGE_COMPLETE"));
        } catch (Exception e) {
            questCompleteSound = Sound.UI_TOAST_CHALLENGE_COMPLETE;
        }
        questCompleteVolume = (float) cfg.getDouble("sounds.quest-complete.volume", 0.8);
        questCompletePitch = (float) cfg.getDouble("sounds.quest-complete.pitch", 1.0);
    }

    public void playGuiClick(Player player) {
        player.playSound(player.getLocation(), guiClickSound, guiClickVolume, guiClickPitch);
    }

    public void playQuestComplete(Player player) {
        player.playSound(player.getLocation(), questCompleteSound, questCompleteVolume, questCompletePitch);
    }

    // Гетери для налаштувань
    public Sound getGuiClickSound() { return guiClickSound; }
    public float getGuiClickVolume() { return guiClickVolume; }
    public float getGuiClickPitch() { return guiClickPitch; }
    public Sound getQuestCompleteSound() { return questCompleteSound; }
    public float getQuestCompleteVolume() { return questCompleteVolume; }
    public float getQuestCompletePitch() { return questCompletePitch; }
}