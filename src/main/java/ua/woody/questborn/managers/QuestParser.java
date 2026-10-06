package ua.woody.questborn.managers;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.effects.QuestEffects;
import ua.woody.questborn.model.*;

import java.io.File;
import java.util.*;

public class QuestParser {
    private final QuestbornPlugin plugin;
    private QuestTypeManager questTypeManager;
    private ua.woody.questborn.integration.ItemsAdderIntegration itemsAdderIntegration;

    public QuestParser(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    public void setQuestTypeManager(QuestTypeManager questTypeManager) {
        this.questTypeManager = questTypeManager;
    }

    public Map<String, Object> deepMap(ConfigurationSection sec) {
        Map<String, Object> map = new HashMap<>();

        for (String key : sec.getKeys(false)) {
            Object v = sec.get(key);

            if (v instanceof ConfigurationSection c)
                map.put(key, deepMap(c));
            else if (v instanceof List<?> list) {
                List<Object> out = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof ConfigurationSection s)
                        out.add(deepMap(s));
                    else
                        out.add(o);
                }
                map.put(key, out);
            } else
                map.put(key, v);
        }
        return map;
    }

    public QuestDefinition parseQuestYaml(YamlConfiguration yaml, String typeId) {
        String id = yaml.getString("id", "");
        if (id.isEmpty())
            return null;

        String name = yaml.getString("name", id);
        List<String> desc = yaml.getStringList("description");
        List<String> rewardsDesc = yaml.getStringList("rewards-description");

        List<QuestStage> questStages = parseQuestPath(yaml, id);

        String seasonTag = yaml.getString("season-tag", null);

        Map<String, Object> rewards = null;
        if (yaml.isConfigurationSection("rewards"))
            rewards = deepMap(yaml.getConfigurationSection("rewards"));

        String iconStr = yaml.getString("icon-material", yaml.getString("icon.material", yaml.getString("icon", null)));
        String iconItemsAdderId = null;
        String iconCraftEngineId = null;
        Material iconMaterial = Material.BOOK;
        if (iconStr != null) {
            if (iconStr.toLowerCase().startsWith("ce:") || iconStr.toLowerCase().startsWith("craftengine:")) {
                iconCraftEngineId = iconStr.toLowerCase().startsWith("ce:") ? iconStr.substring(3) : iconStr.substring(12);
            } else if (iconStr.toLowerCase().startsWith("ia:") || iconStr.toLowerCase().startsWith("itemsadder:")) {
                iconItemsAdderId = iconStr.toLowerCase().startsWith("ia:") ? iconStr.substring(3) : iconStr.substring(11);
            } else {
                iconMaterial = parseIconMaterial(yaml);
            }
        } else {
            iconMaterial = parseIconMaterial(yaml);
        }

        Integer iconCustomModelData = parseIconCustomModelData(yaml);
        String iconBaseHead = parseIconBaseHead(yaml);

        Integer slot = null;
        List<String> requiredQuests = new ArrayList<>();
        String requiredPermission = null;
        Integer page = null;

        QuestTypeConfig typeConfig = null;
        if (questTypeManager != null) {
            typeConfig = questTypeManager.getType(typeId);
        }

        if (typeConfig != null && (typeConfig.getEngine() == EngineType.CHAIN || typeConfig.getEngine() == EngineType.GLOBAL)) {
            if (yaml.contains("slot")) {
                slot = yaml.getInt("slot");
            }

            if (yaml.isList("require-quests")) {
                requiredQuests = yaml.getStringList("require-quests");
            } else if (yaml.contains("require-quest")) {
                String singleQuest = yaml.getString("require-quest");
                if (singleQuest != null && !singleQuest.trim().isEmpty()) {
                    requiredQuests.add(singleQuest.trim());
                }
            }

            if (yaml.contains("require-permission")) {
                requiredPermission = yaml.getString("require-permission");
            }

            if (yaml.contains("page")) {
                page = Math.max(0, yaml.getInt("page") - 1);
            }
        }

        String rotationPool = yaml.getString("rotation-pool", null);

        int maxParticipants = yaml.getInt("max-participants", 0);
        int minParticipants = yaml.getInt("min-participants", 0);
        int personalLimit = yaml.getInt("personal-limit", 0);
        int rewardStep = yaml.getInt("reward-step", 0);

        return new QuestDefinition(
                id, name, typeId, desc, rewardsDesc,
                questStages,
                0L, seasonTag, parseQuestEffects(yaml), rewards,
                iconItemsAdderId, iconCraftEngineId, iconMaterial, iconCustomModelData, iconBaseHead,
                slot, requiredQuests, requiredPermission, page, rotationPool, maxParticipants, minParticipants, personalLimit, rewardStep);
    }

    private List<QuestStage> parseQuestPath(YamlConfiguration yaml, String questId) {
        List<QuestStage> stages = new ArrayList<>();

        if (!yaml.isConfigurationSection("quest-path")) {
            QuestStage objectiveStage = parseLegacyObjective(yaml, questId);
            if (objectiveStage != null)
                stages.add(objectiveStage);

            QuestStage materialsStage = parseLegacyRequiredMaterials(yaml);
            if (materialsStage != null)
                stages.add(materialsStage);

            return stages;
        }

        ConfigurationSection pathSection = yaml.getConfigurationSection("quest-path");
        if (pathSection == null)
            return stages;

        List<String> keys = new ArrayList<>(pathSection.getKeys(false));
        keys.removeIf(k -> !k.matches("stage-\\d+"));
        keys.sort(Comparator.comparingInt(k -> {
            try {
                return Integer.parseInt(k.substring("stage-".length()));
            } catch (Exception ignored) {
                return Integer.MAX_VALUE;
            }
        }));

        for (String stageKey : keys) {
            ConfigurationSection stageSection = pathSection.getConfigurationSection(stageKey);
            if (stageSection == null)
                continue;

            if (stageSection.contains("objective")) {
                QuestStage objectiveStage = parseStageObjective(stageSection, questId);
                if (objectiveStage != null)
                    stages.add(objectiveStage);

            } else if (stageSection.contains("required-materials")) {
                QuestStage materialsStage = parseStageRequiredMaterials(stageSection);
                if (materialsStage != null)
                    stages.add(materialsStage);

            } else {
                plugin.getLogger().warning("│    Unknown stage type in " + stageKey + " for quest: " + questId);
            }
        }

        for (String k : pathSection.getKeys(false)) {
            if (!k.matches("stage-\\d+")) {
                plugin.getLogger().warning("│    Invalid stage key: " + k + " in quest: " + questId);
            }
        }

        return stages;
    }

    private QuestStage parseLegacyObjective(YamlConfiguration yaml, String questId) {
        ConfigurationSection objSec = yaml.getConfigurationSection("objective");
        if (objSec == null)
            return null;

        QuestObjective objective = parseQuestObjective(objSec, questId);
        if (objective == null)
            return null;

        return new QuestStage(objective);
    }

    private QuestStage parseLegacyRequiredMaterials(YamlConfiguration yaml) {
        Map<QuestItem, Integer> items = parseRequiredMaterials(yaml);
        if (items.isEmpty())
            return null;

        return new QuestStage(items);
    }

    private QuestStage parseStageObjective(ConfigurationSection stageSection, String questId) {
        ConfigurationSection objSec = stageSection.getConfigurationSection("objective");
        if (objSec == null)
            return null;

        QuestObjective objective = parseQuestObjective(objSec, questId);
        if (objective == null)
            return null;

        return new QuestStage(objective);
    }

    private QuestStage parseStageRequiredMaterials(ConfigurationSection stageSection) {
        Map<QuestItem, Integer> items = new HashMap<>();

        if (stageSection.isList("required-materials")) {
            List<String> list = stageSection.getStringList("required-materials");
            for (String entry : list) {
                parseRequiredItemEntry(entry, items);
            }
        }

        return items.isEmpty() ? null : new QuestStage(items);
    }

    private QuestObjective parseQuestObjective(ConfigurationSection sec, String questId) {
        QuestObjectiveType objType = parseObjectiveType(sec, questId);

        double distance = sec.getDouble("distance", 0);
        String region = sec.getString("region", null);
        String command = sec.getString("command", null);
        String message = sec.getString("message", null);
        String cause = sec.getString("cause", null);
        double money = sec.getDouble("money", 0);
        int xp = sec.getInt("xp", 0);
        double x = sec.getDouble("x", 0);
        double y = sec.getDouble("y", 0);
        double z = sec.getDouble("z", 0);
        String world = sec.getString("world", null);
        List<String> targetContainers = parseTargetContainers(sec);
        String dimension = sec.getString("dimension", null);
        double minDistance = sec.getDouble("min-distance", 0);
        String item = sec.getString("item", null);
        String weapon = sec.getString("weapon", null);

        ParsedTargets targets = parseTargetMaterialsUniversal(sec);
        ParsedEntities entities = parseTargetEntities(sec);

        int amount = parseObjectiveAmount(objType, sec);

        if (objType == QuestObjectiveType.ENTITY_RIDE && distance > 0) {
            amount = (int) Math.ceil(distance);
        }

        if (objType == QuestObjectiveType.RECEIVE_DAMAGE_TYPE) {
            if ((message == null || message.isBlank()) && cause != null && !cause.isBlank()) {
                message = cause;
            }
        }

        if (objType == QuestObjectiveType.REACH_BIOME) {
            String biomeKey = sec.getString("biome");
            if (biomeKey != null && !biomeKey.isBlank()) {
                message = biomeKey.toLowerCase(Locale.ROOT);
            } else if (message != null) {
                message = message.toLowerCase(Locale.ROOT);
            }
        }

        if (objType == QuestObjectiveType.CHANGE_DIMENSION) {
            if (dimension != null && !dimension.isBlank()) {
                message = dimension;
            }
        }

        if (objType == QuestObjectiveType.PLAY_TIME || objType == QuestObjectiveType.HOLD_ITEM) {
            int timeValue = sec.getInt("time", (objType == QuestObjectiveType.PLAY_TIME ? 60 : 30));
            String timeUnit = sec.getString("time-unit", "seconds").toLowerCase(Locale.ROOT);
            amount = convertToSeconds(timeValue, timeUnit);
        }

        if (objType == QuestObjectiveType.FALL_DISTANCE) {
            double parsedDist = sec.getDouble("distance", sec.getDouble("min-distance", sec.getDouble("amount", 0)));
            if (parsedDist > 0) {
                distance = parsedDist;
                amount = (int) Math.ceil(parsedDist);
            }
        }

        return new QuestObjective.Builder(objType)
                .amount(amount)
                .targetMaterials(targets.materials)
                .targetBlockIds(targets.rawIds)
                .targetItems(targets.itemSpecs)
                .potionTargets(targets.potionTargets)
                .targetEntities(entities.vanillaEntities)
                .targetCustomEntities(entities.customEntities)
                .targetContainers(targetContainers)
                .distance(distance)
                .region(region)
                .command(command)
                .message(message)
                .cause(cause)
                .money(money)
                .xp(xp)
                .item(item)
                .weapon(weapon)
                .location(x, y, z)
                .world(world)
                .npc(sec.getString("npc", null))
                .potionTargets(parsePotionTargets(sec.getStringList("potion-targets")))
                .itemsAdderIntegration(plugin.getItemsAdderIntegration())
                .craftEngineIntegration(plugin.getCraftEngineIntegration())
                .build();
    }

    private Map<QuestItem, Integer> parseRequiredMaterials(YamlConfiguration yaml) {
        Map<QuestItem, Integer> items = new HashMap<>();

        if (yaml.isConfigurationSection("required-material")) {
            ConfigurationSection section = yaml.getConfigurationSection("required-material");
            for (String key : section.getKeys(false)) {
                int amount = section.getInt(key, 1);
                if (amount > 0) {
                    if (key.startsWith("itemsadder:")) {
                        String id = key.substring("itemsadder:".length());
                        items.put(new QuestItem(id), amount);
                    } else if (key.startsWith("ia:")) {
                        String id = key.substring("ia:".length());
                        items.put(new QuestItem(id), amount);
                    } else {
                        Material material = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
                        if (material == null) {
                            plugin.getLogger().warning("Unknown material in required-material: " + key);
                            continue;
                        }
                        items.put(new QuestItem(material), amount);
                    }
                }
            }
        } else if (yaml.isList("required-material")) {
            List<String> list = yaml.getStringList("required-material");
            for (String entry : list) {
                parseRequiredItemEntry(entry, items);
            }
        }

        return items;
    }

    private Map<Material, String> parsePotionTargets(List<String> list) {
        Map<Material, String> map = new java.util.HashMap<>();
        if (list == null) return map;
        for (String s : list) {
            if (s.contains(":")) {
                String[] parts = s.split(":", 2);
                Material mat = Material.matchMaterial(parts[0]);
                if (mat != null) map.put(mat, parts[1]);
            } else {
                map.put(Material.POTION, s);
                map.put(Material.SPLASH_POTION, s);
                map.put(Material.LINGERING_POTION, s);
            }
        }
        return map;
    }

    private void parseRequiredItemEntry(String entry, Map<QuestItem, Integer> items) {
        String[] parts = entry.split(":");
        if (parts.length < 2)
            return;

        String lowerEntry = entry.toLowerCase(Locale.ROOT);
        if (lowerEntry.startsWith("itemsadder:") || lowerEntry.startsWith("ia:") || lowerEntry.startsWith("craftengine:") || lowerEntry.startsWith("ce:")) {
            try {
                String lastPart = parts[parts.length - 1];
                int amount = Integer.parseInt(lastPart.trim());

                String idContent = entry.substring(0, entry.lastIndexOf(":"));

                String id;
                boolean isCe = false;
                if (lowerEntry.startsWith("itemsadder:")) {
                    id = idContent.substring("itemsadder:".length());
                } else if (lowerEntry.startsWith("ia:")) {
                    id = idContent.substring("ia:".length());
                } else if (lowerEntry.startsWith("craftengine:")) {
                    id = idContent.substring("craftengine:".length());
                    isCe = true;
                } else {
                    id = idContent.substring("ce:".length());
                    isCe = true;
                }

                if (amount > 0) {
                    items.put(isCe ? new QuestItem(id, true) : new QuestItem(id), amount);
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid amount in required-materials check: " + entry);
            }
        } else {
            if (parts.length >= 2) {
                Material material = Material.matchMaterial(parts[0].trim().toUpperCase(Locale.ROOT));
                if (material == null) {
                    plugin.getLogger().warning("Unknown material in required-materials: " + parts[0]);
                    return;
                }
                try {
                    int amount = Integer.parseInt(parts[1].trim());
                    if (amount > 0) {
                        items.put(new QuestItem(material), amount);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid amount in required-materials: " + entry);
                }
            }
        }
    }

    private Material parseIconMaterial(YamlConfiguration yaml) {
        String iconStr = yaml.getString("icon-material", null);
        if (iconStr == null || iconStr.trim().isEmpty()) {
            iconStr = yaml.getString("icon.material", null);
        }
        if (iconStr == null || iconStr.trim().isEmpty()) {
            iconStr = yaml.getString("icon", null);
        }
        if (iconStr == null || iconStr.trim().isEmpty()) {
            return Material.BOOK;
        }

        try {
            Material material = Material.matchMaterial(iconStr.trim().toUpperCase(Locale.ROOT));
            if (material == null) {
                material = Material.matchMaterial(iconStr.trim());
            }

            if (material == null) {
                plugin.getLogger().warning("│    Unknown icon-material: " + iconStr + ", using default BOOK");
                return Material.BOOK;
            }

            return material;
        } catch (Exception e) {
            plugin.getLogger().warning("│    Failed to parse icon-material: " + iconStr + ", using default BOOK");
            return Material.BOOK;
        }
    }

    private String parseIconBaseHead(YamlConfiguration yaml) {
        String iconStr = yaml.getString("icon-base-head", null);
        if (iconStr == null || iconStr.trim().isEmpty()) {
            iconStr = yaml.getString("icon.base-head", null);
        }
        return iconStr;
    }

    private Integer parseIconCustomModelData(YamlConfiguration yaml) {
        if (yaml.isInt("custom-model-data")) {
            return yaml.getInt("custom-model-data");
        }

        if (yaml.isInt("customModelData")) {
            return yaml.getInt("customModelData");
        }

        return null;
    }

    private static class ParsedTargets {
        final List<Material> materials = new ArrayList<>();
        final List<String> rawIds = new ArrayList<>();
        final List<String> itemSpecs = new ArrayList<>();
        final Map<Material, String> potionTargets = new HashMap<>();
    }

    private static final Set<String> POTION_CONTAINERS = Set.of(
            "POTION", "SPLASH_POTION", "LINGERING_POTION", "TIPPED_ARROW");

    private ParsedTargets parseTargetMaterialsUniversal(ConfigurationSection sec) {
        ParsedTargets out = new ParsedTargets();

        List<String> entries = new ArrayList<>();

        addAllIfList(sec, "target-materials", entries);
        addOneIfString(sec, "target-material", entries);
        addAllIfList(sec, "target-blocks", entries);
        addOneIfString(sec, "target-block", entries);
        addAllIfList(sec, "target-items", entries);
        addOneIfString(sec, "target-item", entries);

        LinkedHashSet<Material> matSet = new LinkedHashSet<>();
        LinkedHashSet<String> rawSet = new LinkedHashSet<>();
        LinkedHashSet<String> specSet = new LinkedHashSet<>();
        Map<Material, String> potionTargets = new HashMap<>();

        for (String s : entries) {
            if (s == null || s.isBlank())
                continue;

            String trimmed = s.trim();
            String upperTrimmed = trimmed.toUpperCase(Locale.ROOT);

            Material m = Material.matchMaterial(upperTrimmed);
            if (m != null) {
                matSet.add(m);
                specSet.add(m.name());
                continue;
            }

            if (trimmed.contains(":")) {
                String[] parts = trimmed.split(":", 2);
                String left = parts[0].trim().toUpperCase(Locale.ROOT);
                String right = parts[1].trim();

                if (POTION_CONTAINERS.contains(left)) {
                    Material potionMat = Material.matchMaterial(left);
                    if (potionMat != null) {
                        potionTargets.put(potionMat, right);

                        matSet.add(potionMat);
                    }

                    specSet.add(left + ":" + right);
                    continue;
                }

                Material m2 = Material.matchMaterial(right.toUpperCase(Locale.ROOT));
                if (m2 != null) {
                    matSet.add(m2);
                    specSet.add(m2.name());
                    continue;
                }

                rawSet.add(upperTrimmed);
                specSet.add(upperTrimmed);
                continue;
            }

            rawSet.add(upperTrimmed);
            specSet.add(upperTrimmed);
        }

        out.materials.addAll(matSet);
        out.rawIds.addAll(rawSet);
        out.itemSpecs.addAll(specSet);
        out.potionTargets.putAll(potionTargets);

        return out;
    }

    private void addAllIfList(ConfigurationSection sec, String key, List<String> out) {
        if (sec.isList(key))
            out.addAll(sec.getStringList(key));
    }

    private void addOneIfString(ConfigurationSection sec, String key, List<String> out) {
        if (sec.isString(key)) {
            String v = sec.getString(key);
            if (v != null && !v.isBlank())
                out.add(v);
        }
    }

    private List<String> parseTargetContainers(ConfigurationSection sec) {
        List<String> containers = new ArrayList<>();

        if (sec.isList("containers")) {
            for (String container : sec.getStringList("containers")) {
                if (container != null && !container.trim().isEmpty()) {
                    containers.add(container.trim().toUpperCase(Locale.ROOT));
                }
            }
            return containers;
        }

        String containerType = sec.getString("container-type");
        if (containerType != null && !containerType.trim().isEmpty()) {
            containers.add(containerType.trim().toUpperCase(Locale.ROOT));
            return containers;
        }

        String message = sec.getString("message");
        if (message != null && !message.trim().isEmpty()) {
            QuestObjectiveType type = QuestObjectiveType.fromStringOrDefault(
                    sec.getString("type", "break-blocks"),
                    QuestObjectiveType.BLOCK_BREAK);
            if (type == QuestObjectiveType.OPEN_CONTAINER) {
                containers.add(message.trim().toUpperCase(Locale.ROOT));
            }
        }

        return containers;
    }

    private QuestObjectiveType parseObjectiveType(ConfigurationSection sec, String questId) {
        String raw = sec.getString("type", "break-blocks");

        QuestObjectiveType strict = QuestObjectiveType.fromStringStrict(raw);
        if (strict != null)
            return strict;

        QuestObjectiveType fallback = QuestObjectiveType.fromStringOrDefault(raw, QuestObjectiveType.BLOCK_BREAK);
        plugin.getLogger().warning("│    Unknown objective type '" + raw + "' in quest '" + questId
                + "'. Falling back to '" + fallback.canonicalKey() + "'.");
        return fallback;
    }

    private int parseObjectiveAmount(QuestObjectiveType t, ConfigurationSection sec) {
        return switch (t) {
            case TRAVEL_DISTANCE,
                    ELYTRA_FLY,
                    BOAT_TRAVEL,
                    MINECART_TRAVEL ->
                (int) Math.ceil(sec.getDouble("distance", sec.getInt("distance", 1)));

            case SPRINT_DISTANCE,
                    CROUCH_DISTANCE,
                    SWIM_DISTANCE -> (int) Math.ceil(sec.getDouble("distance", sec.getInt("distance", 100)));

            case FALL_DISTANCE -> (int) Math.ceil(sec.getDouble("distance", sec.getDouble("min-distance", sec.getDouble("amount", 10))));

            case ENTITY_RIDE -> sec.getInt("amount", 1);

            case PLAY_TIME -> sec.getInt("time", sec.getInt("amount", 60));
            case HOLD_ITEM -> sec.getInt("time", sec.getInt("amount", 30));
            case JUMP -> sec.getInt("amount", sec.getInt("jumps", 10));
            case CROUCH -> sec.getInt("amount", sec.getInt("crouches", 5));
            case ASSIST_KILL -> sec.getInt("amount", sec.getInt("min-damage", 5));

            default -> sec.getInt("amount", 1);
        };
    }

    private int convertToSeconds(int value, String unit) {
        return switch (unit) {
            case "seconds", "second", "sec", "s" -> value;
            case "minutes", "minute", "min", "m" -> value * 60;
            case "hours", "hour", "hr", "h" -> value * 3600;
            case "days", "day", "d" -> value * 86400;
            case "weeks", "week", "w" -> value * 604800;
            case "months", "month" -> value * 2592000;
            case "years", "year", "yr", "y" -> value * 31536000;
            default -> value;
        };
    }

    private record ParsedEntities(List<EntityType> vanillaEntities, List<String> customEntities) {}

    private ParsedEntities parseTargetEntities(ConfigurationSection sec) {
        List<EntityType> vanilla = new ArrayList<>();
        List<String> custom = new ArrayList<>();

        if (sec.isList("target-entities")) {
            for (String s : sec.getStringList("target-entities")) {
                if (s == null || s.isBlank())
                    continue;
                String trimmed = s.trim();
                if (trimmed.contains(":")) {
                    custom.add(trimmed);
                } else {
                    try {
                        vanilla.add(EntityType.valueOf(trimmed.toUpperCase(Locale.ROOT)));
                    } catch (Exception ignored) {
                        custom.add(trimmed);
                    }
                }
            }
        } else if (sec.isString("target-entity")) {
            String v = sec.getString("target-entity");
            if (v != null && !v.isBlank()) {
                String trimmed = v.trim();
                if (trimmed.contains(":")) {
                    custom.add(trimmed);
                } else {
                    try {
                        vanilla.add(EntityType.valueOf(trimmed.toUpperCase(Locale.ROOT)));
                    } catch (Exception ignored) {
                        custom.add(trimmed);
                    }
                }
            }
        }

        return new ParsedEntities(vanilla, custom);
    }

    private QuestEffects parseQuestEffects(YamlConfiguration yaml) {
        if (!yaml.isConfigurationSection("quest-effects"))
            return null;
        ConfigurationSection s = yaml.getConfigurationSection("quest-effects");
        if (s == null)
            return null;
        String a = s.getString("activate", null);
        String c = s.getString("complete", null);
        String sc = s.getString("stage-change", null);
        return (a != null || c != null || sc != null) ? new QuestEffects(a, c, sc) : null;
    }

    public List<QuestDefinition> loadQuestsFromFolder(File folder, String typeId) {
        List<QuestDefinition> list = new ArrayList<>();
        if (!folder.exists()) {
            folder.mkdirs();
            return list;
        }

        File[] files = folder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null)
            return list;

        for (File f : files) {
            try {
                QuestDefinition q = parseQuestYaml(YamlConfiguration.loadConfiguration(f), typeId);
                if (q != null)
                    list.add(q);
            } catch (Exception ex) {
                plugin.getLogger().severe("│    Failed to load " + f.getName() + ": " + ex.getMessage());
            }
        }
        return list;
    }
}
