package ua.woody.questborn.gui.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.QuestObjectiveGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.model.QuestObjectiveType;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestObjectiveTypeSelectorGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String questId;
    private final File questFile;
    private final YamlConfiguration questConfig;
    private final String yamlPath;
    private final Runnable backAction;
    private PaginatedGui gui;
    private Runnable onDirty;
    private String searchQuery = null;
    private boolean isNavigating = false;

    public QuestObjectiveTypeSelectorGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig, String yamlPath, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.questId = questId;
        this.questFile = questFile;
        this.questConfig = questConfig;
        this.yamlPath = yamlPath;
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

    public QuestObjectiveTypeSelectorGui setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
        return this;
    }

    public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        String title = ColorFormatter.applyColors(EditorUtils.truncateGuiTitle(this.tr("quest_type_editor.objective_selector.title")));
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(title))).rows(6)).pageSize(45).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            Runnable closeAction;
            if (!this.isNavigating && !EditorSessionManager.isForceClosing && (closeAction = EditorSessionManager.getSession(this.player.getUniqueId())) != null) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, closeAction);
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

    public QuestObjectiveTypeSelectorGui setOnDirty(Runnable onDirty) {
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
        String searchLore = this.tr("common_editor.selection_menu.search_lore", Map.of("query", this.searchQuery == null ? this.tr("common_editor.selection_menu.search_all") : this.searchQuery));
        ItemStack searchItem = HexItemBuilder.from(Material.COMPASS).name(ColorFormatter.format(this.tr("common_editor.buttons.search"))).lore(ColorFormatter.format(searchLore)).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 2, new GuiItem(searchItem, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInput(this.player, EditorChat.formatPrompt(this.tr("common_editor.selection_menu.search_prompt")), input -> {
                if (input != null && !input.isBlank()) {
                    String query = input.trim();
                    if (query.equalsIgnoreCase("cancel") || query.equalsIgnoreCase("-")) {
                        new QuestObjectiveTypeSelectorGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath, this.backAction).setOnDirty(this.onDirty).open();
                    } else {
                        new QuestObjectiveTypeSelectorGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath, this.backAction).setSearchQuery(query).setOnDirty(this.onDirty).open();
                    }
                } else {
                    new QuestObjectiveTypeSelectorGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath, this.backAction).setOnDirty(this.onDirty).open();
                }
            });
        }));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 4, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 6, new GuiItem(nextItem, event -> this.gui.next()));
    }

    private void populateTypes() {
        for (QuestObjectiveType type : QuestObjectiveType.values()) {
            String typeName = type.name().toLowerCase();
            if (this.searchQuery != null && !typeName.contains(this.searchQuery.toLowerCase())) continue;
            Material mat = QuestObjectiveTypeSelectorGui.getIconForType(type);
            String descKey = "quest_type_editor.objective_selector.desc." + type.name();
            String descRaw = this.tr(descKey);
            ArrayList<String> loreList = new ArrayList<String>();
            if (!descRaw.equals(descKey)) {
                loreList.add(ColorFormatter.format(descRaw));
                loreList.add("");
            }
            loreList.add(ColorFormatter.format(this.tr("quest_type_editor.objective_selector.item_click")));
            ItemStack item = HexItemBuilder.from(mat).name(ColorFormatter.format("<#ffd470>" + type.name())).lore(loreList).flags(ItemFlag.values()).build();
            this.gui.addItem(new GuiItem(item, event -> {
                this.isNavigating = true;
                this.questConfig.set(this.yamlPath + ".type", (Object)type.name());
                EditorUtils.populateDefaultObjectiveProperties(this.questConfig, this.yamlPath, type);
                this.saveConfig();
                new QuestObjectiveGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath, this.tr("quest_editor.stages_menu.obj_title", Map.of("key", typeName)), this.backAction).setOnDirty(this.onDirty).open();
            }));
        }
    }

    public static Material getIconForType(QuestObjectiveType type) {
        if (type == null) {
            return Material.TARGET;
        }
        return switch (type) {
            case BLOCK_BREAK -> Material.DIAMOND_PICKAXE;
            case BLOCK_PLACE -> Material.GRASS_BLOCK;
            case ITEM_CRAFT -> Material.CRAFTING_TABLE;
            case ITEM_SMELT -> Material.FURNACE;
            case ITEM_ENCHANT, ENCHANT_TABLE_USE -> Material.ENCHANTING_TABLE;
            case ITEM_FISH -> Material.FISHING_ROD;
            case ITEM_COOK -> Material.CAMPFIRE;
            case BREWING -> Material.BREWING_STAND;
            case CONSUME_ITEM -> Material.APPLE;
            case KILL_ENTITY -> Material.ZOMBIE_HEAD;
            case DEAL_DAMAGE -> Material.IRON_SWORD;
            case TAKE_DAMAGE -> Material.SHIELD;
            case TRAVEL_DISTANCE, SPRINT_DISTANCE -> Material.LEATHER_BOOTS;
            case REACH_LOCATION, JOIN_SERVER -> Material.COMPASS;
            case REACH_BIOME -> Material.ACACIA_SAPLING;
            case ENTER_REGION -> Material.FILLED_MAP;
            case LEAVE_REGION -> Material.MAP;
            case INTERACT_BLOCK -> Material.OAK_BUTTON;
            case INTERACT_ENTITY, TAME_ANIMAL -> Material.BONE;
            case NPC_INTERACT -> Material.VILLAGER_SPAWN_EGG;
            case GIVE_ITEM_TO_NPC, VILLAGER_SELL_ITEM, VILLAGER_BUY_ITEM -> Material.EMERALD;
            case USE_ITEM -> Material.FLINT_AND_STEEL;
            case EXECUTE_COMMAND -> Material.COMMAND_BLOCK;
            case CHAT_MESSAGE -> Material.PAPER;
            case LEVEL_UP_REACH, LEVEL_UP_GAIN, EXPERIENCE_ORB_PICKUP -> Material.EXPERIENCE_BOTTLE;
            case FILL_BUCKET, SWIM_DISTANCE -> Material.WATER_BUCKET;
            case EMPTY_BUCKET -> Material.BUCKET;
            case TILL_SOIL -> Material.WOODEN_HOE;
            case PLANT_SEED -> Material.WHEAT_SEEDS;
            case HARVEST_CROP, BREED_ANIMALS -> Material.WHEAT;
            case BONE_MEAL_USE -> Material.BONE_MEAL;
            case STRIP_LOG -> Material.IRON_AXE;
            case WAX_OFF, WAX_ON -> Material.HONEYCOMB;
            case ITEM_REPAIR, ANVIL_USE -> Material.ANVIL;
            case ITEM_RENAME -> Material.NAME_TAG;
            case ITEM_BREAK -> Material.FLINT;
            case DYE_ITEM -> Material.RED_DYE;
            case FILL_FUEL -> Material.COAL;
            case BURN_FUEL -> Material.CHARCOAL;
            case MILK_COW -> Material.MILK_BUCKET;
            case SHEAR_SHEEP -> Material.SHEARS;
            case ENTITY_RIDE -> Material.SADDLE;
            case THROW_EGG -> Material.EGG;
            case ENTER_BED, SLEEP_IN_BED -> Material.RED_BED;
            case CHANGE_DIMENSION -> Material.END_PORTAL_FRAME;
            case FALL_DISTANCE -> Material.FEATHER;
            case BOAT_TRAVEL -> Material.OAK_BOAT;
            case MINECART_TRAVEL -> Material.MINECART;
            case ELYTRA_FLY -> Material.ELYTRA;
            case JUMP -> Material.RABBIT_FOOT;
            case CROUCH, CROUCH_DISTANCE -> Material.LEATHER_LEGGINGS;
            case POTION_SPLASH -> Material.SPLASH_POTION;
            case POTION_DRINK -> Material.POTION;
            case BEACON_ACTIVATE -> Material.BEACON;
            case CONDUIT_ACTIVATE -> Material.CONDUIT;
            case PLAYER_KILL -> Material.PLAYER_HEAD;
            case ASSIST_KILL -> Material.IRON_SWORD;
            case PLAY_TIME -> Material.CLOCK;
            case WEAR_ARMOR -> Material.IRON_CHESTPLATE;
            case HOLD_ITEM -> Material.STICK;
            case DROP_ITEM -> Material.COBBLESTONE;
            case OPEN_CONTAINER -> Material.CHEST;
            case SIGN_EDIT -> Material.OAK_SIGN;
            case BOOK_EDIT -> Material.WRITABLE_BOOK;
            case RECEIVE_DAMAGE_TYPE -> Material.SHIELD;
            default -> Material.TARGET;
        };
    }
}
