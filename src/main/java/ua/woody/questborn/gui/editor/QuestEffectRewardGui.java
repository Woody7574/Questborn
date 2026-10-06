package ua.woody.questborn.gui.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.QuestConfirmationGui;
import ua.woody.questborn.gui.editor.QuestEditorListGui;
import ua.woody.questborn.gui.editor.QuestPotionTypeSelectorGui;
import ua.woody.questborn.gui.editor.UnsavedChangesConfirmGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.util.TimeFormatter;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestEffectRewardGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String questId;
    private final File questFile;
    private final YamlConfiguration questConfig;
    private final String yamlPath;
    private final Runnable backAction;
    private Runnable onDirty = null;
    private boolean isNavigating = false;
    private boolean hasChanges = false;
    private PaginatedGui gui;

    public QuestEffectRewardGui(QuestbornPlugin plugin, Player player, String questId, File questFile, YamlConfiguration questConfig, String yamlPath, Runnable backAction) {
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

    public QuestEffectRewardGui setOnDirty(Runnable onDirty) {
        this.onDirty = onDirty;
        return this;
    }

    private void saveConfig() {
        this.hasChanges = true;
        if (this.onDirty != null) {
            this.onDirty.run();
        }
    }

    public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors("&0" + this.tr("quest_editor.rewards_menu.eff_gui")))))).rows(6)).pageSize(45).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            if (this.hasChanges && !this.isNavigating) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
                    this.isNavigating = true;
                    new UnsavedChangesConfirmGui(this.player, () -> {
                        EditorUtils.saveQuestConfig(this.plugin, this.questConfig, this.questFile);
                        this.hasChanges = false;
                        this.isNavigating = true;
                        new QuestEditorListGui(this.plugin, this.player).open();
                    }, () -> {
                        this.hasChanges = false;
                        this.isNavigating = true;
                        new QuestEditorListGui(this.plugin, this.player).open();
                    }, () -> {
                        Runnable returnAction = EditorSessionManager.getReturnAction(this.player.getUniqueId());
                        if (returnAction != null) {
                            returnAction.run();
                        } else {
                            this.open();
                        }
                    }).open();
                });
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        ItemStack addItem = HexItemBuilder.from(Material.EMERALD_BLOCK).name(ColorFormatter.format(this.tr("common_editor.buttons.add_new"))).lore(ColorFormatter.format(this.tr("quest_editor.eff_reward.add_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 5, new GuiItem(addItem, event -> {
            this.isNavigating = true;
            ArrayList<String> ignoredTypes = new ArrayList<String>();
            List currentRawList = this.questConfig.getList(this.yamlPath);
            if (currentRawList != null) {
                for (Object o : currentRawList) {
                    String lineStr = String.valueOf(o);
                    String[] pts = lineStr.split(":");
                    String tName = pts.length >= 4 && pts[0].equalsIgnoreCase("minecraft") ? pts[1] : (pts.length > 0 ? pts[0] : "UNKNOWN");
                    ignoredTypes.add(tName.toUpperCase(Locale.ROOT));
                }
            }
            new QuestPotionTypeSelectorGui(this.plugin, this.player, ignoredTypes, selectedType -> {
                List rawList = this.questConfig.getList(this.yamlPath);
                ArrayList<String> list = rawList == null ? new ArrayList<>() : new ArrayList<>(rawList);
                String cleanName = selectedType.getName().toUpperCase(Locale.ROOT);
                if (cleanName.startsWith("MINECRAFT:")) {
                    cleanName = cleanName.substring(10);
                }
                list.add(cleanName + ":0:10");
                this.questConfig.set(this.yamlPath, list);
                this.saveConfig();
                this.open();
            }, () -> this.open()).open();
        }));
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            this.backAction.run();
        }));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 3, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 7, new GuiItem(nextItem, event -> this.gui.next()));
        List effs = this.questConfig.getList(this.yamlPath);
        if (effs != null && !effs.isEmpty()) {
            for (int i = 0; i < effs.size(); ++i) {
                String dur;
                String amp;
                String typeName;
                int index = i;
                String line = String.valueOf(effs.get(i));
                String[] parts = line.split(":");
                if (parts.length >= 4 && parts[0].equalsIgnoreCase("minecraft")) {
                    typeName = parts[1];
                    amp = parts[2];
                    dur = parts[3];
                } else {
                    typeName = parts.length > 0 ? parts[0] : "UNKNOWN";
                    amp = parts.length > 1 ? parts[1] : "1";
                    dur = parts.length > 2 ? parts[2] : "200";
                }
                Material icon = Material.POTION;
                ArrayList<String> loreLines = new ArrayList<String>();
                loreLines.add(ColorFormatter.format(this.tr("quest_editor.eff_reward.type_format", Map.of("type", typeName.toUpperCase(Locale.ROOT)))));
                String displayLevel = amp;
                try {
                    int a = Integer.parseInt(amp);
                    displayLevel = String.valueOf(a + 1);
                }
                catch (Exception a) {
                }
                loreLines.add(ColorFormatter.format(this.tr("quest_editor.eff_reward.level_format", Map.of("level", displayLevel))));
                try {
                    int secs = Integer.parseInt(dur);
                    loreLines.add(ColorFormatter.format(this.tr("quest_editor.eff_reward.duration_format", Map.of("secs", String.valueOf(secs), "time", TimeFormatter.format(secs)))));
                }
                catch (Exception e) {
                    loreLines.add(ColorFormatter.format(this.tr("quest_editor.eff_reward.duration_format_raw", Map.of("dur", dur))));
                }
                loreLines.add("");
                loreLines.add(ColorFormatter.format(this.tr("quest_editor.eff_reward.click_add_1s")));
                loreLines.add(ColorFormatter.format(this.tr("quest_editor.eff_reward.click_remove_1s")));
                loreLines.add(ColorFormatter.format(this.tr("quest_editor.eff_reward.click_add_10s")));
                loreLines.add(ColorFormatter.format(this.tr("quest_editor.eff_reward.click_remove_10s")));
                loreLines.add(ColorFormatter.format(this.tr("quest_editor.eff_reward.click_custom_dur")));
                loreLines.add(ColorFormatter.format(this.tr("quest_editor.eff_reward.click_set_level")));
                loreLines.add(ColorFormatter.format(this.tr("quest_editor.eff_reward.click_delete")));
                ItemStack item = HexItemBuilder.from(icon).name(ColorFormatter.format(this.tr("quest_editor.eff_reward.item_format", Map.of("index", String.valueOf(index + 1))))).lore(loreLines.toArray(new String[0])).flags(ItemFlag.HIDE_ATTRIBUTES).build();
                try {
                    PotionMeta meta;
                    PotionEffectType pet = PotionEffectType.getByName((String)typeName);
                    if (pet != null && (meta = (PotionMeta)item.getItemMeta()) != null) {
                        meta.setColor(pet.getColor());
                        item.setItemMeta((ItemMeta)meta);
                    }
                }
                catch (Exception exception) {
                }
                this.gui.addItem(new GuiItem(item, event -> {
                    if (event.getClick() == ClickType.DROP) {
                        this.isNavigating = true;
                        new QuestConfirmationGui(this.plugin, this.player, this.tr("quest_editor.list_string_editor.delete_confirm_title"), this.tr("quest_editor.list_string_editor.delete_confirm_item"), this.tr("quest_editor.list_string_editor.delete_confirm_msg", Map.of("index", String.valueOf(index + 1))), Material.POTION, () -> {
                            List currentRaw = this.questConfig.getList(this.yamlPath);
                            if (currentRaw != null && index < currentRaw.size()) {
                                ArrayList currentList = new ArrayList(currentRaw);
                                currentList.remove(index);
                                this.questConfig.set(this.yamlPath, currentList);
                                this.saveConfig();
                            }
                            this.open();
                        }, () -> this.open()).open();
                    } else if (event.getClick() == ClickType.NUMBER_KEY) {
                        int hotbarButton = event.getHotbarButton();
                        if (hotbarButton >= 0 && hotbarButton <= 8) {
                            int newAmp = hotbarButton;
                            List currentRaw2 = this.questConfig.getList(this.yamlPath);
                            if (currentRaw2 != null && index < currentRaw2.size()) {
                                ArrayList<String> currentList = new ArrayList<>(currentRaw2);
                                currentList.set(index, typeName + ":" + newAmp + ":" + dur);
                                this.questConfig.set(this.yamlPath, currentList);
                                this.saveConfig();
                                this.open();
                            }
                        }
                    } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
                        this.isNavigating = true;
                        this.plugin.getChatInputManager().requestInput(this.player, EditorChat.formatPrompt(this.tr("quest_editor.eff_reward.prompt_dur")), input -> {
                            if (input != null && !input.isEmpty()) {
                                try {
                                    List currentRaw;
                                    int seconds = Integer.parseInt(input.trim());
                                    if (seconds < 1) {
                                        seconds = 1;
                                    }
                                    if ((currentRaw = this.questConfig.getList(this.yamlPath)) != null && index < currentRaw.size()) {
                                        ArrayList<String> currentList = new ArrayList<>(currentRaw);
                                        currentList.set(index, typeName + ":" + amp + ":" + seconds);
                                        this.questConfig.set(this.yamlPath, currentList);
                                        this.saveConfig();
                                    }
                                }
                                catch (NumberFormatException numberFormatException) {
                                }
                            }
                            this.open();
                        });
                    } else {
                        int deltaSecs = 0;
                        if (event.isLeftClick() && !event.isShiftClick()) {
                            deltaSecs = 1;
                        } else if (event.isRightClick() && !event.isShiftClick()) {
                            deltaSecs = -1;
                        } else if (event.isLeftClick() && event.isShiftClick()) {
                            deltaSecs = 10;
                        } else if (event.isRightClick() && event.isShiftClick()) {
                            deltaSecs = -10;
                        }
                        if (deltaSecs != 0) {
                            List currentRaw;
                            int currentSecs = 10;
                            try {
                                currentSecs = Integer.parseInt(dur);
                            }
                            catch (Exception currentRaw2) {
                            }
                            int newSecs = currentSecs + deltaSecs;
                            if (newSecs < 1) {
                                newSecs = 1;
                            }
                            if ((currentRaw = this.questConfig.getList(this.yamlPath)) != null && index < currentRaw.size()) {
                                ArrayList<String> currentList = new ArrayList<>(currentRaw);
                                currentList.set(index, typeName + ":" + amp + ":" + newSecs);
                                this.questConfig.set(this.yamlPath, currentList);
                                this.saveConfig();
                                this.open();
                            }
                        }
                    }
                }));
            }
        } else {
            ItemStack emptyItem = HexItemBuilder.from(Material.STRUCTURE_VOID).name(ColorFormatter.format(this.tr("common_editor.buttons.empty_list"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.empty_list_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
            this.gui.setItem(3, 5, new GuiItem(emptyItem));
        }
        this.isNavigating = true;
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }
}
