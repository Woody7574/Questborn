package ua.woody.questborn.gui.editor;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.NpcLinkedItemsGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.SimpleBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import ua.woody.questborn.utils.HexItemBuilder;

public class NpcLinkedSelectionGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final ConfigurationSection sec;
    private final Runnable onDirty;
    private final Runnable backAction;
    private boolean isNavigating = false;

    public NpcLinkedSelectionGui(QuestbornPlugin plugin, Player player, ConfigurationSection sec, Runnable onDirty, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.sec = sec;
        this.onDirty = onDirty;
        this.backAction = backAction;
    }

    private String tr(String path) {
        if (this.plugin != null && this.plugin.getLanguage() != null) {
            return this.plugin.getLanguage().trEditor(path);
        }
        return path;
    }

    public void open() {
        Gui gui = ((SimpleBuilder)((SimpleBuilder)((SimpleBuilder)Gui.gui().title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors(this.tr("quest_editor.npc_main.linked_selection_gui_title")))))).rows(3)).disableAllInteractions()).create();
        gui.setCloseGuiAction(event -> {
            if (!this.isNavigating) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, this.backAction);
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.getFiller().fill(new GuiItem(bg));
        boolean hasQuests = !this.sec.getStringList("starts.quests").isEmpty() || !this.sec.getStringList("finishes.quests").isEmpty();
        boolean hasTypes = !this.sec.getStringList("starts.types").isEmpty() || !this.sec.getStringList("finishes.types").isEmpty();
        ItemStack questsItem = hasTypes ? HexItemBuilder.from(Material.BARRIER).name(ColorFormatter.format(this.tr("quest_editor.npc_main.linked_quests_title"))).lore(ColorFormatter.format(this.tr("quest_editor.npc_main.linked_blocked_by_types"))).flags(ItemFlag.HIDE_ATTRIBUTES).build() : HexItemBuilder.from(Material.WRITTEN_BOOK).name(ColorFormatter.format(this.tr("quest_editor.npc_main.linked_quests_title"))).lore(ColorFormatter.format(this.tr("quest_editor.npc_main.linked_quests_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(2, 4, new GuiItem(questsItem, event -> {
            if (hasTypes) {
                return;
            }
            this.isNavigating = true;
            new NpcLinkedItemsGui(this.plugin, this.player, this.sec, true, this.onDirty, this::open).open();
        }));
        ItemStack typesItem = hasQuests ? HexItemBuilder.from(Material.BARRIER).name(ColorFormatter.format(this.tr("quest_editor.npc_main.linked_types_title"))).lore(ColorFormatter.format(this.tr("quest_editor.npc_main.linked_blocked_by_quests"))).flags(ItemFlag.HIDE_ATTRIBUTES).build() : HexItemBuilder.from(Material.BOOKSHELF).name(ColorFormatter.format(this.tr("quest_editor.npc_main.linked_types_title"))).lore(ColorFormatter.format(this.tr("quest_editor.npc_main.linked_types_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(2, 6, new GuiItem(typesItem, event -> {
            if (hasQuests) {
                return;
            }
            this.isNavigating = true;
            new NpcLinkedItemsGui(this.plugin, this.player, this.sec, false, this.onDirty, this::open).open();
        }));
        ItemStack backBtn = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(3, 1, new GuiItem(backBtn, event -> {
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
