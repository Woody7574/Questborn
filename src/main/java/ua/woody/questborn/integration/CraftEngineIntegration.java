package ua.woody.questborn.integration;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class CraftEngineIntegration {
    private final boolean isEnabled;

    public CraftEngineIntegration(ua.woody.questborn.QuestbornPlugin plugin) {
        this.isEnabled = Bukkit.getPluginManager().getPlugin("CraftEngine") != null &&
                         plugin.getConfig().getBoolean("integration.craftengine.enabled", true);
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isCustomItem(ItemStack item) {
        if (!isEnabled || item == null) return false;
        try {
            Class<?> clazz = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineItems");
            return (boolean) clazz.getMethod("isCustomItem", ItemStack.class).invoke(null, item);
        } catch (Exception e) {
            return false;
        }
    }

    public String getCustomItemId(ItemStack item) {
        if (!isEnabled || item == null) return null;
        try {
            Class<?> clazz = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineItems");
            Object key = clazz.getMethod("getCustomItemId", ItemStack.class).invoke(null, item);
            return key != null ? key.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public ItemStack getCustomItem(String id) {
        if (!isEnabled || id == null) return null;
        try {
            String keyStr = id.replace("craftengine:", "").replace("ce:", "");
            Class<?> clazz = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineItems");
            Object def = clazz.getMethod("byId", String.class).invoke(null, keyStr);
            if (def != null) {
                return (ItemStack) def.getClass().getMethod("buildBukkitItem").invoke(def);
            } else {
                Class<?> blockClazz = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineBlocks");
                Class<?> keyClazz = Class.forName("net.momirealms.craftengine.core.util.Key");
                Object keyObj = keyClazz.getMethod("of", String.class).invoke(null, keyStr);
                Object blockDef = blockClazz.getMethod("byId", keyClazz).invoke(null, keyObj);
                if (blockDef != null) {
                    Object itemDef = blockDef.getClass().getMethod("item").invoke(blockDef);
                    if (itemDef != null) {
                        return (ItemStack) itemDef.getClass().getMethod("buildBukkitItem").invoke(itemDef);
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public boolean isCustomBlock(Block block) {
        if (!isEnabled || block == null) return false;
        try {
            Class<?> clazz = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineBlocks");
            return (boolean) clazz.getMethod("isCustomBlock", Block.class).invoke(null, block);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getCustomBlockId(Block block) {
        if (!isEnabled || block == null) return null;
        try {
            Class<?> clazz = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineBlocks");
            Object state = clazz.getMethod("getCustomBlockState", Block.class).invoke(null, block);
            if (state != null) {
                Object owner = state.getClass().getMethod("owner").invoke(state);
                if (owner != null) {
                    Class<?> holderClass = Class.forName("net.momirealms.craftengine.core.registry.Holder");
                    Object def = holderClass.getMethod("value").invoke(owner);
                    if (def != null) {
                        Class<?> defClass = Class.forName("net.momirealms.craftengine.core.block.BlockDefinition");
                        Object id = defClass.getMethod("id").invoke(def);
                        if (id != null) {
                            return id.toString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public boolean isFurniture(org.bukkit.entity.Entity entity) {
        if (!isEnabled || entity == null) return false;
        try {
            Class<?> clazz = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineFurniture");
            return (boolean) clazz.getMethod("isFurniture", org.bukkit.entity.Entity.class).invoke(null, entity);
        } catch (Exception e) {
            return false;
        }
    }

    public String getFurnitureId(org.bukkit.entity.Entity entity) {
        if (!isEnabled || entity == null) return null;
        try {
            Class<?> clazz = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineFurniture");
            Object furniture = clazz.getMethod("getLoadedFurnitureByMetaEntity", org.bukkit.entity.Entity.class).invoke(null, entity);
            if (furniture != null) {
                Object key = furniture.getClass().getMethod("id").invoke(furniture);
                if (key != null) {
                    return key.toString();
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
