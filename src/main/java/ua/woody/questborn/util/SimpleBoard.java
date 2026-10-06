package ua.woody.questborn.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;

public class SimpleBoard implements QuestBoard {
    private final Player player;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private boolean deleted = false;
    private String title = "";
    private Scoreboard previousScoreboard;
    private final boolean hideScores;

    public SimpleBoard(Player player, boolean hideScores) {
        this(player, hideScores, Bukkit.getScoreboardManager().getNewScoreboard());
    }

    public SimpleBoard(Player player, boolean hideScores, Scoreboard board) {
        this.player = player;
        this.hideScores = hideScores;
        this.previousScoreboard = player.getScoreboard();

        this.scoreboard = board;

        Objective obj = board.getObjective("qb_" + player.getEntityId());
        if (obj == null) {
            obj = board.registerNewObjective("qb_" + player.getEntityId(), "dummy", "Questborn");
        }
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.objective = obj;

        applyHideScores();

        player.setScoreboard(board);
    }

    public void updateTitle(String title) {
        this.title = title;
        this.objective.setDisplayName(title);
    }

    public void updateLines(List<String> lines) {
        if (deleted) return;

        for (String entry : scoreboard.getEntries()) {
            if (objective.getScore(entry).isScoreSet()) {
                int score = objective.getScore(entry).getScore();
                if (score >= lines.size()) {
                    scoreboard.resetScores(entry);
                }
            }
        }

        int score = lines.size() - 1;
        for (String line : lines) {
            String teamName = "line_" + score;
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }

            String entry = getEntryForScore(score);

            if (!team.hasEntry(entry)) {
                team.addEntry(entry);
            }

            team.setPrefix(line);
            objective.getScore(entry).setScore(score);
            score--;
        }
    }

    public void delete() {
        this.deleted = true;
        if (player.isOnline() && player.getScoreboard() == scoreboard) {
            if (previousScoreboard != null) {
                player.setScoreboard(previousScoreboard);
            } else {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
    }

    public boolean isDeleted() {
        return deleted;
    }

    private String getEntryForScore(int score) {
        if (score < 0 || score > 15) return ChatColor.RESET.toString();
        return ChatColor.values()[score].toString() + ChatColor.RESET;
    }

    private void applyHideScores() {
        if (!this.hideScores) return;
        try {
            boolean applied = false;

            try {
                Class<?> paperFormatClass = Class.forName("io.papermc.paper.scoreboard.numbers.NumberFormat");
                Object blankFormat = paperFormatClass.getMethod("blank").invoke(null);

                java.lang.reflect.Method method = org.bukkit.scoreboard.Objective.class.getMethod("numberFormat", paperFormatClass);
                method.invoke(this.objective, blankFormat);
                applied = true;
            } catch (Throwable t) {
            }

            if (applied) return;

            try {
                Class<?> bukkitFormatClass = Class.forName("org.bukkit.scoreboard.NumberFormat");
                Object blankFormat = bukkitFormatClass.getMethod("blank").invoke(null);

                try {
                    java.lang.reflect.Method method = org.bukkit.scoreboard.Objective.class.getMethod("numberFormat", bukkitFormatClass);
                    method.invoke(this.objective, blankFormat);
                    applied = true;
                } catch (NoSuchMethodException e) {
                    java.lang.reflect.Method method = org.bukkit.scoreboard.Objective.class.getMethod("setNumberFormat", bukkitFormatClass);
                    method.invoke(this.objective, blankFormat);
                    applied = true;
                }
            } catch (Throwable t) {
            }

            if (!applied) {
                Bukkit.getLogger().warning("[Questborn] Could not apply hide-scores: numberFormat method not found in Bukkit API for this version.");
            }
        } catch (Throwable e) {
            Bukkit.getLogger().warning("[Questborn] Error applying hide-scores: " + e.getMessage());
        }
    }
}
