package ua.woody.questborn.model;

import java.util.*;

public enum QuestObjectiveType {
    BLOCK_BREAK,
    BLOCK_PLACE,
    ITEM_CRAFT,
    ITEM_SMELT,
    ITEM_ENCHANT,
    ITEM_FISH,
    ITEM_COOK,
    BREWING,
    CONSUME_ITEM,
    KILL_ENTITY,
    DEAL_DAMAGE,
    TAKE_DAMAGE,
    TRAVEL_DISTANCE,
    REACH_LOCATION,
    REACH_BIOME,
    ENTER_REGION,
    LEAVE_REGION,
    INTERACT_BLOCK,
    INTERACT_ENTITY,
    NPC_INTERACT,
    GIVE_ITEM_TO_NPC,
    USE_ITEM,
    EXECUTE_COMMAND,
    CHAT_MESSAGE,
    LEVEL_UP_REACH,
    LEVEL_UP_GAIN,

    FILL_BUCKET,
    EMPTY_BUCKET,
    TILL_SOIL,
    PLANT_SEED,
    HARVEST_CROP,
    BONE_MEAL_USE,
    STRIP_LOG,
    WAX_OFF,
    WAX_ON,

    ITEM_REPAIR,
    ITEM_RENAME,
    ITEM_BREAK,
    DYE_ITEM,
    FILL_FUEL,
    BURN_FUEL,
    VILLAGER_SELL_ITEM,
    VILLAGER_BUY_ITEM,
    ENCHANT_TABLE_USE,
    ANVIL_USE,

    TAME_ANIMAL,
    BREED_ANIMALS,
    MILK_COW,
    SHEAR_SHEEP,
    ENTITY_RIDE,
    THROW_EGG,
    EXPERIENCE_ORB_PICKUP,

    ENTER_BED,
    CHANGE_DIMENSION,
    FALL_DISTANCE,
    BOAT_TRAVEL,
    MINECART_TRAVEL,
    ELYTRA_FLY,
    JUMP,
    CROUCH,
    SPRINT_DISTANCE,
    CROUCH_DISTANCE,
    SWIM_DISTANCE,

    POTION_SPLASH,
    POTION_DRINK,
    BEACON_ACTIVATE,
    CONDUIT_ACTIVATE,

    PLAYER_KILL,
    ASSIST_KILL,
    JOIN_SERVER,
    PLAY_TIME,

    SLEEP_IN_BED,
    WEAR_ARMOR,
    HOLD_ITEM,
    DROP_ITEM,
    OPEN_CONTAINER,
    SIGN_EDIT,
    BOOK_EDIT,
    RECEIVE_DAMAGE_TYPE;

    private static final Map<String, QuestObjectiveType> LOOKUP = new HashMap<>();
    private static final EnumMap<QuestObjectiveType, String> CANONICAL = new EnumMap<>(QuestObjectiveType.class);
    private static final LinkedHashSet<String> CANONICAL_KEYS = new LinkedHashSet<>();

    static {
        for (QuestObjectiveType t : values()) {
            String auto = t.name().toLowerCase(Locale.ROOT).replace('_', '-');
            setCanonical(t, auto);
            putAlias(auto, t);
            putAlias(t.name(), t);
            putAlias(t.name().toLowerCase(Locale.ROOT), t);
        }

        setCanonical(BLOCK_BREAK, "break-blocks",
                "mine-blocks", "break-block", "mine-block", "block-break", "break_blocks", "mine_blocks");
        setCanonical(BLOCK_PLACE, "place-blocks",
                "place-block", "block-place", "place_blocks");

        setCanonical(ITEM_CRAFT, "craft-items",
                "craft-item", "craft_item", "craft_items");
        setCanonical(ITEM_SMELT, "smelt-items",
                "smelt-item", "smelt_item", "smelt_items");
        setCanonical(ITEM_COOK, "cook-items",
                "cook-item", "cook_item", "cook_items");

        setCanonical(ITEM_ENCHANT, "enchant-item",
                "enchant-items", "enchant_table_use", "enchant-table-use");

        putAlias("ENCHANT_TABLE_USE", ITEM_ENCHANT);

        setCanonical(ITEM_FISH, "fish-catch",
                "fish-catches", "fish");

        setCanonical(KILL_ENTITY, "kill-mobs",
                "kill-entities", "kill-entity", "kill-mob", "kill_mobs",

                "kill-entity-type", "kill_entity_type", "KILL_ENTITY_TYPE");

        setCanonical(PLAYER_KILL, "kill-players",
                "kill-player", "player-kill");

        setCanonical(CONSUME_ITEM, "consume-items",
                "consume-item", "eat-items", "eat-item", "drink-items", "drink-item");

        setCanonical(USE_ITEM, "use-item", "use-items");

        setCanonical(TRAVEL_DISTANCE, "travel-distance", "walk-distance");
        setCanonical(SPRINT_DISTANCE, "sprint-distance");
        setCanonical(CROUCH_DISTANCE, "crouch-distance");
        setCanonical(SWIM_DISTANCE, "swim-distance");

        setCanonical(OPEN_CONTAINER, "open-container", "open-chest");

        setCanonical(FILL_BUCKET, "fill-bucket",
                "bucket-fill", "bucket_fill", "BUCKET_FILL");

        setCanonical(TAME_ANIMAL, "tame-animal",
                "tame-entity", "tame_entity", "TAME_ENTITY");

        setCanonical(INTERACT_ENTITY, "interact-entity",
                "entity-interact", "entity_interact", "ENTITY_INTERACT");

        setCanonical(NPC_INTERACT, "npc-interact", "talk-to-npc");
        setCanonical(GIVE_ITEM_TO_NPC, "give-item-to-npc", "deliver-to-npc");

        setCanonical(ENTITY_RIDE, "entity-ride",
                "riding", "ride", "RIDING");

        setCanonical(VILLAGER_SELL_ITEM, "villager-sell-item",
                "sell-to-villager", "trade-with-villager", "trade_with_villager", "TRADE_WITH_VILLAGER");

        setCanonical(VILLAGER_BUY_ITEM, "villager-buy-item",
                "buy-from-villager");

        setCanonical(FILL_FUEL, "fill-fuel");
        setCanonical(BURN_FUEL, "burn-fuel", "consume-fuel", "furnace-burn");

        setCanonical(REACH_BIOME, "reach-biome", "visit-biome", "biome");
    }

    private static void setCanonical(QuestObjectiveType t, String canonicalKey, String... aliases) {
        String canon = canonicalKey;
        CANONICAL.put(t, canon);
        CANONICAL_KEYS.add(canon);
        putAlias(canon, t);

        if (aliases != null) {
            for (String a : aliases) putAlias(a, t);
        }
    }

    private static void putAlias(String raw, QuestObjectiveType t) {
        if (raw == null || raw.isBlank()) return;
        LOOKUP.put(norm(raw), t);
    }

    private static String norm(String raw) {
        return raw.trim()
                .toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replace(' ', '-')
                .replaceAll("-{2,}", "-");
    }

    public String canonicalKey() {
        String k = CANONICAL.get(this);
        return k != null ? k : name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public static QuestObjectiveType fromStringStrict(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return LOOKUP.get(norm(raw));
    }

    public static QuestObjectiveType fromStringOrDefault(String raw, QuestObjectiveType def) {
        QuestObjectiveType t = fromStringStrict(raw);
        return t != null ? t : def;
    }

    public static List<String> canonicalKeys() {
        return new ArrayList<>(CANONICAL_KEYS);
    }
}
