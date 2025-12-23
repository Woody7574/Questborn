package ua.woody.questborn.listeners;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.persistence.PersistentDataType;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.addons.api.AvailabilityResult;
import ua.woody.questborn.gui.MainMenuGui;
import ua.woody.questborn.gui.QuestDetailsGui;
import ua.woody.questborn.gui.QuestListGui;
import ua.woody.questborn.gui.TopGui;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestTypeConfig;

import java.util.Map;

public class GuiClickListener implements Listener {

    private final QuestbornPlugin plugin;
    private final NamespacedKey questKey;
    private final NamespacedKey nextPageKey;
    private final NamespacedKey prevPageKey;
    private final NamespacedKey mainMenuKey;
    private final NamespacedKey backKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey pageKey;
    private final NamespacedKey topMenuKey;
    private final NamespacedKey topActionKey;

    public GuiClickListener(QuestbornPlugin plugin) {
        this.plugin = plugin;
        this.questKey = new NamespacedKey(plugin, "quest-id");
        this.nextPageKey = new NamespacedKey(plugin, "quest-page-next");
        this.prevPageKey = new NamespacedKey(plugin, "quest-page-prev");
        this.mainMenuKey = new NamespacedKey(plugin, "quest-main-menu");
        this.backKey = new NamespacedKey(plugin, "quest-back-to");
        this.actionKey = new NamespacedKey(plugin, "quest-action");
        this.pageKey = new NamespacedKey(plugin, "page");
        this.topMenuKey = new NamespacedKey(plugin, "top-menu");
        this.topActionKey = new NamespacedKey(plugin, "top-action");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        // Визначаємо, де відбувається взаємодія
        var clickedInventory = e.getClickedInventory();
        var topInventory = e.getView().getTopInventory();
        var topHolder = topInventory.getHolder();

        // Якщо верхній інвентар - не наш GUI, то нас не цікавить
        if (!(topHolder instanceof MainMenuGui ||
                topHolder instanceof TopGui ||
                topHolder instanceof QuestListGui ||
                topHolder instanceof QuestDetailsGui)) {
            return;
        }

        // Тепер дивимось, де саме клікнули
        if (clickedInventory == null) {
            return;
        }

        // Якщо клікнули у верхній інвентар (наш GUI)
        if (clickedInventory.equals(topInventory)) {
            e.setCancelled(true); // Забороняємо взаємодію з GUI

            // Обробляємо клік по GUI
            handleGuiClick(e, player);
            return;
        }

        // Якщо клікнули у нижній інвентар (інвентар гравця)
        // Але SHIFT+CLICK може перемістити предмет у верхній інвентар
        if (e.getClick().isShiftClick()) {
            // SHIFT+CLICK з інвентаря гравця намагається перемістити предмет у GUI
            e.setCancelled(true);
            return;
        }

        // Звичайний клік у інвентарі гравця - дозволяємо
        // (нічого не робимо)
    }

    private void handleGuiClick(InventoryClickEvent e, Player player) {
        var item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        var pdc = item.getItemMeta().getPersistentDataContainer();
        var lang = plugin.getLanguage();
        var holder = e.getInventory().getHolder();

        /* -----------------------------------------------------------
         *                    MAIN MENU
         * ----------------------------------------------------------- */
        if (holder instanceof MainMenuGui) {
            // СПОЧАТКУ перевіряємо клік по кнопці топу
            if (pdc.has(topMenuKey, PersistentDataType.BYTE)) {
                playGuiClickSound(player);
                new TopGui(plugin, player).open();
                return;
            }

            // Перевіряємо кліки на кнопки типів квестів
            int slot = e.getRawSlot();
            for (QuestTypeConfig typeConfig : plugin.getQuestManager().getQuestTypeManager().getEnabledTypes()) {
                if (typeConfig.getGuiSlot() == slot) {
                    playGuiClickSound(player);
                    new QuestListGui(plugin, player, typeConfig).open();
                    return;
                }
            }
            return;
        }

        /* -----------------------------------------------------------
         *                    TOP GUI
         * ----------------------------------------------------------- */
        if (holder instanceof TopGui) {
            // Обробляємо кнопки навігації
            if (pdc.has(topActionKey, PersistentDataType.STRING)) {
                String action = pdc.get(topActionKey, PersistentDataType.STRING);
                playGuiClickSound(player);

                switch (action) {
                    case "main" -> new MainMenuGui(plugin, player).open();
                    case "prev" -> {
                        Integer page = pdc.get(pageKey, PersistentDataType.INTEGER);
                        if (page != null) {
                            new TopGui(plugin, player, Math.max(0, page - 1)).open();
                        } else {
                            new TopGui(plugin, player, 0).open();
                        }
                    }
                    case "next" -> {
                        Integer page = pdc.get(pageKey, PersistentDataType.INTEGER);
                        if (page != null) {
                            new TopGui(plugin, player, page + 1).open();
                        } else {
                            new TopGui(plugin, player, 1).open();
                        }
                    }
                }
            }
            return;
        }

        /* -----------------------------------------------------------
         *                QUEST LIST
         * ----------------------------------------------------------- */
        if (holder instanceof QuestListGui listGui) {
            // 1 — ГОЛОВНЕ МЕНЮ
            if (pdc.has(mainMenuKey, PersistentDataType.BYTE)) {
                playGuiClickSound(player);
                new MainMenuGui(plugin, player).open();
                return;
            }

            // 2 — НАСТУПНА СТОРІНКА
            if (pdc.has(nextPageKey, PersistentDataType.BYTE)) {
                playGuiClickSound(player);
                Integer nextPage = pdc.get(pageKey, PersistentDataType.INTEGER);
                if (nextPage != null) {
                    new QuestListGui(plugin, player, listGui.getTypeConfig(), nextPage).open();
                } else {
                    new QuestListGui(plugin, player, listGui.getTypeConfig(), listGui.getCurrentPage() + 1).open();
                }
                return;
            }

            // 3 — ПОПЕРЕДНЯ СТОРІНКА
            if (pdc.has(prevPageKey, PersistentDataType.BYTE)) {
                playGuiClickSound(player);
                Integer prevPage = pdc.get(pageKey, PersistentDataType.INTEGER);
                if (prevPage != null) {
                    new QuestListGui(plugin, player, listGui.getTypeConfig(), prevPage).open();
                } else {
                    new QuestListGui(plugin, player, listGui.getTypeConfig(), listGui.getCurrentPage() - 1).open();
                }
                return;
            }

            // 4 — КЛІК ПО КВЕСТУ
            String questId = pdc.get(questKey, PersistentDataType.STRING);
            if (questId != null) {
                playGuiClickSound(player);

                QuestDefinition quest = plugin.getQuestManager().getById(questId);
                if (quest == null) return;

                var data = plugin.getPlayerDataStore().get(player.getUniqueId());
                boolean isActive = questId.equalsIgnoreCase(data.getActiveQuestId());

                // ✅ Забороняємо відкривати details якщо locked
                if (!isActive) {
                    AvailabilityResult avail = plugin.getQuestManager().checkAvailability(player, quest);
                    if (!avail.isAllowed()) {
                        String msg = avail.getMessage();
                        if (msg == null || msg.isBlank()) msg = plugin.getLanguage().tr("gui.list.status.locked");
                        player.sendMessage(plugin.getLanguage().color(msg));
                        return;
                    }
                }

                new QuestDetailsGui(plugin, player, quest).open();
            }
            return;
        }

        /* -----------------------------------------------------------
         *                QUEST DETAILS GUI
         * ----------------------------------------------------------- */
        if (holder instanceof QuestDetailsGui detailsGui) {
            // Кнопка "Назад"
            String backTypeId = pdc.get(backKey, PersistentDataType.STRING);
            if (backTypeId != null) {
                playGuiClickSound(player);

                // Якщо гравець підтверджує заміну - очищаємо pending quest
                var data = plugin.getPlayerDataStore().get(player.getUniqueId());
                if (data.hasPendingQuest()) {
                    data.clearPendingQuest();
                    plugin.getPlayerDataStore().save();
                }

                // Отримуємо QuestTypeConfig за typeId
                QuestTypeConfig typeConfig = plugin.getQuestManager().getQuestTypeManager().getType(backTypeId);
                if (typeConfig != null) {
                    new QuestListGui(plugin, player, typeConfig).open();
                } else {
                    Bukkit.getLogger().warning("Invalid quest type in GUI: " + backTypeId);
                    new MainMenuGui(plugin, player).open();
                }
                return;
            }

            // Кнопки дії
            String action = pdc.get(actionKey, PersistentDataType.STRING);
            if (action != null) {
                playGuiClickSound(player);
                QuestDefinition quest = detailsGui.getQuest();
                var data = plugin.getPlayerDataStore().get(player.getUniqueId());

                switch (action.toLowerCase()) {
                    case "activate" -> {
                        boolean ok = plugin.getQuestManager().activateQuest(player, quest.getId());
                        if (ok) {
                            player.sendMessage(lang.tr(
                                    "quest.activate.success",
                                    Map.of("quest", quest.getDisplayName())
                            ));
                            player.closeInventory();
                        } else {
                            player.sendMessage(lang.tr("quest.activate.failed"));
                        }
                    }
                    case "cancel" -> {
                        boolean ok = plugin.getQuestManager().cancelQuest(player, quest.getId());
                        if (ok) {
                            player.sendMessage(lang.tr(
                                    "quest.cancel.success",
                                    Map.of("quest", quest.getDisplayName())
                            ));
                            player.closeInventory();
                        } else {
                            player.sendMessage(lang.tr("quest.cancel.failed"));
                        }
                    }
                    case "request-change" -> {
                        data.setPendingQuestId(quest.getId());
                        plugin.getPlayerDataStore().save();
                        new QuestDetailsGui(plugin, player, quest).open();
                    }
                    case "confirm-change" -> {
                        boolean ok = plugin.getQuestManager().confirmQuestChange(player, quest.getId());
                        if (ok) {
                            player.sendMessage(lang.tr(
                                    "quest.change.confirmed",
                                    Map.of("quest", quest.getDisplayName())
                            ));
                            player.closeInventory();
                        } else {
                            player.sendMessage(lang.tr("quest.change.failed"));
                            data.clearPendingQuest();
                            plugin.getPlayerDataStore().save();
                            new QuestDetailsGui(plugin, player, quest).open();
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        var holder = e.getInventory().getHolder();
        if (holder == null) return;

        boolean isOurGui = holder instanceof MainMenuGui ||
                holder instanceof TopGui ||
                holder instanceof QuestListGui ||
                holder instanceof QuestDetailsGui;

        if (isOurGui) {
            // Перевіряємо, чи drag торкається хоча б одного слота нашого GUI
            for (int slot : e.getRawSlots()) {
                // Raw slots від 0 до size-1 - це GUI, від size і далі - інвентар гравця
                if (slot < e.getInventory().getSize()) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    /**
     * Відтворює звук кліку GUI
     */
    private void playGuiClickSound(Player player) {
        var soundManager = plugin.getSoundManager();
        if (soundManager != null) {
            soundManager.playGuiClick(player);
        }
        // Якщо SoundManager не доступний - просто не граємо звук
    }
}