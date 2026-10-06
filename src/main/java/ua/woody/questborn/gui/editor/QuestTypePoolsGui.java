package ua.woody.questborn.gui.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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
import ua.woody.questborn.gui.editor.QuestConfirmationGui;
import ua.woody.questborn.gui.editor.QuestTypePoolSlotEditorGui;
import ua.woody.questborn.lang.ColorFormatter;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestTypePoolsGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String typeId;
    private final File typeFile;
    private final YamlConfiguration typeConfig;
    private final Runnable backAction;
    private final Consumer<Runnable> onEsc;
    private PaginatedGui gui;
    private Runnable onDirty = () -> {};
    private boolean isNavigating = false;

    public QuestTypePoolsGui(QuestbornPlugin plugin, Player player, String typeId, File typeFile, YamlConfiguration typeConfig, Runnable backAction, Consumer<Runnable> onEsc) {
        this.plugin = plugin;
        this.player = player;
        this.typeId = typeId;
        this.typeFile = typeFile;
        this.typeConfig = typeConfig;
        this.backAction = backAction;
        this.onEsc = onEsc;
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

    public QuestTypePoolsGui setOnDirty(Runnable onDirty) {
        this.onDirty = onDirty;
        return this;
    }

    private void saveConfig() {
        if (this.onDirty != null) {
            this.onDirty.run();
        }
    }

    public void refresh() {
        if (this.gui != null && !this.isNavigating) {
            this.gui.clearPageItems();
            this.populatePools();
            this.gui.update();
        } else if (!this.isNavigating && !EditorSessionManager.isForceClosing) {
            this.open();
        }
    }

    public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        this.migrateLegacyPools();
        String title = EditorUtils.truncateGuiTitle(this.tr("quest_type_editor.pools_gui.title"));
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(ColorFormatter.formatComponent(title))).rows(6)).pageSize(45).disableAllInteractions()).create();
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        this.gui.setCloseGuiAction(event -> {
            Runnable closeAction;
            if (!this.isNavigating && !EditorSessionManager.isForceClosing && (closeAction = EditorSessionManager.getSession(this.player.getUniqueId())) != null) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, closeAction);
            }
        });
        this.setupButtons();
        this.populatePools();
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }

    private void setupButtons() {
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            this.backAction.run();
        }));
        ItemStack addItem = HexItemBuilder.from(Material.EMERALD_BLOCK).name(ColorFormatter.format(this.tr("quest_type_editor.pools_gui.add_title"))).lore(ColorFormatter.format(this.tr("quest_type_editor.pools_gui.add_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 5, new GuiItem(addItem, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInput(this.player, EditorChat.formatPrompt(this.tr("quest_type_editor.prompts.pool_name")), input -> {
                String sanitized;
                if (input != null && !input.isBlank() && !(sanitized = input.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_")).isEmpty()) {
                    this.typeConfig.set("rotation-pools." + sanitized, new ArrayList());
                    this.saveConfig();
                }
                this.open();
            });
        }));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 4, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 6, new GuiItem(nextItem, event -> this.gui.next()));
    }

    private void migrateLegacyPools() {
        if (!this.typeConfig.isConfigurationSection("rotation-pools")) {
            return;
        }
        List legacySlots = this.typeConfig.getIntegerList("rotation-slots");
        if (legacySlots == null || legacySlots.isEmpty()) {
            return;
        }
        ConfigurationSection poolSec = this.typeConfig.getConfigurationSection("rotation-pools");
        if (poolSec == null) {
            return;
        }
        boolean needsMigration = false;
        for (String key : poolSec.getKeys(false)) {
            if (poolSec.isList(key)) continue;
            needsMigration = true;
            break;
        }
        if (needsMigration) {
            int currentSlotIndex = 0;
            for (String key : poolSec.getKeys(false)) {
                if (poolSec.isList(key)) continue;
                int count = poolSec.getInt(key, 1);
                ArrayList<Integer> slots = new ArrayList<Integer>();
                for (int i = 0; i < count; ++i) {
                    if (currentSlotIndex >= legacySlots.size()) continue;
                    slots.add((Integer)legacySlots.get(currentSlotIndex));
                    ++currentSlotIndex;
                }
                this.typeConfig.set("rotation-pools." + key, slots);
            }
            this.typeConfig.set("rotation-slots", null);
        }
    }

    private void populatePools() {
        ConfigurationSection poolSec;
        boolean isEmpty = true;
        if (this.typeConfig.isConfigurationSection("rotation-pools") && (poolSec = this.typeConfig.getConfigurationSection("rotation-pools")) != null && !poolSec.getKeys(false).isEmpty()) {
            isEmpty = false;
            ArrayList<String> sortedPools = new ArrayList(poolSec.getKeys(false));
            sortedPools.sort(String::compareToIgnoreCase);
            Material[] shulkerColors = new Material[]{Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX, Material.LIME_SHULKER_BOX, Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX, Material.CYAN_SHULKER_BOX, Material.PURPLE_SHULKER_BOX, Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX, Material.GREEN_SHULKER_BOX, Material.RED_SHULKER_BOX, Material.BLACK_SHULKER_BOX};
            for (String poolName : poolSec.getKeys(false)) {
                int count = poolSec.isList(poolName) ? poolSec.getIntegerList(poolName).size() : poolSec.getInt(poolName, 1);
                int colorIndex = Math.abs(poolName.hashCode()) % shulkerColors.length;
                Material iconMat = shulkerColors[colorIndex];
                ItemStack item = HexItemBuilder.from(iconMat).name(ColorFormatter.format(this.tr("quest_type_editor.pools_gui.item_title", Map.of("pool", poolName)))).lore(this.plugin.getLanguage().trEditorList("quest_type_editor.pools_gui.item_lore", Map.of("count", String.valueOf(count))), "", this.plugin.getLanguage().trEditorList("quest_type_editor.pools_gui.item_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
                this.gui.addItem(new GuiItem(item, event -> {
                    if (event.getClick() == ClickType.LEFT) {
                        this.isNavigating = true;
                        new QuestTypePoolSlotEditorGui(this.plugin, this.player, this.typeId, this.typeFile, this.typeConfig, poolName, () -> this.open()).setOnDirty(this.onDirty).open();
                    } else if (event.getClick() == ClickType.DROP || event.getClick() == ClickType.SHIFT_RIGHT) {
                        this.isNavigating = true;
                        new QuestConfirmationGui(this.plugin, this.player, this.tr("quest_type_editor.pools_gui.delete_confirm_title", Map.of("pool", poolName)), this.tr("quest_type_editor.pools_gui.delete_confirm_detail", Map.of("pool", poolName)), () -> {
                            this.typeConfig.set("rotation-pools." + poolName, null);
                            if (this.typeConfig.getConfigurationSection("rotation-pools").getKeys(false).isEmpty()) {
                                this.typeConfig.set("rotation-pools", null);
                                this.typeConfig.set("rotation-slots", null);
                            }
                            this.saveConfig();
                            ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> this.open());
                        }, () -> ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> this.open())).open();
                    }
                }));
            }
        }
        if (isEmpty) {
            ItemStack emptyItem = HexItemBuilder.from(Material.STRUCTURE_VOID).name(ColorFormatter.format(this.tr("quest_editor.stages_menu.empty_title"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.stages_menu.empty_lore")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(3, 5, new GuiItem(emptyItem));
        }
    }
}
