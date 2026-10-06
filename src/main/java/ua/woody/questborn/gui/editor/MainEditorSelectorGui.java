package ua.woody.questborn.gui.editor;

import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.GuiUtils;
import ua.woody.questborn.gui.editor.DialogueEditorListGui;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.NpcEditorListGui;
import ua.woody.questborn.gui.editor.QuestEditorListGui;
import ua.woody.questborn.gui.editor.QuestTypeEditorListGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.SimpleBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import ua.woody.questborn.utils.HexItemBuilder;

public class MainEditorSelectorGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private Gui gui;

    public MainEditorSelectorGui(QuestbornPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
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
        String title = ColorFormatter.applyColors(this.tr("common_editor.main_selector.title"));
        this.gui = ((SimpleBuilder)((SimpleBuilder)((SimpleBuilder)Gui.gui().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(3)).disableAllInteractions()).create();
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fill(new GuiItem(bg));
        ItemStack questEdItem = HexItemBuilder.from(Material.PLAYER_HEAD).name(ColorFormatter.format(this.tr("common_editor.main_selector.quest_title"))).lore(this.plugin.getLanguage().trEditorList("common_editor.main_selector.quest_lore"), "", ColorFormatter.format(this.tr("common_editor.main_selector.quest_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        GuiUtils.applyBaseHead(questEdItem, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWNmNmNjZDEyMDE3YmIxYzJjZTY0NTA2YzgwYjE5YWY3MjFjZGNjYjU5ZDUxODEzYjE2NjRmMzRmNTE4MjY5MyJ9fX0=");
        this.gui.setItem(2, 2, new GuiItem(questEdItem, event -> new QuestEditorListGui(this.plugin, this.player).open()));
        ItemStack typeEdItem = HexItemBuilder.from(Material.PLAYER_HEAD).name(ColorFormatter.format(this.tr("common_editor.main_selector.type_title"))).lore(this.plugin.getLanguage().trEditorList("common_editor.main_selector.type_lore"), "", ColorFormatter.format(this.tr("common_editor.main_selector.type_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        GuiUtils.applyBaseHead(typeEdItem, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2Y2YmY5NThhYmQ3ODI5NWVlZDZmZmMyOTNiMWFhNTk1MjZlODBmNTQ5NzY4MjllYTA2ODMzN2MyZjVlOCJ9fX0=");
        this.gui.setItem(2, 4, new GuiItem(typeEdItem, event -> new QuestTypeEditorListGui(this.plugin, this.player).open()));
        ItemStack npcEdItem = HexItemBuilder.from(Material.PLAYER_HEAD).name(ColorFormatter.format(this.tr("common_editor.main_selector.npc_title"))).lore(this.plugin.getLanguage().trEditorList("common_editor.main_selector.npc_lore"), "", ColorFormatter.format(this.tr("common_editor.main_selector.npc_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        GuiUtils.applyBaseHead(npcEdItem, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzdmODBiMjZjODhmMWRlNTgxNmEzOWIzMzEwNjIzYjRlZTljZWE3ZmYxZmQ5ZjhkODY3Zjg1MTE5NGE3ZjY1NCJ9fX0=");
        this.gui.setItem(2, 6, new GuiItem(npcEdItem, event -> new NpcEditorListGui(this.plugin, this.player).open()));
        ItemStack dialogueEdItem = HexItemBuilder.from(Material.PLAYER_HEAD).name(ColorFormatter.format(this.tr("common_editor.main_selector.dialogue_title"))).lore(this.plugin.getLanguage().trEditorList("common_editor.main_selector.dialogue_lore"), "", ColorFormatter.format(this.tr("common_editor.main_selector.dialogue_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        GuiUtils.applyBaseHead(dialogueEdItem, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGE2YTdkMWI3NjI5MTkzOTQyZjhlMTQ2YzZkMWQyZGIxOTFkMjdmODExZDIxZTI5YTJlNGNmYmFiZGEwODgifX19");
        this.gui.setItem(2, 8, new GuiItem(dialogueEdItem, event -> new DialogueEditorListGui(this.plugin, this.player).open()));
        this.gui.open((HumanEntity)this.player);
    }
}
