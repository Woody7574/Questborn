package ua.woody.questborn.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.lang.LanguageManager;
import ua.woody.questborn.managers.QuestManager;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.util.ObjectiveLoreBuilder;

import java.util.*;

public class QuestDetailsGui extends BaseGui {

    private final QuestDefinition quest;
    private final LanguageManager lang;

    private final NamespacedKey backKey;
    private final NamespacedKey actionKey;

    public QuestDetailsGui(QuestbornPlugin plugin, org.bukkit.entity.Player player, QuestDefinition quest) {
        super(plugin, player, 45,
                ChatColor.BLACK + plugin.getLanguage().tr(
                        "gui.details.title",
                        Map.of("quest", stripHex(quest.getDisplayName()))
                )
        );

        this.quest = quest;
        this.lang = plugin.getLanguage();

        this.backKey = new NamespacedKey(plugin, "quest-back-to");
        this.actionKey = new NamespacedKey(plugin, "quest-action");

        build();
    }

    private void build() {
        fillFrame();
        PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());
        long now = System.currentTimeMillis();

        String activeId = data.getActiveQuestId();
        boolean isActiveThis = quest.getId().equalsIgnoreCase(activeId);
        boolean hasOtherActive = activeId != null && !isActiveThis;

        long cdUntil = data.getQuestCooldownUntil(quest.getId());
        boolean onCooldown = cdUntil > now;

        QuestTypeConfig typeConfig = plugin.getQuestManager().getQuestTypeManager().getType(quest.getTypeId());
        boolean isOneTimeType = typeConfig != null && typeConfig.getCooldownSeconds() == 0;
        boolean oneTimeCompleted = isOneTimeType && data.isQuestCompleted(quest.getId());

        // ✅ availability / locked
        var avail = plugin.getQuestManager().checkAvailability(player, quest);
        boolean locked = !isActiveThis && !onCooldown && !oneTimeCompleted && !avail.isAllowed();

        QuestManager.ActivationConflictMode mode = plugin.getQuestManager().getActivationConflictMode();
        boolean isConfirmingChange = data.getPendingQuestId() != null
                && data.getPendingQuestId().equalsIgnoreCase(quest.getId());

        // ======================================================================
        //   🔎  INFO
        // ======================================================================
        ItemStack info = new ItemStack(Material.SPYGLASS);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName(lang.tr("gui.details.info.title"));

        List<String> lore = new ArrayList<>();
        lore.addAll(lang.trList("gui.details.info.header"));
        lore.add("");

        if (quest.getDescription() != null) {
            quest.getDescription().forEach(s -> lore.add(lang.color(s)));
        }

        lore.add("");
        lore.addAll(ObjectiveLoreBuilder.build(quest.getObjective(), lang));
        lore.add("");

        // ============ 🎯 PROGRESS (distance + amount OK) ============
        var o = quest.getObjective();

        boolean distanceQuest =
                o.getType() == QuestObjectiveType.TRAVEL_DISTANCE ||
                        (o.getType() == QuestObjectiveType.ENTITY_RIDE && o.getDistance() > 0);

        int target = distanceQuest ? (int) Math.ceil(o.getDistance()) : o.getAmount();
        if (target <= 0) target = 1;

        // якщо квест на cooldown / one-time completed -> показуємо 100%
        int progress = (onCooldown || oneTimeCompleted) ? target : (isActiveThis ? data.getActiveQuestProgress() : 0);
        if (progress < 0) progress = 0;
        if (progress > target) progress = target;

        int percent = Math.min(100, (int) ((progress / (double) target) * 100.0));
        int filled = (int) Math.round(percent / 10.0);

        // старий бар
        String FULL = "&l-&r";
        String EMPTY = "&l-&r";

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "§a" + FULL : "§7" + EMPTY);
        }

        lore.add(lang.color("<#ffaa00>" + lang.tr("gui.details.info.progress.title")));
        lore.add(lang.color(
                lang.tr("gui.details.info.progress.bar",
                        Map.of("bar", bar.toString(), "percent", String.valueOf(percent)))
        ));
        lore.add(lang.color(
                lang.tr("gui.details.info.progress.value",
                        Map.of("current", String.valueOf(progress), "target", String.valueOf(target)))
        ));

        // ✅ якщо locked — додаємо причину (якщо є)
        if (locked) {
            lore.add("");
            lore.add(lang.tr("gui.details.status.locked"));
            if (avail.getMessage() != null && !avail.getMessage().isBlank()) {
                lore.add(lang.color(" &7" + avail.getMessage()));
            }
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        info.setItemMeta(meta);
        inventory.setItem(20, info);

        // ======================================================================
        // 🎁 REWARDS
        // ======================================================================
        ItemStack rewards = new ItemStack(Material.CHEST_MINECART);
        ItemMeta rMeta = rewards.getItemMeta();
        rMeta.setDisplayName(lang.tr("gui.details.rewards.title"));

        if (quest.getRewardsDescription() != null) {
            rMeta.setLore(quest.getRewardsDescription().stream().map(lang::color).toList());
        } else {
            rMeta.setLore(List.of(lang.tr("gui.details.rewards.empty")));
        }

        rMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        rewards.setItemMeta(rMeta);
        inventory.setItem(22, rewards);

        // ======================================================================
        //  ▶ ACTION BUTTON
        // ======================================================================
        ItemStack actionItem;

        if (isConfirmingChange) {
            actionItem = button(Material.ORANGE_DYE, "confirm-change", "gui.details.action.confirm-change");
        } else if (oneTimeCompleted) {
            actionItem = button(Material.GRAY_DYE, null, "gui.details.action.completed");
        } else if (locked) {
            actionItem = button(Material.GRAY_DYE, null, "gui.details.action.locked");
        } else if (isActiveThis) {
            actionItem = button(Material.RED_DYE, "cancel", "gui.details.action.cancel");
        } else if (onCooldown) {
            actionItem = button(Material.GRAY_DYE, null, "gui.details.action.cooldown");
        } else if (hasOtherActive && mode == QuestManager.ActivationConflictMode.BLOCK) {
            actionItem = button(Material.GRAY_DYE, null, "gui.details.action.blocked");
        } else if (hasOtherActive && mode == QuestManager.ActivationConflictMode.CHANGE) {
            actionItem = button(Material.YELLOW_DYE, "request-change", "gui.details.action.change");
        } else {
            actionItem = button(Material.LIME_DYE, "activate", "gui.details.action.activate");
        }

        inventory.setItem(24, actionItem);

        // ======================================================================
        //  🔙 BACK
        // ======================================================================
        ItemStack back = new ItemStack(Material.GUSTER_BANNER_PATTERN);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.setDisplayName(lang.tr("gui.details.back.title"));
        bMeta.setLore(lang.trList(isConfirmingChange ? "gui.details.back.lore-cancel" : "gui.details.back.lore"));
        bMeta.getPersistentDataContainer().set(backKey, PersistentDataType.STRING, quest.getTypeId());
        bMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        back.setItemMeta(bMeta);
        inventory.setItem(40, back);
    }

    // кнопка-конструктор
    private ItemStack button(Material mat, String action, String langKey) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(lang.tr(langKey + ".title"));
        meta.setLore(lang.trList(langKey + ".lore"));

        if (action != null) {
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private static String stripHex(String s) {
        if (s == null) return "";
        return s.replaceAll("<#([A-Fa-f0-9]{6})>", "")
                .replaceAll("§[0-9A-FK-ORXa-fk-orx]", "")
                .replaceAll("&[0-9A-FK-ORXa-fk-orx]", "");
    }

    public QuestDefinition getQuest() {
        return quest;
    }
}
