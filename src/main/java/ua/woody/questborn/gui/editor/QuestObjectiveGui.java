package ua.woody.questborn.gui.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.QuestConfirmationGui;
import ua.woody.questborn.gui.editor.QuestEntityListEditorGui;
import ua.woody.questborn.gui.editor.QuestEntityTypeSelectorGui;
import ua.woody.questborn.gui.editor.QuestListStringEditorGui;
import ua.woody.questborn.gui.editor.QuestMaterialListEditorGui;
import ua.woody.questborn.gui.editor.QuestMaterialSelectorGui;
import ua.woody.questborn.gui.editor.QuestObjectiveTypeSelectorGui;
import ua.woody.questborn.gui.editor.QuestRequiredMaterialsGui;
import ua.woody.questborn.lang.ColorFormatter;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.model.QuestObjectiveType;
import ua.woody.questborn.util.TimeFormatter;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestObjectiveGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String questId;
    private final File questFile;
    private final YamlConfiguration questConfig;
    private final String yamlPath;
    private final String titleText;
    private final Runnable backAction;
    private PaginatedGui gui;
    private Runnable onDirty;
    private boolean isNavigating = false;

    public QuestObjectiveGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig, String yamlPath, String titleText, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.questId = questId;
        this.questFile = questFile;
        this.questConfig = questConfig;
        this.yamlPath = yamlPath;
        this.titleText = titleText;
        this.backAction = backAction;
    }

    public QuestObjectiveGui setOnDirty(Runnable onDirty) {
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
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(ColorFormatter.formatComponent(EditorUtils.truncateGuiTitle("&0" + this.titleText)))).rows(6)).pageSize(21).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            Runnable closeAction;
            if (!this.isNavigating && !EditorSessionManager.isForceClosing && (closeAction = EditorSessionManager.getSession(this.player.getUniqueId())) != null) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, closeAction);
            }
        });
        this.setupBackground();
        this.populateProperties();
        this.setupNavigation();
        this.isNavigating = true;
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }

    private void setupBackground() {
        ItemStack filler = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(" ").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fillBorder(new GuiItem(filler));
        this.gui.getFiller().fillBetweenPoints(6, 1, 6, 9, new GuiItem(filler));
    }

    private void setupNavigation() {
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            this.backAction.run();
        }));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 4, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 6, new GuiItem(nextItem, event -> this.gui.next()));
    }

    private void populateProperties() {
        ConfigurationSection section = this.questConfig.getConfigurationSection(this.yamlPath);
        if (section == null) {
            if (this.yamlPath.equals("quest-path.stage-1.objective") && this.questConfig.isConfigurationSection("objective")) {
                section = this.questConfig.getConfigurationSection("objective");
            } else {
                this.questConfig.createSection(this.yamlPath);
                section = this.questConfig.getConfigurationSection(this.yamlPath);
            }
        }
        String typeStrFromConfig = section.getString("type");
        QuestObjectiveType objType = null;
        if (typeStrFromConfig != null) {
            objType = QuestObjectiveType.fromStringStrict(typeStrFromConfig);
        }
        List<String> mandatoryProps = EditorUtils.getMandatoryObjectiveProperties(objType);
        List<String> optionalProps = EditorUtils.getOptionalObjectiveProperties(objType);
        LinkedHashSet<String> allKeys = new LinkedHashSet<String>();
        allKeys.add("type");
        if (objType != null) {
            allKeys.addAll(mandatoryProps);
            allKeys.addAll(optionalProps);
        }
        allKeys.addAll(section.getKeys(false));
        for (String key : allKeys) {
            String nameFormatStr;
            String trKey;
            String displayKey;
            Material propMat;
            String typeStr;
            Object valueStr;
            boolean exists = section.contains(key);
            boolean isSection = section.isConfigurationSection(key);
            boolean isList = section.isList(key);
            if (!exists) {
                valueStr = this.tr("quest_type_editor.objective_menu.val_none");
                typeStr = this.tr("quest_type_editor.objective_menu.type_prop");
            } else if (isSection) {
                valueStr = this.tr("quest_type_editor.objective_menu.val_section") + String.join((CharSequence)", ", section.getConfigurationSection(key).getKeys(false));
                typeStr = this.tr("quest_type_editor.objective_menu.type_section");
            } else if (isList) {
                valueStr = key.equalsIgnoreCase("target-materials") || key.equalsIgnoreCase("required-materials") ? this.tr("quest_type_editor.objective_menu.val_list_mats", java.util.Map.of("size", String.valueOf(section.getList(key).size()))) : this.tr("quest_type_editor.objective_menu.val_list", java.util.Map.of("size", String.valueOf(section.getList(key).size())));
                typeStr = this.tr("quest_type_editor.objective_menu.type_list");
            } else {
                valueStr = section.getString(key);
                if (key.equalsIgnoreCase("type") && valueStr != null) {
                    valueStr = ((String)valueStr).toUpperCase(Locale.ROOT).replace('-', '_');
                }
                if (valueStr != null && ((String)valueStr).length() > 30) {
                    valueStr = ((String)valueStr).substring(0, 27) + "...";
                }
                typeStr = this.tr("quest_type_editor.objective_menu.type_value");
            }
            Material material = propMat = exists ? Material.NAME_TAG : Material.GRAY_DYE;
            if (exists) {
                if (key.equalsIgnoreCase("target-materials") || key.equalsIgnoreCase("required-materials")) {
                    propMat = Material.CHEST;
                } else if (key.equalsIgnoreCase("material") || key.equalsIgnoreCase("item") || key.equalsIgnoreCase("target-material")) {
                    propMat = Material.ITEM_FRAME;
                } else if (key.equalsIgnoreCase("type")) {
                    propMat = Material.REPEATER;
                } else if (key.equalsIgnoreCase("amount") || key.equalsIgnoreCase("notify-interval")) {
                    propMat = Material.EXPERIENCE_BOTTLE;
                } else if (key.equalsIgnoreCase("distance") || key.equalsIgnoreCase("min-distance")) {
                    propMat = Material.COMPASS;
                } else if (key.equalsIgnoreCase("entity") || key.equalsIgnoreCase("target-entities")) {
                    propMat = Material.ZOMBIE_HEAD;
                } else if (key.equalsIgnoreCase("region") || key.equalsIgnoreCase("biome") || key.equalsIgnoreCase("world")) {
                    propMat = Material.MAP;
                } else if (key.equalsIgnoreCase("command")) {
                    propMat = Material.COMMAND_BLOCK;
                } else if (key.equalsIgnoreCase("notify")) {
                    propMat = Material.BELL;
                }
            }
            boolean isBool = ((String)valueStr).equalsIgnoreCase("true") || ((String)valueStr).equalsIgnoreCase("false");
            boolean isNum = false;
            if (!(!exists || isSection || isList || isBool || Set.of("message", "message-format", "title", "subtitle", "description", "command", "permission", "world", "biome", "region", "placeholder", "value", "icon", "type", "target-material", "material", "entity", "target-entity", "npc", "npc-name", "name").contains(key.toLowerCase(Locale.ROOT)))) {
                try {
                    Double.parseDouble((String)valueStr);
                    isNum = true;
                }
                catch (Exception exception) {
                }
            }
            ArrayList<String> lore = new ArrayList<String>();
            lore.add(ColorFormatter.format(exists ? this.tr("quest_type_editor.objective_menu.prop_format", java.util.Map.of("key", typeStr, "value", String.valueOf(valueStr))) : this.tr("quest_type_editor.objective_menu.val_not_set")));
            if (exists && isNum && (key.equalsIgnoreCase("cooldown") || key.equalsIgnoreCase("notify-interval") || key.equalsIgnoreCase("duration") || key.equalsIgnoreCase("time-limit") || key.equalsIgnoreCase("time"))) {
                try {
                    long sec = (long)Double.parseDouble((String)valueStr);
                    lore.add(ColorFormatter.format(this.tr("quest_editor.stages_menu.obj_time_format", java.util.Map.of("time", TimeFormatter.format(sec)))));
                }
                catch (Exception sec) {
                }
            }
            lore.add("");
            if (!exists) {
                lore.add(ColorFormatter.format(this.tr("quest_editor.rewards_menu.click_set")));
            } else if (isSection) {
                lore.add(ColorFormatter.format(this.tr("quest_editor.stages_menu.click_open")));
            } else if (isList) {
                lore.add(ColorFormatter.format(this.tr("quest_type_editor.objective_menu.prop_str_list_click")));
            } else if (isBool) {
                lore.add(ColorFormatter.format(this.tr("quest_type_editor.objective_menu.prop_true_false")));
            } else if (key.equalsIgnoreCase("type")) {
                lore.add(ColorFormatter.format(this.tr("quest_type_editor.objective_menu.prop_type_click")));
            } else if (key.equalsIgnoreCase("material") || key.equalsIgnoreCase("icon") || key.equalsIgnoreCase("target-materials") || key.equalsIgnoreCase("target-material")) {
                lore.add(ColorFormatter.format(this.tr("quest_type_editor.objective_menu.prop_type_click")));
            } else if (key.equalsIgnoreCase("entity") || key.equalsIgnoreCase("target-entities") || key.equalsIgnoreCase("target-entity")) {
                lore.add(ColorFormatter.format(this.tr("quest_type_editor.objective_menu.prop_type_click")));
            } else if (isNum) {
                lore.addAll(this.plugin.getLanguage().trEditorList("quest_type_editor.objective_menu.prop_num_click"));
            } else {
                lore.add(ColorFormatter.format(this.tr("quest_type_editor.objective_menu.prop_click_edit")));
            }
            if (exists && !mandatoryProps.contains(key.toLowerCase(Locale.ROOT))) {
                lore.add(ColorFormatter.format(this.tr("quest_type_editor.objective_menu.prop_click_del")));
            }
            if ((displayKey = this.tr(trKey = "quest_type_editor.objective_menu.props." + key)).equals(trKey)) {
                displayKey = key;
            }
            String string = nameFormatStr = exists ? this.tr("quest_type_editor.objective_menu.prop_title_set", java.util.Map.of("key", displayKey)) : this.tr("quest_type_editor.objective_menu.prop_title_unset", java.util.Map.of("key", displayKey));
            if (!exists && mandatoryProps.contains(key.toLowerCase(Locale.ROOT))) {
                nameFormatStr = this.tr("quest_type_editor.objective_menu.prop_title_mandatory", java.util.Map.of("key", displayKey));
            }
            ItemStack basePropItem = new ItemStack(propMat);
            if (exists && (key.equalsIgnoreCase("material") || key.equalsIgnoreCase("item") || key.equalsIgnoreCase("target-material") || key.equalsIgnoreCase("icon"))) {
                ItemStack customBase = EditorUtils.getGuiItemForString(String.valueOf(valueStr), this.plugin);
                if (customBase != null && customBase.getType() != org.bukkit.Material.AIR) {
                    basePropItem = customBase;
                }
            }
            ItemStack item = HexItemBuilder.from(basePropItem).name(ColorFormatter.format(nameFormatStr)).lore(lore).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            boolean finalIsSection = isSection;
            boolean finalIsList = isList;
            String finalKey = key;
            Object finalValueStr = valueStr;
            this.gui.addItem(new GuiItem(item, arg_0 -> this.lambda$populateProperties$22(exists, mandatoryProps, finalKey, finalIsSection, finalIsList, (String)finalValueStr, arg_0)));
        }
    }

    private   void lambda$populateProperties$22(boolean exists, List mandatoryProps, String finalKey, boolean finalIsSection, boolean finalIsList, String finalValueStr, InventoryClickEvent event) {
        if (event.getClick() == ClickType.DROP && exists) {
            if (mandatoryProps.contains(finalKey.toLowerCase(Locale.ROOT))) {
                this.player.playSound(this.player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            this.isNavigating = true;
            new QuestConfirmationGui(this.plugin, this.player, this.tr("quest_type_editor.objective_menu.del_confirm_title"), this.tr("quest_type_editor.objective_menu.del_confirm_msg", java.util.Map.of("key", finalKey)), () -> {
                this.questConfig.set(this.yamlPath + "." + finalKey, null);
                this.markDirty();
                this.open();
            }, () -> this.open()).open();
        } else if (!exists || finalKey.equalsIgnoreCase("type") && event.isLeftClick()) {
            if (finalKey.equalsIgnoreCase("type")) {
                this.isNavigating = true;
                new QuestObjectiveTypeSelectorGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath, () -> this.open()).setOnDirty(this::markDirty).open();
            } else if (finalKey.equalsIgnoreCase("notify")) {
                this.questConfig.set(this.yamlPath + "." + finalKey, (Object)true);
                this.isNavigating = true;
                this.markDirty();
                this.open();
            } else if (finalKey.equalsIgnoreCase("target-materials") || finalKey.equalsIgnoreCase("required-materials")) {
                this.questConfig.set(this.yamlPath + "." + finalKey, new ArrayList());
                this.markDirty();
                if (finalKey.equalsIgnoreCase("required-materials")) {
                    this.isNavigating = true;
                    new QuestRequiredMaterialsGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath + "." + finalKey, this.tr("quest_type_editor.objective_menu.gui_req_mats"), () -> this.open()).setOnDirty(this::markDirty).open();
                } else {
                    this.isNavigating = true;
                    new QuestMaterialListEditorGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath + "." + finalKey, this.tr("quest_type_editor.objective_menu.gui_mats_list", java.util.Map.of("key", finalKey)), () -> this.open()).setOnDirty(this::markDirty).open();
                }
            } else if (finalKey.equalsIgnoreCase("target-entities")) {
                this.questConfig.set(this.yamlPath + "." + finalKey, new ArrayList());
                this.markDirty();
                String innerObjType = this.questConfig.getString(this.yamlPath + ".type");
                String typeNameStr = innerObjType != null ? innerObjType.toUpperCase(Locale.ROOT).replace('-', '_') : finalKey;
                this.isNavigating = true;
                new QuestEntityListEditorGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath + "." + finalKey, this.tr("quest_type_editor.objective_menu.gui_ent_list", java.util.Map.of("key", typeNameStr)), () -> this.open()).setOnDirty(this::markDirty).open();
            } else {
                this.isNavigating = true;
                this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, ColorFormatter.format(this.tr("quest_type_editor.objective_menu.prompt_key", java.util.Map.of("key", finalKey))), "", val -> {
                    if (val != null) {
                        this.questConfig.set(this.yamlPath + "." + finalKey, EditorUtils.parseYamlValue(val));
                        this.markDirty();
                    }
                    this.open();
                });
            }
        } else if (finalKey.equalsIgnoreCase("entity") || finalKey.equalsIgnoreCase("target-entity")) {
            this.isNavigating = true;
            new QuestEntityTypeSelectorGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath + "." + finalKey, () -> this.open()).setOnDirty(this::markDirty).open();
        } else if (finalKey.equalsIgnoreCase("material") || finalKey.equalsIgnoreCase("icon") || finalKey.equalsIgnoreCase("target-material")) {
            this.isNavigating = true;
            new QuestMaterialSelectorGui(this.plugin, this.player, (selectedMat, baseHead) -> {
                this.questConfig.set(this.yamlPath + "." + finalKey, selectedMat);
                String headKey = "base-head";
                if (finalKey.equalsIgnoreCase("icon")) {
                    headKey = "icon-base-head";
                } else if (finalKey.equalsIgnoreCase("target-material")) {
                    headKey = "target-base-head";
                }
                if (selectedMat.equals("PLAYER_HEAD") && baseHead != null) {
                    this.questConfig.set(this.yamlPath + "." + headKey, baseHead);
                } else {
                    this.questConfig.set(this.yamlPath + "." + headKey, null);
                }
                this.markDirty();
                this.open();
            }, () -> this.open()).open();
        } else if (finalIsSection) {
            this.isNavigating = true;
            new QuestObjectiveGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath + "." + finalKey, finalKey, () -> this.open()).setOnDirty(this::markDirty).open();
        } else if (finalIsList || finalKey.equalsIgnoreCase("target-materials") || finalKey.equalsIgnoreCase("target-entities")) {
            if (finalKey.equalsIgnoreCase("required-materials")) {
                this.isNavigating = true;
                new QuestRequiredMaterialsGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath + "." + finalKey, this.tr("quest_type_editor.objective_menu.gui_req_mats"), () -> this.open()).setOnDirty(this::markDirty).open();
            } else if (finalKey.equalsIgnoreCase("target-materials")) {
                String innerObjType = this.questConfig.getString(this.yamlPath + ".type");
                String typeNameStr = innerObjType != null ? innerObjType.toUpperCase(Locale.ROOT).replace('-', '_') : finalKey;
                this.isNavigating = true;
                new QuestMaterialListEditorGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath + "." + finalKey, this.tr("quest_type_editor.objective_menu.gui_mats_list", java.util.Map.of("key", typeNameStr)), () -> this.open()).setOnDirty(this::markDirty).open();
            } else if (finalKey.equalsIgnoreCase("target-entities")) {
                String innerObjType = this.questConfig.getString(this.yamlPath + ".type");
                String typeNameStr = innerObjType != null ? innerObjType.toUpperCase(Locale.ROOT).replace('-', '_') : finalKey;
                this.isNavigating = true;
                new QuestEntityListEditorGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath + "." + finalKey, this.tr("quest_type_editor.objective_menu.gui_ent_list", java.util.Map.of("key", typeNameStr)), () -> this.open()).setOnDirty(this::markDirty).open();
            } else {
                this.isNavigating = true;
                new QuestListStringEditorGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath + "." + finalKey, this.tr("quest_type_editor.objective_menu.gui_str_list", java.util.Map.of("key", finalKey)), () -> this.open()).setOnDirty(this::markDirty).open();
            }
        } else if (finalValueStr.equalsIgnoreCase("true") || finalValueStr.equalsIgnoreCase("false")) {
            this.questConfig.set(this.yamlPath + "." + finalKey, (Object)(!Boolean.parseBoolean(finalValueStr) ? 1 : 0));
            this.isNavigating = true;
            this.markDirty();
            ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, this::open);
        } else {
            boolean isNumeric = false;
            double doubleVal = 0.0;
            if (!Set.of("message", "message-format", "title", "subtitle", "description", "command", "permission", "world", "biome", "region", "placeholder", "value", "icon", "type", "target-material", "material", "entity", "target-entity", "npc", "npc-name", "name").contains(finalKey.toLowerCase(Locale.ROOT))) {
                try {
                    doubleVal = Double.parseDouble(finalValueStr);
                    isNumeric = true;
                }
                catch (Exception exception) {
                }
            }
            if (isNumeric && !finalKey.equalsIgnoreCase("type")) {
                if (event.getClick() == ClickType.SWAP_OFFHAND || event.getClick().name().contains("DROP")) {
                    this.isNavigating = true;
                    this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, ColorFormatter.format(this.tr("quest_type_editor.objective_menu.prompt_key", java.util.Map.of("key", finalKey))), finalValueStr, val -> {
                        if (val != null) {
                            try {
                                this.questConfig.set(this.yamlPath + "." + finalKey, (Object)Integer.parseInt(val));
                            }
                            catch (Exception e) {
                                try {
                                    this.questConfig.set(this.yamlPath + "." + finalKey, (Object)Double.parseDouble(val));
                                }
                                catch (Exception exception) {
                                }
                            }
                            this.markDirty();
                        }
                        this.open();
                    });
                } else if (event.isLeftClick() || event.isRightClick()) {
                    int delta;
                    int n = delta = event.isShiftClick() ? 10 : 1;
                    if (event.isRightClick()) {
                        delta = -delta;
                    }
                    double newVal = Math.max(0.0, doubleVal + (double)delta);
                    if (this.questConfig.getConfigurationSection(this.yamlPath) != null && this.questConfig.getConfigurationSection(this.yamlPath).isInt(finalKey) || doubleVal == (double)((int)doubleVal)) {
                        this.questConfig.set(this.yamlPath + "." + finalKey, (Object)((int)newVal));
                    } else {
                        this.questConfig.set(this.yamlPath + "." + finalKey, (Object)newVal);
                    }
                    this.isNavigating = true;
                    this.markDirty();
                    ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, this::open);
                }
            } else {
                this.isNavigating = true;
                this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, ColorFormatter.format(this.tr("quest_type_editor.objective_menu.prompt_key", java.util.Map.of("key", finalKey))), finalValueStr, val -> {
                    if (val != null) {
                        this.questConfig.set(this.yamlPath + "." + finalKey, EditorUtils.parseYamlValue(val));
                        this.markDirty();
                    }
                    this.open();
                });
            }
        }
    }
}
