package ua.woody.questborn.utils;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HexItemBuilder {
    private final ItemBuilder builder;
    private String name;
    private final List<String> lore = new ArrayList<>();

    private HexItemBuilder(Material material) {
        this.builder = ItemBuilder.from(material);
    }

    private HexItemBuilder(ItemStack item) {
        this.builder = ItemBuilder.from(item);
    }

    private HexItemBuilder(ItemBuilder builder) {
        this.builder = builder;
    }

    public static HexItemBuilder from(Material material) {
        return new HexItemBuilder(material);
    }

    public static HexItemBuilder from(ItemStack item) {
        return new HexItemBuilder(item);
    }

    public static HexItemBuilder skull(String base64) {
        return HexItemBuilder.from(dev.triumphteam.gui.builder.item.ItemBuilder.skull().texture(base64).build());
    }

    public HexItemBuilder name(String name) {
        this.name = name;
        return this;
    }

    public HexItemBuilder lore(Object... elements) {
        for (Object elem : elements) {
            if (elem instanceof String) {
                this.lore.add((String) elem);
            } else if (elem instanceof List) {
                this.lore.addAll((List<String>) elem);
            }
        }
        return this;
    }

    public HexItemBuilder lore(List<String> lore) {
        this.lore.addAll(lore);
        return this;
    }

    public HexItemBuilder flags(ItemFlag... flags) {
        builder.flags(flags);
        return this;
    }

    public HexItemBuilder model(int model) {
        builder.model(model);
        return this;
    }

    public HexItemBuilder amount(int amount) {
        builder.amount(amount);
        return this;
    }

    public HexItemBuilder glow(boolean glow) {
        builder.glow(glow);
        return this;
    }

    public HexItemBuilder enchant(org.bukkit.enchantments.Enchantment enchant) {
        builder.enchant(enchant);
        return this;
    }

    public ItemStack build() {
        ItemStack item = builder.build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(name);
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            try {
                meta.addItemFlags(
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_POTION_EFFECTS,
                    ItemFlag.HIDE_DESTROYS,
                    ItemFlag.HIDE_PLACED_ON,
                    ItemFlag.HIDE_UNBREAKABLE
                );

                try {
                    meta.addItemFlags(ItemFlag.valueOf("HIDE_ARMOR_TRIM"));
                } catch (IllegalArgumentException ignored) {}
            } catch (Exception ignored) {}

            item.setItemMeta(meta);
        }
        return item;
    }
}
