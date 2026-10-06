package ua.woody.questborn.listeners.categories;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import ua.woody.questborn.listeners.QuestProgressListener;

public class ItemEventListener implements Listener {
    private final QuestProgressListener listener;

    public ItemEventListener(QuestProgressListener listener) {
        this.listener = listener;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        HumanEntity who = e.getWhoClicked();
        if (!(who instanceof Player player)) return;

        listener.executeForActiveObjectives(player, ctx -> {
            listener.getItemHandler().onCraft(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            listener.getItemHandler().onFurnaceExtract(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            listener.getFishingHandler().onFish(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            listener.getItemHandler().onConsume(e);
            listener.getMagicHandler().onPotionDrink(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent e) {
        listener.executeForActiveObjectives(e.getEnchanter(), ctx -> {
            listener.getEnchantmentHandler().onEnchantItem(e);
            listener.getItemHandler().onEnchant(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemBreak(PlayerItemBreakEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            listener.getItemBreakHandler().onItemBreak(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewingFinish(BrewEvent event) {
        listener.getBrewingHandler().onBrewingFinish(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockCook(org.bukkit.event.block.BlockCookEvent e) {
        listener.getItemHandler().onBlockCook(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceFuel(FurnaceBurnEvent e) {
        listener.getFuelHandler().onFurnaceFuel(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        HumanEntity who = e.getView().getPlayer();
        if (!(who instanceof Player player)) return;

        listener.executeForActiveObjectives(player, ctx -> {
            listener.getAnvilHandler().onAnvilPrepare(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPrepareDyeCraft(PrepareItemCraftEvent e) {
        HumanEntity who = e.getView().getPlayer();
        if (!(who instanceof Player player)) return;

        listener.executeForActiveObjectives(player, ctx -> {
            listener.getDyeHandler().onPrepareCraft(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            listener.getWorldInteractionHandler().onBucketFill(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            listener.getWorldInteractionHandler().onBucketEmpty(e);
        });
    }
}
