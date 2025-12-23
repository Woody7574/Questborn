package ua.woody.questborn.update;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.lang.LanguageManager;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class UpdateChecker {

    private final QuestbornPlugin plugin;
    private final LanguageManager lang;
    private final int spigotResourceId;

    public UpdateChecker(QuestbornPlugin plugin, LanguageManager lang, int spigotResourceId) {
        this.plugin = plugin;
        this.lang = lang;
        this.spigotResourceId = spigotResourceId;
    }

    public void checkOnStartup() {
        if (!plugin.getConfig().getBoolean("update-check.enabled", true)) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String current = safe(plugin.getDescription().getVersion());
            String latest = null;

            try {
                latest = fetchLatestVersion();

                if (latest != null && isNewer(latest, current)) {
                    boolean notify = plugin.getConfig().getBoolean("update-check.notify-console", true);
                    if (!notify) return;

                    String url = "https://www.spigotmc.org/resources/" + spigotResourceId + "/";

                    Map<String, String> params = new HashMap<>();
                    params.put("plugin", plugin.getName());
                    params.put("current", current);
                    params.put("latest", latest);
                    params.put("url", url);

                    // Виводимо локалізовані рядки (list або fallback string)
                    List<String> lines = lang.trList("update.available", params);
                    if (lines == null || lines.isEmpty()) {
                        String one = lang.tr("update.available", params);
                        lines = List.of(one);
                    }

                    // Лог краще робити в main thread (не обов'язково, але безпечніше)
                    List<String> finalLines = lines;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (String line : finalLines) {
                            // console-friendly
                            plugin.getLogger().warning(ChatColor.stripColor(line));
                        }
                    });
                }

            } catch (Exception ex) {
                boolean logErrors = plugin.getConfig().getBoolean("update-check.log-errors", false);
                if (logErrors) {
                    plugin.getLogger().warning("Update check failed: " + ex.getMessage());
                }
            }
        });
    }

    /* ------------------------------ */

    private String fetchLatestVersion() throws Exception {
        URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + spigotResourceId);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setRequestProperty("User-Agent", plugin.getName() + "/" + plugin.getDescription().getVersion());

        try (InputStream in = con.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    private static boolean isNewer(String latest, String current) {
        return compareVersions(latest, current) > 0;
    }

    private static int compareVersions(String a, String b) {
        List<String> pa = splitVersion(a);
        List<String> pb = splitVersion(b);

        int max = Math.max(pa.size(), pb.size());
        for (int i = 0; i < max; i++) {
            String sa = i < pa.size() ? pa.get(i) : "0";
            String sb = i < pb.size() ? pb.get(i) : "0";

            int cmp = comparePart(sa, sb);
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    private static List<String> splitVersion(String v) {
        v = safe(v).toLowerCase(Locale.ROOT).trim();
        if (v.startsWith("v") && v.length() > 1) v = v.substring(1);

        v = v.replaceAll("[^0-9a-z]+", ".");
        String[] raw = v.split("\\.");

        List<String> parts = new ArrayList<>();
        for (String s : raw) {
            if (!s.isEmpty()) parts.add(s);
        }
        return parts;
    }

    private static int comparePart(String a, String b) {
        boolean na = a.matches("\\d+");
        boolean nb = b.matches("\\d+");

        if (na && nb) {
            return new BigInteger(a).compareTo(new BigInteger(b));
        }
        // numeric > text (1.0.0 > 1.0.0-beta)
        if (na != nb) return na ? 1 : -1;

        return a.compareTo(b);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
