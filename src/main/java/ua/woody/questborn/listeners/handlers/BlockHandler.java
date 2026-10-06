package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

public class BlockHandler extends AbstractQuestHandler {
    public BlockHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = plugin.getQuestManager().resolveObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.BLOCK_BREAK)
            return;

        if (!hasAntiAbuseBypass(p) && !isNaturalBlock(e.getBlock())) {
            return;
        }

        if (o.isTargetBlock(e.getBlock())) {
            plugin.getQuestManager().incrementProgress(p, q, 1);
        }
    }

    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = plugin.getQuestManager().resolveObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.BLOCK_PLACE)
            return;

        if (o.isTargetBlock(e.getBlock())) {
            plugin.getQuestManager().incrementProgress(p, q, 1);
        }
    }

    private boolean hasAntiAbuseBypass(Player player) {
        return player.hasPermission("questborn.antiabuse.bypass");
    }

    private boolean isNaturalBlock(Block block) {
        return !block.hasMetadata("questborn-placed-by");
    }
}
