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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.GuiUtils;
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.QuestConfirmationGui;
import ua.woody.questborn.gui.editor.QuestMaterialSelectorGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestRequiredMaterialsGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String questId;
    private final File questFile;
    private final YamlConfiguration questConfig;
    private final String yamlPath;
    private final String title;
    private final Runnable backAction;
    private PaginatedGui gui;
    private boolean isNavigating = false;
    private boolean hasChanges = false;
    private Runnable onDirty;
    private String customAddItemName;
    private List<String> customAddItemLore;

    public QuestRequiredMaterialsGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig, String yamlPath, String title, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.questId = questId;
        this.questFile = questFile;
        this.questConfig = questConfig;
        this.yamlPath = yamlPath;
        this.title = title;
        this.backAction = backAction;
    }

    public QuestRequiredMaterialsGui setOnDirty(Runnable onDirty) {
        this.onDirty = onDirty;
        return this;
    }

    public QuestRequiredMaterialsGui setAddButtonName(String name) {
        this.customAddItemName = name;
        return this;
    }

    public QuestRequiredMaterialsGui setAddButtonLore(List<String> lore) {
        this.customAddItemLore = lore;
        return this;
    }

    private void markDirty() {
        this.hasChanges = true;
        if (this.onDirty != null) {
            this.onDirty.run();
        }
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
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors("&0" + this.title))))).rows(6)).pageSize(45).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            Runnable closeAction;
            if (!this.isNavigating && !EditorSessionManager.isForceClosing && (closeAction = EditorSessionManager.getSession(this.player.getUniqueId())) != null) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, closeAction);
            }
        });
        ItemStack handBtn = HexItemBuilder.from(Material.HOPPER).name(ColorFormatter.format(this.tr("quest_editor.rewards_menu.hand_btn_title"))).lore(ColorFormatter.format(this.tr("quest_editor.rewards_menu.hand_btn_lore")), ColorFormatter.format(this.tr("quest_editor.rewards_menu.hand_btn_lore2"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 8, new GuiItem(handBtn, event -> {
            ItemStack inHand = this.player.getInventory().getItemInMainHand();
            if (inHand != null && inHand.getType() != Material.AIR) {
                this.markDirty();
                Object currentRaw = this.questConfig.get(this.yamlPath);
                ArrayList<ItemStack> newList = new ArrayList<ItemStack>();
                if (currentRaw instanceof List) {
                    newList.addAll((List)currentRaw);
                }
                newList.add(inHand.clone());
                this.questConfig.set(this.yamlPath, newList);
                this.saveConfig();
                String itemName = inHand.hasItemMeta() && inHand.getItemMeta().hasDisplayName() ? inHand.getItemMeta().getDisplayName() : inHand.getType().name();
                EditorChat.sendSuccess(this.player, this.tr("quest_editor.rewards_menu.hand_added", Map.of("name", itemName, "amount", String.valueOf(inHand.getAmount()))));
                this.refresh();
            } else {
                EditorChat.sendError(this.player, this.tr("quest_editor.rewards_menu.hand_empty"));
            }
        }));
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        String addName = this.customAddItemName != null ? this.customAddItemName : this.tr("common_editor.buttons.add_item");
        List<String> addLore = this.customAddItemLore != null ? this.customAddItemLore : List.of(this.tr("common_editor.buttons.add_item_lore1"), this.tr("common_editor.buttons.add_item_lore2"));
        ArrayList<String> formattedLore = new ArrayList<String>();
        for (String l : addLore) {
            formattedLore.add(ColorFormatter.format(l));
        }
        ItemStack addItem = HexItemBuilder.from(Material.EMERALD_BLOCK).name(ColorFormatter.format(addName)).lore(formattedLore).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 5, new GuiItem(addItem, event -> {
            this.isNavigating = true;
            new QuestMaterialSelectorGui(this.plugin, this.player, (selectedMat, baseHead) -> {
                ArrayList<Object> newList;
                List currentRaw = this.questConfig.getList(this.yamlPath);
                ArrayList<Object> arrayList = newList = currentRaw == null ? new ArrayList<Object>() : new ArrayList(currentRaw);
                if (selectedMat.equals("PLAYER_HEAD") && baseHead != null) {
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    GuiUtils.applyBaseHead(head, baseHead);
                    newList.add(head);
                } else {
                    boolean exists = false;
                    for (int i = 0; i < newList.size(); ++i) {
                        String line;
                        String[] parts;
                        Object obj = newList.get(i);
                        if (!(obj instanceof String) || (parts = (line = (String)obj).split(":")).length <= 0 || !parts[0].equalsIgnoreCase((String)selectedMat)) continue;
                        exists = true;
                        int amount = 1;
                        if (parts.length > 1) {
                            try {
                                amount = Integer.parseInt(parts[1]);
                            }
                            catch (Exception exception) {
                            }
                        }
                        newList.set(i, selectedMat + ":" + (amount + 1));
                        break;
                    }
                    if (!exists) {
                        newList.add(selectedMat + ":1");
                    }
                }
                this.questConfig.set(this.yamlPath, newList);
                this.saveConfig();
                this.refresh();
            }, this::open).open();
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

    private void refresh() {
        this.isNavigating = true;
        this.open();
    }

    private void saveConfig() {
        this.hasChanges = true;
        if (this.onDirty != null) {
            this.onDirty.run();
        }
    }

    private void populateList() {
        List rawList = this.questConfig.getList(this.yamlPath);
        if (rawList == null || rawList.isEmpty()) {
            ItemStack emptyItem = HexItemBuilder.from(Material.STRUCTURE_VOID).name(ColorFormatter.format(this.tr("common_editor.item_list.empty_title"))).lore(this.plugin.getLanguage().trEditorList("common_editor.item_list.empty_lore")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(3, 5, new GuiItem(emptyItem));
            return;
        }
        for (int i = 0; i < rawList.size(); ++i) {
            int index = i;
            Object entry = rawList.get(i);
            ItemStack itemStack = null;
            if (entry instanceof ItemStack) {
                ItemStack s = (ItemStack)entry;
                itemStack = s.clone();
            } else if (entry instanceof Map) {
                Map m = (Map)entry;
                try {
                    ItemStack deserialized;
                    itemStack = deserialized = ItemStack.deserialize((Map)m);
                }
                catch (Exception deserialized) {}
            } else if (entry instanceof String) {
                String line = ((String)entry).trim();
                int displayAmount = 1;
                itemStack = null;

                if (line.toLowerCase().startsWith("craftengine:") || line.toLowerCase().startsWith("ce:")) {
                    String[] parts = line.split("[:;]");
                    String id = parts.length > 1 ? parts[1] : "";
                    if (parts.length > 2) {
                        try { displayAmount = Integer.parseInt(parts[2]); } catch (Exception ignored) {}
                    }
                    if (plugin.getCraftEngineIntegration() != null && plugin.getCraftEngineIntegration().isEnabled()) {
                        ItemStack ceItem = plugin.getCraftEngineIntegration().getCustomItem(id);
                        if (ceItem != null) {
                            itemStack = ceItem.clone();
                            itemStack.setAmount(Math.max(1, displayAmount));
                        }
                    }
                } else if (line.toLowerCase().startsWith("itemsadder:") || line.toLowerCase().startsWith("ia:")) {
                    String[] parts = line.split("[:;]");
                    String id = parts.length > 1 ? parts[1] : "";
                    if (parts.length > 2) {
                        try { displayAmount = Integer.parseInt(parts[2]); } catch (Exception ignored) {}
                    }
                    if (plugin.getItemsAdderIntegration() != null && plugin.getItemsAdderIntegration().isEnabled()) {
                        ItemStack iaItem = plugin.getItemsAdderIntegration().getCustomItem(id);
                        if (iaItem != null) {
                            itemStack = iaItem.clone();
                            itemStack.setAmount(Math.max(1, displayAmount));
                        }
                    }
                } else {
                    String[] parts = line.split("[:;]");
                    Material displayMat = Material.STONE;
                    if (parts.length > 0) {
                        try { displayMat = Material.valueOf(parts[0].toUpperCase()); } catch (Exception ignored) {}
                    }
                    if (parts.length > 1) {
                        try { displayAmount = Integer.parseInt(parts[1]); } catch (Exception ignored) {}
                    }
                    itemStack = new ItemStack(displayMat, Math.max(1, displayAmount));
                }
            }
            if (itemStack == null) {
                itemStack = new ItemStack(Material.STONE, 1);
            }
            int currentAmount = itemStack.getAmount();
            ItemStack finalStack = itemStack.clone();
            ItemMeta meta = itemStack.getItemMeta();
            ArrayList<String> lore = new ArrayList<String>();
            if (meta != null && meta.hasLore()) {
                lore.addAll(meta.getLore());
                lore.add("");
            }
            Object customName = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : "<#ffffff>" + itemStack.getType().name();
            lore.add(ColorFormatter.format(this.tr("common_editor.item_list.item_amount", Map.of("amount", String.valueOf(currentAmount)))));
            lore.add("");
            lore.add(ColorFormatter.format(this.tr("common_editor.item_list.click_modify1")));
            lore.add(ColorFormatter.format(this.tr("common_editor.item_list.click_delete")));
            HexItemBuilder builder = HexItemBuilder.from(itemStack.clone()).amount(Math.max(1, Math.min(64, currentAmount))).name(ColorFormatter.format((String)customName)).lore(lore).flags(ItemFlag.values());
            this.gui.addItem(new GuiItem(builder.build(), event -> {
                if (event.getClick() == ClickType.SWAP_OFFHAND) {
                    this.isNavigating = true;
                    this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, finalStack.getType().name() + "<#ffd470>:", String.valueOf(currentAmount), input -> {
                        this.isNavigating = false;
                        if (input != null && !input.isEmpty()) {
                            try {
                                ArrayList<Object> currentList;
                                int newAmount = Integer.parseInt(input);
                                if (newAmount > 0 && index < (currentList = new ArrayList<Object>(this.questConfig.getList(this.yamlPath))).size()) {
                                    Map m;
                                    Object obj = currentList.get(index);
                                    if (obj instanceof ItemStack) {
                                        ItemStack is = (ItemStack)obj;
                                        is.setAmount(newAmount);
                                        currentList.set(index, is);
                                    } else if (obj instanceof Map && (m = (Map)obj).containsKey("==")) {
                                        ItemStack is = ItemStack.deserialize((Map)m);
                                        is.setAmount(newAmount);
                                        currentList.set(index, is);
                                    } else {
                                        currentList.set(index, finalStack.getType().name() + ":" + newAmount);
                                    }
                                    this.questConfig.set(this.yamlPath, currentList);
                                    this.saveConfig();
                                }
                            }
                            catch (Exception exception) {
                            }
                        }
                        this.open();
                    });
                } else if (event.getClick() == ClickType.DROP) {
                    this.isNavigating = true;
                    new QuestConfirmationGui(this.plugin, this.player, this.tr("common_editor.item_list.delete_confirm_title"), this.tr("common_editor.item_list.delete_confirm_msg", Map.of("index", String.valueOf(index + 1))), () -> {
                        ArrayList currentList = new ArrayList(this.questConfig.getList(this.yamlPath));
                        if (index < currentList.size()) {
                            currentList.remove(index);
                            this.questConfig.set(this.yamlPath, currentList);
                            this.saveConfig();
                        }
                        this.refresh();
                    }, this::open).open();
                } else if (event.isLeftClick() || event.isRightClick()) {
                    int delta;
                    int n = delta = event.isShiftClick() ? 10 : 1;
                    if (event.isRightClick()) {
                        delta = -delta;
                    }
                    int newAmount = Math.max(1, currentAmount + delta);
                    ArrayList<Object> currentList = new ArrayList<Object>(this.questConfig.getList(this.yamlPath));
                    if (index < currentList.size()) {
                        Map m;
                        Object obj = currentList.get(index);
                        if (obj instanceof ItemStack) {
                            ItemStack is = (ItemStack)obj;
                            is.setAmount(newAmount);
                            currentList.set(index, is);
                        } else if (obj instanceof Map && (m = (Map)obj).containsKey("==")) {
                            ItemStack is = ItemStack.deserialize((Map)m);
                            is.setAmount(newAmount);
                            currentList.set(index, is);
                        } else {
                            currentList.set(index, finalStack.getType().name() + ":" + newAmount);
                        }
                        this.questConfig.set(this.yamlPath, currentList);
                        this.saveConfig();
                    }
                    this.refresh();
                }
            }));
        }
    }
}
