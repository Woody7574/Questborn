package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.Conduit;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;

public class MagicHandler extends AbstractQuestHandler {

    public MagicHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    // ==================== POTION_SPLASH ====================
    @EventHandler
    public void onPotionSplash(PotionSplashEvent e) {
        if (!(e.getEntity().getShooter() instanceof Player)) return;

        Player p = (Player) e.getEntity().getShooter();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() == QuestObjectiveType.POTION_SPLASH) {
            ThrownPotion potion = e.getEntity();
            ItemStack potionItem = potion.getItem();

            // Перевіряємо тип зілля
            if (o.isTargetItem(potionItem)) {
                progress(p, q, 1);
            }
        }
    }

    // ==================== POTION_DRINK (можна додати тут, але в BrewingHandler вже є) ====================
    @EventHandler
    public void onPotionDrink(org.bukkit.event.player.PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItem();

        if (item == null || !isPotionItem(item.getType())) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() == QuestObjectiveType.POTION_DRINK) {
            if (o.isTargetItem(item)) {
                progress(p, q, 1);
            }
        }
    }

    // ==================== BEACON_ACTIVATE ====================
    @EventHandler
    public void onBeaconApply(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (e.getInventory().getType() != InventoryType.BEACON) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.BEACON_ACTIVATE) return;

        Block block = p.getTargetBlockExact(5); // шукаємо маяк перед гравцем
        if (block == null || block.getType() != Material.BEACON) return;

        Beacon beacon = (Beacon) block.getState();

        // Реально активований маяк = вибраний ефект
        if (beacon.getPrimaryEffect() != null) {
            progress(p, q, 1);
        }
    }

    // ==================== CONDUIT_ACTIVATE ====================
    @EventHandler
    public void onConduitPowerGain(org.bukkit.event.entity.EntityPotionEffectEvent e) {

        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getNewEffect() == null) return;
        if (!e.getNewEffect().getType().getName().equalsIgnoreCase("CONDUIT_POWER")) return;

        // 🎉 Якщо гравець зараз отримав ефект — провідник активовано поруч
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() == QuestObjectiveType.CONDUIT_ACTIVATE) {
            progress(p, q, 1);
        }
    }


    // ==================== Допоміжні методи ====================
    private boolean isPotionItem(Material material) {
        String materialName = material.name();
        return materialName.contains("POTION") ||
                materialName.contains("SPLASH") ||
                materialName.contains("LINGERING");
    }
}