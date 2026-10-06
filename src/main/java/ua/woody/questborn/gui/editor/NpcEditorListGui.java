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
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.MainEditorSelectorGui;
import ua.woody.questborn.gui.editor.NpcMainEditorGui;
import ua.woody.questborn.gui.editor.QuestConfirmationGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.model.NpcConfig;
import ua.woody.questborn.utils.HexItemBuilder;

public class NpcEditorListGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private PaginatedGui gui;

    public NpcEditorListGui(QuestbornPlugin plugin, Player player) {
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
        int npcCount = this.plugin.getNpcManager().getAllNpcIds().size();
        String title = ColorFormatter.applyColors(this.tr("npc_editor.title_list", Map.of("count", String.valueOf(npcCount))));
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(6)).pageSize(45).disableAllInteractions()).create();
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> new MainEditorSelectorGui(this.plugin, this.player).open()));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 4, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 6, new GuiItem(nextItem, event -> this.gui.next()));
        ItemStack createBtn = HexItemBuilder.from(Material.EMERALD_BLOCK).name(ColorFormatter.format(this.tr("common_editor.buttons.add_new"))).lore(ColorFormatter.format(this.tr("quest_editor.npc_list.create_click_left")), ColorFormatter.format(this.tr("quest_editor.npc_list.create_click_right"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 5, new GuiItem(createBtn, event -> {
            boolean isRightClick = event.isRightClick();
            String promptKey = isRightClick ? "quest_editor.npc_list.create_existing_prompt" : "quest_editor.npc_list.create_prompt";
            this.plugin.getChatInputManager().requestInput(this.player, EditorChat.formatPrompt(this.tr(promptKey)), input -> {
                if (input != null && !input.isBlank()) {
                    File file;
                    YamlConfiguration cfg;
                    String sanitized;
                    if (!isRightClick) {
                        String physicalId;
                        sanitized = input.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
                        if (this.plugin.getNpcIntegrationManager().getProvider() != null && (physicalId = this.plugin.getNpcIntegrationManager().getProvider().createNpc(sanitized, this.player.getLocation())) != null) {
                            sanitized = physicalId;
                        }
                    } else {
                        sanitized = input.trim();
                    }
                    if ((cfg = YamlConfiguration.loadConfiguration((File)(file = new File(this.plugin.getDataFolder(), "npc/npcs.yml")))).contains("npcs." + sanitized)) {
                        EditorChat.sendError(this.player, this.tr("npc_editor.id_exists"));
                        this.open();
                        return;
                    }
                    String displayNameValue = isRightClick ? "<#a7ff99>NPC " + sanitized : "<#a7ff99>" + input.trim();
                    cfg.set("npcs." + sanitized + ".displayname", (Object)displayNameValue);
                    cfg.set("npcs." + sanitized + ".gui-title", (Object)"&0<#1c1c1c>{npc} - Quests");
                    try {
                        cfg.save(file);
                        this.plugin.getNpcManager().reload();
                        EditorChat.sendSuccess(this.player, this.tr("npc_editor.saved_success", Map.of("id", sanitized)));
                        new NpcMainEditorGui(this.plugin, this.player, sanitized).open();
                    }
                    catch (Exception e) {
                        EditorChat.sendError(this.player, this.tr("quest_editor.npc_list.save_error"));
                        this.open();
                    }
                } else {
                    this.open();
                }
            });
        }));
        this.populateNpcs();
        this.gui.open((HumanEntity)this.player);
    }

    private void populateNpcs() {
        Collection<String> npcIds = this.plugin.getNpcManager().getAllNpcIds();
        for (String npcId : npcIds) {
            NpcConfig npc = this.plugin.getNpcManager().getConfigByNpcId(npcId);
            String displayName = npc != null && npc.getDisplayName() != null ? npc.getDisplayName() : npcId;
            String dialogueId = npc != null && npc.getDialogueId() != null ? npc.getDialogueId() : "none";
            HexItemBuilder builder = HexItemBuilder.from(Material.VILLAGER_SPAWN_EGG).name(ColorFormatter.format(displayName)).lore(ColorFormatter.format(this.tr("quest_editor.npc_list.id_lore", Map.of("id", npcId))), ColorFormatter.format(this.tr("quest_editor.npc_list.dialogue_lore", Map.of("dialogue", dialogueId))), "", ColorFormatter.format(this.tr("quest_editor.npc_list.edit_click")), ColorFormatter.format(this.tr("quest_editor.npc_list.delete_click"))).flags(ItemFlag.HIDE_ATTRIBUTES);
            this.gui.addItem(new GuiItem(builder.build(), event -> {
                if (event.getClick() == ClickType.DROP) {
                    new QuestConfirmationGui(this.plugin, this.player, this.tr("npc_editor.delete_confirm_title"), this.tr("npc_editor.delete_confirm_item"), this.tr("npc_editor.delete_confirm_desc", Map.of("id", npcId)), Material.DAMAGED_ANVIL, () -> {
                        File file = new File(this.plugin.getDataFolder(), "npc/npcs.yml");
                        YamlConfiguration cfg = YamlConfiguration.loadConfiguration((File)file);
                        cfg.set("npcs." + npcId, null);
                        try {
                            cfg.save(file);
                            this.plugin.getNpcManager().reload();
                            EditorChat.sendSuccess(this.player, this.tr("npc_editor.deleted_success", Map.of("id", npcId)));
                        }
                        catch (Exception exception) {
                        }
                        this.open();
                    }, () -> {
                        EditorChat.sendError(this.player, this.tr("quest_editor.npc_main.delete_cancelled"));
                        this.open();
                    }).open();
                } else {
                    new NpcMainEditorGui(this.plugin, this.player, npcId).open();
                }
            }));
        }
    }
}
