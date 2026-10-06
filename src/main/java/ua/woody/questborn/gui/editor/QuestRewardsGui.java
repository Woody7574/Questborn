package ua.woody.questborn.gui.editor;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.QuestAttributeRewardGui;
import ua.woody.questborn.gui.editor.QuestBroadcastRewardGui;
import ua.woody.questborn.gui.editor.QuestChanceRewardGui;
import ua.woody.questborn.gui.editor.QuestCommandRewardGui;
import ua.woody.questborn.gui.editor.QuestConfirmationGui;
import ua.woody.questborn.gui.editor.QuestEditorListGui;
import ua.woody.questborn.gui.editor.QuestEffectRewardGui;
import ua.woody.questborn.gui.editor.QuestMessageRewardGui;
import ua.woody.questborn.gui.editor.QuestRequiredMaterialsGui;
import ua.woody.questborn.gui.editor.QuestSilentSettingsGui;
import ua.woody.questborn.gui.editor.UnsavedChangesConfirmGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.SimpleBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestRewardsGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String questId;
    private final File questFile;
    private final YamlConfiguration questConfig;
    private final String yamlPath;
    private final String titleText;
    private final Runnable backAction;
    private Gui gui;
    private Runnable onDirty;
    private boolean isNavigating = false;
    private boolean hasChanges = false;

    public QuestRewardsGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig, String yamlPath, String titleText, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.questId = questId;
        this.questFile = questFile;
        this.questConfig = questConfig;
        this.yamlPath = yamlPath;
        this.titleText = titleText;
        this.backAction = backAction;
    }

    public QuestRewardsGui setOnDirty(Runnable onDirty) {
        this.onDirty = onDirty;
        return this;
    }

    private void markDirty() {
        this.hasChanges = true;
        if (this.onDirty != null) {
            this.onDirty.run();
        }
    }

    public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        String title = ColorFormatter.applyColors("&0" + this.titleText);
        this.gui = ((SimpleBuilder)((SimpleBuilder)((SimpleBuilder)Gui.gui().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(5)).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            if (!this.isNavigating && this.hasChanges) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> new UnsavedChangesConfirmGui(this.player, () -> {
                    EditorUtils.saveQuestConfig(this.plugin, this.questConfig, this.questFile);
                    new QuestEditorListGui(this.plugin, this.player).open();
                }, () -> new QuestEditorListGui(this.plugin, this.player).open(), () -> {
                    Runnable returnAction = EditorSessionManager.getReturnAction(this.player.getUniqueId());
                    if (returnAction != null) {
                        returnAction.run();
                    } else {
                        this.open();
                    }
                }).open());
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(" ").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fill(new GuiItem(bg));
        this.setupButtons();
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }

    private void refresh() {
        this.isNavigating = true;
        this.open();
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

    private void setupButtons() {
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(5, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            this.backAction.run();
        }));
        boolean hasXp = this.questConfig.contains(this.yamlPath + ".xp");
        int xp = 0;
        boolean isLevelMode = false;
        if (hasXp) {
            if (this.questConfig.isConfigurationSection(this.yamlPath + ".xp")) {
                ConfigurationSection sec = this.questConfig.getConfigurationSection(this.yamlPath + ".xp");
                if (sec.contains("levels")) {
                    xp = sec.getInt("levels");
                    isLevelMode = true;
                } else if (sec.contains("points")) {
                    xp = sec.getInt("points");
                }
            } else {
                xp = this.questConfig.getInt(this.yamlPath + ".xp", 0);
            }
        }
        String xpTitle = hasXp ? this.tr("quest_editor.rewards_menu.xp_title_has") : this.tr("quest_editor.rewards_menu.xp_title_none");
        if (isLevelMode) {
            xpTitle = xpTitle.replace("XP", "Levels").replace("xp", "Levels");
        }
        HexItemBuilder xpBuilder = HexItemBuilder.from(Material.EXPERIENCE_BOTTLE).name(ColorFormatter.format(xpTitle)).lore(ColorFormatter.format(hasXp ? this.tr("quest_editor.rewards_menu.val_has", Map.of("val", String.valueOf(xp))) : this.tr("quest_editor.rewards_menu.val_none")), "", this.plugin.getLanguage().trEditorList("quest_editor.main_menu.slot_click"), ColorFormatter.format(this.tr("quest_editor.rewards_menu.xp_toggle_mode"))).flags(ItemFlag.HIDE_ATTRIBUTES);
        if (hasXp) {
            xpBuilder.lore(ColorFormatter.format(this.tr("quest_editor.rewards_menu.click_del")));
            xpBuilder.enchant(Enchantment.DURABILITY);
        }
        boolean finalIsLevelMode = isLevelMode;
        int finalXp = xp;
        this.gui.setItem(2, 4, new GuiItem(xpBuilder.build(), event -> {
            if (event.getClick() == ClickType.CONTROL_DROP) {
                if (finalIsLevelMode) {
                    this.questConfig.set(this.yamlPath + ".xp", (Object)finalXp);
                } else {
                    this.questConfig.set(this.yamlPath + ".xp", null);
                    this.questConfig.set(this.yamlPath + ".xp.levels", (Object)finalXp);
                }
                this.markDirty();
                this.refresh();
            } else if (event.getClick() == ClickType.DROP && hasXp) {
                this.isNavigating = true;
                new QuestConfirmationGui(this.plugin, this.player, this.tr("quest_editor.rewards_menu.confirm_del_title"), this.tr("quest_editor.rewards_menu.confirm_del_desc"), () -> {
                    this.questConfig.set(this.yamlPath + ".xp", null);
                    this.markDirty();
                    this.refresh();
                }, this::open).open();
            } else {
                if (event.getClick() == ClickType.SWAP_OFFHAND) {
                    this.isNavigating = true;
                }
                EditorUtils.handleNumericClick(event, finalXp, 0, 9999999, this.player, this.tr("quest_editor.rewards_menu.prompt_xp"), this.plugin.getChatInputManager(), val -> {
                    if (finalIsLevelMode) {
                        this.questConfig.set(this.yamlPath + ".xp", null);
                        this.questConfig.set(this.yamlPath + ".xp.levels", val);
                    } else {
                        this.questConfig.set(this.yamlPath + ".xp", val);
                    }
                    this.markDirty();
                }, () -> this.open());
            }
        }));
        boolean hasMoney = this.questConfig.contains(this.yamlPath + ".money");
        double money = 0.0;
        if (hasMoney) {
            if (this.questConfig.isConfigurationSection(this.yamlPath + ".money")) {
                money = this.questConfig.getDouble(this.yamlPath + ".money.amount", 0.0);
            } else {
                money = this.questConfig.getDouble(this.yamlPath + ".money", 0.0);
            }
        }

        final double finalMoney = money;
        HexItemBuilder moneyBuilder = HexItemBuilder.from(Material.GOLD_INGOT).name(ColorFormatter.format(hasMoney ? this.tr("quest_editor.rewards_menu.money_title_has") : this.tr("quest_editor.rewards_menu.money_title_none"))).lore(ColorFormatter.format(hasMoney ? this.tr("quest_editor.rewards_menu.val_has", Map.of("val", String.valueOf(money))) : this.tr("quest_editor.rewards_menu.val_none")), "", this.plugin.getLanguage().trEditorList("quest_editor.main_menu.slot_click")).flags(ItemFlag.HIDE_ATTRIBUTES);
        if (hasMoney) {
            moneyBuilder.lore(ColorFormatter.format(this.tr("quest_editor.rewards_menu.click_del")));
            moneyBuilder.enchant(Enchantment.DURABILITY);
        }
        this.gui.setItem(2, 5, new GuiItem(moneyBuilder.build(), event -> {
            if (event.getClick() == ClickType.DROP && hasMoney) {
                this.isNavigating = true;
                new QuestConfirmationGui(this.plugin, this.player, this.tr("quest_editor.rewards_menu.confirm_del_title"), this.tr("quest_editor.rewards_menu.confirm_del_desc"), () -> {
                    this.questConfig.set(this.yamlPath + ".money", null);
                    this.markDirty();
                    this.refresh();
                }, this::open).open();
            } else {
                if (event.getClick() == ClickType.SWAP_OFFHAND) {
                    this.isNavigating = true;
                }
                EditorUtils.handleDoubleNumericClick(event, finalMoney, 0.0, 9999999.0, this.player, this.tr("quest_editor.rewards_menu.prompt_money"), this.plugin.getChatInputManager(), val -> {
                    if (this.questConfig.isConfigurationSection(this.yamlPath + ".money")) {
                        this.questConfig.set(this.yamlPath + ".money.amount", val);
                    } else {
                        this.questConfig.set(this.yamlPath + ".money", val);
                    }
                    this.markDirty();
                }, () -> this.open());
            }
        }));
        int itemsCount = 0;
        if (this.questConfig.isList(this.yamlPath + ".items.list")) {
            itemsCount = this.questConfig.getList(this.yamlPath + ".items.list").size();
        } else if (this.questConfig.isList(this.yamlPath + ".items")) {
            itemsCount = this.questConfig.getList(this.yamlPath + ".items").size();
        }
        HexItemBuilder itemsBuilder = HexItemBuilder.from(Material.DIAMOND_SWORD).name(ColorFormatter.format(this.tr("quest_editor.rewards_menu.items_title"))).lore(ColorFormatter.format(this.tr("quest_editor.rewards_menu.items_lore", Map.of("count", String.valueOf(itemsCount)))), "", ColorFormatter.format(this.tr("quest_editor.rewards_menu.items_click"))).flags(ItemFlag.HIDE_ATTRIBUTES);
        ItemStack itemsItem = itemsBuilder.build();
        this.gui.setItem(2, 6, new GuiItem(itemsItem, event -> {
            this.isNavigating = true;
            QuestRequiredMaterialsGui reqGui = new QuestRequiredMaterialsGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath + ".items", this.tr("quest_editor.rewards_menu.items_gui"), () -> this.open());
            reqGui.setOnDirty(this::markDirty);
            reqGui.setAddButtonName(this.tr("quest_editor.rewards_menu.add_item"));
            reqGui.setAddButtonLore(List.of(this.tr("quest_editor.rewards_menu.add_item_lore")));
            reqGui.open();
        }));
        int cmdsCount = 0;
        if (this.questConfig.isList(this.yamlPath + ".commands.list")) {
            cmdsCount = this.questConfig.getList(this.yamlPath + ".commands.list").size();
        } else if (this.questConfig.isList(this.yamlPath + ".commands")) {
            cmdsCount = this.questConfig.getList(this.yamlPath + ".commands").size();
        }
        HexItemBuilder cmdsBuilder = HexItemBuilder.from(Material.COMMAND_BLOCK).name(ColorFormatter.format(this.tr("quest_editor.rewards_menu.cmd_title"))).lore(ColorFormatter.format(this.tr("quest_editor.rewards_menu.cmd_lore", Map.of("count", String.valueOf(cmdsCount)))), "", ColorFormatter.format(this.tr("quest_editor.rewards_menu.cmd_click"))).flags(ItemFlag.HIDE_ATTRIBUTES);
        ItemStack cmdsItem = cmdsBuilder.build();
        this.gui.setItem(4, 4, new GuiItem(cmdsItem, event -> {
            this.isNavigating = true;
            new QuestCommandRewardGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath + ".commands", () -> this.open()).setOnDirty(this::markDirty).open();
        }));
        int effsCount = 0;
        if (this.questConfig.isList(this.yamlPath + ".effects.list")) {
            effsCount = this.questConfig.getList(this.yamlPath + ".effects.list").size();
        } else if (this.questConfig.isList(this.yamlPath + ".effects")) {
            effsCount = this.questConfig.getList(this.yamlPath + ".effects").size();
        }
        HexItemBuilder effsBuilder = HexItemBuilder.from(Material.POTION).name(ColorFormatter.format(this.tr("quest_editor.rewards_menu.eff_title"))).lore(ColorFormatter.format(this.tr("quest_editor.rewards_menu.eff_lore", Map.of("count", String.valueOf(effsCount)))), "", ColorFormatter.format(this.tr("quest_editor.rewards_menu.eff_click"))).flags(ItemFlag.HIDE_ATTRIBUTES);
        ItemStack effsItem = effsBuilder.build();
        this.gui.setItem(3, 4, new GuiItem(effsItem, event -> {
            this.isNavigating = true;
            new QuestEffectRewardGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath + ".effects.list", () -> this.open()).setOnDirty(this::markDirty).open();
        }));
        int msgCount = 0;
        if (this.questConfig.isList(this.yamlPath + ".message.list")) {
            msgCount = this.questConfig.getList(this.yamlPath + ".message.list").size();
        } else if (this.questConfig.isList(this.yamlPath + ".message")) {
            msgCount = this.questConfig.getList(this.yamlPath + ".message").size();
        }
        HexItemBuilder msgBuilder = HexItemBuilder.from(Material.PAPER).name(ColorFormatter.format(this.tr("quest_editor.rewards_menu.msg_title"))).lore(ColorFormatter.format(this.tr("quest_editor.rewards_menu.msg_lore", Map.of("count", String.valueOf(msgCount)))), "", ColorFormatter.format(this.tr("quest_editor.rewards_menu.msg_click"))).flags(ItemFlag.HIDE_ATTRIBUTES);
        ItemStack msgItem = msgBuilder.build();
        this.gui.setItem(4, 5, new GuiItem(msgItem, event -> {
            this.isNavigating = true;
            new QuestMessageRewardGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath + ".message.list", () -> this.open()).setOnDirty(this::markDirty).open();
        }));
        int brdCount = 0;
        if (this.questConfig.isList(this.yamlPath + ".broadcast.list")) {
            brdCount = this.questConfig.getList(this.yamlPath + ".broadcast.list").size();
        } else if (this.questConfig.isList(this.yamlPath + ".broadcast")) {
            brdCount = this.questConfig.getList(this.yamlPath + ".broadcast").size();
        }
        HexItemBuilder brdBuilder = HexItemBuilder.from(Material.OAK_SIGN).name(ColorFormatter.format(this.tr("quest_editor.rewards_menu.brd_title"))).lore(ColorFormatter.format(this.tr("quest_editor.rewards_menu.brd_lore", Map.of("count", String.valueOf(brdCount)))), "", ColorFormatter.format(this.tr("quest_editor.rewards_menu.brd_click"))).flags(ItemFlag.HIDE_ATTRIBUTES);
        ItemStack brdItem = brdBuilder.build();
        this.gui.setItem(4, 6, new GuiItem(brdItem, event -> {
            this.isNavigating = true;
            new QuestBroadcastRewardGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath + ".broadcast.list", () -> this.open()).setOnDirty(this::markDirty).open();
        }));
        HexItemBuilder chanceBuilder = HexItemBuilder.from(Material.GHAST_TEAR).name(ColorFormatter.format(this.tr("quest_editor.rewards_menu.chance_title"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.rewards_menu.chance_lore"), "", ColorFormatter.format(this.tr("quest_editor.rewards_menu.chance_click"))).flags(ItemFlag.HIDE_ATTRIBUTES);
        ItemStack chanceItem = chanceBuilder.build();
        this.gui.setItem(3, 6, new GuiItem(chanceItem, event -> {
            this.isNavigating = true;
            new QuestChanceRewardGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath + ".chance", this.tr("quest_editor.rewards_menu.chance_gui_title"), () -> this.open()).setOnDirty(this::markDirty).open();
        }));
        HexItemBuilder attrBuilder = HexItemBuilder.from(Material.IRON_CHESTPLATE).name(ColorFormatter.format(this.tr("quest_editor.rewards_menu.attr_title"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.rewards_menu.attr_lore"), "", ColorFormatter.format(this.tr("quest_editor.rewards_menu.attr_click"))).flags(ItemFlag.HIDE_ATTRIBUTES);
        ItemStack attrItem = attrBuilder.build();
        this.gui.setItem(3, 5, new GuiItem(attrItem, event -> {
            this.isNavigating = true;
            new QuestAttributeRewardGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath + ".attributes", this.tr("quest_editor.rewards_menu.attrs_gui"), () -> this.open()).setOnDirty(this::markDirty).open();
        }));
        boolean isSilent = this.questConfig.getBoolean(this.yamlPath + ".silent", false);
        ItemStack silentItem = HexItemBuilder.from(Material.BELL).name(ColorFormatter.format(this.tr("quest_editor.rewards_menu.silent_title"))).lore(ColorFormatter.format(this.tr("quest_editor.rewards_menu.silent_lore")), "", ColorFormatter.format(this.tr("quest_editor.rewards_menu.silent_click_sub"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        if (isSilent) {
            silentItem.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        this.gui.setItem(5, 9, new GuiItem(silentItem, event -> {
            this.isNavigating = true;
            new QuestSilentSettingsGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath, () -> this.open()).setOnDirty(this::markDirty).open();
        }));
    }
}
