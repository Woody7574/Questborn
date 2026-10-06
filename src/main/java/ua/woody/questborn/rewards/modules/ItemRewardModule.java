package ua.woody.questborn.rewards.modules;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.woody.questborn.integration.ItemsAdderIntegration;
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
        if (config == null)
            return;

        boolean silent = false;
        Object listObj;

        if (config instanceof Map<?, ?> map) {
            silent = RewardHandler.getSilent(map);
            listObj = map.get("list");
        } else {
            listObj = config;
        }

        if (!(listObj instanceof List<?> rawList))
            return;

        List<ItemStack> itemsToGive = new ArrayList<>();

        for (Object entry : rawList) {
            if (entry instanceof ItemStack stack) {
                ItemStack cloned = stack.clone();
                cloned.setAmount(Math.max(1, cloned.getAmount() * ctx.getMultiplier()));
                itemsToGive.add(cloned);
                continue;
            }
            if (entry instanceof Map<?, ?> m) {
                if (m.containsKey("==")) {
                    try {
                        @SuppressWarnings("unchecked")
                        ItemStack deserialized = ItemStack.deserialize((Map<String, Object>) m);
                        if (deserialized != null) {
                            deserialized.setAmount(Math.max(1, deserialized.getAmount() * ctx.getMultiplier()));
                            itemsToGive.add(deserialized);
                            continue;
                        }
                    } catch (Exception ignored) {}
                }
            }
            if (entry instanceof String strEntry) {
                String strLower = strEntry.toLowerCase(Locale.ROOT);
                if (strLower.startsWith("ce:") || strLower.startsWith("craftengine:")) {
                    String[] parts = strEntry.split("[:;]");
                    String id = parts.length > 1 ? parts[1] : "";
                    if (ctx.getPlugin().getCraftEngineIntegration() != null && ctx.getPlugin().getCraftEngineIntegration().isEnabled()) {
                        ItemStack ceItem = ctx.getPlugin().getCraftEngineIntegration().getCustomItem(id);
                        if (ceItem != null) {
                            int amt = 1;
                            if (parts.length > 2) {
                                try { amt = Integer.parseInt(parts[2]); } catch (Exception ignored) {}
                            }
                            amt *= ctx.getMultiplier();
                            amt = Math.max(1, amt);
                            ceItem = ceItem.clone();
                            ceItem.setAmount(amt);
                            itemsToGive.add(ceItem);
                        }
                    }
                    continue;
                } else if (strLower.startsWith("ia:") || strLower.startsWith("itemsadder:")) {
                    String[] parts = strEntry.split("[:;]");
                    String id = parts.length > 1 ? parts[1] : "";
                    if (ctx.getPlugin().getItemsAdderIntegration() != null && ctx.getPlugin().getItemsAdderIntegration().isEnabled()) {
                        ItemStack iaItem = ctx.getPlugin().getItemsAdderIntegration().getCustomItem(id);
                        if (iaItem != null) {
                            int amt = 1;
                            if (parts.length > 2) {
                                try { amt = Integer.parseInt(parts[2]); } catch (Exception ignored) {}
                            }
                            amt *= ctx.getMultiplier();
                            amt = Math.max(1, amt);
                            iaItem = iaItem.clone();
                            iaItem.setAmount(amt);
                            itemsToGive.add(iaItem);
                        }
                    }
                    continue;
                }

                String[] parts = strEntry.split("[:;]");
                Material mat = Material.matchMaterial(parts[0]);
                if (mat != null) {
                    int amt = 1;
                    if (parts.length > 1) {
                        try { amt = Integer.parseInt(parts[1]); } catch (Exception ignored) {}
                    }
                    amt *= ctx.getMultiplier();
                    amt = Math.max(1, amt);
                    itemsToGive.add(new ItemStack(mat, amt));
                }
                continue;
            }
            if (!(entry instanceof Map<?, ?> m))
                continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) m;
            ItemStack item = buildItem(data, ctx);
            if (item != null && item.getAmount() > 0) {
                itemsToGive.add(item);
            }
        }

        boolean droppedAny = false;
        for (ItemStack item : itemsToGive) {
            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
            if (leftovers != null && !leftovers.isEmpty()) {
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    droppedAny = true;
                }
            }
        }

        if (!itemsToGive.isEmpty() && !silent && !ctx.isGlobalSilent()) {
            ctx.send(player, "rewards.items.received");
            if (droppedAny) {
                ctx.send(player, "rewards.items.dropped");
            }
        }
    }

    private ItemStack buildItem(Map<String, Object> data, RewardExecutionContext ctx) {
        int amount = 1;
        try {
            if (data.containsKey("amount")) {
                amount = Integer.parseInt(String.valueOf(data.get("amount")));
            }
        } catch (Exception ignored) {
        }
        amount *= ctx.getMultiplier();
        amount = Math.max(1, amount);

        String itemsAdderId = null;
        if (data.containsKey("itemsadder_id")) {
            itemsAdderId = String.valueOf(data.get("itemsadder_id"));
        } else if (data.containsKey("itemsadder")) {
            itemsAdderId = String.valueOf(data.get("itemsadder"));
        } else if (data.containsKey("ia")) {
            itemsAdderId = String.valueOf(data.get("ia"));
        } else if (data.containsKey("custom_item")) {
            itemsAdderId = String.valueOf(data.get("custom_item"));
        } else if (data.containsKey("material")) {
            String matName = String.valueOf(data.get("material"));
            if (matName.toLowerCase(Locale.ROOT).startsWith("itemsadder:")) {
                itemsAdderId = matName.substring(11);
            } else if (matName.toLowerCase(Locale.ROOT).startsWith("ia:")) {
                itemsAdderId = matName.substring(3);
            }
        }

        ItemStack item = null;
        if (itemsAdderId != null && ctx.getPlugin().getItemsAdderIntegration() != null) {
            ItemsAdderIntegration ia = ctx.getPlugin().getItemsAdderIntegration();
            if (ia.isEnabled()) {
                ItemStack iaStack = ia.getCustomItem(itemsAdderId);
                if (iaStack != null) {
                    item = iaStack.clone();
                    item.setAmount(amount);
                }
            }
        }

        if (item == null) {
            String craftEngineId = null;
            if (data.containsKey("craftengine_id")) {
                craftEngineId = String.valueOf(data.get("craftengine_id"));
            } else if (data.containsKey("craftengine")) {
                craftEngineId = String.valueOf(data.get("craftengine"));
            } else if (data.containsKey("ce")) {
                craftEngineId = String.valueOf(data.get("ce"));
            } else if (data.containsKey("material")) {
                String matName = String.valueOf(data.get("material"));
                if (matName.toLowerCase(Locale.ROOT).startsWith("craftengine:")) {
                    craftEngineId = matName.substring(12);
                } else if (matName.toLowerCase(Locale.ROOT).startsWith("ce:")) {
                    craftEngineId = matName.substring(3);
                }
            }

            if (craftEngineId != null && ctx.getPlugin().getCraftEngineIntegration() != null) {
                ua.woody.questborn.integration.CraftEngineIntegration ce = ctx.getPlugin().getCraftEngineIntegration();
                if (ce.isEnabled()) {
                    ItemStack ceStack = ce.getCustomItem(craftEngineId);
                    if (ceStack != null) {
                        item = ceStack.clone();
                        item.setAmount(amount);
                    }
                }
            }
        }

        if (item == null) {
            String materialName = String.valueOf(data.getOrDefault("material", "STONE"));
            Material mat = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
            if (mat == null) {
                ctx.getPlugin().getLogger().warning("[Questborn] Unknown material in reward: " + materialName);
                return null;
            }
            item = new ItemStack(mat, amount);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

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
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        if (data.containsKey("custom-model-data")) {
            try {
                meta.setCustomModelData(Integer.parseInt(String.valueOf(data.get("custom-model-data"))));
            } catch (Exception ignored) {
            }
        }

        if (data.containsKey("unbreakable")) {
            meta.setUnbreakable(Boolean.parseBoolean(String.valueOf(data.get("unbreakable"))));
        }

        if (data.containsKey("potion-type")) {
            if (meta instanceof org.bukkit.inventory.meta.PotionMeta) {
                org.bukkit.inventory.meta.PotionMeta pMeta = (org.bukkit.inventory.meta.PotionMeta) meta;
                try {
                    String ptStr = String.valueOf(data.get("potion-type")).toUpperCase(Locale.ROOT);
                    org.bukkit.potion.PotionType pType = org.bukkit.potion.PotionType.valueOf(ptStr);

                    boolean set = false;
                    try {
                        java.lang.reflect.Method mSetBasePotionType = pMeta.getClass().getMethod("setBasePotionType", org.bukkit.potion.PotionType.class);
                        mSetBasePotionType.invoke(pMeta, pType);
                        set = true;
                    } catch (Throwable ignored) {
                    }

                    if (!set) {
                        try {
                            Object potionData = Class.forName("org.bukkit.potion.PotionData").getConstructor(org.bukkit.potion.PotionType.class).newInstance(pType);
                            java.lang.reflect.Method mSetBasePotionData = pMeta.getClass().getMethod("setBasePotionData", Class.forName("org.bukkit.potion.PotionData"));
                            mSetBasePotionData.invoke(pMeta, potionData);
                        } catch (Throwable ignored) {
                        }
                    }
                } catch (Exception e) {
                    ctx.getPlugin().getLogger().warning("[Questborn] Invalid potion-type in reward: " + data.get("potion-type"));
                }
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("deprecation")
    private Enchantment parseEnchantment(String name) {
        try {
            NamespacedKey key = name.contains(":")
                    ? NamespacedKey.fromString(name.toLowerCase(Locale.ROOT))
                    : NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT));
            if (key != null) {
                Enchantment e = Enchantment.getByKey(key);
                if (e != null)
                    return e;
            }
        } catch (Exception ignored) {
        }
        return Enchantment.getByName(name.toUpperCase(Locale.ROOT));
    }
}
