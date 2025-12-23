package ua.woody.questborn.model;

import java.util.Locale;

public enum AfterMode {
    ALL,
    ANY;

    public static AfterMode fromString(String s) {
        if (s == null) return ALL;
        try {
            return AfterMode.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return ALL;
        }
    }
}
