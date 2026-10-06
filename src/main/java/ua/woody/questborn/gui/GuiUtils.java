package ua.woody.questborn.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.config.GuiItemConfig;
import ua.woody.questborn.config.GuiLayout;
import ua.woody.questborn.lang.ColorFormatter;

import java.util.List;

public class GuiUtils {
    public static void fillLayout(QuestbornPlugin plugin, BaseGui gui, GuiLayout layout) {
        if (layout == null) return;

        GuiItemConfig fillerConfig = layout.getItem("filler");
        if (fillerConfig != null) {
            ItemStack fillerStack = fillerConfig.createItemStack(plugin);
            ItemMeta meta = fillerStack.getItemMeta();
            if (meta != null) {
                String name = fillerConfig.getName();
                meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', name != null ? name : " "));
                fillerStack.setItemMeta(meta);
            }

            for (int slot : layout.getMaskSlots()) {
                if (slot >= 0 && slot < gui.getRows() * 9) {
                    gui.setItem(slot, new dev.triumphteam.gui.guis.GuiItem(fillerStack.clone(), event -> event.setCancelled(true)));
                }
            }
        }

        if (layout.getItems() != null) {
            for (java.util.Map.Entry<String, GuiItemConfig> entry : layout.getItems().entrySet()) {
                if ("filler".equals(entry.getKey())) continue;

                GuiItemConfig cfg = entry.getValue();
                if (!cfg.getActions().isEmpty() && cfg.getSlot() >= 0 && cfg.getSlot() < gui.getRows() * 9) {
                    ItemStack itemStack = cfg.createItemStack(plugin);
                    GuiItem guiItem = new dev.triumphteam.gui.guis.GuiItem(itemStack, event -> {
                        event.setCancelled(true);
                        Player p = (Player) event.getWhoClicked();
                        handleCustomActions(plugin, p, cfg.getActions());
                    });
                    gui.setItem(cfg.getSlot(), guiItem);
                }
            }
        }
    }

    public static void handleCustomActions(QuestbornPlugin plugin, Player player, List<String> actions) {
        if (actions == null || actions.isEmpty()) return;

        boolean closeInv = false;
        for (String act : actions) {
            String action = act.trim();
            if (action.isEmpty()) continue;

            if (action.equalsIgnoreCase("[close]")) {
                closeInv = true;
            } else if (action.toLowerCase().startsWith("[player] ")) {
                String cmd = action.substring(9).trim().replace("%player%", player.getName());
                player.performCommand(cmd);
            } else if (action.toLowerCase().startsWith("[console] ")) {
                String cmd = action.substring(10).trim().replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }

        playClickSound(plugin, player);

        if (closeInv) {
            plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> player.closeInventory());
        }
    }

    public static void playClickSound(QuestbornPlugin plugin, Player player) {
        playClickSound(plugin, player, null);
    }

    public static void playClickSound(QuestbornPlugin plugin, Player player, String npcId) {
        if (npcId != null && !npcId.isEmpty()) {
            ua.woody.questborn.model.NpcConfig npc = plugin.getNpcManager().getConfigByNpcId(npcId);
            if (npc != null && npc.getVoice() != null && !npc.getVoice().equalsIgnoreCase("none")) {
                org.bukkit.Sound sound = ua.woody.questborn.gui.DialogueGui.getSoundFromVoice(npc.getVoice());
                if (sound != null) {
                    float pitch = ua.woody.questborn.gui.DialogueGui.getPitchFromVoice(npc.getVoice());
                    player.playSound(player.getLocation(), sound, 0.4f, pitch);
                    return;
                }
            }
        }

        if (plugin.getSoundManager() != null) {
            plugin.getSoundManager().playGuiClick(player);
        }
    }

    public static String getButtonName(GuiLayout layout, String itemKey, String localizedKey) {
        if (layout != null) {
            GuiItemConfig itemConfig = layout.getItem(itemKey);
            if (itemConfig != null && itemConfig.getName() != null) {
                return ColorFormatter.applyColors(itemConfig.getName());
            }
        }
        return null;
    }

    public static List<String> getButtonLore(GuiLayout layout, String itemKey, String localizedKey) {
        if (layout != null) {
            GuiItemConfig itemConfig = layout.getItem(itemKey);
            if (itemConfig != null && !itemConfig.getLore().isEmpty()) {
                return itemConfig.getLore().stream()
                        .map(ColorFormatter::applyColors)
                        .collect(java.util.stream.Collectors.toList());
            }
        }
        return java.util.Collections.emptyList();
    }

    public static ItemStack createActionButton(QuestbornPlugin plugin, GuiLayout layout, String itemId, Material defaultMaterial, String fallbackNameKey) {
        GuiItemConfig conf = (layout != null) ? layout.getItem(itemId) : null;
        ItemStack item;
        if (conf != null) {
            item = conf.createItemStack(plugin);
        } else {
            item = new ItemStack(defaultMaterial);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean nameSet = (conf != null && conf.getName() != null);
            if (!nameSet) {
                String name = getButtonName(layout, itemId, fallbackNameKey);
                if (name != null) {
                    meta.setDisplayName(name);
                }
            }

            boolean loreSet = (conf != null && !conf.getLore().isEmpty());
            if (!loreSet) {
                List<String> lore = getButtonLore(layout, itemId, fallbackNameKey + ".lore");
                if (!lore.isEmpty()) {
                    meta.setLore(lore);
                }
            }

            applyAllItemFlags(meta);

            item.setItemMeta(meta);
        }

        return item;
    }

    public static void applyAllItemFlags(ItemMeta meta) {
        if (meta != null) {
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_DESTROYS);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_PLACED_ON);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_POTION_EFFECTS);
        }
    }

    public static String extractBaseHead(org.bukkit.inventory.ItemStack item) {
        if (item != null && item.getType() == org.bukkit.Material.PLAYER_HEAD && item.hasItemMeta()) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.SkullMeta) {
                org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) meta;
                com.destroystokyo.paper.profile.PlayerProfile profile = skullMeta.getPlayerProfile();
                if (profile != null) {
                    for (com.destroystokyo.paper.profile.ProfileProperty property : profile.getProperties()) {
                        if (property.getName().equals("textures")) {
                            return property.getValue();
                        }
                    }
                }
            }
        }
        return null;
    }

    public static void applyBaseHead(org.bukkit.inventory.ItemStack item, String baseHead) {
        if (baseHead != null && !baseHead.isEmpty() && item != null && item.getType() == org.bukkit.Material.PLAYER_HEAD) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.SkullMeta) {
                org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) meta;
                java.util.UUID uuid = new java.util.UUID(baseHead.hashCode(), baseHead.hashCode());
                com.destroystokyo.paper.profile.PlayerProfile profile = org.bukkit.Bukkit.createProfile(uuid);
                profile.setProperty(new com.destroystokyo.paper.profile.ProfileProperty("textures", baseHead));
                skullMeta.setPlayerProfile(profile);
                item.setItemMeta(skullMeta);
            }
        }
    }
}
