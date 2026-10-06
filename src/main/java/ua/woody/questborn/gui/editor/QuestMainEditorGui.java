package ua.woody.questborn.gui.editor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.QuestEditorListGui;
import ua.woody.questborn.gui.editor.QuestEffectsGui;
import ua.woody.questborn.gui.editor.QuestGlobalSettingsGui;
import ua.woody.questborn.gui.editor.QuestListStringEditorGui;
import ua.woody.questborn.gui.editor.QuestMaterialSelectorGui;
import ua.woody.questborn.gui.editor.QuestPreviewGui;
import ua.woody.questborn.gui.editor.QuestRewardsGui;
import ua.woody.questborn.gui.editor.QuestRotationPoolGui;
import ua.woody.questborn.gui.editor.QuestSelectionGui;
import ua.woody.questborn.gui.editor.QuestStagesGui;
import ua.woody.questborn.gui.editor.QuestTypeSelectorGui;
import ua.woody.questborn.gui.editor.UnsavedChangesConfirmGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.SimpleBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import ua.woody.questborn.model.EngineType;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestMainEditorGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String questId;
    private final File questFile;
    private final YamlConfiguration questConfig;
    private String pendingQuestId = null;
    private String pendingQuestType = null;
    private boolean isNavigating = false;
    private Gui gui;
    private long lastEscClose = 0L;

    public QuestMainEditorGui(QuestbornPlugin plugin, Player player, String questId) {
        this.plugin = plugin;
        this.player = player;
        this.questId = questId;
        String folderName = "quests";
        QuestDefinition questDef = plugin.getQuestManager().getQuest(questId);
        File resolvedFile = null;
        YamlConfiguration resolvedConfig = null;
        if (questDef != null) {
            File f;
            QuestTypeConfig typeCfg = plugin.getQuestManager().getQuestTypeManager().getType(questDef.getTypeId());
            if (typeCfg != null) {
                folderName = typeCfg.getFolder();
            }
            if ((f = new File(plugin.getDataFolder(), folderName + "/" + questId + ".yml")).exists()) {
                resolvedFile = f;
                resolvedConfig = YamlConfiguration.loadConfiguration((File)f);
            }
        }
        if (resolvedFile == null) {
            for (QuestTypeConfig typeCfg : plugin.getQuestManager().getQuestTypeManager().getAllTypes()) {
                File f = new File(plugin.getDataFolder(), typeCfg.getFolder() + "/" + questId + ".yml");
                if (!f.exists()) continue;
                resolvedFile = f;
                resolvedConfig = YamlConfiguration.loadConfiguration((File)f);
                break;
            }
        }
        if (resolvedFile == null) {
            resolvedFile = new File(plugin.getDataFolder(), folderName + "/" + questId + ".yml");
            resolvedConfig = new YamlConfiguration();
        }
        this.questFile = resolvedFile;
        this.questConfig = resolvedConfig;
    }

    public QuestMainEditorGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig) {
        this.plugin = plugin;
        this.player = player;
        this.questId = questId;
        this.questFile = questFile;
        this.questConfig = questConfig;
    }

    public QuestMainEditorGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig, String pendingId, String pendingType, boolean hasChanges) {
        this(plugin, player, questId, questFile, questConfig);
        this.pendingQuestId = pendingId;
        this.pendingQuestType = pendingType;
        if (hasChanges) EditorSessionManager.markUnsaved(this.player.getUniqueId());
        else EditorSessionManager.clearUnsaved(this.player.getUniqueId());
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
        this.isNavigating = true;
        EditorSessionManager.setSession(this.player.getUniqueId(), this::handleEscClose);
        String displayId = this.pendingQuestId != null ? this.pendingQuestId : this.questId;
        String title = ColorFormatter.applyColors(EditorUtils.formatGuiTitle(this.tr("quest_editor.main_menu.gui_title"), displayId, EditorSessionManager.hasUnsaved(this.player.getUniqueId()) ? " *" : ""));
        this.gui = ((SimpleBuilder)((SimpleBuilder)((SimpleBuilder)Gui.gui().title(Component.text(title))).rows(6)).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            if (!this.isNavigating) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, this::handleEscClose);
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fill(new GuiItem(bg));
        this.setupButtons();
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }

    public void handleEscClose() {
        if (System.currentTimeMillis() - this.lastEscClose < 500L) {
            return;
        }
        this.lastEscClose = System.currentTimeMillis();
        if (EditorSessionManager.hasUnsaved(this.player.getUniqueId())) {
            new UnsavedChangesConfirmGui(this.player, () -> {
                if (this.saveConfig()) {
                    EditorSessionManager.clearSession(this.player.getUniqueId());
                    new QuestEditorListGui(this.plugin, this.player).open();
                } else {
                    this.refresh();
                }
            }, () -> {
                EditorSessionManager.clearSession(this.player.getUniqueId());
                new QuestEditorListGui(this.plugin, this.player).open();
            }, () -> {
                Runnable returnAction = EditorSessionManager.getReturnAction(this.player.getUniqueId());
                if (returnAction != null) {
                    returnAction.run();
                } else {
                    this.createBackAction().run();
                }
            }).open();
        } else {
            EditorSessionManager.clearSession(this.player.getUniqueId());
        }
    }

    private void refresh() {
        this.isNavigating = true;
        this.open();
    }

    private boolean saveConfig() {
        QuestTypeConfig typeCfg;
        String finalId = this.pendingQuestId != null ? this.pendingQuestId : this.questId;
        String finalType = this.pendingQuestType;
        if (finalType == null) {
            QuestDefinition questDef = this.plugin.getQuestManager().getQuest(this.questId);
            if (questDef != null) {
                finalType = questDef.getTypeId();
            } else {
                List<QuestTypeConfig> types = this.plugin.getQuestManager().getQuestTypeManager().getEnabledTypes();
                if (!types.isEmpty()) {
                    finalType = types.get(0).getId();
                }
            }
        }
        File targetFolder = this.questFile.getParentFile();
        if (finalType != null && (typeCfg = this.plugin.getQuestManager().getQuestTypeManager().getType(finalType)) != null && !(targetFolder = new File(this.plugin.getDataFolder(), typeCfg.getFolder())).exists()) {
            targetFolder.mkdirs();
        }
        File newFile = new File(targetFolder, finalId + ".yml");
        if (this.pendingQuestId != null) {
            this.questConfig.set("id", (Object)this.pendingQuestId);
        }
        try {
            this.questConfig.save(newFile);
            if (this.questFile.exists() && !this.questFile.getAbsolutePath().equalsIgnoreCase(newFile.getAbsolutePath())) {
                this.questFile.delete();
            }
            this.plugin.getQuestManager().reload();
            EditorSessionManager.clearUnsaved(this.player.getUniqueId());
            return true;
        }
        catch (IOException e) {
            this.plugin.getLogger().severe("Failed to save quest: " + e.getMessage());
            this.player.sendMessage(ColorFormatter.applyColors(this.tr("quest_editor.main_menu.save_error")));
            return false;
        }
    }

    private void playErrorSound() {
        this.player.playSound(this.player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    private void openQuestSelectionForRequireQuests(YamlConfiguration config, QuestListStringEditorGui editorGui, QuestTypeConfig typeCfg) {
        List rawList = config.getStringList("require-quests");
        HashSet<String> selectedQuests = new HashSet<String>(rawList != null ? rawList : Collections.emptyList());
        new QuestSelectionGui(this.plugin, this.player, typeCfg, selectedQuests, (selectedQuestId, isAdding) -> {
            List raw = config.getList("require-quests");
            ArrayList<String> list = raw == null ? new ArrayList<String>() : new ArrayList(raw);
            ArrayList<String> arrayList = list;
            if (isAdding.booleanValue()) {
                if (!list.contains(selectedQuestId)) {
                    list.add((String)selectedQuestId);
                }
            } else {
                list.remove(selectedQuestId);
            }
            config.set("require-quests", list);
            EditorSessionManager.markUnsaved(this.player.getUniqueId());
            this.openQuestSelectionForRequireQuests(config, editorGui, typeCfg);
        }, () -> editorGui.open()).open();
    }

    private void setupButtons() {
        String displayId = this.pendingQuestId != null ? this.pendingQuestId : this.questId;
        String currentType = this.pendingQuestType;
        if (currentType == null) {
            QuestDefinition questDef = this.plugin.getQuestManager().getQuest(this.questId);
            if (questDef != null) {
                currentType = questDef.getTypeId();
            } else {
                List<QuestTypeConfig> types = this.plugin.getQuestManager().getQuestTypeManager().getEnabledTypes();
                if (!types.isEmpty()) {
                    currentType = types.get(0).getId();
                }
            }
        }
        QuestTypeConfig typeConfig = currentType != null ? this.plugin.getQuestManager().getQuestTypeManager().getType(currentType) : null;
        EngineType engine = typeConfig != null ? typeConfig.getEngine() : EngineType.DEFAULT;
        ItemStack idItem = HexItemBuilder.from(Material.IRON_NUGGET).name(ColorFormatter.format(this.tr("quest_editor.main_menu.id_title"))).lore(ColorFormatter.format(this.tr("quest_editor.main_menu.id_lore", Map.of("id", displayId))), "", ColorFormatter.format(this.tr("quest_editor.main_menu.id_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 2, new GuiItem(idItem, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_editor.main_menu.id_prompt")), displayId, input -> {
                this.isNavigating = false;
                if (input != null && !input.isBlank()) {
                    String sanitized = input.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
                    if (sanitized.isEmpty()) {
                        this.player.sendMessage(ColorFormatter.applyColors(this.tr("quest_editor.main_menu.id_error_invalid")));
                        this.refresh();
                        return;
                    }
                    if (sanitized.equalsIgnoreCase(displayId)) {
                        this.refresh();
                        return;
                    }
                    if (!sanitized.equalsIgnoreCase(this.questId) && this.plugin.getQuestManager().getQuest(sanitized) != null) {
                        this.player.sendMessage(ColorFormatter.applyColors(this.tr("quest_editor.main_menu.id_error_exists", Map.of("id", sanitized))));
                        this.refresh();
                        return;
                    }
                    this.pendingQuestId = sanitized;
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }
                this.open();
            });
        }));
        String currentName = this.questConfig.getString("name", null);
        boolean hasName = currentName != null && !currentName.isEmpty();
        HexItemBuilder nameBuilder = HexItemBuilder.from(hasName ? Material.NAME_TAG : Material.BARRIER).name(ColorFormatter.format(hasName ? this.tr("quest_editor.main_menu.name_title_has") : this.tr("quest_editor.main_menu.name_title_none"))).lore(ColorFormatter.format(this.tr("quest_editor.main_menu.name_lore", Map.of("name", hasName ? currentName : this.tr("quest_editor.main_menu.not_specified")))), "", ColorFormatter.format(this.tr("quest_editor.main_menu.name_click"))).flags(ItemFlag.HIDE_ATTRIBUTES);
        if (!hasName) {
            nameBuilder.enchant(Enchantment.DURABILITY);
        }
        ItemStack nameItem = nameBuilder.build();
        this.gui.setItem(2, 3, new GuiItem(nameItem, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_editor.main_menu.name_prompt")), currentName, input -> {
                this.isNavigating = false;
                if (input != null) {
                    this.questConfig.set("name", input);
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }
                this.open();
            });
        }));
        String engineDisplay = engine.name();
        ItemStack typeItem = HexItemBuilder.from(Material.COMPARATOR).name(ColorFormatter.format(this.tr("quest_editor.main_menu.type_title"))).lore(ColorFormatter.format(this.tr("quest_editor.main_menu.type_lore_type", Map.of("type", currentType))), ColorFormatter.format(this.tr("quest_editor.main_menu.type_lore_engine", Map.of("engine", engineDisplay))), "", ColorFormatter.format(this.tr("quest_editor.main_menu.type_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(3, 2, new GuiItem(typeItem, event -> {
            this.isNavigating = true;
            new QuestTypeSelectorGui(this.plugin, this.player, displayId, this.questFile, this.questConfig, newType -> {
                this.pendingQuestType = newType;
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
                this.refresh();
            }, this.createBackAction()).open();
        }));
        String currentIcon = this.questConfig.getString("icon-material", null);
        if (currentIcon == null) {
            currentIcon = this.questConfig.getString("icon.material", null);
        }
        if (currentIcon == null && this.questConfig.isString("icon")) {
            currentIcon = this.questConfig.getString("icon", null);
        }
        QuestDefinition questDef = this.plugin.getQuestManager().getQuest(this.questId);
        if (currentIcon == null && questDef != null && questDef.getIconMaterial() != null) {
            currentIcon = questDef.getIconMaterial().name();
        }
        if (currentIcon == null || currentIcon.trim().isEmpty()) {
            currentIcon = "BOOK";
        }
        int currentCmd = 0;
        if (this.questConfig.contains("icon-custom-model-data")) {
            currentCmd = this.questConfig.getInt("icon-custom-model-data");
        } else if (this.questConfig.contains("icon.custom-model-data")) {
            currentCmd = this.questConfig.getInt("icon.custom-model-data");
        } else if (this.questConfig.contains("custom-model-data")) {
            currentCmd = this.questConfig.getInt("custom-model-data");
        }
        ItemStack iconBaseItem = EditorUtils.getGuiItemForString(currentIcon, this.plugin);
        if (iconBaseItem == null || iconBaseItem.getType() == Material.AIR) {
            iconBaseItem = new ItemStack(Material.ITEM_FRAME);
        }
        ItemStack iconMatItem = HexItemBuilder.from(iconBaseItem).name(ColorFormatter.format(this.tr("quest_editor.main_menu.icon_title"))).lore(ColorFormatter.format(this.tr("quest_editor.main_menu.icon_lore", Map.of("icon", currentIcon))), "", ColorFormatter.format(this.tr("quest_editor.main_menu.icon_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(3, 3, new GuiItem(iconMatItem, event -> {
            this.isNavigating = true;
            new QuestMaterialSelectorGui(this.plugin, this.player, (selectedMat, baseHead) -> {
                this.questConfig.set("icon-material", selectedMat);
                if (baseHead != null) {
                    this.questConfig.set("icon-base-head", baseHead);
                }
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
                this.refresh();
            }, () -> this.open()).open();
        }));
        ItemStack cmdItem = HexItemBuilder.from(Material.KNOWLEDGE_BOOK).name(ColorFormatter.format(this.tr("quest_editor.main_menu.cmd_title"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.main_menu.cmd_lore"), "", this.plugin.getLanguage().trEditorList("quest_editor.main_menu.slot_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        int finalCmd = currentCmd;
        this.gui.setItem(4, 3, new GuiItem(cmdItem, event -> {
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                this.isNavigating = true;
            }
            EditorUtils.handleNumericClick(event, finalCmd, 0, 9999999, this.player, this.tr("quest_editor.main_menu.cmd_prompt"), this.plugin.getChatInputManager(), val -> {
                this.questConfig.set("icon-custom-model-data", val);
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
            }, () -> this.refresh());
        }));
        String currentPerm = this.questConfig.getString("require-permission", this.questConfig.getString("required-permission", "none"));
        String displayPerm = currentPerm.equalsIgnoreCase("none") || currentPerm.isEmpty() ? this.tr("quest_editor.main_menu.perm_none") : currentPerm;
        ItemStack reqPermItem = HexItemBuilder.from(Material.TRIPWIRE_HOOK).name(ColorFormatter.format(this.tr("quest_editor.main_menu.perm_title"))).lore(ColorFormatter.format(this.tr("quest_editor.main_menu.perm_lore", Map.of("perm", displayPerm))), "", ColorFormatter.format(this.tr("quest_editor.main_menu.perm_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(4, 2, new GuiItem(reqPermItem, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_editor.main_menu.perm_prompt")), currentPerm, input -> {
                this.isNavigating = false;
                if (input != null) {
                    if (input.equalsIgnoreCase("none")) {
                        this.questConfig.set("require-permission", null);
                    } else {
                        this.questConfig.set("require-permission", input);
                    }
                    this.questConfig.set("required-permission", null);
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }
                this.open();
            });
        }));
        ArrayList<String> descLore = new ArrayList<String>();
        List<String> cfgDesc = this.questConfig.getStringList("description");
        if (cfgDesc != null && !cfgDesc.isEmpty()) {
            for (String line : cfgDesc) {
                descLore.add(ColorFormatter.format(line));
            }
        }
        descLore.add("");
        descLore.add(ColorFormatter.format(this.tr("quest_editor.main_menu.desc_click")));
        ItemStack descItem = HexItemBuilder.from(Material.BOOK).name(ColorFormatter.format(this.tr("quest_editor.main_menu.desc_title"))).lore(descLore).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 5, new GuiItem(descItem, event -> {
            this.isNavigating = true;
            new QuestListStringEditorGui(this.plugin, this.player, displayId, this.questFile, this.questConfig, "description", EditorUtils.formatGuiTitle(this.tr("quest_editor.main_menu.desc_gui_title"), displayId, ""), this.createBackAction()).setOnDirty(() -> {
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
            }).open();
        }));
        ArrayList<String> rDescLore = new ArrayList<String>();
        List<String> cfgRDesc = this.questConfig.getStringList("rewards-description");
        if (cfgRDesc != null && !cfgRDesc.isEmpty()) {
            for (String line : cfgRDesc) {
                rDescLore.add(ColorFormatter.format(line));
            }
        }
        rDescLore.add("");
        rDescLore.add(ColorFormatter.format(this.tr("quest_editor.main_menu.rew_desc_click")));
        ItemStack rDescItem = HexItemBuilder.from(Material.ENCHANTED_BOOK).name(ColorFormatter.format(this.tr("quest_editor.main_menu.rew_desc_title"))).lore(rDescLore).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 6, new GuiItem(rDescItem, event -> {
            this.isNavigating = true;
            new QuestListStringEditorGui(this.plugin, this.player, displayId, this.questFile, this.questConfig, "rewards-description", EditorUtils.formatGuiTitle(this.tr("quest_editor.main_menu.rew_desc_gui_title"), displayId, ""), this.createBackAction()).setOnDirty(() -> {
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
            }).open();
        }));
        int stageCount = EditorUtils.getStageCount(this.questConfig);
        boolean hasStages = stageCount > 0;
        HexItemBuilder stagesBuilder = HexItemBuilder.from(Material.TARGET).name(ColorFormatter.format(hasStages ? this.tr("quest_editor.main_menu.stages_title_has") : this.tr("quest_editor.main_menu.stages_title_none"))).lore(ColorFormatter.format(this.tr("quest_editor.main_menu.stages_lore", Map.of("count", String.valueOf(stageCount)))), "", ColorFormatter.format(this.tr("quest_editor.main_menu.stages_click"))).flags(ItemFlag.HIDE_ATTRIBUTES);
        ItemStack stagesItem = stagesBuilder.build();
        this.gui.setItem(3, 5, new GuiItem(stagesItem, event -> {
            this.isNavigating = true;
            new QuestStagesGui(this.plugin, this.player, displayId, this.questFile, this.questConfig, this.createBackAction()).setOnDirty(() -> {
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
            }).open();
        }));
        ItemStack rewardsItem = HexItemBuilder.from(Material.DIAMOND).name(ColorFormatter.format(this.tr("quest_editor.main_menu.rewards_title"))).lore(ColorFormatter.format(this.tr("quest_editor.main_menu.rewards_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(3, 6, new GuiItem(rewardsItem, event -> {
            this.isNavigating = true;
            new QuestRewardsGui(this.plugin, this.player, displayId, this.questFile, this.questConfig, "rewards", this.tr("quest_editor.main_menu.rewards_gui_title") + displayId, this.createBackAction()).setOnDirty(() -> {
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
            }).open();
        }));
        ItemStack effectsItem = HexItemBuilder.from(Material.POTION).name(ColorFormatter.format(this.tr("quest_editor.main_menu.effects_title"))).lore(ColorFormatter.format(this.tr("quest_editor.main_menu.effects_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(4, 5, new GuiItem(effectsItem, event -> {
            this.isNavigating = true;
            new QuestEffectsGui(this.plugin, this.player, displayId, this.questFile, this.questConfig, this.createBackAction()).setOnDirty(() -> {
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
            }).open();
        }));
        if (engine == EngineType.ROTATION) {
            String currentPool = this.questConfig.getString("rotation-pool", "none");
            boolean isNone = currentPool.equalsIgnoreCase("none") || currentPool.isEmpty();
            String lore3 = isNone ? ColorFormatter.format(this.tr("quest_editor.main_menu.rot_pool_lore3_none")) : ColorFormatter.format(this.tr("quest_editor.main_menu.rot_pool_lore3", Map.of("pool", currentPool)));
            ItemStack rotPoolItem = HexItemBuilder.from(Material.CLOCK).name(ColorFormatter.format(this.tr("quest_editor.main_menu.rot_pool_title"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.main_menu.rot_pool_lore"), lore3, "", ColorFormatter.format(this.tr("quest_editor.main_menu.rot_pool_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(2, 8, new GuiItem(rotPoolItem, event -> {
                this.isNavigating = true;
                new QuestRotationPoolGui(this.plugin, this.player, typeConfig, () -> this.open(), pool -> {
                    if (pool == null) {
                        this.questConfig.set("rotation-pool", null);
                    } else {
                        this.questConfig.set("rotation-pool", pool);
                    }
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                    this.open();
                }, () -> {
                    if (EditorSessionManager.hasUnsaved(this.player.getUniqueId())) {
                        new UnsavedChangesConfirmGui(this.player, () -> {
                            if (this.saveConfig()) {
                                new QuestEditorListGui(this.plugin, this.player).open();
                            } else {
                                this.refresh();
                            }
                        }, () -> new QuestEditorListGui(this.plugin, this.player).open(), () -> {
                            Runnable returnAction = EditorSessionManager.getReturnAction(this.player.getUniqueId());
                            if (returnAction != null) {
                                returnAction.run();
                            } else {
                                this.open();
                            }
                        }).open();
                    }
                }).open();
            }));
        } else if (engine == EngineType.CHAIN) {
            if (this.questConfig.contains("required-quests")) {
                this.questConfig.set("require-quests", this.questConfig.get("required-quests"));
                this.questConfig.set("required-quests", null);
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
            }
            ItemStack reqQuestsItem = HexItemBuilder.from(Material.WRITABLE_BOOK).name(ColorFormatter.format(this.tr("quest_editor.main_menu.req_quests_title"))).lore(ColorFormatter.format(this.tr("quest_editor.main_menu.req_quests_lore")), "", ColorFormatter.format(this.tr("quest_editor.main_menu.req_quests_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(2, 8, new GuiItem(reqQuestsItem, event -> {
                this.isNavigating = true;
                QuestListStringEditorGui editorGui = new QuestListStringEditorGui(this.plugin, this.player, displayId, this.questFile, this.questConfig, "require-quests", EditorUtils.formatGuiTitle(this.tr("quest_editor.main_menu.req_quests_gui_title"), displayId, ""), this.createBackAction());
                editorGui.setOnDirty(() -> {
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                });
                editorGui.setAddButtonName(this.tr("quest_editor.main_menu.req_quests_add"));
                editorGui.setAddButtonLore(this.tr("quest_editor.main_menu.req_quests_add_lore"));
                editorGui.setItemLoreEdit(this.tr("quest_editor.main_menu.req_quests_edit"));
                editorGui.setItemLoreDelete(this.tr("quest_editor.main_menu.req_quests_del"));
                editorGui.setShowRawString(false);
                editorGui.setItemNameProvider((qId, index) -> {
                    QuestDefinition reqQuest = this.plugin.getQuestManager().getQuest((String)qId);
                    if (reqQuest != null) {
                        return "<#ffd470>" + reqQuest.getDisplayName();
                    }
                    return this.tr("quest_editor.main_menu.req_quests_unknown", Map.of("id", qId));
                });
                editorGui.setIconProvider(qId -> {
                    QuestDefinition reqQuest = this.plugin.getQuestManager().getQuest((String)qId);
                    if (reqQuest != null) {
                        ItemStack item;
                        ItemMeta meta;
                        Material mat = reqQuest.getIconMaterial();
                        if (mat == null) {
                            mat = Material.WRITABLE_BOOK;
                        }
                        if ((meta = (item = new ItemStack(mat)).getItemMeta()) != null) {
                            if (reqQuest.getIconCustomModelData() != null) {
                                meta.setCustomModelData(reqQuest.getIconCustomModelData());
                            }
                            item.setItemMeta(meta);
                        }
                        return item;
                    }
                    return null;
                });
                QuestDefinition questDef2 = this.plugin.getQuestManager().getQuest(this.questId);
                if (questDef2 != null) {
                    QuestTypeConfig typeCfg = this.plugin.getQuestManager().getQuestTypeManager().getType(questDef2.getTypeId());
                    editorGui.setCustomAddAction(callback -> this.openQuestSelectionForRequireQuests(this.questConfig, editorGui, typeCfg));
                }
                editorGui.open();
            }));
            int pageVal = this.questConfig.getInt("page", 1);
            int slotVal = this.questConfig.getInt("slot", 0);
            ItemStack slotItem = HexItemBuilder.from(Material.COMPASS).name(ColorFormatter.format(this.tr("quest_editor.main_menu.slot_title"))).lore(ColorFormatter.format(this.tr("quest_editor.main_menu.slot_lore", Map.of("slot", String.valueOf(slotVal)))), "", this.plugin.getLanguage().trEditorList("quest_editor.main_menu.slot_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(3, 8, new GuiItem(slotItem, event -> {
                if (event.getClick() == ClickType.SWAP_OFFHAND) {
                    this.isNavigating = true;
                }
                EditorUtils.handleNumericClick(event, slotVal, 0, 54, this.player, this.tr("quest_editor.main_menu.slot_prompt"), this.plugin.getChatInputManager(), val -> {
                    this.questConfig.set("slot", val);
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }, () -> this.refresh());
            }));
            ItemStack pageItem = HexItemBuilder.from(Material.MAP).name(ColorFormatter.format(this.tr("quest_editor.main_menu.page_title"))).lore(ColorFormatter.format(this.tr("quest_editor.main_menu.page_lore", Map.of("page", String.valueOf(pageVal)))), "", this.plugin.getLanguage().trEditorList("quest_editor.main_menu.slot_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(4, 8, new GuiItem(pageItem, event -> {
                if (event.getClick() == ClickType.SWAP_OFFHAND) {
                    this.isNavigating = true;
                }
                EditorUtils.handleNumericClick(event, pageVal, 1, 999, this.player, this.tr("quest_editor.main_menu.page_prompt"), this.plugin.getChatInputManager(), val -> {
                    this.questConfig.set("page", val);
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }, () -> this.refresh());
            }));
        } else if (engine == EngineType.GLOBAL) {
            ItemStack globItem = HexItemBuilder.from(Material.BEACON).name(ColorFormatter.format(this.tr("quest_editor.main_menu.global_title"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.main_menu.global_lore"), "", ColorFormatter.format(this.tr("quest_editor.main_menu.global_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(2, 8, new GuiItem(globItem, event -> {
                this.isNavigating = true;
                new QuestGlobalSettingsGui(this.plugin, this.player, displayId, this.questFile, this.questConfig, this.createBackAction()).setOnDirty(() -> {
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }).open();
            }));
            int pageVal = this.questConfig.getInt("page", this.questConfig.getInt("global-quest.page", 1));
            int slotVal = this.questConfig.getInt("slot", this.questConfig.getInt("global-quest.slot", 0));
            ItemStack slotItem = HexItemBuilder.from(Material.COMPASS).name(ColorFormatter.format(this.tr("quest_editor.main_menu.slot_title"))).lore(ColorFormatter.format(this.tr("quest_editor.main_menu.slot_lore", Map.of("slot", String.valueOf(slotVal)))), "", this.plugin.getLanguage().trEditorList("quest_editor.main_menu.slot_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(3, 8, new GuiItem(slotItem, event -> {
                if (event.getClick() == ClickType.SWAP_OFFHAND) {
                    this.isNavigating = true;
                }
                EditorUtils.handleNumericClick(event, slotVal, 0, 54, this.player, this.tr("quest_editor.main_menu.slot_prompt"), this.plugin.getChatInputManager(), val -> {
                    this.questConfig.set("slot", val);
                    if (this.questConfig.contains("global-quest.slot")) {
                        this.questConfig.set("global-quest.slot", null);
                    }
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }, () -> this.refresh());
            }));
            ItemStack pageItem = HexItemBuilder.from(Material.MAP).name(ColorFormatter.format(this.tr("quest_editor.main_menu.page_title"))).lore(ColorFormatter.format(this.tr("quest_editor.main_menu.page_lore", Map.of("page", String.valueOf(pageVal)))), "", this.plugin.getLanguage().trEditorList("quest_editor.main_menu.slot_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(4, 8, new GuiItem(pageItem, event -> {
                if (event.getClick() == ClickType.SWAP_OFFHAND) {
                    this.isNavigating = true;
                }
                EditorUtils.handleNumericClick(event, pageVal, 1, 999, this.player, this.tr("quest_editor.main_menu.page_prompt"), this.plugin.getChatInputManager(), val -> {
                    this.questConfig.set("page", val);
                    if (this.questConfig.contains("global-quest.page")) {
                        this.questConfig.set("global-quest.page", null);
                    }
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }, () -> this.refresh());
            }));
        }
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("quest_editor.main_menu.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            if (EditorSessionManager.hasUnsaved(this.player.getUniqueId())) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> new UnsavedChangesConfirmGui(this.player, () -> {
                    if (this.saveConfig()) {
                        new QuestEditorListGui(this.plugin, this.player).open();
                    } else {
                        this.refresh();
                    }
                }, () -> new QuestEditorListGui(this.plugin, this.player).open(), () -> {
                    Runnable returnAction = EditorSessionManager.getReturnAction(this.player.getUniqueId());
                    if (returnAction != null) {
                        returnAction.run();
                    } else {
                        this.open();
                    }
                }).open());
            } else {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> new QuestEditorListGui(this.plugin, this.player).open());
            }
        }));
        ItemStack checkItem = HexItemBuilder.from(Material.ANVIL).name(ColorFormatter.format(this.tr("quest_editor.main_menu.btn_check_title"))).lore(ColorFormatter.format(this.tr("quest_editor.main_menu.btn_check_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 5, new GuiItem(checkItem, event -> this.runHealthCheck()));
        ItemStack previewItem = HexItemBuilder.from(Material.ENDER_EYE).name(ColorFormatter.format(this.tr("quest_editor.main_menu.btn_preview_title"))).lore(ColorFormatter.format(this.tr("quest_editor.main_menu.btn_preview_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 7, new GuiItem(previewItem, event -> {
            this.isNavigating = true;
            new QuestPreviewGui(this.plugin, this.player, displayId, this.questConfig, () -> this.open()).open();
        }));
        ItemStack saveItem = HexItemBuilder.from(Material.EMERALD_BLOCK).name(ColorFormatter.format(this.tr("quest_editor.main_menu.btn_save_title") + (EditorSessionManager.hasUnsaved(this.player.getUniqueId()) ? this.tr("quest_editor.main_menu.btn_save_changes") : ""))).lore(ColorFormatter.format(EditorSessionManager.hasUnsaved(this.player.getUniqueId()) ? this.tr("quest_editor.main_menu.btn_save_lore_unsaved") : this.tr("quest_editor.main_menu.btn_save_lore_saved"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 9, new GuiItem(saveItem, event -> {
            if (!EditorSessionManager.hasUnsaved(this.player.getUniqueId())) {
                return;
            }
            if (this.saveConfig()) {
                QuestTypeConfig typeCfg;
                String finalId = this.pendingQuestId != null ? this.pendingQuestId : this.questId;
                EditorChat.sendSuccess(this.player, this.tr("quest_editor.main_menu.save_success", Map.of("id", finalId)));
                String finalType = this.pendingQuestType;
                if (finalType == null) {
                    QuestDefinition qd = this.plugin.getQuestManager().getQuest(this.questId);
                    if (qd != null) {
                        finalType = qd.getTypeId();
                    } else {
                        List<QuestTypeConfig> types = this.plugin.getQuestManager().getQuestTypeManager().getEnabledTypes();
                        if (!types.isEmpty()) {
                            finalType = types.get(0).getId();
                        }
                    }
                }
                File targetFolder = this.questFile.getParentFile();
                if (finalType != null && (typeCfg = this.plugin.getQuestManager().getQuestTypeManager().getType(finalType)) != null) {
                    targetFolder = new File(this.plugin.getDataFolder(), typeCfg.getFolder());
                }
                File newFile = new File(targetFolder, finalId + ".yml");
                new QuestMainEditorGui(this.plugin, this.player, finalId, newFile, this.questConfig).open();
            } else {
                this.refresh();
            }
        }));
    }

    private Runnable createBackAction() {
        String idBefore = this.pendingQuestId;
        String typeBefore = this.pendingQuestType;
        return () -> {
            boolean changes = EditorSessionManager.hasUnsaved(this.player.getUniqueId()) || !Objects.equals(idBefore, this.pendingQuestId) || !Objects.equals(typeBefore, this.pendingQuestType);
            new QuestMainEditorGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.pendingQuestId, this.pendingQuestType, changes).open();
        };
    }

    private void runHealthCheck() {
        ConfigurationSection rewSec;
        ConfigurationSection objSec;
        int sc;
        ConfigurationSection pathSec;
        String displayId = this.pendingQuestId != null ? this.pendingQuestId : this.questId;
        ArrayList<Runnable> printTasks = new ArrayList<Runnable>();
        int errorsCount = 0;
        int warningsCount = 0;
        if (!this.questConfig.contains("name") || this.questConfig.getString("name", "").trim().isEmpty()) {
            printTasks.add(() -> EditorChat.sendError(this.player, this.tr("quest_editor.health_check.err_no_name")));
            ++errorsCount;
        }
        if ((pathSec = this.questConfig.getConfigurationSection("quest-path")) == null) {
            pathSec = this.questConfig.getConfigurationSection("stages");
        }
        if ((sc = EditorUtils.getStageCount(this.questConfig)) == 0) {
            printTasks.add(() -> EditorChat.sendError(this.player, this.tr("quest_editor.health_check.err_no_stages")));
            ++errorsCount;
        } else if (pathSec != null) {
            for (String stageKey : pathSec.getKeys(false)) {
                boolean npcExists;
                String npcId;
                ConfigurationSection objSec2 = pathSec.getConfigurationSection(stageKey + ".objective");
                if (objSec2 == null) {
                    if (pathSec.contains(stageKey + ".required-materials")) continue;
                    printTasks.add(() -> EditorChat.sendWarning(this.player, this.tr("quest_editor.health_check.warn_stage_empty", Map.of("stage", stageKey))));
                    ++warningsCount;
                    continue;
                }
                String typeStr2 = objSec2.getString("type", "");
                QuestObjectiveType objType = null;
                try {
                    objType = QuestObjectiveType.valueOf(typeStr2.toUpperCase());
                }
                catch (Exception exception) {
                }
                List<String> mandatory = EditorUtils.getMandatoryObjectiveProperties(objType);
                for (String mProp : mandatory) {
                    if (objSec2.contains(mProp) && !objSec2.getString(mProp, "").isEmpty()) continue;
                    printTasks.add(() -> EditorChat.sendWarning(this.player, this.tr("quest_editor.health_check.warn_stage_prop", Map.of("stage", stageKey, "prop", mProp))));
                    ++warningsCount;
                }
                if (!objSec2.contains("npc-id") || (npcId = objSec2.getString("npc-id", "")).isEmpty() || (npcExists = this.plugin.getNpcManager() != null && this.plugin.getNpcManager().getConfigByNpcId(npcId) != null)) continue;
                printTasks.add(() -> EditorChat.sendWarning(this.player, this.tr("quest_editor.health_check.warn_stage_npc", Map.of("stage", stageKey, "npc", npcId))));
                ++warningsCount;
            }
        } else if (this.questConfig.contains("objective") && (objSec = this.questConfig.getConfigurationSection("objective")) != null) {
            String typeStr = objSec.getString("type", "");
            QuestObjectiveType objType = null;
            try {
                objType = QuestObjectiveType.valueOf(typeStr.toUpperCase());
            }
            catch (Exception npcExists) {
            }
            List<String> mandatory = EditorUtils.getMandatoryObjectiveProperties(objType);
            for (String mProp : mandatory) {
                if (objSec.contains(mProp) && !objSec.getString(mProp, "").isEmpty()) continue;
                printTasks.add(() -> EditorChat.sendWarning(this.player, this.tr("quest_editor.health_check.warn_obj_prop", Map.of("prop", mProp))));
                ++warningsCount;
            }
        }
        boolean hasRewards = false;
        if (this.questConfig.contains("rewards") && (rewSec = this.questConfig.getConfigurationSection("rewards")) != null && !rewSec.getKeys(false).isEmpty()) {
            hasRewards = true;
        }
        if (!hasRewards) {
            printTasks.add(() -> EditorChat.sendWarning(this.player, this.tr("quest_editor.health_check.warn_no_rewards")));
            ++warningsCount;
        }
        if (errorsCount == 0 && warningsCount == 0) {
            EditorChat.sendSuccess(this.player, this.tr("quest_editor.health_check.success"));
        } else {
            this.player.sendMessage(ColorFormatter.applyColors(EditorChat.getPrefix() + this.tr("quest_editor.health_check.title", Map.of("id", displayId))));
            for (Runnable task : printTasks) {
                task.run();
            }
            this.player.sendMessage(ColorFormatter.applyColors(EditorChat.getPrefix() + this.tr("quest_editor.health_check.summary", Map.of("color1", "<#b0b8c1>", "color2", "<#c4b5b5>", "errors", String.valueOf(errorsCount), "color3", "<#c8beaa>", "warnings", String.valueOf(warningsCount)))));
        }
    }
}
