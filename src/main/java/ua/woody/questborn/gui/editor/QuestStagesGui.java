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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.QuestConfirmationGui;
import ua.woody.questborn.gui.editor.QuestObjectiveGui;
import ua.woody.questborn.gui.editor.QuestObjectiveTypeSelectorGui;
import ua.woody.questborn.gui.editor.QuestRequiredMaterialsGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.model.EngineType;
import ua.woody.questborn.model.QuestObjectiveType;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestStagesGui {
    private boolean isNavigating = false;
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String questId;
    private final File questFile;
    private final YamlConfiguration questConfig;
    private final Runnable backAction;
    private Runnable onDirty;
    private PaginatedGui gui;

    public QuestStagesGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.questId = questId;
        this.questFile = questFile;
        this.questConfig = questConfig;
        this.backAction = backAction;
    }

    public QuestStagesGui setOnDirty(Runnable onDirty) {
        this.onDirty = onDirty;
        return this;
    }

    private void markDirty() {
        if (this.onDirty != null) {
            this.onDirty.run();
        }
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
        String title = ColorFormatter.applyColors(EditorUtils.truncateGuiTitle(this.tr("quest_editor.title_stages", Map.of("id", this.questId))));
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(6)).pageSize(45).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            Runnable closeAction;
            if (!this.isNavigating && !EditorSessionManager.isForceClosing && (closeAction = EditorSessionManager.getSession(this.player.getUniqueId())) != null) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, closeAction);
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        this.setupButtons();
        this.populateStages();
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }

    private boolean isGlobalQuest() {
        QuestTypeConfig typeCfg;
        String typeId = this.questConfig.getString("type", "");
        if (typeId != null && !typeId.isEmpty() && (typeCfg = this.plugin.getQuestManager().getQuestTypeManager().getType(typeId)) != null && typeCfg.getEngine() == EngineType.GLOBAL) {
            return true;
        }
        String folderName = this.questFile.getParentFile().getName();
        return folderName.equalsIgnoreCase("global");
    }

    private void setupButtons() {
        boolean isGlobal;
        boolean hasRoot;
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            this.backAction.run();
        }));
        ConfigurationSection pathSection = this.questConfig.getConfigurationSection("quest-path");
        int currentStages = 0;
        if (pathSection != null) {
            for (String key : pathSection.getKeys(false)) {
                if (!key.startsWith("stage-")) continue;
                ++currentStages;
            }
        }
        boolean bl = hasRoot = currentStages == 0 && (this.questConfig.contains("objective") || this.questConfig.contains("required-materials"));
        if (hasRoot) {
            currentStages = 1;
        }
        Material addMat = (isGlobal = this.isGlobalQuest()) && currentStages >= 1 ? Material.GRAY_CONCRETE : Material.EMERALD_BLOCK;
        HexItemBuilder addBuilder = HexItemBuilder.from(addMat);
        if (isGlobal && currentStages >= 1) {
            addBuilder.name(ColorFormatter.format(this.tr("quest_editor.stages_menu.add_obj_title"))).lore(ColorFormatter.format(this.tr("quest_editor.stages_menu.global_1stage_limit")));
        } else {
            addBuilder.name(ColorFormatter.format(this.tr("quest_editor.stages_menu.add_obj_title"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.stages_menu.add_obj_lore"), ColorFormatter.format(""), ColorFormatter.format(this.tr("quest_editor.stages_menu.add_obj_click_lkm")), ColorFormatter.format(this.tr("quest_editor.stages_menu.add_obj_click_pkm")));
        }
        ItemStack addItem = addBuilder.flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 5, new GuiItem(addItem, event -> {
            boolean liveHasRoot;
            ConfigurationSection pathSec = this.questConfig.getConfigurationSection("quest-path");
            int liveStagesCount = 0;
            if (pathSec != null) {
                for (String key : pathSec.getKeys(false)) {
                    if (!key.startsWith("stage-")) continue;
                    ++liveStagesCount;
                }
            }
            liveHasRoot = liveStagesCount == 0 && (this.questConfig.contains("objective") || this.questConfig.contains("required-materials"));
            if (liveHasRoot) {
                liveStagesCount = 1;
            }
            if (this.isGlobalQuest() && liveStagesCount >= 1) {
                EditorChat.sendWarning(this.player, this.tr("quest_editor.stages_menu.global_1stage_limit"));
                this.open();
                return;
            }
            if (liveHasRoot) {
                Object obj = this.questConfig.get("objective");
                Object req = this.questConfig.get("required-materials");
                if (obj != null) {
                    this.questConfig.set("quest-path.stage-1.objective", obj);
                }
                if (req != null) {
                    this.questConfig.set("quest-path.stage-1.required-materials", req);
                }
                this.questConfig.set("objective", null);
                this.questConfig.set("required-materials", null);
                this.markDirty();
                pathSec = this.questConfig.getConfigurationSection("quest-path");
            }
            this.compactStages();
            int nextId = 1;
            if (pathSec != null) {
                nextId = (int)pathSec.getKeys(false).stream().filter(k -> k.startsWith("stage-")).count() + 1;
            }
            if (event.isRightClick()) {
                String newKey = "quest-path.stage-" + nextId;
                this.questConfig.set(newKey + ".required-materials", List.of("STONE:1"));
                this.markDirty();
                this.open();
            } else {
                String newKey = "quest-path.stage-" + nextId + ".objective";
                this.isNavigating = true;
                new QuestObjectiveTypeSelectorGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, newKey, () -> this.open()).setOnDirty(this::markDirty).open();
            }
        }));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 4, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 6, new GuiItem(nextItem, event -> this.gui.next()));
    }

    private void compactStages() {
        ConfigurationSection pathSec = this.questConfig.getConfigurationSection("quest-path");
        if (pathSec == null) {
            return;
        }
        ArrayList<String> keys = new ArrayList<String>(pathSec.getKeys(false));
        keys.removeIf(k -> !k.startsWith("stage-"));
        ArrayList<Object> tempStageData = new ArrayList<Object>();
        for (String key : keys) {
            tempStageData.add(pathSec.get(key));
        }
        for (String key : keys) {
            this.questConfig.set("quest-path." + key, null);
        }
        for (int i = 0; i < tempStageData.size(); ++i) {
            this.questConfig.set("quest-path.stage-" + (i + 1), tempStageData.get(i));
        }
    }

    private void swapStages(List<String> keys, int indexA, int indexB) {
        if (indexA < 0 || indexA >= keys.size() || indexB < 0 || indexB >= keys.size()) {
            return;
        }
        String keyA = keys.get(indexA);
        String keyB = keys.get(indexB);
        Object dataA = this.questConfig.get("quest-path." + keyA);
        Object dataB = this.questConfig.get("quest-path." + keyB);
        this.questConfig.set("quest-path." + keyA, dataB);
        this.questConfig.set("quest-path." + keyB, dataA);
        this.markDirty();
        this.open();
    }

    private void populateStages() {
        boolean hasRootStage;
        ConfigurationSection pathSection = this.questConfig.getConfigurationSection("quest-path");
        ArrayList<String> keys = new ArrayList<String>();
        if (pathSection != null) {
            keys.addAll(pathSection.getKeys(false));
            keys.removeIf(k -> !k.startsWith("stage-"));
        }
        boolean bl = hasRootStage = keys.isEmpty() && (this.questConfig.contains("objective") || this.questConfig.contains("required-materials"));
        if (hasRootStage) {
            keys.add("root-stage");
        }
        if (keys.isEmpty()) {
            ItemStack emptyItem = HexItemBuilder.from(Material.STRUCTURE_VOID).name(ColorFormatter.format(this.tr("quest_editor.stages_menu.empty_title"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.stages_menu.empty_lore")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(3, 5, new GuiItem(emptyItem));
            return;
        }
        if (!hasRootStage) {
            keys.sort((k1, k2) -> {
                try {
                    int id1 = Integer.parseInt(k1.replace("stage-", ""));
                    int id2 = Integer.parseInt(k2.replace("stage-", ""));
                    return Integer.compare(id1, id2);
                }
                catch (Exception e) {
                    return k1.compareTo((String)k2);
                }
            });
        }
        for (int i = 0; i < keys.size(); ++i) {
            String key = (String)keys.get(i);
            int index = i;
            ArrayList<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add(ColorFormatter.format(this.tr("quest_editor.stages_menu.stage_click_edit")));
            if (index > 0) {
                lore.add(ColorFormatter.format(this.tr("quest_editor.stages_menu.stage_click_up")));
            }
            if (index < keys.size() - 1) {
                lore.add(ColorFormatter.format(this.tr("quest_editor.stages_menu.stage_click_down")));
            }
            lore.add(ColorFormatter.format(this.tr("quest_editor.stages_menu.stage_click_duplicate")));
            lore.add(ColorFormatter.format(this.tr("quest_editor.stages_menu.stage_click_delete")));
            String displayKey = hasRootStage ? "1" : key.replace("stage-", "");
            String typeStr = null;
            if (hasRootStage) {
                if (this.questConfig.contains("required-materials")) {
                    typeStr = "required-materials";
                } else if (this.questConfig.contains("objective.type")) {
                    typeStr = this.questConfig.getString("objective.type");
                }
            } else if (this.questConfig.contains("quest-path." + key + ".required-materials")) {
                typeStr = "required-materials";
            } else if (this.questConfig.contains("quest-path." + key + ".objective.type")) {
                typeStr = this.questConfig.getString("quest-path." + key + ".objective.type");
            }
            Material mat = Material.TARGET;
            ArrayList<String> infoLore = new ArrayList<String>();
            if ("required-materials".equals(typeStr)) {
                ConfigurationSection reqSec;
                mat = Material.CHEST;
                infoLore.add(ColorFormatter.format(this.tr("quest_editor.stages_menu.info_type_materials")));
                ConfigurationSection configurationSection = reqSec = hasRootStage ? this.questConfig.getConfigurationSection("required-materials") : this.questConfig.getConfigurationSection("quest-path." + key + ".required-materials");
                if (reqSec != null) {
                    infoLore.add(ColorFormatter.format(this.tr("quest_editor.stages_menu.info_materials_count", Map.of("count", String.valueOf(reqSec.getKeys(false).size())))));
                }
            } else if (typeStr != null) {
                ConfigurationSection objSec;
                try {
                    QuestObjectiveType objType = QuestObjectiveType.fromStringStrict(typeStr);
                    if (objType != null) {
                        mat = QuestObjectiveTypeSelectorGui.getIconForType(objType);
                    }
                }
                catch (Exception objType) {
                }
                infoLore.add(ColorFormatter.format(this.tr("quest_editor.stages_menu.info_type", Map.of("type", typeStr))));
                ConfigurationSection configurationSection = objSec = hasRootStage ? this.questConfig.getConfigurationSection("objective") : this.questConfig.getConfigurationSection("quest-path." + key + ".objective");
                if (objSec != null) {
                    if (objSec.contains("amount")) {
                        infoLore.add(ColorFormatter.format(this.tr("quest_editor.stages_menu.info_amount", Map.of("amount", objSec.getString("amount")))));
                    }
                    if (objSec.contains("target-entities")) {
                        infoLore.add(ColorFormatter.format(this.tr("quest_editor.stages_menu.info_targets", Map.of("targets", String.join((CharSequence)", ", objSec.getStringList("target-entities"))))));
                    } else if (objSec.contains("target-materials")) {
                        infoLore.add(ColorFormatter.format(this.tr("quest_editor.stages_menu.info_targets", Map.of("targets", String.join((CharSequence)", ", objSec.getStringList("target-materials"))))));
                    } else if (objSec.contains("target-items")) {
                        infoLore.add(ColorFormatter.format(this.tr("quest_editor.stages_menu.info_targets", Map.of("targets", String.join((CharSequence)", ", objSec.getStringList("target-items"))))));
                    }
                    if (objSec.contains("npc")) {
                        infoLore.add(ColorFormatter.format(this.tr("quest_editor.stages_menu.info_npc", Map.of("npc", objSec.getString("npc")))));
                    }
                }
            } else {
                infoLore.add(ColorFormatter.format(this.tr("quest_editor.stages_menu.info_type_unknown")));
            }
            lore.addAll(0, infoLore);
            ItemStack item = HexItemBuilder.from(mat).name(ColorFormatter.format(this.tr("quest_editor.stages_menu.stage_item", Map.of("key", displayKey)))).lore(lore).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.addItem(new GuiItem(item, event -> {
                if (event.isShiftClick()) {
                    if (hasRootStage) {
                        return;
                    }
                    if (event.isLeftClick()) {
                        this.swapStages(keys, index, index - 1);
                    } else if (event.isRightClick()) {
                        this.swapStages(keys, index, index + 1);
                    }
                } else if (event.isLeftClick()) {
                    boolean isMatObjective;
                    isMatObjective = hasRootStage ? this.questConfig.contains("required-materials") : this.questConfig.contains("quest-path." + key + ".required-materials");
                    if (isMatObjective) {
                        Object p = hasRootStage ? "required-materials" : "quest-path." + key + ".required-materials";
                        this.isNavigating = true;
                        new QuestRequiredMaterialsGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, (String)p, this.tr("quest_editor.stages_menu.mats_title", Map.of("key", displayKey)), () -> this.open()).setOnDirty(this::markDirty).open();
                    } else {
                        Object p = hasRootStage ? "objective" : "quest-path." + key + ".objective";
                        this.isNavigating = true;
                        new QuestObjectiveGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, (String)p, this.tr("quest_editor.stages_menu.edit_stage_title", Map.of("key", displayKey)), () -> this.open()).setOnDirty(this::markDirty).open();
                    }
                } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
                    if (this.isGlobalQuest() && keys.size() >= 1) {
                        EditorChat.sendWarning(this.player, this.tr("quest_editor.stages_menu.global_1stage_limit"));
                        return;
                    }
                    if (hasRootStage) {
                        Object currentObj = this.questConfig.get("objective");
                        Object currentReqMats = this.questConfig.get("required-materials");
                        this.questConfig.set("objective", null);
                        this.questConfig.set("required-materials", null);
                        if (currentObj != null) {
                            this.questConfig.set("quest-path.stage-1.objective", currentObj);
                        }
                        if (currentReqMats != null) {
                            this.questConfig.set("quest-path.stage-1.required-materials", currentReqMats);
                        }
                        if (currentObj != null) {
                            this.questConfig.set("quest-path.stage-2.objective", currentObj);
                        }
                        if (currentReqMats != null) {
                            this.questConfig.set("quest-path.stage-2.required-materials", currentReqMats);
                        }
                        this.markDirty();
                        EditorChat.sendSuccess(this.player, this.tr("quest_editor.stages_menu.stage_duplicated", Map.of("key", "stage-1")));
                        this.open();
                        return;
                    }
                    this.compactStages();
                    int nextId = keys.size() + 1;
                    Object currentStageData = this.questConfig.get("quest-path." + key);
                    this.questConfig.set("quest-path.stage-" + nextId, currentStageData);
                    this.markDirty();
                    EditorChat.sendSuccess(this.player, this.tr("quest_editor.stages_menu.stage_duplicated", Map.of("key", key)));
                    this.open();
                } else if (event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) {
                    this.isNavigating = true;
                    new QuestConfirmationGui(this.plugin, this.player, this.tr("quest_editor.stages_menu.delete_confirm_title"), this.tr("quest_editor.stages_menu.delete_confirm_msg", Map.of("key", displayKey)), () -> {
                        if (hasRootStage) {
                            this.questConfig.set("objective", null);
                            this.questConfig.set("required-materials", null);
                        } else {
                            this.questConfig.set("quest-path." + key, null);
                        }
                        this.compactStages();
                        this.markDirty();
                        this.open();
                    }, () -> this.open()).open();
                }
            }));
        }
    }
}
