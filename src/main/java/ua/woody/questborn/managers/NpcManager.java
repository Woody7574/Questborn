package ua.woody.questborn.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.NpcConfig;
import ua.woody.questborn.model.npc.mood.MoodRule;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NpcManager implements ua.woody.questborn.api.managers.NpcManagerAPI {
    public static class PendingNpcAction {
        public final boolean isStart;
        public final boolean isQuest;
        public final String targetId;
        public final boolean isLink;
        public final long createdAt;

        public PendingNpcAction(boolean isStart, boolean isQuest, String targetId, boolean isLink) {
            this.isStart = isStart;
            this.isQuest = isQuest;
            this.targetId = targetId;
            this.isLink = isLink;
            this.createdAt = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > 45000L;
        }
    }

    private final QuestbornPlugin plugin;
    private final Map<String, NpcConfig> npcs = new HashMap<>();
    private final Map<UUID, PendingNpcAction> pendingActions = new ConcurrentHashMap<>();
    private final File configFile;

    public NpcManager(QuestbornPlugin plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "npc");
        if (!dir.exists()) dir.mkdirs();
        this.configFile = new File(dir, "npcs.yml");
        loadNpcs();
    }

    public java.util.Collection<String> getAllNpcIds() {
        if (npcs != null) {
            return new ArrayList<>(npcs.keySet());
        }
        return java.util.Collections.emptyList();
    }

    public boolean hasExplicitFinisher(String questId, String typeId) {
        for (NpcConfig config : npcs.values()) {
            if (config.finishesQuest(questId, typeId)) return true;
        }
        return false;
    }

    private static final String HEADER =
            "===========================================================\n" +
            " Questborn NPCs Configuration\n" +
            "===========================================================\n" +
            " This file stores configurations for individual NPCs.\n" +
            " You can set custom display names, GUI titles, link dialogues,\n" +
            " and adjust indicator hologram offsets.\n" +
            "\n" +
            " Note: Quests are linked automatically using 'start-npc' and\n" +
            " 'finish-npc' in their quest configurations.\n" +
            "\n" +
            " Available properties:\n" +
            "   displayname: Custom name (supports colors)\n" +
            "   gui-title: Custom title for menus\n" +
            "   indicator-offset: Y-axis offset for holograms\n" +
            "   dialogue-id: Linked dialogue preset\n" +
            "   voice: Sound preset\n" +
            "   default_mood: Default mood for NPC (e.g. NEUTRAL)\n" +
            "   mood_rules: Map of conditional moods (rules are checked by priority)\n" +
            "     RESPECTFUL:\n" +
            "       priority: 20\n" +
            "       conditions:\n" +
            "         QUEST_COMPLETED:\n" +
            "           quest_id: \"example_quest\"\n" +
            "         TIME:\n" +
            "           range: \"22000-6000\"\n" +
            "         PLACEHOLDER_1: # Use _1, _2 for multiple conditions of same type\n" +
            "           placeholder: \"%vault_rank%\"\n" +
            "           expression: \"vip\"\n" +
            "===========================================================";

    public void loadNpcs() {
        npcs.clear();

        if (!configFile.exists()) {
            plugin.saveResource("npc/npcs.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection npcsSection = config.getConfigurationSection("npcs");
        if (npcsSection == null) return;

        for (String npcId : npcsSection.getKeys(false)) {
            ConfigurationSection sec = npcsSection.getConfigurationSection(npcId);
            if (sec != null) {
                String guiTitle = sec.getString("gui-title", "&0{npc} - Quests");

                NpcConfig npc = new NpcConfig(npcId, guiTitle);
                if (sec.contains("starts.quests")) npc.getStartsQuests().addAll(sec.getStringList("starts.quests"));
                if (sec.contains("starts.types")) npc.getStartsTypes().addAll(sec.getStringList("starts.types"));
                if (sec.contains("finishes.quests")) npc.getFinishesQuests().addAll(sec.getStringList("finishes.quests"));
                if (sec.contains("finishes.types")) npc.getFinishesTypes().addAll(sec.getStringList("finishes.types"));

                if (!npc.getStartsTypes().isEmpty()) {
                    npc.getStartsQuests().clear();
                }
                if (!npc.getFinishesTypes().isEmpty()) {
                    npc.getFinishesQuests().clear();
                }

                if (sec.contains("displayname")) {
                    npc.setDisplayName(sec.getString("displayname"));
                }
                if (sec.contains("indicator-offset")) {
                    npc.setIndicatorHeightOffset(sec.getDouble("indicator-offset"));
                }
                if (sec.contains("dialogue-id")) {
                    npc.setDialogueId(sec.getString("dialogue-id"));
                }
                if (sec.contains("voice")) {
                    npc.setVoice(sec.getString("voice"));
                }
                if (sec.contains("head_texture")) {
                    npc.setHeadTexture(sec.getString("head_texture"));
                }
                if (sec.contains("default_mood")) {
                    npc.setDefaultMood(sec.getString("default_mood"));
                }
                if (sec.contains("mood_rules")) {
                    List<MoodRule> parsedRules = new ArrayList<>();
                    if (sec.isList("mood_rules")) {
                        List<Map<?, ?>> ruleMaps = sec.getMapList("mood_rules");
                        for (Map<?, ?> rm : ruleMaps) {
                            MoodRule r = plugin.getMoodManager().parseRule(rm);
                            if (r != null) parsedRules.add(r);
                        }
                    } else if (sec.isConfigurationSection("mood_rules")) {
                        ConfigurationSection rulesSec = sec.getConfigurationSection("mood_rules");
                        for (String key : rulesSec.getKeys(false)) {
                            ConfigurationSection ruleSec = rulesSec.getConfigurationSection(key);
                            if (ruleSec != null) {
                                String mood = key.matches(".*_\\d+$") ? key.substring(0, key.lastIndexOf('_')) : key;
                                mood = mood.toUpperCase();
                                MoodRule r = plugin.getMoodManager().parseRuleFromSection(mood, ruleSec);
                                if (r != null) parsedRules.add(r);
                            }
                        }
                    }
                    npc.setMoodRules(parsedRules);
                }
                npcs.put(npcId, npc);
            }
        }
    }

    public void saveNpcsAsync() {
        List<NpcConfig> npcSnapshot = new ArrayList<>(npcs.values());
        plugin.getFoliaLib().getImpl().runAsync(__ -> {
            try {
                YamlConfiguration config = new YamlConfiguration();
                config.options().header(HEADER);
                config.options().copyHeader(true);
                ConfigurationSection npcsSection = config.createSection("npcs");
                for (NpcConfig npc : npcSnapshot) {
                    ConfigurationSection sec = npcsSection.createSection(npc.getNpcId());
                    if (npc.getGuiTitle() != null) sec.set("gui-title", npc.getGuiTitle().replace("§", "&"));
                    if (!npc.getStartsQuests().isEmpty()) sec.set("starts.quests", new ArrayList<>(npc.getStartsQuests()));
                    if (!npc.getStartsTypes().isEmpty()) sec.set("starts.types", new ArrayList<>(npc.getStartsTypes()));
                    if (!npc.getFinishesQuests().isEmpty()) sec.set("finishes.quests", new ArrayList<>(npc.getFinishesQuests()));
                    if (!npc.getFinishesTypes().isEmpty()) sec.set("finishes.types", new ArrayList<>(npc.getFinishesTypes()));

                    if (npc.getDisplayName() != null) sec.set("displayname", npc.getDisplayName().replace("§", "&"));
                    if (npc.getIndicatorHeightOffset() != null) sec.set("indicator-offset", npc.getIndicatorHeightOffset());
                    if (npc.getDialogueId() != null) sec.set("dialogue-id", npc.getDialogueId());
                    if (npc.getVoice() != null) sec.set("voice", npc.getVoice());
                    if (npc.getHeadTexture() != null) sec.set("head_texture", npc.getHeadTexture());
                    if (!"NEUTRAL".equals(npc.getDefaultMood())) sec.set("default_mood", npc.getDefaultMood());

                    if (npc.getMoodRules() != null && !npc.getMoodRules().isEmpty()) {
                        ConfigurationSection rulesSec = sec.createSection("mood_rules");
                        java.util.Map<String, Integer> moodCounts = new java.util.HashMap<>();

                        List<MoodRule> moodRulesSnapshot = new ArrayList<>(npc.getMoodRules());
                        for (MoodRule rule : moodRulesSnapshot) {
                            String mood = rule.getMood();
                            int mc = moodCounts.getOrDefault(mood, 0) + 1;
                            moodCounts.put(mood, mc);
                            String rKey = mc == 1 ? mood : mood + "_" + mc;

                            ConfigurationSection rSec = rulesSec.createSection(rKey);
                            rSec.set("priority", rule.getPriority());

                            if (rule.getConditions() != null && !rule.getConditions().isEmpty()) {
                                ConfigurationSection condsSec = rSec.createSection("conditions");
                                java.util.Map<String, Integer> condCounts = new java.util.HashMap<>();

                                List<ua.woody.questborn.model.npc.mood.MoodCondition> condsSnapshot = new ArrayList<>(rule.getConditions());
                                for (ua.woody.questborn.model.npc.mood.MoodCondition cond : condsSnapshot) {
                                    String type = "";
                                    if (cond instanceof ua.woody.questborn.model.npc.mood.TimeCondition) type = "TIME";
                                    else if (cond instanceof ua.woody.questborn.model.npc.mood.WeatherCondition) type = "WEATHER";
                                    else if (cond instanceof ua.woody.questborn.model.npc.mood.MoneyCondition) type = "MONEY";
                                    else if (cond instanceof ua.woody.questborn.model.npc.mood.ItemCondition) type = "ITEM";
                                    else if (cond instanceof ua.woody.questborn.model.npc.mood.QuestCompletedCondition) type = "QUEST_COMPLETED";
                                    else if (cond instanceof ua.woody.questborn.model.npc.mood.QuestActiveCondition) type = "QUEST_ACTIVE";
                                    else if (cond instanceof ua.woody.questborn.model.npc.mood.PlaceholderCondition) type = "PLACEHOLDER";
                                    else if (cond instanceof ua.woody.questborn.model.npc.mood.ChanceCondition) type = "CHANCE";

                                    int cc = condCounts.getOrDefault(type, 0) + 1;
                                    condCounts.put(type, cc);
                                    String cKey = cc == 1 ? type : type + "_" + cc;

                                    ConfigurationSection cSec = condsSec.createSection(cKey);
                                    if (cond instanceof ua.woody.questborn.model.npc.mood.TimeCondition tc) {
                                        cSec.set("range", tc.getRange());
                                    } else if (cond instanceof ua.woody.questborn.model.npc.mood.WeatherCondition wc) {
                                        cSec.set("state", wc.getRequiredState());
                                    } else if (cond instanceof ua.woody.questborn.model.npc.mood.MoneyCondition mc2) {
                                        cSec.set("amount", mc2.getExpression());
                                    } else if (cond instanceof ua.woody.questborn.model.npc.mood.ItemCondition ic) {
                                        cSec.set("material", ic.getMaterialName());
                                        cSec.set("amount", ic.getAmountExpression());
                                    } else if (cond instanceof ua.woody.questborn.model.npc.mood.QuestCompletedCondition qcc) {
                                        cSec.set("quest_id", qcc.getQuestId());
                                    } else if (cond instanceof ua.woody.questborn.model.npc.mood.QuestActiveCondition qac) {
                                        cSec.set("quest_id", qac.getQuestId());
                                    } else if (cond instanceof ua.woody.questborn.model.npc.mood.PlaceholderCondition pc) {
                                        cSec.set("placeholder", pc.getPlaceholder());
                                        cSec.set("expression", pc.getExpression());
                                    } else if (cond instanceof ua.woody.questborn.model.npc.mood.ChanceCondition cc2) {
                                        cSec.set("percentage", cc2.getPercentage());
                                    }
                                }
                            }
                        }
                    }
                }
                config.save(configFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not save npcs.yml: " + e.getMessage());
            }
        });
    }

    public NpcConfig getConfigByNpcId(String npcId) {
        if (npcId == null) return null;
        NpcConfig config = npcs.get(npcId);
        if (config != null) return config;

        for (Map.Entry<String, NpcConfig> entry : npcs.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(npcId)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public NpcConfig getOrCreateConfig(String npcId) {
        if (!npcs.containsKey(npcId)) {
            npcs.put(npcId, new NpcConfig(npcId, "&0{npc} - Quests"));
        }
        return npcs.get(npcId);
    }

    public String getDisplayName(String npcId) {
        if (npcId == null || npcId.equals("none")) return "NPC";
        NpcConfig cfg = getConfigByNpcId(npcId);
        if (cfg != null && cfg.getDisplayName() != null) {
            return cfg.getDisplayName();
        }
        if (plugin.getNpcIntegrationManager() != null && plugin.getNpcIntegrationManager().getProvider() != null) {
            String pName = plugin.getNpcIntegrationManager().getProvider().getName(npcId);
            if (pName != null) return pName;
        }
        return npcId;
    }

    public Collection<NpcConfig> getAll() {
        return npcs.values();
    }

    public void reload() {
        loadNpcs();
    }

    public void addLink(String npcId, boolean isStart, boolean isQuest, String targetId) {
        NpcConfig npc = getOrCreateConfig(npcId);
        if (isStart) {
            if (isQuest) {
                if (npc.getStartsTypes().isEmpty() && !npc.getStartsQuests().contains(targetId)) {
                    npc.getStartsQuests().add(targetId);
                    saveNpcsAsync();
                }
            } else {
                if (!npc.getStartsTypes().contains(targetId)) {
                    npc.getStartsTypes().add(targetId);
                    npc.getStartsQuests().clear();
                    saveNpcsAsync();
                }
            }
        } else {
            if (isQuest) {
                if (npc.getFinishesTypes().isEmpty() && !npc.getFinishesQuests().contains(targetId)) {
                    npc.getFinishesQuests().add(targetId);
                    saveNpcsAsync();
                }
            } else {
                if (!npc.getFinishesTypes().contains(targetId)) {
                    npc.getFinishesTypes().add(targetId);
                    npc.getFinishesQuests().clear();
                    saveNpcsAsync();
                }
            }
        }
    }

    public void removeLink(String npcId, boolean isStart, boolean isQuest, String targetId) {
        NpcConfig npc = npcs.get(npcId);
        if (npc != null) {
            List<String> list = isStart ? (isQuest ? npc.getStartsQuests() : npc.getStartsTypes())
                                        : (isQuest ? npc.getFinishesQuests() : npc.getFinishesTypes());
            list.remove(targetId);
            if (npc.isEmpty()) {
                npcs.remove(npcId);
            }
            saveNpcsAsync();
        }
    }

    public void setPendingAction(UUID playerId, PendingNpcAction action) {
        if (action == null) {
            pendingActions.remove(playerId);
        } else {
            pendingActions.put(playerId, action);
        }
    }

    public PendingNpcAction getPendingAction(UUID playerId) {
        PendingNpcAction action = pendingActions.get(playerId);
        if (action != null && action.isExpired()) {
            pendingActions.remove(playerId);
            return null;
        }
        return action;
    }

    public void removeNpc(String npcId) {
        if (npcs.containsKey(npcId)) {
            npcs.remove(npcId);
            saveNpcsAsync();
        }
    }
}
