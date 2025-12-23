package ua.woody.questborn.util;

import ua.woody.questborn.QuestbornPlugin;

public class TimeFormatter {

    private static QuestbornPlugin plugin;

    // Викликається один раз у onEnable()
    public static void init(QuestbornPlugin pl) {
        plugin = pl;
    }

    private static String tr(String path) {
        return plugin.getLanguage().tr(path);
    }

    /**
     * Форматує секунди у локалізований вигляд:
     *  - 2р. 3міс.
     *  - 5міс. 12д.
     *  - 12д. 4г.
     *  - 3г. 40хв.
     *  - 18хв. 22сек.
     *  - 45сек.
     */
    public static String format(long sec) {
        if (sec <= 0) {
            return formatSeconds(0);
        }

        long secondsInMinute = 60;
        long secondsInHour = 3600;
        long secondsInDay = 86400;
        long secondsInMonth = secondsInDay * 30;
        long secondsInYear = secondsInDay * 365;

        long years = sec / secondsInYear;
        sec %= secondsInYear;

        long months = sec / secondsInMonth;
        sec %= secondsInMonth;

        long days = sec / secondsInDay;
        sec %= secondsInDay;

        long hours = sec / secondsInHour;
        sec %= secondsInHour;

        long minutes = sec / secondsInMinute;
        long seconds = sec % secondsInMinute;

        // --- YEARS + MONTHS ---
        if (years > 0) {
            return tr("time.format.year-month")
                    .replace("{y}", String.valueOf(years))
                    .replace("{y_unit}", getUnit("year", "years", years))
                    .replace("{m}", String.valueOf(months))
                    .replace("{m_unit}", getUnit("month", "months", months));
        }

        // --- MONTHS + DAYS ---
        if (months > 0) {
            return tr("time.format.month-day")
                    .replace("{m}", String.valueOf(months))
                    .replace("{m_unit}", getUnit("month", "months", months))
                    .replace("{d}", String.valueOf(days))
                    .replace("{d_unit}", getUnit("day", "days", days));
        }

        // --- DAYS + HOURS ---
        if (days > 0) {
            return tr("time.format.day-hour")
                    .replace("{d}", String.valueOf(days))
                    .replace("{d_unit}", getUnit("day", "days", days))
                    .replace("{h}", String.valueOf(hours))
                    .replace("{h_unit}", getUnit("hour", "hours", hours));
        }

        // --- HOURS + MINUTES ---
        if (hours > 0) {
            return tr("time.format.hour-minute")
                    .replace("{h}", String.valueOf(hours))
                    .replace("{h_unit}", getUnit("hour", "hours", hours))
                    .replace("{min}", String.valueOf(minutes))
                    .replace("{min_unit}", getUnit("minute", "minutes", minutes));
        }

        // --- MINUTES + SECONDS ---
        if (minutes > 0) {
            return tr("time.format.minute-second")
                    .replace("{min}", String.valueOf(minutes))
                    .replace("{min_unit}", getUnit("minute", "minutes", minutes))
                    .replace("{s}", String.valueOf(seconds))
                    .replace("{s_unit}", getUnit("second", "seconds", seconds));
        }

        // --- ONLY SECONDS ---
        return formatSeconds(seconds);
    }

    private static String formatSeconds(long sec) {
        return tr("time.format.only-second")
                .replace("{s}", String.valueOf(sec))
                .replace("{s_unit}", getUnit("second", "seconds", sec));
    }

    private static String getUnit(String singular, String plural, long value) {
        return value == 1
                ? tr("time." + singular)
                : tr("time." + plural);
    }
}
