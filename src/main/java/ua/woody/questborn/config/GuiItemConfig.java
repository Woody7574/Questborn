package ua.woody.questborn.config;

import org.bukkit.Material;
import ua.woody.questborn.lang.ColorFormatter;
import java.util.List;
import java.util.ArrayList;

public class GuiItemConfig {
    private final Material material;
    private final String itemsAdderId;
    private final String name;
    private final List<String> lore;
    private final int slot;
    private final int customModelData;
    private final List<String> actions;
    private final String baseHead;

    public GuiItemConfig(Material material, String name, int slot) {
        this(material, null, name, null, slot, 0, null, null);
    }

    public GuiItemConfig(Material material, String itemsAdderId, String name, List<String> lore, int slot,
            int customModelData) {
        this(material, itemsAdderId, name, lore, slot, customModelData, null, null);
    }

    public GuiItemConfig(Material material, String itemsAdderId, String name, List<String> lore, int slot,
            int customModelData, List<String> actions) {
        this(material, itemsAdderId, name, lore, slot, customModelData, actions, null);
    }

    public GuiItemConfig(Material material, String itemsAdderId, String name, List<String> lore, int slot,
            int customModelData, List<String> actions, String baseHead) {
        this.material = material;
        this.itemsAdderId = itemsAdderId;
        this.name = name;
        this.lore = lore != null ? lore : new ArrayList<>();
        this.slot = slot;
        this.customModelData = customModelData;
        this.actions = actions != null ? actions : new ArrayList<>();
        this.baseHead = baseHead;
    }

    public Material getMaterial() {
        return material;
    }

    public String getItemsAdderId() {
        return itemsAdderId;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public int getSlot() {
        return slot;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public List<String> getActions() {
        return actions;
    }

    public org.bukkit.inventory.ItemStack createItemStack(ua.woody.questborn.QuestbornPlugin plugin) {
        org.bukkit.inventory.ItemStack item;

        if (itemsAdderId != null && plugin != null) {
            if (itemsAdderId.startsWith("ce:") && plugin.getCraftEngineIntegration() != null && plugin.getCraftEngineIntegration().isEnabled()) {
                item = plugin.getCraftEngineIntegration().getCustomItem(itemsAdderId.substring(3));
            } else if ((itemsAdderId.startsWith("ia:") || itemsAdderId.startsWith("itemsadder:")) && plugin.getItemsAdderIntegration() != null && plugin.getItemsAdderIntegration().isEnabled()) {
                String iaId = itemsAdderId.startsWith("ia:") ? itemsAdderId.substring(3) : (itemsAdderId.startsWith("itemsadder:") ? itemsAdderId.substring(11) : itemsAdderId);
                item = plugin.getItemsAdderIntegration().getCustomItem(iaId);
            } else if (plugin.getItemsAdderIntegration() != null && plugin.getItemsAdderIntegration().isEnabled()) {
                item = plugin.getItemsAdderIntegration().getCustomItem(itemsAdderId);
            } else {
                item = null;
            }
            if (item == null) {
                item = new org.bukkit.inventory.ItemStack(material != null ? material : Material.STONE);
            }
        } else {
            if (material == null) {
                return new org.bukkit.inventory.ItemStack(Material.AIR);
            }
            item = new org.bukkit.inventory.ItemStack(material);
        }

        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(ColorFormatter.applyColors(name));
            }
            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ColorFormatter.applyColors(line));
                }
                meta.setLore(coloredLore);
            }
            if (customModelData != 0) {
                meta.setCustomModelData(customModelData);
            }
            if (!actions.isEmpty()) {
                String actionsStr = String.join(";;;", actions);

                org.bukkit.plugin.Plugin pluginInstance = org.bukkit.Bukkit.getPluginManager().getPlugin("Questborn");
                if (pluginInstance != null) {
                    meta.getPersistentDataContainer().set(
                        new org.bukkit.NamespacedKey(pluginInstance, "quest-custom-action"),
                        org.bukkit.persistence.PersistentDataType.STRING,
                        actionsStr
                    );
                }
            }
            if (baseHead != null && !baseHead.isEmpty() && meta instanceof org.bukkit.inventory.meta.SkullMeta) {
                org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) meta;
                java.util.UUID uuid = new java.util.UUID(baseHead.hashCode(), baseHead.hashCode());
                com.destroystokyo.paper.profile.PlayerProfile profile = org.bukkit.Bukkit.createProfile(uuid);
                profile.setProperty(new com.destroystokyo.paper.profile.ProfileProperty("textures", baseHead));
                skullMeta.setPlayerProfile(profile);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public org.bukkit.inventory.ItemStack createItemStack() {
        return createItemStack(null);
    }
}
