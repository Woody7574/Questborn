package ua.woody.questborn.api.objective;

import ua.woody.questborn.model.QuestObjectiveType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

public class ObjectiveRegistry {
    private final Map<String, Class<?>> registry = new LinkedHashMap<>();

    public void register(String id, Class<?> clazz) {
        registry.put(id.toUpperCase(Locale.ROOT), clazz);
    }

    public Class<?> get(String id) {
        if (id == null) return null;
        return registry.get(id.toUpperCase(Locale.ROOT));
    }
}
