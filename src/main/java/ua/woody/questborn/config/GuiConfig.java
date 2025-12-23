package ua.woody.questborn.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import ua.woody.questborn.QuestbornPlugin;

import java.util.ArrayList;
import java.util.List;

public class GuiConfig {
    private final QuestbornPlugin plugin;
    private Material fillMaterial;
    private List<Integer> fillSlots;
    private boolean fillEnabled;

    public GuiConfig(QuestbornPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        FileConfiguration config = plugin.getConfig();

        // Завантажуємо матеріал для заповнення
        String materialStr = config.getString("gui.fill.material", "BROWN_STAINED_GLASS_PANE");
        try {
            this.fillMaterial = Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid fill material in config: " + materialStr + ", using BROWN_STAINED_GLASS_PANE");
            this.fillMaterial = Material.BROWN_STAINED_GLASS_PANE;
        }

        // Завантажуємо слоти для заповнення
        this.fillSlots = new ArrayList<>();
        if (config.contains("gui.fill.slots")) {
            if (config.isList("gui.fill.slots")) {
                // Формат списку: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 36, 37, 38, 39, 40, 41, 42, 43, 44]
                List<Integer> slots = config.getIntegerList("gui.fill.slots");
                this.fillSlots.addAll(slots);
            } else if (config.isString("gui.fill.slots")) {
                // Формат рядка: "0,1,2,3,4,5,6,7,8,9,36,37,38,39,40,41,42,43,44"
                String slotsStr = config.getString("gui.fill.slots", "");
                String[] slotsArray = slotsStr.split(",");
                for (String slotStr : slotsArray) {
                    try {
                        int slot = Integer.parseInt(slotStr.trim());
                        this.fillSlots.add(slot);
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid slot in gui.fill.slots: " + slotStr);
                    }
                }
            }
        }

        // Якщо слоти не вказані, використовуємо дефолтні
        if (this.fillSlots.isEmpty()) {
            // Дефолтні слоти для GUI 45 слотів
            for (int i = 0; i < 9; i++) this.fillSlots.add(i); // Верхній ряд
            for (int i = 36; i < 45; i++) this.fillSlots.add(i); // Нижній ряд
            this.fillSlots.addAll(List.of(9, 18, 27, 17, 26, 35)); // Бічні стовпці
        }

        this.fillEnabled = config.getBoolean("gui.fill.enabled", true);
    }

    public Material getFillMaterial() {
        return fillMaterial;
    }

    public List<Integer> getFillSlots() {
        return fillSlots;
    }

    public boolean isFillEnabled() {
        return fillEnabled;
    }

    public void reload() {
        load();
    }
}