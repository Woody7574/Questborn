package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockHandler extends AbstractQuestHandler {

    private final Map<org.bukkit.Location, UUID> placedBlocks = new HashMap<>();
    private final Map<org.bukkit.Location, UUID> freshlyPlacedBlocks = new HashMap<>();

    public BlockHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.BLOCK_BREAK) return;

        if (!hasAntiAbuseBypass(p) && !isNaturalBlock(e.getBlock())) {
            return;
        }

        if (o.isTargetBlock(e.getBlock().getType())) {
            progress(p, q, 1);
        }
    }

    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.BLOCK_PLACE) return;

        if (o.isTargetBlock(e.getBlock().getType())) {
            progress(p, q, 1);
        }
    }

    public void onCropBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.COLLECT_ITEM) return;

        Material type = e.getBlock().getType();
        boolean isCrop = type == Material.WHEAT || type == Material.CARROTS ||
                type == Material.POTATOES || type == Material.BEETROOTS;
        if (!isCrop) return;

        if (e.getBlock().getBlockData() instanceof org.bukkit.block.data.Ageable ageable) {
            if (ageable.getAge() < ageable.getMaximumAge()) return;
        }

        for (ItemStack drop : e.getBlock().getDrops(p.getInventory().getItemInMainHand())) {
            if (o.isTargetItem(drop.getType())) {
                progress(p, q, drop.getAmount());
            }
        }
    }

    private boolean hasAntiAbuseBypass(Player player) {
        return player.hasPermission("questborn.antiabuse.bypass");
    }

    private boolean isNaturalBlock(Block block) {
        return !block.hasMetadata("questborn-placed-by");
    }
}