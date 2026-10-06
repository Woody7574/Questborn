package ua.woody.questborn.util;

import org.bukkit.configuration.ConfigurationSection;

public class ProgressBarUtil {
    private final int length;
    private final String filledSymbol;
    private final String currentSymbol;
    private final String emptySymbol;
    private final String filledColor;
    private final String currentColor;
    private final String emptyColor;

    public ProgressBarUtil(int length, String filledSymbol, String currentSymbol, String emptySymbol,
            String filledColor, String currentColor, String emptyColor) {
        this.length = length;
        this.filledSymbol = filledSymbol;
        this.currentSymbol = currentSymbol;
        this.emptySymbol = emptySymbol;
        this.filledColor = filledColor;
        this.currentColor = currentColor;
        this.emptyColor = emptyColor;
    }

    public static ProgressBarUtil fromConfig(ConfigurationSection wConfig) {
        if (wConfig == null)
            return null;

        int length = wConfig.getInt("length", 10);
        String filledSymbol = wConfig.getString("filled-symbol", "■");
        String currentSymbol = wConfig.getString("current-symbol", "■");
        String emptySymbol = wConfig.getString("empty-symbol", "■");
        String filledColor = wConfig.getString("filled-color", "&a");
        String currentColor = wConfig.getString("current-color", "&e");
        String emptyColor = wConfig.getString("empty-color", "&7");

        return new ProgressBarUtil(length, filledSymbol, currentSymbol, emptySymbol, filledColor, currentColor,
                emptyColor);
    }

    public String getProgressBar(int current, int max) {
        if (max <= 0) {
            return "";
        }

        double percent = (double) current / max;
        if (percent > 1.0)
            percent = 1.0;
        if (percent < 0.0)
            percent = 0.0;

        int filledAmount = (int) (percent * length);
        if (filledAmount > length) {
            filledAmount = length;
        }

        StringBuilder bar = new StringBuilder();

        if (filledAmount > 0) {
            bar.append(filledColor);
            for (int i = 0; i < filledAmount; i++) {
                bar.append(filledSymbol);
            }
        }

        if (filledAmount < length) {
            if (current > 0) {
                bar.append(currentColor).append(currentSymbol);

                int emptyAmount = length - filledAmount - 1;
                if (emptyAmount > 0) {
                    bar.append(emptyColor);
                    for (int i = 0; i < emptyAmount; i++) {
                        bar.append(emptySymbol);
                    }
                }
            } else {
                int emptyAmount = length - filledAmount;
                if (emptyAmount > 0) {
                    bar.append(emptyColor);
                    for (int i = 0; i < emptyAmount; i++) {
                        bar.append(emptySymbol);
                    }
                }
            }
        }

        return bar.toString();
    }
}
