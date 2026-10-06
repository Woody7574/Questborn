package ua.woody.questborn.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.util.SmartYamlUpdater;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuConfig {
    private final QuestbornPlugin plugin;
    private final Map<String, GuiLayout> layouts = new HashMap<>();

    public MenuConfig(QuestbornPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "menus.yml");
        FileConfiguration config = SmartYamlUpdater.updateAndLoad(plugin, file, "menus.yml");
        layouts.clear();

        ConfigurationSection menus = config.getConfigurationSection("menus");
        if (menus == null)
            return;

        for (String key : menus.getKeys(false)) {
            ConfigurationSection section = menus.getConfigurationSection(key);
            if (section != null) {
                layouts.put(key, parseLayout(section));
            }
        }
    }

    private GuiLayout parseLayout(ConfigurationSection section) {
        int rows = section.getInt("rows", 6);

        List<Integer> maskSlots = parseSlotList(section, "mask");

        Map<String, List<Integer>> specialSlots = new HashMap<>();
        ConfigurationSection slotsSection = section.getConfigurationSection("slots");
        if (slotsSection != null) {
            for (String key : slotsSection.getKeys(false)) {
                specialSlots.put(key, parseSlotList(slotsSection, key));
            }
        }

        Map<String, GuiItemConfig> items = new HashMap<>();
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    items.put(key, parseItem(itemSection));
                }
            }
        }

        Map<String, Object> properties = new HashMap<>();
        for (String key : section.getKeys(false)) {
            if (!key.equals("rows") && !key.equals("mask") && !key.equals("slots") && !key.equals("items")) {
                properties.put(key, section.get(key));
            }
        }

        return new GuiLayout(rows, maskSlots, specialSlots, items, properties);
    }

    private GuiItemConfig parseItem(ConfigurationSection section) {
        String matName = section.getString("material", "STONE");
        String itemsAdderId = null;
        Material material = null;

        String lowerMat = matName.toLowerCase();
        if (lowerMat.startsWith("itemsadder:")) {
            itemsAdderId = matName.substring("itemsadder:".length());
            material = Material.STONE;
        } else if (lowerMat.startsWith("ia:")) {
            itemsAdderId = matName.substring("ia:".length());
            material = Material.STONE;
        } else if (lowerMat.startsWith("craftengine:")) {
            itemsAdderId = "ce:" + matName.substring("craftengine:".length());
            material = Material.STONE;
        } else if (lowerMat.startsWith("ce:")) {
            itemsAdderId = "ce:" + matName.substring("ce:".length());
            material = Material.STONE;
        } else {
            String cleanMat = matName.toUpperCase().replace("MINECRAFT:", "");
            try {
                material = Material.valueOf(cleanMat);
            } catch (IllegalArgumentException e) {
                material = Material.matchMaterial(cleanMat);
                if (material == null) {
                    material = Material.STONE;
                }
            }
        }

        String name = section.getString("name");
        List<String> lore = section.getStringList("lore");
        int slot = section.getInt("slot", -1);
        int customModelData = section.getInt("custom-model-data", 0);
        List<String> actions = section.getStringList("actions");
        String baseHead = section.getString("base-head");

        return new GuiItemConfig(material, itemsAdderId, name, lore, slot, customModelData, actions, baseHead);
    }

    private List<Integer> parseSlotList(ConfigurationSection section, String key) {
        List<Integer> result = new ArrayList<>();

        if (section.isList(key)) {
            List<String> rawList = section.getStringList(key);
            for (String s : rawList) {
                result.addAll(parseSlotRange(s));
            }
        } else if (section.isString(key)) {
            result.addAll(parseSlotRange(section.getString(key)));
        }

        return result;
    }

    private List<Integer> parseSlotRange(String rangeColor) {
        List<Integer> slots = new ArrayList<>();
        if (rangeColor == null || rangeColor.isEmpty())
            return slots;

        String[] parts = rangeColor.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                if (range.length == 2) {
                    try {
                        int start = Integer.parseInt(range[0].trim());
                        int end = Integer.parseInt(range[1].trim());
                        for (int i = start; i <= end; i++) {
                            slots.add(i);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else {
                try {
                    slots.add(Integer.parseInt(part));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return slots;
    }

    public GuiLayout getLayout(String name) {
        return layouts.get(name);
    }

    public void reload() {
        load();
    }
}
