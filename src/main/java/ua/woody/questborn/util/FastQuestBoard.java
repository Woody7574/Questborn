package ua.woody.questborn.util;

import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.entity.Player;

import java.util.List;

public class FastQuestBoard implements QuestBoard {
    private final FastBoard board;
    private boolean deleted = false;

    public FastQuestBoard(Player player, boolean hideScores) {
        this.board = new FastBoard(player);
    }

    @Override
    public void updateTitle(String title) {
        this.board.updateTitle(title);
    }

    @Override
    public void updateLines(List<String> lines) {
        if (deleted || board.isDeleted()) return;
        this.board.updateLines(lines);
    }

    @Override
    public void delete() {
        this.deleted = true;
        this.board.delete();
    }

    @Override
    public boolean isDeleted() {
        return this.deleted || this.board.isDeleted();
    }
}
