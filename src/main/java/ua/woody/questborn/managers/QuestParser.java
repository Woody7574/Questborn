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

    public QuestParser(QuestbornPlugin plugin) {
        this.plugin = plugin;
    }

    /* =========================================================
     *                  YAML HELPERS
     * ========================================================= */

    public Map<String, Object> deepMap(ConfigurationSection sec) {
        Map<String, Object> map = new HashMap<>();

        for (String key : sec.getKeys(false)) {
            Object v = sec.get(key);

            if (v instanceof ConfigurationSection c) {
                map.put(key, deepMap(c));
            } else if (v instanceof List<?> list) {
                List<Object> out = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof ConfigurationSection s) out.add(deepMap(s));
                    else out.add(o);
                }
                map.put(key, out);
            } else {
                map.put(key, v);
            }
        }
        return map;
    }

    private int readQuestSlot(YamlConfiguration yaml) {
        for (String k : List.of("slot", "order", "gui-slot", "gui-order")) {
            if (!yaml.contains(k)) continue;
            try {
                return Integer.parseInt(String.valueOf(yaml.get(k)).trim());
            } catch (Exception ignored) {}
        }
        return -1;
    }

    /* =========================================================
     *                  MAIN PARSER
     * ========================================================= */

    public QuestDefinition parseQuestYaml(YamlConfiguration yaml, String typeId) {

        String id = yaml.getString("id");
        if (id == null || id.isBlank()) return null;

        String name = yaml.getString("name", id);

        // engine: якщо не задано - лишаємо null (QuestManager зробить fallback на typeConfig/default)
        String engineId = yaml.getString("engine", null);

        List<String> desc = yaml.getStringList("description");
        List<String> rewardsDesc = yaml.getStringList("rewards-description");
        int slot = readQuestSlot(yaml);

        /* ---------------- OBJECTIVE ---------------- */

        ConfigurationSection objSec = yaml.getConfigurationSection("objective");
        if (objSec == null) return null;

        QuestObjectiveType objType = parseObjectiveType(objSec, id);

        ParsedTargets targets = parseTargetMaterialsUniversal(objSec);
        List<EntityType> targetEntities = parseTargetEntities(objSec);

        int amount = parseObjectiveAmount(objType, objSec);

        double distance = objSec.getDouble("distance", 0);
        if (objType == QuestObjectiveType.ENTITY_RIDE && distance > 0) {
            amount = (int) Math.ceil(distance);
        }

        QuestObjective objective = new QuestObjective.Builder(objType)
                .amount(amount)
                .targetBlocks(targets.materials)
                .targetBlockIds(targets.rawIds)
                .targetItems(targets.itemSpecs)
                .targetEntities(targetEntities)
                .targetContainers(parseTargetContainers(objSec))
                .distance(distance)
                .region(objSec.getString("region"))
                .command(objSec.getString("command"))
                .message(objSec.getString("message"))
                .cause(objSec.getString("cause"))
                .money(objSec.getDouble("money", 0))
                .xp(objSec.getInt("xp", 0))
                .item(objSec.getString("item"))
                .weapon(objSec.getString("weapon"))
                .location(
                        objSec.getDouble("x", 0),
                        objSec.getDouble("y", 0),
                        objSec.getDouble("z", 0)
                )
                .build();

        /* ---------------- AVAILABILITY ---------------- */

        QuestAvailability availability = null;
        if (yaml.isConfigurationSection("availability")) {
            var a = yaml.getConfigurationSection("availability");

            List<String> after = new ArrayList<>();
            if (a != null) {
                if (a.isList("after")) after.addAll(a.getStringList("after"));
                else if (a.isString("after")) after.add(a.getString("after"));

                QuestAvailability tmp = new QuestAvailability(
                        after,
                        AfterMode.fromString(a.getString("after-mode", "ALL")),
                        a.getString("permission"),
                        a.getString("permission-message")
                );

                if (!tmp.isEmpty()) availability = tmp;
            }
        }

        /* ---------------- REWARDS ---------------- */

        Map<String, Object> rewards = null;
        if (yaml.isConfigurationSection("rewards")) {
            ConfigurationSection r = yaml.getConfigurationSection("rewards");
            if (r != null) rewards = deepMap(r);
        }

        /* ---------------- FINAL OBJECT ---------------- */

        return new QuestDefinition(
                id,
                name,
                typeId,
                engineId,
                desc,
                rewardsDesc,
                objective,
                0L,
                yaml.getString("season-tag"),
                parseQuestEffects(yaml),
                rewards,
                availability,
                slot
        );
    }

    /* =========================================================
     *                  LOAD FROM FOLDER
     * ========================================================= */

    public List<QuestDefinition> loadQuestsFromFolder(File folder, String typeId) {
        List<QuestDefinition> list = new ArrayList<>();
        if (!folder.exists()) {
            // створимо папку під квести типу (краще, ніж падати)
            //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();
            return list;
        }

        File[] files = folder.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) return list;

        for (File f : files) {
            try {
                QuestDefinition q = parseQuestYaml(
                        YamlConfiguration.loadConfiguration(f),
                        typeId
                );
                if (q != null) list.add(q);
            } catch (Exception ex) {
                plugin.getLogger().severe(
                        "│ Failed to load " + f.getName() + ": " + ex.getMessage()
                );
            }
        }
        return list;
    }

    /* =========================================================
     *                  INTERNAL HELPERS
     * ========================================================= */

    private static class ParsedTargets {
        final List<Material> materials = new ArrayList<>();
        final List<String> rawIds = new ArrayList<>();
        final List<String> itemSpecs = new ArrayList<>();
    }

    private ParsedTargets parseTargetMaterialsUniversal(ConfigurationSection sec) {
        ParsedTargets out = new ParsedTargets();
        List<String> entries = new ArrayList<>();

        addAllIfList(sec, "target-materials", entries);
        addOneIfString(sec, "target-material", entries);
        addAllIfList(sec, "target-blocks", entries);
        addOneIfString(sec, "target-block", entries);
        addAllIfList(sec, "target-items", entries);
        addOneIfString(sec, "target-item", entries);

        for (String s : entries) {
            if (s == null || s.isBlank()) continue;

            String v = s.trim().toUpperCase(Locale.ROOT);

            Material m = Material.matchMaterial(v);
            if (m != null) {
                out.materials.add(m);
                out.itemSpecs.add(m.name());
                continue;
            }

            // підтримка "POTION:HEALING", "minecraft:stone", тощо — як itemSpec/ID
            if (v.contains(":")) {
                out.rawIds.add(v);
                out.itemSpecs.add(v);
            }
        }
        return out;
    }

    private void addAllIfList(ConfigurationSection sec, String key, List<String> out) {
        if (sec != null && sec.isList(key)) out.addAll(sec.getStringList(key));
    }

    private void addOneIfString(ConfigurationSection sec, String key, List<String> out) {
        if (sec != null && sec.isString(key)) out.add(sec.getString(key));
    }

    private List<String> parseTargetContainers(ConfigurationSection sec) {
        List<String> out = new ArrayList<>();
        if (sec != null && sec.isList("containers")) {
            for (String s : sec.getStringList("containers")) {
                if (s != null && !s.isBlank()) out.add(s.toUpperCase(Locale.ROOT));
            }
        }
        return out;
    }

    private QuestObjectiveType parseObjectiveType(ConfigurationSection sec, String questId) {
        String raw = sec.getString("type", "break-blocks");

        QuestObjectiveType strict = QuestObjectiveType.fromStringStrict(raw);
        if (strict != null) return strict;

        QuestObjectiveType fallback =
                QuestObjectiveType.fromStringOrDefault(raw, QuestObjectiveType.BLOCK_BREAK);

        plugin.getLogger().warning(
                "│ Unknown objective type '" + raw +
                        "' in quest '" + questId +
                        "', fallback to " + fallback.canonicalKey()
        );
        return fallback;
    }

    private int parseObjectiveAmount(QuestObjectiveType t, ConfigurationSection sec) {
        return switch (t) {
            case TRAVEL_DISTANCE, SPRINT_DISTANCE ->
                    (int) Math.ceil(sec.getDouble("distance", 1));
            case FALL_DISTANCE ->
                    (int) Math.ceil(sec.getDouble("min-distance", 10));
            case PLAY_TIME -> sec.getInt("time", 60);
            case HOLD_ITEM -> sec.getInt("time", 30);
            default -> sec.getInt("amount", 1);
        };
    }

    private List<EntityType> parseTargetEntities(ConfigurationSection sec) {
        List<EntityType> list = new ArrayList<>();

        if (sec != null && sec.isList("target-entities")) {
            for (String s : sec.getStringList("target-entities")) {
                if (s == null || s.isBlank()) continue;
                try {
                    list.add(EntityType.valueOf(s.toUpperCase(Locale.ROOT)));
                } catch (Exception ignored) {}
            }
        }

        // optional: один елемент
        if (sec != null && sec.isString("target-entity")) {
            String s = sec.getString("target-entity");
            if (s != null && !s.isBlank()) {
                try {
                    list.add(EntityType.valueOf(s.toUpperCase(Locale.ROOT)));
                } catch (Exception ignored) {}
            }
        }

        return list;
    }

    private QuestEffects parseQuestEffects(YamlConfiguration yaml) {
        if (!yaml.isConfigurationSection("quest-effects")) return null;
        ConfigurationSection s = yaml.getConfigurationSection("quest-effects");
        if (s == null) return null;
        return new QuestEffects(
                s.getString("activate"),
                s.getString("complete")
        );
    }
}
