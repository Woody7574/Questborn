package ua.woody.questborn.util;

import org.bukkit.Material;
import ua.woody.questborn.lang.LanguageManager;

import java.util.Locale;

public class ItemDisplayUtil {
    public static String getItemsAdderDisplayName(String iaId, LanguageManager lang) {
        if (lang.getPlugin() != null && lang.getPlugin().getItemsAdderIntegration() != null) {
            ua.woody.questborn.integration.ItemsAdderIntegration ia = lang.getPlugin().getItemsAdderIntegration();
            if (ia.isEnabled()) {
                org.bukkit.inventory.ItemStack stack = ia.getCustomItem(iaId);
                if (stack != null && stack.getItemMeta() != null) {
                    if (stack.getItemMeta().hasLocalizedName()) {
                        return stack.getItemMeta().getLocalizedName();
                    } else if (stack.getItemMeta().hasDisplayName()) {
                        return stack.getItemMeta().getDisplayName();
                    }
                }

                String nativeName = ia.getCustomItemDisplayName(iaId);
                if (nativeName != null && !nativeName.isEmpty()) {
                    return nativeName;
                }
            }
        }
        return formatMaterialName(iaId);
    }

    public static String getCraftEngineDisplayName(String ceId, LanguageManager lang) {
        if (lang.getPlugin() != null && lang.getPlugin().getCraftEngineIntegration() != null) {
            ua.woody.questborn.integration.CraftEngineIntegration ce = lang.getPlugin().getCraftEngineIntegration();
            if (ce.isEnabled()) {
                org.bukkit.inventory.ItemStack stack = ce.getCustomItem(ceId);
                if (stack != null && stack.getItemMeta() != null) {
                    if (stack.getItemMeta().hasLocalizedName()) {
                        return stack.getItemMeta().getLocalizedName();
                    } else if (stack.getItemMeta().hasDisplayName()) {
                        return stack.getItemMeta().getDisplayName();
                    }
                }
            }
        }
        return formatMaterialName(ceId);
    }

    public static String findLocalization(Material material, LanguageManager lang, boolean blocksFirst) {
        if (material == null)
            return "Unknown";
        String materialName = material.name();
        String itemLoc = lang.localizeItem(materialName);
        String blockLoc = lang.localizeMaterial(material);
        if (blocksFirst) {
            if (isValidLocalization(blockLoc, materialName))
                return blockLoc;
            if (isValidLocalization(itemLoc, materialName))
                return itemLoc;
        } else {
            if (isValidLocalization(itemLoc, materialName))
                return itemLoc;
            if (isValidLocalization(blockLoc, materialName))
                return blockLoc;
        }
        return formatMaterialName(materialName);
    }

    public static boolean isValidLocalization(String text, String materialName) {
        if (text == null)
            return false;
        if (text.isEmpty())
            return false;
        String lowerText = text.toLowerCase();
        if (lowerText.contains("unknown") ||
                lowerText.startsWith("locale.")) {
            return false;
        }
        if (text.equalsIgnoreCase(materialName))
            return false;
        if (text.contains("minecraft:"))
            return false;
        if (text.equals(text.toUpperCase()) && text.contains("_"))
            return false;
        return true;
    }

    public static String formatMaterialName(String materialName) {
        if (materialName == null)
            return "Unknown";

        if (materialName.contains(":")) {
            materialName = materialName.substring(materialName.lastIndexOf(':') + 1);
        }

        String lower = materialName.toLowerCase(Locale.ROOT);
        String[] words = lower.split("_");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (words[i].isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(words[i].charAt(0)));
            if (words[i].length() > 1) {
                sb.append(words[i].substring(1));
            }
            if (i < words.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
