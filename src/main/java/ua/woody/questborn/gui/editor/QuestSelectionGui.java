package ua.woody.questborn.gui.editor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
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
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestSelectionGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final QuestTypeConfig typeConfig;
    private final Set<String> selectedQuests;
    private final BiConsumer<String, Boolean> onQuestToggled;
    private final Runnable backAction;
    private PaginatedGui gui;
    private String searchQuery = null;
    private boolean isNavigating = false;

    public QuestSelectionGui(QuestbornPlugin plugin, Player player, QuestTypeConfig typeConfig, Set<String> selectedQuests, BiConsumer<String, Boolean> onQuestToggled, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.typeConfig = typeConfig;
        this.selectedQuests = selectedQuests;
        this.onQuestToggled = onQuestToggled;
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
        String title = ColorFormatter.applyColors(this.tr("common_editor.selection_menu.title"));
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(6)).pageSize(45).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            if (!this.isNavigating && !EditorSessionManager.isForceClosing) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, this.backAction);
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        this.setupButtons();
        this.populateQuests();
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }

    private void setupButtons() {
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            this.backAction.run();
        }));
        String searchLore = this.tr("common_editor.selection_menu.search_lore", Map.of("query", this.searchQuery == null ? this.tr("common_editor.selection_menu.search_all") : this.searchQuery));
        ItemStack searchItem = HexItemBuilder.from(Material.COMPASS).name(ColorFormatter.format(this.tr("common_editor.buttons.search"))).lore(ColorFormatter.format(searchLore), "", ColorFormatter.format(this.tr("common_editor.selection_menu.search_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 2, new GuiItem(searchItem, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInput(this.player, EditorChat.formatPrompt(this.tr("common_editor.selection_menu.prompt_search")), input -> {
                this.isNavigating = false;
                if (input != null) {
                    this.searchQuery = input.equalsIgnoreCase("clear") || input.isBlank() ? null : input.toLowerCase();
                }
                this.open();
            });
        }));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 4, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 6, new GuiItem(nextItem, event -> this.gui.next()));
    }

    private void populateQuests() {
        ArrayList<QuestDefinition> allQuests = new ArrayList<QuestDefinition>(this.typeConfig != null ? this.plugin.getQuestManager().getByType(this.typeConfig) : this.plugin.getQuestManager().getAll());
        allQuests.sort(Comparator.comparing(QuestDefinition::getId, String.CASE_INSENSITIVE_ORDER));
        for (QuestDefinition quest : allQuests) {
            String qId = quest.getId();
            String qName = ColorFormatter.stripColors(quest.getDisplayName());
            if (this.searchQuery != null && !qId.toLowerCase().contains(this.searchQuery) && !qName.toLowerCase().contains(this.searchQuery)) continue;
            boolean isSelected = this.selectedQuests.contains(qId);
            Material mat = quest.getIconMaterial() != null ? quest.getIconMaterial() : Material.PAPER;
            HexItemBuilder builder = HexItemBuilder.from(mat).name(ColorFormatter.format("<#ffd470>" + quest.getDisplayName())).lore(ColorFormatter.format(this.tr("common_editor.selection_menu.item_id", Map.of("id", qId))), ColorFormatter.format(this.tr("common_editor.selection_menu.item_status", Map.of("status", isSelected ? this.tr("common_editor.states.enabled") : this.tr("common_editor.states.disabled")))), "", ColorFormatter.format(this.tr("common_editor.selection_menu.item_click"))).flags(ItemFlag.HIDE_ATTRIBUTES);
            if (isSelected) {
                builder.enchant(Enchantment.DURABILITY);
            }
            ItemStack item = builder.build();
            this.gui.addItem(new GuiItem(item, event -> {
                boolean newSelectedState;
                boolean bl = newSelectedState = !isSelected;
                if (newSelectedState) {
                    this.selectedQuests.add(qId);
                } else {
                    this.selectedQuests.remove(qId);
                }
                if (this.onQuestToggled != null) {
                    this.onQuestToggled.accept(qId, newSelectedState);
                }
                this.open();
            }));
        }
    }
}
