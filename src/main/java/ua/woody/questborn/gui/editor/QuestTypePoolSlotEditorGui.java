package ua.woody.questborn.gui.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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

public class QuestTypePoolSlotEditorGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String typeId;
    private final File typeFile;
    private final YamlConfiguration typeConfig;
    private final String poolName;
    private final Runnable backAction;
    private Gui gui;
    private Runnable onDirty = () -> {};
    private boolean isNavigating = false;

    public QuestTypePoolSlotEditorGui(QuestbornPlugin plugin, Player player, String typeId, File typeFile, YamlConfiguration typeConfig, String poolName, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.typeId = typeId;
        this.typeFile = typeFile;
        this.typeConfig = typeConfig;
        this.poolName = poolName;
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

    public QuestTypePoolSlotEditorGui setOnDirty(Runnable onDirty) {
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
            this.setupSlots();
            this.gui.update();
        } else if (!this.isNavigating && !EditorSessionManager.isForceClosing) {
            this.open();
        }
    }

    public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        String title = EditorUtils.truncateGuiTitle(this.tr("quest_type_editor.pool_slots_gui.title", Map.of("pool", this.poolName)));
        this.gui = ((SimpleBuilder)((SimpleBuilder)((SimpleBuilder)Gui.gui().title(ColorFormatter.formatComponent(title))).rows(6)).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            Runnable closeAction;
            if (!this.isNavigating && !EditorSessionManager.isForceClosing && (closeAction = EditorSessionManager.getSession(this.player.getUniqueId())) != null) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, closeAction);
            }
        });
        this.gui.setDefaultClickAction(event -> {
            if (event.getClick().isRightClick()) {
                if (this.isNavigating) {
                    return;
                }
                this.isNavigating = true;
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> this.backAction.run());
            }
        });
        this.setupSlots();
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }

    private void setupSlots() {
        ConfigurationSection poolSec = this.typeConfig.getConfigurationSection("rotation-pools");
        for (int i = 0; i < 54; ++i) {
            ItemStack item;
            int slot = i;
            String ownerPool = null;
            if (poolSec != null) {
                for (String pName : poolSec.getKeys(false)) {
                    List slots;
                    if (!poolSec.isList(pName) || !(slots = poolSec.getIntegerList(pName)).contains(slot)) continue;
                    ownerPool = pName;
                    break;
                }
            }
            if (this.poolName.equals(ownerPool)) {
                item = HexItemBuilder.from(Material.LIME_STAINED_GLASS_PANE).name(ColorFormatter.format(this.tr("quest_type_editor.pool_slots_gui.slot_mine_title", Map.of("slot", String.valueOf(slot))))).lore(ColorFormatter.format(this.tr("quest_type_editor.pool_slots_gui.slot_mine_lore")), ColorFormatter.format(this.tr("quest_type_editor.pool_slots_gui.slot_back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
                this.gui.setItem(slot, new GuiItem(item, event -> {
                    if (event.getClick().isRightClick()) {
                        if (this.isNavigating) {
                            return;
                        }
                        this.isNavigating = true;
                        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> this.backAction.run());
                        return;
                    }
                    if (event.getClick().isLeftClick()) {
                        this.removeSlot(this.poolName, slot);
                        this.saveConfig();
                        this.refresh();
                    }
                }));
                continue;
            }
            if (ownerPool != null) {
                String takenPool = ownerPool;
                item = HexItemBuilder.from(Material.ORANGE_STAINED_GLASS_PANE).name(ColorFormatter.format(this.tr("quest_type_editor.pool_slots_gui.slot_taken_title", Map.of("slot", String.valueOf(slot))))).lore(this.plugin.getLanguage().trEditorList("quest_type_editor.pool_slots_gui.slot_taken_lore", Map.of("owner", ownerPool)), ColorFormatter.format(this.tr("quest_type_editor.pool_slots_gui.slot_back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
                this.gui.setItem(slot, new GuiItem(item, event -> {
                    if (event.getClick().isRightClick()) {
                        if (this.isNavigating) {
                            return;
                        }
                        this.isNavigating = true;
                        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> this.backAction.run());
                        return;
                    }
                    if (event.getClick().isLeftClick()) {
                        this.removeSlot(takenPool, slot);
                        this.addSlot(this.poolName, slot);
                        this.saveConfig();
                        this.refresh();
                    }
                }));
                continue;
            }
            item = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(ColorFormatter.format(this.tr("quest_type_editor.pool_slots_gui.slot_free_title", Map.of("slot", String.valueOf(slot))))).lore(ColorFormatter.format(this.tr("quest_type_editor.pool_slots_gui.slot_free_lore")), ColorFormatter.format(this.tr("quest_type_editor.pool_slots_gui.slot_back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(slot, new GuiItem(item, event -> {
                if (event.getClick().isRightClick()) {
                    if (this.isNavigating) {
                        return;
                    }
                    this.isNavigating = true;
                    ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> this.backAction.run());
                    return;
                }
                if (event.getClick().isLeftClick()) {
                    this.addSlot(this.poolName, slot);
                    this.saveConfig();
                    this.refresh();
                }
            }));
        }
    }

    private void removeSlot(String targetPool, int slot) {
        if (!this.typeConfig.isConfigurationSection("rotation-pools")) {
            return;
        }
        ConfigurationSection poolSec = this.typeConfig.getConfigurationSection("rotation-pools");
        if (poolSec == null) {
            return;
        }
        List slots = new ArrayList();
        if (poolSec.isList(targetPool)) {
            slots = poolSec.getIntegerList(targetPool);
        }
        if (slots.contains(slot)) {
            slots.remove((Object)slot);
            this.typeConfig.set("rotation-pools." + targetPool, slots);
        }
    }

    private void addSlot(String targetPool, int slot) {
        ConfigurationSection poolSec;
        if (!this.typeConfig.isConfigurationSection("rotation-pools")) {
            this.typeConfig.createSection("rotation-pools");
        }
        if ((poolSec = this.typeConfig.getConfigurationSection("rotation-pools")) == null) {
            return;
        }
        List<Integer> slots = new ArrayList();
        if (poolSec.isList(targetPool)) {
            slots = poolSec.getIntegerList(targetPool);
        } else if (poolSec.contains(targetPool)) {
            slots = new ArrayList();
        }
        if (!slots.contains(slot)) {
            slots.add(slot);
            this.typeConfig.set("rotation-pools." + targetPool, slots);
        }
    }
}
