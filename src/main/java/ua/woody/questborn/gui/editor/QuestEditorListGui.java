package ua.woody.questborn.gui.editor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.GuiUtils;
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.MainEditorSelectorGui;
import ua.woody.questborn.gui.editor.QuestConfirmationGui;
import ua.woody.questborn.gui.editor.QuestMainEditorGui;
import ua.woody.questborn.gui.editor.QuestTypeSelectorGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import ua.woody.questborn.model.EngineType;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestEditorListGui {
    private static final Map<UUID, Integer> sessionModeIndex = new HashMap<UUID, Integer>();
    private static final Map<UUID, String> sessionSearchQuery = new HashMap<UUID, String>();
    private final QuestbornPlugin plugin;
    private final Player player;
    private PaginatedGui gui;
    private String searchQuery = null;
    private int currentModeIndex = 0;
    private List<ViewMode> modes = new ArrayList<ViewMode>();
    private Map<String, Long> creationDates = new HashMap<String, Long>();
    private Map<String, Long> modificationDates = new HashMap<String, Long>();
    private boolean isNavigating = false;

    public QuestEditorListGui(QuestbornPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.initModes();
        this.currentModeIndex = sessionModeIndex.getOrDefault(player.getUniqueId(), 0);
        if (this.currentModeIndex >= this.modes.size()) {
            this.currentModeIndex = 0;
        }
        this.searchQuery = sessionSearchQuery.get(player.getUniqueId());
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

    private File getQuestFile(QuestDefinition quest) {
        String folderName = "quests";
        QuestTypeConfig typeCfg = this.plugin.getQuestManager().getQuestTypeManager().getType(quest.getTypeId());
        if (typeCfg != null) {
            folderName = typeCfg.getFolder();
        }
        return new File(this.plugin.getDataFolder(), folderName + "/" + quest.getId() + ".yml");
    }

    private EngineType getEngine(QuestDefinition q) {
        QuestTypeConfig typeCfg = this.plugin.getQuestManager().getQuestTypeManager().getType(q.getTypeId());
        return typeCfg != null ? typeCfg.getEngine() : EngineType.DEFAULT;
    }

    private long getCreationDate(QuestDefinition q) {
        return this.creationDates.computeIfAbsent(q.getId(), k -> {
            File f = this.getQuestFile(q);
            try {
                return Files.readAttributes(f.toPath(), BasicFileAttributes.class, new LinkOption[0]).creationTime().toMillis();
            }
            catch (Exception e) {
                return 0L;
            }
        });
    }

    private long getModificationDate(QuestDefinition q) {
        return this.modificationDates.computeIfAbsent(q.getId(), k -> {
            File f = this.getQuestFile(q);
            return f.lastModified();
        });
    }

    private boolean hasIssue(QuestDefinition q) {
        File f = this.getQuestFile(q);
        if (!f.exists()) {
            return true;
        }
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration((File)f);
            if (!cfg.contains("name") || cfg.getString("name", "").isEmpty()) {
                return true;
            }
            if (EditorUtils.getStageCount(cfg) == 0) {
                return true;
            }
            if (!cfg.contains("rewards") || cfg.getConfigurationSection("rewards") == null || cfg.getConfigurationSection("rewards").getKeys(false).isEmpty()) {
                return true;
            }
        }
        catch (Exception ignored) {
            return true;
        }
        return false;
    }

    private void initModes() {
        this.modes.add(new ViewMode(this.tr("quest_editor.list.mode_type"), Comparator.comparing(QuestDefinition::getTypeId).thenComparing(QuestDefinition::getId), q -> true));
        this.modes.add(new ViewMode(this.tr("quest_editor.list.mode_engine_default"), Comparator.comparing(QuestDefinition::getId), q -> this.getEngine((QuestDefinition)q) == EngineType.DEFAULT));
        this.modes.add(new ViewMode(this.tr("quest_editor.list.mode_engine_chain"), Comparator.comparing(QuestDefinition::getId), q -> this.getEngine((QuestDefinition)q) == EngineType.CHAIN));
        this.modes.add(new ViewMode(this.tr("quest_editor.list.mode_engine_rotation"), Comparator.comparing(QuestDefinition::getId), q -> this.getEngine((QuestDefinition)q) == EngineType.ROTATION));
        this.modes.add(new ViewMode(this.tr("quest_editor.list.mode_engine_global"), Comparator.comparing(QuestDefinition::getId), q -> this.getEngine((QuestDefinition)q) == EngineType.GLOBAL));
        this.modes.add(new ViewMode(this.tr("quest_editor.list.mode_issue"), Comparator.comparing(QuestDefinition::getId), q -> this.hasIssue((QuestDefinition)q)));
        this.modes.add(new ViewMode(this.tr("quest_editor.list.mode_mod_new"), (q1, q2) -> Long.compare(this.getModificationDate((QuestDefinition)q2), this.getModificationDate((QuestDefinition)q1)), q -> true));
        this.modes.add(new ViewMode(this.tr("quest_editor.list.mode_mod_old"), (q1, q2) -> Long.compare(this.getModificationDate((QuestDefinition)q1), this.getModificationDate((QuestDefinition)q2)), q -> true));
        this.modes.add(new ViewMode(this.tr("quest_editor.list.mode_cre_new"), (q1, q2) -> Long.compare(this.getCreationDate((QuestDefinition)q2), this.getCreationDate((QuestDefinition)q1)), q -> true));
        this.modes.add(new ViewMode(this.tr("quest_editor.list.mode_cre_old"), (q1, q2) -> Long.compare(this.getCreationDate((QuestDefinition)q1), this.getCreationDate((QuestDefinition)q2)), q -> true));
        this.modes.add(new ViewMode(this.tr("quest_editor.list.mode_id_asc"), Comparator.comparing(QuestDefinition::getId), q -> true));
        this.modes.add(new ViewMode(this.tr("quest_editor.list.mode_id_desc"), Comparator.comparing(QuestDefinition::getId).reversed(), q -> true));
    }

    public void open() {
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        this.isNavigating = true;
        List<QuestDefinition> filteredQuests = this.getFilteredQuests();
        String title = ColorFormatter.applyColors(this.tr("quest_editor.title_list", Map.of("count", String.valueOf(filteredQuests.size()))));
        this.gui = ((PaginatedBuilder)((PaginatedBuilder)((PaginatedBuilder)Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(6)).pageSize(45).disableAllInteractions()).create();
        this.gui.setCloseGuiAction(event -> {
            if (!this.isNavigating && !EditorSessionManager.isForceClosing) {
                sessionModeIndex.remove(this.player.getUniqueId());
                sessionSearchQuery.remove(this.player.getUniqueId());
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").build();
        this.gui.getFiller().fillBottom(new GuiItem(bg));
        ItemStack createBtn = HexItemBuilder.from(Material.EMERALD_BLOCK).name(ColorFormatter.format(this.tr("common_editor.buttons.add_new"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 5, new GuiItem(createBtn, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInput(this.player, EditorChat.formatPrompt(this.tr("quest_editor.prompts.enter_id")), input -> {
                if (input != null && !input.isEmpty()) {
                    File file;
                    File questsDir;
                    input = input.toLowerCase().replace(" ", "_");
                    String folder = "quests";
                    List<QuestTypeConfig> types = this.plugin.getQuestManager().getQuestTypeManager().getEnabledTypes();
                    if (!types.isEmpty()) {
                        folder = types.get(0).getFolder();
                    }
                    if (!(questsDir = new File(this.plugin.getDataFolder(), folder)).exists()) {
                        questsDir.mkdirs();
                    }
                    if ((file = new File(questsDir, input + ".yml")).exists()) {
                        EditorChat.sendError(this.player, this.tr("quest_editor.prompts.id_exists", Map.of("id", input)));
                        this.open();
                    } else {
                        try {
                            YamlConfiguration config = new YamlConfiguration();
                            config.set("id", input);
                            config.set("name", input);
                            config.save(file);
                            this.plugin.getQuestManager().reload();
                            String questInputId = input;
                            File initialFile = file;
                            YamlConfiguration initialConfig = config;
                            new QuestTypeSelectorGui(this.plugin, this.player, questInputId, initialFile, initialConfig, () -> new QuestMainEditorGui(this.plugin, this.player, questInputId).open()).open();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            EditorChat.sendError(this.player, this.tr("quest_editor.list.error_create"));
                        }
                    }
                } else {
                    this.open();
                }
            });
        }));
        ArrayList<String> filterLore = new ArrayList<String>();
        filterLore.add(ColorFormatter.format(this.tr("quest_editor.list.filter_lore_title")));
        for (int i = 0; i < this.modes.size(); ++i) {
            if (i == this.currentModeIndex) {
                filterLore.add(ColorFormatter.format(" <#ffffff>\u25b6 " + this.modes.get((int)i).name));
                continue;
            }
            filterLore.add(ColorFormatter.format(" <#cccccc>  " + this.modes.get((int)i).name));
        }
        filterLore.add("");
        filterLore.add(ColorFormatter.format(this.tr("quest_editor.list.filter_lore_next")));
        filterLore.add(ColorFormatter.format(this.tr("quest_editor.list.filter_lore_prev")));
        ItemStack filterItem = HexItemBuilder.from(Material.HOPPER).name(ColorFormatter.format(this.tr("quest_editor.list.filter_title"))).lore(filterLore).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 2, new GuiItem(filterItem, event -> {
            if (event.isLeftClick()) {
                ++this.currentModeIndex;
                if (this.currentModeIndex >= this.modes.size()) {
                    this.currentModeIndex = 0;
                }
                sessionModeIndex.put(this.player.getUniqueId(), this.currentModeIndex);
                this.isNavigating = true;
                this.open();
            } else if (event.isRightClick()) {
                --this.currentModeIndex;
                if (this.currentModeIndex < 0) {
                    this.currentModeIndex = this.modes.size() - 1;
                }
                sessionModeIndex.put(this.player.getUniqueId(), this.currentModeIndex);
                this.isNavigating = true;
                this.open();
            }
        }));
        String searchLore = this.searchQuery == null ? this.tr("quest_editor.list.search_all") : this.tr("quest_editor.list.search_query", Map.of("query", this.searchQuery));
        ItemStack searchItem = HexItemBuilder.from(Material.COMPASS).name(ColorFormatter.format(this.tr("common_editor.buttons.search"))).lore(ColorFormatter.format(searchLore), "", ColorFormatter.format(this.tr("quest_editor.list.search_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 8, new GuiItem(searchItem, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_editor.list.search_prompt")), this.searchQuery == null ? "" : this.searchQuery, input -> {
                if (input != null) {
                    if (input.equalsIgnoreCase("clear") || input.isBlank()) {
                        this.searchQuery = null;
                        sessionSearchQuery.remove(this.player.getUniqueId());
                        this.open();
                    } else {
                        this.searchQuery = input.toLowerCase();
                        sessionSearchQuery.put(this.player.getUniqueId(), this.searchQuery);
                        this.open();
                    }
                } else {
                    this.open();
                }
            });
        }));
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            new MainEditorSelectorGui(this.plugin, this.player).open();
        }));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 4, new GuiItem(prevItem, event -> this.gui.previous()));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(6, 6, new GuiItem(nextItem, event -> this.gui.next()));
        this.populateQuests(filteredQuests);
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }

    private List<QuestDefinition> getFilteredQuests() {
        Collection<QuestDefinition> allQuests = this.plugin.getQuestManager().getAll();
        ArrayList<QuestDefinition> filteredQuests = new ArrayList<QuestDefinition>();
        ViewMode currentMode = this.modes.get(this.currentModeIndex);
        for (QuestDefinition quest : allQuests) {
            if (this.searchQuery != null) {
                boolean matchName;
                boolean matchId = quest.getId().toLowerCase().contains(this.searchQuery);
                boolean bl = matchName = quest.getDisplayName() != null && quest.getDisplayName().toLowerCase().contains(this.searchQuery);
                if (!matchId && !matchName) continue;
            }
            if (!currentMode.filter.test(quest)) continue;
            filteredQuests.add(quest);
        }
        filteredQuests.sort(currentMode.comparator);
        return filteredQuests;
    }

    private void populateQuests(List<QuestDefinition> filteredQuests) {
        for (QuestDefinition quest : filteredQuests) {
            String iconId = quest.getIconItemsAdderId() != null ? "ia:" + quest.getIconItemsAdderId() : (quest.getIconCraftEngineId() != null ? "ce:" + quest.getIconCraftEngineId() : (quest.getIconMaterial() != null ? quest.getIconMaterial().name() : "PAPER"));
            ItemStack iconBaseItem = EditorUtils.getGuiItemForString(iconId, this.plugin);
            if (iconBaseItem == null || iconBaseItem.getType() == Material.AIR) {
                iconBaseItem = new ItemStack(Material.PAPER);
            }
            HexItemBuilder builder = HexItemBuilder.from(iconBaseItem).name(ColorFormatter.format("<#ffd470>" + quest.getDisplayName())).lore(ColorFormatter.format("<#a8a8a8>ID: <#ffffff>" + quest.getId()), ColorFormatter.format(this.tr("quest_editor.list.item_type", Map.of("type", quest.getTypeId()))), ColorFormatter.format(this.tr("quest_editor.list.item_engine", Map.of("engine", this.getEngine(quest).name()))), "", ColorFormatter.format(this.tr("quest_editor.list.item_edit")), ColorFormatter.format(this.tr("quest_editor.list.item_clone")), ColorFormatter.format(this.tr("quest_editor.list.item_delete"))).flags(ItemFlag.HIDE_ATTRIBUTES);
            if (quest.getIconCustomModelData() != null) {
                builder.model(quest.getIconCustomModelData());
            }
            ItemStack finalItem = builder.build();
            if (iconBaseItem.getType() == Material.PLAYER_HEAD && quest.getIconBaseHead() != null) {
                GuiUtils.applyBaseHead(finalItem, quest.getIconBaseHead());
            }
            this.gui.addItem(new GuiItem(finalItem, event -> {
                if (event.getClick() == ClickType.DROP) {
                    this.isNavigating = true;
                    new QuestConfirmationGui(this.plugin, this.player, this.tr("quest_editor.list.confirm_del_title"), this.tr("quest_editor.list.confirm_del_desc", Map.of("id", quest.getId())), () -> {
                        File f;
                        String folderName = "quests";
                        QuestTypeConfig typeCfg = this.plugin.getQuestManager().getQuestTypeManager().getType(quest.getTypeId());
                        if (typeCfg != null) {
                            folderName = typeCfg.getFolder();
                        }
                        if ((f = new File(this.plugin.getDataFolder(), folderName + "/" + quest.getId() + ".yml")).exists()) {
                            f.delete();
                        }
                        this.plugin.getQuestManager().reload();
                        this.player.sendMessage(ColorFormatter.applyColors(this.tr("quest_editor.list.msg_del_success", Map.of("id", quest.getId()))));
                        this.open();
                    }, () -> this.open()).open();
                } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
                    this.isNavigating = true;
                    String folderNameRaw = "quests";
                    QuestTypeConfig typeCfg = this.plugin.getQuestManager().getQuestTypeManager().getType(quest.getTypeId());
                    if (typeCfg != null) {
                        folderNameRaw = typeCfg.getFolder();
                    }
                    String folderName = folderNameRaw;
                    File qFile = new File(this.plugin.getDataFolder(), folderName + "/" + quest.getId() + ".yml");
                    String baseName = quest.getId();
                    this.isNavigating = true;
                    this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_editor.list.clone_prompt")), baseName + "_copy", input -> {
                        if (input != null && !input.isEmpty()) {
                            input = input.toLowerCase().replace(" ", "_");
                            File srcFile = new File(this.plugin.getDataFolder(), folderName + "/" + quest.getId() + ".yml");
                            File destDir = new File(this.plugin.getDataFolder(), folderName);
                            File destFile = new File(destDir, input + ".yml");
                            if (destFile.exists()) {
                                this.player.sendMessage(ColorFormatter.applyColors(this.tr("quest_editor.list.error_clone_exists")));
                                this.open();
                            } else {
                                try {
                                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration((File)srcFile);
                                    cfg.set("id", input);
                                    cfg.save(destFile);
                                    this.plugin.getQuestManager().reload();
                                    this.player.sendMessage(ColorFormatter.applyColors(this.tr("quest_editor.list.msg_clone_success", Map.of("id", input))));
                                    new QuestMainEditorGui(this.plugin, this.player, (String)input).open();
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                    this.player.sendMessage(ColorFormatter.applyColors(this.tr("quest_editor.list.error_clone")));
                                    this.open();
                                }
                            }
                        } else {
                            this.open();
                        }
                    });
                } else if (event.isLeftClick()) {
                    this.isNavigating = true;
                    new QuestMainEditorGui(this.plugin, this.player, quest.getId()).open();
                }
            }));
        }
    }

    private static class ViewMode {
        String name;
        Comparator<QuestDefinition> comparator;
        Predicate<QuestDefinition> filter;

        ViewMode(String name, Comparator<QuestDefinition> comparator, Predicate<QuestDefinition> filter) {
            this.name = name;
            this.comparator = comparator;
            this.filter = filter;
        }
    }
}
