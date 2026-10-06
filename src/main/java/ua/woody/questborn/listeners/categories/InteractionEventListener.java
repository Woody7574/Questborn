package ua.woody.questborn.listeners.categories;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import ua.woody.questborn.listeners.QuestProgressListener;

public class InteractionEventListener implements Listener {
    private final QuestProgressListener listener;

    public InteractionEventListener(QuestProgressListener listener) {
        this.listener = listener;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        listener.getItemInteractionHandler().onArmorEquipInteract(e);
        listener.getWorldInteractionHandler().onInteract(e);
        listener.getItemHandler().onCampfireInteract(e);
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            listener.getInteractionHandler().onBlockInteract(e);
            listener.getInteractionHandler().onUseItem(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            listener.getInteractionHandler().onChat(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            listener.getInteractionHandler().onCommand(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorChange(InventoryClickEvent e) {
        HumanEntity who = e.getWhoClicked();
        if (!(who instanceof Player player)) return;

        listener.getItemInteractionHandler().onArmorChange(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent e) {
        listener.getItemInteractionHandler().onSlotChange(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        listener.getItemInteractionHandler().onSwap(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent e) {
        listener.getItemInteractionHandler().onDrop(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpenContainer(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;

        listener.getItemInteractionHandler().onOpen(e);
        listener.executeForActiveObjectives(player, ctx -> {
            listener.getFuelHandler().onOpen(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignEdit(SignChangeEvent e) {
        listener.getItemInteractionHandler().onSign(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBookEdit(PlayerEditBookEvent e) {
        listener.getItemInteractionHandler().onBook(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        HumanEntity who = e.getWhoClicked();
        if (!(who instanceof Player player)) return;

        listener.getItemInteractionHandler().onInventoryClick(e);
        listener.getFuelHandler().onInventoryClickFuel(e);
        listener.getBrewingHandler().onBrewingInventoryClick(e);
        listener.executeForActiveObjectives(player, ctx -> {
            listener.getAnvilHandler().onAnvilTake(e);
            listener.getRepairHandler().onInventoryClick(e);
            listener.getDyeHandler().onCraftItemTake(e);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        HumanEntity who = e.getWhoClicked();
        if (!(who instanceof Player player)) return;

        listener.getFuelHandler().onInventoryDragFuel(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;

        listener.getAnvilHandler().onAnvilInventoryClose(e);
        listener.getDyeHandler().onDyeInventoryClose(e);
        listener.getMagicHandler().onBeaconApply(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        listener.getItemHandler().onCampfireBreak(e);
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            listener.getItemHandler().onFurnaceBreak(e);
        });
    }
}
