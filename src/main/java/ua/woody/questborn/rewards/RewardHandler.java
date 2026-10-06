package ua.woody.questborn.rewards;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ua.woody.questborn.lang.LanguageManager;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.rewards.modules.*;

import java.util.*;

public class RewardHandler {
    private static Economy econ;
    private static LanguageManager lang;
    private static JavaPlugin plugin;
    private static RewardRegistry registry;

    public static void init(LanguageManager languageManager, JavaPlugin pluginInstance) {
        lang = languageManager;
        plugin = pluginInstance;

        registry = ua.woody.questborn.api.QuestbornProvider.get().getRewardRegistry();

        registry.register(new MoneyRewardModule());
        registry.register(new ItemRewardModule());
        registry.register(new CommandRewardModule());
        registry.register(new XpRewardModule());
        registry.register(new AttributeRewardModule());
        registry.register(new PotionEffectsRewardModule());
        registry.register(new ChanceRewardModule());
        registry.register(new MessageRewardModule("message"));
        registry.register(new MessageRewardModule("messages"));
        registry.register(new BroadcastRewardModule("broadcast"));
        registry.register(new BroadcastRewardModule("broadcasts"));
    }

    static LanguageManager L() {
        return lang;
    }

    static JavaPlugin plugin() {
        return plugin;
    }

    public static Economy getEconomy() {
        if (!plugin.getConfig().getBoolean("integration.vault.enabled", true))
            return null;

        if (econ != null)
            return econ;

        if (Bukkit.getPluginManager().getPlugin("Vault") == null)
            return null;

        var rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null)
            return null;

        econ = rsp.getProvider();
        return econ;
    }

    public static boolean getSilent(Map<?, ?> map) {
        Object o = map.get("silent");
        if (o == null)
            return false;
        return Boolean.parseBoolean(String.valueOf(o));
    }

    public static void giveRewards(Player player, QuestDefinition quest) {
        if (quest == null || player == null)
            return;

        Map<String, Object> rewards = quest.getRewards();
        if (rewards == null || rewards.isEmpty())
            return;

        boolean globalSilent = false;
        Object globalSilentObj = rewards.get("silent");
        if (globalSilentObj != null) {
            globalSilent = Boolean.parseBoolean(String.valueOf(globalSilentObj));
        }

        RewardExecutionContext ctx = new RewardExecutionContext(plugin, lang, quest, globalSilent);

        List<Runnable> steps = new ArrayList<>();

        for (Map.Entry<String, Object> e : rewards.entrySet()) {
            String key = e.getKey();
            if ("silent".equalsIgnoreCase(key))
                continue;

            RewardModule module = registry.get(key);
            if (module == null) {
                continue;
            }

            Object config = e.getValue();

            steps.add(() -> {
                try {
                    module.execute(ctx, player, config);
                } catch (Throwable t) {
                    plugin.getLogger().warning("Error executing reward module '" +
                            module.getKey() + "' for quest " + quest.getId() + ": " + t.getMessage());
                    t.printStackTrace();
                }
            });
        }

        if (steps.size() <= 3) {
            for (Runnable r : steps) {
                r.run();
            }
        } else {
            ctx.runPipeline(player, steps, 1L);
        }
    }

    public static void giveCustomRewards(Player player, QuestDefinition quest, Map<String, Object> customRewards) {
        giveCustomRewards(player, quest, customRewards, 1);
    }

    public static void giveCustomRewards(Player player, QuestDefinition quest, Map<String, Object> customRewards, int multiplier) {
        if (customRewards == null || customRewards.isEmpty() || player == null) return;

        boolean globalSilent = false;
        Object globalSilentObj = customRewards.get("silent");
        if (globalSilentObj != null) {
            globalSilent = Boolean.parseBoolean(String.valueOf(globalSilentObj));
        }

        RewardExecutionContext ctx = new RewardExecutionContext(plugin, lang, quest, globalSilent, multiplier);
        List<Runnable> steps = new ArrayList<>();

        for (Map.Entry<String, Object> e : customRewards.entrySet()) {
            String key = e.getKey();
            if ("silent".equalsIgnoreCase(key)) continue;

            RewardModule module = registry.get(key);
            if (module == null) continue;

            Object config = e.getValue();
            steps.add(() -> {
                try {
                    module.execute(ctx, player, config);
                } catch (Throwable t) {
                    plugin.getLogger().warning("Error executing custom reward module '" + module.getKey() + "': " + t.getMessage());
                }
            });
        }

        if (steps.size() <= 3) {
            for (Runnable r : steps) {
                r.run();
            }
        } else {
            ctx.runPipeline(player, steps, 1L);
        }
    }
}
