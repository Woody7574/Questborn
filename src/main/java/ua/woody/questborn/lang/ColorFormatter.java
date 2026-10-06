package ua.woody.questborn.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorFormatter {
    private static final Map<String, String> COLOR_CACHE = new ConcurrentHashMap<>(512);
    private static final int MAX_CACHE_SIZE = 2048;

    public static void clearCache() {
        COLOR_CACHE.clear();
    }

    public static String applyColors(String input) {
        if (input == null) return null;
        if (input.isEmpty() || input.trim().isEmpty()) return " ";

        if (COLOR_CACHE.size() > MAX_CACHE_SIZE) {
            COLOR_CACHE.clear();
        }

        String result = COLOR_CACHE.computeIfAbsent(input, ColorFormatter::computeColors);
        return (result == null || result.isEmpty()) ? " " : result;
    }

    public static String format(String input) {
        if (input == null) return " ";
        String colored = applyColors(input);
        if (colored == null || colored.isEmpty() || colored.isBlank()) return " ";
        return "§r" + colored;
    }

    public static Component formatComponent(String input) {
        return Component.text(format(input));
    }

    private static String computeColors(String input) {
        String withSection = ChatColor.translateAlternateColorCodes('&', input);

        withSection = withSection.replaceAll("(?i)<(/?)(whisper|shout|normal)>", "\\\\<$1$2>");

        try {
            Component comp = MiniMessage.miniMessage().deserialize(withSection);

            return LegacyComponentSerializer.builder()
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build()
                    .serialize(comp);
        } catch (Throwable t) {
            Matcher m = Pattern.compile("<#([A-Fa-f0-9]{6})>").matcher(withSection);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String hex = m.group(1);
                StringBuilder replacement = new StringBuilder("§x");
                for (char c : hex.toCharArray()) {
                    replacement.append('§').append(c);
                }
                m.appendReplacement(sb, replacement.toString());
            }
            m.appendTail(sb);
            String result = sb.toString();

            result = result.replaceAll("</#([A-Fa-f0-9]{6})>", "");
            result = result.replaceAll("(?i)</?shout>|</?whisper>|</?normal>", "");
            return result;
        }
    }

    public static String stripColors(String input) {
        if (input == null) return null;
        String withSection = ChatColor.translateAlternateColorCodes('&', input);
        String stripped = ChatColor.stripColor(withSection);
        if (stripped == null) return "";
        return stripped.replaceAll("<[^>]*>", "");
    }

    public static String getLastColor(String input) {
        if (input == null || input.isEmpty())
            return "";

        String lastColor = "";

        Matcher m = Pattern.compile("(§[0-9a-fk-orX]|§x(§[0-9a-fA-F]){6})").matcher(input);
        while (m.find()) {
            lastColor = m.group();
        }

        return lastColor;
    }
}
