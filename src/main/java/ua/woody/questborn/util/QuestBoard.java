package ua.woody.questborn.util;

import java.util.List;

public interface QuestBoard {
    void updateTitle(String title);
    void updateLines(List<String> lines);
    void delete();
    boolean isDeleted();
}
