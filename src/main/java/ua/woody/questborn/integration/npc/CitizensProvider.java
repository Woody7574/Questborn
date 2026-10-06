package ua.woody.questborn.integration.npc;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ua.woody.questborn.QuestbornPlugin;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CitizensProvider implements NpcProvider, Listener {
    private final QuestbornPlugin plugin;

    public CitizensProvider(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isNpc(Entity entity) {
        return CitizensAPI.getNPCRegistry().isNPC(entity);
    }

    @Override
    public String getId(Entity entity) {
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
        return (npc != null) ? String.valueOf(npc.getId()) : null;
    }

    @Override
    public String getName(Entity entity) {
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
        return (npc != null) ? npc.getName() : (entity != null ? entity.getName() : "NPC");
    }

    @Override
    public String getName(String id) {
        try {
            int intId = Integer.parseInt(id);
            NPC npc = CitizensAPI.getNPCRegistry().getById(intId);
            return (npc != null) ? npc.getName() : "NPC";
        } catch (NumberFormatException e) {
            return "NPC";
        }
    }

    @Override
    public Location getStoredLocation(String id) {
        try {
            int intId = Integer.parseInt(id);
            NPC npc = CitizensAPI.getNPCRegistry().getById(intId);
            return (npc != null) ? npc.getStoredLocation() : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Entity getEntity(String id) {
        try {
            int intId = Integer.parseInt(id);
            NPC npc = CitizensAPI.getNPCRegistry().getById(intId);
            return (npc != null) ? npc.getEntity() : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public org.bukkit.entity.EntityType getType(String id) {
        try {
            int intId = Integer.parseInt(id);
            NPC npc = CitizensAPI.getNPCRegistry().getById(intId);
            if (npc != null) {
                if (npc.getEntity() != null) {
                    return npc.getEntity().getType();
                }
                if (npc.hasTrait(net.citizensnpcs.api.trait.trait.MobType.class)) {
                    net.citizensnpcs.api.trait.trait.MobType mobType = npc.getTrait(net.citizensnpcs.api.trait.trait.MobType.class);
                    if (mobType != null && mobType.getType() != null) {
                        return mobType.getType();
                    }
                }
            }
            return org.bukkit.entity.EntityType.PLAYER;
        } catch (NumberFormatException e) {
            return org.bukkit.entity.EntityType.PLAYER;
        }
    }

    @Override
    public double getHeight(String id) {
        try {
            int intId = Integer.parseInt(id);
            NPC npc = CitizensAPI.getNPCRegistry().getById(intId);
            if (npc != null && npc.getEntity() != null) {
                return npc.getEntity().getHeight();
            }

            org.bukkit.entity.EntityType type = getType(id);

            return 2.0;

        } catch (NumberFormatException e) {
            return 2.0;
        }
    }

    @Override
    public String getSkinTextureBase64(String id) {
        try {
            int intId = Integer.parseInt(id);
            NPC npc = CitizensAPI.getNPCRegistry().getById(intId);
            if (npc != null) {
                if (npc.data().has(NPC.PLAYER_SKIN_TEXTURE_PROPERTIES_METADATA)) {
                    return npc.data().get(NPC.PLAYER_SKIN_TEXTURE_PROPERTIES_METADATA);
                }
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    @Override
    public Collection<String> getAllNpcIds() {
        List<NPC> list = new java.util.ArrayList<>();
        CitizensAPI.getNPCRegistry().iterator().forEachRemaining(list::add);
        return list.stream().map(npc -> String.valueOf(npc.getId())).collect(Collectors.toList());
    }

    @Override
    public String createNpc(String name, Location location) {
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(org.bukkit.entity.EntityType.PLAYER, name);
        npc.spawn(location);
        return String.valueOf(npc.getId());
    }

    @Override
    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void shutdown() {
        NPCRightClickEvent.getHandlerList().unregister(this);
        NPCLeftClickEvent.getHandlerList().unregister(this);
        org.bukkit.event.HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {
        String button = plugin.getConfig().getString("integration.npc.interaction-button", plugin.getConfig().getString("integration.interaction-button", "RIGHT"));
        if ("LEFT".equalsIgnoreCase(button))
            return;

        plugin.getNpcIntegrationManager().handleNpcClick(event.getClicker(), String.valueOf(event.getNPC().getId()),
                event.getNPC().getEntity());
    }

    @EventHandler
    public void onNpcLeftClick(NPCLeftClickEvent event) {
        String button = plugin.getConfig().getString("integration.npc.interaction-button", plugin.getConfig().getString("integration.interaction-button", "RIGHT"));
        if (!"LEFT".equalsIgnoreCase(button) && !"ANY".equalsIgnoreCase(button))
            return;

        plugin.getNpcIntegrationManager().handleNpcClick(event.getClicker(), String.valueOf(event.getNPC().getId()),
                event.getNPC().getEntity());
    }
}
