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

public class DialogueOptionConditionGui {
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

    public DialogueOptionConditionGui(QuestbornPlugin plugin, Player player, String dialogueId, String nodeKey, int optionIndex, File dialogueFile, YamlConfiguration config, Runnable onDirty, Runnable backAction) {
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

    private void updateConditions(List<String> conditions) {
        Map<String, Object> optMap = getOptionMap();
        if (optMap == null) return;
        optMap.remove("condition");
        if (conditions.isEmpty()) {
            optMap.remove("conditions");
        } else {
            optMap.put("conditions", conditions);
        }

        ConfigurationSection nodesSec = config.getConfigurationSection("dialogues." + dialogueId + ".nodes");
        List<Map<?, ?>> optionsList = nodesSec.getMapList(nodeKey + ".options");
        optionsList.set(optionIndex, optMap);
        nodesSec.set(nodeKey + ".options", optionsList);
        EditorSessionManager.markUnsaved(this.player.getUniqueId());
        onDirty.run();
    }

    private List<String> getConditions() {
        Map<String, Object> optMap = getOptionMap();
        List<String> condList = new ArrayList<>();
        if (optMap != null) {
            if (optMap.containsKey("conditions")) {
                Object rawConds = optMap.get("conditions");
                if (rawConds instanceof List) condList.addAll((List<String>) rawConds);
            } else if (optMap.containsKey("condition")) {
                condList.add((String) optMap.get("condition"));
            }
        }
        return condList;
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
        List<String> currentConds = getConditions();

        String titleKey = tr("quest_editor.dialogue_main.cond_gui_title", java.util.Map.of("num", String.valueOf(optionIndex + 1)));
        String title = EditorUtils.formatGuiTitle(titleKey, dialogueId, "");
        Gui gui = Gui.gui()
                .title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors(title))))
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

        List<String> displayLore = new ArrayList<>();
        for (int i = 0; i < currentConds.size(); i++) {
            displayLore.add(ColorFormatter.format("&7" + (i + 1) + ". &f" + currentConds.get(i)));
        }
        if (displayLore.isEmpty()) displayLore.add(ColorFormatter.format(tr("quest_editor.dialogue_main.cond_none")));
        displayLore.add("");
        displayLore.add(ColorFormatter.format(tr("quest_editor.dialogue_main.cond_clear")));

        ItemStack currentItem = HexItemBuilder.from(Material.REPEATER)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.cond_current_title")))
                .lore(displayLore)
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(1, 5, new GuiItem(currentItem, event -> {
            if (event.isRightClick() && !currentConds.isEmpty()) {
                updateConditions(new ArrayList<>());
                open();
            }
        }));

        ItemStack questItem = HexItemBuilder.from(Material.ENDER_EYE)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.cond_quest_title")))
                .lore(ColorFormatter.format(tr("quest_editor.dialogue_main.cond_quest_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(3, 3, new GuiItem(questItem, event -> {
            isNavigating = true;
            new QuestSelectionGui(plugin, player, null, new java.util.HashSet<>(), (selectedQuest, isAdding) -> {
                if (isAdding) {
                    isNavigating = true;
                    plugin.getChatInputManager().requestInputWithSuggestion(player, EditorChat.formatPrompt(tr("quest_editor.dialogue_main.cond_quest_prompt")), "COMPLETED", input -> {
                        isNavigating = false;
                        if (input != null && !input.isBlank()) {
                            List<String> list = getConditions();
                            list.add("quest_state:" + selectedQuest + ":" + input.trim().toUpperCase());
                            updateConditions(list);
                        }
                        open();
                    });
                } else {
                    open();
                }
            }, this::open).open();
        }));

        ItemStack rawItem = HexItemBuilder.from(Material.WRITABLE_BOOK)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.cond_raw_title")))
                .lore(ColorFormatter.format(tr("quest_editor.dialogue_main.cond_raw_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(3, 7, new GuiItem(rawItem, event -> {
            isNavigating = true;
            plugin.getChatInputManager().requestInput(player, EditorChat.formatPrompt(tr("quest_editor.dialogue_main.cond_raw_prompt")), input -> {
                isNavigating = false;
                if (input != null && !input.isBlank()) {
                    List<String> list = getConditions();
                    list.add(input.trim());
                    updateConditions(list);
                }
                open();
            });
        }));

        ItemStack permItem = HexItemBuilder.from(Material.BLAZE_POWDER)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.cond_perm_title")))
                .lore(ColorFormatter.format(tr("quest_editor.dialogue_main.cond_perm_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(3, 5, new GuiItem(permItem, event -> {
            isNavigating = true;
            plugin.getChatInputManager().requestInput(player, EditorChat.formatPrompt(tr("quest_editor.dialogue_main.cond_perm_prompt")), input -> {
                isNavigating = false;
                if (input != null && !input.isBlank()) {
                    List<String> list = getConditions();
                    list.add("permission:" + input.trim());
                    updateConditions(list);
                }
                open();
            });
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
