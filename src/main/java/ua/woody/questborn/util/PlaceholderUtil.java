package ua.woody.questborn.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderUtil {
    public static String format(QuestbornPlugin plugin, Player player, String text) {
        if (text == null) return null;

        if (player != null) {
            text = text.replace("%player_name%", player.getName());
        }

        if (plugin.getConfig().getBoolean("integration.placeholderapi.enabled", true)
                && plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                return PlaceholderAPI.setPlaceholders(player, text);
            } catch (Exception e) {
                return text;
            }
        }

        return text;
    }

    public static List<String> format(QuestbornPlugin plugin, Player player, List<String> textList) {
        if (textList == null) return null;
        List<String> formatted = new ArrayList<>();
        for (String text : textList) {
            formatted.add(format(plugin, player, text));
        }
        return formatted;
    }
}
