package ua.woody.questborn.gui.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.EffectPresetSelectionGui;
import ua.woody.questborn.gui.editor.UnsavedChangesConfirmGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.SimpleBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestEffectsGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String questId;
    private final File questFile;
    private final YamlConfiguration questConfig;
    private final Runnable backAction;
    private Gui gui;
    private Runnable onDirty;
    private boolean isNavigating = false;
    private boolean hasChanges = false;

    public QuestEffectsGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig, Runnable backAction) {
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

    public QuestEffectsGui setOnDirty(Runnable onDirty) {
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
        String title = ColorFormatter.applyColors(EditorUtils.formatGuiTitle(this.tr("quest_editor.main_menu.effects_gui_title"), this.questId, this.hasChanges ? " *" : ""));
        this.gui = ((SimpleBuilder)((SimpleBuilder)((SimpleBuilder)Gui.gui().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(3)).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            if (!this.isNavigating && this.hasChanges) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> new UnsavedChangesConfirmGui(this.player, () -> {
                    EditorUtils.saveQuestConfig(this.plugin, this.questConfig, this.questFile);
                    this.hasChanges = false;
                    this.player.closeInventory();
                }, () -> {
                    this.hasChanges = false;
                    this.player.closeInventory();
                }, () -> {
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
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(3, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            this.backAction.run();
        }));
        String activatePreset = this.questConfig.getString("quest-effects.activate", "none");
        boolean hasActivate = !activatePreset.equals("none");
        String displayActivate = hasActivate ? activatePreset : this.tr("common_editor.none");
        ArrayList<String> activateLore = new ArrayList<String>();
        activateLore.add(ColorFormatter.format(this.tr("quest_editor.main_menu.effects_activate_lore", Map.of("val", displayActivate))));
        activateLore.add("");
        activateLore.add(ColorFormatter.format(this.tr("quest_editor.main_menu.effects_click")));
        activateLore.add(ColorFormatter.format(this.tr("quest_editor.main_menu.effects_click_swap")));
        if (hasActivate) {
            activateLore.add(ColorFormatter.format(this.tr("quest_editor.main_menu.effects_click_drop")));
        }
        ItemStack activateItem = HexItemBuilder.from(Material.BEACON).name(ColorFormatter.format(this.tr("quest_editor.main_menu.effects_activate_title"))).lore(activateLore).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 3, new GuiItem(activateItem, event -> {
            if (event.getClick() == ClickType.DROP) {
                if (hasActivate) {
                    this.questConfig.set("quest-effects.activate", null);
                    this.checkEmptyEffects();
                    this.markDirty();
                    this.open();
                }
                return;
            }
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                this.isNavigating = true;
                this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("common_editor.prompts.preset_id")), activatePreset.equals("none") ? "" : activatePreset, val -> {
                    if (val != null) {
                        this.questConfig.set("quest-effects.activate", val);
                        this.markDirty();
                    }
                    this.open();
                });
                return;
            }
            this.isNavigating = true;
            new EffectPresetSelectionGui(this.plugin, this.player, activatePreset, val -> {
                this.questConfig.set("quest-effects.activate", val);
                this.markDirty();
            }, this::open).open();
        }));
        String stagePreset = this.questConfig.getString("quest-effects.stage-change", "none");
        boolean hasStage = !stagePreset.equals("none");
        String displayStage = hasStage ? stagePreset : this.tr("common_editor.none");
        ArrayList<String> stageLore = new ArrayList<String>();
        stageLore.add(ColorFormatter.format(this.tr("quest_editor.main_menu.effects_stage_lore", Map.of("val", displayStage))));
        stageLore.add("");
        stageLore.add(ColorFormatter.format(this.tr("quest_editor.main_menu.effects_click")));
        stageLore.add(ColorFormatter.format(this.tr("quest_editor.main_menu.effects_click_swap")));
        if (hasStage) {
            stageLore.add(ColorFormatter.format(this.tr("quest_editor.main_menu.effects_click_drop")));
        }
        ItemStack stageItem = HexItemBuilder.from(Material.END_CRYSTAL).name(ColorFormatter.format(this.tr("quest_editor.main_menu.effects_stage_title"))).lore(stageLore).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 5, new GuiItem(stageItem, event -> {
            if (event.getClick() == ClickType.DROP) {
                if (hasStage) {
                    this.questConfig.set("quest-effects.stage-change", null);
                    this.checkEmptyEffects();
                    this.markDirty();
                    this.open();
                }
                return;
            }
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                this.isNavigating = true;
                this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("common_editor.prompts.preset_id")), stagePreset.equals("none") ? "" : stagePreset, val -> {
                    if (val != null) {
                        this.questConfig.set("quest-effects.stage-change", val);
                        this.markDirty();
                    }
                    this.open();
                });
                return;
            }
            this.isNavigating = true;
            new EffectPresetSelectionGui(this.plugin, this.player, stagePreset, val -> {
                this.questConfig.set("quest-effects.stage-change", val);
                this.markDirty();
            }, this::open).open();
        }));
        String completePreset = this.questConfig.getString("quest-effects.complete", "none");
        boolean hasComplete = !completePreset.equals("none");
        String displayComplete = hasComplete ? completePreset : this.tr("common_editor.none");
        ArrayList<String> completeLore = new ArrayList<String>();
        completeLore.add(ColorFormatter.format(this.tr("quest_editor.main_menu.effects_complete_lore", Map.of("val", displayComplete))));
        completeLore.add("");
        completeLore.add(ColorFormatter.format(this.tr("quest_editor.main_menu.effects_click")));
        completeLore.add(ColorFormatter.format(this.tr("quest_editor.main_menu.effects_click_swap")));
        if (hasComplete) {
            completeLore.add(ColorFormatter.format(this.tr("quest_editor.main_menu.effects_click_drop")));
        }
        ItemStack completeItem = HexItemBuilder.from(Material.TOTEM_OF_UNDYING).name(ColorFormatter.format(this.tr("quest_editor.main_menu.effects_complete_title"))).lore(completeLore).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 7, new GuiItem(completeItem, event -> {
            if (event.getClick() == ClickType.DROP) {
                if (hasComplete) {
                    this.questConfig.set("quest-effects.complete", null);
                    this.checkEmptyEffects();
                    this.markDirty();
                    this.open();
                }
                return;
            }
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                this.isNavigating = true;
                this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("common_editor.prompts.preset_id")), completePreset.equals("none") ? "" : completePreset, val -> {
                    if (val != null) {
                        this.questConfig.set("quest-effects.complete", val);
                        this.markDirty();
                    }
                    this.open();
                });
                return;
            }
            this.isNavigating = true;
            new EffectPresetSelectionGui(this.plugin, this.player, completePreset, val -> {
                this.questConfig.set("quest-effects.complete", val);
                this.markDirty();
            }, this::open).open();
        }));
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }

    private void checkEmptyEffects() {
        if (this.questConfig.isConfigurationSection("quest-effects") && this.questConfig.getConfigurationSection("quest-effects").getKeys(false).isEmpty()) {
            this.questConfig.set("quest-effects", null);
        }
    }
}
