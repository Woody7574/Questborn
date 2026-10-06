package ua.woody.questborn.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.integration.ItemsAdderIntegration;
import ua.woody.questborn.integration.CraftEngineIntegration;
import ua.woody.questborn.lang.ColorFormatter;

import java.util.Objects;
import java.util.Optional;

public class QuestItem {
    private final Material material;
    private final int customModelData;
    private final String itemsAdderId;
    private final String craftEngineId;
    private final String displayName;

    public QuestItem(Material material) {
        this(material, 0, null, null, null);
    }

    public QuestItem(Material material, int customModelData) {
        this(material, customModelData, null, null, null);
    }

    public QuestItem(String itemsAdderId) {
        this(null, 0, itemsAdderId, null, null);
    }

    public QuestItem(String customPluginId, boolean isCraftEngine) {
        this(null, 0, isCraftEngine ? null : customPluginId, isCraftEngine ? customPluginId : null, null);
    }

    public QuestItem(Material material, int customModelData, String itemsAdderId, String craftEngineId, String displayName) {
        this.material = material;
        this.customModelData = customModelData;
        this.itemsAdderId = itemsAdderId;
        this.craftEngineId = craftEngineId;
        this.displayName = displayName;
    }

    public boolean isItemsAdderItem() {
        return itemsAdderId != null && !itemsAdderId.isEmpty();
    }

    public boolean isCraftEngineItem() {
        return craftEngineId != null && !craftEngineId.isEmpty();
    }

    public boolean isVanilla() {
        return !isItemsAdderItem() && !isCraftEngineItem();
    }

    public String getItemsAdderId() {
        return itemsAdderId;
    }

    public String getCraftEngineId() {
        return craftEngineId;
    }

    public Material getMaterial() {
        return material;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public boolean matches(ItemStack stack, ItemsAdderIntegration ia, CraftEngineIntegration ce) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }

        if (isItemsAdderItem()) {
            if (ia != null && ia.isEnabled()) {
                if (ia.isCustomItem(stack)) {
                    String id = ia.getCustomItemId(stack);
                    return itemsAdderId.equalsIgnoreCase(id) || itemsAdderId.equalsIgnoreCase("itemsadder:" + id);
                }
            }
            return false;
        }

        if (isCraftEngineItem()) {
            if (ce != null && ce.isEnabled()) {
                if (ce.isCustomItem(stack)) {
                    String id = ce.getCustomItemId(stack);
                    return craftEngineId.equalsIgnoreCase(id) || craftEngineId.equalsIgnoreCase("craftengine:" + id) || craftEngineId.equalsIgnoreCase("ce:" + id);
                }
            }
            return false;
        }

        if (stack.getType() != material) {
            return false;
        }
        if (customModelData > 0) {
            if (!stack.hasItemMeta() || !stack.getItemMeta().hasCustomModelData()) {
                return false;
            }
            return stack.getItemMeta().getCustomModelData() == customModelData;
        }
        return true;
    }

    public ItemStack toItemStack(ItemsAdderIntegration ia, int amount) {
        ItemStack stack = null;

        if (isItemsAdderItem() && ia != null && ia.isEnabled()) {
            stack = ia.getCustomItem(itemsAdderId);
        }

        if (stack == null) {
            Material mat = (material != null) ? material : Material.STONE;
            stack = new ItemStack(mat);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                if (customModelData != 0) {
                    meta.setCustomModelData(customModelData);
                }
                if (displayName != null) {
                    meta.setDisplayName(ColorFormatter.applyColors(displayName));
                }
                stack.setItemMeta(meta);
            }
        }

        stack.setAmount(Math.max(1, amount));
        return stack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        QuestItem questItem = (QuestItem) o;
        return customModelData == questItem.customModelData &&
                material == questItem.material &&
                Objects.equals(itemsAdderId, questItem.itemsAdderId) &&
                Objects.equals(craftEngineId, questItem.craftEngineId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(material, customModelData, itemsAdderId, craftEngineId);
    }

    @Override
    public String toString() {
        if (isItemsAdderItem())
            return "ItemsAdder:" + itemsAdderId;
        if (isCraftEngineItem())
            return "CraftEngine:" + craftEngineId;
        return material + (customModelData != 0 ? ":" + customModelData : "");
    }

    public String serialize() {
        if (isItemsAdderItem()) {
            return "ia:" + itemsAdderId;
        }
        if (isCraftEngineItem()) {
            return "ce:" + craftEngineId;
        }
        return "vanilla:" + (material != null ? material.name() : "AIR") + ":" + customModelData;
    }

    public static QuestItem deserialize(String str) {
        if (str == null || str.isEmpty()) return null;
        if (str.startsWith("ia:")) {
            return new QuestItem(str.substring(3), false);
        } else if (str.startsWith("ce:")) {
            return new QuestItem(str.substring(3), true);
        } else if (str.startsWith("vanilla:")) {
            String[] parts = str.substring(8).split(":");
            if (parts.length >= 1) {
                try {
                    Material mat = Material.valueOf(parts[0]);
                    int cmd = 0;
                    if (parts.length >= 2) {
                        cmd = Integer.parseInt(parts[1]);
                    }
                    return new QuestItem(mat, cmd);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }
}
