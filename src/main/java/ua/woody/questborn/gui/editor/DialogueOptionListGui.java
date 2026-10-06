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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DialogueOptionListGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String dialogueId;
    private final String nodeKey;
    private final File dialogueFile;
    private final YamlConfiguration config;
    private final Runnable onDirty;
    private final Runnable backAction;
    private boolean isNavigating = false;

    public DialogueOptionListGui(QuestbornPlugin plugin, Player player, String dialogueId, String nodeKey, File dialogueFile, YamlConfiguration config, Runnable onDirty, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.dialogueId = dialogueId;
        this.nodeKey = nodeKey;
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
        dev.triumphteam.gui.guis.PaginatedGui gui = dev.triumphteam.gui.guis.Gui.paginated()
                .title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors(tr("quest_editor.dialogue_main.opt_list_gui_title") + nodeKey))))
                .rows(6)
                .pageSize(45)
                .disableAllInteractions()
                .create();

        EditorSessionManager.setSession(player.getUniqueId(), this::handleEscClose);
        gui.setCloseGuiAction(event -> {
            if (!isNavigating && !EditorSessionManager.isForceClosing) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(plugin, this.player, this::handleEscClose);
            }
        });

        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.getFiller().fillBottom(new GuiItem(bg));

        ConfigurationSection nodesSec = config.getConfigurationSection("dialogues." + dialogueId + ".nodes");
        if (nodesSec != null && nodesSec.contains(nodeKey)) {
            List<Map<?, ?>> options = nodesSec.getMapList(nodeKey + ".options");

            for (int i = 0; i < options.size(); i++) {
                final int index = i;
                Map<?, ?> optMap = options.get(i);

                String text = (String) optMap.get("text");
                if(text == null) text = "No Text";

                String material = (String) optMap.get("material");
                ItemStack baseItem = new ItemStack(Material.PAPER);
                if (material != null) {
                    String matStr = material.trim();
                    if ((matStr.toLowerCase().startsWith("craftengine:") || matStr.toLowerCase().startsWith("ce:")) && plugin.getCraftEngineIntegration() != null && plugin.getCraftEngineIntegration().isEnabled()) {
                        String ceId = matStr.toLowerCase().startsWith("ce:") ? matStr.substring(3) : matStr.substring(12);
                        ItemStack ceItem = plugin.getCraftEngineIntegration().getCustomItem(ceId);
                        if (ceItem != null) baseItem = ceItem.clone();
                    } else if ((matStr.toLowerCase().startsWith("itemsadder:") || matStr.toLowerCase().startsWith("ia:")) && plugin.getItemsAdderIntegration() != null && plugin.getItemsAdderIntegration().isEnabled()) {
                        String iaId = matStr.toLowerCase().startsWith("ia:") ? matStr.substring(3) : matStr.substring(11);
                        ItemStack iaItem = plugin.getItemsAdderIntegration().getCustomItem(iaId);
                        if (iaItem != null) baseItem = iaItem.clone();
                    } else {
                        try { baseItem = new ItemStack(Material.valueOf(matStr.toUpperCase())); } catch(Exception ignored){}
                    }
                }

                String action = (String) optMap.get("action");
                if(action == null) action = (String) optMap.get("goto");
                if(action == null) action = "close";

                List<String> lore = new ArrayList<>();
                lore.add(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_list_text", java.util.Map.of("text", EditorUtils.truncateGuiTitle(text.replaceAll("<[^>]*>", "").replaceAll("&[0-9a-fk-orA-FK-OR]", ""))))));
                if (action.startsWith("dialogue:")) {
                    String targetNode = action.substring(9);
                    if (nodesSec.contains(targetNode)) {
                        lore.add(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_linked_node_lore", java.util.Map.of("node", targetNode))));
                    } else {
                        lore.add(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_linked_node_missing_lore", java.util.Map.of("node", targetNode))));
                    }
                } else if (action.equalsIgnoreCase("close")) {
                    lore.add(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_linked_close_lore")));
                } else {
                    lore.add(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_linked_action_lore", java.util.Map.of("action", action))));
                }

                if (optMap.containsKey("slot")) {
                    lore.add(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_list_slot", java.util.Map.of("slot", String.valueOf(optMap.get("slot"))))));
                }

                lore.add("");
                lore.addAll(this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.opt_list_click"));

                ItemStack item = HexItemBuilder.from(baseItem)
                        .name(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_list_item_title", java.util.Map.of("index", String.valueOf(i + 1)))))
                        .lore(lore)
                        .flags(ItemFlag.HIDE_ATTRIBUTES).build();

                gui.addItem(new GuiItem(item, event -> {
                    isNavigating = true;
                    if (event.getClick() == org.bukkit.event.inventory.ClickType.DROP) {
                        new QuestConfirmationGui(plugin, player, tr("quest_editor.dialogue_main.opt_list_del_gui_title"), tr("quest_editor.dialogue_main.opt_list_del_title"), tr("quest_editor.dialogue_main.opt_list_del_desc", java.util.Map.of("index", String.valueOf(index + 1))), () -> {
                            List<Map<?, ?>> newOptions = nodesSec.getMapList(nodeKey + ".options");
                            if (index < newOptions.size()) {
                                newOptions.remove(index);
                                nodesSec.set(nodeKey + ".options", newOptions);
                                EditorSessionManager.markUnsaved(this.player.getUniqueId());
                                onDirty.run();
                            }
                            open();
                        }, this::open).open();
                    } else {
                        new DialogueOptionEditorGui(plugin, player, dialogueId, nodeKey, index, dialogueFile, config, onDirty, this::open).open();
                    }
                }));
            }
        }

        ItemStack backBtn = HexItemBuilder.from(Material.OAK_DOOR)
                .name(ColorFormatter.format(tr("common_editor.buttons.back"))).lore(ColorFormatter.format(tr("common_editor.buttons.back_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(6, 1, new GuiItem(backBtn, event -> {
            isNavigating = true;
            backAction.run();
        }));

        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(6, 4, new GuiItem(prevItem, event -> ((dev.triumphteam.gui.guis.PaginatedGui)gui).previous()));

        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(6, 6, new GuiItem(nextItem, event -> ((dev.triumphteam.gui.guis.PaginatedGui)gui).next()));

        ItemStack addBtn = HexItemBuilder.from(Material.EMERALD_BLOCK)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_list_add_title")))
                .lore(ColorFormatter.format(tr("quest_editor.dialogue_main.opt_list_add_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(6, 5, new GuiItem(addBtn, event -> {
            ConfigurationSection nodesSec1 = config.getConfigurationSection("dialogues." + dialogueId + ".nodes");
            if (nodesSec1 != null) {
                List<Map<?, ?>> options = nodesSec1.getMapList(nodeKey + ".options");
                Map<String, Object> newOpt = new LinkedHashMap<>();
                newOpt.put("text", "New Option");
                newOpt.put("action", "close");
                options.add(newOpt);
                nodesSec1.set(nodeKey + ".options", options);
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
                onDirty.run();
                open();
            }
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
