package ua.woody.questborn.util;

import ua.woody.questborn.QuestbornPlugin;

public class ItemsAdderFontUtil {
    public static String processFontFormats(String text, QuestbornPlugin plugin) {
        if (text == null || plugin == null || plugin.getItemsAdderIntegration() == null) {
            return text;
        }

        if (!plugin.getItemsAdderIntegration().isEnabled()) {
            return text;
        }

        return text;
    }

    public static boolean containsFontFormats(String text) {
        if (text == null) {
            return false;
        }

        return text.contains(":offset_") ||
               text.contains(":font_") ||
               text.contains(":size_") ||
               text.matches(":[a-zA-Z_0-9-]+:");
    }
}
