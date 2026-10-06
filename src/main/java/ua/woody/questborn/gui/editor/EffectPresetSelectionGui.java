package ua.woody.questborn.gui.editor;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.utils.HexItemBuilder;

public class EffectPresetSelectionGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private String selectedPreset;
    private final Consumer<String> onPresetSelected;
    private final Runnable backAction;
    private PaginatedGui gui;
    private String searchQuery = null;

    public EffectPresetSelectionGui(QuestbornPlugin plugin, Player player, String selectedPreset, Consumer<String> onPresetSelected, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.selectedPreset = selectedPreset;
        this.onPresetSelected = onPresetSelected;
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
        String title = ColorFormatter.applyColors(this.tr("quest_editor.main_menu.effects_gui_selection_title"));
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(6)).pageSize(45).disableAllInteractions()).create();
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        this.setupButtons();
        this.populatePresets();
        this.gui.open((HumanEntity)this.player);
    }

    private void setupButtons() {
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> this.backAction.run()));
        String searchLore = this.tr("common_editor.selection_menu.search_lore", Map.of("query", this.searchQuery == null ? this.tr("common_editor.selection_menu.search_all") : this.searchQuery));
        ItemStack searchItem = HexItemBuilder.from(Material.COMPASS).name(ColorFormatter.format(this.tr("common_editor.buttons.search"))).lore(ColorFormatter.format(searchLore), "", ColorFormatter.format(this.tr("common_editor.selection_menu.search_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 2, new GuiItem(searchItem, event -> this.plugin.getChatInputManager().requestInput(this.player, EditorChat.formatPrompt(this.tr("common_editor.selection_menu.prompt_search")), input -> {
            if (input != null) {
                this.searchQuery = input.equalsIgnoreCase("clear") || input.isBlank() ? null : input.toLowerCase();
            }
            this.open();
        })));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 4, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 6, new GuiItem(nextItem, event -> this.gui.next()));
    }

    private void populatePresets() {
        ArrayList<String> allPresets = new ArrayList<String>(this.plugin.getQuestManager().getEffectPresetManager().getAllPresetIds());
        allPresets.sort(String.CASE_INSENSITIVE_ORDER);
        for (String pId : allPresets) {
            if (this.searchQuery != null && !pId.toLowerCase().contains(this.searchQuery)) continue;
            boolean isSelected = pId.equals(this.selectedPreset);
            Material mat = isSelected ? Material.LIME_DYE : Material.GRAY_DYE;
            ItemStack item = HexItemBuilder.from(mat).name(ColorFormatter.format(this.tr("quest_editor.main_menu.effects_gui_selection_item_title", Map.of("id", pId)))).lore(ColorFormatter.format(this.tr("common_editor.selection_menu.item_id", Map.of("id", pId))), ColorFormatter.format(this.tr("common_editor.selection_menu.item_status", Map.of("status", isSelected ? this.tr("common_editor.selection_menu.status_selected") : this.tr("common_editor.selection_menu.status_unselected")))), "", ColorFormatter.format(this.tr("common_editor.selection_menu.item_click")), ColorFormatter.format(this.tr("quest_editor.main_menu.effects_gui_selection_item_play"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.addItem(new GuiItem(item, event -> {
                if (event.getClick() == ClickType.SWAP_OFFHAND) {
                    this.player.closeInventory();
                    this.plugin.getQuestManager().getEffectPresetManager().playPreset(this.player, pId);
                    ua.woody.questborn.utils.SchedulerUtils.runTaskLater(this.plugin, this.player, () -> this.open(), 60L);
                    return;
                }
                if (isSelected) {
                    this.selectedPreset = "none";
                    this.onPresetSelected.accept(null);
                } else {
                    this.selectedPreset = pId;
                    this.onPresetSelected.accept(pId);
                }
                this.open();
            }));
        }
    }
}
