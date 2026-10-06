package ua.woody.questborn.gui.editor;

import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.SimpleBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestCommandSenderGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final Runnable backAction;
    private final Consumer<String> onSelect;
    private Gui gui;
    private boolean isNavigating = false;

    public QuestCommandSenderGui(QuestbornPlugin plugin, Player player, Runnable backAction, Consumer<String> onSelect) {
        this.plugin = plugin;
        this.player = player;
        this.backAction = backAction;
        this.onSelect = onSelect;
    }

    private String tr(String path) {
        if (this.plugin != null && this.plugin.getLanguage() != null) {
            return this.plugin.getLanguage().trEditor(path);
        }
        return path;
    }

    public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        this.gui = ((SimpleBuilder)((SimpleBuilder)((SimpleBuilder)Gui.gui().title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors("&0" + this.tr("quest_editor.cmd_reward.sender_gui_title")))))).rows(3)).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            if (!this.isNavigating && !EditorSessionManager.isForceClosing) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> this.backAction.run());
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fill(new GuiItem(bg));
        ItemStack playerItem = HexItemBuilder.from(Material.PLAYER_HEAD).name(ColorFormatter.format(this.tr("quest_editor.cmd_reward.sender_player"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.cmd_reward.sender_player_lore")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(11, new GuiItem(playerItem, event -> {
            this.isNavigating = true;
            this.onSelect.accept("[player] ");
        }));
        ItemStack silentPlayerItem = HexItemBuilder.from(Material.CREEPER_HEAD).name(ColorFormatter.format(this.tr("quest_editor.cmd_reward.sender_silent_player"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.cmd_reward.sender_silent_player_lore")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(12, new GuiItem(silentPlayerItem, event -> {
            this.isNavigating = true;
            this.onSelect.accept("[silent_player] ");
        }));
        ItemStack consoleItem = HexItemBuilder.from(Material.COMMAND_BLOCK).name(ColorFormatter.format(this.tr("quest_editor.cmd_reward.sender_console"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.cmd_reward.sender_console_lore")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(13, new GuiItem(consoleItem, event -> {
            this.isNavigating = true;
            this.onSelect.accept("[console] ");
        }));
        ItemStack silentConsoleItem = HexItemBuilder.from(Material.REPEATING_COMMAND_BLOCK).name(ColorFormatter.format(this.tr("quest_editor.cmd_reward.sender_silent_console"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.cmd_reward.sender_silent_console_lore")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(14, new GuiItem(silentConsoleItem, event -> {
            this.isNavigating = true;
            this.onSelect.accept("[silent_console] ");
        }));
        ItemStack noneItem = HexItemBuilder.from(Material.BARRIER).name(ColorFormatter.format(this.tr("quest_editor.cmd_reward.sender_none"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.cmd_reward.sender_none_lore")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(16, new GuiItem(noneItem, event -> {
            this.isNavigating = true;
            this.onSelect.accept("");
        }));
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(3, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            this.backAction.run();
        }));
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }
}
