package ua.woody.questborn.gui.editor;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.GuiUtils;
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.MainEditorSelectorGui;
import ua.woody.questborn.gui.editor.QuestConfirmationGui;
import ua.woody.questborn.gui.editor.QuestTypeMainEditorGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestTypeEditorListGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private PaginatedGui gui;

    public QuestTypeEditorListGui(QuestbornPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
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
        Collection<QuestTypeConfig> allTypes = this.plugin.getQuestManager().getQuestTypeManager().getAllTypes();
        String title = ColorFormatter.applyColors(this.tr("quest_type_editor.title_list", Map.of("count", String.valueOf(allTypes.size()))));
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(6)).pageSize(45).disableAllInteractions()).create();
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> new MainEditorSelectorGui(this.plugin, this.player).open()));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 4, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 6, new GuiItem(nextItem, event -> this.gui.next()));
        ItemStack createBtn = HexItemBuilder.from(Material.EMERALD_BLOCK).name(ColorFormatter.format(this.tr("common_editor.buttons.add_new"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.add_new_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 5, new GuiItem(createBtn, event -> this.plugin.getChatInputManager().requestInput(this.player, EditorChat.formatPrompt(this.tr("quest_type_editor.prompts.new_type_id")), input -> {
            if (input != null && !input.isBlank()) {
                File file;
                String sanitized = input.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
                File typesDir = new File(this.plugin.getDataFolder(), "types");
                if (!typesDir.exists()) {
                    typesDir.mkdirs();
                }
                if ((file = new File(typesDir, sanitized + ".yml")).exists()) {
                    EditorChat.sendError(this.player, this.tr("quest_type_editor.id_exists"));
                    this.open();
                    return;
                }
                YamlConfiguration cfg = new YamlConfiguration();
                cfg.set("id", (Object)sanitized);
                cfg.set("display-name", (Object)("<#7dd3ff>" + sanitized));
                cfg.set("material", (Object)"BOOK");
                cfg.set("custom-model-data", (Object)0);
                cfg.set("cooldown-seconds", (Object)86400);
                cfg.set("folder", (Object)("quests/" + sanitized));
                cfg.set("enabled", (Object)true);
                cfg.set("engine", (Object)"DEFAULT");
                try {
                    cfg.save(file);
                    this.plugin.getQuestManager().getQuestTypeManager().reload();
                    this.plugin.getQuestManager().restartRotationAnnounceTask();
                    EditorChat.sendSuccess(this.player, this.tr("quest_type_editor.type_created", Map.of("id", sanitized)));
                    new QuestTypeMainEditorGui(this.plugin, this.player, sanitized, file, cfg).open();
                }
                catch (Exception e) {
                    EditorChat.sendError(this.player, this.tr("quest_type_editor.error_create"));
                    this.open();
                }
            } else {
                this.open();
            }
        })));
        this.populateTypes();
        this.gui.open((HumanEntity)this.player);
    }

    private void populateTypes() {
        Collection<QuestTypeConfig> types = this.plugin.getQuestManager().getQuestTypeManager().getAllTypes();
        for (QuestTypeConfig type : types) {
            String typeId = type.getId();
            String iconId = type.getItemsAdderId() != null ? "ia:" + type.getItemsAdderId() : (type.getCraftEngineId() != null ? "ce:" + type.getCraftEngineId() : (type.getMaterial() != null ? type.getMaterial().name() : "BOOK"));
            ItemStack iconBaseItem = EditorUtils.getGuiItemForString(iconId, this.plugin);
            if (iconBaseItem == null || iconBaseItem.getType() == Material.AIR) {
                iconBaseItem = new ItemStack(Material.BOOK);
            }
            File file = type.getSourceFile() != null ? type.getSourceFile() : new File(this.plugin.getDataFolder(), "types/" + typeId + ".yml");
            YamlConfiguration cfg = file.exists() ? YamlConfiguration.loadConfiguration((File)file) : new YamlConfiguration();
            int questCount = this.plugin.getQuestManager().getByType(typeId).size();
            HexItemBuilder builder = HexItemBuilder.from(iconBaseItem).name(ColorFormatter.format((String)(type.getDisplayName() != null ? type.getDisplayName() : "<#ffd470>" + typeId))).lore(ColorFormatter.format(this.tr("common_editor.selection_menu.item_id", Map.of("id", typeId))), ColorFormatter.format(this.tr("quest_type_editor.type_list_engine", Map.of("engine", type.getEngine().name()))), ColorFormatter.format(this.tr("quest_type_editor.type_list_folder", Map.of("folder", type.getFolder()))), ColorFormatter.format(this.tr("quest_type_editor.type_list_quests", Map.of("count", String.valueOf(questCount)))), "", ColorFormatter.format(this.tr("quest_type_editor.type_list_click")), ColorFormatter.format(this.tr("quest_type_editor.type_list_clone")), ColorFormatter.format(this.tr("quest_type_editor.type_list_delete"))).flags(ItemFlag.HIDE_ATTRIBUTES);
            if (type.getCustomModelData() != null && type.getCustomModelData() > 0) {
                builder.model(type.getCustomModelData());
            }
            ItemStack finalItem = builder.build();
            if (iconBaseItem.getType() == Material.PLAYER_HEAD && type.getBaseHead() != null) {
                GuiUtils.applyBaseHead(finalItem, type.getBaseHead());
            }
            this.gui.addItem(new GuiItem(finalItem, event -> {
                if (event.getClick() == ClickType.DROP) {
                    new QuestConfirmationGui(this.plugin, this.player, this.tr("quest_type_editor.confirm_del_gui_title", Map.of("id", typeId)), this.tr("quest_type_editor.confirm_del_title"), this.tr("quest_type_editor.confirm_del_desc", Map.of("id", typeId)), () -> {
                        if (file.exists()) {
                            file.delete();
                        }
                        this.plugin.getQuestManager().getQuestTypeManager().reload();
                        this.plugin.getQuestManager().restartRotationAnnounceTask();
                        EditorChat.sendSuccess(this.player, this.tr("quest_type_editor.msg_del_success", Map.of("id", typeId)));
                        this.open();
                    }, () -> this.open()).open();
                } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
                    this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_type_editor.clone_prompt")), typeId + "_copy", input -> {
                        if (input != null && !input.isEmpty()) {
                            String sanitized = input.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
                            File destFile = new File(file.getParentFile(), sanitized + ".yml");
                            if (destFile.exists()) {
                                EditorChat.sendError(this.player, this.tr("quest_type_editor.error_clone_exists"));
                                this.open();
                            } else {
                                try {
                                    YamlConfiguration cloneCfg = YamlConfiguration.loadConfiguration((File)file);
                                    cloneCfg.set("id", (Object)sanitized);
                                    String origFolder = cloneCfg.getString("folder", "quests/" + typeId);
                                    Object newFolder = origFolder.replace(typeId, sanitized);
                                    if (((String)newFolder).equals(origFolder)) {
                                        newFolder = "quests/" + sanitized;
                                    }
                                    cloneCfg.set("folder", newFolder);
                                    cloneCfg.save(destFile);
                                    this.plugin.getQuestManager().getQuestTypeManager().reload();
                                    this.plugin.getQuestManager().restartRotationAnnounceTask();
                                    EditorChat.sendSuccess(this.player, this.tr("quest_type_editor.msg_clone_success", Map.of("id", sanitized)));
                                    new QuestTypeMainEditorGui(this.plugin, this.player, sanitized, destFile, cloneCfg).open();
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                    EditorChat.sendError(this.player, this.tr("quest_type_editor.error_clone"));
                                    this.open();
                                }
                            }
                        } else {
                            this.open();
                        }
                    });
                } else {
                    new QuestTypeMainEditorGui(this.plugin, this.player, typeId, file, cfg).open();
                }
            }));
        }
    }
}
