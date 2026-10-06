package ua.woody.questborn.managers;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;

public class SoundManager {
    private final QuestbornPlugin plugin;

    private Sound guiClickSound;
    private float guiClickVolume;
    private float guiClickPitch;

    private Sound globalQuestStartSound;
    private float globalQuestStartVolume;
    private float globalQuestStartPitch;

    private Sound globalQuestCompleteSound;
    private float globalQuestCompleteVolume;
    private float globalQuestCompletePitch;

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
            globalQuestStartSound = Sound.valueOf(cfg.getString("sounds.global-quest-start.sound", "ITEM_GOAT_HORN_SOUND_0"));
        } catch (Exception e) {
            try {
                globalQuestStartSound = Sound.valueOf("ITEM_GOAT_HORN_SOUND_0");
            } catch (Exception ex) {
                globalQuestStartSound = Sound.EVENT_RAID_HORN;
            }
        }
        globalQuestStartVolume = (float) cfg.getDouble("sounds.global-quest-start.volume", 0.8);
        globalQuestStartPitch = (float) cfg.getDouble("sounds.global-quest-start.pitch", 1.0);

        try {
            globalQuestCompleteSound = Sound.valueOf(cfg.getString("sounds.global-quest-complete.sound", "BLOCK_END_PORTAL_SPAWN"));
        } catch (Exception e) {
            globalQuestCompleteSound = Sound.BLOCK_END_PORTAL_SPAWN;
        }
        globalQuestCompleteVolume = (float) cfg.getDouble("sounds.global-quest-complete.volume", 0.7);
        globalQuestCompletePitch = (float) cfg.getDouble("sounds.global-quest-complete.pitch", 0.7);
    }

    public void playGuiClick(Player player) {
        player.playSound(player.getLocation(), guiClickSound, guiClickVolume, guiClickPitch);
    }

    public void playGlobalQuestStart(Player player) {
        if (player != null && globalQuestStartSound != null) {
            player.playSound(player.getLocation(), globalQuestStartSound, globalQuestStartVolume, globalQuestStartPitch);
        }
    }

    public void playGlobalQuestComplete(Player player) {
        player.playSound(player.getLocation(), globalQuestCompleteSound, globalQuestCompleteVolume, globalQuestCompletePitch);
    }

    public Sound getGuiClickSound() { return guiClickSound; }
    public float getGuiClickVolume() { return guiClickVolume; }
    public float getGuiClickPitch() { return guiClickPitch; }
}
