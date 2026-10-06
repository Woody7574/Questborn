package ua.woody.questborn.gui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.GuiUtils;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.gui.editor.EditorUtils;
import ua.woody.questborn.lang.ColorFormatter;
import ua.woody.questborn.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import dev.triumphteam.gui.builder.gui.SimpleBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import ua.woody.questborn.model.EngineType;
import ua.woody.questborn.model.GlobalQuestProgress;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.utils.HexItemBuilder;

public class QuestPreviewGui {
    private final QuestbornPlugin plugin;
    private final Player player;
    private final String questId;
    private final YamlConfiguration questConfig;
    private final Runnable backAction;

    public QuestPreviewGui(QuestbornPlugin plugin, Player player, String questId, YamlConfiguration questConfig, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.questId = questId;
        this.questConfig = questConfig;
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
        List<String> rewardsDesc = null;
        List reqQuests;
        List<String> desc = null;
        int stageCount;
        Material mat;
        EditorSessionManager.setReturnAction(this.player.getUniqueId(), this::open);
        String currentType = null;
        QuestDefinition questDef = this.plugin.getQuestManager().getQuest(this.questId);
        if (questDef != null) {
            currentType = questDef.getTypeId();
        } else {
            List<QuestTypeConfig> types = this.plugin.getQuestManager().getQuestTypeManager().getEnabledTypes();
            if (!types.isEmpty()) {
                currentType = types.get(0).getId();
            }
        }
        QuestTypeConfig typeConfig = currentType != null ? this.plugin.getQuestManager().getQuestTypeManager().getType(currentType) : null;
        EngineType engine = typeConfig != null ? typeConfig.getEngine() : EngineType.DEFAULT;
        String title = ColorFormatter.applyColors(EditorUtils.formatGuiTitle(this.tr("quest_editor.preview.title") + ": ", this.questId, ""));
        Gui gui = ((SimpleBuilder)((SimpleBuilder)((SimpleBuilder)Gui.gui().title(Component.text(EditorUtils.truncateGuiTitle(title)))).rows(5)).disableAllInteractions()).create();
        ItemStack bg = HexItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name("").flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.getFiller().fill(new GuiItem(bg));
        LanguageManager lang = this.plugin.getLanguage();
        String currentIcon = this.questConfig.getString("icon-material", null);
        if (currentIcon == null) {
            currentIcon = this.questConfig.getString("icon.material", null);
        }
        if (currentIcon == null && this.questConfig.isString("icon")) {
            currentIcon = this.questConfig.getString("icon", null);
        }
        if (currentIcon == null && questDef != null && questDef.getIconMaterial() != null) {
            currentIcon = questDef.getIconMaterial().name();
        }
        if (currentIcon == null || currentIcon.trim().isEmpty()) {
            currentIcon = "BOOK";
        }
        ItemStack itemStack = EditorUtils.getGuiItemForString(currentIcon, this.plugin);
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            itemStack = new ItemStack(Material.BOOK);
        } else {
            itemStack = itemStack.clone();
        }
        mat = itemStack.getType();
        ItemMeta meta = itemStack.getItemMeta();
        String rawName = this.questConfig.getString("name", this.questId);
        if (meta != null) {
            meta.setDisplayName(ColorFormatter.applyColors("<#ffe999>&l" + rawName));
            int currentCmd = this.questConfig.getInt("icon-custom-model-data", this.questConfig.getInt("custom-model-data", 0));
            if (currentCmd > 0) {
                meta.setCustomModelData(Integer.valueOf(currentCmd));
            }
        }
        ArrayList<String> lore = new ArrayList<String>();
        String statusText = lang != null ? lang.tr("gui.quest_list.status.available") : "<#99ffd4>Status: <#cccccc>Available";
        lore.add(ColorFormatter.applyColors(statusText));
        if (engine != EngineType.GLOBAL && this.questConfig.isConfigurationSection("quest-path") && (stageCount = this.questConfig.getConfigurationSection("quest-path").getKeys(false).size()) > 1) {
            Object stagesText = lang != null ? lang.tr("gui.quest_list.stages", Map.of("count", String.valueOf(stageCount))) : "<#a8a8a8>Stages: <#ffffff>" + stageCount;
            lore.add(ColorFormatter.applyColors((String)stagesText));
        }
        if (!(desc = this.questConfig.getStringList("description")).isEmpty()) {
            lore.add(" ");
            for (String line : desc) {
                lore.add(ColorFormatter.applyColors(line));
            }
        }
        if ((reqQuests = this.questConfig.getStringList("require-quests")).isEmpty()) {
            reqQuests = this.questConfig.getStringList("required-quests");
        }
        if (!reqQuests.isEmpty()) {
            lore.add(" ");
            String reqHeader = lang != null ? lang.tr("gui.quest_list.chain.required-quests") : "<#cccccc>Required quests:";
            lore.add(ColorFormatter.applyColors(reqHeader));
            for (Object reqId : reqQuests) {
                lore.add(ColorFormatter.applyColors(" <#ed6868>\u2717 <#ffffff>" + (String)reqId));
            }
        }
        if (this.plugin.getGuiConfig().isShowRewardsInList() && !(rewardsDesc = this.questConfig.getStringList("rewards-description")).isEmpty()) {
            lore.add(" ");
            String rHeader = lang.tr("gui.quest_list.rewards-header");
            lore.add(ColorFormatter.applyColors(rHeader));
            for (String rLine : rewardsDesc) {
                lore.add(ColorFormatter.applyColors("<#ffffff> " + rLine));
            }
        }
        if (engine == EngineType.GLOBAL) {
            GlobalQuestProgress gp;
            int globalGoal = 100;
            if (questDef != null) {
                globalGoal = this.plugin.getQuestManager().getProgressProcessor().getGlobalGoal(questDef);
            }
            int currentGlobal = (gp = this.plugin.getGlobalQuestDataStore().get(this.questId)) != null ? gp.getGlobalProgress() : 0;
            int currentP = gp != null ? gp.getParticipantCount() : 0;
            int myContribution = gp != null ? gp.getContribution(this.player.getUniqueId()) : 0;
            int maxP = this.questConfig.getInt("max-participants", 0);
            int minP = this.questConfig.getInt("min-participants", 0);
            int personalLimit = this.questConfig.getInt("personal-limit", 0);
            lore.add(" ");
            String gProgTitle = lang != null ? lang.tr("gui.quest_list.global-progress") : "<#ffd470>Global progress";
            lore.add(ColorFormatter.applyColors(gProgTitle));
            String gBar = this.plugin.getGuiConfig().getProgressBar(currentGlobal, globalGoal);
            int gPercent = globalGoal > 0 ? (int)((double)currentGlobal / (double)globalGoal * 100.0) : 0;
            String barText = lang != null ? lang.tr("gui.quest_details.info.progress.bar", Map.of("bar", gBar, "percent", String.valueOf(gPercent))) : gBar + " <#7dd3ff>" + gPercent + "%";
            String valText = lang != null ? lang.tr("gui.quest_details.info.progress.value", Map.of("current", String.valueOf(currentGlobal), "target", String.valueOf(globalGoal))) : "<#cccccc>(" + currentGlobal + " / " + globalGoal + ")";
            lore.add(ColorFormatter.applyColors(barText));
            lore.add(ColorFormatter.applyColors(valText));
            lore.add(" ");
            Object partText = maxP > 0 ? (minP > 0 && lang != null ? lang.tr("gui.quest_list.global-participants-min", Map.of("current", String.valueOf(currentP), "max", String.valueOf(maxP), "min", String.valueOf(minP))) : (lang != null ? lang.tr("gui.quest_list.global-participants", Map.of("current", String.valueOf(currentP), "max", String.valueOf(maxP))) : "<#a8a8a8>Participants: <#ffffff>" + currentP + " / " + maxP)) : (minP > 0 && lang != null ? lang.tr("gui.quest_list.global-participants-no-max-min", Map.of("current", String.valueOf(currentP), "min", String.valueOf(minP))) : (lang != null ? lang.tr("gui.quest_list.global-participants-no-max", Map.of("current", String.valueOf(currentP))) : "<#a8a8a8>Participants: <#ffffff>" + currentP));
            lore.add(ColorFormatter.applyColors((String)partText));
            Object contribText = personalLimit > 0 && lang != null ? lang.tr("gui.quest_list.global-contribution", Map.of("current", String.valueOf(myContribution), "max", String.valueOf(personalLimit))) : (lang != null ? lang.tr("gui.quest_list.global-contribution-no-max", Map.of("current", String.valueOf(myContribution))) : "<#a8a8a8>Your contribution: <#ffffff>" + myContribution + (String)(personalLimit > 0 ? " / " + personalLimit : ""));
            lore.add(ColorFormatter.applyColors((String)contribText));
        }
        lore.add(" ");
        String openDetailsText = lang != null ? lang.tr("gui.quest_list.open-details") : "<#a7ff99>LMB <#cccccc>- details / activate";
        lore.add(ColorFormatter.applyColors(openDetailsText));
        if (meta != null) {
            meta.setLore(lore);
            meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
            itemStack.setItemMeta(meta);
        }
        if (mat == Material.PLAYER_HEAD) {
            String bHead = this.questConfig.getString("icon-base-head", null);
            if (bHead == null) {
                bHead = this.questConfig.getString("base-head", null);
            }
            if (bHead == null) {
                bHead = this.questConfig.getString("base_head", null);
            }
            if (bHead == null && questDef != null) {
                bHead = questDef.getIconBaseHead();
            }
            if (bHead != null && !bHead.trim().isEmpty()) {
                GuiUtils.applyBaseHead(itemStack, bHead);
            }
        }
        gui.setItem(3, 5, new GuiItem(itemStack));
        ItemStack backItem = HexItemBuilder.from(Material.OAK_DOOR).name(ColorFormatter.format(this.tr("common_editor.buttons.back"))).lore(ColorFormatter.format(this.tr("common_editor.buttons.back_lore"))).flags(ItemFlag.HIDE_ATTRIBUTES).build();
        gui.setItem(5, 5, new GuiItem(backItem, event -> this.backAction.run()));
        gui.open((HumanEntity)this.player);
    }
}
