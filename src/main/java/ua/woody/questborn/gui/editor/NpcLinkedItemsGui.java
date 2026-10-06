package ua.woody.questborn.gui.editor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.NpcLinkedItemConfigGui;
import ua.woody.questborn.gui.editor.QuestConfirmationGui;
import ua.woody.questborn.gui.editor.QuestSelectionGui;
import ua.woody.questborn.gui.editor.QuestTypeSelectorGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.utils.HexItemBuilder;

public class NpcLinkedItemsGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final ConfigurationSection sec;
    private final boolean isQuests;
    private final Runnable onDirty;
    private final Runnable backAction;
    private boolean isNavigating = false;

    public NpcLinkedItemsGui(QuestbornPlugin plugin, Player player, ConfigurationSection sec, boolean isQuests, Runnable onDirty, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.sec = sec;
        this.isQuests = isQuests;
        this.onDirty = onDirty;
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

    public void open() {
        String titleKey = this.isQuests ? "quest_editor.npc_main.linked_quests_list_title" : "quest_editor.npc_main.linked_types_list_title";
        PaginatedGui gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors(this.tr(titleKey)))))).rows(6)).pageSize(45).disableAllInteractions()).create();
        gui.setCloseGuiAction(event -> {
            if (!this.isNavigating) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, this.backAction);
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.getFiller().fillBottom(new GuiItem(bg));
        HashSet uniqueItems = new HashSet();
        List startsList = this.sec.getStringList(this.isQuests ? "starts.quests" : "starts.types");
        List finishesList = this.sec.getStringList(this.isQuests ? "finishes.quests" : "finishes.types");
        uniqueItems.addAll(startsList);
        uniqueItems.addAll(finishesList);
        ArrayList<String> sortedItems = new ArrayList(uniqueItems);
        sortedItems.sort(String::compareTo);
        for (String itemId : sortedItems) {
            Material iconMat = Material.ANVIL;
            if (this.isQuests) {
                QuestDefinition q = this.plugin.getQuestManager().getQuest(itemId);
                if (q != null && q.getIconMaterial() != null) {
                    iconMat = q.getIconMaterial();
                }
            } else {
                QuestTypeConfig qt = this.plugin.getQuestManager().getQuestTypeManager().getType(itemId);
                if (qt != null && qt.getGuiMaterial() != null) {
                    iconMat = qt.getGuiMaterial();
                }
            }
            ItemStack item = HexItemBuilder.from(iconMat).name(ColorFormatter.format(this.isQuests ? this.tr("quest_editor.npc_main.linked_item_quest_name", Map.of("id", itemId)) : this.tr("quest_editor.npc_main.linked_item_type_name", Map.of("id", itemId)))).lore(ColorFormatter.format(this.tr("quest_editor.npc_main.linked_item_click")), ColorFormatter.format(this.tr("quest_editor.npc_main.linked_item_drop"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            gui.addItem(new GuiItem(item, event -> {
                if (event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) {
                    this.isNavigating = true;
                    new QuestConfirmationGui(this.plugin, this.player, this.tr("quest_editor.npc_main.linked_delete_title"), this.tr("quest_editor.npc_main.linked_delete_item"), this.tr("quest_editor.npc_main.linked_delete_desc", Map.of("id", itemId)), Material.DAMAGED_ANVIL, () -> {
                        List starts = this.sec.getStringList(this.isQuests ? "starts.quests" : "starts.types");
                        List finishes = this.sec.getStringList(this.isQuests ? "finishes.quests" : "finishes.types");
                        starts.remove(itemId);
                        finishes.remove(itemId);
                        this.sec.set(this.isQuests ? "starts.quests" : "starts.types", (Object)(starts.isEmpty() ? null : starts));
                        this.sec.set(this.isQuests ? "finishes.quests" : "finishes.types", (Object)(finishes.isEmpty() ? null : finishes));
                        this.onDirty.run();
                        this.open();
                    }, this::open).open();
                } else {
                    this.isNavigating = true;
                    new NpcLinkedItemConfigGui(this.plugin, this.player, this.sec, itemId, this.isQuests, this.onDirty, this::open).open();
                }
            }));
        }
        ItemStack addBtn = HexItemBuilder.from(Material.EMERALD_BLOCK).name(ColorFormatter.format(this.tr("common_editor.buttons.add"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(6, 5, new GuiItem(addBtn, event -> {
            this.isNavigating = true;
            if (this.isQuests) {
                new QuestSelectionGui(this.plugin, this.player, null, uniqueItems, (selectedQuest, isAdding) -> {
                    List starts = this.sec.getStringList("starts.quests");
                    List finishes = this.sec.getStringList("finishes.quests");
                    if (isAdding.booleanValue()) {
                        if (!starts.contains(selectedQuest) && !finishes.contains(selectedQuest)) {
                            starts.add(selectedQuest);
                            this.sec.set("starts.quests", (Object)starts);
                            this.onDirty.run();
                        }
                    } else {
                        starts.remove(selectedQuest);
                        finishes.remove(selectedQuest);
                        this.sec.set("starts.quests", (Object)(starts.isEmpty() ? null : starts));
                        this.sec.set("finishes.quests", (Object)(finishes.isEmpty() ? null : finishes));
                        this.onDirty.run();
                    }
                }, this::open).open();
            } else {
                new QuestTypeSelectorGui(this.plugin, this.player, null, null, null, selectedType -> {
                    if (selectedType != null) {
                        List starts = this.sec.getStringList("starts.types");
                        List finishes = this.sec.getStringList("finishes.types");
                        if (!starts.contains(selectedType) && !finishes.contains(selectedType)) {
                            starts.add(selectedType);
                            this.sec.set("starts.types", (Object)starts);
                            this.onDirty.run();
                        }
                        new NpcLinkedItemConfigGui(this.plugin, this.player, this.sec, (String)selectedType, false, this.onDirty, this::open).open();
                    } else {
                        this.open();
                    }
                }, this::open).open();
            }
        }));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(6, 4, new GuiItem(prevItem, event -> gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(6, 6, new GuiItem(nextItem, event -> gui.next()));
        ItemStack backBtn = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(6, 1, new GuiItem(backBtn, event -> {
            this.isNavigating = true;
            this.backAction.run();
        }));
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }
}
