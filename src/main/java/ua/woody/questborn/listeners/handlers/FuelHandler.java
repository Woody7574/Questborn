package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class FuelHandler extends AbstractQuestHandler {
    private final Map<Location, OwnerStamp> lastUser = new HashMap<>();
    private final long TTL_MS = 60_000;

    public FuelHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p))
            return;

        InventoryType t = e.getInventory().getType();
        if (t != InventoryType.FURNACE && t != InventoryType.BLAST_FURNACE && t != InventoryType.SMOKER)
            return;

        Location loc = e.getInventory().getLocation();
        if (loc == null)
            return;

        lastUser.put(loc, new OwnerStamp(p.getUniqueId(), System.currentTimeMillis()));
        cleanupOld();
    }

    public void onInventoryClickFuel(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p))
            return;

        InventoryType t = e.getInventory().getType();
        if (t != InventoryType.FURNACE && t != InventoryType.BLAST_FURNACE && t != InventoryType.SMOKER)
            return;

        Location loc = e.getInventory().getLocation();
        if (loc != null) {
            lastUser.put(loc, new OwnerStamp(p.getUniqueId(), System.currentTimeMillis()));
            cleanupOld();
        }

        monitorFuelPlacement(p, e.getInventory());
    }

    public void onInventoryDragFuel(org.bukkit.event.inventory.InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p))
            return;

        InventoryType t = e.getInventory().getType();
        if (t != InventoryType.FURNACE && t != InventoryType.BLAST_FURNACE && t != InventoryType.SMOKER)
            return;

        monitorFuelPlacement(p, e.getInventory());
    }

    private void monitorFuelPlacement(Player p, org.bukkit.inventory.Inventory inv) {
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;
        QuestObjective o = getCurrentObjective(p, q);
        if (o == null || o.getType() != QuestObjectiveType.FILL_FUEL) return;

        ItemStack before = inv.getItem(1);
        final ItemStack beforeClone = before == null ? null : before.clone();

        plugin.getFoliaLib().getImpl().runAtEntity(p, __ -> {
            QuestDefinition qAfter = getActiveQuest(p);
            if (qAfter == null) return;
            QuestObjective oAfter = getCurrentObjective(p, qAfter);
            if (oAfter == null || oAfter.getType() != QuestObjectiveType.FILL_FUEL) return;

            ItemStack after = inv.getItem(1);
            if (after == null || after.getType() == Material.AIR) return;

            boolean sameType = (beforeClone != null && beforeClone.isSimilar(after));
            int amountBefore = sameType ? beforeClone.getAmount() : 0;
            int amountAfter = after.getAmount();

            if (amountAfter > amountBefore) {
                if (oAfter.isTargetItem(after)) {
                    progress(p, qAfter, amountAfter - amountBefore);
                }
            }
        });
    }

    public void onFurnaceFuel(FurnaceBurnEvent e) {
        BlockState state = e.getBlock().getState();
        if (!(state instanceof Furnace))
            return;

        Location loc = e.getBlock().getLocation();
        OwnerStamp os = lastUser.get(loc);

        if (os == null)
            return;
        if (System.currentTimeMillis() - os.timestamp > TTL_MS) {
            lastUser.remove(loc);
            return;
        }

        Player p = plugin.getServer().getPlayer(os.playerId);
        if (p == null || !p.isOnline())
            return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.BURN_FUEL)
            return;

        ItemStack fuel = e.getFuel();
        if (fuel == null || fuel.getType() == Material.AIR)
            return;

        if (o.isTargetItem(fuel)) {
            progress(p, q, 1);
        }
    }

    private void cleanupOld() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Location, OwnerStamp>> it = lastUser.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, OwnerStamp> en = it.next();
            if (now - en.getValue().timestamp > TTL_MS)
                it.remove();
        }
    }

    private static class OwnerStamp {
        final UUID playerId;
        final long timestamp;

        OwnerStamp(UUID playerId, long timestamp) {
            this.playerId = playerId;
            this.timestamp = timestamp;
        }
    }
}
