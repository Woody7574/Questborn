package ua.woody.questborn.gui.editor;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
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

public class QuestEntityTypeSelectorGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String questId;
    private final File questFile;
    private final YamlConfiguration questConfig;
    private final String keyPath;
    private final Runnable backAction;
    private final Consumer<String> onSelect;
    private PaginatedGui gui;
    private Runnable onDirty;
    private boolean isNavigating = false;

    public QuestEntityTypeSelectorGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig, String keyPath, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.questId = questId;
        this.questFile = questFile;
        this.questConfig = questConfig;
        this.keyPath = keyPath;
        this.backAction = backAction;
        this.onSelect = typeName -> {
            questConfig.set(keyPath, typeName);
            this.saveConfig();
        };
    }

    public QuestEntityTypeSelectorGui(QuestbornPlugin plugin, Player player, Consumer<String> onSelect, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.questId = null;
        this.questFile = null;
        this.questConfig = null;
        this.keyPath = null;
        this.backAction = backAction;
        this.onSelect = onSelect;
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
        String title = ColorFormatter.applyColors(this.tr("quest_type_editor.entity_selector.title"));
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(6)).pageSize(45).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            Runnable closeAction;
            if (!this.isNavigating && !EditorSessionManager.isForceClosing && (closeAction = EditorSessionManager.getSession(this.player.getUniqueId())) != null) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, closeAction);
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        this.setupButtons();
        this.populateEntities();
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }

    public QuestEntityTypeSelectorGui setOnDirty(Runnable onDirty) {
        this.onDirty = onDirty;
        return this;
    }

    private void saveConfig() {
        if (this.onDirty != null) {
            this.onDirty.run();
        }
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
            this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_type_editor.material_selector.input_chat_prompt")), "ZOMBIE", input -> {
                if (input != null && !input.isBlank()) {
                    String ent = input.trim();
                    if (EditorUtils.isValidEntity(ent, this.plugin)) {
                        if (this.onSelect != null) {
                            this.isNavigating = true;
                        }
                        this.onSelect.accept(ent);
                        this.isNavigating = true;
                        this.backAction.run();
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
                String customId = EditorUtils.getCustomIdFromItem(inHand, this.plugin);
                if (this.onSelect != null) {
                    this.isNavigating = true;
                }
                this.onSelect.accept(customId);
                this.isNavigating = true;
                this.backAction.run();
            } else {
                EditorChat.sendError(this.player, this.tr("quest_type_editor.material_selector.hand_error"));
                this.open();
            }
        }));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 4, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 6, new GuiItem(nextItem, event -> this.gui.next()));
    }

    private void populateEntities() {
        for (EntityType type : EntityType.values()) {
            if (!type.isAlive() && type != EntityType.ARMOR_STAND && type != EntityType.BOAT && type != EntityType.MINECART || type == EntityType.UNKNOWN) continue;
            String typeName = type.name();
            Material eggMat = EditorUtils.getDisplayMaterialForEntity(typeName);
            ItemStack item = HexItemBuilder.from(eggMat).name(ColorFormatter.format("<#ffd470>" + typeName)).lore("", ColorFormatter.format(this.tr("quest_editor.entity_selector.click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.addItem(new GuiItem(item, event -> {
                if (this.onSelect != null) {
                    this.isNavigating = true;
                    this.onSelect.accept(typeName);
                }
                this.isNavigating = true;
                this.backAction.run();
            }));
        }
    }
}
