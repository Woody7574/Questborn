package ua.woody.questborn.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.config.ActionBarMode;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;
import ua.woody.questborn.storage.PlayerDataStore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionBarManager {

    private final QuestbornPlugin plugin;
    private final PlayerDataStore playerData;

    private ActionBarMode mode;
    private String format;
    private String completeMessage;
    private int interval;
    private int taskId = -1;

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final Pattern HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");

    public ActionBarManager(QuestbornPlugin plugin, PlayerDataStore playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
    }

    public void loadConfig(ActionBarMode mode, String format, String completeMessage, int interval) {
        this.mode = mode;
        this.format = format;
        this.completeMessage = completeMessage;
        this.interval = interval;
    }

    public void startTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }

        if (mode != ActionBarMode.STATIC) {
            return;
        }

        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                sendForPlayer(p);
            }
        }, interval, interval).getTaskId();
    }

    public void stopTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void sendForPlayer(Player player) {
        var data = playerData.get(player.getUniqueId());
        String questId = data.getActiveQuestId();
        if (questId == null) return;

        QuestDefinition def = plugin.getQuestManager().getById(questId);
        if (def == null) return;

        int progress = data.getActiveQuestProgress();
        int target = getTargetAmount(def);

        String message = format
                .replace("{quest_name}", def.getDisplayName())
                .replace("{progress}", String.valueOf(progress))
                .replace("{target}", String.valueOf(target));

        Component component = LEGACY.deserialize(applyHexColors(message));
        player.sendActionBar(component);
    }

    public void sendCompleteMessage(Player player, QuestDefinition quest) {
        String message = completeMessage.replace("{quest_name}", quest.getDisplayName());
        Component component = LEGACY.deserialize(applyHexColors(message));
        player.sendActionBar(component);
    }

    private int getTargetAmount(QuestDefinition quest) {
        var o = quest.getObjective();

        // 🔥 усі distance-квести — в тому числі ENTITY_RIDE
        if (o.getType() == QuestObjectiveType.TRAVEL_DISTANCE ||
                (o.getType() == QuestObjectiveType.ENTITY_RIDE && o.getDistance() > 0)) {
            return (int) o.getDistance(); // target = distance
        }

        // інші — amount
        return o.getAmount();
    }


    private String applyHexColors(String input) {
        if (input == null) return null;

        Matcher m = HEX_PATTERN.matcher(input);
        StringBuffer out = new StringBuffer();

        while (m.find()) {
            String hex = m.group(1);
            StringBuilder rep = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                rep.append('§').append(c);
            }
            m.appendReplacement(out, rep.toString());
        }
        m.appendTail(out);

        return ChatColor.translateAlternateColorCodes('&', out.toString());
    }

    public ActionBarMode getMode() {
        return mode;
    }

    public void setMode(ActionBarMode mode) {
        this.mode = mode;
    }
}