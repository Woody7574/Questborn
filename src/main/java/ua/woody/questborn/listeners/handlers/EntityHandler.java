package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityHandler extends AbstractQuestHandler {
    private final Map<UUID, Map<UUID, Double>> playerDamageMap = new HashMap<>();

    private final NamespacedKey spawnerKey;

    public EntityHandler(QuestbornPlugin plugin) {
        super(plugin);
        this.spawnerKey = new NamespacedKey(plugin, "spawner_mob");
    }

    public void onEntitySpawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            e.getEntity().getPersistentDataContainer().set(spawnerKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        QuestDefinition q = getActiveQuest(killer);
        if (q == null) return;

        QuestObjective o = getCurrentObjective(killer, q);
        if (o == null) return;

        if (o.getType() == QuestObjectiveType.KILL_ENTITY) {
            if (!plugin.getConfig().getBoolean("gameplay.allow-spawner-kills", false)) {
                if (e.getEntity().getPersistentDataContainer().has(spawnerKey, PersistentDataType.BYTE)) {
                    return;
                }
            }

            if (!weaponMatches(killer, o)) return;

            if (o.isTargetEntity(e.getEntity())) {
                progress(killer, q, 1);
            }
            return;
        }

        if (o.getType() == QuestObjectiveType.PLAYER_KILL && e.getEntity() instanceof Player) {
            if (!weaponMatches(killer, o)) return;
            progress(killer, q, 1);
        }
    }

    private boolean weaponMatches(Player killer, QuestObjective o) {
        if (o.getWeapon() == null || o.getWeapon().isEmpty()) return true;

        Material used = killer.getInventory().getItemInMainHand().getType();

        try {
            return o.isTargetWeapon(used);
        } catch (Throwable ignored) {
            return o.getWeapon().equalsIgnoreCase(used.name());
        }
    }

    public void onDealDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;

        QuestDefinition q = getActiveQuest(p);
        if (q != null) {
            QuestObjective o = getCurrentObjective(p, q);
            if (o != null && o.getType() == QuestObjectiveType.DEAL_DAMAGE) {
                int add = (int) Math.ceil(e.getFinalDamage());
                if (add < 1) add = 1;
                progress(p, q, add);
            }
        }

        if (e.getEntity() instanceof Player victim) {
            UUID victimId = victim.getUniqueId();
            UUID damagerId = p.getUniqueId();

            playerDamageMap.putIfAbsent(victimId, new HashMap<>());
            Map<UUID, Double> damagers = playerDamageMap.get(victimId);

            damagers.put(damagerId, damagers.getOrDefault(damagerId, 0.0) + e.getFinalDamage());
        }
    }

    public void onTakeDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        QuestObjective o = getCurrentObjective(p, q);
        if (o == null) return;

        if (o.getType() == QuestObjectiveType.TAKE_DAMAGE) {
            int add = (int) Math.ceil(e.getFinalDamage());
            if (add < 1) add = 1;
            progress(p, q, add);
            return;
        }

        if (o.getType() == QuestObjectiveType.RECEIVE_DAMAGE_TYPE) {
            EntityDamageEvent.DamageCause cause = e.getCause();

            String target = (o.getMessage() != null ? o.getMessage().trim() : "");
            if (!target.isEmpty()) {
                String targetCause = target.toUpperCase();
                if (!targetCause.equals("ANY") && !cause.name().equalsIgnoreCase(targetCause)) return;
            }

            int add = (int) Math.ceil(e.getFinalDamage());
            if (add < 1) add = 1;
            progress(p, q, add);
        }
    }

    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        UUID victimId = victim.getUniqueId();

        Map<UUID, Double> damagers = playerDamageMap.get(victimId);
        if (damagers == null || damagers.isEmpty()) return;

        Player killer = victim.getKiller();
        UUID killerId = (killer != null ? killer.getUniqueId() : null);

        for (Map.Entry<UUID, Double> entry : damagers.entrySet()) {
            UUID damagerId = entry.getKey();

            if (killerId != null && killerId.equals(damagerId)) continue;

            Player damager = plugin.getServer().getPlayer(damagerId);
            if (damager == null || !damager.isOnline()) continue;

            QuestDefinition q = getActiveQuest(damager);
            if (q == null) continue;

            QuestObjective o = getCurrentObjective(damager, q);
            if (o == null) continue;

            if (o.getType() != QuestObjectiveType.ASSIST_KILL) continue;

            double damageDealt = entry.getValue();
            double minDamage = (o.getAmount() > 0 ? o.getAmount() : 1.0);

            if (damageDealt >= minDamage) {
                progress(damager, q, 1);
            }
        }

        playerDamageMap.remove(victimId);
    }

    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent e) {
        UUID playerId = e.getPlayer().getUniqueId();

        playerDamageMap.remove(playerId);
        for (Map<UUID, Double> damagers : playerDamageMap.values()) {
            damagers.remove(playerId);
        }
    }
}
