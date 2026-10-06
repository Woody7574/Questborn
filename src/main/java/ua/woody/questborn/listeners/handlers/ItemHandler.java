package ua.woody.questborn.listeners.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.*;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Campfire;
import org.bukkit.block.Furnace;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

import java.util.*;

public class ItemHandler extends AbstractQuestHandler {
    private final Map<Location, Queue<UUID>> campfirePlacements = new HashMap<>();

    public ItemHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player p))
            return;
        if (e.isCancelled())
            return;

        if (e.getSlotType() != InventoryType.SlotType.RESULT)
            return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null || q.getId() == null)
            return;

        QuestObjective o = plugin.getQuestManager().resolveObjective(p, q);
        if (o == null || o.getType() != QuestObjectiveType.ITEM_CRAFT)
            return;

        Recipe recipe = e.getRecipe();
        if (recipe == null)
            return;

        ItemStack result = e.getCurrentItem();
        if (result == null || result.getType().isAir())
            result = recipe.getResult();
        if (result == null || result.getType().isAir())
            return;

        Material craftedType = result.getType();
        if (!o.isTargetItem(result))
            return;

        final ItemStack finalResult = result.clone();
        final String questId = q.getId();
        final int resultPerCraft = finalResult.getAmount();

        final int beforeInv = countMaterial(p.getInventory(), craftedType) + countOnCursor(p, craftedType);

        final Inventory clickedInv = e.getInventory();
        final ItemStack[] matrixBefore;
        if (clickedInv instanceof CraftingInventory ci) {
            ItemStack[] m = ci.getMatrix();
            matrixBefore = new ItemStack[m.length];
            for (int i = 0; i < m.length; i++)
                matrixBefore[i] = (m[i] == null ? null : m[i].clone());
        } else {
            matrixBefore = null;
        }

        ClickType click = e.getClick();
        InventoryAction action = e.getAction();
        final boolean isDropClick = click == ClickType.DROP || click == ClickType.CONTROL_DROP
                || action == InventoryAction.DROP_ONE_SLOT || action == InventoryAction.DROP_ALL_SLOT;

        plugin.getFoliaLib().getImpl().runAtEntity(p, __ -> {
            if (!p.isOnline())
                return;

            QuestDefinition qNow = getActiveQuest(p);
            if (qNow == null || qNow.getId() == null || !qNow.getId().equals(questId))
                return;

            QuestObjective oNow = plugin.getQuestManager().resolveObjective(p, qNow);
            if (oNow == null || oNow.getType() != QuestObjectiveType.ITEM_CRAFT)
                return;
            if (!oNow.isTargetItem(finalResult))
                return;

            int afterInv = countMaterial(p.getInventory(), craftedType) + countOnCursor(p, craftedType);
            int producedByDelta = afterInv - beforeInv;
            int craftsFromMatrix = 0;

            if (matrixBefore != null && clickedInv instanceof CraftingInventory ciNow) {
                craftsFromMatrix = estimateCraftsFromMatrixDiff(matrixBefore, ciNow.getMatrix());
            }

            int produced = 0;

            if (isDropClick) {
                if (craftsFromMatrix > 0) {
                    produced = craftsFromMatrix * resultPerCraft;
                } else {
                    return;
                }

            } else if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT
                    || action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                if (craftsFromMatrix > 0) {
                    produced = craftsFromMatrix * resultPerCraft;
                } else if (producedByDelta > 0) {
                    produced = producedByDelta;
                }

            } else {
                if (producedByDelta > 0) {
                    produced = producedByDelta;
                } else if (craftsFromMatrix > 0) {
                    produced = craftsFromMatrix * resultPerCraft;
                }
            }

            if (produced > 0) {
                plugin.getQuestManager().incrementProgress(p, qNow, produced);
            }
        });
    }

    private int estimateCraftsFromMatrixDiff(ItemStack[] before, ItemStack[] after) {
        if (before == null || after == null)
            return 0;

        int crafts = 0;
        int len = Math.min(before.length, after.length);

        for (int i = 0; i < len; i++) {
            ItemStack b = before[i];
            if (b == null || b.getType().isAir())
                continue;

            ItemStack a = after[i];

            int delta;
            if (a == null || a.getType().isAir()) {
                delta = b.getAmount();
            } else if (a.getType() != b.getType()) {
                delta = Math.max(1, b.getAmount() - a.getAmount());
            } else {
                delta = b.getAmount() - a.getAmount();
            }

            if (delta > crafts)
                crafts = delta;
        }

        return crafts;
    }

    private int countMaterial(PlayerInventory inv, Material type) {
        int total = 0;
        if (inv == null)
            return 0;

        for (ItemStack it : inv.getContents()) {
            if (it == null || it.getType().isAir())
                continue;
            if (it.getType() == type)
                total += it.getAmount();
        }
        return total;
    }

    private int countOnCursor(Player p, Material type) {
        ItemStack cursor = p.getItemOnCursor();
        if (cursor == null || cursor.getType().isAir())
            return 0;
        return cursor.getType() == type ? cursor.getAmount() : 0;
    }

    public void onFurnaceExtract(FurnaceExtractEvent e) {
        Player p = e.getPlayer();

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = plugin.getQuestManager().resolveObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.ITEM_SMELT &&
                o.getType() != QuestObjectiveType.ITEM_COOK) {
            return;
        }

        if (o.isTargetItem(e.getItemType())) {
            plugin.getQuestManager().incrementProgress(p, q, e.getItemAmount());
        }
    }

    public void onConsume(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = plugin.getQuestManager().resolveObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.CONSUME_ITEM)
            return;

        ItemStack item = e.getItem();
        if (item == null)
            return;

        if (o.isTargetItem(item)) {
            plugin.getQuestManager().incrementProgress(p, q, 1);
        }
    }

    public void onEnchant(EnchantItemEvent e) {
        Player p = e.getEnchanter();

        QuestDefinition q = getActiveQuest(p);
        if (q == null)
            return;

        QuestObjective o = plugin.getQuestManager().resolveObjective(p, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.ITEM_ENCHANT)
            return;

        ItemStack item = e.getItem();
        if (item == null)
            return;

        if (o.isTargetItem(item)) {
            plugin.getQuestManager().incrementProgress(p, q, 1);
        }
    }

    public void onCampfireInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block b = e.getClickedBlock();
        if (b == null || (b.getType() != Material.CAMPFIRE && b.getType() != Material.SOUL_CAMPFIRE)) return;

        ItemStack item = e.getItem();
        if (item == null || item.getType().isAir()) return;

        if (!(b.getState() instanceof Campfire campfire)) return;
        int currentItems = getCampfireItemCount(campfire);
        if (currentItems >= 4) return;

        Player p = e.getPlayer();

        plugin.getFoliaLib().getImpl().runAtLocation(b.getLocation(), __ -> {
            if (b.getType() != Material.CAMPFIRE && b.getType() != Material.SOUL_CAMPFIRE) return;
            if (!(b.getState() instanceof Campfire after)) return;

            int newItems = getCampfireItemCount(after);
            if (newItems > currentItems) {
                campfirePlacements.computeIfAbsent(b.getLocation(), k -> new LinkedList<>()).add(p.getUniqueId());
            }
        });
    }

    public void onBlockCook(BlockCookEvent e) {
        Block b = e.getBlock();
        if (b.getType() != Material.CAMPFIRE && b.getType() != Material.SOUL_CAMPFIRE) return;

        Queue<UUID> queue = campfirePlacements.get(b.getLocation());
        if (queue == null || queue.isEmpty()) return;

        UUID playerId = queue.poll();
        if (queue.isEmpty()) {
            campfirePlacements.remove(b.getLocation());
        }

        Player p = Bukkit.getPlayer(playerId);
        if (p == null || !p.isOnline()) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = plugin.getQuestManager().resolveObjective(p, q);
        if (o == null || o.getType() != QuestObjectiveType.ITEM_COOK) return;

        ItemStack result = e.getResult();
        if (o.isTargetItem(result.getType())) {
            plugin.getQuestManager().incrementProgress(p, q, 1);
        }
    }

    public void onCampfireBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (b.getType() == Material.CAMPFIRE || b.getType() == Material.SOUL_CAMPFIRE) {
            campfirePlacements.remove(b.getLocation());
        }
    }

    public void onFurnaceBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Material type = b.getType();

        if (type != Material.FURNACE && type != Material.SMOKER && type != Material.BLAST_FURNACE) return;

        if (!(b.getState() instanceof Furnace furnace)) return;

        ItemStack result = furnace.getInventory().getResult();
        if (result == null || result.getType().isAir()) return;

        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = plugin.getQuestManager().resolveObjective(p, q);
        if (o == null) return;

        if (o.getType() != QuestObjectiveType.ITEM_SMELT && o.getType() != QuestObjectiveType.ITEM_COOK) return;

        if (o.isTargetItem(result)) {
            plugin.getQuestManager().incrementProgress(p, q, result.getAmount());
        }
    }

    private int getCampfireItemCount(Campfire campfire) {
        int count = 0;
        for (int i = 0; i < campfire.getSize(); i++) {
            ItemStack item = campfire.getItem(i);
            if (item != null && !item.getType().isAir()) {
                count++;
            }
        }
        return count;
    }
}
