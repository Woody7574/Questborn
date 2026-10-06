package ua.woody.questborn.gui.editor;

import java.util.ArrayList;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.SimpleBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestConfirmationGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String titleText;
    private final String itemNameText;
    private final String detailText;
    private final Material displayMaterial;
    private final Runnable onConfirm;
    private final Runnable onCancel;
    private boolean isNavigating = false;

    public QuestConfirmationGui(QuestbornPlugin plugin, Player player, String titleText, String detailText, Runnable onConfirm, Runnable onCancel) {
        this(plugin, player, titleText, titleText, detailText, Material.DAMAGED_ANVIL, onConfirm, onCancel);
    }

    public QuestConfirmationGui(QuestbornPlugin plugin, Player player, String titleText, String itemNameText, String detailText, Runnable onConfirm, Runnable onCancel) {
        this(plugin, player, titleText, itemNameText, detailText, Material.DAMAGED_ANVIL, onConfirm, onCancel);
    }

    public QuestConfirmationGui(QuestbornPlugin plugin, Player player, String titleText, String itemNameText, String detailText, Material displayMaterial, Runnable onConfirm, Runnable onCancel) {
        this.plugin = plugin;
        this.player = player;
        this.titleText = titleText;
        this.itemNameText = itemNameText;
        this.detailText = detailText;
        this.displayMaterial = displayMaterial;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
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
        String title = ColorFormatter.applyColors("&0" + this.titleText);
        Gui gui = ((SimpleBuilder)((SimpleBuilder)((SimpleBuilder)Gui.gui().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(3)).disableAllInteractions()).create();
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
                gui.setCloseGuiAction(event -> {
            if (!this.isNavigating && !EditorSessionManager.isForceClosing) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, this.onCancel);
            }
        });
        gui.getFiller().fill(new GuiItem(bg));
        ArrayList<String> wrappedLore = new ArrayList<String>();
        String[] words = this.detailText.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > 45 && currentLine.length() > 0) {
                wrappedLore.add(ColorFormatter.format("<#a8a8a8>" + currentLine.toString().trim()));
                currentLine = new StringBuilder();
            }
            currentLine.append(word).append(" ");
        }
        if (currentLine.length() > 0) {
            wrappedLore.add(ColorFormatter.format("<#a8a8a8>" + currentLine.toString().trim()));
        }
        ItemStack infoItem = HexItemBuilder.from(this.displayMaterial != null ? this.displayMaterial : Material.DAMAGED_ANVIL).name(ColorFormatter.format("<#ffd470>" + this.itemNameText)).lore(wrappedLore).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(2, 5, new GuiItem(infoItem));
        ItemStack confirmItem = HexItemBuilder.from(Material.LIME_DYE).name(ColorFormatter.format(this.tr("common_editor.buttons.confirm"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.confirm_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(2, 3, new GuiItem(confirmItem, event -> {
            this.isNavigating = true;
            this.onConfirm.run();
        }));
        ItemStack cancelItem = HexItemBuilder.from(Material.ORANGE_DYE).name(ColorFormatter.format(this.tr("common_editor.buttons.cancel"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.cancel_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(2, 7, new GuiItem(cancelItem, event -> {
            this.isNavigating = true;
            this.onCancel.run();
        }));
        gui.open((HumanEntity)this.player);
    }
}
