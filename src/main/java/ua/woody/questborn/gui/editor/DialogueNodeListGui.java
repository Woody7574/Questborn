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

public class DialogueNodeListGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String dialogueId;
    private final File dialogueFile;
    private final YamlConfiguration config;
    private final Runnable onDirty;
    private final Runnable backAction;
    private boolean isNavigating = false;

    public DialogueNodeListGui(QuestbornPlugin plugin, Player player, String dialogueId, File dialogueFile, YamlConfiguration config, Runnable onDirty, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.dialogueId = dialogueId;
        this.dialogueFile = dialogueFile;
        this.config = config;
        this.onDirty = onDirty;
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
        ConfigurationSection dlgSec = config.getConfigurationSection("dialogues." + dialogueId);
        ConfigurationSection nodesSec = dlgSec != null ? dlgSec.getConfigurationSection("nodes") : null;

        dev.triumphteam.gui.guis.PaginatedGui nodesGui = dev.triumphteam.gui.guis.Gui.paginated()
                .title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors(EditorUtils.formatGuiTitle(tr("quest_editor.dialogue_main.nodes_gui_title"), dialogueId, "")))))
                .rows(6)
                .pageSize(45)
                .disableAllInteractions()
                .create();

        EditorSessionManager.setSession(player.getUniqueId(), this::handleEscClose);
        nodesGui.setCloseGuiAction(event -> {
            if (!isNavigating && !EditorSessionManager.isForceClosing) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(plugin, this.player, this::handleEscClose);
            }
        });

        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        nodesGui.getFiller().fillBottom(new GuiItem(bg));

        if (nodesSec != null) {
            for (String nodeKey : nodesSec.getKeys(false)) {
                List<String> textLines = nodesSec.getStringList(nodeKey + ".text");
                List<String> lore = new ArrayList<>();
                lore.add(ColorFormatter.format(tr("quest_editor.dialogue_main.nodes_lines", java.util.Map.of("count", String.valueOf(textLines.size())))));

                int optionsCount = 0;
                if (nodesSec.contains(nodeKey + ".options")) {
                    optionsCount = nodesSec.getMapList(nodeKey + ".options").size();
                }
                lore.add(ColorFormatter.format("&7Options: &e" + optionsCount));

                if (!textLines.isEmpty()) {
                    lore.add(ColorFormatter.format(tr("quest_editor.dialogue_main.nodes_text")));
                    int linesShown = 0;
                    for (String rawLine : textLines) {
                        if (linesShown >= 4) {
                            lore.add(ColorFormatter.format(tr("quest_editor.dialogue_main.nodes_text_more", java.util.Map.of("count", String.valueOf(textLines.size() - linesShown)))));
                            break;
                        }
                        List<String> wrappedParts = wrapPreviewLine(rawLine, 45);
                        boolean isFirst = true;
                        for (String part : wrappedParts) {
                            if (isFirst) {
                                lore.add(ColorFormatter.format("  <#6e6e6e>--- <#ffffff>" + part));
                                isFirst = false;
                            } else {
                                lore.add(ColorFormatter.format("    <#ffffff>" + part));
                            }
                        }
                        linesShown++;
                    }
                }
                lore.add("");
                lore.addAll(this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.nodes_item_click"));

                Material iconMat = nodeKey.equalsIgnoreCase("start") ? Material.BOOKSHELF : Material.PAPER;
                String displayName = nodeKey.equalsIgnoreCase("start") ? tr("quest_editor.dialogue_main.nodes_item_start") : tr("quest_editor.dialogue_main.nodes_item_normal", java.util.Map.of("node", nodeKey));

                ItemStack nodeItem = HexItemBuilder.from(iconMat)
                        .name(ColorFormatter.applyColors(displayName))
                        .lore(lore)
                        .flags(ItemFlag.HIDE_ATTRIBUTES).build();

                nodesGui.addItem(new GuiItem(nodeItem, event -> {
                    isNavigating = true;
                    if (event.getClick() == org.bukkit.event.inventory.ClickType.DROP) {
                        new QuestConfirmationGui(plugin, player, tr("quest_editor.dialogue_main.nodes_del_confirm_title"), tr("quest_editor.dialogue_main.nodes_del_confirm_desc", java.util.Map.of("node", nodeKey)), () -> {
                            nodesSec.set(nodeKey, null);
                            EditorSessionManager.markUnsaved(this.player.getUniqueId());
                            onDirty.run();
                            player.sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors(tr("quest_editor.dialogue_main.nodes_del_success", java.util.Map.of("node", nodeKey))));
                            open();
                        }, this::open).open();
                    } else {
                        new DialogueNodeEditorGui(plugin, player, dialogueId, nodeKey, dialogueFile, config, onDirty, this::open).open();
                    }
                }));
            }
        }

        ItemStack backBtn = HexItemBuilder.from(Material.OAK_DOOR)
                .name(ColorFormatter.format(tr("common_editor.buttons.back"))).lore(ColorFormatter.format(tr("common_editor.buttons.back_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        nodesGui.setItem(6, 1, new GuiItem(backBtn, event -> {
            isNavigating = true;
            backAction.run();
        }));

        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        nodesGui.setItem(6, 4, new GuiItem(prevItem, event -> ((dev.triumphteam.gui.guis.PaginatedGui)nodesGui).previous()));

        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        nodesGui.setItem(6, 6, new GuiItem(nextItem, event -> ((dev.triumphteam.gui.guis.PaginatedGui)nodesGui).next()));

        ItemStack addNodeBtn = HexItemBuilder.from(Material.EMERALD_BLOCK)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.nodes_add_title")))
                .lore(ColorFormatter.format(tr("quest_editor.dialogue_main.nodes_add_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        nodesGui.setItem(6, 5, new GuiItem(addNodeBtn, event -> {
            ConfigurationSection dSec = config.getConfigurationSection("dialogues." + dialogueId);
            if (dSec == null) dSec = config.createSection("dialogues." + dialogueId);
            ConfigurationSection nSec = dSec.getConfigurationSection("nodes");
            if (nSec == null) nSec = dSec.createSection("nodes");

            String nodeKey = "start";
            if (nSec.contains("start")) {
                int idIndex = 1;
                while (nSec.contains("node_" + idIndex)) {
                    idIndex++;
                }
                nodeKey = "node_" + idIndex;
            }

            nSec.set(nodeKey + ".text", List.of(tr("quest_editor.dialogue_main.nodes_add_default_text")));
            nSec.set(nodeKey + ".sound", "ENTITY_VILLAGER_AMBIENT");
            EditorSessionManager.markUnsaved(this.player.getUniqueId());
            onDirty.run();

            isNavigating = true;
            new DialogueNodeEditorGui(plugin, player, dialogueId, nodeKey, dialogueFile, config, onDirty, this::open).open();
        }));

        nodesGui.open(player);
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

    private List<String> wrapPreviewLine(String text, int maxLineLen) {
        List<String> wrapped = new ArrayList<>();
        if (text == null || text.isBlank()) return wrapped;

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        StringBuilder lastTags = new StringBuilder();
        int currentLen = 0;

        for (String word : words) {
            String cleanWord = word.replaceAll("<[^>]*>", "").replaceAll("&[0-9a-fk-orA-FK-OR]", "");
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("<[^>]*>|&[0-9a-fk-orA-FK-OR]").matcher(word);
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
