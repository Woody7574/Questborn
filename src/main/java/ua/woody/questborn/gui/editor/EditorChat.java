package ua.woody.questborn.gui.editor;

import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.lang.ColorFormatter;

public class EditorChat {
    public static final String TEXT_MUTED = "<#b0b8c1>";
    public static final String TEXT_VALUE = "<#e2e8f0>";
    public static final String TEXT_ERROR = "<#c4b5b5>";
    public static final String TEXT_WARN = "<#c8beaa>";

    public static String getPrefix() {
        return QuestbornPlugin.getInstance().getLanguage().trEditor("common_editor.prefix");
    }

    public static String getSuccessPrefix() {
        return EditorChat.getPrefix() + "<#78b185>\u2714 ";
    }

    public static String getErrorPrefix() {
        return EditorChat.getPrefix() + "<#c96a6a>\u2716 ";
    }

    public static String getWarningPrefix() {
        return EditorChat.getPrefix() + "<#d9a752>\u26a0 ";
    }

    public static String getPromptPrefix() {
        return EditorChat.getPrefix() + "<#6aa0c9>\u270f ";
    }

    private static boolean hasPrefix(String message) {
        if (message == null) return false;
        String plainMessage = ColorFormatter.stripColors(message);
        String plainPrefix = ColorFormatter.stripColors(getPrefix());
        if (plainPrefix == null || plainPrefix.isEmpty()) return false;
        return plainMessage.contains(plainPrefix.trim());
    }

    public static void sendSuccess(Player player, String message) {
        if (hasPrefix(message)) {
            player.sendMessage(ColorFormatter.applyColors(message));
            return;
        }
        player.sendMessage(ColorFormatter.applyColors(EditorChat.getSuccessPrefix() + TEXT_MUTED + message));
    }

    public static void sendError(Player player, String message) {
        if (hasPrefix(message)) {
            player.sendMessage(ColorFormatter.applyColors(message));
            return;
        }
        player.sendMessage(ColorFormatter.applyColors(EditorChat.getErrorPrefix() + TEXT_ERROR + message));
    }

    public static void sendWarning(Player player, String message) {
        if (hasPrefix(message)) {
            player.sendMessage(ColorFormatter.applyColors(message));
            return;
        }
        player.sendMessage(ColorFormatter.applyColors(EditorChat.getWarningPrefix() + TEXT_WARN + message));
    }

    public static String formatPrompt(String promptText) {
        return ColorFormatter.applyColors(EditorChat.getPromptPrefix() + TEXT_MUTED + promptText);
    }

    public static String formatPromptWithSuggestion(String promptText, String targetKey) {
        return ColorFormatter.applyColors(EditorChat.getPromptPrefix() + TEXT_MUTED + promptText + " <#e2e8f0>" + targetKey);
    }
}
