package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

public class MagicHandler extends AbstractQuestHandler {
    public MagicHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onPotionSplash(PotionSplashEvent e) {
        if (!(e.getEntity().getShooter() instanceof Player p)) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null) return;

        if (o.getType() != QuestObjectiveType.POTION_SPLASH) return;

        ThrownPotion potion = e.getEntity();
        ItemStack potionItem = potion.getItem();
        if (potionItem == null) return;

        if (o.isTargetItem(potionItem)) {
            progress(p, q, 1);
        }
    }

    public void onPotionDrink(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        if (item == null || !isPotionItem(item.getType())) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null) return;

        if (o.getType() == QuestObjectiveType.POTION_DRINK) {
            if (o.isTargetItem(item)) {
                progress(p, q, 1);
            }
        }
    }

    public void onBeaconApply(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (e.getInventory().getType() != InventoryType.BEACON) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null) return;

        if (o.getType() != QuestObjectiveType.BEACON_ACTIVATE) return;

        Block block = p.getTargetBlockExact(5);
        if (block == null || block.getType() != Material.BEACON) return;

        Beacon beacon = (Beacon) block.getState();
        if (beacon.getPrimaryEffect() != null) {
            progress(p, q, 1);
        }
    }

    public void onConduitPowerGain(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getNewEffect() == null) return;

        if (!e.getNewEffect().getType().getName().equalsIgnoreCase("CONDUIT_POWER")) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null) return;

        if (o.getType() == QuestObjectiveType.CONDUIT_ACTIVATE) {
            progress(p, q, 1);
        }
    }

    private boolean isPotionItem(Material material) {
        String n = material.name();
        return n.contains("POTION") || n.contains("SPLASH") || n.contains("LINGERING");
    }
}
