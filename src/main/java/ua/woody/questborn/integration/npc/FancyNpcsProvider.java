package ua.woody.questborn.integration.npc;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import de.oliver.fancynpcs.api.actions.ActionTrigger;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ua.woody.questborn.QuestbornPlugin;

import java.util.Collection;

public class FancyNpcsProvider implements NpcProvider, Listener {
    private final QuestbornPlugin plugin;

    public FancyNpcsProvider(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isNpc(Entity entity) {
        return false;
    }

    @Override
    public String getId(Entity entity) {
        if (entity == null) return null;
        for (Npc npc : FancyNpcsPlugin.get().getNpcManager().getAllNpcs()) {
            if (npc.getEntityId() == entity.getEntityId()) {
                return npc.getData().getName();
            }
        }
        return null;
    }

    @Override
    public String getName(Entity entity) {
        return "NPC";
    }

    @Override
    public String getName(String id) {
        Npc npc = findNpcById(id);
        if (npc != null && npc.getData().getDisplayName() != null) {
            String dn = org.bukkit.ChatColor.stripColor(npc.getData().getDisplayName().replace("&", "§"));
            return dn.equals("<empty>") ? id : dn;
        }
        return id;
    }

    @Override
    public Location getStoredLocation(String id) {
        Npc npc = findNpcById(id);
        return (npc != null) ? npc.getData().getLocation() : null;
    }

    @Override
    public org.bukkit.entity.EntityType getType(String id) {
        Npc npc = findNpcById(id);
        return (npc != null) ? npc.getData().getType() : org.bukkit.entity.EntityType.PLAYER;
    }

    @Override
    public double getHeight(String id) {
        Npc npc = findNpcById(id);
        if (npc == null || npc.getData() == null)
            return 2.0;

        double baseHeight = 2.0;

        org.bukkit.entity.EntityType type = npc.getData().getType();
        if (type != null) {
            switch (type.name()) {
                case "AXOLOTL", "CHICKEN", "CAT", "OCELOT", "VILLAGER_BABY", "WOLF", "FOX", "BEE", "PARROT", "RABBIT",
                        "SILVERFISH", "TURTLE", "ITEM_DISPLAY", "BLOCK_DISPLAY", "INTERACTION", "SLIME", "MAGMA_CUBE", "FROG", "PUFFERFISH" ->
                    baseHeight = 0.5;
                case "SPIDER", "CAVE_SPIDER", "PIG", "SHEEP", "COW", "MUSHROOM_COW", "POLAR_BEAR", "PANDA", "HOGLIN", "ZOGLIN" -> baseHeight = 1.0;
                case "ARMOR_STAND", "PLAYER", "ZOMBIE", "SKELETON", "VILLAGER", "PIGLIN" -> baseHeight = 1.8;
                case "ENDERMAN" -> baseHeight = 2.9;
                case "GHAST" -> baseHeight = 4.0;
                case "GIANT" -> baseHeight = 12.0;

                default -> baseHeight = 1.8;
            }
        }

        float scale = npc.getData().getScale();
        if (scale > 0) {
            baseHeight *= scale;
        }

        return baseHeight;
    }

    @Override
    public String getSkinTextureBase64(String id) {
        Npc npc = findNpcById(id);
        if (npc != null && npc.getData() != null) {
            de.oliver.fancynpcs.api.skins.SkinData skinData = npc.getData().getSkinData();
            if (skinData != null) {
                return skinData.getTextureValue();
            }
        }
        return null;
    }

    @Override
    public String getTargetNpcId(org.bukkit.entity.Player player, int range) {
        Location eyeLoc = player.getEyeLocation();
        org.bukkit.util.Vector dir = eyeLoc.getDirection();

        String bestId = null;
        double closest = Double.MAX_VALUE;

        for (Npc npc : FancyNpcsPlugin.get().getNpcManager().getAllNpcs()) {
            Location loc = npc.getData().getLocation();
            if (loc == null || loc.getWorld() != eyeLoc.getWorld()) continue;

            double height = getHeight(npc.getData().getName());
            org.bukkit.util.BoundingBox box = org.bukkit.util.BoundingBox.of(
                loc.clone().add(-0.5, 0, -0.5),
                loc.clone().add(0.5, height, 0.5)
            );

            org.bukkit.util.RayTraceResult hit = box.rayTrace(eyeLoc.toVector(), dir, range);
            if (hit != null) {
                double dist = hit.getHitPosition().distanceSquared(eyeLoc.toVector());
                if (dist < closest) {
                    closest = dist;
                    bestId = npc.getData().getName();
                }
            }
        }

        return bestId;
    }

    @Override
    public Entity getEntity(String id) {
        return null;
    }

    @Override
    public Collection<String> getAllNpcIds() {
        Collection<Npc> npcs = FancyNpcsPlugin.get().getNpcManager().getAllNpcs();
        java.util.List<String> ids = new java.util.ArrayList<>(npcs.size());
        for (Npc npc : npcs) {
            ids.add(npc.getData().getName());
        }
        return ids;
    }

    @Override
    public String createNpc(String name, Location location) {
        String id = name.replace(" ", "_");
        try {
            Class<?> npcDataClass = Class.forName("de.oliver.fancynpcs.api.NpcData");
            Object npcData = npcDataClass.getConstructor(String.class, java.util.UUID.class, Location.class).newInstance(id, java.util.UUID.randomUUID(), location);

            Object fancyPlugin = FancyNpcsPlugin.get();
            Object npcManager = fancyPlugin.getClass().getMethod("getNpcManager").invoke(fancyPlugin);

            Object adapter = fancyPlugin.getClass().getMethod("getNpcAdapter").invoke(fancyPlugin);
            @SuppressWarnings("unchecked")
            java.util.function.Function<Object, Object> function = (java.util.function.Function<Object, Object>) adapter;
            Object npc = function.apply(npcData);

            Class<?> npcClass = Class.forName("de.oliver.fancynpcs.api.Npc");

            npcManager.getClass().getMethod("registerNpc", npcClass).invoke(npcManager, npc);

            npc.getClass().getMethod("create").invoke(npc);
            npc.getClass().getMethod("spawnForAll").invoke(npc);

            return id;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create FancyNpcs NPC via API: " + e.getMessage());
            e.printStackTrace();

            ua.woody.questborn.utils.SchedulerUtils.runTaskGlobal(plugin, () -> {
                org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "fancynpcs create " + id);
            });
            return id;
        }
    }

    @Override
    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void shutdown() {
        NpcInteractEvent.getHandlerList().unregister(this);
        org.bukkit.event.HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onNpcInteract(NpcInteractEvent event) {
        String button = plugin.getConfig().getString("integration.npc.interaction-button", plugin.getConfig().getString("integration.interaction-button", "RIGHT"));

        ActionTrigger trigger = event.getInteractionType();
        boolean isLeft = trigger == ActionTrigger.LEFT_CLICK;
        boolean isRight = trigger == ActionTrigger.RIGHT_CLICK;

        if ("LEFT".equalsIgnoreCase(button) && !isLeft)
            return;
        if ("RIGHT".equalsIgnoreCase(button) && !isRight)
            return;

        String id = event.getNpc().getData().getName();
        plugin.getNpcIntegrationManager().handleNpcClick(event.getPlayer(), id, null);
    }

    private Npc findNpcById(String id) {
        return FancyNpcsPlugin.get().getNpcManager().getNpc(id);
    }
}
