package ua.woody.questborn.listeners.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.*;

public class ItemInteractionHandler extends AbstractQuestHandler {

    /* ================= HOLD_ITEM STORAGE ================= */
    private final Map<UUID, Integer> holdTicks = new HashMap<>();  // рахуємо тики (20 = 1 сек)
    private final Map<UUID, Material> holdingItem = new HashMap<>();

    public ItemInteractionHandler(QuestbornPlugin plugin) {
        super(plugin);
        startHoldChecker(); // запуск тік-обробки
    }

    /* ==========================================================
     * ================= WEAR_ARMOR (з відніманням) =============
     * ========================================================== */
    @EventHandler
    public void onArmorChange(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        if (e.getInventory().getType() != InventoryType.PLAYER &&
                e.getSlotType() != InventoryType.SlotType.ARMOR &&
                !e.isShiftClick()) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.WEAR_ARMOR) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            int wearing = countArmor(p, o);
            int needed = Math.max(1, o.getAmount());
            int desired = Math.min(wearing, needed);

            PlayerQuestProgress data = plugin.getPlayerDataStore().get(p.getUniqueId());
            int current = Math.max(0, Math.min(data.getActiveQuestProgress(), needed));

            int diff = desired - current;
            if (diff != 0) progress(p, q, diff);
        });
    }

    private int countArmor(Player p, QuestObjective o) {
        int c = 0;
        for (ItemStack item : p.getInventory().getArmorContents())
            if (item != null && item.getType() != Material.AIR && o.isTargetItem(item.getType()))
                c++;
        return c;
    }


    /* ==========================================================
     * ====================== HOLD_ITEM =========================
     * ========================================================== */

    private void stop(Player p){
        UUID id = p.getUniqueId();
        holdTicks.remove(id);
        holdingItem.remove(id);
    }

    private void tryHoldStart(Player p) {
        QuestDefinition q = getActiveQuest(p);
        if (q == null) { stop(p); return; }

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.HOLD_ITEM) { stop(p); return; }

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType()==Material.AIR || !o.isTargetItem(hand.getType())) {
            stop(p); return;
        }

        holdingItem.put(p.getUniqueId(), hand.getType());
        holdTicks.put(p.getUniqueId(), 0);
    }

    // коли міняє слот
    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent e) {
        Bukkit.getScheduler().runTask(plugin, () -> tryHoldStart(e.getPlayer()));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e){
        if(e.getWhoClicked() instanceof Player p)
            Bukkit.getScheduler().runTask(plugin, () -> tryHoldStart(p));
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e){
        Bukkit.getScheduler().runTask(plugin, () -> tryHoldStart(e.getPlayer()));
    }

    /* ⏳ ТІК-ОБРОБКА — 20 тиков → +1 сек → +1 прогрес */
    private void startHoldChecker(){
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for(UUID id : new HashSet<>(holdTicks.keySet())){

                Player p = Bukkit.getPlayer(id);
                if(p == null){ stopById(id); continue; }

                QuestDefinition q = getActiveQuest(p);
                if(q == null || q.getObjective().getType()!=QuestObjectiveType.HOLD_ITEM){
                    stop(p); continue;
                }

                ItemStack hand = p.getInventory().getItemInMainHand();
                if(hand == null || hand.getType() != holdingItem.get(id)){
                    stop(p); continue;
                }

                int ticks = holdTicks.get(id) + 1;
                holdTicks.put(id, ticks);

                if(ticks >= 20){ // 1 секунда
                    progress(p, q, 1);
                    holdTicks.put(id, 0); // скидаємо і продовжуємо
                }
            }
        }, 1, 1);
    }

    private void stopById(UUID id){
        holdTicks.remove(id);
        holdingItem.remove(id);
    }


    /* ==========================================================
     * ===================== DROP_ITEM ==========================
     * ========================================================== */
    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType()==QuestObjectiveType.DROP_ITEM &&
                o.isTargetItem(e.getItemDrop().getItemStack().getType())){

            progress(p,q,e.getItemDrop().getItemStack().getAmount());
        }
    }


    /* ==========================================================
     * =================== OPEN_CONTAINER =======================
     * ========================================================== */
    @EventHandler
    public void onOpen(InventoryOpenEvent e){
        if(!(e.getPlayer() instanceof Player p)) return;

        QuestDefinition q = getActiveQuest(p);
        if(q == null) return;

        var o = q.getObjective();
        if(o.getType()==QuestObjectiveType.OPEN_CONTAINER){

            // 🔥 ФІЛЬТР: Тільки реальні контейнери
            InventoryType type = e.getInventory().getType();
            if(!isRealContainer(type)){
                return;
            }

            String inventoryType = type.name();

            if(o.isTargetContainerType(inventoryType)){
                progress(p,q,1);
            }
        }
    }

    private boolean isRealContainer(InventoryType type){
        String typeName = type.name();

        // Дозволені типи інвентарів (контейнери гри)
        switch(typeName){
            case "CHEST":
            case "BARREL":
            case "SHULKER_BOX":
            case "DISPENSER":
            case "DROPPER":
            case "HOPPER":
            case "FURNACE":
            case "BLAST_FURNACE":
            case "SMOKER":
            case "BREWING": // Варильна стійка
            case "ENCHANTING": // Столик для зачарувань
            case "ANVIL":
            case "BEACON":
            case "WORKBENCH":
            case "LECTERN":
            case "CARTOGRAPHY_TABLE":
            case "GRINDSTONE":
            case "SMITHING_TABLE":
            case "STONECUTTER":
            case "LOOM":
                return true;
            default:
                return false;
        }
    }

    /* ==========================================================
     * ================== SIGN & BOOK_EDIT =======================
     * ========================================================== */
    @EventHandler
    public void onSign(SignChangeEvent e){
        Player p=e.getPlayer();
        QuestDefinition q=getActiveQuest(p);
        if(q==null)return;

        var o=q.getObjective();
        if(o.getType()==QuestObjectiveType.SIGN_EDIT){
            if(o.getMessage()==null)progress(p,q,1);
            else for(String line:e.getLines())
                if(line!=null&&line.contains(o.getMessage()))
                    progress(p,q,1);
        }
    }

    @EventHandler
    public void onBook(PlayerEditBookEvent e){
        Player p=e.getPlayer();
        QuestDefinition q=getActiveQuest(p);
        if(q!=null&&q.getObjective().getType()==QuestObjectiveType.BOOK_EDIT)
            progress(p,q,1);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e){
        stop(e.getPlayer());
    }
}
