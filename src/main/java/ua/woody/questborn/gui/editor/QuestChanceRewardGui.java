package ua.woody.questborn.gui.editor;

import java.io.File;
import java.io.IOException;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.QuestConfirmationGui;
import ua.woody.questborn.gui.editor.QuestEditorListGui;
import ua.woody.questborn.gui.editor.QuestRewardsGui;
import ua.woody.questborn.gui.editor.UnsavedChangesConfirmGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestChanceRewardGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String questId;
    private final File questFile;
    private final YamlConfiguration questConfig;
    private final String yamlPath;
    private final String title;
    private final Runnable onBack;
    private PaginatedGui gui;
    private Runnable onDirty;
    private boolean isNavigating = false;
    private boolean hasChanges = false;

    public QuestChanceRewardGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig, String yamlPath, String title, Runnable onBack) {
        this.plugin = plugin;
        this.player = player;
        this.questId = questId;
        this.questFile = questFile;
        this.questConfig = questConfig;
        this.yamlPath = yamlPath;
        this.title = title;
        this.onBack = onBack;
        this.setupGui();
    }

    public QuestChanceRewardGui setOnDirty(Runnable onDirty) {
        this.onDirty = onDirty;
        return this;
    }

    private void markDirty() {
        this.hasChanges = true;
        if (this.onDirty != null) {
            this.onDirty.run();
        }
    }

    private String tr(String key) {
        return this.plugin.getLanguage().trEditor(key);
    }

    private void saveConfig() {
        try {
            this.questConfig.save(this.questFile);
            this.markDirty();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }

    private void refresh() {
        this.populateList();
    }

    private void setupGui() {
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.format(this.title))))).rows(6)).pageSize(45).create();
        this.gui.disableAllInteractions();
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
        this.populateList();
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            if (this.onBack != null) {
                this.onBack.run();
            }
        }));
        ItemStack addItem = HexItemBuilder.from(Material.EMERALD_BLOCK).name(ColorFormatter.format(this.tr("quest_editor.chance_reward.add_title"))).lore(ColorFormatter.format(this.tr("quest_editor.chance_reward.add_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 5, new GuiItem(addItem, event -> {
            ConfigurationSection sec = this.questConfig.getConfigurationSection(this.yamlPath);
            int nextId = 1;
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    if (key.equalsIgnoreCase("silent")) continue;
                    try {
                        int id = Integer.parseInt(key);
                        if (id < nextId) continue;
                        nextId = id + 1;
                    }
                    catch (Exception exception) {}
                }
            }
            this.questConfig.set(this.yamlPath + "." + nextId + ".percentage", (Object)50.0);
            this.saveConfig();
            this.refresh();
        }));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 4, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 6, new GuiItem(nextItem, event -> this.gui.next()));
    }

    private void populateList() {
        this.gui.clearPageItems();
        ConfigurationSection sec = this.questConfig.getConfigurationSection(this.yamlPath);
        boolean isEmpty = true;
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                if (key.equalsIgnoreCase("silent") || key.equalsIgnoreCase("list")) continue;
                isEmpty = false;
                double val = sec.getDouble(key + ".percentage", sec.getDouble(key + ".chance", 100.0));
                ItemStack item = HexItemBuilder.from(Material.GHAST_TEAR).name(ColorFormatter.format("<#ffd470>" + this.tr("quest_editor.chance_reward.item_title") + " #" + key + " <gray>-> <#a3ff7a>" + val + "%")).lore("", ColorFormatter.format(this.tr("quest_editor.chance_reward.click_open_rewards")), ColorFormatter.format(this.tr("quest_editor.chance_reward.click_edit_chance")), ColorFormatter.format(this.tr("quest_editor.chance_reward.click_delete"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
                this.gui.addItem(new GuiItem(item, event -> {
                    if (event.getClick() == ClickType.DROP) {
                        this.isNavigating = true;
                        new QuestConfirmationGui(this.plugin, this.player, this.tr("quest_editor.attribute_reward.confirm_del_title"), this.tr("quest_editor.attribute_reward.confirm_del_item"), this.tr("quest_editor.chance_reward.confirm_del_desc").replace("{attr}", key), () -> {
                            this.questConfig.set(this.yamlPath + "." + key, null);
                            this.saveConfig();
                            this.refresh();
                            this.open();
                        }, () -> this.open()).open();
                    } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
                        this.isNavigating = true;
                        this.promptValue(key, () -> {
                            this.refresh();
                            this.open();
                        });
                    } else if (event.isLeftClick() || event.isRightClick()) {
                        this.isNavigating = true;
                        String rewardsTitle = this.tr("quest_editor.chance_reward.rewards_gui_title").replace("{attr}", key);
                        new QuestRewardsGui(this.plugin, this.player, this.questId, this.questFile, this.questConfig, this.yamlPath + "." + key + ".rewards", rewardsTitle, () -> this.open()).setOnDirty(this::markDirty).open();
                    }
                }));
            }
        }
        if (isEmpty) {
            ItemStack emptyItem = HexItemBuilder.from(Material.STRUCTURE_VOID).name(ColorFormatter.format(this.tr("common_editor.item_list.empty_title"))).lore(this.plugin.getLanguage().trEditorList("common_editor.item_list.empty_lore")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(3, 5, new GuiItem(emptyItem));
        } else {
            this.gui.removeItem(3, 5);
        }
        this.gui.update();
    }

    private void promptValue(String key, Runnable andThen) {
        this.player.closeInventory();
        this.isNavigating = true;
        this.plugin.getChatInputManager().requestInput(this.player, this.tr("quest_editor.chance_reward.prompt_val"), input -> {
            if (input != null) {
                try {
                    double val = Double.parseDouble(input);
                    val = Math.max(0.0, Math.min(100.0, val));
                    this.questConfig.set(this.yamlPath + "." + key + ".percentage", (Object)val);
                    this.saveConfig();
                }
                catch (NumberFormatException numberFormatException) {
                }
            }
            if (andThen != null) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, andThen);
            }
        });
    }
}
