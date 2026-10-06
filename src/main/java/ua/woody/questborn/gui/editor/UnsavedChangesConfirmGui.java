package ua.woody.questborn.gui.editor;

import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.SimpleBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import ua.woody.questborn.utils.HexItemBuilder;

public class UnsavedChangesConfirmGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final Runnable onSaveAndExit;
    private final Runnable onDiscardAndExit;
    private final Runnable onReturnToEditor;
    private boolean isNavigating = false;

    public UnsavedChangesConfirmGui(Player player, Runnable onSaveAndExit, Runnable onDiscardAndExit, Runnable onReturnToEditor) {
        this(QuestbornPlugin.getInstance(), player, onSaveAndExit, onDiscardAndExit, onReturnToEditor);
    }

    public UnsavedChangesConfirmGui(QuestbornPlugin plugin, Player player, Runnable onSaveAndExit, Runnable onDiscardAndExit, Runnable onReturnToEditor) {
        this.plugin = plugin;
        this.player = player;
        this.onSaveAndExit = onSaveAndExit;
        this.onDiscardAndExit = onDiscardAndExit;
        this.onReturnToEditor = onReturnToEditor;
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
        String titleText = this.tr("common_editor.unsaved_modal.title");
        Gui gui = ((SimpleBuilder)((SimpleBuilder)((SimpleBuilder)Gui.gui().title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors(titleText))))).rows(5)).disableAllInteractions()).create();
        gui.setCloseGuiAction(event -> {
            if (!this.isNavigating && this.onReturnToEditor != null) {
                this.isNavigating = true;
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> this.onReturnToEditor.run());
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.getFiller().fill(new GuiItem(bg));
        String infoTitle = this.tr("common_editor.unsaved_modal.info_title");
        String infoLore = this.tr("common_editor.unsaved_modal.info_lore");
        ItemStack infoBtn = HexItemBuilder.from(Material.WRITABLE_BOOK).name(ColorFormatter.format(infoTitle)).lore(ColorFormatter.format(infoLore)).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(2, 5, new GuiItem(infoBtn));
        String saveTitle = this.tr("common_editor.unsaved_modal.save_exit");
        String saveLore = this.tr("common_editor.unsaved_modal.save_exit_lore");
        ItemStack saveBtn = HexItemBuilder.from(Material.LIME_DYE).name(ColorFormatter.format(saveTitle)).lore(ColorFormatter.format(saveLore)).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(3, 3, new GuiItem(saveBtn, event -> {
            if (this.isNavigating) {
                return;
            }
            this.isNavigating = true;
            if (this.onSaveAndExit != null) {
                this.onSaveAndExit.run();
            }
        }));
        String discardTitle = this.tr("common_editor.unsaved_modal.discard_exit");
        String discardLore = this.tr("common_editor.unsaved_modal.discard_exit_lore");
        ItemStack discardBtn = HexItemBuilder.from(Material.RED_DYE).name(ColorFormatter.format(discardTitle)).lore(ColorFormatter.format(discardLore)).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(3, 7, new GuiItem(discardBtn, event -> {
            if (this.isNavigating) {
                return;
            }
            this.isNavigating = true;
            if (this.onDiscardAndExit != null) {
                this.onDiscardAndExit.run();
            }
        }));
        String returnTitle = this.tr("common_editor.unsaved_modal.return_editor");
        String returnLore = this.tr("common_editor.unsaved_modal.return_editor_lore");
        ItemStack returnBtn = HexItemBuilder.from(Material.ORANGE_DYE).name(ColorFormatter.format(returnTitle)).lore(ColorFormatter.format(returnLore)).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(4, 5, new GuiItem(returnBtn, event -> {
            if (this.isNavigating) {
                return;
            }
            this.isNavigating = true;
            if (this.onReturnToEditor != null) {
                this.onReturnToEditor.run();
            }
        }));
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }
}
