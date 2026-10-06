package ua.woody.questborn.gui.editor;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.SimpleBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import ua.woody.questborn.utils.HexItemBuilder;

public class NpcLinkedItemConfigGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final ConfigurationSection sec;
    private final String itemId;
    private final boolean isQuests;
    private final Runnable onDirty;
    private final Runnable backAction;
    private boolean isNavigating = false;

    public NpcLinkedItemConfigGui(QuestbornPlugin plugin, Player player, ConfigurationSection sec, String itemId, boolean isQuests, Runnable onDirty, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.sec = sec;
        this.itemId = itemId;
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

    public void open() {
        Gui gui = ((SimpleBuilder)((SimpleBuilder)((SimpleBuilder)Gui.gui().title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors(this.tr("quest_editor.npc_main.linked_config_title") + ": " + this.itemId))))).rows(3)).disableAllInteractions()).create();
        gui.setCloseGuiAction(event -> {
            if (!this.isNavigating) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, this.backAction);
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.getFiller().fill(new GuiItem(bg));
        List startsList = this.sec.getStringList(this.isQuests ? "starts.quests" : "starts.types");
        List finishesList = this.sec.getStringList(this.isQuests ? "finishes.quests" : "finishes.types");
        boolean isStarts = startsList.contains(this.itemId);
        boolean isFinishes = finishesList.contains(this.itemId);
        ItemStack startsItem = HexItemBuilder.from(isStarts ? Material.LIME_DYE : Material.GRAY_DYE).name(ColorFormatter.format(this.tr("quest_editor.npc_main.linked_config_starts"))).lore(ColorFormatter.format(this.tr("quest_editor.npc_main.linked_config_starts_lore")), "", ColorFormatter.format(isStarts ? this.tr("common_editor.states.enabled") : this.tr("common_editor.states.disabled"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(2, 4, new GuiItem(startsItem, event -> {
            if (isStarts && !isFinishes) {
                EditorChat.sendError(this.player, this.tr("quest_editor.npc_main.linked_config_error_last"));
                return;
            }
            if (isStarts) {
                startsList.remove(this.itemId);
            } else {
                startsList.add(this.itemId);
            }
            this.sec.set(this.isQuests ? "starts.quests" : "starts.types", (Object)(startsList.isEmpty() ? null : startsList));
            this.onDirty.run();
            this.open();
        }));
        ItemStack finishesItem = HexItemBuilder.from(isFinishes ? Material.LIME_DYE : Material.GRAY_DYE).name(ColorFormatter.format(this.tr("quest_editor.npc_main.linked_config_finishes"))).lore(ColorFormatter.format(this.tr("quest_editor.npc_main.linked_config_finishes_lore")), "", ColorFormatter.format(isFinishes ? this.tr("common_editor.states.enabled") : this.tr("common_editor.states.disabled"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(2, 6, new GuiItem(finishesItem, event -> {
            if (isFinishes && !isStarts) {
                EditorChat.sendError(this.player, this.tr("quest_editor.npc_main.linked_config_error_last"));
                return;
            }
            if (isFinishes) {
                finishesList.remove(this.itemId);
            } else {
                finishesList.add(this.itemId);
            }
            this.sec.set(this.isQuests ? "finishes.quests" : "finishes.types", (Object)(finishesList.isEmpty() ? null : finishesList));
            this.onDirty.run();
            this.open();
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
