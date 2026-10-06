package ua.woody.questborn.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import ua.woody.questborn.QuestbornPlugin;

public class CommandListener implements Listener {
    private final QuestbornPlugin plugin;

    public CommandListener(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        String prefix = plugin.getName().toLowerCase() + ":";
        event.getCommands().removeIf(cmd -> cmd.toLowerCase().startsWith(prefix));
    }
}
