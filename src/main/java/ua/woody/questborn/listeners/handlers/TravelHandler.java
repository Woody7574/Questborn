package ua.woody.questborn.listeners.handlers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjectiveType;
import ua.woody.questborn.util.RegionUtils;

public class TravelHandler extends AbstractQuestHandler {

    public TravelHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onTravel(PlayerMoveEvent e) {
        if (e.getFrom().distanceSquared(e.getTo()) < 0.0001) return;

        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.TRAVEL_DISTANCE) return;

        // Рахуємо тільки XZ переміщення
        double dx = e.getTo().getX() - e.getFrom().getX();
        double dz = e.getTo().getZ() - e.getFrom().getZ();
        double dist = Math.sqrt(dx*dx + dz*dz);

        PlayerQuestProgress data = plugin.getPlayerDataStore().get(p.getUniqueId());

        // додаємо дистанцію
        data.addTravel(dist);

        // отримуємо скільки "повних блоків" накопичено
        int blocks = data.consumeTravel(); // ✔ тепер PEP повертає саме ЦІЛІ БЛОКИ

        if (blocks > 0) {
            progress(p, q, blocks); // ✔ тепер рахує не 1, а 500 як в amount
        }
    }



    public void onReachLocation(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();
        if (o.getType() != QuestObjectiveType.REACH_LOCATION) return;

        Location target = new Location(p.getWorld(), o.getX(), o.getY(), o.getZ());
        if (p.getLocation().distance(target) <= 2) {
            progress(p, q, 1);
        }
    }

    public void onRegionMove(PlayerMoveEvent e) {
        if (e.getFrom().distanceSquared(e.getTo()) < 0.01) return;

        Player p = e.getPlayer();
        QuestDefinition q = getActiveQuest(p);
        if (q == null) return;

        var o = q.getObjective();

        if (o.getType() == QuestObjectiveType.ENTER_REGION) {
            String region = o.getRegion();
            if (region == null) return;

            var from = RegionUtils.getRegionsAt(e.getFrom());
            var to = RegionUtils.getRegionsAt(e.getTo());

            if (!from.contains(region.toLowerCase()) && to.contains(region.toLowerCase())) {
                progress(p, q, 1);
            }
        }

        if (o.getType() == QuestObjectiveType.LEAVE_REGION) {
            String region = o.getRegion();
            if (region == null) return;

            var from = RegionUtils.getRegionsAt(e.getFrom());
            var to = RegionUtils.getRegionsAt(e.getTo());

            if (from.contains(region.toLowerCase()) && !to.contains(region.toLowerCase())) {
                progress(p, q, 1);
            }
        }
    }
}