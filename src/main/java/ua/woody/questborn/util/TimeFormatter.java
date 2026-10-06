package ua.woody.questborn.util;

import ua.woody.questborn.QuestbornPlugin;

public class TimeFormatter {
    private static QuestbornPlugin plugin;

    public static void init(QuestbornPlugin pl) {
        plugin = pl;
    }

    private static String tr(String path) {
        return plugin.getLanguage().tr(path);
    }

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

        if (years > 0) {
            return tr("system.time.format.year-month")
                    .replace("{y}", String.valueOf(years))
                    .replace("{y_unit}", getUnit("year", "years", years))
                    .replace("{m}", String.valueOf(months))
                    .replace("{m_unit}", getUnit("month", "months", months));
        }

        if (months > 0) {
            return tr("system.time.format.month-day")
                    .replace("{m}", String.valueOf(months))
                    .replace("{m_unit}", getUnit("month", "months", months))
                    .replace("{d}", String.valueOf(days))
                    .replace("{d_unit}", getUnit("day", "days", days));
        }

        if (days > 0) {
            return tr("system.time.format.day-hour")
                    .replace("{d}", String.valueOf(days))
                    .replace("{d_unit}", getUnit("day", "days", days))
                    .replace("{h}", String.valueOf(hours))
                    .replace("{h_unit}", getUnit("hour", "hours", hours));
        }

        if (hours > 0) {
            return tr("system.time.format.hour-minute")
                    .replace("{h}", String.valueOf(hours))
                    .replace("{h_unit}", getUnit("hour", "hours", hours))
                    .replace("{min}", String.valueOf(minutes))
                    .replace("{min_unit}", getUnit("minute", "minutes", minutes));
        }

        if (minutes > 0) {
            return tr("system.time.format.minute-second")
                    .replace("{min}", String.valueOf(minutes))
                    .replace("{min_unit}", getUnit("minute", "minutes", minutes))
                    .replace("{s}", String.valueOf(seconds))
                    .replace("{s_unit}", getUnit("second", "seconds", seconds));
        }

        return formatSeconds(seconds);
    }

    private static String formatSeconds(long sec) {
        return tr("system.time.format.only-second")
                .replace("{s}", String.valueOf(sec))
                .replace("{s_unit}", getUnit("second", "seconds", sec));
    }

    private static String getUnit(String singular, String plural, long value) {
        return value == 1
                ? tr("system.time." + singular)
                : tr("system.time." + plural);
    }

    public static long parseDuration(String input) {
        if (input == null || input.trim().isEmpty()) {
            return 0L;
        }

        input = input.trim().toLowerCase();

        if (input.matches("\\d+")) {
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }

        long totalSeconds = 0L;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)([dhms])").matcher(input);

        while (m.find()) {
            try {
                long val = Long.parseLong(m.group(1));
                String unit = m.group(2);

                switch (unit) {
                    case "d":
                        totalSeconds += val * 86400L;
                        break;
                    case "h":
                        totalSeconds += val * 3600L;
                        break;
                    case "m":
                        totalSeconds += val * 60L;
                        break;
                    case "s":
                        totalSeconds += val;
                        break;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return totalSeconds;
    }
}
