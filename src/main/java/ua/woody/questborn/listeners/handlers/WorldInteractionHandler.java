package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;

public class WorldInteractionHandler extends AbstractQuestHandler {

    public WorldInteractionHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    // ==================== BUCKET EVENTS ====================

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.FILL_BUCKET) return;

        // визначаємо, що саме за рідина була набрана
        Material fluid = e.getBlock().getType(); // <--- ключове

        // якщо потрібна тільки вода — то рахуємо тільки воду
        if (o.isTargetBlock(fluid)) {
            progress(p, q, 1);
        }
    }


    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.EMPTY_BUCKET) return;

        // Отримуємо тип відра
        Material bucketType = e.getBucket();

        // Мапа для конвертації типу відра в тип рідини
        java.util.Map<Material, Material> bucketToFluid = new java.util.HashMap<>();
        bucketToFluid.put(Material.LAVA_BUCKET, Material.LAVA);
        bucketToFluid.put(Material.WATER_BUCKET, Material.WATER);

        // Додаємо підтримку для порошкового снігу (якщо потрібно)
        bucketToFluid.put(Material.POWDER_SNOW_BUCKET, Material.POWDER_SNOW);

        // Конвертуємо тип відра в тип рідини
        Material fluidType = bucketToFluid.get(bucketType);

        if (fluidType != null) {
            // Перевіряємо, чи це цільовий тип рідини
            if (o.isTargetBlock(fluidType)) {
                progress(p, q, 1);
            }
        }
    }

    // ==================== INTERACTION EVENTS ====================

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getItem() == null) return;

        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        Block clicked = e.getClickedBlock();
        ItemStack item = e.getItem();

        switch (o.getType()) {
            case TILL_SOIL -> handleTillSoil(p, q, o, clicked, item);
            case PLANT_SEED -> handlePlantSeed(p, q, o, clicked, item);
            case BONE_MEAL_USE -> handleBoneMealUse(p, q, o, clicked, item);
            case STRIP_LOG -> handleStripLog(p, q, o, clicked, item);
            case WAX_OFF -> handleWaxOff(p, q, o, clicked, item);
            case WAX_ON -> handleWaxOn(p, q, o, clicked, item);
            default -> {}
        }
    }

    private void handleTillSoil(Player p, QuestDefinition q, ua.woody.questborn.model.QuestObjective o,
                                Block clicked, ItemStack item) {
        if (clicked == null) return;

        // Перевірка, чи це мотика
        String itemName = item.getType().name();
        if (!itemName.contains("_HOE")) return;

        // Перевірка, чи це земля/трава, яку можна обробити
        Material soilType = clicked.getType();
        if (soilType == Material.GRASS_BLOCK || soilType == Material.DIRT ||
                soilType == Material.COARSE_DIRT || soilType == Material.ROOTED_DIRT) {

            // Якщо вказано конкретні блоки - перевіряємо
            if (!o.getTargetBlocks().isEmpty() || !o.getTargetBlockIds().isEmpty()) {
                if (o.isTargetBlock(soilType)) {
                    progress(p, q, 1);
                }
            } else {
                // Якщо не вказано - будь-яка обробка землі підходить
                progress(p, q, 1);
            }
        }
    }

    private void handlePlantSeed(Player p, QuestDefinition q, ua.woody.questborn.model.QuestObjective o,
                                 Block clicked, ItemStack item) {
        if (clicked == null) return;

        // Перевірка, чи це фермерська земля
        if (!(clicked.getBlockData() instanceof Farmland)) return;

        // Перевірка насіння
        Material seedType = item.getType();
        if (o.isTargetItem(seedType)) {
            progress(p, q, 1);
        }
    }

    private void handleBoneMealUse(Player p, QuestDefinition q, ua.woody.questborn.model.QuestObjective o,
                                   Block clicked, ItemStack item) {
        if (clicked == null) return;
        if (item.getType() != Material.BONE_MEAL) return;

        // Перевірка блоку, на який використано борошно
        Material blockType = clicked.getType();
        if (o.isTargetBlock(blockType)) {
            progress(p, q, 1);
        }
    }

    private void handleStripLog(Player p, QuestDefinition q, ua.woody.questborn.model.QuestObjective o,
                                Block clicked, ItemStack item) {
        if (clicked == null) return;

        // Перевірка, чи це будь-яка сокира
        String itemName = item.getType().name();
        if (!itemName.contains("_AXE")) return;

        // Перевірка, чи це колода, яку можна обтісати
        Material logType = clicked.getType();
        boolean isStrippable = logType.name().contains("_LOG") && !logType.name().contains("STRIPPED");

        if (isStrippable && o.isTargetBlock(logType)) {
            progress(p, q, 1);
        }
    }

    private void handleWaxOff(Player p, QuestDefinition q, ua.woody.questborn.model.QuestObjective o,
                              Block clicked, ItemStack item) {
        if (clicked == null) return;

        // Перевірка, чи це будь-яка сокира
        String itemName = item.getType().name();
        if (!itemName.contains("_AXE")) return;

        // Перевірка, чи це навощений мідний блок
        Material blockType = clicked.getType();
        boolean isWaxedCopper = blockType.name().startsWith("WAXED_");

        if (isWaxedCopper && o.isTargetBlock(blockType)) {
            progress(p, q, 1);
        }
    }

    private void handleWaxOn(Player p, QuestDefinition q, ua.woody.questborn.model.QuestObjective o,
                             Block clicked, ItemStack item) {
        if (clicked == null) return;
        if (item.getType() != Material.HONEYCOMB) return;

        // Перевірка, чи це не навощений мідний блок
        Material blockType = clicked.getType();
        boolean isCopper = blockType.name().contains("COPPER") && !blockType.name().startsWith("WAXED_");

        if (isCopper && o.isTargetBlock(blockType)) {
            progress(p, q, 1);
        }
    }

    // ==================== HARVEST CROP (через BlockBreakEvent) ====================

    public void onHarvestCrop(org.bukkit.event.block.BlockBreakEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.HARVEST_CROP) return;

        Block block = e.getBlock();

        // Список культур, які можна збирати
        switch (block.getType()) {
            case WHEAT:
            case CARROTS:
            case POTATOES:
            case BEETROOTS:
            case MELON:
            case PUMPKIN:
            case SWEET_BERRY_BUSH:
            case COCOA:
            case NETHER_WART:
                // Перевіряємо, чи дозріла культура
                if (block.getBlockData() instanceof Ageable ageable) {
                    if (ageable.getAge() >= ageable.getMaximumAge()) {
                        if (o.isTargetBlock(block.getType())) {
                            progress(p, q, 1);
                        }
                    }
                } else {
                    // Для кавуна, гарбуза - просто перевіряємо тип
                    if (o.isTargetBlock(block.getType())) {
                        progress(p, q, 1);
                    }
                }
                break;
            default:
                break;
        }
    }
}