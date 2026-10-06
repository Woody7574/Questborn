package ua.woody.questborn.listeners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.listeners.handlers.*;
import ua.woody.questborn.model.PlayerQuestProgress;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;
import ua.woody.questborn.storage.PlayerDataStore;

public class QuestProgressListener {
    private final QuestbornPlugin plugin;
    private final BlockHandler blockHandler;
    private final EntityHandler entityHandler;
    private final ItemHandler itemHandler;
    private final TravelHandler travelHandler;
    private final InteractionHandler interactionHandler;
    private final BrewingHandler brewingHandler;
    private final LevelAndExperienceHandler levelAndExperienceHandler;
    private final WorldInteractionHandler worldInteractionHandler;
    private final AnvilHandler anvilHandler;
    private final RepairHandler repairHandler;
    private final DyeHandler dyeHandler;
    private final FuelHandler fuelHandler;
    private final TradeHandler tradeHandler;
    private final EnchantmentHandler enchantmentHandler;
    private final ItemBreakHandler itemBreakHandler;
    private final AnimalHandler animalHandler;
    private final FishingHandler fishingHandler;
    private final WorldHandler worldHandler;
    private final MovementHandler movementHandler;
    private final MagicHandler magicHandler;
    private final ItemInteractionHandler itemInteractionHandler;
    private final RegionHandler regionHandler;

    private final Map<UUID, Long> lastMoveTime = new ConcurrentHashMap<>();
    private long moveCooldownMs = 50L;
    private double minMoveDistanceSquared = 0.04;
    private final Map<UUID, PlayerQuestCache> activeObjectivesCache = new ConcurrentHashMap<>();
    private final Set<GameMode> allowedGameModes = EnumSet.noneOf(GameMode.class);

    public QuestProgressListener(QuestbornPlugin plugin) {
        this.plugin = plugin;
        this.blockHandler = new BlockHandler(plugin);
        this.entityHandler = new EntityHandler(plugin);
        this.itemHandler = new ItemHandler(plugin);
        this.travelHandler = new TravelHandler(plugin);
        this.interactionHandler = new InteractionHandler(plugin);
        this.brewingHandler = new BrewingHandler(plugin);
        this.levelAndExperienceHandler = new LevelAndExperienceHandler(plugin);
        this.worldInteractionHandler = new WorldInteractionHandler(plugin);
        this.anvilHandler = new AnvilHandler(plugin);
        this.repairHandler = new RepairHandler(plugin);
        this.dyeHandler = new DyeHandler(plugin);
        this.fuelHandler = new FuelHandler(plugin);
        this.tradeHandler = new TradeHandler(plugin);
        this.enchantmentHandler = new EnchantmentHandler(plugin);
        this.itemBreakHandler = new ItemBreakHandler(plugin);
        this.animalHandler = new AnimalHandler(plugin);
        this.fishingHandler = new FishingHandler(plugin);
        this.worldHandler = new WorldHandler(plugin);
        this.movementHandler = new MovementHandler(plugin);
        this.magicHandler = new MagicHandler(plugin);
        this.itemInteractionHandler = new ItemInteractionHandler(plugin);
        this.regionHandler = new RegionHandler(plugin);
        this.loadAllowedGameModes();
        this.loadOptimizationConfig();
    }

    public void loadAllowedGameModes() {
        this.allowedGameModes.clear();
        List<String> modes = this.plugin.getConfig().getStringList("gameplay.allowed-gamemodes");
        if (modes == null || modes.isEmpty()) {
            this.allowedGameModes.add(GameMode.SURVIVAL);
            this.allowedGameModes.add(GameMode.ADVENTURE);
        } else {
            for (String modeName : modes) {
                try {
                    this.allowedGameModes.add(GameMode.valueOf(modeName.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    this.plugin.getLogger().warning("Invalid game mode in config: " + modeName);
                }
            }
        }
    }

    public void loadOptimizationConfig() {
        this.moveCooldownMs = this.plugin.getConfig().getLong("optimization.move-handling.cooldown-ms", 50L);
        double dist = this.plugin.getConfig().getDouble("optimization.move-handling.min-move-distance", 0.2);
        this.minMoveDistanceSquared = dist * dist;
    }

    public void updatePlayerCache(Player player) {
        if (player == null) return;

        List<ActiveContext> ctxs = this.getContexts(player);
        Set<QuestObjectiveType> types = new HashSet<>();
        for (ActiveContext ctx : ctxs) {
            if (this.isObjectiveStage(ctx)) {
                types.add(ctx.objective().getType());
            }
        }
        if (types.isEmpty()) {
            this.activeObjectivesCache.remove(player.getUniqueId());
        } else {
            this.activeObjectivesCache.put(player.getUniqueId(), new PlayerQuestCache(types, ctxs));
        }
    }

    public void clearPlayerCache(UUID uuid) {
        if (uuid != null) {
            this.activeObjectivesCache.remove(uuid);
        }
    }

    public void reload() {
        this.loadAllowedGameModes();
        this.loadOptimizationConfig();
    }

    public boolean isValidGamemode(Player player) {
        if (player == null) return false;
        return this.allowedGameModes.contains(player.getGameMode());
    }

    public void executeForActiveObjectives(Player player, Consumer<ActiveContext> action) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        PlayerQuestCache cache = this.activeObjectivesCache.get(uuid);
        if (cache == null || cache.types().isEmpty()) return;

        for (ActiveContext ctx : cache.contexts()) {
            if (ctx == null || !isObjectiveStage(ctx)) continue;
            AbstractQuestHandler.FORCE_ACTIVE_QUEST.set(ctx.quest().getId());
            try {
                action.accept(ctx);
            } finally {
                AbstractQuestHandler.FORCE_ACTIVE_QUEST.remove();
            }
        }
    }

    private List<QuestDefinition> getActiveQuests(Player player) {
        if (player == null) return Collections.emptyList();
        PlayerDataStore store = this.plugin.getPlayerDataStore();
        if (store == null) return Collections.emptyList();
        PlayerQuestProgress data = store.get(player.getUniqueId());
        if (data == null) return Collections.emptyList();

        String mode = this.plugin.getConfig().getString("gameplay.quest-tracking-mode", "STRICT_FOCUS");
        List<QuestDefinition> quests = new ArrayList<>();

        if ("STRICT_FOCUS".equalsIgnoreCase(mode)) {
            String activeId = data.getTrackedQuestId();
            if (activeId != null && !activeId.isEmpty()) {
                QuestDefinition q = this.plugin.getQuestManager().getById(activeId);
                if (q != null) quests.add(q);
            }
        } else {
            for (String qid : data.getActiveQuests().keySet()) {
                QuestDefinition q = this.plugin.getQuestManager().getById(qid);
                if (q != null) quests.add(q);
            }
        }
        return quests;
    }

    private List<ActiveContext> getContexts(Player player) {
        if (!this.isValidGamemode(player)) {
            return Collections.emptyList();
        }
        List<ActiveContext> list = new ArrayList<>();
        for (QuestDefinition quest : this.getActiveQuests(player)) {
            QuestObjective objective = this.plugin.getQuestManager().resolveObjective(player, quest);
            if (objective != null) {
                list.add(new ActiveContext(player, quest, objective));
            }
        }
        return list;
    }

    private boolean isObjectiveStage(ActiveContext ctx) {
        return ctx != null && ctx.objective() != null;
    }

    public QuestbornPlugin getPlugin() { return plugin; }
    public BlockHandler getBlockHandler() { return blockHandler; }
    public EntityHandler getEntityHandler() { return entityHandler; }
    public ItemHandler getItemHandler() { return itemHandler; }
    public TravelHandler getTravelHandler() { return travelHandler; }
    public InteractionHandler getInteractionHandler() { return interactionHandler; }
    public BrewingHandler getBrewingHandler() { return brewingHandler; }
    public LevelAndExperienceHandler getLevelAndExperienceHandler() { return levelAndExperienceHandler; }
    public WorldInteractionHandler getWorldInteractionHandler() { return worldInteractionHandler; }
    public AnvilHandler getAnvilHandler() { return anvilHandler; }
    public RepairHandler getRepairHandler() { return repairHandler; }
    public DyeHandler getDyeHandler() { return dyeHandler; }
    public FuelHandler getFuelHandler() { return fuelHandler; }
    public TradeHandler getTradeHandler() { return tradeHandler; }
    public EnchantmentHandler getEnchantmentHandler() { return enchantmentHandler; }
    public ItemBreakHandler getItemBreakHandler() { return itemBreakHandler; }
    public AnimalHandler getAnimalHandler() { return animalHandler; }
    public FishingHandler getFishingHandler() { return fishingHandler; }
    public WorldHandler getWorldHandler() { return worldHandler; }
    public MovementHandler getMovementHandler() { return movementHandler; }
    public MagicHandler getMagicHandler() { return magicHandler; }
    public ItemInteractionHandler getItemInteractionHandler() { return itemInteractionHandler; }
    public RegionHandler getRegionHandler() { return regionHandler; }
    public Map<UUID, Long> getLastMoveTime() { return lastMoveTime; }
    public long getMoveCooldownMs() { return moveCooldownMs; }
    public double getMinMoveDistanceSquared() { return minMoveDistanceSquared; }

    public record PlayerQuestCache(Set<QuestObjectiveType> types, List<ActiveContext> contexts) {}
    public record ActiveContext(Player player, QuestDefinition quest, QuestObjective objective) {}
}
