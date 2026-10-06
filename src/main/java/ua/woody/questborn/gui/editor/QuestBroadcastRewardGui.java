package ua.woody.questborn.gui.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.QuestConfirmationGui;
import ua.woody.questborn.gui.editor.QuestEditorListGui;
import ua.woody.questborn.gui.editor.UnsavedChangesConfirmGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestBroadcastRewardGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String questId;
    private final File questFile;
    private final YamlConfiguration questConfig;
    private final String yamlPath;
    private final Runnable backAction;
    private Runnable onDirty = null;
    private boolean isNavigating = false;
    private boolean hasChanges = false;
    private PaginatedGui gui;

    public QuestBroadcastRewardGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig, String yamlPath, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.questId = questId;
        this.questFile = questFile;
        this.questConfig = questConfig;
        this.yamlPath = yamlPath;
        this.backAction = backAction;
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

    public QuestBroadcastRewardGui setOnDirty(Runnable onDirty) {
        this.onDirty = onDirty;
        return this;
    }

    private void saveConfig() {
        this.hasChanges = true;
        if (this.onDirty != null) {
            this.onDirty.run();
        }
    }

    public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors("&0" + this.tr("quest_editor.rewards_menu.brd_gui")))))).rows(6)).pageSize(45).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            if (this.hasChanges && !this.isNavigating) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
                    this.isNavigating = true;
                    new UnsavedChangesConfirmGui(this.player, () -> {
                        EditorUtils.saveQuestConfig(this.plugin, this.questConfig, this.questFile);
                        this.hasChanges = false;
                        this.isNavigating = true;
                        new QuestEditorListGui(this.plugin, this.player).open();
                    }, () -> {
                        this.hasChanges = false;
                        this.isNavigating = true;
                        new QuestEditorListGui(this.plugin, this.player).open();
                    }, () -> {
                        Runnable returnAction = EditorSessionManager.getReturnAction(this.player.getUniqueId());
                        if (returnAction != null) {
                            returnAction.run();
                        } else {
                            this.open();
                        }
                    }).open();
                });
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        ItemStack addItem = HexItemBuilder.from(Material.EMERALD_BLOCK).name(ColorFormatter.format(this.tr("common_editor.buttons.add_new"))).lore(ColorFormatter.format(this.tr("quest_editor.list_string_editor.add_lore")), ColorFormatter.format(this.tr("quest_editor.list_string_editor.add_lore_empty"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 5, new GuiItem(addItem, event -> {
            if (event.isRightClick()) {
                List rawList = this.questConfig.getList(this.yamlPath);
                ArrayList<String> list = rawList == null ? new ArrayList<String>() : new ArrayList(rawList);
                list.add("");
                this.questConfig.set(this.yamlPath, list);
                this.saveConfig();
                this.open();
            } else {
                this.isNavigating = true;
                this.plugin.getChatInputManager().requestInput(this.player, EditorChat.formatPrompt(this.tr("quest_editor.list_string_editor.prompt_add")), input -> {
                    if (input != null && !input.isEmpty()) {
                        List rawList = this.questConfig.getList(this.yamlPath);
                        ArrayList<String> list = rawList == null ? new ArrayList<String>() : new ArrayList(rawList);
                        list.add((String)input);
                        this.questConfig.set(this.yamlPath, list);
                        this.saveConfig();
                    }
                    this.open();
                });
            }
        }));
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            this.backAction.run();
        }));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 3, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 7, new GuiItem(nextItem, event -> this.gui.next()));
        List msgs = this.questConfig.getList(this.yamlPath);
        if (msgs != null && !msgs.isEmpty()) {
            for (int i = 0; i < msgs.size(); ++i) {
                int index = i;
                String line = String.valueOf(msgs.get(i));
                ArrayList<String> loreLines = new ArrayList<String>();
                loreLines.add(ColorFormatter.format(line));
                loreLines.add("");
                loreLines.add(ColorFormatter.format(this.tr("quest_editor.brd_reward.click_edit")));
                if (index > 0) {
                    loreLines.add(ColorFormatter.format(this.tr("quest_editor.brd_reward.click_move_up")));
                }
                if (index < msgs.size() - 1) {
                    loreLines.add(ColorFormatter.format(this.tr("quest_editor.brd_reward.click_move_down")));
                }
                loreLines.add(ColorFormatter.format(this.tr("quest_editor.brd_reward.click_clone")));
                loreLines.add(ColorFormatter.format(this.tr("quest_editor.brd_reward.click_delete")));
                ItemStack item = HexItemBuilder.from(Material.OAK_SIGN).name(ColorFormatter.format(this.tr("quest_editor.brd_reward.item_format", Map.of("index", String.valueOf(index + 1))))).lore(loreLines.toArray(new String[0])).flags(ItemFlag.HIDE_ATTRIBUTES).build();
                this.gui.addItem(new GuiItem(item, event -> {
                    if (event.getClick() == ClickType.DROP) {
                        this.isNavigating = true;
                        new QuestConfirmationGui(this.plugin, this.player, this.tr("quest_editor.list_string_editor.delete_confirm_title"), this.tr("quest_editor.list_string_editor.delete_confirm_item"), this.tr("quest_editor.list_string_editor.delete_confirm_msg", Map.of("index", String.valueOf(index + 1))), Material.OAK_SIGN, () -> {
                            List currentRaw = this.questConfig.getList(this.yamlPath);
                            if (currentRaw != null && index < currentRaw.size()) {
                                ArrayList currentList = new ArrayList(currentRaw);
                                currentList.remove(index);
                                this.questConfig.set(this.yamlPath, currentList);
                                this.saveConfig();
                            }
                            this.open();
                        }, () -> this.open()).open();
                    } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
                        List currentRaw2 = this.questConfig.getList(this.yamlPath);
                        if (currentRaw2 != null && index < currentRaw2.size()) {
                            ArrayList currentList = new ArrayList(currentRaw2);
                            currentList.add(index + 1, currentList.get(index));
                            this.questConfig.set(this.yamlPath, currentList);
                            this.saveConfig();
                            this.open();
                        }
                    } else if (event.isLeftClick() && !event.isShiftClick()) {
                        this.isNavigating = true;
                        this.plugin.getChatInputManager().requestInput(this.player, this.tr("quest_editor.list_string_editor.prompt_add"), input -> {
                            List currentRaw = this.questConfig.getList(this.yamlPath);
if (input != null && !input.isEmpty() && currentRaw != null && index < currentRaw.size()) {
                                ArrayList<String> currentList = new ArrayList<String>(currentRaw);
                                currentList.set(index, (String)input);
                                this.questConfig.set(this.yamlPath, currentList);
                                this.saveConfig();
                            }
                            this.open();
                        });
                    } else if (event.isLeftClick() && event.isShiftClick()) {
                        List currentRaw3 = this.questConfig.getList(this.yamlPath);
if (index > 0 && currentRaw3 != null && index < currentRaw3.size()) {
                            ArrayList currentList = new ArrayList(currentRaw3);
                            Object temp = currentList.get(index - 1);
                            currentList.set(index - 1, currentList.get(index));
                            currentList.set(index, temp);
                            this.questConfig.set(this.yamlPath, currentList);
                            this.saveConfig();
                            this.open();
                        }
                    } else if (event.isRightClick() && event.isShiftClick()) {
List currentRaw = this.questConfig.getList(this.yamlPath);
if (currentRaw != null && index < currentRaw.size() - 1) {
                        ArrayList currentList = new ArrayList(currentRaw);
                        Object temp = currentList.get(index + 1);
                        currentList.set(index + 1, currentList.get(index));
                        currentList.set(index, temp);
                        this.questConfig.set(this.yamlPath, currentList);
                        this.saveConfig();
                        this.open();
                    }
                }
            }));
            }
        } else {
            ItemStack emptyItem = HexItemBuilder.from(Material.STRUCTURE_VOID).name(ColorFormatter.format(this.tr("common_editor.buttons.empty_list"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.empty_list_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(3, 5, new GuiItem(emptyItem));
        }
        this.isNavigating = true;
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }
}
