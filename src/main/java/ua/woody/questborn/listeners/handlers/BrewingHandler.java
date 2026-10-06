package ua.woody.questborn.listeners.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;

import java.lang.reflect.Method;
import java.util.*;

public class BrewingHandler extends AbstractQuestHandler {
    private static final class BrewedSnapshot {
        final Material container;
        final String baseKey;
        final boolean extended;
        final boolean upgraded;
        final int customEffectsCount;

        BrewedSnapshot(Material container, String baseKey, boolean extended, boolean upgraded, int customEffectsCount) {
            this.container = container;
            this.baseKey = baseKey != null ? baseKey : "";
            this.extended = extended;
            this.upgraded = upgraded;
            this.customEffectsCount = customEffectsCount;
        }

        boolean matches(ItemStack stack) {
            if (stack == null || stack.getType() != container)
                return false;

            SnapshotInfo info = snapshotInfo(stack);
            if (info == null)
                return false;

            if (this.baseKey.isEmpty() && (info.baseKey == null || info.baseKey.isEmpty())) {
                return info.customEffectsCount == this.customEffectsCount;
            }

            return normalize(info.baseKey).equals(normalize(this.baseKey))
                    && info.extended == this.extended
                    && info.upgraded == this.upgraded
                    && info.customEffectsCount == this.customEffectsCount;
        }
    }

    private static final class SnapshotInfo {
        final String baseKey;
        final boolean extended;
        final boolean upgraded;
        final int customEffectsCount;

        SnapshotInfo(String baseKey, boolean extended, boolean upgraded, int customEffectsCount) {
            this.baseKey = baseKey;
            this.extended = extended;
            this.upgraded = upgraded;
            this.customEffectsCount = customEffectsCount;
        }
    }

    private final Map<String, BrewedSnapshot> brewedPotions = new HashMap<>();

    private final Set<String> processedClicks = new HashSet<>();

    public BrewingHandler(QuestbornPlugin plugin) {
        super(plugin);
    }

    public void onBrewingFinish(BrewEvent event) {
        if (event.getBlock() == null)
            return;

        Location loc = event.getBlock().getLocation();
        if (loc.getWorld() == null)
            return;

        plugin.getFoliaLib().getImpl().runAtLocation(loc, __task -> snapshotStandNextTick(loc));
    }

    private void snapshotStandNextTick(Location loc) {
        BlockState state = loc.getBlock().getState();
        if (!(state instanceof BrewingStand stand))
            return;

        BrewerInventory inv = stand.getInventory();
        String baseLocKey = locKey(loc);

        for (int i = 0; i < 3; i++) {
            ItemStack potion = inv.getItem(i);
            if (potion == null || potion.getType() == Material.AIR)
                continue;
            if (!isBrewingStandPotionContainer(potion.getType()))
                continue;

            BrewedSnapshot snap = snapshot(potion);
            String key = baseLocKey + ":" + i;
            brewedPotions.put(key, snap);

            plugin.getFoliaLib().getImpl().runAtLocationLater(loc, () -> brewedPotions.remove(key), 20L * 60L * 5L);
        }
    }

    public void onBrewingInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (event.getInventory().getType() != InventoryType.BREWING)
            return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot > 2)
            return;

        Location loc = resolveBrewingLocation(event);
        if (loc == null || loc.getWorld() == null)
            return;

        String key = locKey(loc) + ":" + slot;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (!isBrewingStandPotionContainer(clicked.getType()))
            return;

        BrewedSnapshot snap = brewedPotions.get(key);
        if (snap == null)
            return;

        if (!snap.matches(clicked))
            return;

        String clickId = player.getUniqueId() + ":" + key + ":" + (System.currentTimeMillis() / 1000);
        if (processedClicks.contains(clickId))
            return;

        processedClicks.add(clickId);
        plugin.getFoliaLib().getImpl().runAtEntityLater(player, () -> processedClicks.remove(clickId), 20L);

        final ItemStack clickedClone = clicked.clone();
        plugin.getFoliaLib().getImpl().runAtEntity(player, __ -> {
            if (!player.isOnline()) return;
            ItemStack now = event.getInventory().getItem(slot);
            if (snap.matches(now)) {
                return;
            }

            brewedPotions.remove(key);
            checkBrewingObjective(player, clickedClone);
        });
    }

    private Location resolveBrewingLocation(InventoryClickEvent event) {
        try {
            Location loc = event.getInventory().getLocation();
            if (loc != null)
                return loc;
        } catch (Throwable ignored) {
        }

        try {
            InventoryHolder holder = event.getInventory().getHolder();
            if (holder instanceof BrewingStand bs)
                return bs.getLocation();
        } catch (Throwable ignored) {
        }

        try {
            if (event.getInventory() instanceof BrewerInventory bi)
                return bi.getLocation();
        } catch (Throwable ignored) {
        }

        return null;
    }

    private boolean isBrewingStandPotionContainer(Material material) {
        return material == Material.POTION
                || material == Material.SPLASH_POTION
                || material == Material.LINGERING_POTION;
    }

    private static String locKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private BrewedSnapshot snapshot(ItemStack stack) {
        SnapshotInfo info = snapshotInfo(stack);

        String baseKey = "";
        boolean ext = false;
        boolean upg = false;
        int custom = 0;

        if (info != null) {
            baseKey = info.baseKey != null ? info.baseKey : "";
            ext = info.extended;
            upg = info.upgraded;
            custom = info.customEffectsCount;
        }

        return new BrewedSnapshot(stack.getType(), baseKey, ext, upg, custom);
    }

    private static SnapshotInfo snapshotInfo(ItemStack stack) {
        if (stack == null)
            return null;

        int custom = 0;
        String base = "";
        boolean ext = false;
        boolean upg = false;

        if (stack.getItemMeta() instanceof PotionMeta meta) {
            custom = meta.getCustomEffects().size();

            try {
                PotionType t = meta.getBasePotionData().getType();
                if (t != null)
                    base = t.name();
            } catch (Throwable ignored) {
            }

            try {
                Method mGetData = meta.getClass().getMethod("getBasePotionData");
                Object data = mGetData.invoke(meta);
                if (data != null) {
                    Method mGetType = data.getClass().getMethod("getType");
                    Object typeObj = mGetType.invoke(data);
                    if (typeObj instanceof Enum<?> en) {
                        String n = en.name();
                        if (base.isEmpty())
                            base = n;
                    } else if (typeObj != null && base.isEmpty()) {
                        base = String.valueOf(typeObj);
                    }

                    Method mExt = data.getClass().getMethod("isExtended");
                    Method mUpg = data.getClass().getMethod("isUpgraded");
                    Object oExt = mExt.invoke(data);
                    Object oUpg = mUpg.invoke(data);
                    if (oExt instanceof Boolean b)
                        ext = b;
                    if (oUpg instanceof Boolean b)
                        upg = b;
                }
            } catch (Throwable ignored) {
            }

            String norm = normalize(base);
            if (norm.startsWith("STRONG_")) {
                upg = true;
                base = norm.substring("STRONG_".length());
            } else if (norm.startsWith("LONG_")) {
                ext = true;
                base = norm.substring("LONG_".length());
            }
        }

        return new SnapshotInfo(base, ext, upg, custom);
    }

    private static String normalize(String s) {
        if (s == null)
            return "";
        return s.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private void checkBrewingObjective(Player player, ItemStack potion) {
        QuestDefinition q = getActiveQuest(player);
        if (q == null)
            return;

        QuestObjective o = getCurrentObjective(player, q);
        if (o == null)
            return;

        if (o.getType() != QuestObjectiveType.BREWING)
            return;

        if (o.isTargetItem(potion)) {
            progress(player, q, 1);
        }
    }
}
