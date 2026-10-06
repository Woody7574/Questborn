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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DialogueOptionActionGui {
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

    public DialogueOptionActionGui(QuestbornPlugin plugin, Player player, String dialogueId, String nodeKey, int optionIndex, File dialogueFile, YamlConfiguration config, Runnable onDirty, Runnable backAction) {
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

    private void updateAction(String newAction) {
        ConfigurationSection nodesSec = config.getConfigurationSection("dialogues." + dialogueId + ".nodes");
        if (nodesSec == null) return;
        List<Map<?, ?>> optionsList = nodesSec.getMapList(nodeKey + ".options");
        if (optionIndex >= 0 && optionIndex < optionsList.size()) {
            Map<String, Object> opt = (Map<String, Object>) optionsList.get(optionIndex);
            opt.put("action", newAction);
            opt.remove("goto");
            optionsList.set(optionIndex, opt);
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
            backAction.run();
            return;
        }

        String currentAction = (String) optMap.get("action");
        if (currentAction == null) currentAction = (String) optMap.get("goto");
        if (currentAction == null) currentAction = "close";
        final String fCurrentAction = currentAction;

        String titleKey = this.tr("quest_editor.dialogue_main.opt_action_gui_title", java.util.Map.of("num", String.valueOf(optionIndex + 1)));
        String title = ColorFormatter.applyColors(EditorUtils.truncateGuiTitle(EditorUtils.formatGuiTitle(titleKey, dialogueId, "")));
        Gui gui = Gui.gui()
                .title(Component.text(EditorUtils.truncateGuiTitle(title)))
                .rows(6)
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

        java.util.List<String> currentLore = new java.util.ArrayList<>();
        currentLore.add(ColorFormatter.format(tr("quest_editor.dialogue_main.action_current_selected") + " &f" + fCurrentAction));
        currentLore.addAll(this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.action_current_lore"));

        ItemStack currentItem = HexItemBuilder.from(Material.OAK_SIGN)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.action_current_title")))
                .lore(currentLore)
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(1, 5, new GuiItem(currentItem));

        HexItemBuilder nodeItemB = HexItemBuilder.from(Material.COMPASS)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.action_goto_title", java.util.Map.of("tag", "dialogue:<node>"))))
                .lore(ColorFormatter.format(tr("quest_editor.dialogue_main.action_goto_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES);
        if (fCurrentAction.startsWith("dialogue:")) {
            nodeItemB.enchant(org.bukkit.enchantments.Enchantment.DURABILITY);
            nodeItemB.flags(ItemFlag.HIDE_ENCHANTS);
        }
        ItemStack nodeItem = nodeItemB.build();
        gui.setItem(3, 2, new GuiItem(nodeItem, event -> {
            isNavigating = true;
            openNodesSelector();
        }));

        ItemStack createNodeItem = HexItemBuilder.from(Material.CHAIN)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.action_create_title")))
                .lore(ColorFormatter.format(tr("quest_editor.dialogue_main.action_create_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(5, 5, new GuiItem(createNodeItem, event -> {
            ConfigurationSection nodesSec = config.getConfigurationSection("dialogues." + dialogueId + ".nodes");
            if (nodesSec == null) nodesSec = config.createSection("dialogues." + dialogueId + ".nodes");

            int idIndex = 1;
            while (nodesSec.contains("node_" + idIndex)) {
                idIndex++;
            }
            String sanitized = "node_" + idIndex;

            nodesSec.set(sanitized + ".text", List.of(tr("quest_editor.dialogue_main.nodes_add_default_text")));
            nodesSec.set(sanitized + ".sound", "ENTITY_VILLAGER_AMBIENT");

            updateAction("dialogue:" + sanitized);

            isNavigating = true;
            new DialogueNodeEditorGui(plugin, player, dialogueId, sanitized, dialogueFile, config, onDirty, this::open).open();
        }));

        HexItemBuilder startQuestItemB = HexItemBuilder.from(Material.EMERALD)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.action_start_quest_title")))
                .lore(ColorFormatter.format(tr("quest_editor.dialogue_main.action_start_quest_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES);
        if (fCurrentAction.startsWith("quest:start:")) {
            startQuestItemB.enchant(org.bukkit.enchantments.Enchantment.DURABILITY);
            startQuestItemB.flags(ItemFlag.HIDE_ENCHANTS);
        }
        ItemStack startQuestItem = startQuestItemB.build();
        gui.setItem(3, 4, new GuiItem(startQuestItem, event -> {
            isNavigating = true;
            new QuestSelectionGui(plugin, player, null, new java.util.HashSet<>(), (selectedQuest, isAdding) -> {
                if (isAdding) {
                    updateAction("quest:start:" + selectedQuest);
                    open();
                } else {
                    open();
                }
            }, this::open).open();
        }));

        HexItemBuilder finishQuestItemB = HexItemBuilder.from(Material.GOLD_INGOT)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.action_finish_quest_title")))
                .lore(ColorFormatter.format(tr("quest_editor.dialogue_main.action_finish_quest_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES);
        if (fCurrentAction.startsWith("quest:finish:")) {
            finishQuestItemB.enchant(org.bukkit.enchantments.Enchantment.DURABILITY);
            finishQuestItemB.flags(ItemFlag.HIDE_ENCHANTS);
        }
        ItemStack finishQuestItem = finishQuestItemB.build();
        gui.setItem(3, 3, new GuiItem(finishQuestItem, event -> {
            isNavigating = true;
            new QuestSelectionGui(plugin, player, null, new java.util.HashSet<>(), (selectedQuest, isAdding) -> {
                if (isAdding) {
                    updateAction("quest:finish:" + selectedQuest);
                    open();
                } else {
                    open();
                }
            }, this::open).open();
        }));

        HexItemBuilder consoleItemB = HexItemBuilder.from(Material.REPEATING_COMMAND_BLOCK)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.action_console_cmd_title")))
                .lore(this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.action_console_cmd_lore"))
                .flags(ItemFlag.HIDE_ATTRIBUTES);
        if (fCurrentAction.startsWith("console:")) {
            consoleItemB.enchant(org.bukkit.enchantments.Enchantment.DURABILITY);
            consoleItemB.flags(ItemFlag.HIDE_ENCHANTS);
        }
        ItemStack consoleItem = consoleItemB.build();
        gui.setItem(3, 6, new GuiItem(consoleItem, event -> {
            isNavigating = true;
            plugin.getChatInputManager().requestInput(player, EditorChat.formatPrompt(tr("quest_editor.dialogue_main.action_console_prompt")), input -> {
                isNavigating = false;
                if (input != null && !input.isBlank()) {
                    updateAction("console:" + input.trim());
                }
                open();
            });
        }));

        HexItemBuilder playerCmdItemB = HexItemBuilder.from(Material.CHAIN_COMMAND_BLOCK)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.action_player_cmd_title")))
                .lore(ColorFormatter.format(tr("quest_editor.dialogue_main.action_player_cmd_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES);
        if (fCurrentAction.startsWith("player:")) {
            playerCmdItemB.enchant(org.bukkit.enchantments.Enchantment.DURABILITY);
            playerCmdItemB.flags(ItemFlag.HIDE_ENCHANTS);
        }
        ItemStack playerCmdItem = playerCmdItemB.build();
        gui.setItem(3, 5, new GuiItem(playerCmdItem, event -> {
            isNavigating = true;
            plugin.getChatInputManager().requestInput(player, EditorChat.formatPrompt(tr("quest_editor.dialogue_main.action_player_prompt")), input -> {
                isNavigating = false;
                if (input != null && !input.isBlank()) {
                    updateAction("player:" + input.trim());
                }
                open();
            });
        }));

        HexItemBuilder rawItemB = HexItemBuilder.from(Material.WRITABLE_BOOK)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.action_custom_title")))
                .lore(ColorFormatter.format(tr("quest_editor.dialogue_main.action_custom_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES);
        if ((!fCurrentAction.startsWith("dialogue:") && !fCurrentAction.startsWith("quest:start:") && !fCurrentAction.startsWith("quest:finish:") && !fCurrentAction.startsWith("console:") && !fCurrentAction.startsWith("player:") && !fCurrentAction.equals("close") && !fCurrentAction.isEmpty())) {
            rawItemB.enchant(org.bukkit.enchantments.Enchantment.DURABILITY);
            rawItemB.flags(ItemFlag.HIDE_ENCHANTS);
        }
        ItemStack rawItem = rawItemB.build();
        gui.setItem(3, 8, new GuiItem(rawItem, event -> {
            isNavigating = true;
            plugin.getChatInputManager().requestInputWithSuggestion(player, EditorChat.formatPrompt(tr("quest_editor.dialogue_main.action_raw_prompt")), fCurrentAction, input -> {
                isNavigating = false;
                if (input != null && !input.isBlank()) {
                    updateAction(input.trim());
                }
                open();
            });
        }));

        ItemStack backBtn = HexItemBuilder.from(Material.OAK_DOOR)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.action_back_title"))).lore(ColorFormatter.format(tr("quest_editor.dialogue_main.action_back_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(6, 1, new GuiItem(backBtn, event -> {
            isNavigating = true;
            backAction.run();
        }));

        gui.open(player);
        this.isNavigating = false;
    }

    private void openNodesSelector() {
        ConfigurationSection dlgSec = config.getConfigurationSection("dialogues." + dialogueId);
        ConfigurationSection nodesSec = dlgSec != null ? dlgSec.getConfigurationSection("nodes") : null;

        dev.triumphteam.gui.guis.PaginatedGui nodesGui = dev.triumphteam.gui.guis.Gui.paginated()
                .title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors(tr("quest_editor.dialogue_main.action_builder_select_node")))))
                .rows(6)
                .pageSize(45)
                .disableAllInteractions()
                .create();

        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        nodesGui.getFiller().fillBottom(new GuiItem(bg));

        EditorSessionManager.setSession(player.getUniqueId(), this::open);
        nodesGui.setCloseGuiAction(event -> {
            if (!isNavigating && !EditorSessionManager.isForceClosing) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(plugin, this.player, this::open);
            }
        });

        if (nodesSec != null) {
            for (String key : nodesSec.getKeys(false)) {
                Material iconMat = key.equalsIgnoreCase("start") ? Material.BOOKSHELF : Material.PAPER;
                ItemStack item = HexItemBuilder.from(iconMat)
                        .name(ColorFormatter.applyColors("&e" + key))
                        .lore(ColorFormatter.format(tr("quest_editor.dialogue_main.action_node_link_lore")))
                        .flags(ItemFlag.HIDE_ATTRIBUTES).build();
                nodesGui.addItem(new GuiItem(item, event -> {
                    updateAction("dialogue:" + key);
                    open();
                }));
            }
        }

        ItemStack backBtn = HexItemBuilder.from(Material.OAK_DOOR)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.action_back_title"))).lore(ColorFormatter.format(tr("quest_editor.dialogue_main.action_builder_back_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        nodesGui.setItem(6, 1, new GuiItem(backBtn, event -> this.open()));

        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        nodesGui.setItem(6, 4, new GuiItem(prevItem, event -> nodesGui.previous()));

        ItemStack addNodeBtn = HexItemBuilder.from(Material.EMERALD_BLOCK)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.action_create_title")))
                .lore(ColorFormatter.format(tr("quest_editor.dialogue_main.action_create_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        nodesGui.setItem(6, 5, new GuiItem(addNodeBtn, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInput(this.player, EditorChat.formatPrompt(this.tr("quest_editor.dialogue_main.nodes_add_prompt")), input -> {
                if (input != null && !input.isBlank()) {
                    String newNodeKey = input.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
                    ConfigurationSection nodesSecRef = this.config.getConfigurationSection("dialogues." + this.dialogueId + ".nodes");
                    if (nodesSecRef != null) {
                        nodesSecRef.set(newNodeKey + ".text", List.of(this.tr("quest_editor.dialogue_main.nodes_add_default_text")));
                        nodesSecRef.set(newNodeKey + ".sound", "ENTITY_VILLAGER_AMBIENT");
                        this.onDirty.run();
                        this.player.sendMessage(ColorFormatter.applyColors(this.tr("quest_editor.dialogue_main.nodes_add_success", java.util.Map.of("node", newNodeKey))));
                        this.updateAction("dialogue:" + newNodeKey);
                        this.open();
                        return;
                    }
                }
                this.openNodesSelector();
            });
        }));

        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        nodesGui.setItem(6, 6, new GuiItem(nextItem, event -> nodesGui.next()));

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
}
