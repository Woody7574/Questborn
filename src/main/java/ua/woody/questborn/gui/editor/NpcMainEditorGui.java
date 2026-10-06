package ua.woody.questborn.gui.editor;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.NpcEditorListGui;
import ua.woody.questborn.gui.editor.NpcLinkedSelectionGui;
import ua.woody.questborn.gui.editor.NpcVoiceSelectorGui;
import ua.woody.questborn.gui.editor.UnsavedChangesConfirmGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.builder.gui.SimpleBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.model.Dialogue;
import ua.woody.questborn.utils.HexItemBuilder;

public class NpcMainEditorGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String npcId;
    private final File npcFile;
    private final YamlConfiguration config;
    private String pendingNpcId = null;
    private boolean isNavigating = false;
    private Gui gui;
    private long lastEscClose = 0L;

    public NpcMainEditorGui(QuestbornPlugin plugin, Player player, String npcId) {
        this.plugin = plugin;
        this.player = player;
        this.npcId = npcId;
        this.npcFile = new File(plugin.getDataFolder(), "npc/npcs.yml");
        this.config = YamlConfiguration.loadConfiguration((File)this.npcFile);
    }

    public NpcMainEditorGui(QuestbornPlugin plugin, Player player, String npcId, String pendingId, boolean hasChanges) {
        this(plugin, player, npcId, pendingId, hasChanges, null);
    }

    public NpcMainEditorGui(QuestbornPlugin plugin, Player player, String npcId, String pendingId, boolean hasChanges, YamlConfiguration config) {
        this.plugin = plugin;
        this.player = player;
        this.npcId = npcId;
        this.npcFile = new File(plugin.getDataFolder(), "npc/npcs.yml");
        this.config = config != null ? config : YamlConfiguration.loadConfiguration((File)this.npcFile);
        this.pendingNpcId = pendingId;
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
        String displayId = this.pendingNpcId != null ? this.pendingNpcId : this.npcId;
        String title = ColorFormatter.applyColors(EditorUtils.truncateGuiTitle(this.tr("npc_editor.title_main", Map.of("id", displayId))) + (EditorSessionManager.hasUnsaved(this.player.getUniqueId()) ? " *" : ""));
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
            new UnsavedChangesConfirmGui(this.player, () -> {
                if (this.saveConfig()) {
                    EditorSessionManager.clearSession(this.player.getUniqueId());
                    new NpcEditorListGui(this.plugin, this.player).open();
                } else {
                    this.refresh();
                }
            }, () -> {
                EditorSessionManager.clearSession(this.player.getUniqueId());
                new NpcEditorListGui(this.plugin, this.player).open();
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

    private ConfigurationSection getNpcSection() {
        ConfigurationSection sec;
        ConfigurationSection npcsSec = this.config.getConfigurationSection("npcs");
        if (npcsSec == null) {
            npcsSec = this.config.createSection("npcs");
        }
        if ((sec = npcsSec.getConfigurationSection(this.npcId)) == null) {
            sec = npcsSec.createSection(this.npcId);
        }
        return sec;
    }

    private boolean saveConfig() {
        ConfigurationSection npcsSec;
        if (this.pendingNpcId != null && (npcsSec = this.config.getConfigurationSection("npcs")) != null && npcsSec.contains(this.npcId) && !this.npcId.equalsIgnoreCase(this.pendingNpcId)) {
            Object oldData = npcsSec.get(this.npcId);
            npcsSec.set(this.npcId, null);
            npcsSec.set(this.pendingNpcId, oldData);
        }
        try {
            this.config.save(this.npcFile);
            this.plugin.getNpcManager().reload();
            EditorSessionManager.clearUnsaved(this.player.getUniqueId());
            return true;
        }
        catch (IOException e) {
            EditorChat.sendError(this.player, this.tr("quest_editor.npc_main.save_error", Map.of("msg", e.getMessage())));
            return false;
        }
    }

    private void setupButtons() {
        ConfigurationSection sec = this.getNpcSection();
        String displayId = this.pendingNpcId != null ? this.pendingNpcId : this.npcId;
        ItemStack idItem = HexItemBuilder.from(Material.IRON_NUGGET).name(ColorFormatter.format(this.tr("quest_editor.npc_main.id_title"))).lore(ColorFormatter.format(this.tr("quest_editor.npc_main.id_lore", Map.of("id", displayId))), "", ColorFormatter.format(this.tr("quest_editor.npc_main.id_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 2, new GuiItem(idItem, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_editor.npc_main.id_prompt")), displayId, input -> {
                this.isNavigating = false;
                if (input != null && !input.isBlank()) {
                    String sanitized = input.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
                    if (sanitized.equalsIgnoreCase(displayId)) {
                        this.refresh();
                        return;
                    }
                    ConfigurationSection npcsSec = this.config.getConfigurationSection("npcs");
                    if (npcsSec != null && npcsSec.contains(sanitized) && !sanitized.equalsIgnoreCase(this.npcId)) {
                        this.player.sendMessage(ColorFormatter.applyColors(this.tr("quest_editor.npc_main.id_exists", Map.of("id", sanitized))));
                        this.refresh();
                        return;
                    }
                    this.pendingNpcId = sanitized;
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }
                this.open();
            });
        }));
        String currentName = sec.getString("displayname", this.npcId);
        ItemStack nameItem = HexItemBuilder.from(Material.NAME_TAG).name(ColorFormatter.format(this.tr("quest_editor.npc_main.name_title"))).lore(ColorFormatter.format(this.tr("quest_editor.npc_main.name_lore")) + ColorFormatter.applyColors(currentName), "", ColorFormatter.format(this.tr("quest_editor.npc_main.name_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 3, new GuiItem(nameItem, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_editor.npc_main.name_prompt")), currentName, input -> {
                this.isNavigating = false;
                if (input != null) {
                    sec.set("displayname", input);
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }
                this.open();
            });
        }));
        String rawTitle = sec.getString("gui-title");
        String displayTitle = rawTitle != null ? ColorFormatter.applyColors("&f" + rawTitle) : ColorFormatter.format(this.tr("quest_editor.npc_main.title_not_set"));
        String suggestionTitle = rawTitle != null ? rawTitle : "<#1c1c1c>{npc} - Quests";
        ItemStack titleItem = HexItemBuilder.from(Material.OAK_SIGN).name(ColorFormatter.format(this.tr("quest_editor.npc_main.title_title"))).lore(ColorFormatter.format(this.tr("quest_editor.npc_main.title_lore")) + displayTitle, "", ColorFormatter.format(this.tr("quest_editor.npc_main.title_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 5, new GuiItem(titleItem, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_editor.npc_main.title_prompt")), suggestionTitle, input -> {
                this.isNavigating = false;
                if (input != null) {
                    sec.set("gui-title", input);
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }
                this.open();
            });
        }));
        String currentDialogue = sec.getString("dialogue-id", "none");
        ItemStack dialogueItem = HexItemBuilder.from(Material.BOOKSHELF).name(ColorFormatter.format(this.tr("quest_editor.npc_main.dialogue_title"))).lore(ColorFormatter.format(this.tr("quest_editor.npc_main.dialogue_lore", Map.of("dialogue", currentDialogue))), "", this.plugin.getLanguage().trEditorList("quest_editor.npc_main.dialogue_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(3, 7, new GuiItem(dialogueItem, event -> {
            this.isNavigating = true;
            if (event.isRightClick()) {
                this.isNavigating = true;
                this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_editor.npc_main.dialogue_prompt")), currentDialogue, input -> {
                    this.isNavigating = false;
                    if (input != null) {
                        if (input.equalsIgnoreCase("none")) {
                            sec.set("dialogue-id", null);
                        } else {
                            sec.set("dialogue-id", (Object)input.trim());
                        }
                        EditorSessionManager.markUnsaved(this.player.getUniqueId());
                    }
                    this.open();
                });
            } else {
                this.openDialogueSelector();
            }
        }));
        String currentVoice = sec.getString("voice", "villager");
        ItemStack voiceItem = HexItemBuilder.from(Material.JUKEBOX).name(ColorFormatter.format(this.tr("quest_editor.npc_main.voice_title"))).lore(ColorFormatter.format(this.tr("quest_editor.npc_main.voice_lore", Map.of("voice", currentVoice))), "", ColorFormatter.format(this.tr("quest_editor.npc_main.voice_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(4, 7, new GuiItem(voiceItem, event -> {
            this.isNavigating = true;
            new NpcVoiceSelectorGui(this.plugin, this.player, currentVoice, selected -> {
                if (selected != null) {
                    sec.set("voice", selected);
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }
                this.open();
            }, this::open).open();
        }));
        String currentHead = sec.getString("head_texture", "none");
        ItemStack headItem = HexItemBuilder.from(Material.PLAYER_HEAD)
                .name(ColorFormatter.format(this.tr("quest_editor.npc_main.head_title")))
                .lore(
                        ColorFormatter.format(this.tr("quest_editor.npc_main.head_lore_current", Map.of("current", (currentHead.length() > 20 ? currentHead.substring(0, 20) + "..." : currentHead)))),
                        "",
                        ColorFormatter.format(this.tr("quest_editor.npc_main.head_click"))
                ).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(3, 2, new GuiItem(headItem, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_editor.npc_main.head_prompt")), currentHead, input -> {
                this.isNavigating = false;
                if (input != null) {
                    if (input.equalsIgnoreCase("none")) {
                        sec.set("head_texture", null);
                    } else {
                        sec.set("head_texture", (Object)input.trim());
                    }
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }
                this.open();
            });
        }));
        String currentMood = sec.getString("default_mood", "NEUTRAL");
        ItemStack moodItem = HexItemBuilder.from(Material.SUNFLOWER)
                .name(ColorFormatter.format(this.tr("quest_editor.npc_main.mood_title")))
                .lore(
                        ColorFormatter.format(this.tr("quest_editor.npc_main.mood_lore_current", Map.of("current", currentMood))),
                        "",
                        ColorFormatter.format(this.tr("quest_editor.npc_main.mood_click"))
                ).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(3, 3, new GuiItem(moodItem, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_editor.npc_main.mood_prompt")), currentMood, input -> {
                this.isNavigating = false;
                if (input != null) {
                    sec.set("default_mood", (Object)input.trim().toUpperCase());
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }
                this.open();
            });
        }));
        double offset = sec.getDouble("indicator-offset", 0.6);
        ItemStack offsetItem = HexItemBuilder.from(Material.ARMOR_STAND).name(ColorFormatter.format(this.tr("quest_editor.npc_main.offset_title"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.npc_main.offset_lore", Map.of("offset", String.valueOf(offset))), " ", this.plugin.getLanguage().trEditorList("quest_editor.npc_main.offset_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(3, 5, new GuiItem(offsetItem, event -> EditorUtils.handleDoubleNumericClick(event, offset, -10.0, 10.0, 0.1, 2.5, 0.6, this.player, EditorChat.formatPrompt(this.tr("quest_editor.npc_main.offset_prompt")), this.plugin.getChatInputManager(), val -> {
            sec.set("indicator-offset", (Object)((double)Math.round(val * 10.0) / 10.0));
            EditorSessionManager.markUnsaved(this.player.getUniqueId());
        }, this::refresh)));
        ItemStack tpItem = HexItemBuilder.from(Material.ENDER_PEARL).name(ColorFormatter.format(this.tr("quest_editor.npc_main.tp_title"))).lore(ColorFormatter.format(this.tr("quest_editor.npc_main.tp_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(4, 2, new GuiItem(tpItem, event -> {
            if (this.plugin.getNpcIntegrationManager().getProvider() != null) {
                Location loc = this.plugin.getNpcIntegrationManager().getProvider().getStoredLocation(this.npcId);
                if (loc != null) {
                    this.player.teleport(loc);
                    this.player.sendMessage(ColorFormatter.applyColors(this.tr("npc_editor.teleported")));
                } else {
                    this.player.sendMessage(ColorFormatter.applyColors(this.tr("npc_editor.location_not_found")));
                }
            } else {
                this.player.sendMessage(ColorFormatter.applyColors(this.tr("npc_editor.no_integration")));
            }
        }));
        ItemStack linkedItem = HexItemBuilder.from(Material.WRITTEN_BOOK).name(ColorFormatter.format(this.tr("quest_editor.npc_main.linked_selection_item_title"))).lore(ColorFormatter.format(this.tr("quest_editor.npc_main.linked_selection_lore")), " ", ColorFormatter.format(this.tr("quest_editor.npc_main.linked_selection_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 7, new GuiItem(linkedItem, event -> {
            this.isNavigating = true;
            new NpcLinkedSelectionGui(this.plugin, this.player, sec, () -> {
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
            }, this.createBackAction()).open();
        }));
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            if (EditorSessionManager.hasUnsaved(this.player.getUniqueId())) {
                this.handleEscClose();
            } else {
                EditorSessionManager.clearSession(this.player.getUniqueId());
                new NpcEditorListGui(this.plugin, this.player).open();
            }
        }));
        String saveName = this.tr("quest_editor.main_menu.btn_save_title") + (EditorSessionManager.hasUnsaved(this.player.getUniqueId()) ? this.tr("quest_editor.main_menu.btn_save_changes") : "");
        String saveLore = EditorSessionManager.hasUnsaved(this.player.getUniqueId()) ? this.tr("quest_editor.main_menu.btn_save_lore_unsaved") : this.tr("quest_editor.main_menu.btn_save_lore_saved");
        ItemStack saveItem = HexItemBuilder.from(Material.EMERALD_BLOCK).name(ColorFormatter.format(saveName)).lore(ColorFormatter.format(saveLore)).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 9, new GuiItem(saveItem, event -> {
            if (!EditorSessionManager.hasUnsaved(this.player.getUniqueId())) {
                return;
            }
            if (this.saveConfig()) {
                String finalId = this.pendingNpcId != null ? this.pendingNpcId : this.npcId;
                EditorChat.sendSuccess(this.player, this.tr("npc_editor.saved_success", Map.of("id", finalId)));
                new NpcMainEditorGui(this.plugin, this.player, finalId).open();
            } else {
                this.refresh();
            }
        }));
    }

    private void openDialogueSelector() {
        Map<String, Dialogue> dialogues = this.plugin.getDialogueManager().getDialogues();
        if (dialogues.isEmpty()) {
            this.player.sendMessage(ColorFormatter.applyColors(this.tr("quest_editor.npc_main.dialogue_missing")));
            return;
        }
        PaginatedGui selGui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors(this.tr("quest_editor.npc_main.dialogue_gui_title")))))).rows(6)).pageSize(45).disableAllInteractions()).create();
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        selGui.getFiller().fillBottom(new GuiItem(bg));
        for (String dlgId : dialogues.keySet()) {
            ItemStack dlgItem = HexItemBuilder.from(Material.BOOKSHELF).name(ColorFormatter.applyColors("<#ff99d4>" + dlgId)).lore(ColorFormatter.format(this.tr("quest_editor.npc_main.dialogue_select_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            selGui.addItem(new GuiItem(dlgItem, event -> {
                this.getNpcSection().set("dialogue-id", (Object)dlgId);
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
                this.refresh();
            }));
        }
        ItemStack noneItem = HexItemBuilder.from(Material.BARRIER).name(ColorFormatter.format(this.tr("quest_editor.npc_main.dialogue_none_title"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        selGui.setItem(6, 5, new GuiItem(noneItem, event -> {
            this.getNpcSection().set("dialogue-id", null);
            EditorSessionManager.markUnsaved(this.player.getUniqueId());
            this.refresh();
        }));
        ItemStack backBtn = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        selGui.setItem(6, 1, new GuiItem(backBtn, event -> this.open()));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        selGui.setItem(6, 4, new GuiItem(prevItem, event -> selGui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        selGui.setItem(6, 6, new GuiItem(nextItem, event -> selGui.next()));
        selGui.open((HumanEntity)this.player);
    }

    private Runnable createBackAction() {
        String stateBefore = this.config.saveToString();
        String idBefore = this.pendingNpcId;
        return () -> {
            boolean changes;
            boolean bl = changes = EditorSessionManager.hasUnsaved(this.player.getUniqueId()) || !Objects.equals(idBefore, this.pendingNpcId);
            if (!stateBefore.equals(this.config.saveToString())) {
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
                new NpcMainEditorGui(this.plugin, this.player, this.npcId, this.pendingNpcId, true, this.config).open();
            } else {
                new NpcMainEditorGui(this.plugin, this.player, this.npcId, this.pendingNpcId, changes, this.config).open();
            }
        };
    }
}
