package ua.woody.questborn.listeners.handlers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityHandler extends AbstractQuestHandler {

    private final Map<UUID, Map<UUID, Double>> playerDamageMap = new HashMap<>();

    public EntityHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    // ==================== KILL_ENTITY та PLAYER_KILL ====================
    @EventHandler
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        QuestDefinition q = getActiveQuest(killer);
        if (q == null) return;

        var o = q.getObjective();

        // Звичайне вбивство істоти
        if (o.getType() == QuestObjectiveType.KILL_ENTITY) {
            if (o.getWeapon() != null && !o.getWeapon().isEmpty()) {
                Material usedWeapon = killer.getInventory().getItemInMainHand().getType();
                if (!o.getWeapon().equalsIgnoreCase(usedWeapon.name())) return;
            }

            if (o.isTargetEntity(e.getEntityType())) {
                progress(killer, q, 1);
            }
        }

        // Вбивство гравця
        else if (o.getType() == QuestObjectiveType.PLAYER_KILL && e.getEntity() instanceof Player victim) {
            if (o.getWeapon() != null && !o.getWeapon().isEmpty()) {
                Material usedWeapon = killer.getInventory().getItemInMainHand().getType();
                if (!o.getWeapon().equalsIgnoreCase(usedWeapon.name())) return;
            }

            // Перевірка за типом зброї (якщо вказано)
            boolean weaponMatches = true;
            if (o.getWeapon() != null && !o.getWeapon().isEmpty()) {
                Material usedWeapon = killer.getInventory().getItemInMainHand().getType();
                weaponMatches = o.isTargetWeapon(usedWeapon);
            }

            if (weaponMatches) {
                progress(killer, q, 1);
            }
        }
    }

    // ==================== DEAL_DAMAGE ====================
    @EventHandler
    public void onDealDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() == QuestObjectiveType.DEAL_DAMAGE) {
            progress(p, q, (int) e.getDamage());
        }

        // Записуємо шкоду для ASSIST_KILL (тільки якщо ціль - гравець)
        if (e.getEntity() instanceof Player victim) {
            UUID victimId = victim.getUniqueId();
            UUID damagerId = p.getUniqueId();

            playerDamageMap.putIfAbsent(victimId, new HashMap<>());
            playerDamageMap.get(victimId).put(damagerId,
                    playerDamageMap.get(victimId).getOrDefault(damagerId, 0.0) + e.getDamage());
        }
    }

    // ==================== TAKE_DAMAGE та RECEIVE_DAMAGE_TYPE ====================
    @EventHandler
    public void onTakeDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();

        // Звичайне отримання шкоди
        if (o.getType() == QuestObjectiveType.TAKE_DAMAGE) {
            progress(p, q, (int) e.getDamage());
        }

        // Отримання шкоди певного типу
        else if (o.getType() == QuestObjectiveType.RECEIVE_DAMAGE_TYPE) {
            EntityDamageEvent.DamageCause cause = e.getCause();

            // Перевіряємо тип шкоди
            if (o.getMessage() != null && !o.getMessage().isEmpty()) {
                String targetCause = o.getMessage().toUpperCase();
                String actualCause = cause.name();

                if (actualCause.equals(targetCause) || targetCause.equals("ANY")) {
                    progress(p, q, (int) e.getDamage());
                }
            } else {
                // Якщо тип не вказано - будь-яка шкода
                progress(p, q, (int) e.getDamage());
            }
        }
    }

    // ==================== ASSIST_KILL ====================
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        UUID victimId = victim.getUniqueId();

        // Знаходимо всіх, хто наносив шкоду цьому гравцю
        Map<UUID, Double> damagers = playerDamageMap.get(victimId);
        if (damagers == null) return;

        for (Map.Entry<UUID, Double> entry : damagers.entrySet()) {
            Player damager = plugin.getServer().getPlayer(entry.getKey());
            if (damager != null && damager.isOnline()) {
                QuestDefinition q = getActiveQuest(damager);
                if (q == null) continue;

                var o = q.getObjective();
                if (o.getType() == QuestObjectiveType.ASSIST_KILL) {
                    double damageDealt = entry.getValue();
                    double minDamage = o.getAmount() > 0 ? o.getAmount() : 1.0;

                    if (damageDealt >= minDamage) {
                        progress(damager, q, 1);
                    }
                }
            }
        }

        // Очищаємо дані про шкоду для цього гравця
        playerDamageMap.remove(victimId);
    }

    // ==================== Очищення даних ====================
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent e) {
        UUID playerId = e.getPlayer().getUniqueId();

        // Видаляємо гравця з мапи як жертву
        playerDamageMap.remove(playerId);

        // Видаляємо гравця з мапи як нападника
        for (Map<UUID, Double> damagers : playerDamageMap.values()) {
            damagers.remove(playerId);
        }
    }
}