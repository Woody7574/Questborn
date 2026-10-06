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
import dev.triumphteam.gui.builder.gui.SimpleBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestAddObjectivePropertyGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final Consumer<String> onPropertySelected;
    private final Runnable backAction;

    public QuestAddObjectivePropertyGui(QuestbornPlugin plugin, Player player, Consumer<String> onPropertySelected, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.onPropertySelected = onPropertySelected;
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
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        Gui gui = ((SimpleBuilder)((SimpleBuilder)((SimpleBuilder)Gui.gui().title(ColorFormatter.formatComponent(EditorUtils.truncateGuiTitle(this.tr("quest_editor.add_property.title"))))).rows(5)).disableAllInteractions()).create();
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.getFiller().fill(new GuiItem(bg));
        this.addPropertyIcon(gui, 10, Material.EXPERIENCE_BOTTLE, "amount", this.tr("quest_editor.add_property.amount"));
        this.addPropertyIcon(gui, 11, Material.DIAMOND_PICKAXE, "target-materials", this.tr("quest_editor.add_property.materials"));
        this.addPropertyIcon(gui, 12, Material.ZOMBIE_HEAD, "target-entities", this.tr("quest_editor.add_property.entities"));
        this.addPropertyIcon(gui, 13, Material.GRASS_BLOCK, "world", this.tr("quest_editor.add_property.world"));
        this.addPropertyIcon(gui, 14, Material.OAK_SAPLING, "biome", this.tr("quest_editor.add_property.biome"));
        this.addPropertyIcon(gui, 15, Material.MAP, "region", this.tr("quest_editor.add_property.region"));
        this.addPropertyIcon(gui, 16, Material.LEATHER_BOOTS, "distance", this.tr("quest_editor.add_property.distance"));
        this.addPropertyIcon(gui, 19, Material.PAPER, "message", this.tr("quest_editor.add_property.message"));
        this.addPropertyIcon(gui, 20, Material.COMMAND_BLOCK, "command", this.tr("quest_editor.add_property.command"));
        this.addPropertyIcon(gui, 21, Material.SLIME_BALL, "chance", this.tr("quest_editor.add_property.chance"));
        ItemStack customItem = HexItemBuilder.from(Material.NAME_TAG).name(ColorFormatter.format(this.tr("quest_editor.add_property.custom_title"))).lore(ColorFormatter.format(this.tr("quest_editor.add_property.custom_lore1")), "", ColorFormatter.format(this.tr("quest_editor.add_property.custom_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(25, new GuiItem(customItem, event -> this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_editor.add_property.custom_prompt")), "", key -> {
            if (key != null && !key.isEmpty()) {
                this.onPropertySelected.accept((String)key);
            } else {
                this.open();
            }
        })));
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(40, new GuiItem(backItem, event -> this.backAction.run()));
        gui.open((HumanEntity)this.player);
    }

    private void addPropertyIcon(Gui gui, int slot, Material mat, String key, String name) {
        ItemStack item = HexItemBuilder.from(mat).name(ColorFormatter.format(this.tr("quest_editor.add_property.item_title", Map.of("name", name)))).lore(ColorFormatter.format(this.tr("quest_editor.add_property.item_lore", Map.of("key", key))), "", ColorFormatter.format(this.tr("quest_editor.add_property.item_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(slot, new GuiItem(item, event -> this.onPropertySelected.accept(key)));
    }
}
