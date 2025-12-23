package ua.woody.questborn.rewards.modules;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.woody.questborn.lang.ColorFormatter;
import ua.woody.questborn.rewards.RewardExecutionContext;
import ua.woody.questborn.rewards.RewardHandler;
import ua.woody.questborn.rewards.RewardModule;

import java.util.*;

public class ItemRewardModule implements RewardModule {

    @Override
    public String getKey() {
        return "items";
    }

    @Override
    public void execute(RewardExecutionContext ctx, Player player, Object config) {
        if (config == null) return;

        boolean silent = false;
        Object listObj;

        if (config instanceof Map<?, ?> map) {
            silent = RewardHandler.getSilent(map);
            listObj = map.get("list");
        } else {
            listObj = config;
        }

        if (!(listObj instanceof List<?> rawList)) return;

        List<ItemStack> itemsToGive = new ArrayList<>();

        for (Object entry : rawList) {
            if (!(entry instanceof Map<?, ?> m)) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) m;
            ItemStack item = buildItem(data, ctx);
            if (item != null && item.getAmount() > 0) {
                itemsToGive.add(item);
            }
        }

        for (ItemStack item : itemsToGive) {
            player.getInventory().addItem(item);
        }

        if (!itemsToGive.isEmpty() && !silent && !ctx.isGlobalSilent()) {
            player.sendMessage(ctx.getLang().tr("reward.items.received"));
        }
    }

    private ItemStack buildItem(Map<String, Object> data, RewardExecutionContext ctx) {
        String materialName = String.valueOf(data.getOrDefault("material", "STONE"));
        Material mat = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
        if (mat == null) {
            ctx.getPlugin().getLogger().warning("[Questborn] Unknown material in reward: " + materialName);
            return null;
        }

        int amount = 1;
        try {
            amount = Integer.parseInt(String.valueOf(data.get("amount")));
        } catch (Exception ignored) {}

        ItemStack item = new ItemStack(mat, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (data.containsKey("name")) {
            meta.setDisplayName(ColorFormatter.applyColors(String.valueOf(data.get("name"))));
        }

        if (data.containsKey("lore")) {
            List<String> lore = new ArrayList<>();
            Object rawLore = data.get("lore");
            if (rawLore instanceof List<?> list) {
                for (Object line : list) {
                    lore.add(ColorFormatter.applyColors(String.valueOf(line)));
                }
            } else {
                lore.add(ColorFormatter.applyColors(String.valueOf(rawLore)));
            }
            meta.setLore(lore);
        }

        if (data.containsKey("enchantments")) {
            Object raw = data.get("enchantments");
            if (raw instanceof List<?> list) {
                for (Object o : list) {
                    String[] p = String.valueOf(o).split(":");
                    Enchantment ench = parseEnchantment(p[0]);
                    if (ench != null) {
                        int level = (p.length > 1 ? Integer.parseInt(p[1]) : 1);
                        meta.addEnchant(ench, level, true);
                    }
                }
            }
        }

        if (data.containsKey("flags")) {
            Object raw = data.get("flags");
            if (raw instanceof List<?> list) {
                for (Object o : list) {
                    try {
                        meta.addItemFlags(ItemFlag.valueOf(String.valueOf(o).toUpperCase(Locale.ROOT)));
                    } catch (Exception ignored) {}
                }
            }
        }

        if (data.containsKey("custom-model-data")) {
            try {
                meta.setCustomModelData(Integer.parseInt(String.valueOf(data.get("custom-model-data"))));
            } catch (Exception ignored) {}
        }

        if (data.containsKey("unbreakable")) {
            meta.setUnbreakable(Boolean.parseBoolean(String.valueOf(data.get("unbreakable"))));
        }

        item.setItemMeta(meta);
        return item;
    }

    private Enchantment parseEnchantment(String name) {
        try {
            Enchantment e = Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
            if (e != null) return e;
        } catch (Exception ignored) {}
        return Enchantment.getByName(name.toUpperCase(Locale.ROOT));
    }
}
