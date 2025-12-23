package ua.woody.questborn.listeners.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;

public class BrewingHandler extends AbstractQuestHandler {

    public BrewingHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onBrewingFinish(BrewEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            Location loc = event.getBlock() != null ? event.getBlock().getLocation() : null;
            if (loc == null || loc.getWorld() == null) return;

            QuestDefinition dummy; // (нічого не треба, лишаю щоб не було плутанини)

            for (int i = 0; i < 3; i++) {
                ItemStack potion = event.getContents().getItem(i);
                if (potion == null || !isPotionItem(potion.getType())) continue;

                Player player = findNearestPlayer(loc);
                if (player != null) {
                    checkBrewingObjective(player, potion);
                }
            }
        }, 20L);
    }

    public void onBrewingInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getInventory().getType() != InventoryType.BREWING) return;

        // brewing stand top slots: 0..2 bottles, 3 ingredient, 4 fuel
        int slot = event.getRawSlot();
        if (slot < 0 || slot > 2) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && isPotionItem(clicked.getType())) {
            checkBrewingObjective(player, clicked);
        }
    }

    private Player findNearestPlayer(Location location) {
        if (location == null || location.getWorld() == null) return null;

        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : location.getWorld().getPlayers()) {
            double distance = player.getLocation().distance(location);
            if (distance <= 10 && (nearest == null || distance < nearestDistance)) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private boolean isPotionItem(Material material) {
        String materialName = material.name();
        return materialName.contains("POTION") ||
                materialName.contains("SPLASH") ||
                materialName.contains("LINGERING");
    }

    private void checkBrewingObjective(Player player, ItemStack potion) {
        QuestDefinition q = getActiveQuest(player);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.BREWING) return;

        if (o.isTargetItem(potion)) {
            progress(player, q, 1);
        }
    }
}
