package ua.woody.questborn.gui.editor;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.lang.ColorFormatter;
import ua.woody.questborn.model.npc.mood.MoodRule;
import ua.woody.questborn.utils.HexItemBuilder;

import java.util.ArrayList;
import java.util.List;

public class NpcMoodRulesGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String npcId;
    private final ConfigurationSection npcSec;
    private final Runnable onDirty;
    private final Runnable backAction;
    private boolean isNavigating = false;

    public NpcMoodRulesGui(QuestbornPlugin plugin, Player player, String npcId, ConfigurationSection npcSec, Runnable onDirty, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.npcId = npcId;
        this.npcSec = npcSec;
        this.onDirty = onDirty;
        this.backAction = backAction;
    }

    private String tr(String path) {
        if (plugin != null && plugin.getLanguage() != null) {
            return plugin.getLanguage().trEditor(path);
        }
        return path;
    }

    private String tr(String path, java.util.Map<String, String> placeholders) {
        if (plugin != null && plugin.getLanguage() != null) {
            return plugin.getLanguage().trEditor(path, placeholders);
        }
        return path;
    }

    public void open() {
        dev.triumphteam.gui.guis.PaginatedGui gui = dev.triumphteam.gui.guis.Gui.paginated()
                .title(Component.text(EditorUtils.truncateGuiTitle(ColorFormatter.applyColors("&8Mood Rules: &1" + npcId))))
                .rows(6)
                .pageSize(45)
                .disableAllInteractions()
                .create();

        gui.setCloseGuiAction(event -> {
            if (!isNavigating && !EditorSessionManager.isForceClosing) {
                ua.woody.questborn.utils.SchedulerUtils.runTask(plugin, this.player, this.backAction);
            }
        });

        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.getFiller().fillBottom(new GuiItem(bg));

        ua.woody.questborn.model.NpcConfig npcCfg = plugin.getNpcManager().getConfigByNpcId(npcId);
        List<MoodRule> rules = npcCfg != null ? npcCfg.getMoodRules() : new ArrayList<>();

        int idx = 1;
        for (MoodRule rule : rules) {
            List<String> lore = new ArrayList<>();
            lore.add(ColorFormatter.format("&7Priority: &e" + rule.getPriority()));
            lore.add(ColorFormatter.format("&7Conditions: &e" + (rule.getConditions() != null ? rule.getConditions().size() : 0)));
            lore.add("");
            lore.add(ColorFormatter.format(tr("quest_editor.npc_main.mood_rule_delete")));
            lore.add(ColorFormatter.format("&c(To add/edit, use npcs.yml for now)"));

            ItemStack item = HexItemBuilder.from(Material.SUNFLOWER)
                    .name(ColorFormatter.format("&eRule #" + idx + ": &6" + rule.getMood()))
                    .lore(lore)
                    .flags(ItemFlag.HIDE_ATTRIBUTES).build();

            final int fIdx = idx;
            gui.addItem(new GuiItem(item, event -> {
                if (event.getClick() == org.bukkit.event.inventory.ClickType.DROP) {
                    isNavigating = true;
                    new QuestConfirmationGui(plugin, player, "&cDelete Rule?", "&7Are you sure you want to delete this rule?", () -> {
                        player.sendMessage(ColorFormatter.applyColors("&cPlease delete mood rules directly in npcs.yml for safety."));
                        open();
                    }, this::open).open();
                }
            }));
            idx++;
        }

        ItemStack addBtn = HexItemBuilder.from(Material.EMERALD_BLOCK)
                .name(ColorFormatter.format(tr("quest_editor.npc_main.mood_rules_add_title")))
                .lore(ColorFormatter.format(tr("quest_editor.npc_main.mood_rules_add_lore")), ColorFormatter.format(tr("quest_editor.npc_main.mood_rules_add_lore2")))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(6, 5, new GuiItem(addBtn, event -> {
            player.sendMessage(ColorFormatter.applyColors("&ePlease open &fplugins/Questborn/npc/npcs.yml &eto configure mood rules."));
        }));

        ItemStack backBtn = HexItemBuilder.from(Material.OAK_DOOR)
                .name(ColorFormatter.format(tr("quest_editor.dialogue_main.action_back_title"))).lore(ColorFormatter.format("&7Return to NPC Editor"))
                .flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(6, 1, new GuiItem(backBtn, event -> {
            isNavigating = true;
            backAction.run();
        }));

        gui.open(player);
        this.isNavigating = false;
    }
}
