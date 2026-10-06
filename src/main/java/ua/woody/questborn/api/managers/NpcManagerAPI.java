package ua.woody.questborn.api.managers;

import ua.woody.questborn.model.NpcConfig;

import java.util.Collection;

public interface NpcManagerAPI {
    Collection<String> getAllNpcIds();
    NpcConfig getConfigByNpcId(String npcId);
    NpcConfig getOrCreateConfig(String npcId);
    String getDisplayName(String npcId);
    Collection<NpcConfig> getAll();
}
