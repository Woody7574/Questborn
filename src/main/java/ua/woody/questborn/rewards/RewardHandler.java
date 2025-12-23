package ua.woody.questborn.rewards;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.lang.LanguageManager;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.rewards.modules.*;

import java.util.*;

/**
 * Новий RewardHandler:
 * - тримає реєстр модулів
 * - запускає пайплайн нагород (черга)
 * - кешує Economy
 */
public class RewardHandler {

    private static Economy econ;
    private static LanguageManager lang;
    private static JavaPlugin plugin;
    private static RewardRegistry registry;

    public static void init(LanguageManager languageManager, JavaPlugin pluginInstance) {
        lang = languageManager;
        plugin = pluginInstance;

        // 1) ініціалізація реєстру
        registry = new RewardRegistry();

        // 2) реєстрація модулів
        registry.register(new MoneyRewardModule());
        registry.register(new ItemRewardModule());
        registry.register(new CommandRewardModule());
        registry.register(new XpRewardModule());
        registry.register(new AttributeRewardModule());
        registry.register(new PotionEffectsRewardModule());

        // За бажанням: here you can later add custom / зовнішні модулі
        // registry.register(new MyCustomRewardModule(...));
    }

    static LanguageManager L() {
        return lang;
    }

    static JavaPlugin plugin() {
        return plugin;
    }

    // =======================================
    // ECONOMY (зберігаємо для MoneyRewardModule)
    // =======================================
    public static Economy getEconomy() {
        if (econ != null) return econ;

        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return null;

        var rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return null;

        econ = rsp.getProvider();
        return econ;
    }

    // Універсальний silent-reader для Map
    public static boolean getSilent(Map<?, ?> map) {
        Object o = map.get("silent");
        if (o == null) return false;
        return Boolean.parseBoolean(String.valueOf(o));
    }

    // =======================================
    // MAIN ENTRY POINT
    // =======================================
    public static void giveRewards(Player player, QuestDefinition quest) {
        if (quest == null || player == null) return;

        Map<String, Object> rewards = quest.getRewards();
        if (rewards == null || rewards.isEmpty()) return;

        // Можемо додати глобальний silent через rewards.silent, якщо захочеш
        boolean globalSilent = false;
        Object globalSilentObj = rewards.get("silent");
        if (globalSilentObj != null) {
            globalSilent = Boolean.parseBoolean(String.valueOf(globalSilentObj));
        }

        RewardExecutionContext ctx =
                new RewardExecutionContext(plugin, lang, quest, globalSilent);

        // Формуємо пайплайн кроків
        List<Runnable> steps = new ArrayList<>();

        for (Map.Entry<String, Object> e : rewards.entrySet()) {
            String key = e.getKey();
            if ("silent".equalsIgnoreCase(key)) continue; // службове

            RewardModule module = registry.get(key);
            if (module == null) {
                // Поки що просто ігноруємо невідомі ключі
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

        // Якщо кроків мало – можна виконати одразу
        if (steps.size() <= 3) {
            for (Runnable r : steps) {
                r.run();
            }
        } else {
            // Інакше — розтягуємо по тікам (асинхронний пайплайн)
            ctx.runPipeline(steps, 1L);
        }
    }
}
