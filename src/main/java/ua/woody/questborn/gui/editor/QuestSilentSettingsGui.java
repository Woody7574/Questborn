package ua.woody.questborn.gui.editor;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.QuestEditorListGui;
import ua.woody.questborn.gui.editor.UnsavedChangesConfirmGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.SimpleBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestSilentSettingsGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String questId;
    private final File questFile;
    private final YamlConfiguration questConfig;
    private final String yamlPath;
    private final Runnable backAction;
    private Gui gui;
    private boolean isNavigating = false;
    private boolean hasChanges = false;
    private Runnable onDirty;

    public QuestSilentSettingsGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig, String yamlPath, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.questId = questId;
        this.questFile = questFile;
        this.questConfig = questConfig;
        this.yamlPath = yamlPath;
        this.backAction = backAction;
    }

    public QuestSilentSettingsGui setOnDirty(Runnable onDirty) {
        this.onDirty = onDirty;
        return this;
    }

    private void markDirty() {
        this.hasChanges = true;
        if (this.onDirty != null) {
            this.onDirty.run();
        }
    }

    public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        String title = ColorFormatter.applyColors(EditorUtils.truncateGuiTitle("&0" + this.tr("quest_editor.silent_settings.title", Map.of("id", this.questId))));
        this.gui = ((SimpleBuilder)((SimpleBuilder)((SimpleBuilder)Gui.gui().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(4)).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            if (!this.isNavigating && this.hasChanges) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> new UnsavedChangesConfirmGui(this.player, () -> {
                    EditorUtils.saveQuestConfig(this.plugin, this.questConfig, this.questFile);
                    new QuestEditorListGui(this.plugin, this.player).open();
                }, () -> new QuestEditorListGui(this.plugin, this.player).open(), () -> {
                    Runnable returnAction = EditorSessionManager.getReturnAction(this.player.getUniqueId());
                    if (returnAction != null) {
                        returnAction.run();
                    } else {
                        this.open();
                    }
                }).open());
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(" ").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fill(new GuiItem(bg));
        this.setupButtons();
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }

    private void refresh() {
        this.isNavigating = true;
        this.open();
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

    private void toggleCategorySilent(String categoryPath) {
        Object current;
        if (!this.questConfig.isConfigurationSection(categoryPath) && (current = this.questConfig.get(categoryPath)) != null) {
            if (current instanceof List) {
                this.questConfig.set(categoryPath, null);
                this.questConfig.set(categoryPath + ".list", current);
            } else if (categoryPath.endsWith(".money")) {
                this.questConfig.set(categoryPath, null);
                this.questConfig.set(categoryPath + ".amount", current);
            } else if (categoryPath.endsWith(".xp")) {
                this.questConfig.set(categoryPath, null);
                this.questConfig.set(categoryPath + ".points", current);
            }
        }
        boolean currentSilent = this.questConfig.getBoolean(categoryPath + ".silent", false);
        this.questConfig.set(categoryPath + ".silent", (Object)(!currentSilent ? Boolean.valueOf(true) : null));
        this.markDirty();
        this.refresh();
    }

    private ItemStack createToggleButton(Material mat, String namePath, String categoryPath) {
        boolean isSilent = this.questConfig.getBoolean(categoryPath + ".silent", false);
        HexItemBuilder builder = HexItemBuilder.from(mat).name(ColorFormatter.format(this.tr(namePath))).lore(ColorFormatter.format(isSilent ? this.tr("quest_editor.rewards_menu.silent_on") : this.tr("quest_editor.rewards_menu.silent_off")), "", ColorFormatter.format(this.tr("quest_editor.rewards_menu.silent_click_lmb"))).flags(ItemFlag.HIDE_ATTRIBUTES);
        if (isSilent) {
            builder.enchant(Enchantment.DURABILITY);
        }
        return builder.build();
    }

    private void setupButtons() {
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(4, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            this.backAction.run();
        }));
        this.gui.setItem(2, 3, new GuiItem(this.createToggleButton(Material.EXPERIENCE_BOTTLE, "quest_editor.rewards_menu.xp_title_none", this.yamlPath + ".xp"), event -> this.toggleCategorySilent(this.yamlPath + ".xp")));
        this.gui.setItem(2, 4, new GuiItem(this.createToggleButton(Material.GOLD_INGOT, "quest_editor.rewards_menu.money_title", this.yamlPath + ".money"), event -> this.toggleCategorySilent(this.yamlPath + ".money")));
        this.gui.setItem(2, 5, new GuiItem(this.createToggleButton(Material.DIAMOND_SWORD, "quest_editor.rewards_menu.items_title", this.yamlPath + ".items"), event -> this.toggleCategorySilent(this.yamlPath + ".items")));
        this.gui.setItem(2, 6, new GuiItem(this.createToggleButton(Material.POTION, "quest_editor.rewards_menu.effs_title", this.yamlPath + ".effects"), event -> this.toggleCategorySilent(this.yamlPath + ".effects")));
        this.gui.setItem(2, 7, new GuiItem(this.createToggleButton(Material.IRON_CHESTPLATE, "quest_editor.rewards_menu.attr_title", this.yamlPath + ".attributes"), event -> this.toggleCategorySilent(this.yamlPath + ".attributes")));
        boolean isGlobalSilent = this.questConfig.getBoolean(this.yamlPath + ".silent", false);
        HexItemBuilder globalBuilder = HexItemBuilder.from(Material.BELL).name(ColorFormatter.format(this.tr("quest_editor.silent_settings.global_title"))).lore(ColorFormatter.format(isGlobalSilent ? this.tr("quest_editor.rewards_menu.silent_on") : this.tr("quest_editor.rewards_menu.silent_off")), "", ColorFormatter.format(this.tr("quest_editor.rewards_menu.silent_click_lmb"))).flags(ItemFlag.HIDE_ATTRIBUTES);
        if (isGlobalSilent) {
            globalBuilder.enchant(Enchantment.DURABILITY);
        }
        this.gui.setItem(4, 5, new GuiItem(globalBuilder.build(), event -> {
            this.questConfig.set(this.yamlPath + ".silent", (Object)(!isGlobalSilent ? Boolean.valueOf(true) : null));
            this.markDirty();
            this.refresh();
        }));
    }
}
