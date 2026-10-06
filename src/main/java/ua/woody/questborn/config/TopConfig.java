package ua.woody.questborn.config;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.integration.skin.SkinsRestorerProvider;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.model.TopEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TopConfig {
    private final QuestbornPlugin plugin;

    public TopConfig(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
    }

    public void reload() {
    }

    private ua.woody.questborn.config.GuiLayout getLayout() {
        return plugin.getMenuConfig().getLayout("top");
    }

    public boolean isEnabled() {
        ua.woody.questborn.config.GuiLayout layout = getLayout();
        if (layout == null)
            return false;
        return layout.getBoolean("enabled", true);
    }

    public boolean isTopEnabled() {
        return isEnabled();
    }

    public int getLimit() {
        ua.woody.questborn.config.GuiLayout layout = getLayout();
        if (layout == null)
            return 100;
        return layout.getInt("limit", 100);
    }

    public int getSize() {
        return getLimit();
    }

    public ItemStack createPlayerItem(TopEntry entry, int position, int questCount) {
        ua.woody.questborn.config.GuiLayout layout = getLayout();
        if (layout == null)
            return new ItemStack(Material.PLAYER_HEAD);

        ua.woody.questborn.config.GuiItemConfig itemConfig = layout.getItem("player");
        if (itemConfig == null)
            return new ItemStack(Material.PLAYER_HEAD);

        ItemStack item = itemConfig.createItemStack(plugin);

        if (item.getType() == Material.PLAYER_HEAD && entry != null) {
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            if (skullMeta != null) {
                setSkullProfile(skullMeta, entry);
                item.setItemMeta(skullMeta);
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String playerName = entry != null ? entry.getName() : "Unknown";
            if (playerName == null)
                playerName = "Unknown";

            String rankColor = "&7";
            String rankPrefix = "";
            String footer = "";

            Object rankStylesObj = layout.getProperty("rank-styles");
            if (rankStylesObj instanceof ConfigurationSection) {
                ConfigurationSection rankStyles = (ConfigurationSection) rankStylesObj;
                String key = String.valueOf(position);
                ConfigurationSection style = rankStyles.getConfigurationSection(key);

                if (style == null) {
                    style = rankStyles.getConfigurationSection("default");
                }

                if (style != null) {
                    rankPrefix = style.getString("prefix", "");

                    if (style.contains("color")) {
                        rankColor = style.getString("color");
                    } else {
                        String lastColor = ua.woody.questborn.lang.ColorFormatter.getLastColor(rankPrefix);
                        if (!lastColor.isEmpty()) {
                            rankColor = lastColor;
                        }
                    }

                    footer = style.getString("footer", "");
                }
            } else {
                if (position == 1) {
                    rankColor = "&6&l";
                    rankPrefix = "♛ ";
                    footer = "&6&l🏆 TOP 1 LEADER 🏆";
                } else if (position == 2) {
                    rankColor = "&f&l";
                    rankPrefix = "🥈 ";
                    footer = "&f&l🥈 RUNNER UP";
                } else if (position == 3) {
                    rankColor = "&6";
                    rankPrefix = "🥉 ";
                    footer = "&6&l🥉 THIRD PLACE";
                }
            }

            if (meta.hasDisplayName()) {
                String displayName = meta.getDisplayName()
                        .replace("{player}", rankColor + playerName)
                        .replace("{position}", rankPrefix + rankColor + "#" + position)
                        .replace("{count}", String.valueOf(questCount));
                meta.setDisplayName(plugin.getLanguage().color(displayName));
            }

            if (meta.hasLore()) {
                List<String> coloredLore = new ArrayList<>();

                String activeQuestName = "&cNone";

                if (entry != null && entry.getUuid() != null) {
                    try {
                        ua.woody.questborn.storage.PlayerDataStore store = plugin.getPlayerDataStore();
                        if (store != null) {
                            ua.woody.questborn.model.PlayerQuestProgress progress = store
                                    .getCachedOrNull(entry.getUuid());
                            if (progress != null) {
                                String qId = progress.getTrackedQuestId();
                                if (qId != null) {
                                    ua.woody.questborn.model.QuestDefinition quest = plugin.getQuestManager()
                                            .getQuest(qId);
                                    if (quest != null) {
                                        activeQuestName = "&a" + quest.getDisplayName();
                                    } else {
                                        activeQuestName = "&7" + qId;
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }

                for (String line : meta.getLore()) {
                    String processedLine = line
                            .replace("{player}", playerName)
                            .replace("{position}", String.valueOf(position))
                            .replace("{count}", String.valueOf(questCount))
                            .replace("{active_quest}", activeQuestName);

                    coloredLore.add(plugin.getLanguage().color(processedLine));
                }

                if (footer != null && !footer.isEmpty()) {
                    coloredLore.add(" ");
                    coloredLore.add(plugin.getLanguage().color(footer));
                }

                meta.setLore(coloredLore);
            }

            ua.woody.questborn.gui.GuiUtils.applyAllItemFlags(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createStatsItem(String playerName, Map<String, Integer> questCountsByType, int rank) {
        ua.woody.questborn.config.GuiLayout layout = getLayout();
        if (layout == null)
            return new ItemStack(Material.PLAYER_HEAD);

        ua.woody.questborn.config.GuiItemConfig itemConfig = layout.getItem("stats");
        if (itemConfig == null)
            return new ItemStack(Material.PLAYER_HEAD);

        ItemStack item = itemConfig.createItemStack(plugin);

        if (item.getType() == Material.PLAYER_HEAD) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            TopEntry tempEntry = new TopEntry(player.getUniqueId(), playerName, 0);

            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            if (skullMeta != null) {
                setSkullProfile(skullMeta, tempEntry);
                item.setItemMeta(skullMeta);
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String rankStr = rank > 0 ? String.valueOf(rank) : "-";

            if (meta.hasDisplayName()) {
                meta.setDisplayName(meta.getDisplayName()
                        .replace("{player}", playerName)
                        .replace("{rank}", rankStr));
            }

            List<String> rawLore = itemConfig.getLore();
            if (rawLore != null && !rawLore.isEmpty()) {
                List<String> finalLore = new ArrayList<>();
                int totalQuests = questCountsByType.values().stream().mapToInt(Integer::intValue).sum();

                for (String line : rawLore) {
                    if (line.contains("{type}") && line.contains("{count}")) {
                        for (Map.Entry<String, Integer> entry : questCountsByType.entrySet()) {
                            String typeId = entry.getKey();
                            QuestTypeConfig typeConfig = plugin.getQuestManager().getQuestTypeManager().getType(typeId);
                            if (typeConfig != null && typeConfig.isEnabled()) {
                                int count = entry.getValue();
                                if (count > 0) {
                                    String typeName = typeConfig.getDisplayName();
                                    String processedLine = line
                                            .replace("{type}", typeName)
                                            .replace("{count}", String.valueOf(count));
                                    finalLore.add(plugin.getLanguage().color(processedLine));
                                }
                            }
                        }
                    } else {
                        String processedLine = line
                                .replace("{player}", playerName)
                                .replace("{total}", String.valueOf(totalQuests))
                                .replace("{rank}", rankStr);
                        finalLore.add(plugin.getLanguage().color(processedLine));
                    }
                }
                meta.setLore(finalLore);
            }

            ua.woody.questborn.gui.GuiUtils.applyAllItemFlags(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static final String STEVE_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTYxMjQ1NTdc3RyaW5nIiwKICAicHJvZmlsZUlkIiA6ICJjMDZiYjg1NDA1NGM0MDgwODk4NmFhYWRlYWNiMmFmMSIsCiAgInByb2ZpbGVOYW1lIiA6ICJTdGV2ZSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80NTZlZWNjZWFhNWM2NzUyN2U5ODliYWRiYjNiZjQyMzk4YThjMTlmMzEwOWM4YTUwYmIwNjZmZGI1OWM2Zjg1IgogICAgfQogIH0KfQ==";

    private void setSkullProfile(SkullMeta meta, TopEntry entry) {
        String textureValue = entry.getSkinTexture();
        String textureSignature = entry.getSkinSignature();
        UUID uuid = entry.getUuid();
        String name = entry.getName() != null ? entry.getName() : "Unknown";

        if (textureValue == null) {
            try {
                ua.woody.questborn.storage.PlayerDataStore store = plugin.getPlayerDataStore();
                if (store != null) {
                    ua.woody.questborn.model.PlayerQuestProgress progress = store.getCachedOrNull(uuid);
                    if (progress != null && progress.getSkinTexture() != null) {
                        textureValue = progress.getSkinTexture();
                        textureSignature = progress.getSkinSignature();
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (textureValue == null) {
            SkinsRestorerProvider srProvider = plugin.getSkinsRestorerProvider();
            if (srProvider != null && srProvider.isEnabled()) {
                java.util.Optional<String[]> skinData = srProvider.getSkinData(uuid, name);
                if (skinData.isPresent()) {
                    textureValue = skinData.get()[0];
                    textureSignature = skinData.get()[1];
                }
            }
        }

        org.bukkit.entity.Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (textureValue == null && onlinePlayer != null) {
            try {
                PlayerProfile profile = onlinePlayer.getPlayerProfile();
                for (ProfileProperty property : profile.getProperties()) {
                    if ("textures".equals(property.getName())) {
                        textureValue = property.getValue();
                        textureSignature = property.getSignature();
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (textureValue == null) {
            textureValue = STEVE_TEXTURE;
        }

        try {
            UUID id = new UUID(textureValue.hashCode(), textureValue.hashCode());
            PlayerProfile profile = Bukkit.createProfile(id, name);
            profile.setProperty(new ProfileProperty("textures", textureValue, textureSignature));
            meta.setPlayerProfile(profile);
        } catch (Throwable e) {
            try {
                java.lang.reflect.Method getPropertiesMethod = null;
                Object profile = null;
                try {
                    Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
                    UUID id = new UUID(textureValue.hashCode(), textureValue.hashCode());
                    profile = gameProfileClass.getConstructor(UUID.class, String.class).newInstance(id, name);
                    getPropertiesMethod = gameProfileClass.getMethod("getProperties");
                } catch (Exception ex) { }

                if (profile != null) {
                    Object propertyMap = getPropertiesMethod.invoke(profile);
                    Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
                    Object property;
                    try {
                        property = propertyClass.getConstructor(String.class, String.class, String.class).newInstance("textures", textureValue, textureSignature);
                    } catch (Exception ex) {
                        property = propertyClass.getConstructor(String.class, String.class).newInstance("textures", textureValue);
                    }
                    propertyMap.getClass().getMethod("put", Object.class, Object.class).invoke(propertyMap, "textures", property);

                    java.lang.reflect.Field profileField = meta.getClass().getDeclaredField("profile");
                    profileField.setAccessible(true);
                    profileField.set(meta, profile);
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
