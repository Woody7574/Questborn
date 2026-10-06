package ua.woody.questborn.integration.npc;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestDefinition;

import java.util.List;

public class DialogueConditionParser {
    public static boolean checkConditions(QuestbornPlugin plugin, Player player, String npcId, List<String> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        for (String cond : conditions) {
            if (!checkCondition(plugin, player, npcId, cond)) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkCondition(QuestbornPlugin plugin, Player player, String npcId, String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }

        condition = condition.trim();
        boolean inverse = false;
        if (condition.startsWith("!")) {
            inverse = true;
            condition = condition.substring(1).trim();
        }

        boolean result = evaluateConditionLogic(plugin, player, npcId, condition);
        return inverse ? !result : result;
    }

    private static boolean evaluateConditionLogic(QuestbornPlugin plugin, Player player, String npcId, String condition) {
        if (condition.equalsIgnoreCase("has_reward")) {
            return ua.woody.questborn.gui.DialogueGui.hasRewardFromNpc(plugin, player, npcId);
        }

        if (condition.equalsIgnoreCase("has_give_item")) {
            return ua.woody.questborn.gui.DialogueGui.getGiveItemQuestStatic(plugin, player, npcId) != null;
        }

        if (condition.toLowerCase().startsWith("has_permission:")) {
            String perm = condition.substring("has_permission:".length()).trim();
            return player.hasPermission(perm);
        }

        if (condition.toLowerCase().startsWith("quest_completed:")) {
            String qId = condition.substring("quest_completed:".length()).trim();
            PlayerQuestProgress progress = plugin.getPlayerDataStore().get(player.getUniqueId());
            return progress != null && progress.isQuestCompleted(qId);
        }

        if (condition.toLowerCase().startsWith("quest_active:")) {
            String qId = condition.substring("quest_active:".length()).trim();
            PlayerQuestProgress progress = plugin.getPlayerDataStore().get(player.getUniqueId());
            return progress != null && progress.hasActiveQuest(qId);
        }

        if (condition.equalsIgnoreCase("has_available_quests")) {
            return hasAvailableQuests(plugin, player, npcId);
        }

        if (condition.equalsIgnoreCase("has_quests_on_cooldown")) {
            return hasQuestsOnCooldown(plugin, player, npcId);
        }

        if (condition.equalsIgnoreCase("has_quests_assigned")) {
            return hasQuestsAssigned(plugin, npcId);
        }

        if (condition.equalsIgnoreCase("all_quests_completed")) {
            return allQuestsCompleted(plugin, player, npcId);
        }

        if (condition.toLowerCase().startsWith("npc_mood:")) {
            String moodName = condition.substring("npc_mood:".length()).trim();
            if (plugin.getMoodManager() != null) {
                ua.woody.questborn.model.NpcConfig npcConfig = plugin.getNpcManager().getConfigByNpcId(npcId);
                if (npcConfig != null) {
                    String currentMood = plugin.getMoodManager().calculateMood(player, npcConfig);
                    return moodName.equalsIgnoreCase(currentMood);
                }
            }
            return false;
        }

        if (condition.toLowerCase().startsWith("has_item:")) {
            String itemStr = condition.substring("has_item:".length()).trim();
            String[] parts = itemStr.split(":");
            if (parts.length >= 1) {
                Material mat = Material.matchMaterial(parts[0].trim());
                if (mat != null) {
                    int requiredAmount = 1;
                    if (parts.length >= 2) {
                        try {
                            requiredAmount = Integer.parseInt(parts[1].trim());
                        } catch (NumberFormatException ignored) {}
                    }
                    return hasItemAmount(player, mat, requiredAmount);
                }
            }
            return false;
        }

        if (condition.toLowerCase().startsWith("has_money:")) {
            String amountStr = condition.substring("has_money:".length()).trim();
            try {
                double amount = Double.parseDouble(amountStr);
                if (plugin.getEconomy() != null) {
                    return plugin.getEconomy().has(player, amount);
                }
            } catch (NumberFormatException ignored) {}
            return false;
        }

        plugin.getLogger().warning("Unknown dialogue condition '" + condition + "' for NPC '" + npcId + "'");
        return true;
    }

    private static boolean hasItemAmount(Player player, Material material, int requiredAmount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count >= requiredAmount;
    }

    private static boolean hasQuestsAssigned(QuestbornPlugin plugin, String npcId) {
        if (npcId == null || npcId.equals("none")) return false;

        var npcConfig = plugin.getNpcManager().getConfigByNpcId(npcId);
        if (npcConfig != null) {
            if (!npcConfig.getStartsQuests().isEmpty() || !npcConfig.getStartsTypes().isEmpty()
                    || !npcConfig.getFinishesQuests().isEmpty() || !npcConfig.getFinishesTypes().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean allQuestsCompleted(QuestbornPlugin plugin, Player player, String npcId) {
        if (!hasQuestsAssigned(plugin, npcId)) return false;
        return !hasAvailableQuests(plugin, player, npcId) && !hasQuestsOnCooldown(plugin, player, npcId);
    }

    private static void getMatchedQuestsAndTypes(QuestbornPlugin plugin, String npcId, List<ua.woody.questborn.model.QuestTypeConfig> matchedTypes, List<QuestDefinition> matchedQuests) {
        var npcConfig = plugin.getNpcManager().getConfigByNpcId(npcId);
        if (npcConfig != null) {
            for (String tId : npcConfig.getStartsTypes()) {
                var t = plugin.getQuestManager().getQuestTypeManager().getType(tId);
                if (t != null && !matchedTypes.contains(t)) matchedTypes.add(t);
            }
            for (String tId : npcConfig.getFinishesTypes()) {
                var t = plugin.getQuestManager().getQuestTypeManager().getType(tId);
                if (t != null && !matchedTypes.contains(t)) matchedTypes.add(t);
            }
            for (String qId : npcConfig.getStartsQuests()) {
                var q = plugin.getQuestManager().getQuest(qId);
                if (q != null && !matchedQuests.contains(q)) matchedQuests.add(q);
            }
            for (String qId : npcConfig.getFinishesQuests()) {
                var q = plugin.getQuestManager().getQuest(qId);
                if (q != null && !matchedQuests.contains(q)) matchedQuests.add(q);
            }
        }

        if (!matchedTypes.isEmpty()) {
            matchedQuests.removeIf(q -> {
                for (ua.woody.questborn.model.QuestTypeConfig t : matchedTypes) {
                    if (t.getId().equalsIgnoreCase(q.getTypeId())) return true;
                }
                return false;
            });
        }
    }

    private static boolean hasAvailableQuests(QuestbornPlugin plugin, Player player, String npcId) {
        if (npcId == null || npcId.equals("none")) return false;
        var data = plugin.getPlayerDataStore().get(player.getUniqueId());
        if (data == null) return false;
        long now = System.currentTimeMillis();

        List<ua.woody.questborn.model.QuestTypeConfig> matchedTypes = new java.util.ArrayList<>();
        List<QuestDefinition> matchedQuests = new java.util.ArrayList<>();
        getMatchedQuestsAndTypes(plugin, npcId, matchedTypes, matchedQuests);

        for (ua.woody.questborn.model.QuestTypeConfig type : matchedTypes) {
            java.util.Collection<QuestDefinition> quests = plugin.getQuestManager().getByType(type);
            for (QuestDefinition q : quests) {
                if (data.hasActiveQuest(q.getId()) || isQuestStartable(plugin, player, q, type, data, now)) {
                    return true;
                }
            }
        }

        for (QuestDefinition q : matchedQuests) {
            ua.woody.questborn.model.QuestTypeConfig type = plugin.getQuestManager().getQuestTypeManager().getType(q.getTypeId());
            if (data.hasActiveQuest(q.getId()) || isQuestStartable(plugin, player, q, type, data, now)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasQuestsOnCooldown(QuestbornPlugin plugin, Player player, String npcId) {
        if (npcId == null || npcId.equals("none")) return false;
        var data = plugin.getPlayerDataStore().get(player.getUniqueId());
        if (data == null) return false;
        long now = System.currentTimeMillis();

        List<ua.woody.questborn.model.QuestTypeConfig> matchedTypes = new java.util.ArrayList<>();
        List<QuestDefinition> matchedQuests = new java.util.ArrayList<>();
        getMatchedQuestsAndTypes(plugin, npcId, matchedTypes, matchedQuests);

        for (ua.woody.questborn.model.QuestTypeConfig type : matchedTypes) {
            java.util.Collection<QuestDefinition> quests = plugin.getQuestManager().getByType(type);
            for (QuestDefinition q : quests) {
                if (isQuestOnCooldown(plugin, player, q, type, data, now)) {
                    return true;
                }
            }
        }

        for (QuestDefinition q : matchedQuests) {
            ua.woody.questborn.model.QuestTypeConfig type = plugin.getQuestManager().getQuestTypeManager().getType(q.getTypeId());
            if (isQuestOnCooldown(plugin, player, q, type, data, now)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isQuestStartable(QuestbornPlugin plugin, Player player, QuestDefinition q, ua.woody.questborn.model.QuestTypeConfig typeConfig, PlayerQuestProgress data, long now) {
        if (!plugin.getQuestManager().isQuestAvailable(player, q, data)) return false;
        String qid = q.getId();
        if (data.hasActiveQuest(qid) || data.hasPendingReward(qid)) return false;

        boolean isCompletedOnce = data.isQuestCompleted(qid);
        long cdUntil;
        if (typeConfig != null && typeConfig.getEngine() == ua.woody.questborn.model.EngineType.ROTATION && isCompletedOnce) {
            cdUntil = plugin.getQuestManager().getNextRotationResetTimestamp(typeConfig, data.getRotationAssignedAt(typeConfig.getId()));
        } else {
            cdUntil = data.getQuestCooldownUntil(qid);
        }
        if (cdUntil > now) return false;

        boolean isOneTime = (typeConfig != null ? typeConfig.getCooldownSeconds() : 0L) == 0 && (typeConfig == null || typeConfig.getEngine() != ua.woody.questborn.model.EngineType.ROTATION);
        if (isOneTime && isCompletedOnce) return false;

        return true;
    }

    private static boolean isQuestOnCooldown(QuestbornPlugin plugin, Player player, QuestDefinition q, ua.woody.questborn.model.QuestTypeConfig typeConfig, PlayerQuestProgress data, long now) {
        String qid = q.getId();
        if (data.hasActiveQuest(qid) || data.hasPendingReward(qid)) return false;

        boolean isCompletedOnce = data.isQuestCompleted(qid);
        long cdUntil;
        if (typeConfig != null && typeConfig.getEngine() == ua.woody.questborn.model.EngineType.ROTATION && isCompletedOnce) {
            cdUntil = plugin.getQuestManager().getNextRotationResetTimestamp(typeConfig, data.getRotationAssignedAt(typeConfig.getId()));
        } else {
            cdUntil = data.getQuestCooldownUntil(qid);
        }

        return cdUntil > now;
    }
}
