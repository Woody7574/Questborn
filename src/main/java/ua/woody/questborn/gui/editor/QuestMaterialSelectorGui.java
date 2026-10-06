package ua.woody.questborn.gui.editor;

import java.util.Map;
import java.util.function.BiConsumer;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.GuiUtils;
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

public class QuestMaterialSelectorGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final BiConsumer<String, String> onSelect;
    private final Runnable backAction;
    private PaginatedGui gui;
    private String searchQuery = null;
    private String categoryFilter = "ALL";
    private boolean isNavigating = false;

    public QuestMaterialSelectorGui(QuestbornPlugin plugin, Player player, BiConsumer<String, String> onSelect, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
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
        String title = ColorFormatter.applyColors(this.tr("quest_type_editor.material_selector.title"));
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(6)).pageSize(45).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            if (!this.isNavigating && !EditorSessionManager.isForceClosing && this.backAction != null) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
                    this.isNavigating = true;
                    this.backAction.run();
                });
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        this.setupButtons();
        this.populateMaterials();
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
        ItemStack chatItem = HexItemBuilder.from(Material.NAME_TAG).name(ColorFormatter.format(this.tr("quest_type_editor.material_selector.input_chat"))).lore(ColorFormatter.format(this.tr("quest_type_editor.material_selector.input_chat_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 2, new GuiItem(chatItem, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_type_editor.material_selector.input_chat_prompt")), "DIAMOND", input -> {
                if (input != null && !input.isBlank()) {
                    String mat = input.trim();
                    if (EditorUtils.isValidMaterial(mat, this.plugin)) {
                        this.isNavigating = true;
                        this.onSelect.accept(mat, null);
                    } else {
                        this.player.sendMessage(ColorFormatter.applyColors(this.tr("quest_type_editor.material_selector.error_invalid")));
                        this.open();
                    }
                } else {
                    this.open();
                }
            });
        }));
        ItemStack handItem = HexItemBuilder.from(Material.HOPPER).name(ColorFormatter.format(this.tr("quest_type_editor.material_selector.hand"))).lore(ColorFormatter.format(this.tr("quest_type_editor.material_selector.hand_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 3, new GuiItem(handItem, event -> {
            ItemStack inHand = this.player.getInventory().getItemInMainHand();
            if (inHand != null && inHand.getType() != Material.AIR) {
                this.isNavigating = true;
                this.onSelect.accept(EditorUtils.getCustomIdFromItem(inHand, this.plugin), GuiUtils.extractBaseHead(inHand));
            } else {
                EditorChat.sendError(this.player, this.tr("quest_type_editor.material_selector.hand_error"));
                this.open();
            }
        }));
        String searchLore = this.searchQuery == null ? this.tr("common_editor.search.none") : this.tr("common_editor.search.query", Map.of("query", this.searchQuery));
        ItemStack searchItem = HexItemBuilder.from(Material.COMPASS).name(ColorFormatter.format(this.tr("quest_type_editor.material_selector.search"))).lore(ColorFormatter.format(searchLore), "", ColorFormatter.format(this.tr("common_editor.search.click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 8, new GuiItem(searchItem, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_type_editor.material_selector.search_prompt")), this.searchQuery == null ? "" : this.searchQuery, input -> {
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

    private void populateMaterials() {
        for (Material mat : Material.values()) {
            if (mat.isAir() || mat.name().startsWith("LEGACY_") || !mat.isItem()) continue;
            String name = mat.name();
            if (this.searchQuery != null && !name.toLowerCase().contains(this.searchQuery)) continue;
            ItemStack item = HexItemBuilder.from(mat).name(ColorFormatter.format("<#ffd470>" + name)).lore("", ColorFormatter.format(this.tr("quest_type_editor.material_selector.item_click"))).flags(ItemFlag.values()).build();
            this.gui.addItem(new GuiItem(item, event -> {
                this.isNavigating = true;
                this.onSelect.accept(name, null);
            }));
        }
    }
}
