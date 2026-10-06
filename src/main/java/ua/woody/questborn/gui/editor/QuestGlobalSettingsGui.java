package ua.woody.questborn.gui.editor;

import java.io.File;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.lang.ColorFormatter;
import dev.triumphteam.gui.builder.gui.SimpleBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestGlobalSettingsGui {
    private boolean isNavigating = false;
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String questId;
    private final File questFile;
    private final YamlConfiguration questConfig;
    private final Runnable backAction;
    private Gui gui;
    private Runnable onDirty;

    public QuestGlobalSettingsGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.questId = questId;
        this.questFile = questFile;
        this.questConfig = questConfig;
        this.backAction = backAction;
    }

    private String tr(String path) {
        if (this.plugin != null && this.plugin.getLanguage() != null) {
            return this.plugin.getLanguage().trEditor(path);
        }
        return path;
    }

    private String tr(String path, Map<String, String> placeholders) {
        if (this.plugin != null && this.plugin.getLanguage() != null) {
            return this.plugin.getLanguage().trEditor(path, placeholders);
        }
        return path;
    }

    public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        String title = EditorUtils.truncateGuiTitle(this.tr("quest_editor.global_settings.title", Map.of("id", this.questId)));
        this.gui = ((SimpleBuilder)((SimpleBuilder)((SimpleBuilder)Gui.gui().title(ColorFormatter.formatComponent(EditorUtils.truncateGuiTitle(title)))).rows(3)).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            Runnable closeAction;
            if (!this.isNavigating && !EditorSessionManager.isForceClosing && (closeAction = EditorSessionManager.getSession(this.player.getUniqueId())) != null) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, closeAction);
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fill(new GuiItem(bg));
        this.setupButtons();
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }

    public QuestGlobalSettingsGui setOnDirty(Runnable onDirty) {
        this.onDirty = onDirty;
        return this;
    }

    private void saveConfig() {
        if (this.onDirty != null) {
            this.onDirty.run();
        }
    }

    private void setupButtons() {
        int maxP = this.questConfig.getInt("max-participants", this.questConfig.getInt("global-quest.max-participants", 0));
        ItemStack maxPItem = HexItemBuilder.from(Material.DIAMOND_HELMET).name(ColorFormatter.format(this.tr("quest_editor.global_settings.max_p_title"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.global_settings.max_p_lore", Map.of("val", String.valueOf(maxP))), "", this.plugin.getLanguage().trEditorList("quest_editor.main_menu.slot_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 3, new GuiItem(maxPItem, event -> EditorUtils.handleNumericClick(event, maxP, 0, Integer.MAX_VALUE, this.player, ColorFormatter.applyColors(this.tr("quest_editor.global_settings.max_p_prompt")), this.plugin.getChatInputManager(), val -> {
            this.questConfig.set("max-participants", val);
            if (this.questConfig.contains("global-quest.max-participants")) {
                this.questConfig.set("global-quest.max-participants", null);
            }
            this.saveConfig();
        }, () -> this.open())));
        int minP = this.questConfig.getInt("min-participants", this.questConfig.getInt("global-quest.min-participants", 0));
        ItemStack minPItem = HexItemBuilder.from(Material.GOLDEN_HELMET).name(ColorFormatter.format(this.tr("quest_editor.global_settings.min_p_title"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.global_settings.min_p_lore", Map.of("val", String.valueOf(minP))), "", this.plugin.getLanguage().trEditorList("quest_editor.main_menu.slot_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 4, new GuiItem(minPItem, event -> EditorUtils.handleNumericClick(event, minP, 0, Integer.MAX_VALUE, this.player, ColorFormatter.applyColors(this.tr("quest_editor.global_settings.min_p_prompt")), this.plugin.getChatInputManager(), val -> {
            this.questConfig.set("min-participants", val);
            if (this.questConfig.contains("global-quest.min-participants")) {
                this.questConfig.set("global-quest.min-participants", null);
            }
            this.saveConfig();
        }, () -> this.open())));
        int persL = this.questConfig.getInt("personal-limit", this.questConfig.getInt("global-quest.personal-limit", 0));
        ItemStack persLItem = HexItemBuilder.from(Material.CHAINMAIL_HELMET).name(ColorFormatter.format(this.tr("quest_editor.global_settings.limit_title"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.global_settings.limit_lore", Map.of("val", String.valueOf(persL))), "", this.plugin.getLanguage().trEditorList("quest_editor.main_menu.slot_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 5, new GuiItem(persLItem, event -> EditorUtils.handleNumericClick(event, persL, 0, Integer.MAX_VALUE, this.player, ColorFormatter.applyColors(this.tr("quest_editor.global_settings.limit_prompt")), this.plugin.getChatInputManager(), val -> {
            this.questConfig.set("personal-limit", val);
            if (this.questConfig.contains("global-quest.personal-limit")) {
                this.questConfig.set("global-quest.personal-limit", null);
            }
            this.saveConfig();
        }, () -> this.open())));
        int rewardS = this.questConfig.getInt("reward-step", this.questConfig.getInt("global-quest.reward-step", 0));
        ItemStack rewardSItem = HexItemBuilder.from(Material.EMERALD).name(ColorFormatter.format(this.tr("quest_editor.global_settings.step_title"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.global_settings.step_lore", Map.of("val", String.valueOf(rewardS))), "", this.plugin.getLanguage().trEditorList("quest_editor.main_menu.slot_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 7, new GuiItem(rewardSItem, event -> EditorUtils.handleNumericClick(event, rewardS, 0, Integer.MAX_VALUE, this.player, ColorFormatter.applyColors(this.tr("quest_editor.global_settings.step_prompt")), this.plugin.getChatInputManager(), val -> {
            this.questConfig.set("reward-step", val);
            if (this.questConfig.contains("global-quest.reward-step")) {
                this.questConfig.set("global-quest.reward-step", null);
            }
            this.saveConfig();
        }, () -> this.open())));
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(3, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            this.backAction.run();
        }));
    }
}
