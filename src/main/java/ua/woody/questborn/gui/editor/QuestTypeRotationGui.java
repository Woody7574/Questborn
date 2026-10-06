package ua.woody.questborn.gui.editor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
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
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.QuestTypePoolsGui;
import ua.woody.questborn.lang.ColorFormatter;
import dev.triumphteam.gui.builder.gui.SimpleBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import ua.woody.questborn.util.TimeFormatter;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestTypeRotationGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String typeId;
    private final File typeFile;
    private final YamlConfiguration typeConfig;
    private final Runnable backAction;
    private final Consumer<Runnable> onEsc;
    private Gui gui;
    private Runnable onDirty = () -> {};
    private boolean isNavigating = false;

    public QuestTypeRotationGui(QuestbornPlugin plugin, Player player, String typeId, File typeFile, YamlConfiguration typeConfig, Runnable backAction, Consumer<Runnable> onEsc) {
        this.plugin = plugin;
        this.player = player;
        this.typeId = typeId;
        this.typeFile = typeFile;
        this.typeConfig = typeConfig;
        this.backAction = backAction;
        this.onEsc = onEsc;
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

    public QuestTypeRotationGui setOnDirty(Runnable onDirty) {
        this.onDirty = onDirty;
        return this;
    }

    private void saveConfig() {
        if (this.onDirty != null) {
            this.onDirty.run();
        }
    }

    public void refresh() {
        if (this.gui != null && !this.isNavigating) {
            this.setupButtons();
            this.gui.update();
        } else if (!this.isNavigating && !EditorSessionManager.isForceClosing) {
            this.open();
        }
    }

    public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        String title = EditorUtils.truncateGuiTitle(this.tr("quest_type_editor.rotation_gui.title", Map.of("id", this.typeId)));
        this.gui = ((SimpleBuilder)((SimpleBuilder)((SimpleBuilder)Gui.gui().title(ColorFormatter.formatComponent(title))).rows(3)).disableAllInteractions()).create();
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fill(new GuiItem(bg));
        this.setupButtons();
        this.gui.setCloseGuiAction(event -> {
            Runnable closeAction;
            if (!this.isNavigating && !EditorSessionManager.isForceClosing && (closeAction = EditorSessionManager.getSession(this.player.getUniqueId())) != null) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, closeAction);
            }
        });
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }

    private void setupButtons() {
        ConfigurationSection poolSec;
        String resetRaw = this.typeConfig.getString("reset-period", this.typeConfig.getString("reset-seconds", "0"));
        long resetSec = TimeFormatter.parseDuration(resetRaw);
        String formattedInterval = TimeFormatter.format(resetSec);
        if (resetSec == 0L) {
            formattedInterval = this.tr("quest_type_editor.main_menu.status_disabled");
        }
        ItemStack intervalItem = HexItemBuilder.from(Material.CLOCK).name(ColorFormatter.format(this.tr("quest_type_editor.rotation_gui.interval_title"))).lore(this.plugin.getLanguage().trEditorList("quest_type_editor.rotation_gui.interval_lore", Map.of("val", formattedInterval)), "", this.plugin.getLanguage().trEditorList("quest_type_editor.rotation_gui.interval_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 3, new GuiItem(intervalItem, event -> {
            if (event.getClick() == ClickType.LEFT) {
                this.typeConfig.set("reset-period", (Object)(resetSec + 3600L));
            } else if (event.getClick() == ClickType.RIGHT) {
                this.typeConfig.set("reset-period", (Object)Math.max(0L, resetSec - 3600L));
            } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                this.typeConfig.set("reset-period", (Object)(resetSec + 86400L));
            } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                this.typeConfig.set("reset-period", (Object)Math.max(0L, resetSec - 86400L));
            } else if (event.getClick() == ClickType.DROP) {
                this.typeConfig.set("reset-period", (Object)0);
            } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
                this.isNavigating = true;
                this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_type_editor.prompts.rotation_interval")), String.valueOf(resetSec), input -> {
                    this.isNavigating = false;
                    if (input != null) {
                        try {
                            this.typeConfig.set("reset-period", (Object)TimeFormatter.parseDuration(input.trim()));
                        }
                        catch (Exception e) {
                            try {
                                this.typeConfig.set("reset-period", (Object)Long.parseLong(input.trim()));
                            }
                            catch (Exception exception) {
                            }
                        }
                        if (this.typeConfig.contains("reset-seconds")) {
                            this.typeConfig.set("reset-seconds", null);
                        }
                        this.saveConfig();
                    }
                    this.open();
                });
                return;
            }
            if (this.typeConfig.contains("reset-seconds")) {
                this.typeConfig.set("reset-seconds", null);
            }
            this.saveConfig();
            this.refresh();
        }));
        String resetTime = this.typeConfig.getString("reset-time", null);
        ItemStack timeItem = HexItemBuilder.from(Material.COMPASS).name(ColorFormatter.format(this.tr("quest_type_editor.rotation_gui.time_title"))).lore(this.plugin.getLanguage().trEditorList("quest_type_editor.rotation_gui.time_lore", Map.of("val", resetTime != null ? resetTime : this.tr("quest_type_editor.main_menu.status_disabled"))), "", this.plugin.getLanguage().trEditorList("quest_type_editor.rotation_gui.time_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 4, new GuiItem(timeItem, event -> {
            int currentMinutes = 0;
            if (resetTime != null && resetTime.contains(":")) {
                String[] parts = resetTime.split(":");
                try {
                    currentMinutes = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
                }
                catch (Exception exception) {
                }
            }
            if (event.getClick() == ClickType.LEFT) {
                currentMinutes = (currentMinutes + 60) % 1440;
            } else if (event.getClick() == ClickType.RIGHT) {
                currentMinutes = (currentMinutes - 60 + 1440) % 1440;
            } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                currentMinutes = (currentMinutes + 10) % 1440;
            } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                currentMinutes = (currentMinutes - 10 + 1440) % 1440;
            } else {
                if (event.getClick() == ClickType.DROP) {
                    this.typeConfig.set("reset-time", null);
                    this.saveConfig();
                    this.refresh();
                    return;
                }
                if (event.getClick() == ClickType.SWAP_OFFHAND) {
                    this.isNavigating = true;
                    this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_type_editor.prompts.rotation_time")), resetTime != null ? resetTime : "00:00", input -> {
                        this.isNavigating = false;
                        if (input != null && input.contains(":")) {
                            this.typeConfig.set("reset-time", (Object)input.trim());
                            this.saveConfig();
                        }
                        this.open();
                    });
                    return;
                }
            }
            String newTime = String.format("%02d:%02d", currentMinutes / 60, currentMinutes % 60);
            this.typeConfig.set("reset-time", (Object)newTime);
            this.saveConfig();
            this.refresh();
        }));
        int anchorDay = this.typeConfig.getInt("reset-anchor-day", 1);
        String[] days = new String[]{this.tr("common_editor.days.monday"), this.tr("common_editor.days.tuesday"), this.tr("common_editor.days.wednesday"), this.tr("common_editor.days.thursday"), this.tr("common_editor.days.friday"), this.tr("common_editor.days.saturday"), this.tr("common_editor.days.sunday")};
        String dayName = anchorDay >= 1 && anchorDay <= 7 ? days[anchorDay - 1] : this.tr("common_editor.days.unknown");
        ItemStack anchorItem = HexItemBuilder.from(Material.HEART_OF_THE_SEA).name(ColorFormatter.format(this.tr("quest_type_editor.rotation_gui.anchor_title"))).lore(this.plugin.getLanguage().trEditorList("quest_type_editor.rotation_gui.anchor_lore", Map.of("val", dayName)), "", this.plugin.getLanguage().trEditorList("quest_type_editor.rotation_gui.anchor_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 5, new GuiItem(anchorItem, event -> {
            int nextDay = anchorDay + 1;
            if (nextDay > 7) {
                nextDay = 1;
            }
            this.typeConfig.set("reset-anchor-day", (Object)nextDay);
            this.saveConfig();
            this.refresh();
        }));
        HashMap<String, Integer> poolsMap = new HashMap<String, Integer>();
        if (this.typeConfig.isConfigurationSection("rotation-pools") && (poolSec = this.typeConfig.getConfigurationSection("rotation-pools")) != null) {
            for (String key : poolSec.getKeys(false)) {
                poolsMap.put(key, poolSec.getInt(key, 1));
            }
        }
        String poolsStr = poolsMap.isEmpty() ? this.tr("quest_type_editor.main_menu.status_disabled") : String.join((CharSequence)", ", poolsMap.keySet());
        ItemStack poolsItem = HexItemBuilder.from(Material.CHEST).name(ColorFormatter.format(this.tr("quest_type_editor.rotation_gui.pools_title"))).lore(this.plugin.getLanguage().trEditorList("quest_type_editor.rotation_gui.pools_lore", Map.of("val", poolsStr)), "", this.plugin.getLanguage().trEditorList("quest_type_editor.rotation_gui.pools_click")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 7, new GuiItem(poolsItem, event -> {
            this.isNavigating = true;
            new QuestTypePoolsGui(this.plugin, this.player, this.typeId, this.typeFile, this.typeConfig, () -> this.open(), this.onEsc).setOnDirty(this.onDirty).open();
        }));
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(3, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            this.backAction.run();
        }));
    }
}
