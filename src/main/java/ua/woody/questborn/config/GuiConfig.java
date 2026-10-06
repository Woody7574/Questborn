package ua.woody.questborn.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import ua.woody.questborn.QuestbornPlugin;

import java.util.ArrayList;
import java.util.List;

public class GuiConfig {
    private final QuestbornPlugin plugin;
    private ua.woody.questborn.util.ProgressBarUtil progressBarUtil;
    private boolean progressBarEnabled;

    private boolean centerNpcQuests;
    private boolean centerNpcTypes;
    private boolean hideNpcBoundQuests;
    private boolean showRewardsInList;

    public GuiConfig(QuestbornPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        this.centerNpcQuests = plugin.getConfig().getBoolean("gui.center-npc-quests", true);
        this.centerNpcTypes = plugin.getConfig().getBoolean("gui.center-npc-types", true);
        this.hideNpcBoundQuests = plugin.getConfig().getBoolean("gui.hide-npc-bound-quests", true);
        this.showRewardsInList = plugin.getConfig().getBoolean("gui.quest-list.show-rewards", true);

        this.progressBarEnabled = plugin.getConfig().getBoolean("gui.details.progress-bar.enabled", true);
        if (this.progressBarEnabled) {
            this.progressBarUtil = ua.woody.questborn.util.ProgressBarUtil
                    .fromConfig(plugin.getConfig().getConfigurationSection("progress-bar"));
        } else {
            this.progressBarUtil = null;
        }
    }

    public void reload() {
        load();
    }

    public String getProgressBar(int current, int max) {
        if (!progressBarEnabled || progressBarUtil == null) {
            return "";
        }
        return progressBarUtil.getProgressBar(current, max);
    }

    public boolean isCenterNpcQuests() {
        return centerNpcQuests;
    }

    public boolean isCenterNpcTypes() {
        return centerNpcTypes;
    }

    public boolean isHideNpcBoundQuests() {
        return hideNpcBoundQuests;
    }

    public boolean isShowRewardsInList() {
        return showRewardsInList;
    }
}
