package ua.woody.questborn.listeners;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.PlayerQuestProgress;

public class SkinUpdateListener implements Listener {
    private final QuestbornPlugin plugin;

    public SkinUpdateListener(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getFoliaLib().getImpl().runLaterAsync(() -> {
            try {
                PlayerQuestProgress progress = plugin.getPlayerDataStore().get(event.getPlayer().getUniqueId());
                if (progress == null)
                    return;

                String texture = null;
                String signature = null;

                if (plugin.getSkinsRestorerProvider() != null && plugin.getSkinsRestorerProvider().isEnabled()) {
                    String[] skinData = plugin.getSkinsRestorerProvider()
                            .getSkinData(event.getPlayer().getUniqueId(), event.getPlayer().getName())
                            .orElse(null);
                    if (skinData != null) {
                        texture = skinData[0];
                        signature = skinData[1];
                    }
                }

                if (texture == null) {
                    try {
                        PlayerProfile profile = event.getPlayer().getPlayerProfile();
                        for (ProfileProperty property : profile.getProperties()) {
                            if ("textures".equals(property.getName())) {
                                texture = property.getValue();
                                signature = property.getSignature();
                                break;
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }

                if (texture != null) {
                    boolean changed = !texture.equals(progress.getSkinTexture()) ||
                            (signature != null && !signature.equals(progress.getSkinSignature()));

                    if (changed) {
                        progress.setSkinTexture(texture);
                        progress.setSkinSignature(signature);

                        plugin.getPlayerDataStore().saveIfDirty(event.getPlayer().getUniqueId());
                    }
                }

            } catch (Exception e) {
                plugin.getLogger().warning(
                        "[Questborn] Failed to update skin for " + event.getPlayer().getName() + ": " + e.getMessage());
            }
        }, 20L);
    }
}
