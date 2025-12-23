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

    public RewardExecutionContext(JavaPlugin plugin,
                                  LanguageManager lang,
                                  QuestDefinition quest,
                                  boolean globalSilent) {
        this.plugin = (QuestbornPlugin) plugin;
        this.lang = lang;
        this.quest = quest;
        this.globalSilent = globalSilent;
    }

    public QuestbornPlugin getPlugin() {
        return plugin;
    }

    public LanguageManager getLang() {
        return lang;
    }

    public QuestDefinition getQuest() {
        return quest;
    }

    public boolean isGlobalSilent() {
        return globalSilent;
    }

    // Універсальний sender з плейсхолдерами
    public void send(Player player, String key, Map<String, String> placeholders) {
        if (globalSilent) return;
        player.sendMessage(lang.tr(key, placeholders));
    }

    public void send(Player player, String key) {
        send(player, key, Map.of());
    }

    // Планування (для складних майбутніх reward-пайплайнів)
    public void runSync(Runnable task) {
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    public void runLater(long ticks, Runnable task) {
        plugin.getServer().getScheduler().runTaskLater(plugin, task, ticks);
    }

    public void runPipeline(java.util.List<Runnable> steps, long intervalTicks) {
        if (steps.isEmpty()) return;

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (index >= steps.size()) {
                    cancel();
                    return;
                }
                try {
                    steps.get(index++).run();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }.runTaskTimer(plugin, 0L, Math.max(1L, intervalTicks));
    }
}
