package ua.woody.questborn.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GuiLayout {
    private final int rows;
    private final List<Integer> maskSlots;
    private final Map<String, List<Integer>> specialSlots;
    private final Map<String, GuiItemConfig> items;
    private final Map<String, Object> properties;

    public GuiLayout(int rows, List<Integer> maskSlots, Map<String, List<Integer>> specialSlots,
            Map<String, GuiItemConfig> items, Map<String, Object> properties) {
        this.rows = rows;
        this.maskSlots = maskSlots != null ? maskSlots : Collections.emptyList();
        this.specialSlots = specialSlots != null ? specialSlots : Collections.emptyMap();
        this.items = items != null ? items : Collections.emptyMap();
        this.properties = properties != null ? properties : Collections.emptyMap();
    }

    public int getRows() {
        return rows;
    }

    public int getSize() {
        return rows * 9;
    }

    public List<Integer> getMaskSlots() {
        return maskSlots;
    }

    public List<Integer> getSlots(String key) {
        return specialSlots.getOrDefault(key, Collections.emptyList());
    }

    public GuiItemConfig getItem(String key) {
        return items.get(key);
    }

    public Map<String, GuiItemConfig> getItems() {
        return items;
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public boolean getBoolean(String key, boolean def) {
        Object val = properties.get(key);
        if (val instanceof Boolean)
            return (Boolean) val;
        return def;
    }

    public int getInt(String key, int def) {
        Object val = properties.get(key);
        if (val instanceof Number)
            return ((Number) val).intValue();
        return def;
    }
}
