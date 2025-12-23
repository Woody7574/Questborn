package ua.woody.questborn.gui;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.lang.LanguageManager;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.util.TimeFormatter;

import java.util.*;

public class QuestListGui extends BaseGui {

    private final QuestTypeConfig typeConfig;
    private final LanguageManager lang;
    private final int currentPage;
    private final List<QuestDefinition> quests;

    private static final int[] QUEST_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private static final int QUESTS_PER_PAGE = QUEST_SLOTS.length;

    public QuestListGui(QuestbornPlugin plugin, org.bukkit.entity.Player player, QuestTypeConfig typeConfig) {
        this(plugin, player, typeConfig, 0);
    }

    public QuestListGui(QuestbornPlugin plugin, org.bukkit.entity.Player player, QuestTypeConfig typeConfig, int page) {
        super(plugin, player, 45,
                plugin.getLanguage().tr("gui.list.title",
                        Map.of(
                                "type", "&0" + stripColors(typeConfig.getDisplayName()),
                                "page", String.valueOf(page + 1)
                        )
                )
        );

        this.typeConfig = typeConfig;
        this.lang = plugin.getLanguage();
        this.currentPage = page;
        this.quests = getSortedQuests(plugin, player, typeConfig);
        build();
    }

    private static String stripColors(String input) {
        if (input == null) return "";
        String result = input.replaceAll("<#([A-Fa-f0-9]{6})>", "");
        result = result.replaceAll("§[0-9A-FK-ORXa-fk-orx]", "");
        result = result.replaceAll("&[0-9A-FK-ORXa-fk-orx]", "");
        return result;
    }

    private static boolean isQuestGridSlot(int slot) {
        for (int s : QUEST_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    private List<QuestDefinition> getSortedQuests(QuestbornPlugin plugin, org.bukkit.entity.Player player, QuestTypeConfig typeConfig) {
        PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());
        long now = System.currentTimeMillis();

        List<QuestDefinition> allQuests = new ArrayList<>(plugin.getQuestManager().getByType(typeConfig));

        allQuests.sort((q1, q2) -> {
            int p1 = getQuestPriority(plugin, player, typeConfig, q1, data, now);
            int p2 = getQuestPriority(plugin, player, typeConfig, q2, data, now);

            int byPriority = Integer.compare(p1, p2);
            if (byPriority != 0) return byPriority;

            // ✅ sort by quest slot/order inside same group
            int s1 = q1.getSlot();
            int s2 = q2.getSlot();
            boolean has1 = s1 >= 0;
            boolean has2 = s2 >= 0;

            if (has1 && has2) {
                int bySlot = Integer.compare(s1, s2);
                if (bySlot != 0) return bySlot;
            } else if (has1 != has2) {
                return has1 ? -1 : 1; // slot заданий -> вище
            }

            int byName = stripColors(q1.getDisplayName()).compareToIgnoreCase(stripColors(q2.getDisplayName()));
            if (byName != 0) return byName;

            return q1.getId().compareToIgnoreCase(q2.getId());
        });

        return allQuests;
    }

    private int getQuestPriority(QuestbornPlugin plugin, org.bukkit.entity.Player player,
                                 QuestTypeConfig typeConfig,
                                 QuestDefinition quest, PlayerQuestProgress data, long now) {

        String qid = quest.getId();
        boolean isActive = qid.equalsIgnoreCase(data.getActiveQuestId());

        boolean isOneTime = typeConfig.getCooldownSeconds() == 0;
        boolean completedOnce = isOneTime && data.isQuestCompleted(qid);

        long cdUntil = data.getQuestCooldownUntil(qid);
        boolean onCooldown = cdUntil > now;

        // locked by availability (тільки якщо не active / не cooldown / не completedOnce)
        boolean locked = false;
        if (!isActive && !onCooldown && !completedOnce) {
            var res = plugin.getQuestManager().checkAvailability(player, quest);
            locked = !res.isAllowed();
        }

        // ✅ available має бути вище locked
        if (isActive) return 1;
        if (!onCooldown && !completedOnce && !locked) return 2; // available
        if (onCooldown) return 3;                               // cooldown
        if (locked) return 4;                                   // locked
        if (completedOnce) return 5;                             // completed one-time
        return 6;
    }

    private void build() {
        fillFrame();

        PlayerQuestProgress data = plugin.getPlayerDataStore().get(player.getUniqueId());
        long now = System.currentTimeMillis();

        int startIndex = currentPage * QUESTS_PER_PAGE;
        int endIndex = Math.min(startIndex + QUESTS_PER_PAGE, quests.size());

        if (startIndex >= quests.size()) {
            addMainMenuButton(40);
            addNavigationButtons();
            return;
        }

        List<QuestDefinition> pageQuests = quests.subList(startIndex, endIndex);

        // ✅ 1) спочатку ставимо квести з заданим slot
        Set<Integer> usedSlots = new HashSet<>();
        List<QuestDefinition> remaining = new ArrayList<>();

        for (QuestDefinition q : pageQuests) {
            int desired = q.getSlot();
            boolean placed = false;

            if (desired >= 0 && isQuestGridSlot(desired) && !usedSlots.contains(desired)) {
                inventory.setItem(desired, createQuestItem(q, data, now));
                usedSlots.add(desired);
                placed = true;
            }

            if (!placed) remaining.add(q);
        }

        // ✅ 2) решту квестів — у перші вільні слоти
        int freeIndex = 0;
        for (QuestDefinition q : remaining) {
            while (freeIndex < QUEST_SLOTS.length && usedSlots.contains(QUEST_SLOTS[freeIndex])) {
                freeIndex++;
            }
            if (freeIndex >= QUEST_SLOTS.length) break;

            int slot = QUEST_SLOTS[freeIndex++];
            inventory.setItem(slot, createQuestItem(q, data, now));
            usedSlots.add(slot);
        }

        addMainMenuButton(40);
        addNavigationButtons();
    }

    private ItemStack createQuestItem(QuestDefinition q, PlayerQuestProgress data, long now) {
        String qid = q.getId();
        boolean isActive = qid.equalsIgnoreCase(data.getActiveQuestId());

        long cdUntil = data.getQuestCooldownUntil(qid);
        boolean onCooldown = cdUntil > now;

        boolean isOneTime = typeConfig.getCooldownSeconds() == 0;
        boolean completedOnce = isOneTime && data.isQuestCompleted(qid);

        // availability/locked (перевіряємо лише коли реально треба)
        boolean locked = false;
        ua.woody.questborn.addons.api.AvailabilityResult avail = null;
        if (!isActive && !onCooldown && !completedOnce) {
            avail = plugin.getQuestManager().checkAvailability(player, q);
            locked = !avail.isAllowed();
        }

        Material icon;
        if (completedOnce) icon = Material.FILLED_MAP;
        else if (isActive) icon = Material.WRITABLE_BOOK;
        else if (locked) icon = Material.BARRIER;
        else if (onCooldown) icon = Material.CLOCK;
        else icon = Material.BOOK;

        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(lang.color(q.getDisplayName()));

        // ✅ IMPORTANT: locked -> не даємо відкрити details
        if (!locked) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "quest-id"),
                    PersistentDataType.STRING,
                    q.getId()
            );
        }

        List<String> lore = new ArrayList<>();
        boolean printedCooldownStatus = false;

        if (completedOnce) {
            lore.add(lang.tr("gui.list.status.completed-onetime"));
        } else if (isActive) {
            lore.add(lang.tr("gui.list.status.active"));
        } else if (onCooldown) {
            long secs = Math.max(0, (cdUntil - now) / 1000L);
            lore.add(lang.tr("gui.list.status.cooldown", Map.of("time", TimeFormatter.format(secs))));
            printedCooldownStatus = true;
        } else if (locked) {
            lore.add(lang.tr("gui.list.status.locked"));
            if (avail != null && avail.getMessage() != null && !avail.getMessage().isBlank()) {
                lore.add(lang.color(" &7" + avail.getMessage()));
            }
        } else {
            lore.add(lang.tr("gui.list.status.available"));
        }

        lore.add("");

        if (q.getDescription() != null) {
            for (String line : q.getDescription()) lore.add(lang.color(line));
        }

        lore.add("");
        lore.add(lang.tr("gui.list.rewards-header"));

        if (q.getRewardsDescription() != null) {
            for (String line : q.getRewardsDescription()) lore.add(lang.color(" &f" + line));
        }

        if (!printedCooldownStatus && q.getTimeLimitSeconds() > 0) {
            lore.add("");
            lore.add(lang.tr("gui.list.cooldown-time",
                    Map.of("time", TimeFormatter.format(q.getTimeLimitSeconds()))));
        }

        boolean showOpenDetails = !locked && !onCooldown && !completedOnce;
        if (showOpenDetails) {
            lore.add("");
            lore.add(lang.tr("gui.list.open-details"));
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        return item;
    }

    private void addNavigationButtons() {
        int totalPages = (int) Math.ceil((double) quests.size() / QUESTS_PER_PAGE);

        if (currentPage > 0) addPrevPageButton(38);
        if (currentPage < totalPages - 1) addNextPageButton(42);
    }

    private void addMainMenuButton(int slot) {
        ItemStack item = new ItemStack(Material.GUSTER_BANNER_PATTERN);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(lang.tr("gui.button.main"));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "quest-main-menu"),
                PersistentDataType.BYTE,
                (byte) 1
        );

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    private void addPrevPageButton(int slot) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(lang.tr("gui.button.prev"));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "quest-page-prev"),
                PersistentDataType.BYTE,
                (byte) 1
        );

        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "page"),
                PersistentDataType.INTEGER,
                currentPage - 1
        );

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    private void addNextPageButton(int slot) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(lang.tr("gui.button.next"));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "quest-page-next"),
                PersistentDataType.BYTE,
                (byte) 1
        );

        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "page"),
                PersistentDataType.INTEGER,
                currentPage + 1
        );

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    public QuestTypeConfig getTypeConfig() { return typeConfig; }
    public String getTypeId() { return typeConfig.getId(); }
    public int getCurrentPage() { return currentPage; }
}
