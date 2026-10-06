package ua.woody.questborn.gui.editor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.GuiUtils;
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.QuestListStringEditorGui;
import ua.woody.questborn.gui.editor.QuestMaterialSelectorGui;
import ua.woody.questborn.gui.editor.QuestTypeEditorListGui;
import ua.woody.questborn.gui.editor.QuestTypeRotationGui;
import ua.woody.questborn.gui.editor.UnsavedChangesConfirmGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.SimpleBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import ua.woody.questborn.model.EngineType;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.util.TimeFormatter;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestTypeMainEditorGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String typeId;
    private final File typeFile;
    private final YamlConfiguration typeConfig;
    private String pendingTypeId = null;
    private boolean isNavigating = false;
    private Gui gui;
    private long lastEscClose = 0L;

    public QuestTypeMainEditorGui(QuestbornPlugin plugin, Player player, String typeId, File typeFile, YamlConfiguration typeConfig) {
        this.plugin = plugin;
        this.player = player;
        this.typeId = typeId;
        this.typeFile = typeFile;
        this.typeConfig = typeConfig;
    }

    public QuestTypeMainEditorGui(QuestbornPlugin plugin, Player player, String typeId, File typeFile, YamlConfiguration typeConfig, String pendingId, boolean hasChanges) {
        this(plugin, player, typeId, typeFile, typeConfig);
        this.pendingTypeId = pendingId;
        if (hasChanges) EditorSessionManager.markUnsaved(this.player.getUniqueId());
        else EditorSessionManager.clearUnsaved(this.player.getUniqueId());
    }

public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        this.isNavigating = true;
        String displayId = this.pendingTypeId != null ? this.pendingTypeId : this.typeId;
        String title = ColorFormatter.applyColors(EditorUtils.truncateGuiTitle(this.tr("quest_type_editor.title_main", Map.of("id", displayId))) + (EditorSessionManager.hasUnsaved(this.player.getUniqueId()) ? " *" : ""));
        this.gui = ((SimpleBuilder)((SimpleBuilder)((SimpleBuilder)Gui.gui().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(6)).disableAllInteractions()).create();
        EditorSessionManager.setSession(this.player.getUniqueId(), this::handleEscClose);
        this.gui.setCloseGuiAction(event -> {
            if (!this.isNavigating && !EditorSessionManager.isForceClosing) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, this::handleEscClose);
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fill(new GuiItem(bg));
        this.setupButtons();
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
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
            new UnsavedChangesConfirmGui(this.plugin, this.player, () -> {
                if (this.saveConfig()) {
                    EditorSessionManager.clearSession(this.player.getUniqueId());
                    new QuestTypeEditorListGui(this.plugin, this.player).open();
                } else {
                    this.refresh();
                }
            }, () -> {
                EditorSessionManager.clearSession(this.player.getUniqueId());
                new QuestTypeEditorListGui(this.plugin, this.player).open();
            }, () -> {
                Runnable returnAction = EditorSessionManager.getReturnAction(this.player.getUniqueId());
                if (returnAction != null) {
                    returnAction.run();
                } else {
                    this.open();
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

    private boolean saveConfig() {
        String finalId = this.pendingTypeId != null ? this.pendingTypeId : this.typeId;
        File targetFolder = this.typeFile.getParentFile();
        File newFile = new File(targetFolder, finalId + ".yml");
        if (this.pendingTypeId != null) {
            this.typeConfig.set("id", (Object)this.pendingTypeId);
        }
        try {
            this.typeConfig.save(newFile);
            if (this.typeFile.exists() && !this.typeFile.getAbsolutePath().equalsIgnoreCase(newFile.getAbsolutePath())) {
                this.typeFile.delete();
            }
            this.plugin.getQuestManager().reload();
            EditorSessionManager.clearUnsaved(this.player.getUniqueId());
            return true;
        }
        catch (IOException e) {
            EditorChat.sendError(this.player, this.tr("quest_type_editor.error_save") + e.getMessage());
            return false;
        }
    }

    private void setupButtons() {
        String engineStr = this.typeConfig.getString("engine", "DEFAULT").toUpperCase();
        EngineType engine = EngineType.DEFAULT;
        try {
            engine = EngineType.valueOf(engineStr);
        }
        catch (Exception exception) {
        }
        String displayId = this.pendingTypeId != null ? this.pendingTypeId : this.typeId;
        ItemStack idItem = HexItemBuilder.from(Material.IRON_NUGGET).name(ColorFormatter.format(this.tr("quest_type_editor.main_menu.id_title"))).lore(ColorFormatter.format(this.tr("quest_type_editor.main_menu.id_lore", Map.of("id", displayId))), "", ColorFormatter.format(this.tr("quest_type_editor.main_menu.id_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 2, new GuiItem(idItem, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_type_editor.main_menu.id_prompt")), displayId, input -> {
                this.isNavigating = false;
                if (input != null && !input.isBlank()) {
                    String sanitized = input.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
                    if (sanitized.isEmpty()) {
                        this.refresh();
                        return;
                    }
                    if (sanitized.equalsIgnoreCase(displayId)) {
                        this.refresh();
                        return;
                    }
                    File newFile = new File(this.plugin.getDataFolder(), "types/" + sanitized + ".yml");
                    if (!sanitized.equalsIgnoreCase(this.typeId) && newFile.exists()) {
                        this.player.sendMessage(ColorFormatter.applyColors(this.tr("common_editor.symbols.error") + this.tr("quest_type_editor.id_exists")));
                        this.refresh();
                        return;
                    }
                    this.pendingTypeId = sanitized;
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }
                this.open();
            });
        }));
        String currentName = this.typeConfig.getString("display-name", this.typeConfig.getString("name", this.typeId));
        ItemStack nameItem = HexItemBuilder.from(Material.NAME_TAG).name(ColorFormatter.format(this.tr("quest_type_editor.main_menu.name_title"))).lore(ColorFormatter.format(this.tr("quest_type_editor.main_menu.name_lore", Map.of("name", currentName))), "", ColorFormatter.format(this.tr("quest_type_editor.main_menu.name_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 3, new GuiItem(nameItem, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_type_editor.prompts.name")), currentName, input -> {
                this.isNavigating = false;
                if (input != null) {
                    this.typeConfig.set("display-name", input);
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }
                this.open();
            });
        }));
        ItemStack engineItem = HexItemBuilder.from(Material.COMPARATOR).name(ColorFormatter.format(this.tr("quest_type_editor.main_menu.engine_title"))).lore(ColorFormatter.format(this.tr("quest_type_editor.main_menu.engine_lore", Map.of("engine", engine.name()))), "", ColorFormatter.format(this.tr("quest_type_editor.main_menu.engine_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        EngineType finalEngine = engine;
        this.gui.setItem(3, 2, new GuiItem(engineItem, event -> {
            EngineType nextEngine = switch (finalEngine) {
                default -> throw new IncompatibleClassChangeError();
                case DEFAULT -> EngineType.CHAIN;
                case CHAIN -> EngineType.ROTATION;
                case ROTATION -> EngineType.GLOBAL;
                case GLOBAL -> EngineType.DEFAULT;
            };
            this.typeConfig.set("engine", (Object)nextEngine.name());
            EditorSessionManager.markUnsaved(this.player.getUniqueId());
            this.refresh();
        }));
        String currentIcon = this.typeConfig.getString("material", "BOOK");
        int currentCmd = this.typeConfig.getInt("custom-model-data", 0);
        ItemStack iconBaseItem = EditorUtils.getGuiItemForString(currentIcon, this.plugin);
        if (iconBaseItem == null || iconBaseItem.getType() == Material.AIR) {
            iconBaseItem = new ItemStack(Material.BOOK);
        }
        HexItemBuilder iconBuilder = HexItemBuilder.from(iconBaseItem).name(ColorFormatter.format(this.tr("quest_type_editor.main_menu.icon_title"))).lore(ColorFormatter.format(this.tr("quest_type_editor.main_menu.icon_lore", Map.of("icon", currentIcon))), "", ColorFormatter.format(this.tr("quest_type_editor.main_menu.icon_click"))).flags(ItemFlag.HIDE_ATTRIBUTES);
        if (iconBaseItem.getType() == Material.PLAYER_HEAD) {
            String bHead = this.typeConfig.getString("base-head", null);
            if (bHead == null) {
                bHead = this.typeConfig.getString("base_head", null);
            }
            QuestTypeConfig tDef = this.plugin.getQuestManager().getQuestTypeManager().getType(this.typeId);
            if (bHead == null && tDef != null) {
                bHead = tDef.getBaseHead();
            }
            iconBuilder.lore(ColorFormatter.format(this.tr("quest_type_editor.main_menu.icon_basehead_click")));
            ItemStack iconItem = iconBuilder.build();
            if (bHead != null) {
                GuiUtils.applyBaseHead(iconItem, bHead);
            }
            this.gui.setItem(3, 3, new GuiItem(iconItem, event -> {
                if (event.getClick() == ClickType.SWAP_OFFHAND) {
                    this.isNavigating = true;
                    this.plugin.getChatInputManager().requestInput(this.player, EditorChat.formatPrompt(this.tr("quest_type_editor.material_selector.search_prompt")), val -> {
                        if (val != null) {
                            if (val.equalsIgnoreCase("none") || val.trim().isEmpty()) {
                                this.typeConfig.set("base-head", null);
                            } else {
                                this.typeConfig.set("base-head", val);
                            }
                            EditorSessionManager.markUnsaved(this.player.getUniqueId());
                        }
                        this.open();
                    });
                    return;
                }
                this.isNavigating = true;
                new QuestMaterialSelectorGui(this.plugin, this.player, (selectedMat, baseHead) -> {
                    this.typeConfig.set("material", selectedMat);
                    if (baseHead != null) {
                        this.typeConfig.set("base-head", baseHead);
                    }
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                    this.refresh();
                }, () -> {
                    Runnable returnAction = EditorSessionManager.getReturnAction(this.player.getUniqueId());
                    if (returnAction != null) {
                        returnAction.run();
                    } else {
                        this.open();
                    }
                }).open();
            }));
        } else {
            ItemStack iconItem = iconBuilder.build();
            this.gui.setItem(3, 3, new GuiItem(iconItem, event -> {
                this.isNavigating = true;
                new QuestMaterialSelectorGui(this.plugin, this.player, (selectedMat, baseHead) -> {
                    this.typeConfig.set("material", selectedMat);
                    if (baseHead != null) {
                        this.typeConfig.set("base-head", baseHead);
                    }
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                    this.refresh();
                }, () -> {
                    Runnable returnAction = EditorSessionManager.getReturnAction(this.player.getUniqueId());
                    if (returnAction != null) {
                        returnAction.run();
                    } else {
                        this.open();
                    }
                }).open();
            }));
        }
        ItemStack cmdItem = HexItemBuilder.from(Material.KNOWLEDGE_BOOK).name(ColorFormatter.format(this.tr("quest_type_editor.main_menu.cmd_title"))).lore(this.plugin.getLanguage().trEditorList("quest_type_editor.main_menu.cmd_lore", Map.of("cmd", String.valueOf(currentCmd))), "", this.plugin.getLanguage().trEditorList("quest_type_editor.main_menu.cmd_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        int finalCmd = currentCmd;
        this.gui.setItem(3, 6, new GuiItem(cmdItem, event -> {
            if (event.getClick() == ClickType.DROP) {
                this.typeConfig.set("custom-model-data", (Object)0);
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
                this.refresh();
                return;
            }
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                this.isNavigating = true;
            }
            EditorUtils.handleNumericClick(event, finalCmd, 0, 9999999, this.player, this.tr("quest_type_editor.prompts.cmd"), this.plugin.getChatInputManager(), val -> {
                this.typeConfig.set("custom-model-data", val);
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
            }, () -> this.open());
        }));
        boolean enabled = this.typeConfig.getBoolean("enabled", true);
        ItemStack enabledItem = HexItemBuilder.from(enabled ? Material.LIME_DYE : Material.GRAY_DYE).name(ColorFormatter.format(this.tr("quest_type_editor.main_menu.status_title"))).lore(ColorFormatter.format(this.tr("quest_type_editor.main_menu.status_desc")), ColorFormatter.format(this.tr("quest_type_editor.main_menu.status_lore", Map.of("status", enabled ? this.tr("quest_type_editor.main_menu.status_enabled") : this.tr("quest_type_editor.main_menu.status_disabled")))), "", ColorFormatter.format(this.tr("quest_type_editor.main_menu.status_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 8, new GuiItem(enabledItem, event -> {
            this.typeConfig.set("enabled", !enabled);
            EditorSessionManager.markUnsaved(this.player.getUniqueId());
            this.refresh();
        }));
        ArrayList<String> loreDesc = new ArrayList<String>();
        List<String> cfgLore = this.typeConfig.getStringList("lore");
        if (cfgLore != null && !cfgLore.isEmpty()) {
            for (String line : cfgLore) {
                loreDesc.add(ColorFormatter.format(line));
            }
            loreDesc.add("");
        }
        loreDesc.add(ColorFormatter.format(this.tr("quest_type_editor.main_menu.lore_click")));
        ItemStack loreItem = HexItemBuilder.from(Material.BOOK).name(ColorFormatter.format(this.tr("quest_type_editor.main_menu.lore_title"))).lore(loreDesc).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(3, 5, new GuiItem(loreItem, event -> {
            this.isNavigating = true;
            new QuestListStringEditorGui(this.plugin, this.player, displayId, this.typeFile, this.typeConfig, "lore", EditorUtils.formatGuiTitle(this.tr("quest_type_editor.preview_title"), displayId, ""), this.createBackAction()).setOnDirty(() -> {
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
            }).open();
        }));
        if (engine != EngineType.CHAIN && engine != EngineType.ROTATION) {
            long cooldown = this.typeConfig.getLong("cooldown-seconds", 0L);
            ItemStack cdItem = HexItemBuilder.from(Material.CLOCK).name(ColorFormatter.format(this.tr("quest_type_editor.main_menu.cooldown_title"))).lore(ColorFormatter.format(this.tr("quest_type_editor.main_menu.cooldown_lore", Map.of("time", cooldown <= 0L ? this.tr("quest_type_editor.main_menu.cooldown_none") : TimeFormatter.format(cooldown), "secs", String.valueOf(cooldown)))), "", this.plugin.getLanguage().trEditorList("quest_type_editor.rotation_gui.interval_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(4, 2, new GuiItem(cdItem, event -> {
                if (event.getClick() == ClickType.LEFT) {
                    this.typeConfig.set("cooldown-seconds", (Object)(cooldown + 3600L));
                } else if (event.getClick() == ClickType.RIGHT) {
                    this.typeConfig.set("cooldown-seconds", (Object)Math.max(0L, cooldown - 3600L));
                } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                    this.typeConfig.set("cooldown-seconds", (Object)(cooldown + 86400L));
                } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                    this.typeConfig.set("cooldown-seconds", (Object)Math.max(0L, cooldown - 86400L));
                } else if (event.getClick() == ClickType.DROP) {
                    this.typeConfig.set("cooldown-seconds", (Object)0L);
                } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
                    this.isNavigating = true;
                    String sugg = cooldown > 0L ? TimeFormatter.format(cooldown) : "";
                    this.isNavigating = true;
                    this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_type_editor.prompts.cooldown")), sugg, input -> {
                        this.isNavigating = false;
                        if (input != null && !input.isBlank()) {
                            long parsed = TimeFormatter.parseDuration(input);
                            this.typeConfig.set("cooldown-seconds", (Object)parsed);
                            EditorSessionManager.markUnsaved(this.player.getUniqueId());
                        }
                        this.open();
                    });
                    return;
                }
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
                this.refresh();
            }));
        }
        String folder = this.typeConfig.getString("folder", "quests/" + this.typeId);
        int slotVal = this.typeConfig.getInt("slot", 0);
        ItemStack folderItem = HexItemBuilder.from(Material.CHEST).name(ColorFormatter.format(this.tr("quest_type_editor.main_menu.folder_title"))).lore(ColorFormatter.format(this.tr("quest_type_editor.main_menu.folder_lore", Map.of("folder", folder))), "", ColorFormatter.format(this.tr("quest_type_editor.main_menu.folder_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 5, new GuiItem(folderItem, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_type_editor.prompts.folder")), folder, input -> {
                this.isNavigating = false;
                if (input != null && !input.isBlank()) {
                    this.typeConfig.set("folder", (Object)input.trim());
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }
                this.open();
            });
        }));
        ItemStack slotItem = HexItemBuilder.from(Material.COMPASS).name(ColorFormatter.format(this.tr("quest_type_editor.main_menu.slot_title"))).lore(ColorFormatter.format(this.tr("quest_type_editor.main_menu.slot_lore", Map.of("slot", String.valueOf(slotVal)))), "", this.plugin.getLanguage().trEditorList("quest_type_editor.main_menu.slot_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        int finalSlot = slotVal;
        this.gui.setItem(2, 6, new GuiItem(slotItem, event -> {
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                this.isNavigating = true;
            }
            EditorUtils.handleNumericClick(event, finalSlot, 0, 54, this.player, this.tr("quest_type_editor.prompts.slot"), this.plugin.getChatInputManager(), val -> {
                this.typeConfig.set("slot", val);
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
            }, () -> this.open());
        }));
        if (engine == EngineType.ROTATION) {
            ConfigurationSection poolSec;
            String resetRaw = this.typeConfig.getString("reset-period", this.typeConfig.getString("reset-seconds", "0"));
            long resetSec = TimeFormatter.parseDuration(resetRaw);
            String formattedInterval = resetSec == 0L ? this.tr("common_editor.state_none") : TimeFormatter.format(resetSec);
            String resetTime = this.typeConfig.getString("reset-time", "none");
            HashMap<String, Integer> poolsMap = new HashMap<String, Integer>();
            if (this.typeConfig.isConfigurationSection("rotation-pools") && (poolSec = this.typeConfig.getConfigurationSection("rotation-pools")) != null) {
                for (String key : poolSec.getKeys(false)) {
                    poolsMap.put(key, poolSec.getInt(key, 1));
                }
            }
            String poolsStr = poolsMap.isEmpty() ? this.tr("common_editor.none") : String.join((CharSequence)", ", poolsMap.keySet());
            ItemStack rotResetItem = HexItemBuilder.from(Material.REPEATER).name(ColorFormatter.format(this.tr("quest_type_editor.main_menu.rotation_title"))).lore(this.plugin.getLanguage().trEditorList("quest_type_editor.main_menu.rotation_lore", Map.of("interval", formattedInterval, "time", resetTime, "pool", poolsStr)), "", ColorFormatter.format(this.tr("quest_type_editor.main_menu.rotation_click_new"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(4, 2, new GuiItem(rotResetItem, event -> {
                this.isNavigating = true;
                Consumer<Runnable> onEsc = currentReopen -> {
                    if (EditorSessionManager.hasUnsaved(this.player.getUniqueId())) {
                        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> new UnsavedChangesConfirmGui(this.player, () -> {
                            if (this.saveConfig()) {
                                new QuestTypeEditorListGui(this.plugin, this.player).open();
                            } else {
                                currentReopen.run();
                            }
                        }, () -> new QuestTypeEditorListGui(this.plugin, this.player).open(), () -> {
                            Runnable returnAction = EditorSessionManager.getReturnAction(this.player.getUniqueId());
                            if (returnAction != null) {
                                returnAction.run();
                            } else {
                                currentReopen.run();
                            }
                        }).open());
                    }
                };
                new QuestTypeRotationGui(this.plugin, this.player, displayId, this.typeFile, this.typeConfig, this.createBackAction(), onEsc).setOnDirty(() -> {
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }).open();
            }));
        } else if (engine == EngineType.CHAIN) {
            boolean autoNext = this.typeConfig.getBoolean("auto-activate-next", false);
            HexItemBuilder autoBuilder = HexItemBuilder.from(Material.CHAIN).name(ColorFormatter.format(this.tr("quest_type_editor.main_menu.chain_title"))).lore(ColorFormatter.format(this.tr("quest_type_editor.main_menu.chain_lore", Map.of("chain", autoNext ? this.tr("quest_type_editor.main_menu.status_enabled") : this.tr("quest_type_editor.main_menu.status_disabled")))), "", ColorFormatter.format(this.tr("quest_type_editor.main_menu.chain_click"))).flags(ItemFlag.HIDE_ATTRIBUTES);
            if (autoNext) {
                autoBuilder.enchant(Enchantment.DURABILITY);
            }
            this.gui.setItem(4, 2, new GuiItem(autoBuilder.build(), event -> {
                this.typeConfig.set("auto-activate-next", !autoNext);
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
                this.refresh();
            }));
        }
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, this::handleBack);
        }));
        ItemStack checkItem = HexItemBuilder.from(Material.ANVIL).name(ColorFormatter.format(this.tr("quest_editor.main_menu.btn_check_title"))).lore(ColorFormatter.format(this.tr("quest_editor.main_menu.btn_check_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 5, new GuiItem(checkItem, event -> this.runHealthCheck()));
        String saveName = EditorSessionManager.hasUnsaved(this.player.getUniqueId()) ? this.tr("common_editor.buttons.save_unsaved") : this.tr("common_editor.buttons.save");
        String saveLore = EditorSessionManager.hasUnsaved(this.player.getUniqueId()) ? this.tr("common_editor.buttons.save_unsaved_lore") : this.tr("common_editor.buttons.save_lore");
        ItemStack saveItem = HexItemBuilder.from(Material.EMERALD_BLOCK).name(ColorFormatter.format(saveName)).lore(ColorFormatter.format(saveLore)).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 9, new GuiItem(saveItem, event -> {
            if (!EditorSessionManager.hasUnsaved(this.player.getUniqueId())) {
                return;
            }
            if (this.saveConfig()) {
                String finalId = this.pendingTypeId != null ? this.pendingTypeId : this.typeId;
                EditorChat.sendSuccess(this.player, this.tr("quest_type_editor.saved_success", Map.of("id", finalId)));
                File newFile = new File(this.typeFile.getParentFile(), finalId + ".yml");
                new QuestTypeMainEditorGui(this.plugin, this.player, finalId, newFile, this.typeConfig).open();
            } else {
                this.refresh();
            }
        }));
    }

    private void handleBack() {
        if (EditorSessionManager.hasUnsaved(this.player.getUniqueId())) {
            new UnsavedChangesConfirmGui(this.player, () -> {
                if (this.saveConfig()) {
                    new QuestTypeEditorListGui(this.plugin, this.player).open();
                } else {
                    this.refresh();
                }
            }, () -> new QuestTypeEditorListGui(this.plugin, this.player).open(), () -> {
                Runnable returnAction = EditorSessionManager.getReturnAction(this.player.getUniqueId());
                if (returnAction != null) {
                    returnAction.run();
                } else {
                    this.open();
                }
            }).open();
        } else {
            new QuestTypeEditorListGui(this.plugin, this.player).open();
        }
    }

    private Runnable createBackAction() {
        return () -> new QuestTypeMainEditorGui(this.plugin, this.player, this.typeId, this.typeFile, this.typeConfig, this.pendingTypeId, EditorSessionManager.hasUnsaved(this.player.getUniqueId())).open();
    }

    private void runHealthCheck() {
        String engineStr;
        int errors = 0;
        ArrayList<Runnable> printTasks = new ArrayList<Runnable>();
        if (!this.typeConfig.contains("display-name") || this.typeConfig.getString("display-name", "").trim().isEmpty()) {
            printTasks.add(() -> EditorChat.sendError(this.player, this.tr("quest_type_editor.health_check.err_no_name")));
            ++errors;
        }
        if ((engineStr = this.typeConfig.getString("engine", "DEFAULT").toUpperCase()).equals("ROTATION")) {
            ConfigurationSection rotationSec;
            String resetRaw = this.typeConfig.getString("reset-period", this.typeConfig.getString("reset-seconds", "0"));
            long resetSec = TimeFormatter.parseDuration(resetRaw);
            if (resetSec <= 0L) {
                printTasks.add(() -> EditorChat.sendError(this.player, this.tr("quest_type_editor.health_check.err_no_rotation_period")));
                ++errors;
            }
            if ((rotationSec = this.typeConfig.getConfigurationSection("rotation-pools")) == null || rotationSec.getKeys(false).isEmpty()) {
                printTasks.add(() -> EditorChat.sendError(this.player, this.tr("quest_type_editor.health_check.err_no_pools")));
                ++errors;
            }
        }
        this.player.closeInventory();
        int finalErrors = errors;
        ua.woody.questborn.utils.SchedulerUtils.runTaskLater(this.plugin, this.player, () -> {
            if (finalErrors == 0) {
                EditorChat.sendSuccess(this.player, this.tr("quest_type_editor.health_check.success"));
                this.player.sendMessage(ColorFormatter.applyColors(EditorChat.getPrefix() + this.tr("quest_type_editor.health_check.desc_no_errors")));
                this.player.playSound(this.player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            } else {
                EditorChat.sendError(this.player, this.tr("quest_type_editor.health_check.title_warning", Map.of("count", String.valueOf(finalErrors))));
                EditorChat.sendError(this.player, this.tr("quest_type_editor.health_check.desc_has_errors"));
                this.player.playSound(this.player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                for (Runnable task : printTasks) {
                    task.run();
                }
            }
        }, 1L);
    }
}
