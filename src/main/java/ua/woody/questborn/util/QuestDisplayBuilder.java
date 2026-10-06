package ua.woody.questborn.util;

import ua.woody.questborn.lang.LanguageManager;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.model.QuestObjectiveType;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.*;

public class QuestDisplayBuilder {
    private static String F_BLOCKS = "<#55ff55> • {value}";
    private static String F_ITEMS = "<#55ccee> • {value}";
    private static String F_ENTITIES = "<#ff5555> • {value}";
    private static String F_LOCATION = "<#ffaa00> • {value}";
    private static String F_FLUIDS = "<#5599ff> • {value}";
    private static String F_CAUSE = "<#ff5555> • {value}";
    private static String F_TIME = "<#aa55ff> • {value}";
    private static String F_DEFAULT = "<#55ff55> ▪ {value}";

    private static ua.woody.questborn.QuestbornPlugin plugin;

    public static void setPlugin(ua.woody.questborn.QuestbornPlugin p) {
        plugin = p;
    }

    public static void loadFormat(org.bukkit.configuration.file.FileConfiguration config) {
        F_BLOCKS = config.getString("gui.details.lore-format.blocks", F_BLOCKS);
        F_ITEMS = config.getString("gui.details.lore-format.items", F_ITEMS);
        F_ENTITIES = config.getString("gui.details.lore-format.entities", F_ENTITIES);
        F_LOCATION = config.getString("gui.details.lore-format.location", F_LOCATION);
        F_FLUIDS = config.getString("gui.details.lore-format.fluids", F_FLUIDS);
        F_CAUSE = config.getString("gui.details.lore-format.cause", F_CAUSE);
        F_TIME = config.getString("gui.details.lore-format.time", F_TIME);
        F_DEFAULT = config.getString("gui.details.lore-format.default", F_DEFAULT);
    }

    public static List<String> buildQuestDescription(QuestDefinition quest,
            boolean objectiveCompleted,
            boolean itemsTransferred,
            LanguageManager lang) {
        List<String> lore = new ArrayList<>();

        List<String> description = quest.getDescription();
        if (!description.isEmpty()) {
            description.forEach(s -> lore.add(lang.color(s)));
        }
        List<String> objectiveLore = build(quest.getObjective(), lang);
        if (!objectiveLore.isEmpty()) {
            if (!lore.isEmpty())
                lore.add(" ");
            lore.addAll(objectiveLore);
        }

        if (quest.hasRequiredMaterials()) {
            List<String> requiredLore = buildRequiredMaterials(quest, objectiveCompleted, itemsTransferred, lang);
            if (!requiredLore.isEmpty()) {
                if (!lore.isEmpty())
                    lore.add(" ");
                lore.addAll(requiredLore);
            }
        }

        return lore;
    }

    public static List<String> buildRequiredMaterials(QuestDefinition quest,
            boolean objectiveCompleted,
            boolean itemsTransferred,
            LanguageManager lang) {
        List<String> lore = new ArrayList<>();

        if (!quest.hasRequiredMaterials()) {
            return lore;
        }

        lore.add(lang.color(lang.tr("gui.quest_details.info.required-items")));

        for (Map.Entry<ua.woody.questborn.model.QuestItem, Integer> entry : quest.getRequiredItems().entrySet()) {
            ua.woody.questborn.model.QuestItem qItem = entry.getKey();
            int amount = entry.getValue();
            if ("global".equalsIgnoreCase(quest.getTypeId()) && quest.getPersonalLimit() > 0) {
                amount = quest.getPersonalLimit();
            }
            String localizedName;
            if (qItem.isItemsAdderItem()) {
                localizedName = ItemDisplayUtil.getItemsAdderDisplayName(qItem.getItemsAdderId(), lang);
            } else if (qItem.isCraftEngineItem()) {
                localizedName = ItemDisplayUtil.getCraftEngineDisplayName(qItem.getCraftEngineId(), lang);
            } else {
                localizedName = ItemDisplayUtil.findLocalization(qItem.getMaterial(), lang, false);
            }
            lore.add(lang.color(" &7• &f" + localizedName + " &7x&f" + amount));
        }

        if (itemsTransferred || objectiveCompleted) {
            lore.add(" ");
            if (itemsTransferred) {
                lore.add(lang.tr("gui.quest_details.info.items-transferred"));
            } else if (objectiveCompleted) {
                lore.add(lang.tr("gui.quest_details.info.awaiting-transfer"));
            }
        }

        return lore;
    }

    public static List<String> buildTransferRequirements(QuestDefinition quest,
            LanguageManager lang) {
        List<String> lore = new ArrayList<>();

        if (!quest.hasRequiredMaterials()) {
            return lore;
        }

        lore.add(lang.color(lang.tr("gui.quest_details.required_items")));

        for (Map.Entry<ua.woody.questborn.model.QuestItem, Integer> entry : quest.getRequiredItems().entrySet()) {
            ua.woody.questborn.model.QuestItem qItem = entry.getKey();
            int amount = entry.getValue();
            if ("global".equalsIgnoreCase(quest.getTypeId()) && quest.getPersonalLimit() > 0) {
                amount = quest.getPersonalLimit();
            }
            String localizedName;
            if (qItem.isItemsAdderItem()) {
                localizedName = ItemDisplayUtil.getItemsAdderDisplayName(qItem.getItemsAdderId(), lang);
            } else if (qItem.isCraftEngineItem()) {
                localizedName = ItemDisplayUtil.getCraftEngineDisplayName(qItem.getCraftEngineId(), lang);
            } else {
                localizedName = ItemDisplayUtil.findLocalization(qItem.getMaterial(), lang, false);
            }
            lore.add(lang.color(" &7• &f" + localizedName + " &7x&f" + amount));
        }

        return lore;
    }

    public static List<String> build(QuestObjective obj, LanguageManager lang) {
        List<String> lore = new ArrayList<>();

        lore.add(lang.tr("quests.objectives.header"));

        switch (obj.getType()) {
            case BLOCK_BREAK -> {
                lore.add(lang.color(lang.tr("quests.objectives.block_break")));
                addBlocks(obj, lore, lang);
            }
            case BLOCK_PLACE -> {
                lore.add(lang.color(lang.tr("quests.objectives.block_place")));
                addBlocks(obj, lore, lang);
            }
            case ITEM_CRAFT -> {
                lore.add(lang.color(lang.tr("quests.objectives.item_craft")));
                addTargetItems(obj, lore, lang);
            }
            case ITEM_SMELT -> {
                lore.add(lang.color(lang.tr("quests.objectives.item_smelt")));
                addTargetItems(obj, lore, lang);
            }
            case FILL_FUEL -> {
                lore.add(lang.color(lang.tr("quests.objectives.fill-fuel")));
                if (obj.getItem() != null && !obj.getItem().isEmpty() ||
                        (obj.getTargetItems() != null && !obj.getTargetItems().isEmpty())) {
                    addTargetItems(obj, lore, lang);
                } else {
                    lore.add(fmt(F_ITEMS, lang.tr("quests.types.items.any_fuel"), lang));
                }
            }
            case BURN_FUEL -> {
                lore.add(lang.color(lang.tr("quests.objectives.burn-fuel")));
                if (obj.getItem() != null && !obj.getItem().isEmpty() ||
                        (obj.getTargetItems() != null && !obj.getTargetItems().isEmpty())) {
                    addTargetItems(obj, lore, lang);
                } else {
                    lore.add(fmt(F_ITEMS, lang.tr("quests.types.items.any_fuel"), lang));
                }
            }
            case ITEM_ENCHANT -> {
                lore.add(lang.color(lang.tr("quests.objectives.item_enchant")));
                addTargetItems(obj, lore, lang);
            }
            case ITEM_FISH -> {
                lore.add(lang.color(lang.tr("quests.objectives.item_fish")));
                addTargetItems(obj, lore, lang);
            }
            case ITEM_COOK -> {
                lore.add(lang.color(lang.tr("quests.objectives.item_cook")));
                addTargetItems(obj, lore, lang);
            }
            case BREWING -> {
                lore.add(lang.color(lang.tr("quests.objectives.brewing")));
                addTargetItems(obj, lore, lang);
            }
            case CONSUME_ITEM -> {
                lore.add(lang.color(lang.tr("quests.objectives.consume_item")));
                addTargetItems(obj, lore, lang);
            }
            case KILL_ENTITY -> {
                lore.add(lang.color(lang.tr("quests.objectives.kill")));
                addEntities(obj, lore, lang);
            }
            case DEAL_DAMAGE -> {
                lore.add(lang.color(lang.tr("quests.objectives.damage",
                        p("amount", "" + obj.getAmount()))));
            }
            case TAKE_DAMAGE -> {
                lore.add(lang.color(lang.tr("quests.objectives.take-damage",
                        p("amount", "" + obj.getAmount()))));
            }
            case TRAVEL_DISTANCE -> {
                lore.add(lang.color(lang.tr("quests.objectives.travel",
                        p("blocks", "" + obj.getAmount()))));
            }
            case REACH_LOCATION -> {
                lore.add(lang.color(lang.tr("quests.objectives.location")));
                lore.add(fmt(F_LOCATION, "X: " + obj.getX(), lang));
                lore.add(fmt(F_LOCATION, "Y: " + obj.getY(), lang));
                lore.add(fmt(F_LOCATION, "Z: " + obj.getZ(), lang));
            }
            case REACH_BIOME -> {
                lore.add(lang.color(lang.tr("quests.objectives.reach-biome",
                        Map.of(
                                "biome", lang.localizeBiome(obj.getMessage()),
                                "amount", String.valueOf(obj.getAmount())
                        ))));
            }
            case ENTER_REGION -> {
                lore.add(lang.color(lang.tr("quests.objectives.enter-region",
                        p("region", safe(obj.getRegion())))));
            }
            case LEAVE_REGION -> {
                lore.add(lang.color(lang.tr("quests.objectives.leave-region",
                        p("region", safe(obj.getRegion())))));
            }
            case INTERACT_BLOCK -> {
                lore.add(lang.color(lang.tr("quests.objectives.interact-block")));
                addBlocks(obj, lore, lang);
            }
            case INTERACT_ENTITY -> {
                lore.add(lang.color(lang.tr("quests.objectives.interact-entity")));
                addEntities(obj, lore, lang);
            }
            case USE_ITEM -> {
                lore.add(lang.color(lang.tr("quests.objectives.use_item")));
                addTargetItems(obj, lore, lang);
            }
            case EXECUTE_COMMAND -> {
                lore.add(lang.color(lang.tr("quests.objectives.command",
                        p("cmd", "/" + safe(obj.getCommand())))));
            }
            case CHAT_MESSAGE -> {
                lore.add(lang.color(lang.tr("quests.objectives.chat",
                        p("text", safe(obj.getMessage())))));
            }
            case LEVEL_UP_REACH -> {
                lore.add(lang.color(lang.tr("quests.objectives.level-up-reach",
                        p("levels", "" + obj.getAmount()))));
            }
            case LEVEL_UP_GAIN -> {
                lore.add(lang.color(lang.tr("quests.objectives.level-up-gain",
                        p("levels", "" + obj.getAmount()))));
            }
            case FILL_BUCKET -> {
                lore.add(lang.color(lang.tr("quests.objectives.fill-bucket")));
                addFluids(obj, lore, lang);
            }
            case EMPTY_BUCKET -> {
                lore.add(lang.color(lang.tr("quests.objectives.empty-bucket")));
                addFluids(obj, lore, lang);
            }
            case TILL_SOIL -> {
                lore.add(lang.color(lang.tr("quests.objectives.till-soil")));
                addBlocks(obj, lore, lang);
            }
            case PLANT_SEED -> {
                lore.add(lang.color(lang.tr("quests.objectives.plant-seed")));
                addTargetItems(obj, lore, lang);
            }
            case HARVEST_CROP -> {
                lore.add(lang.color(lang.tr("quests.objectives.harvest-crop")));
                addBlocks(obj, lore, lang);
            }
            case BONE_MEAL_USE -> {
                lore.add(lang.color(lang.tr("quests.objectives.bone-meal-use")));
                addBlocks(obj, lore, lang);
            }
            case STRIP_LOG -> {
                lore.add(lang.color(lang.tr("quests.objectives.strip-log")));
                addBlocks(obj, lore, lang);
            }
            case WAX_OFF -> {
                lore.add(lang.color(lang.tr("quests.objectives.wax-off")));
                addBlocks(obj, lore, lang);
            }
            case WAX_ON -> {
                lore.add(lang.color(lang.tr("quests.objectives.wax-on")));
                addBlocks(obj, lore, lang);
            }
            case ITEM_REPAIR -> {
                lore.add(lang.color(lang.tr("quests.objectives.item-repair")));
                addTargetItems(obj, lore, lang);
            }
            case ITEM_RENAME -> {
                lore.add(lang.color(lang.tr("quests.objectives.item-rename")));
                addTargetItems(obj, lore, lang);
                if (obj.getMessage() != null && !obj.getMessage().isEmpty()) {
                    lore.add(fmt(F_DEFAULT, lang.tr("quests.objectives.new-name",
                            p("name", obj.getMessage())), lang));
                }
            }
            case ITEM_BREAK -> {
                lore.add(lang.color(lang.tr("quests.objectives.item-break")));
                addTargetItems(obj, lore, lang);
            }
            case DYE_ITEM -> {
                lore.add(lang.color(lang.tr("quests.objectives.dye-item")));
                addTargetItems(obj, lore, lang);
            }

            case VILLAGER_SELL_ITEM -> {
                lore.add(lang.color(lang.tr("quests.objectives.villager-sell-item")));
                addEntities(obj, lore, lang);
                if (obj.getItem() != null && !obj.getItem().isEmpty() ||
                        (obj.getTargetItems() != null && !obj.getTargetItems().isEmpty())) {
                    addTargetItems(obj, lore, lang);
                }
            }
            case VILLAGER_BUY_ITEM -> {
                lore.add(lang.color(lang.tr("quests.objectives.villager-buy-item")));
                addEntities(obj, lore, lang);
                if (obj.getItem() != null && !obj.getItem().isEmpty() ||
                        (obj.getTargetItems() != null && !obj.getTargetItems().isEmpty())) {
                    addTargetItems(obj, lore, lang);
                }
            }
            case ENCHANT_TABLE_USE -> {
                lore.add(lang.color(lang.tr("quests.objectives.enchant-table-use")));
                addTargetItems(obj, lore, lang);
            }
            case ANVIL_USE -> {
                lore.add(lang.color(lang.tr("quests.objectives.anvil-use")));
                addTargetItems(obj, lore, lang);
            }
            case TAME_ANIMAL -> {
                lore.add(lang.color(lang.tr("quests.objectives.tame-animal")));
                addEntities(obj, lore, lang);
            }
            case BREED_ANIMALS -> {
                lore.add(lang.color(lang.tr("quests.objectives.breed-animals")));
                addEntities(obj, lore, lang);
            }
            case MILK_COW -> {
                lore.add(lang.color(lang.tr("quests.objectives.milk-cow")));
                addEntities(obj, lore, lang);
            }
            case SHEAR_SHEEP -> {
                lore.add(lang.color(lang.tr("quests.objectives.shear-sheep")));
                addEntities(obj, lore, lang);
            }
            case ENTITY_RIDE -> {
                lore.add(lang.color(lang.tr("quests.objectives.entity-ride")));
                addEntities(obj, lore, lang);
            }
            case THROW_EGG -> {
                lore.add(lang.color(lang.tr("quests.objectives.throw-egg")));
                if (!obj.getTargetEntities().isEmpty()) {
                    addEntities(obj, lore, lang);
                }
            }
            case EXPERIENCE_ORB_PICKUP -> {
                lore.add(lang.color(lang.tr("quests.objectives.experience-orb-pickup",
                        p("amount", "" + obj.getAmount()))));
            }
            case PLAYER_KILL -> {
                lore.add(lang.color(lang.tr("quests.objectives.player-kill",
                        p("amount", "" + obj.getAmount()))));
                if (obj.getWeapon() != null && !obj.getWeapon().isEmpty()) {
                    lore.add(fmt(F_ITEMS, ItemDisplayUtil.formatMaterialName(obj.getWeapon()), lang));
                }
            }
            case ASSIST_KILL -> {
                lore.add(lang.color(lang.tr("quests.objectives.assist-kill",
                        p("amount", "" + obj.getAmount()))));
                lore.add(fmt(F_DEFAULT, lang.tr("quests.objectives.min-damage",
                        p("damage", "" + obj.getAmount())), lang));
            }
            case ENTER_BED -> {
                lore.add(lang.color(lang.tr("quests.objectives.enter-bed",
                        p("amount", "" + obj.getAmount()))));
            }
            case CHANGE_DIMENSION -> {
                lore.add(lang.color(lang.tr("quests.objectives.change-dimension")));
                if (obj.getMessage() != null && !obj.getMessage().isEmpty()) {
                    String rawDim = obj.getMessage();
                    String localizedDim = ua.woody.questborn.QuestbornPlugin.getInstance().getConfig().getString("worlds." + rawDim.toLowerCase(), rawDim);
                    lore.add(fmt(F_LOCATION,
                            lang.tr("quests.objectives.dimension",
                                    p("dimension", localizedDim)),
                            lang));
                }
            }
            case FALL_DISTANCE -> {
                int dist = obj.getDistance() > 0 ? (int) obj.getDistance() : obj.getAmount();
                lore.add(lang.color(lang.tr("quests.objectives.fall-distance",
                        p("blocks", "" + dist))));
            }
            case BOAT_TRAVEL -> {
                lore.add(lang.color(lang.tr("quests.objectives.boat-travel",
                        p("blocks", "" + obj.getAmount()))));
            }
            case MINECART_TRAVEL -> {
                lore.add(lang.color(lang.tr("quests.objectives.minecart-travel",
                        p("blocks", "" + obj.getAmount()))));
            }
            case ELYTRA_FLY -> {
                lore.add(lang.color(lang.tr("quests.objectives.elytra-fly",
                        p("amount", "" + obj.getAmount()))));
            }
            case JUMP -> {
                lore.add(lang.color(lang.tr("quests.objectives.jump",
                        p("jumps", "" + obj.getAmount()))));
            }
            case CROUCH -> {
                lore.add(lang.color(lang.tr("quests.objectives.crouch",
                        p("crouches", "" + obj.getAmount()))));
            }
            case SPRINT_DISTANCE -> {
                lore.add(lang.color(lang.tr("quests.objectives.sprint-distance",
                        p("blocks", "" + obj.getAmount()))));
            }
            case CROUCH_DISTANCE -> {
                lore.add(lang.color(lang.tr("quests.objectives.crouch-distance",
                        p("blocks", "" + obj.getAmount()))));
            }
            case SWIM_DISTANCE -> {
                lore.add(lang.color(lang.tr("quests.objectives.swim-distance",
                        p("blocks", "" + obj.getAmount()))));
            }
            case POTION_SPLASH -> {
                lore.add(lang.color(lang.tr("quests.objectives.potion-splash")));
                addTargetItems(obj, lore, lang);
            }
            case POTION_DRINK -> {
                lore.add(lang.color(lang.tr("quests.objectives.potion-drink")));
                addTargetItems(obj, lore, lang);
            }
            case BEACON_ACTIVATE -> {
                lore.add(lang.color(lang.tr("quests.objectives.beacon-activate",
                        p("amount", "" + obj.getAmount()))));
            }
            case CONDUIT_ACTIVATE -> {
                lore.add(lang.color(lang.tr("quests.objectives.conduit-activate",
                        p("amount", "" + obj.getAmount()))));
            }
            case JOIN_SERVER -> {
                lore.add(lang.color(lang.tr("quests.objectives.join-server",
                        p("amount", "" + obj.getAmount()))));
            }
            case PLAY_TIME -> {
                String timeFormatted = ua.woody.questborn.util.TimeFormatter.format(obj.getAmount());
                lore.add(lang.color(lang.tr("quests.objectives.play-time",
                        p("time", timeFormatted))));
            }
            case SLEEP_IN_BED -> {
                lore.add(lang.color(lang.tr("quests.objectives.sleep-in-bed",
                        p("amount", "" + obj.getAmount()))));
            }
            case WEAR_ARMOR -> {
                lore.add(lang.color(lang.tr("quests.objectives.wear-armor")));
                addTargetItems(obj, lore, lang);
            }
            case HOLD_ITEM -> {
                lore.add(lang.color(lang.tr("quests.objectives.hold-item")));
                addTargetItems(obj, lore, lang);
                String holdTimeFormatted = ua.woody.questborn.util.TimeFormatter.format(obj.getAmount());
                lore.add(fmt(F_TIME,
                        lang.tr("quests.objectives.hold-time",
                                p("time", holdTimeFormatted)),
                        lang));
            }
            case DROP_ITEM -> {
                lore.add(lang.color(lang.tr("quests.objectives.drop-item")));
                addTargetItems(obj, lore, lang);
            }
            case OPEN_CONTAINER -> {
                lore.add(lang.color(lang.tr("quests.objectives.open-container",
                        p("amount", "" + obj.getAmount()))));
                List<String> containers = obj.getTargetContainers();
                if (containers != null && !containers.isEmpty()) {
                    for (String containerType : containers) {
                        String displayType = getLocalizedContainerType(containerType, lang);
                        lore.add(fmt(F_ITEMS, displayType, lang));
                    }
                } else if (obj.getMessage() != null && !obj.getMessage().isEmpty()) {
                    String displayType = getLocalizedContainerType(obj.getMessage(), lang);
                    lore.add(fmt(F_ITEMS,
                            lang.tr("quests.objectives.container-type",
                                    p("type", displayType)),
                            lang));
                }
            }
            case SIGN_EDIT -> {
                lore.add(lang.color(lang.tr("quests.objectives.sign.edit",
                        p("amount", "" + obj.getAmount()))));
                if (obj.getMessage() != null && !obj.getMessage().isEmpty()) {
                    String messageLine = fmt(F_BLOCKS,
                            lang.tr("quests.objectives.sign.message",
                                    p("message", obj.getMessage())),
                            lang);
                    lore.add(messageLine);
                }
            }
            case BOOK_EDIT -> {
                lore.add(lang.color(lang.tr("quests.objectives.book.edit",
                        p("amount", "" + obj.getAmount()))));
                if (obj.getMessage() != null && !obj.getMessage().isEmpty()) {
                    String messageLine = fmt(F_ITEMS,
                            lang.tr("quests.objectives.book.message",
                                    p("message", obj.getMessage())),
                            lang);
                    lore.add(messageLine);
                }
            }
            case RECEIVE_DAMAGE_TYPE -> {
                lore.add(lang.color(lang.tr("quests.objectives.receive-damage-type",
                        p("damage", "" + obj.getAmount()))));
                if (obj.getMessage() != null && !obj.getMessage().isEmpty()) {
                    String localizedCause = getLocalizedDamageCause(obj.getMessage(), lang);
                    lore.add(fmt(F_CAUSE, localizedCause, lang));
                } else {
                    lore.add(fmt(F_CAUSE,
                            lang.tr("quests.objectives.damage-cause.any"), lang));
                }
            }
            case NPC_INTERACT -> {
                lore.add(lang.color(lang.tr("quests.objectives.npc-interact")));
                if (obj.getNpc() != null && !obj.getNpc().isEmpty()) {
                    String npcName = plugin != null && plugin.getNpcManager() != null ? plugin.getNpcManager().getDisplayName(obj.getNpc()) : obj.getNpc();
                    lore.add(fmt(F_ENTITIES, npcName, lang));
                }
            }
            case GIVE_ITEM_TO_NPC -> {
                lore.add(lang.color(lang.tr("quests.objectives.give-item-to-npc")));
                if (obj.getNpc() != null && !obj.getNpc().isEmpty()) {
                    String npcName = plugin != null && plugin.getNpcManager() != null ? plugin.getNpcManager().getDisplayName(obj.getNpc()) : obj.getNpc();
                    lore.add(fmt(F_ENTITIES, npcName, lang));
                }
                if (obj.getItem() != null && !obj.getItem().isEmpty() ||
                        (obj.getTargetItems() != null && !obj.getTargetItems().isEmpty())) {
                    addTargetItems(obj, lore, lang);
                }
            }
            default -> {
                lore.add(fmt(F_DEFAULT, lang.tr("quests.objectives.none"), lang));
            }
        }

        return lore;
    }

    private static String fmt(String pat, String value, LanguageManager lang) {
        String safeValue = (value == null ? "Unknown" : value);
        return lang.color(pat.replace("{value}", safeValue));
    }

    public static String getItemsAdderDisplayName(String iaId, LanguageManager lang) {
        return ItemDisplayUtil.getItemsAdderDisplayName(iaId, lang);
    }

    public static String findLocalization(Material material, LanguageManager lang, boolean blocksFirst) {
        if (material == null)
            return "Unknown";
        String materialName = material.name();
        String itemLoc = lang.localizeItem(materialName);
        String blockLoc = lang.localizeMaterial(material);
        if (blocksFirst) {
            if (isValidLocalization(blockLoc, materialName))
                return blockLoc;
            if (isValidLocalization(itemLoc, materialName))
                return itemLoc;
        } else {
            if (isValidLocalization(itemLoc, materialName))
                return itemLoc;
            if (isValidLocalization(blockLoc, materialName))
                return blockLoc;
        }
        return formatMaterialName(materialName);
    }

    private static void addBlocks(QuestObjective obj, List<String> lore, LanguageManager lang) {
        List<String> raw = obj.getTargetBlockIds();
        List<Material> old = obj.getTargetMaterials();
        if ((raw == null || raw.isEmpty()) && (old == null || old.isEmpty())) {
            lore.add(fmt(F_BLOCKS, lang.tr("quests.types.blocks.any"), lang));
            return;
        }
        if (raw != null) {
            for (String id : raw) {
                if (id == null || id.isEmpty())
                    continue;

                String lowerId = id.toLowerCase(Locale.ROOT);
                if (lowerId.startsWith("itemsadder:")) {
                    String iaId = id.substring("itemsadder:".length()).trim().toLowerCase(Locale.ROOT);
                    lore.add(fmt(F_BLOCKS, ItemDisplayUtil.getItemsAdderDisplayName(iaId, lang), lang));
                    continue;
                } else if (lowerId.startsWith("ia:")) {
                    String iaId = id.substring("ia:".length()).trim().toLowerCase(Locale.ROOT);
                    lore.add(fmt(F_BLOCKS, ItemDisplayUtil.getItemsAdderDisplayName(iaId, lang), lang));
                    continue;
                } else if (lowerId.startsWith("craftengine:")) {
                    String ceId = id.substring("craftengine:".length()).trim();
                    lore.add(fmt(F_BLOCKS, ItemDisplayUtil.getCraftEngineDisplayName(ceId, lang), lang));
                    continue;
                } else if (lowerId.startsWith("ce:")) {
                    String ceId = id.substring("ce:".length()).trim();
                    lore.add(fmt(F_BLOCKS, ItemDisplayUtil.getCraftEngineDisplayName(ceId, lang), lang));
                    continue;
                }

                Material m = Material.matchMaterial(id);
                if (m == null) {
                    lore.add(fmt(F_BLOCKS, lang.tr("quests.types.blocks.unknown"), lang));
                    continue;
                }
                String loc = findLocalization(m, lang, true);
                lore.add(fmt(F_BLOCKS, loc, lang));
            }
        }
        if (old != null) {
            for (Material m : old) {
                String loc = findLocalization(m, lang, true);
                lore.add(fmt(F_BLOCKS, loc, lang));
            }
        }
    }

    private static void addEntities(QuestObjective obj, List<String> lore, LanguageManager lang) {
        List<EntityType> list = obj.getTargetEntities();
        List<String> customList = obj.getTargetCustomEntities();

        if (list.isEmpty() && (customList == null || customList.isEmpty())) {
            lore.add(fmt(F_ENTITIES, lang.tr("quests.types.entities.any"), lang));
            return;
        }
        for (EntityType t : list) {
            String loc = lang.localizeEntity(t);
            lore.add(fmt(F_ENTITIES, loc, lang));
        }
        if (customList != null) {
            for (String customId : customList) {
                String loc = ItemDisplayUtil.formatMaterialName(customId);
                lore.add(fmt(F_ENTITIES, loc, lang));
            }
        }
    }

    private static void addTargetItems(QuestObjective obj, List<String> lore, LanguageManager lang) {
        Set<String> lines = new LinkedHashSet<>();
        if (obj.getItem() != null && !obj.getItem().isEmpty()) {
            addItemSpecLine(obj.getItem(), lines, lang);
        }
        if (obj.getTargetItems() != null) {
            for (String spec : obj.getTargetItems()) {
                addItemSpecLine(spec, lines, lang);
            }
        }
        if (lines.isEmpty()) {
            lore.add(fmt(F_ITEMS, lang.tr("quests.types.items.any"), lang));
            return;
        }
        for (String line : lines) {
            lore.add(fmt(F_ITEMS, line, lang));
        }
    }

    private static void addItemSpecLine(String spec, Set<String> out, LanguageManager lang) {
        if (spec == null || spec.isBlank())
            return;
        String trimmed = spec.trim();

        if (trimmed.toLowerCase(Locale.ROOT).startsWith("itemsadder:")) {
            String iaId = trimmed.substring("itemsadder:".length()).trim().toLowerCase(Locale.ROOT);
            out.add(ItemDisplayUtil.getItemsAdderDisplayName(iaId, lang));
            return;
        }

        if (trimmed.toLowerCase(Locale.ROOT).startsWith("ia:")) {
            String iaId = trimmed.substring("ia:".length()).trim().toLowerCase(Locale.ROOT);
            out.add(ItemDisplayUtil.getItemsAdderDisplayName(iaId, lang));
            return;
        }

        if (trimmed.toLowerCase(Locale.ROOT).startsWith("craftengine:")) {
            String ceId = trimmed.substring("craftengine:".length()).trim();
            out.add(ItemDisplayUtil.getCraftEngineDisplayName(ceId, lang));
            return;
        }

        if (trimmed.toLowerCase(Locale.ROOT).startsWith("ce:")) {
            String ceId = trimmed.substring("ce:".length()).trim();
            out.add(ItemDisplayUtil.getCraftEngineDisplayName(ceId, lang));
            return;
        }

        String lowerTrimmed = trimmed.toLowerCase(Locale.ROOT);
        if (lang.getPlugin() != null && lang.getPlugin().getItemsAdderIntegration() != null &&
                lang.getPlugin().getItemsAdderIntegration().isItemsAdderItem(lowerTrimmed)) {
            out.add(ItemDisplayUtil.getItemsAdderDisplayName(lowerTrimmed, lang));
            return;
        }

        if (lang.getPlugin() != null && lang.getPlugin().getCraftEngineIntegration() != null &&
                lang.getPlugin().getCraftEngineIntegration().getCustomItem(lowerTrimmed) != null) {
            out.add(ItemDisplayUtil.getCraftEngineDisplayName(lowerTrimmed, lang));
            return;
        }

        Material mat = Material.matchMaterial(trimmed);
        if (mat == null)
            mat = Material.matchMaterial(trimmed.toUpperCase(Locale.ROOT));
        if (mat != null) {
            out.add(findLocalization(mat, lang, false));
            return;
        }
        if (trimmed.contains(":")) {
            String[] p = trimmed.split(":", 2);
            String left = p[0].trim().toUpperCase(Locale.ROOT);
            String right = p[1].trim().toUpperCase(Locale.ROOT);
            if (left.equals("POTION") || left.equals("SPLASH_POTION") || left.equals("LINGERING_POTION")) {
                String prefix = switch (left) {
                    case "POTION" -> "potion";
                    case "SPLASH_POTION" -> "splash_potion";
                    case "LINGERING_POTION" -> "lingering_potion";
                    default -> left.toLowerCase(Locale.ROOT);
                };
                String potionKey = prefix + "_" + right.toLowerCase(Locale.ROOT);
                String loc = lang.localizeItem(potionKey);
                if (loc != null && !loc.isBlank()) {
                    out.add(loc);
                    return;
                }
                out.add(formatMaterialName(left) + ": " + formatMaterialName(right));
                return;
            }
            Material byRight = Material.matchMaterial(right);
            if (byRight == null)
                byRight = Material.matchMaterial(right.toUpperCase(Locale.ROOT));
            if (byRight != null) {
                out.add(findLocalization(byRight, lang, false));
                return;
            }
            out.add(formatMaterialName(trimmed));
            return;
        }
        out.add(formatMaterialName(trimmed));
    }

    private static void addFluids(QuestObjective obj, List<String> lore, LanguageManager lang) {
        List<String> raw = obj.getTargetBlockIds();
        List<Material> old = obj.getTargetMaterials();
        if ((raw == null || raw.isEmpty()) && (old == null || old.isEmpty())) {
            lore.add(fmt(F_FLUIDS, lang.tr("quests.types.fluids.any"), lang));
            return;
        }
        if (raw != null) {
            for (String id : raw) {
                if (id == null || id.isEmpty())
                    continue;
                Material m = Material.matchMaterial(id);
                if (m == null) {
                    lore.add(fmt(F_FLUIDS, lang.tr("quests.types.fluids.unknown"), lang));
                    continue;
                }
                String loc = findLocalization(m, lang, true);
                lore.add(fmt(F_FLUIDS, loc, lang));
            }
        }
        if (old != null) {
            for (Material m : old) {
                String loc = findLocalization(m, lang, true);
                lore.add(fmt(F_FLUIDS, loc, lang));
            }
        }
    }

    private static boolean isValidLocalization(String text, String materialName) {
        if (text == null)
            return false;
        if (text.isEmpty())
            return false;
        String lowerText = text.toLowerCase();
        if (lowerText.contains("unknown") ||
                lowerText.startsWith("locale.")) {
            return false;
        }
        if (text.equalsIgnoreCase(materialName))
            return false;
        if (text.contains("minecraft:"))
            return false;
        if (text.equals(text.toUpperCase()) && text.contains("_"))
            return false;
        return true;
    }

    private static String formatMaterialName(String materialName) {
        if (materialName == null || materialName.isEmpty())
            return "Unknown";
        if (materialName.contains(":")) {
            materialName = materialName.substring(materialName.indexOf(":") + 1);
        }
        String formatted = materialName.replace("_", " ");
        StringBuilder result = new StringBuilder();
        String[] words = formatted.toLowerCase().split(" ");
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return result.toString().trim();
    }

    private static String safe(String s) {
        return s == null ? "ANY" : s;
    }

    private static Map<String, String> p(String k, String v) {
        return Map.of(k, v);
    }

    private static String getLocalizedContainerType(String containerType, LanguageManager lang) {
        if (containerType == null || containerType.trim().isEmpty()) {
            return lang.tr("quests.types.containers.any");
        }
        String trimmed = containerType.trim();
        Material material = Material.matchMaterial(trimmed);
        if (material != null) {
            String localized = lang.localizeMaterial(material);
            if (localized != null && !localized.toLowerCase().contains("unknown") &&
                    !localized.equalsIgnoreCase(trimmed)) {
                return localized;
            }
        }
        return formatMaterialName(trimmed);
    }

    private static String getLocalizedDamageCause(String damageCause, LanguageManager lang) {
        if (damageCause == null || damageCause.trim().isEmpty()) {
            return lang.tr("quests.objectives.damage-cause.any");
        }
        String trimmed = damageCause.trim().toUpperCase();
        switch (trimmed) {
            case "CONTACT":
                return lang.tr("quests.objectives.damage-cause.contact");
            case "ENTITY_ATTACK":
                return lang.tr("quests.objectives.damage-cause.entity_attack");
            case "PROJECTILE":
                return lang.tr("quests.objectives.damage-cause.projectile");
            case "SUFFOCATION":
                return lang.tr("quests.objectives.damage-cause.suffocation");
            case "FALL":
                return lang.tr("quests.objectives.damage-cause.fall");
            case "FIRE":
                return lang.tr("quests.objectives.damage-cause.fire");
            case "FIRE_TICK":
                return lang.tr("quests.objectives.damage-cause.fire_tick");
            case "LAVA":
                return lang.tr("quests.objectives.damage-cause.lava");
            case "DROWNING":
                return lang.tr("quests.objectives.damage-cause.drowning");
            case "BLOCK_EXPLOSION":
                return lang.tr("quests.objectives.damage-cause.block_explosion");
            case "ENTITY_EXPLOSION":
                return lang.tr("quests.objectives.damage-cause.entity_explosion");
            case "VOID":
                return lang.tr("quests.objectives.damage-cause.void");
            case "LIGHTNING":
                return lang.tr("quests.objectives.damage-cause.lightning");
            case "SUICIDE":
                return lang.tr("quests.objectives.damage-cause.suicide");
            case "STARVATION":
                return lang.tr("quests.objectives.damage-cause.starvation");
            case "POISON":
                return lang.tr("quests.objectives.damage-cause.poison");
            case "MAGIC":
                return lang.tr("quests.objectives.damage-cause.magic");
            case "WITHER":
                return lang.tr("quests.objectives.damage-cause.wither");
            case "FALLING_BLOCK":
                return lang.tr("quests.objectives.damage-cause.falling_block");
            case "THORNS":
                return lang.tr("quests.objectives.damage-cause.thorns");
            case "DRAGON_BREATH":
                return lang.tr("quests.objectives.damage-cause.dragon_breath");
            case "FLY_INTO_WALL":
                return lang.tr("quests.objectives.damage-cause.fly_into_wall");
            case "HOT_FLOOR":
                return lang.tr("quests.objectives.damage-cause.hot_floor");
            case "CRAMMING":
                return lang.tr("quests.objectives.damage-cause.cramming");
            case "DRYOUT":
                return lang.tr("quests.objectives.damage-cause.dryout");
            case "FREEZE":
                return lang.tr("quests.objectives.damage-cause.freeze");
            case "SONIC_BOOM":
                return lang.tr("quests.objectives.damage-cause.sonic_boom");
            case "CUSTOM":
            case "UNKNOWN":
                return lang.tr("quests.objectives.damage-cause.unknown");
            case "ANY":
                return lang.tr("quests.objectives.damage-cause.any");
            default:
                return formatDamageCauseName(damageCause);
        }
    }

    private static String formatDamageCauseName(String damageCause) {
        if (damageCause == null || damageCause.isEmpty())
            return "Unknown";
        String formatted = damageCause.replace("_", " ");
        StringBuilder result = new StringBuilder();
        String[] words = formatted.toLowerCase().split(" ");
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return result.toString().trim();
    }
}
