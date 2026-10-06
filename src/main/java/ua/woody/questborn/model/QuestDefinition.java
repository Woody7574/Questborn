package ua.woody.questborn.model;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import ua.woody.questborn.effects.QuestEffects;
import ua.woody.questborn.lang.ColorFormatter;

import java.util.*;

public class QuestDefinition {
    private final String id;
    private final String displayName;
    private final String typeId;
    private final List<String> description;
    private final List<String> rewardsDescription;
    private final List<QuestStage> questStages;
    private final Map<Integer, QuestStage> stagesByNumber;
    private final long timeLimitSeconds;
    private final String seasonTag;
    private final QuestEffects effects;
    private final Map<String, Object> rewards;
    private final String iconItemsAdderId;
    private final String iconCraftEngineId;
    private final Material iconMaterial;
    private final Integer iconCustomModelData;
    private final String iconBaseHead;
    private final Integer slot;
    private final List<String> requiredQuests;
    private final String requiredPermission;
    private final Integer page;
    private final String rotationPool;

    private final int maxParticipants;
    private final int minParticipants;
    private final int personalLimit;
    private final int rewardStep;

    private long startedAt = System.currentTimeMillis();

    public QuestDefinition(
            String id,
            String name,
            String typeId,
            List<String> description,
            List<String> rewardsDescription,
            List<QuestStage> questStages,
            long timeLimitSeconds,
            String seasonTag,
            QuestEffects effects,
            Map<String, Object> rewards,
            String iconItemsAdderId,
            String iconCraftEngineId,
            Material iconMaterial,
            Integer iconCustomModelData,
            String iconBaseHead,
            Integer slot,
            List<String> requiredQuests,
            String requiredPermission,
            Integer page,
            String rotationPool,
            int maxParticipants,
            int minParticipants,
            int personalLimit,
            int rewardStep) {
        this.id = id;
        this.displayName = applyHexColor(name);
        this.typeId = typeId;
        this.description = description;
        this.rewardsDescription = rewardsDescription;
        this.questStages = questStages != null ? questStages : new ArrayList<>();
        this.timeLimitSeconds = timeLimitSeconds;
        this.seasonTag = seasonTag;
        this.effects = effects;
        this.rewards = rewards;
        this.iconItemsAdderId = iconItemsAdderId;
        this.iconCraftEngineId = iconCraftEngineId;
        this.iconMaterial = (iconMaterial != null) ? iconMaterial : Material.BOOK;
        this.iconCustomModelData = iconCustomModelData;
        this.iconBaseHead = iconBaseHead;
        this.slot = slot;
        this.requiredQuests = requiredQuests;
        this.requiredPermission = requiredPermission;
        this.page = page;
        this.rotationPool = rotationPool;
        this.maxParticipants = maxParticipants;
        this.minParticipants = minParticipants;
        this.personalLimit = personalLimit;
        this.rewardStep = rewardStep;

        this.stagesByNumber = new HashMap<>();
        for (int i = 0; i < this.questStages.size(); i++) {
            this.stagesByNumber.put(i + 1, this.questStages.get(i));
        }

        this.startedAt = System.currentTimeMillis();
    }

    public QuestDefinition(
            String id,
            String name,
            String typeId,
            List<String> description,
            List<String> rewardsDescription,
            List<QuestStage> questStages,
            long timeLimitSeconds,
            String seasonTag,
            QuestEffects effects,
            Map<String, Object> rewards,
            String iconItemsAdderId,
            String iconCraftEngineId,
            Material iconMaterial,
            Integer iconCustomModelData,
            String iconBaseHead,
            Integer slot,
            List<String> requiredQuests,
            String requiredPermission,
            Integer page,
            String rotationPool,
            int maxParticipants,
            int personalLimit,
            int rewardStep) {
        this(id, name, typeId, description, rewardsDescription, questStages,
                timeLimitSeconds, seasonTag, effects, rewards, iconItemsAdderId, iconCraftEngineId, iconMaterial,
                iconCustomModelData, iconBaseHead, slot, requiredQuests, requiredPermission, page, rotationPool, maxParticipants, 0, personalLimit, rewardStep);
    }

    public QuestDefinition(
            String id,
            String name,
            String typeId,
            List<String> description,
            List<String> rewardsDescription,
            QuestObjective objective,
            long timeLimitSeconds,
            String seasonTag,
            QuestEffects effects,
            Map<String, Object> rewards,
            String iconItemsAdderId,
            String iconCraftEngineId,
            Material iconMaterial,
            Integer iconCustomModelData,
            String iconBaseHead,
            Integer slot,
            List<String> requiredQuests,
            String requiredPermission,
            Integer page,
            Map<Material, Integer> requiredMaterials) {
        this(id, name, typeId, description, rewardsDescription, null,
                timeLimitSeconds, seasonTag, effects, rewards, iconItemsAdderId, iconCraftEngineId, iconMaterial,
                iconCustomModelData, iconBaseHead, slot, requiredQuests, requiredPermission, page, "default", 0, 0, 0, 0);

        List<QuestStage> stages = new ArrayList<>();
        if (objective != null) {
            stages.add(new QuestStage(objective));
        }
        if (requiredMaterials != null && !requiredMaterials.isEmpty()) {
            Map<QuestItem, Integer> items = new HashMap<>();
            for (Map.Entry<Material, Integer> entry : requiredMaterials.entrySet()) {
                items.put(new QuestItem(entry.getKey()), entry.getValue());
            }
            stages.add(new QuestStage(items));
        }

        this.questStages.addAll(stages);

        for (int i = 0; i < this.questStages.size(); i++) {
            this.stagesByNumber.put(i + 1, this.questStages.get(i));
        }
    }

    public QuestDefinition(
            String id,
            String name,
            String typeId,
            List<String> description,
            List<String> rewardsDescription,
            QuestObjective objective,
            long timeLimitSeconds,
            String seasonTag,
            QuestEffects effects,
            Map<String, Object> rewards,
            String iconItemsAdderId,
            String iconCraftEngineId,
            Material iconMaterial,
            Integer iconCustomModelData,
            String iconBaseHead) {
        this(id, name, typeId, description, rewardsDescription, objective,
                timeLimitSeconds, seasonTag, effects, rewards, iconItemsAdderId, iconCraftEngineId, iconMaterial,
                iconCustomModelData, iconBaseHead, null, null, null, null, null);
    }

    public QuestDefinition(
            String id,
            String name,
            String typeId,
            List<String> description,
            List<String> rewardsDescription,
            QuestObjective objective,
            long timeLimitSeconds,
            String seasonTag,
            QuestEffects effects,
            Map<String, Object> rewards,
            String iconItemsAdderId,
            String iconCraftEngineId,
            Material iconMaterial,
            Integer iconCustomModelData,
            String iconBaseHead,
            Integer slot,
            List<String> requiredQuests,
            String requiredPermission) {
        this(id, name, typeId, description, rewardsDescription, objective,
                timeLimitSeconds, seasonTag, effects, rewards, iconItemsAdderId, iconCraftEngineId, iconMaterial,
                iconCustomModelData, iconBaseHead, slot, requiredQuests, requiredPermission, null, null);
    }

    public QuestDefinition(
            String id,
            String name,
            String typeId,
            List<String> description,
            List<String> rewardsDescription,
            QuestObjective objective,
            long timeLimitSeconds,
            String seasonTag,
            QuestEffects effects,
            Map<String, Object> rewards,
            String iconItemsAdderId,
            String iconCraftEngineId,
            Material iconMaterial,
            Integer iconCustomModelData,
            String iconBaseHead,
            Integer slot,
            List<String> requiredQuests,
            String requiredPermission,
            Integer page) {
        this(id, name, typeId, description, rewardsDescription, objective,
                timeLimitSeconds, seasonTag, effects, rewards, iconItemsAdderId, iconCraftEngineId, iconMaterial,
                iconCustomModelData, iconBaseHead, slot, requiredQuests, requiredPermission, page, null);
    }

    public void markStarted() {
        this.startedAt = System.currentTimeMillis();
    }

    public boolean startedRecently(long millis) {
        return System.currentTimeMillis() - startedAt < millis;
    }

    public boolean startedRecently() {
        return startedRecently(1500);
    }

    public Integer getPage() {
        return page;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTypeId() {
        return typeId;
    }

    public List<String> getDescription() {
        return description;
    }

    public List<String> getRewardsDescription() {
        return rewardsDescription;
    }

    public long getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public String getSeasonTag() {
        return seasonTag;
    }

    public Map<String, Object> getRewards() {
        return rewards;
    }

    public QuestEffects getEffects() {
        return effects;
    }

    public String getIconItemsAdderId() {
        return iconItemsAdderId;
    }

    public String getIconCraftEngineId() {
        return iconCraftEngineId;
    }

    public Material getIconMaterial() {
        return iconMaterial;
    }

    public Integer getIconCustomModelData() {
        return iconCustomModelData;
    }

    public String getIconBaseHead() {
        return iconBaseHead;
    }

    public Integer getSlot() {
        return slot;
    }

    public List<String> getRequiredQuests() {
        return requiredQuests;
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }

    public List<QuestStage> getQuestStages() {
        return questStages;
    }

    public QuestStage getStage(int stageNumber) {
        return stagesByNumber.get(stageNumber);
    }

    public boolean hasStages() {
        return !questStages.isEmpty();
    }

    public int getStageCount() {
        return questStages.size();
    }

    public boolean hasRequiredMaterials() {
        for (QuestStage stage : questStages) {
            if (stage.isRequiredMaterials()) {
                return true;
            }
        }
        return false;
    }

    public Map<QuestItem, Integer> getRequiredItems() {
        for (QuestStage stage : questStages) {
            if (stage.isRequiredMaterials()) {
                return stage.getRequiredItems();
            }
        }
        return Map.of();
    }

    public QuestObjective getObjective() {
        for (QuestStage stage : questStages) {
            if (stage.isObjective()) {
                return stage.getObjective();
            }
        }
        return null;
    }

    public String getRotationPool() {
        return rotationPool;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public int getMinParticipants() {
        return minParticipants;
    }

    public int getPersonalLimit() {
        return personalLimit;
    }

    public int getRewardStep() {
        return rewardStep;
    }

    private String applyHexColor(String input) {
        return ColorFormatter.applyColors(input);
    }
}
