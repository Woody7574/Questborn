package ua.woody.questborn.gui.editor;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.lang.ColorFormatter;
import ua.woody.questborn.utils.HexItemBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DialogueOptionEditorGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String dialogueId;
    private final String nodeKey;
    private final int optionIndex;
    private final File dialogueFile;
    private final YamlConfiguration config;
    private final Runnable onDirty;
    private final Runnable backAction;
    private boolean isNavigating = false;

    public DialogueOptionEditorGui(QuestbornPlugin plugin, Player player, String dialogueId, String nodeKey, int optionIndex, File dialogueFile, YamlConfiguration config, Runnable onDirty, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.dialogueId = dialogueId;
        this.nodeKey = nodeKey;
        this.optionIndex = optionIndex;
        this.dialogueFile = dialogueFile;
        this.config = config;
        this.onDirty = onDirty;
        this.backAction = backAction;
    }

    private Map<String, Object> getOptionMap() {
        ConfigurationSection nodesSec = config.getConfigurationSection("dialogues." + dialogueId + ".nodes");
        if (nodesSec == null) return null;
        List<Map<?, ?>> optionsList = nodesSec.getMapList(nodeKey + ".options");
        if (optionIndex < 0 || optionIndex >= optionsList.size()) return null;
        return (Map<String, Object>) optionsList.get(optionIndex);
    }

    private void updateOption(Map<String, Object> modifiedOption) {
        ConfigurationSection nodesSec = config.getConfigurationSection("dialogues." + dialogueId + ".nodes");
        if (nodesSec == null) return;
        List<Map<?, ?>> optionsList = nodesSec.getMapList(nodeKey + ".options");
        if (optionIndex >= 0 && optionIndex < optionsList.size()) {
            optionsList.set(optionIndex, modifiedOption);
            nodesSec.set(nodeKey + ".options", optionsList);
            EditorSessionManager.markUnsaved(this.player.getUniqueId());
            onDirty.run();
        }
    }

    private String tr(String path) {
        if (plugin != null && plugin.getLanguage() != null) {
            return plugin.getLanguage().trEditor(path);
        }
        return path;
    }

    private String tr(String path, java.util.Map<String, String> placeholders) {
        if (plugin != null && plugin.getLanguage() != null) {
            return plugin.getLanguage().trEditor(path, placeholders);
        }
        return path;
    }

public void open() {
        Map<String, Object> optMap = getOptionMap();
        if (optMap == null) {
            player.sendMessage(ColorFormatter.applyColors("&cError: Option not found!"));
            backAction.run();
            return;
        }

        String titleKey = tr("quest_editor.dialogue_main.opt_edit_gui_title", java.util.Map.of("num", String.valueOf(optionIndex + 1)));
        String title = ColorFormatter.applyColors(EditorUtils.truncateGuiTitle(EditorUtils.formatGuiTitle(titleKey, dialogueId, "")));
        Gui gui = Gui.gui()
                .title(Component.text(EditorUtils.truncateGuiTitle(title)))
                .rows(4)
                .disableAllInteractions()
                .create();

        EditorSessionManager.setSession(player.getUniqueId(), this::handleEscClose);
        gui.setCloseGuiAction(event -> {
            if (!isNavigating && !EditorSessionManager.isForceClosing) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(plugin, this.player, this::handleEscClose);
            }
        });

        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.getFiller().fill(new GuiItem(bg));

        String currentText = (String) optMap.get("text");
        if (currentText == null) currentText = "No Text";
        ItemStack textItem = HexItemBuilder.from(Material.NAME_TAG)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_reply_text_title")))
                .lore(
                        ColorFormatter.format(tr("quest_editor.dialogue_main.opt_edit_current", java.util.Map.of("current", EditorUtils.truncateGuiTitle(currentText.replaceAll("<[^>]*>", "").replaceAll("&[0-9a-fk-orA-FK-OR]", ""))))),
                        "",
                        ColorFormatter.format(tr("quest_editor.dialogue_main.opt_reply_text_click"))
                ).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(2, 2, new GuiItem(textItem, event -> {
            isNavigating = true;
            String fText = optMap.containsKey("text") ? (String) optMap.get("text") : "New Option";
            plugin.getChatInputManager().requestInputWithSuggestion(player, EditorChat.formatPrompt(tr("quest_editor.dialogue_main.opt_edit_prompt_text")), fText, input -> {
                isNavigating = false;
                if (input != null) {
                    Map<String, Object> mOpt = getOptionMap();
                    mOpt.put("text", input);
                    updateOption(mOpt);
                }
                open();
            });
        }));

        ItemStack loreItem = HexItemBuilder.from(Material.WRITABLE_BOOK)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_reply_lore_title")))
                .lore(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_reply_lore_click")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(2, 4, new GuiItem(loreItem, event -> {
            isNavigating = true;
            String tmpPath = "dialogues." + dialogueId + ".nodes." + nodeKey + ".options." + optionIndex + ".lore";
            new QuestListStringEditorGui(plugin, player, dialogueId, dialogueFile, config, tmpPath, tr("quest_editor.dialogue_main.opt_edit_lore_title", java.util.Map.of("num", String.valueOf(optionIndex + 1))), () -> {
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
                onDirty.run();
                open();
            }).setOnDirty(onDirty).open();
        }));

        String currentMaterial = optMap.containsKey("material") ? (String) optMap.get("material") : "PAPER";
        ItemStack matItem = HexItemBuilder.from(Material.ITEM_FRAME)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_material_title")))
                .lore(
                        ColorFormatter.format(tr("quest_editor.dialogue_main.opt_edit_current", java.util.Map.of("current", currentMaterial))),
                        "",
                        ColorFormatter.format(tr("quest_editor.dialogue_main.opt_material_click"))
                ).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(2, 6, new GuiItem(matItem, event -> {
            isNavigating = true;
            plugin.getChatInputManager().requestInputWithSuggestion(player, EditorChat.formatPrompt(tr("quest_editor.dialogue_main.opt_edit_prompt_mat")), currentMaterial, input -> {
                isNavigating = false;
                    if (input != null) {
                        try {
                            String trimmed = input.trim();
                            boolean valid = false;
                            if (trimmed.toLowerCase().startsWith("craftengine:") || trimmed.toLowerCase().startsWith("ce:")) {
                                valid = true;
                            } else if (trimmed.toLowerCase().startsWith("itemsadder:") || trimmed.toLowerCase().startsWith("ia:")) {
                                valid = true;
                            } else {
                                Material newMat = Material.valueOf(trimmed.toUpperCase());
                                trimmed = newMat.name();
                                valid = true;
                            }
                            if (valid) {
                                Map<String, Object> mOpt = getOptionMap();
                                mOpt.put("material", trimmed);
                                updateOption(mOpt);
                            }
                        } catch (Exception e) {
                            player.sendMessage(ColorFormatter.applyColors(tr("quest_editor.dialogue_main.opt_edit_prompt_mat_invalid")));
                        }
                    }
                open();
            });
        }));

        int currentSlot = optMap.containsKey("slot") ? ((Number) optMap.get("slot")).intValue() : -1;
        ItemStack slotItem = HexItemBuilder.from(Material.CHEST)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_slot_title")))
                .lore(
                        ColorFormatter.format(tr("quest_editor.dialogue_main.opt_edit_current", java.util.Map.of("current", String.valueOf(currentSlot)))),
                        "",
                        this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.opt_slot_click")
                ).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(2, 8, new GuiItem(slotItem, event -> {
            if (event.getClick() == org.bukkit.event.inventory.ClickType.DROP) {
                isNavigating = true;
                plugin.getChatInputManager().requestInput(player, EditorChat.formatPrompt(tr("quest_editor.dialogue_main.opt_edit_prompt_slot")), input -> {
                    isNavigating = false;
                    if (input != null) {
                        try {
                            int val = Integer.parseInt(input);
                            Map<String, Object> mOpt = getOptionMap();
                            mOpt.put("slot", val);
                            updateOption(mOpt);
                        } catch (NumberFormatException ignored) {}
                    }
                    open();
                });
            } else {
                int newVal = currentSlot;
                if (event.getClick() == org.bukkit.event.inventory.ClickType.LEFT) newVal += 1;
                else if (event.getClick() == org.bukkit.event.inventory.ClickType.RIGHT) newVal -= 1;

                Map<String, Object> mOpt = getOptionMap();
                mOpt.put("slot", newVal);
                updateOption(mOpt);
                open();
            }
        }));

        String currentSound = optMap.containsKey("sound") ? (String) optMap.get("sound") : "none";
        ItemStack soundItem = HexItemBuilder.from(Material.NOTE_BLOCK)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_sound_title")))
                .lore(
                        ColorFormatter.format(tr("quest_editor.dialogue_main.opt_edit_current", java.util.Map.of("current", currentSound))),
                        "",
                        ColorFormatter.format(tr("quest_editor.dialogue_main.opt_sound_click"))
                ).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(3, 2, new GuiItem(soundItem, event -> {
            isNavigating = true;
            plugin.getChatInputManager().requestInputWithSuggestion(player, EditorChat.formatPrompt(tr("quest_editor.dialogue_main.opt_edit_prompt_sound")), currentSound, input -> {
                isNavigating = false;
                if (input != null) {
                    Map<String, Object> mOpt = getOptionMap();
                    if(input.equalsIgnoreCase("none")) mOpt.remove("sound");
                    else mOpt.put("sound", input.toUpperCase().trim());
                    updateOption(mOpt);
                }
                open();
            });
        }));

        ItemStack actionItem = HexItemBuilder.from(Material.REPEATER)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_action_title")))
                .lore(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_action_click")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(3, 4, new GuiItem(actionItem, event -> {
            isNavigating = true;
            new DialogueOptionActionGui(plugin, player, dialogueId, nodeKey, optionIndex, dialogueFile, config, onDirty, this::open).open();
        }));

        ItemStack condItem = HexItemBuilder.from(Material.COMPARATOR)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_conds_title")))
                .lore(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_conds_click")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(3, 6, new GuiItem(condItem, event -> {
            isNavigating = true;
            new DialogueOptionConditionGui(plugin, player, dialogueId, nodeKey, optionIndex, dialogueFile, config, onDirty, this::open).open();
        }));

        ItemStack createNodeBtn = HexItemBuilder.from(Material.CHAIN)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_create_node_title")))
                .lore(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_create_node_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(3, 8, new GuiItem(createNodeBtn, event -> {
            ConfigurationSection nodesSec = config.getConfigurationSection("dialogues." + dialogueId + ".nodes");
            if (nodesSec == null) return;

            int idIndex = 1;
            while (nodesSec.contains("node_" + idIndex)) {
                idIndex++;
            }
            String sanitized = "node_" + idIndex;

            nodesSec.set(sanitized + ".text", List.of(tr("quest_editor.dialogue_main.nodes_add_default_text")));
            nodesSec.set(sanitized + ".sound", "ENTITY_VILLAGER_AMBIENT");

            Map<String, Object> mOpt = getOptionMap();
            mOpt.put("action", "dialogue:" + sanitized);
            updateOption(mOpt);

            isNavigating = true;
            new DialogueNodeEditorGui(plugin, player, dialogueId, sanitized, dialogueFile, config, onDirty, this::open).open();
        }));

        ItemStack backBtn = HexItemBuilder.from(Material.OAK_DOOR)
                .name(ColorFormatter.format(tr("common_editor.buttons.back"))).lore(ColorFormatter.format(tr("common_editor.buttons.back_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(4, 1, new GuiItem(backBtn, event -> {
            isNavigating = true;
            backAction.run();
        }));

        gui.open(player);
        this.isNavigating = false;
    }

    private void handleEscClose() {
        if (EditorSessionManager.hasUnsaved(this.player.getUniqueId())) {
            new UnsavedChangesConfirmGui(this.player, () -> {
                try {
                    this.config.save(this.dialogueFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                EditorSessionManager.clearSession(this.player.getUniqueId());
                this.player.closeInventory();
            }, () -> {
                EditorSessionManager.clearSession(this.player.getUniqueId());
                this.player.closeInventory();
            }, () -> {
                this.isNavigating = true;
                this.open();
            }).open();
        } else {
            EditorSessionManager.clearSession(player.getUniqueId());
        }
    }
}
