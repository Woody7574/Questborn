package ua.woody.questborn.gui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.utils.HexItemBuilder;

public class AttributeSelectorGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final Consumer<Attribute> onSelect;
    private final Runnable onBack;
    private final List<String> ignoredAttributes;
    private PaginatedGui gui;

    public AttributeSelectorGui(QuestbornPlugin plugin, Player player, List<String> ignoredAttributes, Consumer<Attribute> onSelect, Runnable onBack) {
        this.plugin = plugin;
        this.player = player;
        this.ignoredAttributes = ignoredAttributes;
        this.onSelect = onSelect;
        this.onBack = onBack;
        this.setupGui();
    }

    private String tr(String key) {
        return this.plugin.getLanguage().trEditor(key);
    }

    private void setupGui() {
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.format(this.tr("quest_editor.attribute_selector.title")))))).rows(6)).pageSize(45).create();
        this.gui.disableAllInteractions();
        for (Attribute attr : Attribute.values()) {
            if (this.ignoredAttributes != null && this.ignoredAttributes.contains(attr.name())) continue;
            Material icon = AttributeSelectorGui.getIconForAttribute(attr.name());
            String descKey = "quest_editor.attribute_selector.desc." + attr.name();
            String descRaw = this.tr(descKey);
            ArrayList<String> loreList = new ArrayList<String>();
            if (!descRaw.equals(descKey)) {
                loreList.add(ColorFormatter.format(descRaw));
                loreList.add("");
            }
            loreList.add(ColorFormatter.format(this.tr("quest_editor.attribute_selector.click_select")));
            ItemStack item = HexItemBuilder.from(icon).name(ColorFormatter.format("<#ffd470>" + attr.name())).lore(loreList).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.addItem(new GuiItem(item, event -> {
                if (this.onSelect != null) {
                    this.onSelect.accept(attr);
                }
            }));
        }
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> {
            if (this.onBack != null) {
                this.onBack.run();
            }
        }));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 4, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 6, new GuiItem(nextItem, event -> this.gui.next()));
    }

    public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        this.gui.open((HumanEntity)this.player);
    }

    public static Material getIconForAttribute(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        if (upper.contains("MAX_ABSORPTION")) {
            return Material.GOLDEN_APPLE;
        }
        if (upper.contains("MAX_HEALTH")) {
            return Material.APPLE;
        }
        if (upper.contains("ARMOR_TOUGHNESS")) {
            return Material.DIAMOND_CHESTPLATE;
        }
        if (upper.contains("ARMOR")) {
            return Material.IRON_CHESTPLATE;
        }
        if (upper.contains("EXPLOSION_KNOCKBACK_RESISTANCE")) {
            return Material.OBSIDIAN;
        }
        if (upper.contains("KNOCKBACK_RESISTANCE")) {
            return Material.NETHERITE_CHESTPLATE;
        }
        if (upper.contains("ATTACK_DAMAGE")) {
            return Material.IRON_SWORD;
        }
        if (upper.contains("ATTACK_SPEED")) {
            return Material.GOLDEN_SWORD;
        }
        if (upper.contains("ATTACK_KNOCKBACK")) {
            return Material.PISTON;
        }
        if (upper.contains("SWEEPING_DAMAGE_RATIO")) {
            return Material.DIAMOND_SWORD;
        }
        if (upper.contains("FLYING_SPEED")) {
            return Material.ELYTRA;
        }
        if (upper.contains("WATER_MOVEMENT_EFFICIENCY")) {
            return Material.HEART_OF_THE_SEA;
        }
        if (upper.contains("MOVEMENT_EFFICIENCY")) {
            return Material.SUGAR;
        }
        if (upper.contains("MOVEMENT_SPEED")) {
            return Material.FEATHER;
        }
        if (upper.contains("SNEAKING_SPEED")) {
            return Material.LEATHER_LEGGINGS;
        }
        if (upper.contains("JUMP_STRENGTH")) {
            return Material.SLIME_BLOCK;
        }
        if (upper.contains("STEP_HEIGHT")) {
            return Material.OAK_STAIRS;
        }
        if (upper.contains("LUCK")) {
            return Material.RABBIT_FOOT;
        }
        if (upper.contains("FALL_DAMAGE")) {
            return Material.HAY_BLOCK;
        }
        if (upper.contains("SAFE_FALL_DISTANCE")) {
            return Material.COBWEB;
        }
        if (upper.contains("GRAVITY")) {
            return Material.ANVIL;
        }
        if (upper.contains("OXYGEN_BONUS")) {
            return Material.GLASS_BOTTLE;
        }
        if (upper.contains("BURNING_TIME")) {
            return Material.BLAZE_POWDER;
        }
        if (upper.contains("SUBMERGED_MINING_SPEED")) {
            return Material.PRISMARINE_SHARD;
        }
        if (upper.contains("MINING_EFFICIENCY")) {
            return Material.GOLDEN_PICKAXE;
        }
        if (upper.contains("BLOCK_BREAK_SPEED")) {
            return Material.DIAMOND_PICKAXE;
        }
        if (upper.contains("BLOCK_INTERACTION_RANGE")) {
            return Material.OAK_LOG;
        }
        if (upper.contains("ENTITY_INTERACTION_RANGE")) {
            return Material.ZOMBIE_HEAD;
        }
        if (upper.contains("FOLLOW_RANGE")) {
            return Material.COMPASS;
        }
        if (upper.contains("SPAWN_REINFORCEMENTS")) {
            return Material.ZOMBIE_SPAWN_EGG;
        }
        if (upper.contains("TEMPT_RANGE")) {
            return Material.CARROT_ON_A_STICK;
        }
        if (upper.contains("SCALE")) {
            return Material.SLIME_BALL;
        }
        if (upper.contains("BOUNCINESS")) {
            return Material.SLIME_BLOCK;
        }
        if (upper.contains("FRICTION")) {
            return Material.PACKED_ICE;
        }
        if (upper.contains("AIR_DRAG")) {
            return Material.PHANTOM_MEMBRANE;
        }
        if (upper.contains("CAMERA_DISTANCE")) {
            return Material.ENDER_EYE;
        }
        if (upper.contains("NAME_TAG") || upper.contains("BELOW_NAME")) {
            return Material.NAME_TAG;
        }
        if (upper.contains("WAYPOINT_TRANSMIT")) {
            return Material.NOTE_BLOCK;
        }
        if (upper.contains("WAYPOINT_RECEIVE")) {
            return Material.JUKEBOX;
        }
        return Material.PAPER;
    }
}
