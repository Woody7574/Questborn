package ua.woody.questborn.util;

import ua.woody.questborn.lang.LanguageManager;
import ua.woody.questborn.model.QuestObjective;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.*;

public class ObjectiveLoreBuilder {

    // === Формати, які підтягуються з config.yml ===
    private static String F_BLOCKS   = "<#55ff55> • {value}";
    private static String F_ITEMS    = "<#55ccee> • {value}";
    private static String F_ENTITIES = "<#ff5555> • {value}";
    private static String F_LOCATION = "<#ffaa00> • {value}";
    private static String F_FLUIDS   = "<#5599ff> • {value}";
    private static String F_CAUSE    = "<#ff5555> • {value}";
    private static String F_TIME     = "<#aa55ff> • {value}";
    private static String F_DEFAULT  = "<#55ff55> • {value}";

    // Викликати 1 раз при запуску плагіну!
    public static void loadFormat(org.bukkit.configuration.file.FileConfiguration config) {
        F_BLOCKS   = config.getString("gui.details.lore-format.blocks",   F_BLOCKS);
        F_ITEMS    = config.getString("gui.details.lore-format.items",    F_ITEMS);
        F_ENTITIES = config.getString("gui.details.lore-format.entities", F_ENTITIES);
        F_LOCATION = config.getString("gui.details.lore-format.location", F_LOCATION);
        F_FLUIDS   = config.getString("gui.details.lore-format.fluids",   F_FLUIDS);
        F_CAUSE    = config.getString("gui.details.lore-format.cause",    F_CAUSE);
        F_TIME     = config.getString("gui.details.lore-format.time",     F_TIME);
        F_DEFAULT  = config.getString("gui.details.lore-format.default",  F_DEFAULT);
    }

    // ======================================================================

    public static List<String> build(QuestObjective obj, LanguageManager lang) {
        List<String> lore = new ArrayList<>();

        lore.add(lang.color("<#ffaa00>" + lang.tr("gui.details.objective.header")));

        switch (obj.getType()) {

            case BLOCK_BREAK -> {
                lore.add(lang.tr("gui.details.objective.block_break"));
                addBlocks(obj, lore, lang);
            }

            case BLOCK_PLACE -> {
                lore.add(lang.tr("gui.details.objective.block_place"));
                addBlocks(obj, lore, lang);
            }

            case COLLECT_ITEM, ITEM_CRAFT, ITEM_SMELT, ITEM_COOK,
                 ITEM_FISH, ITEM_ENCHANT, CONSUME_ITEM, BREWING, USE_ITEM -> {
                lore.add(lang.tr("gui.details.objective." + obj.getType().name().toLowerCase()));
                addTargetItems(obj, lore, lang);
            }

            case KILL_ENTITY -> {
                lore.add(lang.tr("gui.details.objective.kill"));
                addEntities(obj.getTargetEntities(), lore, lang);
            }

            case DEAL_DAMAGE ->
                    lore.add(lang.tr("gui.details.objective.damage", p("amount", ""+obj.getAmount())));

            case TAKE_DAMAGE ->
                    lore.add(lang.tr("gui.details.objective.take-damage", p("amount", ""+obj.getAmount())));

            case TRAVEL_DISTANCE ->
                    lore.add(lang.tr("gui.details.objective.travel", p("blocks", ""+obj.getAmount())));

            case REACH_LOCATION -> {
                lore.add(lang.tr("gui.details.objective.location"));
                lore.add(fmt(F_LOCATION, "X: "+obj.getX(), lang));
                lore.add(fmt(F_LOCATION, "Y: "+obj.getY(), lang));
                lore.add(fmt(F_LOCATION, "Z: "+obj.getZ(), lang));
            }

            case INTERACT_BLOCK -> {
                lore.add(lang.tr("gui.details.objective.interact-block"));
                addBlocks(obj, lore, lang);
            }

            case INTERACT_ENTITY -> {
                lore.add(lang.tr("gui.details.objective.interact-entity"));
                addEntities(obj.getTargetEntities(), lore, lang);
            }

            case ENTER_REGION ->
                    lore.add(lang.tr("gui.details.objective.enter-region", p("region", safe(obj.getRegion()))));

            case LEAVE_REGION ->
                    lore.add(lang.tr("gui.details.objective.leave-region", p("region", safe(obj.getRegion()))));

            case CHAT_MESSAGE ->
                    lore.add(lang.tr("gui.details.objective.chat", p("text", safe(obj.getMessage()))));

            case EXECUTE_COMMAND ->
                    lore.add(lang.tr("gui.details.objective.command", p("cmd", "/"+safe(obj.getCommand()))));

            case LEVEL_UP_REACH ->
                    lore.add(lang.tr("gui.details.objective.level-up-reach", p("levels", ""+obj.getAmount())));

            case LEVEL_UP_GAIN ->
                    lore.add(lang.tr("gui.details.objective.level-up-gain", p("levels", ""+obj.getAmount())));

            // ============ НОВІ ТИПИ (БЛОКИ ТА СВІТ) ============
            case FILL_BUCKET -> {
                lore.add(lang.tr("gui.details.objective.fill-bucket"));
                addFluids(obj, lore, lang);
            }

            case EMPTY_BUCKET -> {
                lore.add(lang.tr("gui.details.objective.empty-bucket"));
                addFluids(obj, lore, lang);
            }

            case TILL_SOIL -> {
                lore.add(lang.tr("gui.details.objective.till-soil"));
                addBlocks(obj, lore, lang);
            }

            case PLANT_SEED -> {
                lore.add(lang.tr("gui.details.objective.plant-seed"));
                addTargetItems(obj, lore, lang);
            }

            case HARVEST_CROP -> {
                lore.add(lang.tr("gui.details.objective.harvest-crop"));
                addBlocks(obj, lore, lang);
            }

            case BONE_MEAL_USE -> {
                lore.add(lang.tr("gui.details.objective.bone-meal-use"));
                addBlocks(obj, lore, lang);
            }

            case STRIP_LOG -> {
                lore.add(lang.tr("gui.details.objective.strip-log"));
                addBlocks(obj, lore, lang);
            }

            case WAX_OFF -> {
                lore.add(lang.tr("gui.details.objective.wax-off"));
                addBlocks(obj, lore, lang);
            }

            case WAX_ON -> {
                lore.add(lang.tr("gui.details.objective.wax-on"));
                addBlocks(obj, lore, lang);
            }

            // ============ НОВІ ТИПИ (ПРЕДМЕТИ ТА РЕМЕСЛА) ============
            case ITEM_REPAIR -> {
                lore.add(lang.tr("gui.details.objective.item-repair"));
                addTargetItems(obj, lore, lang);
            }

            case ITEM_RENAME -> {
                lore.add(lang.tr("gui.details.objective.item-rename"));
                addTargetItems(obj, lore, lang);
                if (obj.getMessage() != null && !obj.getMessage().isEmpty()) {
                    lore.add(lang.tr("gui.details.objective.new-name",
                            p("name", obj.getMessage())));
                }
            }

            case ITEM_BREAK -> {
                lore.add(lang.tr("gui.details.objective.item-break"));
                addTargetItems(obj, lore, lang);
            }

            case DYE_ITEM -> {
                lore.add(lang.tr("gui.details.objective.dye-item"));
                addTargetItems(obj, lore, lang);
            }

            case FILL_FUEL -> {
                lore.add(lang.tr("gui.details.objective.fill-fuel"));
                addTargetItems(obj, lore, lang);
            }

            case TRADE_WITH_VILLAGER -> {
                lore.add(lang.tr("gui.details.objective.trade-with-villager"));
                addEntities(obj.getTargetEntities(), lore, lang);
                if (obj.getItem() != null && !obj.getItem().isEmpty() ||
                        (obj.getTargetItems() != null && !obj.getTargetItems().isEmpty())) {
                    addTargetItems(obj, lore, lang);
                }
            }

            case ENCHANT_TABLE_USE -> {
                lore.add(lang.tr("gui.details.objective.enchant-table-use"));
                addTargetItems(obj, lore, lang);
            }

            case ANVIL_USE -> {
                lore.add(lang.tr("gui.details.objective.anvil-use"));
                addTargetItems(obj, lore, lang);
            }

            // ============ НОВІ ТИПИ (БОЇ ТА ІСТОТИ) ============
            case TAME_ANIMAL -> {
                lore.add(lang.tr("gui.details.objective.tame-animal"));
                addEntities(obj.getTargetEntities(), lore, lang);
            }

            case BREED_ANIMALS -> {
                lore.add(lang.tr("gui.details.objective.breed-animals"));
                addEntities(obj.getTargetEntities(), lore, lang);
            }

            case MILK_COW -> {
                lore.add(lang.tr("gui.details.objective.milk-cow"));
                addEntities(obj.getTargetEntities(), lore, lang);
            }

            case SHEAR_SHEEP -> {
                lore.add(lang.tr("gui.details.objective.shear-sheep"));
                addEntities(obj.getTargetEntities(), lore, lang);
            }

            case ENTITY_RIDE -> {
                lore.add(lang.tr("gui.details.objective.entity-ride"));
                addEntities(obj.getTargetEntities(), lore, lang);
            }

            case THROW_EGG -> {
                lore.add(lang.tr("gui.details.objective.throw-egg"));
                if (!obj.getTargetEntities().isEmpty()) {
                    addEntities(obj.getTargetEntities(), lore, lang);
                }
            }

            case FISHING_BOBBER_HOOK ->
                    lore.add(lang.tr("gui.details.objective.fishing-bobber-hook", p("amount", ""+obj.getAmount())));

            case EXPERIENCE_ORB_PICKUP ->
                    lore.add(lang.tr("gui.details.objective.experience-orb-pickup", p("amount", ""+obj.getAmount())));

            case PLAYER_KILL -> {
                lore.add(lang.tr("gui.details.objective.player-kill", p("amount", ""+obj.getAmount())));
                if (obj.getWeapon() != null && !obj.getWeapon().isEmpty()) {
                    lore.add(fmt(F_ITEMS, formatMaterialName(obj.getWeapon()), lang));
                }
            }

            case ASSIST_KILL -> {
                lore.add(lang.tr("gui.details.objective.assist-kill", p("amount", ""+obj.getAmount())));
                lore.add(fmt(F_DEFAULT, lang.tr("gui.details.objective.min-damage",
                        p("damage", ""+obj.getAmount())), lang));
            }

            // ============ НОВІ ТИПИ (ПЕРЕМІЩЕННЯ ТА ДОСЛІДЖЕННЯ) ============
            case ENTER_BED ->
                    lore.add(lang.tr("gui.details.objective.enter-bed", p("amount", ""+obj.getAmount())));

            case CHANGE_DIMENSION -> {
                lore.add(lang.tr("gui.details.objective.change-dimension"));
                if (obj.getMessage() != null && !obj.getMessage().isEmpty()) {
                    lore.add(fmt(F_LOCATION,
                            lang.tr("gui.details.objective.dimension",
                                    p("dimension", obj.getMessage())), lang));
                }
            }

            case FALL_DISTANCE -> {
                lore.add(lang.tr("gui.details.objective.fall-distance"));
                lore.add(fmt(F_DEFAULT,
                        lang.tr("gui.details.objective.min-height",
                                p("height", ""+obj.getAmount())), lang));
            }

            case BOAT_TRAVEL ->
                    lore.add(lang.tr("gui.details.objective.boat-travel", p("blocks", ""+obj.getAmount())));

            case MINECART_TRAVEL ->
                    lore.add(lang.tr("gui.details.objective.minecart-travel", p("blocks", ""+obj.getAmount())));

            case ELYTRA_FLY ->
                    lore.add(lang.tr("gui.details.objective.elytra-fly", p("amount", ""+obj.getAmount())));

            case JUMP ->
                    lore.add(lang.tr("gui.details.objective.jump", p("jumps", ""+obj.getAmount())));

            case CROUCH ->
                    lore.add(lang.tr("gui.details.objective.crouch", p("crouches", ""+obj.getAmount())));

            case SPRINT_DISTANCE ->
                    lore.add(lang.tr("gui.details.objective.sprint-distance", p("blocks", ""+obj.getAmount())));

            // ============ НОВІ ТИПИ (МАГІЯ ТА АЛХІМІЯ) ============
            case POTION_SPLASH -> {
                lore.add(lang.tr("gui.details.objective.potion-splash"));
                addTargetItems(obj, lore, lang);
            }

            case POTION_DRINK -> {
                lore.add(lang.tr("gui.details.objective.potion-drink"));
                addTargetItems(obj, lore, lang);
            }

            case BEACON_ACTIVATE ->
                    lore.add(lang.tr("gui.details.objective.beacon-activate", p("amount", ""+obj.getAmount())));

            case CONDUIT_ACTIVATE ->
                    lore.add(lang.tr("gui.details.objective.conduit-activate", p("amount", ""+obj.getAmount())));

            // ============ НОВІ ТИПИ (СОЦІАЛЬНІ) ============
            case TELEPORT -> {
                lore.add(lang.tr("gui.details.objective.teleport",
                        p("amount", ""+obj.getAmount())));

                // Отримуємо текст причини для відображення
                String causeText = getTeleportCauseText(obj, lang);
                if (causeText != null) {
                    lore.add(fmt(F_CAUSE, causeText, lang));
                }
            }

            case JOIN_SERVER ->
                    lore.add(lang.tr("gui.details.objective.join-server", p("amount", ""+obj.getAmount())));

            case PLAY_TIME -> {
                // Використовуємо TimeFormatter для форматування часу
                String timeFormatted = ua.woody.questborn.util.TimeFormatter.format(obj.getAmount());
                lore.add(lang.tr("gui.details.objective.play-time", p("time", timeFormatted)));
            }

            // ============ НОВІ ТИПИ (ІНШЕ) ============
            case SLEEP_IN_BED ->
                    lore.add(lang.tr("gui.details.objective.sleep-in-bed", p("amount", ""+obj.getAmount())));

            case WEAR_ARMOR -> {
                lore.add(lang.tr("gui.details.objective.wear-armor"));
                addTargetItems(obj, lore, lang);
            }

            case HOLD_ITEM -> {
                lore.add(lang.tr("gui.details.objective.hold-item"));
                addTargetItems(obj, lore, lang);
                // Використовуємо TimeFormatter для часу тримання
                String holdTimeFormatted = ua.woody.questborn.util.TimeFormatter.format(obj.getAmount());
                lore.add(fmt(F_TIME,
                        lang.tr("gui.details.objective.hold-time",
                                p("time", holdTimeFormatted)), lang));
            }

            case DROP_ITEM -> {
                lore.add(lang.tr("gui.details.objective.drop-item"));
                addTargetItems(obj, lore, lang);
            }

            case OPEN_CONTAINER -> {
                lore.add(lang.tr("gui.details.objective.open-container",
                        p("amount", ""+obj.getAmount())));

                List<String> containers = obj.getTargetContainers();
                if (containers != null && !containers.isEmpty()) {
                    for (String containerType : containers) {
                        String displayType = getLocalizedContainerType(containerType, lang);
                        lore.add(fmt(F_ITEMS, displayType, lang));
                    }
                } else if (obj.getMessage() != null && !obj.getMessage().isEmpty()) {
                    // Зворотна сумісність для старих квестів
                    String displayType = getLocalizedContainerType(obj.getMessage(), lang);
                    lore.add(fmt(F_ITEMS,
                            lang.tr("gui.details.objective.container-type",
                                    p("type", displayType)), lang));
                }
            }

            case SIGN_EDIT -> {
                lore.add(lang.tr("gui.details.objective.sign.edit",
                        p("amount", ""+obj.getAmount())));

                if (obj.getMessage() != null && !obj.getMessage().isEmpty()) {
                    // Застосовуємо формат для блоків F_BLOCKS
                    String messageLine = fmt(F_BLOCKS,
                            lang.tr("gui.details.objective.sign.message",
                                    p("message", obj.getMessage())),
                            lang);
                    lore.add(messageLine);
                }
            }

            case BOOK_EDIT -> {
                lore.add(lang.tr("gui.details.objective.book.edit",
                        p("amount", ""+obj.getAmount())));

                if (obj.getMessage() != null && !obj.getMessage().isEmpty()) {
                    // Застосовуємо формат для предметів F_ITEMS
                    String messageLine = fmt(F_ITEMS,
                            lang.tr("gui.details.objective.book.message",
                                    p("message", obj.getMessage())),
                            lang);
                    lore.add(messageLine);
                }
            }

            case RECEIVE_DAMAGE_TYPE -> {
                lore.add(lang.tr("gui.details.objective.receive-damage-type",
                        p("damage", ""+obj.getAmount())));

                if (obj.getMessage() != null && !obj.getMessage().isEmpty()) {
                    // Отримуємо локалізовану причину шкоди
                    String localizedCause = getLocalizedDamageCause(obj.getMessage(), lang);
                    lore.add(fmt(F_CAUSE, localizedCause, lang));
                } else {
                    // Якщо причина не вказана - показуємо "будь-яка"
                    lore.add(fmt(F_CAUSE,
                            lang.tr("gui.details.objective.damage-cause.any"), lang));
                }
            }

            default ->
                    lore.add(fmt(F_DEFAULT, lang.tr("gui.details.objective.none"), lang));
        }

        return lore;
    }

    // 🔥 НОВИЙ МЕТОД: Отримати локалізовану назву типу шкоди
    // 🔥 НОВИЙ МЕТОД: Отримати локалізовану назву типу шкоди
    private static String getLocalizedDamageCause(String damageCause, LanguageManager lang) {
        if (damageCause == null || damageCause.trim().isEmpty()) {
            return lang.tr("gui.details.objective.damage-cause.any");
        }

        String trimmed = damageCause.trim().toUpperCase();

        // Спеціальна обробка для типів шкоди
        switch (trimmed) {
            case "CONTACT": // Контакт з кактусом, ягідками
                return lang.tr("gui.details.objective.damage-cause.contact");
            case "ENTITY_ATTACK": // Атака істоти
                return lang.tr("gui.details.objective.damage-cause.entity_attack");
            case "PROJECTILE": // Стріла, сніжок
                return lang.tr("gui.details.objective.damage-cause.projectile");
            case "SUFFOCATION": // Задушення в блоці
                return lang.tr("gui.details.objective.damage-cause.suffocation");
            case "FALL": // Падіння
                return lang.tr("gui.details.objective.damage-cause.fall");
            case "FIRE": // Вогонь
                return lang.tr("gui.details.objective.damage-cause.fire");
            case "FIRE_TICK": // Пошкодження від вогню
                return lang.tr("gui.details.objective.damage-cause.fire_tick");
            case "LAVA": // Лава
                return lang.tr("gui.details.objective.damage-cause.lava");
            case "DROWNING": // Утоплення
                return lang.tr("gui.details.objective.damage-cause.drowning");
            case "BLOCK_EXPLOSION": // Вибух блоку (кріпера)
                return lang.tr("gui.details.objective.damage-cause.block_explosion");
            case "ENTITY_EXPLOSION": // Вибух істоти
                return lang.tr("gui.details.objective.damage-cause.entity_explosion");
            case "VOID": // Провал у пустоту
                return lang.tr("gui.details.objective.damage-cause.void");
            case "LIGHTNING": // Удар блискавки
                return lang.tr("gui.details.objective.damage-cause.lightning");
            case "SUICIDE": // Самогубство
                return lang.tr("gui.details.objective.damage-cause.suicide");
            case "STARVATION": // Голод
                return lang.tr("gui.details.objective.damage-cause.starvation");
            case "POISON": // Отруєння
                return lang.tr("gui.details.objective.damage-cause.poison");
            case "MAGIC": // Магія (зілля)
                return lang.tr("gui.details.objective.damage-cause.magic");
            case "WITHER": // Візер
                return lang.tr("gui.details.objective.damage-cause.wither");
            case "FALLING_BLOCK": // Падаючий блок
                return lang.tr("gui.details.objective.damage-cause.falling_block");
            case "THORNS": // Шипи
                return lang.tr("gui.details.objective.damage-cause.thorns");
            case "DRAGON_BREATH": // Подих дракона
                return lang.tr("gui.details.objective.damage-cause.dragon_breath");
            case "FLY_INTO_WALL": // Політ у стіну
                return lang.tr("gui.details.objective.damage-cause.fly_into_wall");
            case "HOT_FLOOR": // Гаряча підлога (магма)
                return lang.tr("gui.details.objective.damage-cause.hot_floor");
            case "CRAMMING": // Скупчення
                return lang.tr("gui.details.objective.damage-cause.cramming");
            case "DRYOUT": // Висушування (у воді)
                return lang.tr("gui.details.objective.damage-cause.dryout");
            case "FREEZE": // Замороження
                return lang.tr("gui.details.objective.damage-cause.freeze");
            case "SONIC_BOOM": // Звуковий удар (warden)
                return lang.tr("gui.details.objective.damage-cause.sonic_boom");
            case "CUSTOM": // Кастомна шкода → тепер unknown
            case "UNKNOWN": // Невідома причина
                return lang.tr("gui.details.objective.damage-cause.unknown");
            case "ANY":
                return lang.tr("gui.details.objective.damage-cause.any");
            default:
                // Для інших причин - форматуємо назву
                return formatDamageCauseName(damageCause);
        }
    }

    // 🔥 НОВИЙ МЕТОД: Форматування назви причини шкоди
    private static String formatDamageCauseName(String damageCause) {
        if (damageCause == null || damageCause.isEmpty()) return "Unknown";

        // Замінюємо підкреслення на пробіли
        String formatted = damageCause.replace("_", " ");

        // Робимо першу літеру кожної слова великою
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

    private static String getLocalizedContainerType(String containerType, LanguageManager lang) {
        if (containerType == null || containerType.trim().isEmpty()) {
            return lang.tr("locale.containers.any");
        }

        String trimmed = containerType.trim();

        // Спроба отримати Material
        Material material = Material.matchMaterial(trimmed);
        if (material != null) {
            String localized = lang.getLocalizationService().localizeMaterial(material);
            if (localized != null && !localized.toLowerCase().contains("unknown") &&
                    !localized.equalsIgnoreCase(trimmed)) {
                return localized;
            }
        }

        // Fallback - форматуємо назву
        return formatMaterialName(trimmed);
    }

    // 🔥 НОВИЙ МЕТОД: Отримати текст причини телепортації для відображення
    private static String getTeleportCauseText(QuestObjective obj, LanguageManager lang) {
        // Спочатку беремо cause, потім message як fallback
        String cause = obj.getCause();
        if (cause == null || cause.trim().isEmpty()) {
            cause = obj.getMessage();
        }

        // 1. Якщо причина не вказана взагалі → показуємо "будь-яка"
        if (cause == null || cause.trim().isEmpty()) {
            return lang.tr("gui.details.objective.cause.any");
        }

        String trimmedCause = cause.trim();

        // 2. Якщо вказано "ANY" → показуємо "будь-яка"
        if (trimmedCause.equalsIgnoreCase("ANY")) {
            return lang.tr("gui.details.objective.cause.any");
        }

        // 3. Перевіряємо чи підтримується причина
        String upperCause = trimmedCause.toUpperCase();
        switch (upperCause) {
            case "COMMAND":
                return lang.tr("gui.details.objective.cause.command");
            case "PLUGIN":
                return lang.tr("gui.details.objective.cause.plugin");
            case "NETHER_PORTAL":
                return lang.tr("gui.details.objective.cause.nether_portal");
            case "END_PORTAL":
                return lang.tr("gui.details.objective.cause.end_portal");
            default:
                // 4. Непідтримувана причина → "невідомо"
                return lang.tr("gui.details.objective.cause.unknown");
        }
    }

    // 🔥 НОВИЙ МЕТОД: Форматування назви причини (для нестандартних причин, якщо потрібно)
    private static String formatCauseName(String cause) {
        if (cause == null || cause.isEmpty()) return "Unknown";

        // Замінюємо підкреслення на пробіли
        String formatted = cause.replace("_", " ");

        // Робимо першу літеру кожної слова великою
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

    // ======================================================================
    // ДОПОМІЖНІ МЕТОДИ (залишаються без змін)

    private static void addBlocks(QuestObjective obj, List<String> lore, LanguageManager lang) {
        List<String> raw = obj.getTargetBlockIds();
        List<Material> old = obj.getTargetBlocks();

        if ((raw == null || raw.isEmpty()) && (old == null || old.isEmpty())) {
            lore.add(fmt(F_BLOCKS, lang.tr("locale.blocks.any"), lang));
            return;
        }

        if (raw != null) {
            for (String id : raw) {
                if(id == null || id.isEmpty()) continue;
                Material m = Material.matchMaterial(id);

                if(m == null) {
                    lore.add(fmt(F_BLOCKS, lang.tr("locale.blocks.unknown"), lang));
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

    private static void addEntities(List<EntityType> list, List<String> lore, LanguageManager lang) {
        if(list.isEmpty()) {
            lore.add(fmt(F_ENTITIES, lang.tr("locale.entities.any"), lang));
            return;
        }

        for(EntityType t : list) {
            String loc = lang.getLocalizationService().localizeEntity(t);
            lore.add(fmt(F_ENTITIES, loc, lang));
        }
    }

    private static void addTargetItems(QuestObjective obj, List<String> lore, LanguageManager lang) {
        Set<String> lines = new LinkedHashSet<>();

        // legacy single item
        if (obj.getItem() != null && !obj.getItem().isEmpty()) {
            addItemSpecLine(obj.getItem(), lines, lang);
        }

        // new unified list (може містити і MATERIAL, і minecraft:..., і POTION:HEALING)
        if (obj.getTargetItems() != null) {
            for (String spec : obj.getTargetItems()) {
                addItemSpecLine(spec, lines, lang);
            }
        }

        if (lines.isEmpty()) {
            lore.add(fmt(F_ITEMS, lang.tr("locale.items.any"), lang));
            return;
        }

        for (String line : lines) {
            lore.add(fmt(F_ITEMS, line, lang));
        }
    }

    private static void addItemSpecLine(String spec, Set<String> out, LanguageManager lang) {
        if (spec == null || spec.isBlank()) return;

        String trimmed = spec.trim();

        // ✅ 1) пробуємо як Material (підтримує minecraft:oak_log)
        Material mat = Material.matchMaterial(trimmed);
        if (mat == null) mat = Material.matchMaterial(trimmed.toUpperCase(Locale.ROOT));

        if (mat != null) {
            out.add(findLocalization(mat, lang, false));
            return;
        }

        // ✅ 2) спец-формат типу POTION:HEALING / SPLASH_POTION:REGENERATION
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

                String potionKey = prefix + "_" + right.toLowerCase(Locale.ROOT); // potion_healing / potion_strong_healing ...

                // ✅ 1) пробуємо locale.items.potion_healing (або splash_potion_healing і т.д.)
                String loc = lang.getLocalizationService().localizeItem(potionKey);
                if (loc != null && !loc.isBlank()) {
                    out.add(loc);
                    return;
                }

                // ✅ 2) fallback як було (якщо ключа нема в мовному файлі)
                out.add(formatMaterialName(left) + ": " + formatMaterialName(right));
                return;
            }

            // ✅ 3) fallback: якщо це namespaced (minecraft:oak_log), беремо праву частину
            Material byRight = Material.matchMaterial(right);
            if (byRight == null) byRight = Material.matchMaterial(right.toUpperCase(Locale.ROOT));
            if (byRight != null) {
                out.add(findLocalization(byRight, lang, false));
                return;
            }

            // кастомне/невідоме — просто форматуємо
            out.add(formatMaterialName(trimmed));
            return;
        }

        // ✅ 4) просто текст / невідоме
        out.add(formatMaterialName(trimmed));
    }

    private static void addFluids(QuestObjective obj, List<String> lore, LanguageManager lang) {
        List<String> raw = obj.getTargetBlockIds();
        List<Material> old = obj.getTargetBlocks();

        if ((raw == null || raw.isEmpty()) && (old == null || old.isEmpty())) {
            lore.add(fmt(F_FLUIDS, lang.tr("locale.fluids.any"), lang));
            return;
        }

        if (raw != null) {
            for (String id : raw) {
                if(id == null || id.isEmpty()) continue;
                Material m = Material.matchMaterial(id);

                if(m == null) {
                    lore.add(fmt(F_FLUIDS, lang.tr("locale.fluids.unknown"), lang));
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

    private static String findLocalization(Material material, LanguageManager lang, boolean blocksFirst) {
        if (material == null) return "Unknown";

        String materialName = material.name();
        String itemLoc = lang.getLocalizationService().localizeItem(materialName);
        String blockLoc = lang.getLocalizationService().localizeMaterial(material);

        if (blocksFirst) {
            if (isValidLocalization(blockLoc, materialName)) return blockLoc;
            if (isValidLocalization(itemLoc, materialName)) return itemLoc;
        } else {
            if (isValidLocalization(itemLoc, materialName)) return itemLoc;
            if (isValidLocalization(blockLoc, materialName)) return blockLoc;
        }

        return formatMaterialName(materialName);
    }

    private static boolean isValidLocalization(String text, String materialName) {
        if (text == null) return false;
        if (text.isEmpty()) return false;

        String lowerText = text.toLowerCase();
        if (lowerText.contains("невідомий") ||
                lowerText.contains("unknown") ||
                lowerText.startsWith("locale.")) {
            return false;
        }

        if (text.equalsIgnoreCase(materialName)) return false;
        if (text.contains("minecraft:")) return false;
        if (text.equals(text.toUpperCase()) && text.contains("_")) return false;

        return true;
    }

    private static String formatMaterialName(String materialName) {
        if (materialName == null || materialName.isEmpty()) return "Unknown";

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

    private static String fmt(String pat, String value, LanguageManager lang) {
        String safeValue = (value == null ? "Unknown" : value);
        return lang.color(pat.replace("{value}", safeValue));
    }


    private static String safe(String s){ return s==null?"ANY":s; }

    private static Map<String,String> p(String k,String v){ return Map.of(k,v); }
}