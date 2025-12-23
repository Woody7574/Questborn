package ua.woody.questborn.listeners;

import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.listeners.handlers.*;

public class QuestProgressListener implements Listener {

    private final QuestbornPlugin plugin;

    private final BlockHandler blockHandler;
    private final EntityHandler entityHandler;
    private final ItemHandler itemHandler;
    private final TravelHandler travelHandler;
    private final InteractionHandler interactionHandler;
    private final BrewingHandler brewingHandler;
    private final LevelAndExperienceHandler levelAndExperienceHandler;
    private final WorldInteractionHandler worldInteractionHandler;

    private final AnvilHandler anvilHandler;
    private final RepairHandler repairHandler;
    private final DyeHandler dyeHandler;
    private final FuelHandler fuelHandler;
    private final TradeHandler tradeHandler;
    private final EnchantmentHandler enchantmentHandler;
    private final ItemBreakHandler itemBreakHandler;

    private final AnimalHandler animalHandler;
    private final FishingHandler fishingHandler;
    private final WorldHandler worldHandler;
    private final MovementHandler movementHandler;
    private final MagicHandler magicHandler;
    private final SocialHandler socialHandler; // сам реєструє свої івенти
    private final ItemInteractionHandler itemInteractionHandler;

    public QuestProgressListener(QuestbornPlugin plugin) {
        this.plugin = plugin;

        this.blockHandler = new BlockHandler(plugin);
        this.entityHandler = new EntityHandler(plugin);
        this.itemHandler = new ItemHandler(plugin);
        this.travelHandler = new TravelHandler(plugin);
        this.interactionHandler = new InteractionHandler(plugin);
        this.brewingHandler = new BrewingHandler(plugin);
        this.levelAndExperienceHandler = new LevelAndExperienceHandler(plugin);
        this.worldInteractionHandler = new WorldInteractionHandler(plugin);

        this.anvilHandler = new AnvilHandler(plugin);
        this.repairHandler = new RepairHandler(plugin);
        this.dyeHandler = new DyeHandler(plugin);
        this.fuelHandler = new FuelHandler(plugin);
        this.tradeHandler = new TradeHandler(plugin);
        this.enchantmentHandler = new EnchantmentHandler(plugin);
        this.itemBreakHandler = new ItemBreakHandler(plugin);

        this.animalHandler = new AnimalHandler(plugin);
        this.fishingHandler = new FishingHandler(plugin);
        this.worldHandler = new WorldHandler(plugin);
        this.movementHandler = new MovementHandler(plugin);
        this.magicHandler = new MagicHandler(plugin);
        this.socialHandler = new SocialHandler(plugin); // реєструється в конструкторі
        this.itemInteractionHandler = new ItemInteractionHandler(plugin);
    }

    /* ================= BLOCK ================= */

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        blockHandler.onBlockBreak(e);
        blockHandler.onCropBreak(e);
        worldInteractionHandler.onHarvestCrop(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        blockHandler.onBlockPlace(e);

        e.getBlock().setMetadata(
                "questborn-placed-by",
                new FixedMetadataValue(plugin, e.getPlayer().getUniqueId().toString())
        );
    }

    /* ================= ENTITY / DAMAGE ================= */

    @EventHandler(ignoreCancelled = true)
    public void onKill(EntityDeathEvent e) {
        entityHandler.onKill(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e) {
        entityHandler.onPlayerDeath(e); // ASSIST_KILL
    }

    @EventHandler(ignoreCancelled = true)
    public void onDealDamage(EntityDamageByEntityEvent e) {
        entityHandler.onDealDamage(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAnyDamage(EntityDamageEvent e) {
        entityHandler.onTakeDamage(e);
        movementHandler.onFallDamage(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityLoveMode(EntityEnterLoveModeEvent e) {
        interactionHandler.onEntityLoveMode(e);
    }

    /* ================= ANIMALS ================= */

    @EventHandler(ignoreCancelled = true)
    public void onAnimalTame(EntityTameEvent e) {
        animalHandler.onAnimalTame(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAnimalBreed(EntityBreedEvent e) {
        animalHandler.onAnimalBreed(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMilkCow(PlayerInteractEntityEvent e) {
        animalHandler.onMilkCow(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onShearSheep(PlayerShearEntityEvent e) {
        animalHandler.onShearSheep(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onThrowEgg(PlayerEggThrowEvent e) {
        animalHandler.onThrowEgg(e);
    }

    /* --- ENTITY_RIDE: Vehicle (boat/minecart etc якщо треба) --- */
    @EventHandler(ignoreCancelled = true)
    public void onEntityRide(VehicleEnterEvent e) {
        if (!(e.getEntered() instanceof Player)) return;
        animalHandler.onEntityRide(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityRideExit(VehicleExitEvent e) {
        animalHandler.onVehicleExit(e);
    }

    /* --- ENTITY_RIDE: Mount (horse/pig/strider/camel/llama etc) --- */
    @EventHandler(ignoreCancelled = true)
    public void onEntityMount(EntityMountEvent e) {
        animalHandler.onEntityMount(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDismount(EntityDismountEvent e) {
        animalHandler.onEntityDismount(e);
    }

    /* ================= ITEMS / CRAFT / FURNACE ================= */

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        itemHandler.onCraft(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent e) {
        itemHandler.onFurnaceExtract(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        // ⚠️ було: onFish + onFishingHook -> часто дає дубль прогресу
        fishingHandler.onFish(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        itemHandler.onConsume(e);

        // ⚠️ brewingHandler.onPotionDrink(e) прибрано, бо він робив CONSUME_ITEM і міг дублювати itemHandler
        magicHandler.onPotionDrink(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent e) {
        itemHandler.onEnchant(e);
        enchantmentHandler.onEnchantItem(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemBreak(PlayerItemBreakEvent e) {
        itemBreakHandler.onItemBreak(e);
    }

    /* ================= INTERACT (ONE PLACE) ================= */

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        interactionHandler.onBlockInteract(e);
        interactionHandler.onUseItem(e);
        interactionHandler.onCakeConsume(e);

        worldInteractionHandler.onInteract(e);

        enchantmentHandler.onEnchantTableUse(e);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        interactionHandler.onChat(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        interactionHandler.onCommand(e);
    }

    /* ================= ITEM INTERACTION ================= */

    @EventHandler(ignoreCancelled = true)
    public void onArmorChange(InventoryClickEvent e) {
        itemInteractionHandler.onArmorChange(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent e) {
        itemInteractionHandler.onSlotChange(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        itemInteractionHandler.onSwap(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent e) {
        itemInteractionHandler.onDrop(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onOpenContainer(InventoryOpenEvent e) {
        itemInteractionHandler.onOpen(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignEdit(SignChangeEvent e) {
        itemInteractionHandler.onSign(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBookEdit(PlayerEditBookEvent e) {
        itemInteractionHandler.onBook(e);
    }

    /* ================= MOVE (ONE PLACE) ================= */

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        travelHandler.onTravel(e);
        travelHandler.onReachLocation(e);
        travelHandler.onRegionMove(e);

        movementHandler.onJump(e);
        movementHandler.onSprintMove(e);
        movementHandler.onElytraFlightDistance(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleSprint(PlayerToggleSprintEvent e) {
        movementHandler.onSprintToggle(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleSneak(PlayerToggleSneakEvent e) {
        movementHandler.onCrouch(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent e) {
        movementHandler.onBoatTravel(e);
        movementHandler.onMinecartTravel(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleGlide(EntityToggleGlideEvent e) {
        movementHandler.onElytraToggle(e);
    }

    /* ================= BREWING / FUEL ================= */

    @EventHandler(ignoreCancelled = true)
    public void onBrewingFinish(BrewEvent event) {
        brewingHandler.onBrewingFinish(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceFuel(FurnaceBurnEvent e) {
        fuelHandler.onFurnaceFuel(e);
    }

    /* ================= INVENTORIES ================= */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        itemInteractionHandler.onInventoryClick(e);

        if (e.getInventory().getType() == InventoryType.ANVIL) {
            anvilHandler.onAnvilTake(e);
        }

        repairHandler.onInventoryClick(e);
        dyeHandler.onCraftItemTake(e);
        fuelHandler.onInventoryClickFuel(e);

        brewingHandler.onBrewingInventoryClick(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        anvilHandler.onAnvilPrepare(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareDyeCraft(PrepareItemCraftEvent e) {
        dyeHandler.onPrepareCraft(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent e) {
        anvilHandler.onAnvilInventoryClose(e);
        dyeHandler.onDyeInventoryClose(e);
        magicHandler.onBeaconApply(e);
    }

    /* ================= TRADE ================= */

    @EventHandler(ignoreCancelled = true)
    public void onVillagerTrade(PlayerTradeEvent e) {
        tradeHandler.onVillagerTrade(e);
    }

    /* ================= WORLD ================= */

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        worldInteractionHandler.onBucketFill(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        worldInteractionHandler.onBucketEmpty(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnterBed(PlayerBedEnterEvent e) {
        worldHandler.onEnterBed(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeaveBed(PlayerBedLeaveEvent e) {
        worldHandler.onLeaveBed(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onChangeDimension(PlayerChangedWorldEvent e) {
        worldHandler.onChangeDimension(e);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        worldHandler.onJoinServer(e);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        entityHandler.onPlayerQuit(e);
        worldHandler.onQuitServer(e);
        itemInteractionHandler.onQuit(e);
    }

    /* ================= MAGIC ================= */

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent e) {
        magicHandler.onPotionSplash(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onConduitPowerGain(EntityPotionEffectEvent e) {
        magicHandler.onConduitPowerGain(e);
    }

    /* ================= LEVEL / EXP ================= */

    @EventHandler(ignoreCancelled = true)
    public void onLevelUp(PlayerLevelChangeEvent e) {
        levelAndExperienceHandler.onLevelUp(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onExperiencePickup(PlayerExpChangeEvent e) {
        levelAndExperienceHandler.onExperiencePickup(e);
    }
}
