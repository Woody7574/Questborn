package ua.woody.questborn.gui.editor;

import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.utils.HexItemBuilder;

public class NpcVoiceSelectorGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String currentVoice;
    private final Consumer<String> callback;
    private final Runnable goBack;
    private PaginatedGui gui;

    public NpcVoiceSelectorGui(QuestbornPlugin plugin, Player player, String currentVoice, Consumer<String> callback, Runnable goBack) {
        this.plugin = plugin;
        this.player = player;
        this.currentVoice = currentVoice;
        this.callback = callback;
        this.goBack = goBack;
    }

    private String tr(String path) {
        if (this.plugin != null && this.plugin.getLanguage() != null) {
            return this.plugin.getLanguage().trEditor(path);
        }
        return path;
    }

    private String tr(String path, Map<String, String> map) {
        if (this.plugin != null && this.plugin.getLanguage() != null) {
            return this.plugin.getLanguage().trEditor(path, map);
        }
        return path;
    }

    public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        String title = ColorFormatter.applyColors(this.tr("quest_editor.npc_voice.title"));
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(6)).pageSize(45).disableAllInteractions()).create();
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> this.goBack.run()));
        ItemStack customItem = HexItemBuilder.from(Material.COMMAND_BLOCK).name(ColorFormatter.format(this.tr("quest_editor.npc_voice.custom_title"))).lore(ColorFormatter.format(this.tr("quest_editor.npc_voice.custom_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 5, new GuiItem(customItem, event -> this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_editor.npc_voice.custom_prompt")), "entity.villager.trade", input -> {
            if (input != null && !input.isBlank()) {
                this.callback.accept(input.trim());
            } else {
                this.open();
            }
        })));
        this.populateVoices();
        this.gui.open((HumanEntity)this.player);
    }

    private void populateVoices() {
        String[] presets;
        for (String preset : presets = new String[]{"villager", "witch", "wandering_trader", "player", "cat", "wolf", "none"}) {
            boolean isCurrent = preset.equalsIgnoreCase(this.currentVoice);
            String nameCol = isCurrent ? "<#a7ff99>" : "<#ffd470>";
            Material mat = Material.JUKEBOX;
            if (preset.equals("none")) {
                mat = Material.BARRIER;
            } else if (preset.equals("villager")) {
                mat = Material.EMERALD;
            } else if (preset.equals("witch")) {
                mat = Material.GLASS_BOTTLE;
            } else if (preset.equals("cat")) {
                mat = Material.COD;
            } else if (preset.equals("wolf")) {
                mat = Material.BONE;
            } else if (preset.equals("player")) {
                mat = Material.PLAYER_HEAD;
            }
            HexItemBuilder builder = HexItemBuilder.from(mat).name(ColorFormatter.format(nameCol + preset)).lore(isCurrent ? ColorFormatter.format("<#a7ff99>\u25a0 " + this.tr("quest_editor.npc_voice.selected")) : "", "", ColorFormatter.format(this.tr("quest_editor.npc_voice.select_click"))).flags(ItemFlag.HIDE_ATTRIBUTES);
            if (isCurrent) {
                builder.glow(true);
            }
            this.gui.addItem(new GuiItem(builder.build(), event -> this.callback.accept(preset)));
        }
    }
}
