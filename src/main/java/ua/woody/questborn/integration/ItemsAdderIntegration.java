package ua.woody.questborn.integration;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomMob;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;

import java.util.Optional;

public class ItemsAdderIntegration {
    private final boolean enabled;

    public ItemsAdderIntegration(QuestbornPlugin plugin) {
        if (plugin.getConfig().getBoolean("integration.itemsadder.enabled", true)
                && Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            this.enabled = true;
        } else {
            this.enabled = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ItemStack getCustomItem(String id) {
        if (!enabled || id == null)
            return null;
        try {
            String fullId = id.contains(":") ? id : "itemsadder:" + id;
            CustomStack stack = CustomStack.getInstance(fullId);
            if (stack == null && !fullId.equals(id)) {
                stack = CustomStack.getInstance(id);
            }
            if (stack != null) {
                return stack.getItemStack();
            }
        } catch (Exception e) {
        }
        return null;
    }

    public boolean isCustomItem(ItemStack item) {
        if (!enabled || item == null)
            return false;
        return CustomStack.byItemStack(item) != null;
    }

    public boolean isItemsAdderItem(String id) {
        if (!enabled || id == null)
            return false;
        String fullId = id.contains(":") ? id : "itemsadder:" + id;
        CustomStack stack = CustomStack.getInstance(fullId);
        if (stack == null && !fullId.equals(id)) {
            stack = CustomStack.getInstance(id);
        }
        return stack != null;
    }

    public String getCustomItemId(ItemStack item) {
        if (!enabled || item == null)
            return null;
        CustomStack stack = CustomStack.byItemStack(item);
        return stack != null ? stack.getNamespacedID() : null;
    }

    public String getCustomItemDisplayName(String id) {
        if (!enabled || id == null)
            return null;
        try {
            String fullId = id.contains(":") ? id : "itemsadder:" + id;
            CustomStack stack = CustomStack.getInstance(fullId);
            if (stack == null && !fullId.equals(id)) {
                stack = CustomStack.getInstance(id);
            }
            if (stack != null) {
                return stack.getDisplayName();
            }
        } catch (Exception e) {
        }
        return null;
    }

    public boolean isCustomBlock(Block block) {
        if (!enabled || block == null) return false;
        try {
            return CustomBlock.byAlreadyPlaced(block) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public String getCustomBlockId(Block block) {
        if (!enabled || block == null) return null;
        try {
            CustomBlock cb = CustomBlock.byAlreadyPlaced(block);
            if (cb != null) {
                return cb.getNamespacedID();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public boolean isCustomMob(Entity entity) {
        if (!enabled) return false;
        try {
            return dev.lone.itemsadder.api.CustomMob.byEntity(entity) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public String getCustomMobId(Entity entity) {
        if (!enabled) return null;
        try {
            dev.lone.itemsadder.api.CustomMob mob = dev.lone.itemsadder.api.CustomMob.byEntity(entity);
            if (mob != null) {
                return mob.getNamespacedID();
            }
        } catch (Throwable t) {
        }
        return null;
    }
}
