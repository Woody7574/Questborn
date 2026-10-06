package ua.woody.questborn.listeners.categories;

import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import ua.woody.questborn.listeners.QuestProgressListener;
import ua.woody.questborn.model.QuestObjectiveType;

public class BlockEventListener implements Listener {
    private final QuestProgressListener listener;

    public BlockEventListener(QuestProgressListener listener) {
        this.listener = listener;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            QuestObjectiveType type = ctx.objective().getType();
            if (type == QuestObjectiveType.BLOCK_BREAK) {
                listener.getBlockHandler().onBlockBreak(e);
            }
            if (type == QuestObjectiveType.HARVEST_CROP) {
                listener.getWorldInteractionHandler().onHarvestCrop(e);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        e.getBlock().setMetadata("questborn-placed-by",
                new FixedMetadataValue(listener.getPlugin(), e.getPlayer().getUniqueId().toString()));
        listener.executeForActiveObjectives(e.getPlayer(), ctx -> {
            if (ctx.objective().getType() == QuestObjectiveType.BLOCK_PLACE) {
                listener.getBlockHandler().onBlockPlace(e);
            }
        });
    }
}
