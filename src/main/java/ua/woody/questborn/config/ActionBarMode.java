package ua.woody.questborn.config;

public enum ActionBarMode {
    STATIC,
    ON_PROGRESS_CHANGE;

    public static ActionBarMode fromString(String s) {
        try {
            return ActionBarMode.valueOf(s.toUpperCase());
        } catch (Exception ex) {
            return ON_PROGRESS_CHANGE;
        }
    }
}
