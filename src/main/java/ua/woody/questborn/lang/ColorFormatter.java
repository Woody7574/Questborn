package ua.woody.questborn.lang;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorFormatter {

    private static final Pattern HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");

    /**
     * Застосовує HEX кольори та & коди до рядка
     */
    public static String applyColors(String input) {
        if (input == null) return null;

        // Конвертуємо HEX кольори (<#ffaa00>)
        Matcher m = HEX_PATTERN.matcher(input);
        StringBuffer out = new StringBuffer();

        while (m.find()) {
            String hex = m.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            m.appendReplacement(out, replacement.toString());
        }

        m.appendTail(out);

        // Конвертуємо & коди
        return ChatColor.translateAlternateColorCodes('&', out.toString());
    }

    /**
     * Видаляє всі кольорові коди з рядка
     */
    public static String stripColors(String input) {
        if (input == null) return "";

        // Видаляємо HEX кольори
        String result = input.replaceAll("<#([A-Fa-f0-9]{6})>", "");

        // Видаляємо § коди
        result = result.replaceAll("§[0-9A-FK-ORXa-fk-orx]", "");

        // Видаляємо & коди
        result = result.replaceAll("&[0-9A-FK-ORXa-fk-orx]", "");

        return result;
    }
}