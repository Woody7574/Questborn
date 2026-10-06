package ua.woody.questborn.gui.editor;

import java.util.HashMap;
import java.util.Map;
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
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestRotationPoolGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final QuestTypeConfig typeConfig;
    private final Runnable backAction;
    private final Consumer<String> onSelect;
    private final Runnable onCloseAction;
    private PaginatedGui gui;
    private boolean isNavigating = false;

    public QuestRotationPoolGui(QuestbornPlugin plugin, Player player, QuestTypeConfig typeConfig, Runnable backAction, Consumer<String> onSelect, Runnable onCloseAction) {
        this.plugin = plugin;
        this.player = player;
        this.typeConfig = typeConfig;
        this.backAction = backAction;
        this.onSelect = onSelect;
        this.onCloseAction = onCloseAction;
    }

    public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors("&0" + this.tr("quest_editor.rot_pool_gui.title")))))).rows(6)).pageSize(45).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            Runnable closeAction;
            if (!this.isNavigating && !EditorSessionManager.isForceClosing && (closeAction = EditorSessionManager.getSession(this.player.getUniqueId())) != null) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, closeAction);
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(" ").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            this.backAction.run();
        }));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 3, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 7, new GuiItem(nextItem, event -> this.gui.next()));
        Map<String, Integer> pools = this.typeConfig.getRotationPools();
        if (pools == null || pools.isEmpty()) {
            ItemStack emptyItem = HexItemBuilder.from(Material.STRUCTURE_VOID).name(ColorFormatter.format(this.tr("common_editor.item_list.empty_title"))).lore(this.plugin.getLanguage().trEditorList("common_editor.item_list.empty_lore")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(3, 5, new GuiItem(emptyItem));
        } else {
            Material[] shulkerColors = new Material[]{Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX, Material.LIME_SHULKER_BOX, Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX, Material.CYAN_SHULKER_BOX, Material.PURPLE_SHULKER_BOX, Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX, Material.GREEN_SHULKER_BOX, Material.RED_SHULKER_BOX, Material.BLACK_SHULKER_BOX};
            for (String pool : pools.keySet()) {
                int colorIndex = Math.abs(pool.hashCode()) % shulkerColors.length;
                Material iconMat = shulkerColors[colorIndex];
                long count = this.plugin.getQuestManager().getAll().stream().filter(q -> q.getTypeId().equalsIgnoreCase(this.typeConfig.getId())).filter(q -> q.getRotationPool() != null && q.getRotationPool().equalsIgnoreCase(pool)).count();
                ItemStack item = HexItemBuilder.from(iconMat).name(ColorFormatter.format(this.tr("quest_editor.rot_pool_gui.pool_item", Map.of("pool", pool)))).lore(ColorFormatter.format(this.tr("quest_editor.rot_pool_gui.pool_lore", Map.of("count", String.valueOf(count)))), "", ColorFormatter.format(this.tr("quest_editor.rot_pool_gui.pool_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
                this.gui.addItem(new GuiItem(item, event -> {
                    this.isNavigating = true;
                    this.onSelect.accept(pool);
                }));
            }
        }
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }

    private String tr(String key) {
        return this.plugin.getLanguageManager().trEditor(key, new HashMap<String, String>());
    }

    private String tr(String key, Map<String, String> placeholders) {
        return this.plugin.getLanguageManager().trEditor(key, placeholders);
    }
}
