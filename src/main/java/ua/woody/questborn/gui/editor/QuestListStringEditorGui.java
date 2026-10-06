package ua.woody.questborn.gui.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
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
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestListStringEditorGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String questId;
    private final File questFile;
    private final YamlConfiguration questConfig;
    private final String yamlPath;
    private final String title;
    private final Runnable backAction;
    private Consumer<Consumer<String>> customAddAction;
    private Runnable onDirty = null;
    private String addButtonName;
    private String addButtonLore;
    private String addButtonLoreEmpty;
    private String itemNameFormat;
    private String itemLoreEdit;
    private String itemLoreDelete;
    private String itemLoreMoveUp;
    private String itemLoreMoveDown;
    private String itemLoreClone;
    private boolean showRawString = true;
    private Function<String, ItemStack> iconProvider = null;
    private BiFunction<String, Integer, String> itemNameProvider = null;
    private PaginatedGui gui;
    private boolean isNavigating = false;

    public QuestListStringEditorGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig, String yamlPath, String title, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.questId = questId;
        this.questFile = questFile;
        this.questConfig = questConfig;
        this.yamlPath = yamlPath;
        this.title = title;
        this.backAction = backAction;
        this.addButtonLore = this.tr("quest_editor.list_string_editor.add_lore");
        this.addButtonLoreEmpty = this.tr("quest_editor.list_string_editor.add_lore_empty");
        this.itemNameFormat = this.tr("quest_editor.list_string_editor.item_format").replace("{index}", "%d");
        this.itemLoreEdit = this.tr("quest_editor.list_string_editor.item_edit");
        this.itemLoreDelete = this.tr("quest_editor.list_string_editor.item_delete");
        this.itemLoreMoveUp = this.tr("quest_editor.list_string_editor.item_move_up");
        this.itemLoreMoveDown = this.tr("quest_editor.list_string_editor.item_move_down");
        this.itemLoreClone = this.tr("quest_editor.list_string_editor.item_clone");
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

    public QuestListStringEditorGui setCustomAddAction(Consumer<Consumer<String>> customAddAction) {
        this.customAddAction = customAddAction;
        return this;
    }

    public QuestListStringEditorGui setOnDirty(Runnable onDirty) {
        this.onDirty = onDirty;
        return this;
    }

    public QuestListStringEditorGui setAddButtonName(String addButtonName) {
        this.addButtonName = addButtonName;
        return this;
    }

    public QuestListStringEditorGui setAddButtonLore(String addButtonLore) {
        this.addButtonLore = addButtonLore;
        return this;
    }

    public QuestListStringEditorGui setItemNameFormat(String itemNameFormat) {
        this.itemNameFormat = itemNameFormat;
        return this;
    }

    public QuestListStringEditorGui setItemLoreEdit(String itemLoreEdit) {
        this.itemLoreEdit = itemLoreEdit;
        return this;
    }

    public QuestListStringEditorGui setItemLoreDelete(String itemLoreDelete) {
        this.itemLoreDelete = itemLoreDelete;
        return this;
    }

    public QuestListStringEditorGui setShowRawString(boolean showRawString) {
        this.showRawString = showRawString;
        return this;
    }

    public QuestListStringEditorGui setIconProvider(Function<String, ItemStack> iconProvider) {
        this.iconProvider = iconProvider;
        return this;
    }

    public QuestListStringEditorGui setItemNameProvider(BiFunction<String, Integer, String> itemNameProvider) {
        this.itemNameProvider = itemNameProvider;
        return this;
    }

    public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors("&0" + this.title))))).rows(6)).pageSize(45).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            Runnable closeAction;
            if (!this.isNavigating && !EditorSessionManager.isForceClosing && (closeAction = EditorSessionManager.getSession(this.player.getUniqueId())) != null) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, closeAction);
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        String nameToAdd = this.addButtonName != null ? this.addButtonName : this.tr("common_editor.buttons.add_new");
        ItemStack addItem = HexItemBuilder.from(Material.EMERALD_BLOCK).name(ColorFormatter.format(nameToAdd)).lore(ColorFormatter.format(this.addButtonLore), ColorFormatter.format(this.addButtonLoreEmpty)).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 5, new GuiItem(addItem, event -> {
            if (event.isRightClick()) {
                List rawList = this.questConfig.getList(this.yamlPath);
                ArrayList<String> list = rawList == null ? new ArrayList<String>() : new ArrayList(rawList);
                list.add("");
                this.questConfig.set(this.yamlPath, list);
                this.saveConfig();
                this.open();
            } else if (this.customAddAction != null) {
                this.customAddAction.accept(input -> {
                    if (input != null && !input.isEmpty()) {
                        List rawList = this.questConfig.getList(this.yamlPath);
                        ArrayList<String> list = rawList == null ? new ArrayList<String>() : new ArrayList(rawList);
                        list.add((String)input);
                        this.questConfig.set(this.yamlPath, list);
                        this.saveConfig();
                    }
                    this.open();
                });
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
        this.gui.setItem(6, 4, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 6, new GuiItem(nextItem, event -> this.gui.next()));
        this.populateList();
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }

    private void saveConfig() {
        if (this.onDirty != null) {
            this.onDirty.run();
        }
    }

    private void populateList() {
        List rawList = this.questConfig.getList(this.yamlPath);
        if (rawList == null || rawList.isEmpty()) {
            ItemStack emptyItem = HexItemBuilder.from(Material.STRUCTURE_VOID).name(ColorFormatter.format(this.tr("common_editor.buttons.empty_list"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.empty_list_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(3, 5, new GuiItem(emptyItem));
            return;
        }
        for (int i = 0; i < rawList.size(); ++i) {
            ItemStack item;
            int index = i;
            Object obj = rawList.get(i);
            String line = String.valueOf(obj);
            ItemStack displayItem = new ItemStack(line.trim().isEmpty() ? Material.MAP : Material.FILLED_MAP);
            int displayAmount = 1;
            if (obj instanceof Map) {
                Map map = (Map)obj;
                String matStr = null;
                if (map.containsKey("material")) {
                    matStr = String.valueOf(map.get("material"));
                } else if (map.containsKey("type")) {
                    matStr = String.valueOf(map.get("type"));
                }

                if (matStr != null) {
                    if (matStr.toLowerCase().startsWith("craftengine:") || matStr.toLowerCase().startsWith("ce:")) {
                        String id = matStr.toLowerCase().startsWith("ce:") ? matStr.substring(3) : matStr.substring(12);
                        if (plugin.getCraftEngineIntegration() != null && plugin.getCraftEngineIntegration().isEnabled()) {
                            ItemStack ceItem = plugin.getCraftEngineIntegration().getCustomItem(id);
                            if (ceItem != null) displayItem = ceItem.clone();
                        }
                    } else if (matStr.toLowerCase().startsWith("itemsadder:") || matStr.toLowerCase().startsWith("ia:")) {
                        String id = matStr.toLowerCase().startsWith("ia:") ? matStr.substring(3) : matStr.substring(11);
                        if (plugin.getItemsAdderIntegration() != null && plugin.getItemsAdderIntegration().isEnabled()) {
                            ItemStack iaItem = plugin.getItemsAdderIntegration().getCustomItem(id);
                            if (iaItem != null) displayItem = iaItem.clone();
                        }
                    } else {
                        try {
                            displayItem = new ItemStack(Material.valueOf(matStr.toUpperCase()));
                        } catch (Exception ignored) {}
                    }
                }
                if (map.containsKey("amount")) {
                    try {
                        displayAmount = Integer.parseInt(String.valueOf(map.get("amount")));
                    }
                    catch (Exception exception) {
                    }
                }
            }
            if (this.iconProvider != null) {
                item = this.iconProvider.apply(line);
            } else {
                ArrayList<String> lore = new ArrayList<String>();
                if (this.showRawString) {
                    String displayLine = line.replaceAll("(?i)(</?(?:whisper|shout|normal)>)", "<#ffffff>$1<#ffffff>");
                    lore.add(ColorFormatter.format("<#ffffff>" + displayLine));
                    lore.add("");
                }
                lore.add(ColorFormatter.format(this.itemLoreEdit));
                lore.add(ColorFormatter.format(this.itemLoreMoveUp));
                lore.add(ColorFormatter.format(this.itemLoreMoveDown));
                lore.add(ColorFormatter.format(this.itemLoreClone));
                lore.add(ColorFormatter.format(this.itemLoreDelete));
                String name = this.itemNameProvider != null ? this.itemNameProvider.apply(line, index + 1) : String.format(this.itemNameFormat, index + 1);
                item = HexItemBuilder.from(displayItem).amount(displayAmount).name(ColorFormatter.format(name)).lore(lore).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            }
            Material finalDisplayMat = displayItem != null ? displayItem.getType() : Material.STONE;
            this.gui.addItem(new GuiItem(item, event -> {
                if (event.getClick() == ClickType.DROP) {
                    new QuestConfirmationGui(this.plugin, this.player, this.tr("quest_editor.list_string_editor.delete_confirm_title"), this.tr("quest_editor.list_string_editor.delete_confirm_item"), this.tr("quest_editor.list_string_editor.delete_confirm_msg", Map.of("index", String.valueOf(index + 1))), finalDisplayMat, () -> {
                        List currentRaw = this.questConfig.getList(this.yamlPath);
                        if (currentRaw != null && index < currentRaw.size()) {
                            ArrayList currentList = new ArrayList(currentRaw);
                            currentList.remove(index);
                            this.questConfig.set(this.yamlPath, currentList);
                            this.saveConfig();
                            this.open();
                        } else {
                            this.open();
                        }
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
                    if (this.customAddAction != null) {
                        this.customAddAction.accept(input -> {
                            List currentRaw = this.questConfig.getList(this.yamlPath);
if (input != null && !input.isEmpty() && currentRaw != null && index < currentRaw.size()) {
                                ArrayList<String> currentList = new ArrayList<String>(currentRaw);
                                currentList.set(index, (String)input);
                                this.questConfig.set(this.yamlPath, currentList);
                                this.saveConfig();
                            }
                            this.open();
                        });
                    } else {
                        this.isNavigating = true;
                        this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_editor.list_string_editor.prompt_edit")), line, input -> {
                            List currentRaw = this.questConfig.getList(this.yamlPath);
if (input != null && !input.isEmpty() && currentRaw != null && index < currentRaw.size()) {
                                ArrayList<String> currentList = new ArrayList<String>(currentRaw);
                                currentList.set(index, (String)input);
                                this.questConfig.set(this.yamlPath, currentList);
                                this.saveConfig();
                            }
                            this.open();
                        });
                    }
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
    }
}
