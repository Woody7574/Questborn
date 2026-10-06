package ua.woody.questborn.gui.editor;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.lang.ColorFormatter;
import ua.woody.questborn.managers.NpcValidator;
import ua.woody.questborn.utils.HexItemBuilder;

public class NpcValidationGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String npcId;
    private final Runnable backAction;
    private boolean isNavigating = false;

    public NpcValidationGui(QuestbornPlugin plugin, Player player, String npcId, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.npcId = npcId;
        this.backAction = backAction;
    }

    private String tr(String path) {
        if (plugin != null && plugin.getLanguage() != null) {
            return plugin.getLanguage().trEditor(path);
        }
        return path;
    }

    private String tr(String path, java.util.Map<String, String> placeholders) {
        if (plugin != null && plugin.getLanguage() != null) {
            return plugin.getLanguage().trEditor(path, placeholders);
        }
        return path;
    }

    public void open() {
        NpcValidator validator = new NpcValidator(plugin);
        NpcValidator.ValidationResult result = validator.validate(npcId);

        String title = "<#52de7d>Validation: " + npcId;
        if (result.hasErrors) {
            title = "<#ff5e5e>Validation Errors: " + npcId;
        } else if (result.hasWarnings) {
            title = "<#ffc940>Validation Warnings: " + npcId;
        }

        PaginatedGui gui = Gui.paginated()
                .title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors(title))))
                .rows(6)
                .pageSize(45)
                .disableAllInteractions()
                .create();

                gui.setCloseGuiAction(event -> {
            if (!this.isNavigating && !EditorSessionManager.isForceClosing) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, this.backAction);
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.getFiller().fillBottom(new GuiItem(bg));

        for (String msg : result.messages) {
            Material mat = Material.PAPER;
            if (msg.contains("❌")) mat = Material.BARRIER;
            if (msg.contains("⚠️")) mat = Material.YELLOW_DYE;
            if (msg.contains("✔️")) mat = Material.LIME_DYE;

            ItemStack item = HexItemBuilder.from(mat)
                    .name(ColorFormatter.format(msg))
                    .flags(ItemFlag.HIDE_ATTRIBUTES).build();
            gui.addItem(new GuiItem(item));
        }

        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR)
                .name(ColorFormatter.format(plugin.getLanguage().trEditor("common_editor.buttons.back")))
                .lore(ColorFormatter.format(plugin.getLanguage().trEditor("common_editor.buttons.back_lore")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(6, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            backAction.run();
        }));

        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(plugin.getLanguage().trEditor("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(6, 4, new GuiItem(prevItem, event -> gui.previous()));

        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(plugin.getLanguage().trEditor("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(6, 6, new GuiItem(nextItem, event -> gui.next()));

        gui.open(player);
    }
}
