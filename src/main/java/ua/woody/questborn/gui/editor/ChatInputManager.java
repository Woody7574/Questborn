package ua.woody.questborn.gui.editor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.EditorChat;
import ua.woody.questborn.gui.editor.EditorSessionManager;
import ua.woody.questborn.lang.ColorFormatter;

public class ChatInputManager
implements Listener {
    private final QuestbornPlugin plugin;
    private final Map<UUID, InputSession> pendingInputs = new ConcurrentHashMap<UUID, InputSession>();

    public ChatInputManager(QuestbornPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)plugin);
    }

    public void requestInput(Player player, String promptMessage, Consumer<String> callback) {
        this.requestInputWithSuggestion(player, promptMessage, null, callback, false);
    }

    public void requestInputWithSuggestion(Player player, String promptMessage, String suggestion, Consumer<String> callback) {
        this.requestInputWithSuggestion(player, promptMessage, suggestion, callback, false);
    }

    public void requestInputAllowingCommands(Player player, String promptMessage, Consumer<String> callback) {
        this.requestInputWithSuggestion(player, promptMessage, null, callback, true);
    }

    public void requestInputWithSuggestionAllowingCommands(Player player, String promptMessage, String suggestion, Consumer<String> callback) {
        this.requestInputWithSuggestion(player, promptMessage, suggestion, callback, true);
    }

    private void requestInputWithSuggestion(Player player, String promptMessage, String suggestion, Consumer<String> callback, boolean allowCommands) {
        if (promptMessage != null && !promptMessage.isEmpty()) {
            player.sendMessage(ColorFormatter.applyColors(""));
            String rawPrompt = ColorFormatter.applyColors(promptMessage);
            String rawPrefix = ColorFormatter.applyColors(EditorChat.getPrefix());
            String strippedPrompt = ChatColor.stripColor((String)rawPrompt);
            String strippedPrefix = ChatColor.stripColor((String)rawPrefix).trim();
            if (strippedPrefix.isEmpty()) {
                strippedPrefix = "Editor";
            }
            if (strippedPrompt.contains(strippedPrefix)) {
                player.sendMessage(rawPrompt);
            } else {
                player.sendMessage(ColorFormatter.applyColors(EditorChat.formatPrompt(promptMessage)));
            }
        }
        BaseComponent[] baseCancel = TextComponent.fromLegacyText((String)ColorFormatter.applyColors(this.plugin.getLanguage().trEditor("common_editor.input.cancel_instruction")));
        TextComponent message = new TextComponent("");
        for (BaseComponent component : baseCancel) {
            message.addExtra(component);
        }
        player.spigot().sendMessage((BaseComponent)message);
        if (suggestion != null && !suggestion.isEmpty()) {
            BaseComponent[] clickable;
            for (BaseComponent component : clickable = TextComponent.fromLegacyText((String)ColorFormatter.applyColors(this.plugin.getLanguage().trEditor("common_editor.input.edit_current")))) {
                component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestion));
            }
            player.spigot().sendMessage(clickable);
        }
        player.sendMessage(ColorFormatter.applyColors(""));
        this.plugin.getFoliaLib().getImpl().runAtEntity((Entity)player, task -> player.closeInventory());
        this.pendingInputs.put(player.getUniqueId(), new InputSession(callback, allowCommands));
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        InputSession session = this.pendingInputs.remove(player.getUniqueId());
        if (session != null) {
            event.setCancelled(true);
            String message = event.getMessage();
            this.plugin.getFoliaLib().getImpl().runAtEntity((Entity)player, task -> {
                if (message.equalsIgnoreCase("cancel")) {
                    EditorChat.sendWarning(player, this.plugin.getLanguage().trEditor("common_editor.input.cancelled"));
                    session.callback.accept(null);
                } else {
                    session.callback.accept(message);
                }
            });
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        InputSession session = this.pendingInputs.remove(player.getUniqueId());
        if (session != null) {
            event.setCancelled(true);
            String message = event.getMessage();
            this.plugin.getFoliaLib().getImpl().runAtEntity((Entity)player, task -> {
                if (message.equalsIgnoreCase("/cancel")) {
                    EditorChat.sendWarning(player, this.plugin.getLanguage().trEditor("common_editor.input.cancelled"));
                    session.callback.accept(null);
                } else if (session.allowCommands) {
                    session.callback.accept(message);
                } else {
                    EditorChat.sendWarning(player, this.plugin.getLanguage().trEditor("common_editor.input.cancelled_cmd"));
                    session.callback.accept(null);
                }
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.pendingInputs.remove(event.getPlayer().getUniqueId());
        EditorSessionManager.clearSession(event.getPlayer().getUniqueId());
    }

    private static class InputSession {
        final Consumer<String> callback;
        final boolean allowCommands;

        InputSession(Consumer<String> callback, boolean allowCommands) {
            this.callback = callback;
            this.allowCommands = allowCommands;
        }
    }
}
