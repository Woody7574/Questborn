package ua.woody.questborn.rewards;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.lang.LanguageManager;
import ua.woody.questborn.model.QuestDefinition;

import java.util.Map;

public class RewardExecutionContext {
    private final QuestbornPlugin plugin;
    private final LanguageManager lang;
    private final QuestDefinition quest;
    private final boolean globalSilent;
    private final int multiplier;

    public RewardExecutionContext(JavaPlugin plugin,
            LanguageManager lang,
            QuestDefinition quest,
            boolean globalSilent) {
        this(plugin, lang, quest, globalSilent, 1);
    }

    public RewardExecutionContext(JavaPlugin plugin,
            LanguageManager lang,
            QuestDefinition quest,
            boolean globalSilent,
            int multiplier) {
        this.plugin = (QuestbornPlugin) plugin;
        this.lang = lang;
        this.quest = quest;
        this.globalSilent = globalSilent;
        this.multiplier = multiplier;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public QuestbornPlugin getPlugin() {
        return plugin;
    }

    public LanguageManager getLang() {
        return lang;
    }

    public LanguageManager lang() {
        return lang;
    }

    public QuestDefinition getQuest() {
        return quest;
    }

    public boolean isSilent() {
        return globalSilent;
    }

    public boolean isGlobalSilent() {
        return globalSilent;
    }

    public void send(Player player, String key, Map<String, String> placeholders) {
        if (globalSilent)
            return;
        player.sendMessage(lang.tr(key, placeholders));
    }

    public void send(Player player, String key) {
        send(player, key, Map.of());
    }

    public void runSync(Player player, Runnable task) {
        plugin.getFoliaLib().getImpl().runAtEntity(player, __task -> task.run());
    }

    public void runLater(Player player, long ticks, Runnable task) {
        plugin.getFoliaLib().getImpl().runAtEntityLater(player, task, ticks);
    }

    public void runPipeline(Player player, java.util.List<Runnable> steps, long intervalTicks) {
        if (steps.isEmpty())
            return;

        final int[] index = {0};
        plugin.getFoliaLib().getImpl().runAtEntityTimer(player, __task -> {
            if (index[0] >= steps.size()) {
                __task.cancel();
                return;
            }
            try {
                steps.get(index[0]++).run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, 1L, Math.max(1L, intervalTicks));
    }
}
