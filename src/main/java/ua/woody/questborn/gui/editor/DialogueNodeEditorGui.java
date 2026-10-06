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

public class DialogueNodeEditorGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String dialogueId;
    private final String nodeKey;
    private final File dialogueFile;
    private final YamlConfiguration config;
    private final Runnable onDirty;
    private final Runnable backAction;
    private boolean isNavigating = false;

    public DialogueNodeEditorGui(QuestbornPlugin plugin, Player player, String dialogueId, String nodeKey, File dialogueFile, YamlConfiguration config, Runnable onDirty, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.dialogueId = dialogueId;
        this.nodeKey = nodeKey;
        this.dialogueFile = dialogueFile;
        this.config = config;
        this.onDirty = () -> {
            EditorSessionManager.markUnsaved(this.player.getUniqueId());
            if (onDirty != null) onDirty.run();
        };
        this.backAction = backAction;
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
        String titleKey = this.tr("quest_editor.dialogue_main.node_gui_title");
        String title = ColorFormatter.applyColors(EditorUtils.truncateGuiTitle(titleKey + nodeKey));
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

        ConfigurationSection dlgSec = config.getConfigurationSection("dialogues." + dialogueId);
        ConfigurationSection nodesSec = dlgSec != null ? dlgSec.getConfigurationSection("nodes") : null;
        if (nodesSec == null || !nodesSec.contains(nodeKey)) {
            player.sendMessage(ColorFormatter.applyColors("&cError: Node not found!"));
            backAction.run();
            return;
        }

        ItemStack idItem = HexItemBuilder.from(Material.NAME_TAG)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.node_id_title")))
                .lore(
                        ColorFormatter.format(tr("quest_editor.dialogue_main.id_lore", java.util.Map.of("id", nodeKey))),
                        "",
                        ColorFormatter.format(tr("quest_editor.dialogue_main.id_click"))
                ).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(2, 2, new GuiItem(idItem, event -> {
            isNavigating = true;
            plugin.getChatInputManager().requestInputWithSuggestion(player, EditorChat.formatPrompt(tr("quest_editor.dialogue_main.node_id_prompt")), nodeKey, input -> {
                isNavigating = false;
                if (input != null && !input.isBlank()) {
                    String sanitized = input.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
                    if (!sanitized.equals(nodeKey)) {
                        if (nodesSec.contains(sanitized)) {
                            player.sendMessage(ColorFormatter.applyColors("&cNode with this ID already exists!"));
                            open();
                            return;
                        }

                        Object data = nodesSec.get(nodeKey);
                        nodesSec.set(nodeKey, null);
                        nodesSec.set(sanitized, data);

                        for (String k : nodesSec.getKeys(false)) {
                            List<Map<?, ?>> opts = nodesSec.getMapList(k + ".options");
                            if (opts != null && !opts.isEmpty()) {
                                boolean changed = false;
                                List<Map<String, Object>> newOpts = new ArrayList<>();
                                for (Map<?, ?> opt : opts) {
                                    Map<String, Object> newOpt = new java.util.LinkedHashMap<>((Map<String, Object>) opt);
                                    String act = (String) newOpt.get("action");
                                    if (act == null) act = (String) newOpt.get("goto");
                                    if (("dialogue:" + nodeKey).equals(act)) {
                                        newOpt.put("action", "dialogue:" + sanitized);
                                        newOpt.remove("goto");
                                        changed = true;
                                    }
                                    newOpts.add(newOpt);
                                }
                                if (changed) {
                                    nodesSec.set(k + ".options", newOpts);
                                }
                            }
                        }

                        onDirty.run();
                        new DialogueNodeEditorGui(plugin, player, dialogueId, sanitized, dialogueFile, config, onDirty, backAction).open();
                        return;
                    }
                }
                open();
            });
        }));

        List<String> textLines = nodesSec.getStringList(nodeKey + ".text");
        List<String> textLore = new ArrayList<>();
        textLore.addAll(this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.node_text_lore"));
        int linesShown = 0;
        for (String rawLine : textLines) {
            if (linesShown >= 5) {
                textLore.add(ColorFormatter.format(tr("quest_editor.dialogue_main.node_text_more", java.util.Map.of("count", String.valueOf(textLines.size() - linesShown)))));
                break;
            }
            List<String> wrappedParts = this.wrapPreviewLine(rawLine, 45);
            boolean isFirst = true;
            for (String part : wrappedParts) {
                String displayPart = part.replaceAll("(?i)(</?(?:whisper|shout|normal)>)", "<#ffffff>$1<#ffffff>");
                if (isFirst) {
                    textLore.add(ColorFormatter.format("  <#6e6e6e>- <#ffffff>" + displayPart));
                    isFirst = false;
                    continue;
                }
                textLore.add(ColorFormatter.format("    <#ffffff>" + displayPart));
            }
            ++linesShown;
        }
        textLore.add("");
        textLore.add(ColorFormatter.format(tr("quest_editor.dialogue_main.node_text_click")));

        ItemStack textItem = HexItemBuilder.from(Material.WRITABLE_BOOK)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.node_text_title")))
                .lore(textLore).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(2, 4, new GuiItem(textItem, event -> {
            isNavigating = true;
            new QuestListStringEditorGui(plugin, player, dialogueId + "_" + nodeKey, dialogueFile, config, "dialogues." + dialogueId + ".nodes." + nodeKey + ".text", tr("quest_editor.dialogue_main.nodes_edit_gui_title") + nodeKey, () -> {
                this.onDirty.run();
                this.open();
            }).setOnDirty(onDirty).open();
        }));

        int optionsCount = 0;
        if (nodesSec.contains(nodeKey + ".options")) {
            optionsCount = nodesSec.getMapList(nodeKey + ".options").size();
        }
        ItemStack optionsItem = HexItemBuilder.from(Material.OAK_SIGN)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.node_opts_title")))
                .lore(
                        this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.node_opts_lore", java.util.Map.of("count", String.valueOf(optionsCount))),
                        "",
                        ColorFormatter.format(tr("quest_editor.dialogue_main.node_opts_click"))
                ).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(2, 6, new GuiItem(optionsItem, event -> {
            isNavigating = true;
            new DialogueOptionListGui(plugin, player, dialogueId, nodeKey, dialogueFile, config, onDirty, this::open).open();
        }));

        String currentSound = nodesSec.getString(nodeKey + ".sound", "ENTITY_VILLAGER_AMBIENT");
        ItemStack soundItem = HexItemBuilder.from(Material.NOTE_BLOCK)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.node_sound_title")))
                .lore(
                        this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.node_sound_lore", java.util.Map.of("sound", currentSound)),
                        "",
                        ColorFormatter.format(tr("quest_editor.dialogue_main.node_sound_click"))
                ).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(2, 8, new GuiItem(soundItem, event -> {
            isNavigating = true;
            plugin.getChatInputManager().requestInputWithSuggestion(player, EditorChat.formatPrompt(tr("quest_editor.dialogue_main.opt_edit_prompt_sound")), currentSound, input -> {
                isNavigating = false;
                if (input != null) {
                    if(input.equalsIgnoreCase("none")) nodesSec.set(nodeKey + ".sound", null);
                    else nodesSec.set(nodeKey + ".sound", input.toUpperCase().trim());
                    onDirty.run();
                }
                open();
            });
        }));

        ItemStack quickReplyBtn = HexItemBuilder.from(Material.CHAIN)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.node_quick_reply_title")))
                .lore(ColorFormatter.format(tr("quest_editor.dialogue_main.node_quick_reply_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(3, 5, new GuiItem(quickReplyBtn, event -> {
            int idIndex = 1;
            while (nodesSec.contains("node_" + idIndex)) {
                idIndex++;
            }
            String sanitized = "node_" + idIndex;

            nodesSec.set(sanitized + ".text", List.of(tr("quest_editor.dialogue_main.nodes_add_default_text")));
            nodesSec.set(sanitized + ".sound", "ENTITY_VILLAGER_AMBIENT");

            List<Map<?, ?>> optionsList = nodesSec.getMapList(nodeKey + ".options");
            if (optionsList == null) optionsList = new ArrayList<>();
            Map<String, Object> newOpt = new java.util.LinkedHashMap<>();
            newOpt.put("text", tr("quest_editor.dialogue_main.node_quick_reply_text", java.util.Map.of("default", "Next...")));
            newOpt.put("action", "dialogue:" + sanitized);
            newOpt.put("material", "PAPER");
            newOpt.put("slot", optionsList.size());

            List<Map<String, Object>> writableList = new ArrayList<>();
            for (Map<?, ?> map : optionsList) {
                writableList.add((Map<String, Object>) map);
            }
            writableList.add(newOpt);

            nodesSec.set(nodeKey + ".options", writableList);
            onDirty.run();

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
            }, this::open).open();
        } else {
            EditorSessionManager.clearSession(this.player.getUniqueId());
        }
    }

    private List<String> wrapPreviewLine(String text, int maxLineLen) {
        List<String> wrapped = new ArrayList<>();
        if (text == null || text.isBlank()) return wrapped;

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        StringBuilder lastTags = new StringBuilder();
        int currentLen = 0;

        for (String word : words) {
            String cleanWord = word.replaceAll("(?i)</?[a-z0-9_#]+>", "");

            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)</?[a-z0-9_#]+>").matcher(word);
            while (m.find()) {
                lastTags.append(m.group());
            }

            if (currentLen + cleanWord.length() > maxLineLen && currentLen > 0) {
                wrapped.add(currentLine.toString().trim());
                currentLine.setLength(0);
                currentLine.append(lastTags.toString());
                currentLen = 0;
            }

            currentLine.append(word).append(" ");
            currentLen += cleanWord.length() + 1;
        }

        if (currentLine.length() > 0) {
            String trimmed = currentLine.toString().trim();
            if (!trimmed.isEmpty() && !trimmed.equals(lastTags.toString().trim())) {
                wrapped.add(trimmed);
            }
        }

        return wrapped;
    }
}
