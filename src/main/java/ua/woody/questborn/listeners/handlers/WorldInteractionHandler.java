package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WorldInteractionHandler extends AbstractQuestHandler {
    private static final Map<Material, Material> BUCKET_TO_FLUID = new HashMap<>();
    static {
        BUCKET_TO_FLUID.put(Material.LAVA_BUCKET, Material.LAVA);
        BUCKET_TO_FLUID.put(Material.WATER_BUCKET, Material.WATER);
        try {
            Material powderSnowBucket = Material.getMaterial("POWDER_SNOW_BUCKET");
            Material powderSnow = Material.getMaterial("POWDER_SNOW");
            if (powderSnowBucket != null && powderSnow != null) {
                BUCKET_TO_FLUID.put(powderSnowBucket, powderSnow);
            }
        } catch (Exception ignored) {
        }
    }

    public WorldInteractionHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onBucketFill(PlayerBucketFillEvent e) {
        Player p = e.getPlayer();

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.FILL_BUCKET)
            return;

        Block src = e.getBlock();
        if (src == null) {
            try {
                src = (Block) e.getClass().getMethod("getBlockClicked").invoke(e);

            } catch (Exception ignored) {
            }
        }
        if (src == null)
            return;

        Material fluid = src.getType();
        if (o.isTargetBlock(src)) {
            progress(p, q, 1);
        }
    }

    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        Player p = e.getPlayer();

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.EMPTY_BUCKET)
            return;

        Material bucketType = e.getBucket();
        Material fluidType = BUCKET_TO_FLUID.get(bucketType);
        if (fluidType == null)
            return;

        if (o.isTargetBlock(e.getBlock())) {
            progress(p, q, 1);
        }
    }

    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() == null)
            return;
        if (e.getItem() == null)
            return;

        Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK)
            return;

        Player p = e.getPlayer();

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        Block clicked = e.getClickedBlock();
        ItemStack item = e.getItem();

        switch (o.getType()) {
            case TILL_SOIL -> handleTillSoil(p, q, o, clicked, item);
            case PLANT_SEED -> handlePlantSeed(p, q, o, clicked, item);
            case BONE_MEAL_USE -> handleBoneMealUse(p, q, o, clicked, item);
            case STRIP_LOG -> handleStripLog(p, q, o, clicked, item);
            case WAX_OFF -> handleWaxOff(p, q, o, clicked, item);
            case WAX_ON -> handleWaxOn(p, q, o, clicked, item);
            case HARVEST_CROP -> handleHarvestCropInteract(p, q, o, clicked);
            default -> {
            }
        }
    }

    private void handleTillSoil(Player p, QuestDefinition q, QuestObjective o, Block clicked, ItemStack item) {
        if (clicked == null)
            return;

        String itemName = item.getType().name();
        if (!itemName.contains("_HOE"))
            return;

        Material soil = clicked.getType();
        boolean isRootedDirt = false;
        try {
            isRootedDirt = soil.getKey().getKey().equalsIgnoreCase("rooted_dirt");
        } catch (Exception ignored) {
        }

        final boolean isRootedDirtFinal = isRootedDirt;

        if (soil == Material.GRASS_BLOCK || soil == Material.DIRT ||
                soil == Material.COARSE_DIRT || isRootedDirt) {
            boolean matches = false;
            if ((o.getTargetMaterials() == null || o.getTargetMaterials().isEmpty()) &&
                    (o.getTargetBlockIds() == null || o.getTargetBlockIds().isEmpty())) {
                matches = true;
            } else if (o.isTargetBlock(clicked)) {
                matches = true;
            }

            if (matches) {
                plugin.getFoliaLib().getImpl().runAtEntityLater(p, () -> {
                    Material newType = clicked.getType();
                    if (newType == Material.FARMLAND || (isRootedDirtFinal && newType == Material.DIRT)) {
                        progress(p, q, 1);
                    }
                }, 1L);
            }
        }
    }

    private void handlePlantSeed(Player p, QuestDefinition q, QuestObjective o, Block clicked, ItemStack item) {
        if (clicked == null)
            return;

        Material seedType = item.getType();

        if (!o.isTargetItem(item))
            return;

        boolean isSapling = seedType.name().endsWith("_SAPLING");

        if (isSapling) {
            Set<Material> validSaplingBlocks = new java.util.HashSet<>();
            validSaplingBlocks.add(Material.DIRT);
            validSaplingBlocks.add(Material.GRASS_BLOCK);
            validSaplingBlocks.add(Material.PODZOL);
            validSaplingBlocks.add(Material.COARSE_DIRT);
            validSaplingBlocks.add(Material.FARMLAND);

            try {
                if (Material.getMaterial("ROOTED_DIRT") != null)
                    validSaplingBlocks.add(Material.getMaterial("ROOTED_DIRT"));
                if (Material.getMaterial("MOSS_BLOCK") != null)
                    validSaplingBlocks.add(Material.getMaterial("MOSS_BLOCK"));
            } catch (Exception ignored) {
            }
            if (validSaplingBlocks.contains(clicked.getType())) {
                progress(p, q, 1);
            }
        }

        else if (clicked.getBlockData() instanceof Farmland) {
            progress(p, q, 1);
        }
        else if (seedType == Material.NETHER_WART && clicked.getType() == Material.SOUL_SAND) {
            progress(p, q, 1);
        }
        else if (seedType == Material.COCOA_BEANS && clicked.getType().name().contains("JUNGLE_LOG")) {
            progress(p, q, 1);
        }
        else if (seedType.name().equals("SUGAR_CANE") && (clicked.getType() == Material.SAND || clicked.getType() == Material.DIRT || clicked.getType() == Material.GRASS_BLOCK || clicked.getType() == Material.PODZOL || clicked.getType() == Material.COARSE_DIRT)) {
            progress(p, q, 1);
        }
        else if (seedType.name().equals("SWEET_BERRIES") && (clicked.getType() == Material.GRASS_BLOCK || clicked.getType() == Material.DIRT || clicked.getType() == Material.PODZOL || clicked.getType() == Material.COARSE_DIRT)) {
            progress(p, q, 1);
        }
    }

    private void handleBoneMealUse(Player p, QuestDefinition q, QuestObjective o, Block clicked, ItemStack item) {
        if (clicked == null)
            return;
        if (item.getType() != Material.BONE_MEAL)
            return;

        if (o.isTargetBlock(clicked)) {
            progress(p, q, 1);
        }
    }

    private void handleStripLog(Player p, QuestDefinition q, QuestObjective o, Block clicked, ItemStack item) {
        if (clicked == null)
            return;
        if (!item.getType().name().contains("_AXE"))
            return;

        Material log = clicked.getType();
        boolean isStrippable = log.name().endsWith("_LOG") && !log.name().startsWith("STRIPPED_");
        if (isStrippable && o.isTargetBlock(clicked)) {
            plugin.getFoliaLib().getImpl().runAtEntityLater(p, () -> {
                Material newType = clicked.getType();
                if (newType.name().startsWith("STRIPPED_")) {
                    progress(p, q, 1);
                }
            }, 1L);
        }
    }

    private void handleWaxOff(Player p, QuestDefinition q, QuestObjective o, Block clicked, ItemStack item) {
        if (clicked == null)
            return;
        if (!item.getType().name().contains("_AXE"))
            return;

        Material block = clicked.getType();
        boolean waxed = block.name().startsWith("WAXED_");
        if (waxed && o.isTargetBlock(clicked)) {
            plugin.getFoliaLib().getImpl().runAtEntityLater(p, () -> {
                Material newType = clicked.getType();
                if (!newType.name().startsWith("WAXED_")) {
                    progress(p, q, 1);
                }
            }, 1L);
        }
    }

    private void handleWaxOn(Player p, QuestDefinition q, QuestObjective o, Block clicked, ItemStack item) {
        if (clicked == null)
            return;

        if (!item.getType().name().equals("HONEYCOMB"))
            return;

        Material block = clicked.getType();
        boolean copper = block.name().contains("COPPER") && !block.name().startsWith("WAXED_");
        if (copper && o.isTargetBlock(clicked)) {
            plugin.getFoliaLib().getImpl().runAtEntityLater(p, () -> {
                Material newType = clicked.getType();
                if (newType.name().startsWith("WAXED_")) {
                    progress(p, q, 1);
                }
            }, 1L);
        }
    }

    private void handleHarvestCropInteract(Player p, QuestDefinition q, QuestObjective o, Block clicked) {
        if (clicked == null) return;
        Material blockType = clicked.getType();

        if (blockType.name().equals("SWEET_BERRY_BUSH")) {
            if (clicked.getBlockData() instanceof Ageable ageable) {
                if (ageable.getAge() >= ageable.getMaximumAge()) {
                    Material berries;
                    try {
                        berries = Material.getMaterial("SWEET_BERRIES");
                    } catch (Exception ignored) {
                        return;
                    }
                    if (berries != null && (o.isTargetItem(berries) || o.isTargetBlock(clicked))) {
                        plugin.getFoliaLib().getImpl().runAtEntityLater(p, () -> {
                            if (clicked.getBlockData() instanceof Ageable afterAge) {
                                if (afterAge.getAge() < ageable.getMaximumAge()) {
                                    progress(p, q, 1);
                                }
                            }
                        }, 1L);
                    }
                }
            }
        }
    }

    public void onHarvestCrop(org.bukkit.event.block.BlockBreakEvent e) {
        Player p = e.getPlayer();

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.HARVEST_CROP)
            return;

        Block block = e.getBlock();
        Material blockType = block.getType();

        Map<Material, Material> cropToItemMap = new HashMap<>();
        cropToItemMap.put(Material.WHEAT, Material.WHEAT);
        cropToItemMap.put(Material.CARROTS, Material.CARROT);
        cropToItemMap.put(Material.POTATOES, Material.POTATO);
        cropToItemMap.put(Material.BEETROOTS, Material.BEETROOT);
        cropToItemMap.put(Material.MELON, Material.MELON_SLICE);
        cropToItemMap.put(Material.PUMPKIN, Material.PUMPKIN);
        cropToItemMap.put(Material.COCOA, Material.COCOA_BEANS);
        cropToItemMap.put(Material.NETHER_WART, Material.NETHER_WART);

        try {
            Material bush = Material.getMaterial("SWEET_BERRY_BUSH");
            Material berries = Material.getMaterial("SWEET_BERRIES");
            if (bush != null && berries != null) {
                cropToItemMap.put(bush, berries);
            }
        } catch (Exception ignored) {
        }

        if (cropToItemMap.containsKey(blockType)) {
            if (block.getBlockData() instanceof Ageable ageable) {
                if (ageable.getAge() < ageable.getMaximumAge())
                    return;
            }

            Material itemType = cropToItemMap.get(blockType);
            if (o.isTargetItem(itemType) || o.isTargetBlock(block)) {
                progress(p, q, 1);
            }
        }
    }
}
