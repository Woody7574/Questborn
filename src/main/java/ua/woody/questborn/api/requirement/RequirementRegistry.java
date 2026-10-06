package ua.woody.questborn.api.requirement;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

public class RequirementRegistry {
    private final Map<String, RequirementHandler> registry = new LinkedHashMap<>();

    public void register(String id, RequirementHandler handler) {
        registry.put(id.toUpperCase(Locale.ROOT), handler);
    }

    public RequirementHandler get(String id) {
        if (id == null) return null;
        return registry.get(id.toUpperCase(Locale.ROOT));
    }
}
