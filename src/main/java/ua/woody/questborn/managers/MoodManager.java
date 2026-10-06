package ua.woody.questborn.managers;

import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.NpcConfig;
import ua.woody.questborn.model.npc.mood.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MoodManager {
    private final QuestbornPlugin plugin;

    public MoodManager(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    public String calculateMood(Player player, NpcConfig npc) {
        if (npc == null) return "NEUTRAL";

        List<MoodRule> rules = npc.getMoodRules();
        if (rules == null || rules.isEmpty()) {
            return npc.getDefaultMood();
        }

        for (MoodRule rule : rules) {
            if (rule.isApplicable(player, npc)) {
                return rule.getMood();
            }
        }

        return npc.getDefaultMood();
    }

    public MoodRule parseRule(Map<?, ?> ruleMap) {
        String mood = (String) ruleMap.get("mood");
        if (mood == null) return null;

        int priority = ruleMap.containsKey("priority") ? ((Number) ruleMap.get("priority")).intValue() : 0;

        List<MoodCondition> conditionsList = new ArrayList<>();
        if (ruleMap.containsKey("conditions")) {
            Object rawConds = ruleMap.get("conditions");
            if (rawConds instanceof List) {
                List<Map<?, ?>> conditionsData = (List<Map<?, ?>>) rawConds;
                for (Map<?, ?> condData : conditionsData) {
                    MoodCondition cond = parseCondition(condData);
                    if (cond != null) conditionsList.add(cond);
                }
            } else if (rawConds instanceof org.bukkit.configuration.ConfigurationSection) {
                org.bukkit.configuration.ConfigurationSection condsSec = (org.bukkit.configuration.ConfigurationSection) rawConds;
                for (String key : condsSec.getKeys(false)) {
                    org.bukkit.configuration.ConfigurationSection condSec = condsSec.getConfigurationSection(key);
                    if (condSec != null) {
                        String type = key.matches(".*_\\d+$") ? key.substring(0, key.lastIndexOf('_')) : key;
                        type = type.toUpperCase();
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        map.put("type", type);
                        for (String k : condSec.getKeys(false)) map.put(k, condSec.get(k));
                        MoodCondition c = parseCondition(map);
                        if (c != null) conditionsList.add(c);
                    }
                }
            }
        }

        return new MoodRule(mood, priority, conditionsList);
    }

    public MoodRule parseRuleFromSection(String mood, org.bukkit.configuration.ConfigurationSection sec) {
        int priority = sec.getInt("priority", 0);
        List<MoodCondition> conditionsList = new ArrayList<>();

        if (sec.contains("conditions")) {
            if (sec.isList("conditions")) {
                List<Map<?, ?>> conditionsData = sec.getMapList("conditions");
                for (Map<?, ?> condData : conditionsData) {
                    MoodCondition cond = parseCondition(condData);
                    if (cond != null) conditionsList.add(cond);
                }
            } else if (sec.isConfigurationSection("conditions")) {
                org.bukkit.configuration.ConfigurationSection condsSec = sec.getConfigurationSection("conditions");
                for (String key : condsSec.getKeys(false)) {
                    org.bukkit.configuration.ConfigurationSection condSec = condsSec.getConfigurationSection(key);
                    if (condSec != null) {
                        String type = key.matches(".*_\\d+$") ? key.substring(0, key.lastIndexOf('_')) : key;
                        type = type.toUpperCase();
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        map.put("type", type);
                        for (String k : condSec.getKeys(false)) map.put(k, condSec.get(k));
                        MoodCondition c = parseCondition(map);
                        if (c != null) conditionsList.add(c);
                    }
                }
            }
        }

        return new MoodRule(mood, priority, conditionsList);
    }

    public MoodCondition parseCondition(Map<?, ?> condData) {
        String type = (String) condData.get("type");
        if (type == null) return null;

        try {
            switch (type.toUpperCase()) {
                case "TIME":
                    if (condData.containsKey("range")) {
                        return new TimeCondition((String) condData.get("range"));
                    }
                    break;
                case "WEATHER":
                    if (condData.containsKey("state")) {
                        return new WeatherCondition((String) condData.get("state"));
                    }
                    break;
                case "MONEY":
                    if (condData.containsKey("amount")) {
                        return new MoneyCondition(condData.get("amount").toString());
                    }
                    break;
                case "ITEM":
                    if (condData.containsKey("material") && condData.containsKey("amount")) {
                        return new ItemCondition((String) condData.get("material"), condData.get("amount").toString());
                    }
                    break;
                case "QUEST_COMPLETED":
                    if (condData.containsKey("quest_id")) {
                        return new QuestCompletedCondition((String) condData.get("quest_id"));
                    }
                    break;
                case "QUEST_ACTIVE":
                    if (condData.containsKey("quest_id")) {
                        return new QuestActiveCondition((String) condData.get("quest_id"));
                    }
                    break;
                case "PLACEHOLDER":
                    if (condData.containsKey("placeholder")) {
                        String expression = condData.containsKey("expression") ? condData.get("expression").toString() : "";
                        return new PlaceholderCondition((String) condData.get("placeholder"), expression);
                    }
                    break;
                case "CHANCE":
                    if (condData.containsKey("percentage")) {
                        double chance = 50.0;
                        try {
                            chance = Double.parseDouble(condData.get("percentage").toString());
                        } catch (NumberFormatException ignored) {}
                        return new ChanceCondition(chance);
                    }
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse mood condition: " + condData + " - " + e.getMessage());
        }
        return null;
    }
}
