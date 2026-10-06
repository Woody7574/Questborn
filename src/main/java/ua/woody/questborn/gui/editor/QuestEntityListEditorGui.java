package ua.woody.questborn.gui.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.QuestEntityTypeSelectorGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestEntityListEditorGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String questId;
    private final File questFile;
    private final YamlConfiguration questConfig;
    private final String yamlPath;
    private final String title;
    private final Runnable backAction;
    private Runnable onDirty = null;
    private PaginatedGui gui;
    private boolean isNavigating = false;

    public QuestEntityListEditorGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig, String yamlPath, String title, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.questId = questId;
        this.questFile = questFile;
        this.questConfig = questConfig;
        this.yamlPath = yamlPath;
        this.title = title;
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

    public QuestEntityListEditorGui setOnDirty(Runnable onDirty) {
        this.onDirty = onDirty;
        return this;
    }

    public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors("&0" + this.title))))).rows(6)).pageSize(45).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            Runnable closeAction;
            if (!this.isNavigating && !EditorSessionManager.isForceClosing && (closeAction = EditorSessionManager.getSession(this.player.getUniqueId())) != null) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, closeAction);
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        ItemStack addItem = HexItemBuilder.from(Material.EMERALD_BLOCK).name(ColorFormatter.format(this.tr("common_editor.buttons.add_new"))).lore(ColorFormatter.format(this.tr("quest_type_editor.entity_list.add_new_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 5, new GuiItem(addItem, event -> {
            this.isNavigating = true;
            new QuestEntityTypeSelectorGui(this.plugin, this.player, selectedEnt -> {
                List rawList = this.questConfig.getList(this.yamlPath);
                ArrayList<String> list = rawList == null ? new ArrayList<String>() : new ArrayList(rawList);
                list.add((String)selectedEnt);
                this.questConfig.set(this.yamlPath, list);
                this.saveConfig();
                this.open();
            }, () -> {
                this.isNavigating = true;
                this.open();
            }).open();
        }));
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            this.backAction.run();
        }));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 4, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 6, new GuiItem(nextItem, event -> this.gui.next()));
        this.populateList();
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }

    private void saveConfig() {
        if (this.onDirty != null) {
            this.onDirty.run();
        }
    }

    private void populateList() {
        List rawList = this.questConfig.getList(this.yamlPath);
        if (rawList == null || rawList.isEmpty()) {
            ItemStack emptyItem = HexItemBuilder.from(Material.STRUCTURE_VOID).name(ColorFormatter.format(this.tr("common_editor.item_list.empty_title"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(3, 5, new GuiItem(emptyItem));
            return;
        }
        for (int i = 0; i < rawList.size(); ++i) {
            int index = i;
            Object obj = rawList.get(i);
            String line = String.valueOf(obj);
            ItemStack baseItem = EditorUtils.getGuiItemForString(line, this.plugin);
            if (baseItem.getType() == Material.PAPER) {
                Material eggMat = EditorUtils.getDisplayMaterialForEntity(line);
                baseItem = new ItemStack(eggMat);
            }
            ItemStack item = HexItemBuilder.from(baseItem).name(ColorFormatter.format("<#ffffff>" + line.toUpperCase(Locale.ROOT))).lore(ColorFormatter.format(this.tr("quest_type_editor.entity_list.click_edit")), ColorFormatter.format(this.tr("quest_type_editor.entity_list.click_del"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.addItem(new GuiItem(item, event -> {
                if (event.getClick() == ClickType.DROP) {
                    List currentRaw = this.questConfig.getList(this.yamlPath);
                    if (currentRaw != null && index < currentRaw.size()) {
                        ArrayList currentList = new ArrayList(currentRaw);
                        currentList.remove(index);
                        this.questConfig.set(this.yamlPath, currentList);
                        this.saveConfig();
                    }
                    this.open();
                } else if (event.isLeftClick()) {
                    this.isNavigating = true;
                    new QuestEntityTypeSelectorGui(this.plugin, this.player, selectedEnt -> {
                        List currentRaw = this.questConfig.getList(this.yamlPath);
                        if (currentRaw != null && index < currentRaw.size()) {
                            ArrayList<String> currentList = new ArrayList<String>(currentRaw);
                            currentList.set(index, (String)selectedEnt);
                            this.questConfig.set(this.yamlPath, currentList);
                            this.saveConfig();
                        }
                        this.open();
                    }, () -> {
                        this.isNavigating = true;
                        this.open();
                    }).open();
                }
            }));
        }
    }
}
