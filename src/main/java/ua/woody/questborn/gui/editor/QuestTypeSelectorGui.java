package ua.woody.questborn.gui.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.QuestMainEditorGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestTypeSelectorGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String questId;
    private final File questFile;
    private final YamlConfiguration questConfig;
    private final Consumer<String> onTypeSelected;
    private final Runnable backAction;
    private PaginatedGui gui;
    private boolean isNavigating = false;

    public QuestTypeSelectorGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig, Consumer<String> onTypeSelected, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.questId = questId;
        this.questFile = questFile;
        this.questConfig = questConfig;
        this.onTypeSelected = onTypeSelected;
        this.backAction = backAction;
    }

    public QuestTypeSelectorGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig, Runnable backAction) {
        this(plugin, player, questId, questFile, questConfig, null, backAction);
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
        String title = ColorFormatter.applyColors(this.tr("quest_type_editor.title_selector"));
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(6)).pageSize(45).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            if (!this.isNavigating && !EditorSessionManager.isForceClosing) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, this.backAction);
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        this.setupButtons();
        this.populateTypes();
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
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 4, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 6, new GuiItem(nextItem, event -> this.gui.next()));
    }

    private void populateTypes() {
        for (QuestTypeConfig type : this.plugin.getQuestManager().getQuestTypeManager().getAllTypes()) {
            String typeName = type.getId();
            Material mat = type.getMaterial() != null ? type.getMaterial() : Material.BOOK;
            HexItemBuilder builder = HexItemBuilder.from(mat);
            Object displayName = type.getDisplayName();
            if (displayName == null || ((String)displayName).isEmpty()) {
                displayName = "<#ffd470>" + typeName;
            }
            builder.name(ColorFormatter.format((String)displayName));
            if (type.getCustomModelData() != null && type.getCustomModelData() > 0) {
                builder.model(type.getCustomModelData());
            }
            ArrayList<String> typeLore = new ArrayList<String>();
            if (type.getLore() != null && !type.getLore().isEmpty()) {
                for (String l : type.getLore()) {
                    String formatted = l.replace("{cooldown}", "0").replace("{total}", "0").replace("{available}", "0").replace("{completed}", "0").replace("{assigned}", "0").replace("{reset}", "-");
                    typeLore.add(ColorFormatter.format(formatted));
                }
                typeLore.add("");
            }
            typeLore.add(ColorFormatter.format(this.tr("quest_type_editor.select_type_click")));
            ItemStack item = builder.lore(typeLore).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.addItem(new GuiItem(item, event -> {
                QuestTypeConfig newTypeConfig = this.plugin.getQuestManager().getQuestTypeManager().getType(typeName);
                if (newTypeConfig != null) {
                    if (this.onTypeSelected != null) {
                        this.onTypeSelected.accept(typeName);
                    } else {
                        this.questConfig.set("id", (Object)this.questId);
                        this.questConfig.set("type", null);
                        if (this.questFile.exists()) {
                            File newFile;
                            File targetDir = new File(this.plugin.getDataFolder(), newTypeConfig.getFolder());
                            if (!targetDir.exists()) {
                                targetDir.mkdirs();
                            }
                            if (!(newFile = new File(targetDir, this.questId + ".yml")).equals(this.questFile)) {
                                this.questFile.delete();
                                try {
                                    this.questConfig.save(newFile);
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        this.plugin.getQuestManager().reload();
                        new QuestMainEditorGui(this.plugin, this.player, this.questId).open();
                    }
                }
            }));
        }
    }
}
