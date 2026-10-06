package ua.woody.questborn.gui.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.SimpleBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestRewardTypeSelectorGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String questId;
    private final File questFile;
    private final YamlConfiguration questConfig;
    private final String yamlPath;
    private final Runnable backAction;
    private Gui gui;
    private Runnable onDirty = null;

    public QuestRewardTypeSelectorGui setOnDirty(Runnable onDirty) {
        this.onDirty = onDirty;
        return this;
    }

    public QuestRewardTypeSelectorGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig, String yamlPath, Runnable backAction) {
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

    public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        this.gui = ((SimpleBuilder)((SimpleBuilder)((SimpleBuilder)Gui.gui().title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors("&0" + this.tr("quest_editor.rewards_menu.selector_title")))))).rows(3)).disableAllInteractions()).create();
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fill(new GuiItem(bg));
        this.setupButtons();
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> this.gui.open((HumanEntity)this.player));
    }

    private void saveConfig() {
        if (this.onDirty != null) {
            this.onDirty.run();
        }
    }

    private void setupButtons() {
        ItemStack handItem = HexItemBuilder.from(Material.HOPPER).name(ColorFormatter.format(this.tr("quest_editor.rewards_menu.hand_btn_title"))).lore(ColorFormatter.format(this.tr("quest_editor.rewards_menu.hand_btn_lore")), ColorFormatter.format(this.tr("quest_editor.rewards_menu.hand_btn_lore2")), "", ColorFormatter.format(this.tr("common_editor.buttons.confirm_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 2, new GuiItem(handItem, event -> {
            ItemStack inHand = this.player.getInventory().getItemInMainHand();
            if (inHand != null && inHand.getType() != Material.AIR) {
                List rawList = this.questConfig.getList(this.yamlPath + ".items");
                ArrayList<String> list = rawList == null ? new ArrayList<>() : new ArrayList<>(rawList);
                list.add(inHand.getType().name() + ";" + inHand.getAmount());
                this.questConfig.set(this.yamlPath + ".items", list);
                this.saveConfig();
                EditorChat.sendSuccess(this.player, this.tr("quest_editor.rewards_menu.hand_added", Map.of("name", inHand.getType().name(), "amount", String.valueOf(inHand.getAmount()))));
            } else {
                EditorChat.sendError(this.player, this.tr("quest_editor.rewards_menu.hand_empty"));
            }
            this.backAction.run();
        }));
        ItemStack itemsItem = HexItemBuilder.from(Material.DIAMOND_SWORD).name(ColorFormatter.format(this.tr("quest_editor.rewards_menu.selector_items_title"))).lore(ColorFormatter.format(this.tr("quest_editor.rewards_menu.selector_items_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 3, new GuiItem(itemsItem, event -> {
            if (!this.questConfig.contains(this.yamlPath + ".items")) {
                this.questConfig.set(this.yamlPath + ".items", new ArrayList());
            }
            this.saveConfig();
            this.backAction.run();
        }));
        ItemStack cmdItem = HexItemBuilder.from(Material.COMMAND_BLOCK).name(ColorFormatter.format(this.tr("quest_editor.rewards_menu.selector_cmd_title"))).lore(ColorFormatter.format(this.tr("quest_editor.rewards_menu.selector_cmd_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 4, new GuiItem(cmdItem, event -> {
            if (!this.questConfig.contains(this.yamlPath + ".commands")) {
                this.questConfig.set(this.yamlPath + ".commands", new ArrayList());
            }
            this.saveConfig();
            this.backAction.run();
        }));
        ItemStack xpItem = HexItemBuilder.from(Material.EXPERIENCE_BOTTLE).name(ColorFormatter.format(this.tr("quest_editor.rewards_menu.selector_xp_title"))).lore(ColorFormatter.format(this.tr("quest_editor.rewards_menu.selector_xp_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 5, new GuiItem(xpItem, event -> this.plugin.getChatInputManager().requestInput(this.player, this.tr("quest_editor.rewards_menu.prompt_xp"), val -> {
            if (val != null) {
                try {
                    this.questConfig.set(this.yamlPath + ".xp", (Object)Integer.parseInt(val));
                    this.saveConfig();
                }
                catch (Exception exception) {
                }
            }
            this.backAction.run();
        })));
        ItemStack moneyItem = HexItemBuilder.from(Material.GOLD_INGOT).name(ColorFormatter.format(this.tr("quest_editor.rewards_menu.selector_money_title"))).lore(ColorFormatter.format(this.tr("quest_editor.rewards_menu.selector_money_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 6, new GuiItem(moneyItem, event -> this.plugin.getChatInputManager().requestInput(this.player, this.tr("quest_editor.rewards_menu.prompt_money"), val -> {
            if (val != null) {
                try {
                    this.questConfig.set(this.yamlPath + ".money", (Object)Double.parseDouble(val));
                    this.saveConfig();
                }
                catch (Exception exception) {
                }
            }
            this.backAction.run();
        })));
        ItemStack customItem = HexItemBuilder.from(Material.NAME_TAG).name(ColorFormatter.format(this.tr("quest_editor.rewards_menu.selector_custom_title"))).lore(ColorFormatter.format(this.tr("quest_editor.rewards_menu.selector_custom_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 7, new GuiItem(customItem, event -> this.plugin.getChatInputManager().requestInput(this.player, this.tr("quest_editor.rewards_menu.prompt_custom_key"), key -> {
            if (key != null && !key.isEmpty()) {
                this.plugin.getChatInputManager().requestInput(this.player, this.tr("quest_editor.rewards_menu.prompt_custom_val", Map.of("key", key)), val -> {
                    if (val != null) {
                        block6: {
                            try {
                                this.questConfig.set(this.yamlPath + "." + key, (Object)Integer.parseInt(val));
                            }
                            catch (NumberFormatException e) {
                                try {
                                    this.questConfig.set(this.yamlPath + "." + key, (Object)Double.parseDouble(val));
                                }
                                catch (NumberFormatException e2) {
                                    if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false")) {
                                        this.questConfig.set(this.yamlPath + "." + key, (Object)Boolean.parseBoolean(val));
                                        break block6;
                                    }
                                    this.questConfig.set(this.yamlPath + "." + key, val);
                                }
                            }
                        }
                        this.saveConfig();
                    }
                    this.backAction.run();
                });
            } else {
                this.backAction.run();
            }
        })));
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(3, 1, new GuiItem(backItem, event -> this.backAction.run()));
    }
}
