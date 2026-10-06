package ua.woody.questborn.gui.editor;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.DialogueMainEditorGui;
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.MainEditorSelectorGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.model.Dialogue;
import ua.woody.questborn.utils.HexItemBuilder;

public class DialogueEditorListGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private PaginatedGui gui;

    public DialogueEditorListGui(QuestbornPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
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
        int count = this.plugin.getDialogueManager().getDialogues().size();
        String title = ColorFormatter.applyColors(this.tr("dialogue_editor.title_list", java.util.Map.of("count", String.valueOf(count))));
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(6)).pageSize(45).disableAllInteractions()).create();
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> new MainEditorSelectorGui(this.plugin, this.player).open()));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 4, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 6, new GuiItem(nextItem, event -> this.gui.next()));
        ItemStack createBtn = HexItemBuilder.from(Material.EMERALD_BLOCK).name(ColorFormatter.format(this.tr("common_editor.buttons.add_new"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 5, new GuiItem(createBtn, event -> this.plugin.getChatInputManager().requestInput(this.player, EditorChat.formatPrompt(this.tr("quest_editor.dialogue_list.create_prompt")), input -> {
            if (input != null && !input.isBlank()) {
                String sanitized = input.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
                File file = new File(this.plugin.getDataFolder(), "npc/dialogues.yml");
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration((File)file);
                if (cfg.contains("dialogues." + sanitized)) {
                    EditorChat.sendError(this.player, this.tr("dialogue_editor.id_exists"));
                    this.open();
                    return;
                }
                cfg.set("dialogues." + sanitized + ".npc-name", "{npc}");
                cfg.set("dialogues." + sanitized + ".chars-per-tick", (Object)1);
                cfg.set("dialogues." + sanitized + ".nodes.start.text", List.of(this.tr("quest_editor.dialogue_list.create_default_text")));
                cfg.set("dialogues." + sanitized + ".nodes.start.sound", (Object)"ENTITY_VILLAGER_AMBIENT");
                try {
                    cfg.save(file);
                    this.plugin.getDialogueManager().reload();
                    EditorChat.sendSuccess(this.player, this.tr("dialogue_editor.saved_success", Map.of("id", sanitized)));
                    new DialogueMainEditorGui(this.plugin, this.player, sanitized).open();
                }
                catch (Exception e) {
                    EditorChat.sendError(this.player, this.tr("quest_editor.dialogue_list.save_error"));
                    this.open();
                }
            } else {
                this.open();
            }
        })));
        this.populateDialogues();
        this.gui.open((HumanEntity)this.player);
    }

    private void populateDialogues() {
        Map<String, Dialogue> dialogues = this.plugin.getDialogueManager().getDialogues();
        for (Map.Entry<String, Dialogue> entry : dialogues.entrySet()) {
            String dialogueId = entry.getKey();
            Dialogue d = entry.getValue();
            int nodeCount = d != null && d.getNodes() != null ? d.getNodes().size() : 0;

            HexItemBuilder builder = HexItemBuilder.from(Material.BOOK)
                .name(ColorFormatter.format("<#ffd470>" + dialogueId))
                .lore(
                    ColorFormatter.format(this.tr("quest_editor.dialogue_list.nodes_lore", java.util.Map.of("count", String.valueOf(nodeCount)))),
                    "",
                    ColorFormatter.format(this.tr("quest_editor.dialogue_list.edit_click")),
                    ColorFormatter.format(this.tr("quest_editor.dialogue_list.clone_click")),
                    ColorFormatter.format(this.tr("quest_editor.dialogue_list.delete_click"))
                )
                .flags(ItemFlag.HIDE_ATTRIBUTES);
            this.gui.addItem(new GuiItem(builder.build(), event -> {
                if (event.getClick() == org.bukkit.event.inventory.ClickType.DROP) {
                    new ua.woody.questborn.gui.editor.QuestConfirmationGui(this.plugin, this.player, this.tr("dialogue_editor.delete_confirm_title"), this.tr("dialogue_editor.delete_confirm_item"), this.tr("dialogue_editor.delete_confirm_desc", java.util.Map.of("id", dialogueId)), () -> {
                        org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
                        java.io.File dialogueFile = new java.io.File(this.plugin.getDataFolder(), "npc/dialogues.yml");
                        try {
                            config.load(dialogueFile);
                            org.bukkit.configuration.ConfigurationSection dlgSec = config.getConfigurationSection("dialogues");
                            if (dlgSec != null) {
                                dlgSec.set(dialogueId, null);
                            }
                            config.save(dialogueFile);
                            this.plugin.getDialogueManager().reload();
                            ua.woody.questborn.gui.editor.EditorChat.sendSuccess(this.player, this.tr("dialogue_editor.deleted_success", java.util.Map.of("id", dialogueId)));
                            new DialogueEditorListGui(this.plugin, this.player).open();
                        } catch (Exception e) {
                            ua.woody.questborn.gui.editor.EditorChat.sendError(this.player, this.tr("dialogue_editor.save_error"));
                        }
                    }, () -> {
                        this.open();
                    }).open();
                } else if (event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND) {
                    this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, ua.woody.questborn.gui.editor.EditorChat.formatPrompt(this.tr("dialogue_editor.clone_prompt")), dialogueId + "_copy", input -> {
                        if (input != null && !input.isEmpty()) {
                            String sanitized = input.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
                            org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
                            java.io.File dialogueFile = new java.io.File(this.plugin.getDataFolder(), "npc/dialogues.yml");
                            try {
                                config.load(dialogueFile);
                                org.bukkit.configuration.ConfigurationSection dlgSec = config.getConfigurationSection("dialogues");
                                if (dlgSec != null) {
                                    if (dlgSec.contains(sanitized)) {
                                        ua.woody.questborn.gui.editor.EditorChat.sendError(this.player, this.tr("dialogue_editor.error_clone_exists"));
                                        this.open();
                                    } else {
                                        dlgSec.set(sanitized, dlgSec.getConfigurationSection(dialogueId));
                                        config.save(dialogueFile);
                                        this.plugin.getDialogueManager().reload();
                                        ua.woody.questborn.gui.editor.EditorChat.sendSuccess(this.player, this.tr("dialogue_editor.msg_clone_success", java.util.Map.of("id", sanitized)));
                                        this.open();
                                    }
                                }
                            } catch (Exception e) {
                                ua.woody.questborn.gui.editor.EditorChat.sendError(this.player, this.tr("dialogue_editor.error_clone_failed"));
                                this.open();
                            }
                        } else {
                            this.open();
                        }
                    });
                    this.player.closeInventory();
                } else {
                    new DialogueMainEditorGui(this.plugin, this.player, dialogueId).open();
                }
            }));
        }
    }
}
