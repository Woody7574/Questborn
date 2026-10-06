package ua.woody.questborn.gui.editor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import ua.woody.questborn.gui.editor.DialogueEditorListGui;
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.gui.editor.NpcVoiceSelectorGui;
import ua.woody.questborn.gui.editor.QuestConfirmationGui;
import ua.woody.questborn.gui.editor.QuestListStringEditorGui;
import ua.woody.questborn.gui.editor.UnsavedChangesConfirmGui;
import ua.woody.questborn.lang.ColorFormatter;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.SimpleBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import ua.woody.questborn.utils.HexItemBuilder;

public class DialogueMainEditorGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String dialogueId;
    private final File dialogueFile;
    private final YamlConfiguration config;
    private String pendingDialogueId = null;
    private boolean isNavigating = false;
    private Gui gui;

    public DialogueMainEditorGui(QuestbornPlugin plugin, Player player, String dialogueId) {
        this.plugin = plugin;
        this.player = player;
        this.dialogueId = dialogueId;
        this.dialogueFile = new File(plugin.getDataFolder(), "npc/dialogues.yml");
        this.config = YamlConfiguration.loadConfiguration((File)this.dialogueFile);
    }

    public DialogueMainEditorGui(QuestbornPlugin plugin, Player player, String dialogueId, String pendingId, boolean hasChanges) {
        this(plugin, player, dialogueId);
        this.pendingDialogueId = pendingId;
        if (hasChanges) EditorSessionManager.markUnsaved(this.player.getUniqueId());
        else EditorSessionManager.clearUnsaved(this.player.getUniqueId());
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
        this.isNavigating = true;
        String displayId = this.pendingDialogueId != null ? this.pendingDialogueId : this.dialogueId;
        String title = ColorFormatter.applyColors(EditorUtils.truncateGuiTitle(this.tr("dialogue_editor.title_main", Map.of("id", displayId))) + (EditorSessionManager.hasUnsaved(this.player.getUniqueId()) ? " *" : ""));
        this.gui = ((SimpleBuilder)((SimpleBuilder)((SimpleBuilder)Gui.gui().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(5)).disableAllInteractions()).create();
        EditorSessionManager.setSession(this.player.getUniqueId(), this::handleEscClose);
        this.gui.setCloseGuiAction(event -> {
            if (!this.isNavigating && !EditorSessionManager.isForceClosing) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, this::handleEscClose);
            }
        });
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.getFiller().fill(new GuiItem(bg));
        this.setupButtons();
        ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, () -> {
            this.isNavigating = true;
            this.gui.open((HumanEntity)this.player);
            this.isNavigating = false;
        });
    }

    private void openTutorialGui() {
        Gui tutorialGui = Gui.gui()
                .title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors(this.tr("quest_editor.dialogue_main.dialogue_tutorial_title")))))
                .rows(5)
                .disableAllInteractions()
                .create();

        tutorialGui.setCloseGuiAction(event -> {
            ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, this::open);
        });

        tutorialGui.getFiller().fill(new GuiItem(HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build()));

        List<String> step1Lore = new ArrayList<>();
        for (String line : this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.tutorial_step1_lore")) {
            step1Lore.add("§r" + line);
        }
        ItemStack step1 = HexItemBuilder.skull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2ZlNmNhNmJmM2JmYjc1OTJlZGZlNTJiYjc4YmNhOGNkMDliNmJkOGRlNTYyZGZlMThlMzkxY2I0MTg2MmQifX19")
            .name(ColorFormatter.format(this.tr("quest_editor.dialogue_main.tutorial_step1_title")))
            .lore(step1Lore)
            .flags(ItemFlag.HIDE_ATTRIBUTES).build();

        List<String> step2Lore = new ArrayList<>();
        for (String line : this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.tutorial_step2_lore")) {
            step2Lore.add("§r" + line);
        }
        ItemStack step2 = HexItemBuilder.skull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDBjNGY4ODk3N2RjOTFhZTc0MmIyM2MzNWM2MzhlNDQ0ZTY5Mzg3Zjc2MDhhYThlNmQzNTVjYjMxNDdhIn19fQ==")
            .name(ColorFormatter.format(this.tr("quest_editor.dialogue_main.tutorial_step2_title")))
            .lore(step2Lore)
            .flags(ItemFlag.HIDE_ATTRIBUTES).build();

        List<String> step3Lore = new ArrayList<>();
        for (String line : this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.tutorial_step3_lore")) {
            step3Lore.add("§r" + line);
        }
        ItemStack step3 = HexItemBuilder.skull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzJmMjkyNmY5YjQ4NWZmMjIyOWM1M2MxMzUzYzFlNzlhZDc3MWNjNzEzNjM4ZjhjM2U1MWMyNmViMzQyYjUifX19")
            .name(ColorFormatter.format(this.tr("quest_editor.dialogue_main.tutorial_step3_title")))
            .lore(step3Lore)
            .flags(ItemFlag.HIDE_ATTRIBUTES).build();

        List<String> step4Lore = new ArrayList<>();
        for (String line : this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.tutorial_step4_lore")) {
            step4Lore.add("§r" + line);
        }
        ItemStack step4 = HexItemBuilder.skull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmIxMjVjNjU3NmRjZmU2YTg4Nzk1OGQ3MzU4NDVkM2JhZTc0ZGNmYjU1ZjBiODgzNGU4YzkxNWJjZmFmNzAifX19")
            .name(ColorFormatter.format(this.tr("quest_editor.dialogue_main.tutorial_step4_title")))
            .lore(step4Lore)
            .flags(ItemFlag.HIDE_ATTRIBUTES).build();

        List<String> step5Lore = new ArrayList<>();
        for (String line : this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.tutorial_step5_lore")) {
            step5Lore.add("§r" + line);
        }
        ItemStack step5 = HexItemBuilder.skull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTM3MmRhMzJiZjc3N2E5YWYxZTNhNzczZGFlYzQyN2ZiNzE4ZTI3N2ZmNTZjZTJkMmZhYjMyNWZkYzhmOTUxNSJ9fX0=")
            .name(ColorFormatter.format(this.tr("quest_editor.dialogue_main.tutorial_step5_title")))
            .lore(step5Lore)
            .flags(ItemFlag.HIDE_ATTRIBUTES).build();

        List<String> step6Lore = new ArrayList<>();
        for (String line : this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.tutorial_step6_lore")) {
            step6Lore.add("§r" + line);
        }
        ItemStack step6 = HexItemBuilder.skull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzFmZjRhOTQxY2UyYjBjMGRmZGJiZWRjYzJkY2I4ZjdkYmZhYzJjM2U1YzZhOTc2NDQ3OTJmYjJmMTM5YWQifX19")
            .name(ColorFormatter.format(this.tr("quest_editor.dialogue_main.tutorial_step6_title")))
            .lore(step6Lore)
            .flags(ItemFlag.HIDE_ATTRIBUTES).build();

        List<String> step7Lore = new ArrayList<>();
        for (String line : this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.tutorial_step7_lore")) {
            step7Lore.add("§r" + line);
        }
        ItemStack step7 = HexItemBuilder.skull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzkxODU1ZDhiZGM5Yjg0MTlhNGFiMmEyMmJmMmYyZjU4N2MwYmI1ZDM0NDJiZjEzZDJkZWZmYWVkODE0MCJ9fX0=")
            .name(ColorFormatter.format(this.tr("quest_editor.dialogue_main.tutorial_step7_title")))
            .lore(step7Lore)
            .flags(ItemFlag.HIDE_ATTRIBUTES).build();

        tutorialGui.setItem(2, 2, new GuiItem(step1));
        tutorialGui.setItem(2, 3, new GuiItem(step2));
        tutorialGui.setItem(2, 4, new GuiItem(step3));
        tutorialGui.setItem(2, 5, new GuiItem(step4));
        tutorialGui.setItem(2, 6, new GuiItem(step5));
        tutorialGui.setItem(2, 7, new GuiItem(step6));
        tutorialGui.setItem(2, 8, new GuiItem(step7));

        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        tutorialGui.setItem(5, 1, new GuiItem(backItem, event -> {
            ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, this::open);
        }));

        tutorialGui.open((HumanEntity)this.player);
    }

    public void handleEscClose() {
        if (EditorSessionManager.hasUnsaved(this.player.getUniqueId())) {
            new UnsavedChangesConfirmGui(this.player, () -> {
                if (this.saveConfig()) {
                    EditorSessionManager.clearSession(this.player.getUniqueId());
                    new DialogueEditorListGui(this.plugin, this.player).open();
                } else {
                    this.refresh();
                }
            }, () -> {
                EditorSessionManager.clearSession(this.player.getUniqueId());
                new DialogueEditorListGui(this.plugin, this.player).open();
            }, () -> {
                Runnable returnAction = EditorSessionManager.getReturnAction(this.player.getUniqueId());
                if (returnAction != null) {
                    returnAction.run();
                } else {
                    this.open();
                }
            }).open();
        } else {
            EditorSessionManager.clearSession(this.player.getUniqueId());
        }
    }

    private void refresh() {
        this.isNavigating = true;
        this.open();
    }

    private ConfigurationSection getDialogueSection() {
        ConfigurationSection sec;
        ConfigurationSection dlgSec = this.config.getConfigurationSection("dialogues");
        if (dlgSec == null) {
            dlgSec = this.config.createSection("dialogues");
        }
        if ((sec = dlgSec.getConfigurationSection(this.dialogueId)) == null) {
            sec = dlgSec.createSection(this.dialogueId);
        }
        return sec;
    }

    private boolean saveConfig() {
        ConfigurationSection dlgSec;
        if (this.pendingDialogueId != null && (dlgSec = this.config.getConfigurationSection("dialogues")) != null && dlgSec.contains(this.dialogueId) && !this.dialogueId.equalsIgnoreCase(this.pendingDialogueId)) {
            Object oldData = dlgSec.get(this.dialogueId);
            dlgSec.set(this.dialogueId, null);
            dlgSec.set(this.pendingDialogueId, oldData);
        }
        try {
            this.config.save(this.dialogueFile);
            this.plugin.getDialogueManager().reload();
            EditorSessionManager.clearUnsaved(this.player.getUniqueId());
            return true;
        }
        catch (IOException e) {
            EditorChat.sendError(this.player, this.tr("quest_editor.dialogue_main.save_error", Map.of("msg", e.getMessage())));
            return false;
        }
    }

    private void setupButtons() {
        ConfigurationSection sec = this.getDialogueSection();
        String displayId = this.pendingDialogueId != null ? this.pendingDialogueId : this.dialogueId;
        ItemStack idItem = HexItemBuilder.from(Material.IRON_NUGGET).name(ColorFormatter.format(this.tr("quest_editor.dialogue_main.id_title"))).lore(ColorFormatter.format(this.tr("quest_editor.dialogue_main.id_lore", Map.of("id", displayId))), "", ColorFormatter.format(this.tr("quest_editor.dialogue_main.id_click"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 3, new GuiItem(idItem, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInputWithSuggestion(this.player, EditorChat.formatPrompt(this.tr("quest_editor.dialogue_main.id_prompt")), displayId, input -> {
                this.isNavigating = false;
                if (input != null && !input.isBlank()) {
                    String sanitized = input.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
                    if (sanitized.equalsIgnoreCase(displayId)) {
                        this.refresh();
                        return;
                    }
                    ConfigurationSection dlgSec = this.config.getConfigurationSection("dialogues");
                    if (dlgSec != null && dlgSec.contains(sanitized) && !sanitized.equalsIgnoreCase(this.dialogueId)) {
                        this.player.sendMessage(ColorFormatter.applyColors(this.tr("quest_editor.dialogue_main.id_exists", Map.of("id", sanitized))));
                        this.refresh();
                        return;
                    }
                    this.pendingDialogueId = sanitized;
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                }
                this.open();
            });
        }));

        int cpt = sec.getInt("cpt", 1);
        List<String> cptLore = new ArrayList<>();
        cptLore.addAll(this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.cpt_lore", Map.of("cpt", String.valueOf(cpt))));
        cptLore.add(" ");
        cptLore.addAll(this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.cpt_click"));
        ItemStack cptItem = HexItemBuilder.from(Material.FEATHER).name(ColorFormatter.format(this.tr("quest_editor.dialogue_main.cpt_title"))).lore(cptLore).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(3, 4, new GuiItem(cptItem, event -> {
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                this.isNavigating = true;
            }
            EditorUtils.handleNumericClick(event, cpt, 1, 5, this.player, EditorChat.formatPrompt(this.tr("quest_editor.dialogue_main.cpt_prompt")), this.plugin.getChatInputManager(), val -> {
                sec.set("cpt", val);
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
            }, this::refresh);
        }));

        long updatePeriod = sec.getLong("update_period", 1L);
        List<String> upLore = new ArrayList<>();
        upLore.addAll(this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.update_period_lore", Map.of("ticks", String.valueOf(updatePeriod))));
        upLore.add(" ");
        upLore.addAll(this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.update_period_click"));
        ItemStack updatePeriodItem = HexItemBuilder.from(Material.CLOCK).name(ColorFormatter.format(this.tr("quest_editor.dialogue_main.update_period_title"))).lore(upLore).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(3, 6, new GuiItem(updatePeriodItem, event -> {
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                this.isNavigating = true;
            }
            EditorUtils.handleNumericClick(event, (int)updatePeriod, 1, 100, this.player, EditorChat.formatPrompt(this.tr("quest_editor.dialogue_main.update_period_prompt")), this.plugin.getChatInputManager(), val -> {
                sec.set("update_period", val.longValue());
                EditorSessionManager.markUnsaved(this.player.getUniqueId());
            }, this::refresh);
        }));

        ConfigurationSection nodesSec = sec.getConfigurationSection("nodes");
        Set nodeKeys = nodesSec != null ? nodesSec.getKeys(false) : Set.of();
        int nodeCount = nodeKeys.size();
        ArrayList<String> nodesLore = new ArrayList<String>();
        nodesLore.addAll(this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.nodes_lore", Map.of("count", String.valueOf(nodeCount))));
        nodesLore.addAll(this.formatKeysLore(nodeKeys, 4));
        nodesLore.add("");
        nodesLore.add(ColorFormatter.format(this.tr("quest_editor.dialogue_main.nodes_click")));
        ItemStack nodesItem = HexItemBuilder.from(Material.TARGET).name(ColorFormatter.format(this.tr("quest_editor.dialogue_main.nodes_title"))).lore(nodesLore).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 5, new GuiItem(nodesItem, event -> {
            this.isNavigating = true;
            this.openNodesSelector();
        }));

        ItemStack validatorItem = HexItemBuilder.from(Material.ANVIL).name(ColorFormatter.format(this.tr("quest_editor.dialogue_main.dialogue_validator_title"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.dialogue_validator_lore")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(5, 5, new GuiItem(validatorItem, event -> {
            this.isNavigating = true;
            this.player.closeInventory();
            ConfigurationSection nodes = this.getDialogueSection().getConfigurationSection("nodes");
            int errors = 0;
            List<String> errorMessages = new ArrayList<>();
            if (nodes == null || !nodes.contains("start")) {
                errorMessages.add(this.tr("quest_editor.dialogue_main.validator_start_missing"));
                errors++;
            }
            if (nodes != null) {
                for (String nodeKey : nodes.getKeys(false)) {
                    List<Map<?, ?>> opts = nodes.getMapList(nodeKey + ".options");
                    for (int i = 0; i < opts.size(); i++) {
                        String act = (String) opts.get(i).get("action");
                        if (act == null) act = (String) opts.get(i).get("goto");
                        if (act != null && act.startsWith("dialogue:")) {
                            String target = act.substring(9);
                            if (!nodes.contains(target)) {
                                errorMessages.add(this.tr("quest_editor.dialogue_main.validator_dead_link", Map.of("node", nodeKey, "target", target, "index", String.valueOf(i+1))));
                                errors++;
                            }
                        }
                    }
                }
            }
            if (errors == 0) {
                EditorChat.sendSuccess(this.player, this.tr("quest_editor.dialogue_main.validator_links_ok"));
            } else {
                this.player.sendMessage(ColorFormatter.applyColors(EditorChat.getPrefix() + this.tr("quest_editor.dialogue_main.validator_header", Map.of("id", this.dialogueId))));
                for (String err : errorMessages) {
                    EditorChat.sendError(this.player, err);
                }
                this.player.sendMessage(ColorFormatter.applyColors(EditorChat.getPrefix() + this.tr("quest_editor.dialogue_main.validator_footer", Map.of("errors", String.valueOf(errors), "warnings", "0"))));
            }
        }));

        ItemStack tutorialItem = HexItemBuilder.from(Material.WRITTEN_BOOK).name(ColorFormatter.format(this.tr("quest_editor.dialogue_main.dialogue_tutorial_item_title"))).lore(this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.dialogue_tutorial_lore")).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(2, 7, new GuiItem(tutorialItem, event -> {
            this.isNavigating = true;
            this.openTutorialGui();
        }));

        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(5, 1, new GuiItem(backItem, event -> {
            this.isNavigating = true;
            ua.woody.questborn.utils.SchedulerUtils.runTask(this.plugin, this.player, this::handleBack);
        }));

        String saveName = this.tr("quest_editor.main_menu.btn_save_title") + (EditorSessionManager.hasUnsaved(this.player.getUniqueId()) ? this.tr("quest_editor.main_menu.btn_save_changes") : "");
        String saveLore = EditorSessionManager.hasUnsaved(this.player.getUniqueId()) ? this.tr("quest_editor.main_menu.btn_save_lore_unsaved") : this.tr("quest_editor.main_menu.btn_save_lore_saved");
        ItemStack saveItem = HexItemBuilder.from(Material.EMERALD_BLOCK).name(ColorFormatter.format(saveName)).lore(ColorFormatter.format(saveLore)).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        this.gui.setItem(5, 9, new GuiItem(saveItem, event -> {
            if (!EditorSessionManager.hasUnsaved(this.player.getUniqueId())) {
                return;
            }
            if (this.saveConfig()) {
                String finalId = this.pendingDialogueId != null ? this.pendingDialogueId : this.dialogueId;
                EditorChat.sendSuccess(this.player, this.tr("dialogue_editor.saved_success", Map.of("id", finalId)));
                new DialogueMainEditorGui(this.plugin, this.player, finalId).open();
            } else {
                this.refresh();
            }
        }));
    }

    private List<String> formatKeysLore(Set<String> keys, int maxToShow) {
        ArrayList<String> list = new ArrayList<String>();
        if (keys == null || keys.isEmpty()) {
            list.add(ColorFormatter.format(this.tr("quest_editor.dialogue_main.keys_none")));
            return list;
        }
        ArrayList<String> keyList = new ArrayList<String>(keys);
        if (keyList.size() <= maxToShow) {
            list.add(ColorFormatter.format(this.tr("quest_editor.dialogue_main.keys_list", Map.of("keys", String.join((CharSequence)", ", keyList)))));
        } else {
            List firstFew = keyList.subList(0, maxToShow);
            list.add(ColorFormatter.format(this.tr("quest_editor.dialogue_main.keys_list", Map.of("keys", String.join((CharSequence)", ", firstFew) + this.tr("quest_editor.dialogue_main.keys_more", Map.of("count", String.valueOf(keyList.size() - maxToShow)))))));
        }
        return list;
    }

    private void handleBack() {
        if (EditorSessionManager.hasUnsaved(this.player.getUniqueId())) {
            new UnsavedChangesConfirmGui(this.player, () -> {
                if (this.saveConfig()) {
                    new DialogueEditorListGui(this.plugin, this.player).open();
                } else {
                    this.refresh();
                }
            }, () -> new DialogueEditorListGui(this.plugin, this.player).open(), () -> {
                Runnable returnAction = EditorSessionManager.getReturnAction(this.player.getUniqueId());
                if (returnAction != null) {
                    returnAction.run();
                } else {
                    this.open();
                }
            }).open();
        } else {
            new DialogueEditorListGui(this.plugin, this.player).open();
        }
    }

    private List<String> wrapPreviewLine(String text, int maxLineLen) {
        List<String> wrapped = new ArrayList<>();
        if (text == null || text.isBlank()) return wrapped;

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        StringBuilder lastTags = new StringBuilder();
        int currentLen = 0;

        for (String word : words) {
            String cleanWord = word.replaceAll("<[^>]*>", "").replaceAll("&[0-9a-fk-orA-FK-OR]", "");
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("<[^>]*>|&[0-9a-fk-orA-FK-OR]").matcher(word);
            while (m.find()) {
                lastTags.append(m.group());
            }

            if (currentLen + cleanWord.length() > maxLineLen && currentLen > 0) {
                wrapped.add(currentLine.toString().trim());
                currentLine.setLength(0);
                currentLine.append(lastTags.toString());
                currentLen = 0;
            }

            currentLine.append(word).append(" ");
            currentLen += cleanWord.length() + 1;
        }

        if (currentLine.length() > 0) {
            String trimmed = currentLine.toString().trim();
            if (!trimmed.isEmpty() && !trimmed.equals(lastTags.toString().trim())) {
                wrapped.add(trimmed);
            }
        }

        return wrapped;
    }

    private void openNodesSelector() {
        ConfigurationSection nodesSec = this.getDialogueSection().getConfigurationSection("nodes");
        dev.triumphteam.gui.guis.PaginatedGui nodesGui = dev.triumphteam.gui.guis.Gui.paginated().title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors(EditorUtils.formatGuiTitle(this.tr("quest_editor.dialogue_main.nodes_gui_title"), this.dialogueId, ""))))).rows(6).pageSize(45).disableAllInteractions().create();
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        nodesGui.getFiller().fillBottom(new GuiItem(bg));
        if (nodesSec != null) {
            for (String nodeKey : nodesSec.getKeys(false)) {
                List<String> textLines = nodesSec.getStringList(nodeKey + ".text");
                ArrayList<String> lore = new ArrayList<String>();
                lore.add(ColorFormatter.format(this.tr("quest_editor.dialogue_main.nodes_lines", Map.of("count", String.valueOf(textLines.size())))));

                lore.add("");
                lore.addAll(this.plugin.getLanguage().trEditorList("quest_editor.dialogue_main.nodes_item_click"));
                Material iconMat = nodeKey.equalsIgnoreCase("start") ? Material.HEART_OF_THE_SEA : Material.PRISMARINE_CRYSTALS;
                String displayName = nodeKey.equalsIgnoreCase("start") ? this.tr("quest_editor.dialogue_main.nodes_item_start") : this.tr("quest_editor.dialogue_main.nodes_item_normal", Map.of("node", nodeKey));
                ItemStack nodeItem = HexItemBuilder.from(iconMat).name(ColorFormatter.applyColors(displayName)).lore(lore).flags(ItemFlag.HIDE_ATTRIBUTES).build();
                nodesGui.addItem(new GuiItem(nodeItem, event -> {
                    this.isNavigating = true;
                    if (event.getClick() == ClickType.DROP) {
                        new QuestConfirmationGui(this.plugin, this.player, this.tr("quest_editor.dialogue_main.nodes_del_confirm_title"), this.tr("quest_editor.dialogue_main.nodes_del_confirm_desc", Map.of("node", nodeKey)), () -> {
                            nodesSec.set(nodeKey, null);
                            EditorSessionManager.markUnsaved(this.player.getUniqueId());
                            this.player.sendMessage(ColorFormatter.applyColors(this.tr("quest_editor.dialogue_main.nodes_del_success", Map.of("node", nodeKey))));
                            this.openNodesSelector();
                        }, () -> this.openNodesSelector()).open();
                    } else {
                        new DialogueNodeEditorGui(this.plugin, this.player, this.dialogueId, nodeKey, this.dialogueFile, this.config, () -> {
                            EditorSessionManager.markUnsaved(this.player.getUniqueId());
                        }, this::openNodesSelector).open();
                    }
                }));
            }
        }
        ItemStack backBtn = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        nodesGui.setItem(6, 1, new GuiItem(backBtn, event -> this.open()));
        ItemStack prevItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.prev_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        nodesGui.setItem(6, 4, new GuiItem(prevItem, event -> nodesGui.previous()));
        ItemStack addNodeBtn = HexItemBuilder.from(Material.EMERALD_BLOCK).name(ColorFormatter.format(this.tr("quest_editor.dialogue_main.nodes_add_title"))).lore(ColorFormatter.format(this.tr("quest_editor.dialogue_main.nodes_add_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        nodesGui.setItem(6, 5, new GuiItem(addNodeBtn, event -> {
            this.isNavigating = true;
            this.plugin.getChatInputManager().requestInput(this.player, EditorChat.formatPrompt(this.tr("quest_editor.dialogue_main.nodes_add_prompt")), input -> {
                if (input != null && !input.isBlank()) {
                    String nodeKey = input.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
                    this.getDialogueSection().set("nodes." + nodeKey + ".text", List.of(this.tr("quest_editor.dialogue_main.nodes_add_default_text")));
                    this.getDialogueSection().set("nodes." + nodeKey + ".sound", (Object)"ENTITY_VILLAGER_AMBIENT");
                    EditorSessionManager.markUnsaved(this.player.getUniqueId());
                    this.player.sendMessage(ColorFormatter.applyColors(this.tr("quest_editor.dialogue_main.nodes_add_success", Map.of("node", nodeKey))));
                    this.openNodesSelector();
                } else {
                    this.openNodesSelector();
                }
            });
        }));
        ItemStack nextItem = HexItemBuilder.from(Material.ARROW).name(ColorFormatter.format(this.tr("common_editor.buttons.next_page"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        nodesGui.setItem(6, 6, new GuiItem(nextItem, event -> nodesGui.next()));
        nodesGui.open((HumanEntity)this.player);
    }

    private Runnable createBackAction() {
        String stateBefore = this.config.saveToString();
        String idBefore = this.pendingDialogueId;
        return () -> {
            boolean changes;
            boolean bl = changes = EditorSessionManager.hasUnsaved(this.player.getUniqueId()) || !Objects.equals(idBefore, this.pendingDialogueId);
            if (!stateBefore.equals(this.config.saveToString())) {
                changes = true;
            }
            new DialogueMainEditorGui(this.plugin, this.player, this.dialogueId, this.pendingDialogueId, changes).open();
        };
    }
}
