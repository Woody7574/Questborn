package ua.woody.questborn.gui.editor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
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

public class QuestPotionTypeSelectorGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final Consumer<PotionEffectType> onSelect;
    private final Runnable backAction;
    private final List<String> ignoredTypes;
    private PaginatedGui gui;
    private String searchQuery = null;

    public QuestPotionTypeSelectorGui(QuestbornPlugin plugin, Player player, List<String> ignoredTypes, Consumer<PotionEffectType> onSelect, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.ignoredTypes = ignoredTypes;
        this.onSelect = onSelect;
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
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors("&0" + this.tr("quest_editor.eff_reward.selector_title")))))).rows(6)).pageSize(45).disableAllInteractions()).create();
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        this.setupButtons();
        this.populateTypes();
        this.gui.open((HumanEntity)this.player);
    }

    private void setupButtons() {
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> this.backAction.run()));
        String searchLore = this.tr("common_editor.selection_menu.search_lore", Map.of("query", this.searchQuery == null ? this.tr("common_editor.selection_menu.search_all") : this.searchQuery));
        ItemStack searchItem = HexItemBuilder.from(Material.COMPASS).name(ColorFormatter.format(this.tr("common_editor.buttons.search"))).lore(ColorFormatter.format(searchLore), "", ColorFormatter.format(this.tr("common_editor.selection_menu.search_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 2, new GuiItem(searchItem, event -> this.plugin.getChatInputManager().requestInput(this.player, EditorChat.formatPrompt(this.tr("common_editor.selection_menu.prompt_search")), input -> {
            if (input != null) {
                this.searchQuery = input.equalsIgnoreCase("clear") || input.isBlank() ? null : input.toLowerCase();
            }
            this.open();
        })));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 4, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 6, new GuiItem(nextItem, event -> this.gui.next()));
    }

    private void populateTypes() {
        ArrayList<PotionEffectType> types = new ArrayList<PotionEffectType>();
        for (PotionEffectType type : PotionEffectType.values()) {
            if (type == null) continue;
            types.add(type);
        }
        types.sort(Comparator.comparing(PotionEffectType::getName));
        for (PotionEffectType type : types) {
            String name = type.getName().toUpperCase(Locale.ROOT);
            if (name.startsWith("MINECRAFT:")) {
                name = name.substring(10);
            }
            if (this.ignoredTypes != null && this.ignoredTypes.contains(name) || this.searchQuery != null && !name.toLowerCase().contains(this.searchQuery)) continue;
            String descKey = "quest_editor.eff_reward.descriptions." + name;
            String descRaw = this.tr(descKey);
            ArrayList<String> loreList = new ArrayList<String>();
            if (!descRaw.equals(descKey)) {
                loreList.add(ColorFormatter.format(descRaw));
                loreList.add("");
            }
            loreList.add(ColorFormatter.format(this.tr("common_editor.selection_menu.item_click")));
            ItemStack item = HexItemBuilder.from(Material.POTION).name(ColorFormatter.format("&a" + name)).lore(loreList).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            try {
                PotionMeta meta = (PotionMeta)item.getItemMeta();
                if (meta != null) {
                    meta.setColor(type.getColor());
                    item.setItemMeta((ItemMeta)meta);
                }
            }
            catch (Exception exception) {
            }
            this.gui.addItem(new GuiItem(item, event -> this.onSelect.accept(type)));
        }
    }
}
