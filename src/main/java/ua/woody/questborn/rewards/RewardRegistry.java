package ua.woody.questborn.rewards;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class RewardRegistry {

    private final Map<String, RewardModule> modules = new LinkedHashMap<>();

    public void register(RewardModule module) {
        modules.put(module.getKey().toLowerCase(), module);
    }

    public RewardModule get(String key) {
        if (key == null) return null;
        return modules.get(key.toLowerCase());
    }

    public Collection<RewardModule> getAll() {
        return modules.values();
    }
}
