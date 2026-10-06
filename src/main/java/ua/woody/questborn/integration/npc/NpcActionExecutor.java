package ua.woody.questborn.integration.npc;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.util.PlaceholderUtil;

public class NpcActionExecutor {
    public static void executeAction(QuestbornPlugin plugin, Player player, String npcId, String actionStr) {
        executeAction(plugin, player, npcId, actionStr, null, null);
    }

    public static void executeAction(QuestbornPlugin plugin, Player player, String npcId, String actionStr, String dialogueId, String nodeId) {
        if (actionStr == null || actionStr.isEmpty()) return;

        String[] actions = actionStr.split(";");
        for (String action : actions) {
            action = action.trim();
            if (action.isEmpty()) continue;

            action = PlaceholderUtil.format(plugin, player, action);

            if (action.equals("close")) {
                plugin.getFoliaLib().getImpl().runAtEntity(player, __ -> player.closeInventory());
            } else if (action.equals("open_quest_menu")) {
                plugin.getFoliaLib().getImpl().runAtEntity(player, __ -> {
                    plugin.getNpcIntegrationManager().handleNpcClick(player, npcId, null, true, true, 0, dialogueId, nodeId);
                });
            } else if (action.equals("claim_reward")) {
                boolean claimed = ua.woody.questborn.gui.DialogueGui.claimRewardFromNpc(plugin, player, npcId);
                if (claimed) {
                    spawnParticles(plugin, player, Particle.VILLAGER_HAPPY, 15);
                }
                plugin.getFoliaLib().getImpl().runNextTick(__ -> player.closeInventory());
            } else if (action.equals("give_items")) {
                ua.woody.questborn.model.QuestDefinition giveItemQuest = ua.woody.questborn.gui.DialogueGui.getGiveItemQuestStatic(plugin, player, npcId);
                if (giveItemQuest != null) {
                    plugin.getFoliaLib().getImpl().runNextTick(__ -> {
                        new ua.woody.questborn.gui.QuestTransferGui(plugin, player, giveItemQuest, false, null, npcId, dialogueId, nodeId).open();
                    });
                }
            } else if (action.startsWith("open_quest:")) {
                String qId = action.substring("open_quest:".length()).trim();
                ua.woody.questborn.model.QuestDefinition q = plugin.getQuestManager().getQuest(qId);
                if (q != null) {
                    plugin.getFoliaLib().getImpl().runNextTick(__ -> {
                        org.bukkit.entity.EntityType npcType = plugin.getNpcIntegrationManager().getProvider().getType(npcId);
                        String npcName = plugin.getNpcIntegrationManager().getProvider().getName(npcId);
                        org.bukkit.inventory.ItemStack npcIcon = plugin.getNpcIntegrationManager().createNpcIcon(npcType, npcName, npcName);
                        new ua.woody.questborn.gui.QuestDetailsGui(plugin, player, q, false, npcIcon, npcId, dialogueId, nodeId).open();
                    });
                }
            } else if (action.startsWith("console:")) {
                String cmd = action.substring("console:".length()).trim();
                plugin.getFoliaLib().getImpl().runLater(() -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }, 2L);
            } else if (action.startsWith("player:")) {
                String rawCmd = action.substring("player:".length()).trim();
                final String cmd = rawCmd.startsWith("/") ? rawCmd.substring(1) : rawCmd;
                plugin.getFoliaLib().getImpl().runAtEntityLater(player, () -> {
                    player.performCommand(cmd);
                }, 2L);
            } else if (action.startsWith("message:")) {
                String msg = action.substring("message:".length()).trim();
                if (msg.contains("{npc}")) {
                    msg = msg.replace("{npc}", plugin.getNpcManager().getDisplayName(npcId));
                }
                final String finalMsg = msg;
                plugin.getFoliaLib().getImpl().runAtEntity(player, __ -> {
                    player.sendMessage(ua.woody.questborn.lang.ColorFormatter.applyColors(finalMsg));
                });
            } else if (action.startsWith("title:")) {
                String title = action.substring("title:".length()).trim();
                plugin.getFoliaLib().getImpl().runAtEntity(player, __ -> {
                    player.sendTitle(ua.woody.questborn.lang.ColorFormatter.applyColors(title), "", 10, 70, 20);
                });
            } else if (action.startsWith("sound:")) {
                String soundStr = action.substring("sound:".length()).trim();
                plugin.getFoliaLib().getImpl().runAtEntity(player, __ -> {
                    try {
                        org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundStr.toUpperCase());
                        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                    } catch (Exception ignored) {}
                });
            } else if (action.startsWith("take_money:")) {
                String amtStr = action.substring("take_money:".length()).trim();
                try {
                    double amt = Double.parseDouble(amtStr);
                    if (plugin.getEconomy() != null && plugin.getEconomy().has(player, amt)) {
                        plugin.getEconomy().withdrawPlayer(player, amt);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public static void spawnParticles(QuestbornPlugin plugin, Player player, Particle particle, int count) {
        plugin.getFoliaLib().getImpl().runAtEntity(player, __ -> {
            player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1.5, 0), count, 0.5, 0.5, 0.5, 0.1);
        });
    }
}
