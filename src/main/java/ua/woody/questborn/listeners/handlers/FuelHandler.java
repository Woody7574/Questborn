package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;
import org.bukkit.event.inventory.InventoryType;

public class FuelHandler extends AbstractQuestHandler {

    public FuelHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceFuel(FurnaceBurnEvent e) {
        BlockState state = e.getBlock().getState();

        // Працює для Furnace / BlastFurnace / Smoker (всі вони instanceof Furnace)
        if (!(state instanceof Furnace furnace)) return;

        Player p = findNearestPlayer(furnace.getLocation());
        if (p == null) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.FILL_FUEL) return;

        // Найнадійніше: паливо саме з івента (а не з інвентаря)
        ItemStack fuel = e.getFuel();
        if (fuel == null) return;

        if (o.isTargetItem(fuel.getType())) {
            progress(p, q, 1);
        }
    }

    @EventHandler
    public void onInventoryClickFuel(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        // Перевіряємо інвентар печі, коптильні
        InventoryType invType = e.getInventory().getType();
        if (invType != InventoryType.FURNACE &&
                invType != InventoryType.BLAST_FURNACE &&
                invType != InventoryType.SMOKER) {
            return;
        }

        // Перевіряємо, чи це слот палива
        if (e.getSlot() != 1) return; // Слот палива у печі

        ItemStack fuel = e.getCursor();
        if (fuel == null || fuel.getType() == Material.AIR) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.FILL_FUEL) return;

        if (o.isTargetItem(fuel.getType())) {
            progress(p, q, 1);
        }
    }

    private Player findNearestPlayer(org.bukkit.Location location) {
        if (location == null || location.getWorld() == null) return null;

        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : location.getWorld().getPlayers()) {
            double distance = player.getLocation().distance(location);
            if (distance <= 5 && (nearest == null || distance < nearestDistance)) {
                nearest = player;
                nearestDistance = distance;
            }
        }

        return nearest;
    }
}