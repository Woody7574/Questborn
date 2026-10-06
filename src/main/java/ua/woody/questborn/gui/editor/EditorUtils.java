package ua.woody.questborn.gui.editor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.gui.editor.ChatInputManager;
import ua.woody.questborn.model.QuestObjectiveType;

public class EditorUtils {
    public static java.util.List<String> combineLore(Object... elements) {
        java.util.List<String> lore = new java.util.ArrayList<>();
        for (Object elem : elements) {
            if (elem instanceof String) {
                lore.add((String) elem);
            } else if (elem instanceof java.util.List) {
                lore.addAll((java.util.List<String>) elem);
            }
        }
        return lore;
    }

    public static Material getDisplayMaterialForEntity(String typeName) {
        String upper;
        if (typeName == null) {
            return Material.EGG;
        }
        switch (upper = typeName.toUpperCase()) {
            case "MUSHROOM_COW": {
                return Material.matchMaterial((String)"MOOSHROOM_SPAWN_EGG");
            }
            case "SNOWMAN": {
                return Material.matchMaterial((String)"SNOW_GOLEM_SPAWN_EGG");
            }
            case "VILLAGER_GOLEM":
            case "IRON_GOLEM": {
                return Material.matchMaterial((String)"IRON_GOLEM_SPAWN_EGG");
            }
            case "ILLUSIONER": {
                return Material.matchMaterial((String)"VINDICATOR_SPAWN_EGG") != null ? Material.matchMaterial((String)"VINDICATOR_SPAWN_EGG") : Material.BOW;
            }
            case "GIANT": {
                return Material.ZOMBIE_HEAD;
            }
            case "ENDER_DRAGON": {
                return Material.DRAGON_HEAD;
            }
            case "WITHER": {
                return Material.WITHER_SKELETON_SKULL;
            }
            case "PLAYER": {
                return Material.PLAYER_HEAD;
            }
            case "ARMOR_STAND": {
                return Material.ARMOR_STAND;
            }
            case "BOAT": {
                return Material.OAK_BOAT;
            }
            case "MINECART": {
                return Material.MINECART;
            }
        }
        Material eggMat = Material.matchMaterial((String)(upper + "_SPAWN_EGG"));
        if (eggMat != null) {
            return eggMat;
        }
        if (upper.contains("ZOMBIE")) {
            return Material.ZOMBIE_HEAD;
        }
        if (upper.contains("SKELETON")) {
            return Material.SKELETON_SKULL;
        }
        if (upper.contains("CREEPER")) {
            return Material.CREEPER_HEAD;
        }
        if (upper.contains("PIGLIN")) {
            return Material.matchMaterial((String)"PIGLIN_HEAD") != null ? Material.matchMaterial((String)"PIGLIN_HEAD") : Material.ZOMBIE_HEAD;
        }
        if (upper.contains("BOAT")) {
            return Material.OAK_BOAT;
        }
        if (upper.contains("MINECART")) {
            return Material.MINECART;
        }
        return Material.EGG;
    }

    public static int getStageCount(YamlConfiguration config) {
        ConfigurationSection sec;
        if (config == null) {
            return 0;
        }
        if (config.isConfigurationSection("quest-path") && (sec = config.getConfigurationSection("quest-path")) != null && !sec.getKeys(false).isEmpty()) {
            return sec.getKeys(false).size();
        }
        if (config.isConfigurationSection("stages") && (sec = config.getConfigurationSection("stages")) != null && !sec.getKeys(false).isEmpty()) {
            return sec.getKeys(false).size();
        }
        if (config.contains("objective") || config.contains("required-materials")) {
            return 1;
        }
        return 0;
    }

    public static void saveQuestConfig(QuestbornPlugin plugin, YamlConfiguration config, File file) {
        try {
            config.save(file);
            plugin.getQuestManager().reload();
        }
        catch (IOException e) {
            plugin.getLogger().severe("Failed to save quest configuration to " + file.getName() + ": " + e.getMessage());
        }
    }

    public static List<String> getMandatoryObjectiveProperties(QuestObjectiveType type) {
        if (type == null) {
            return List.of("type");
        }
        ArrayList<String> list = new ArrayList<String>();
        list.add("type");
        switch (type) {
            case BLOCK_BREAK:
            case BLOCK_PLACE:
            case ITEM_CRAFT:
            case ITEM_SMELT:
            case ITEM_ENCHANT:
            case ITEM_FISH:
            case ITEM_COOK:
            case CONSUME_ITEM:
            case USE_ITEM:
            case INTERACT_BLOCK:
            case HOLD_ITEM:
            case DROP_ITEM:
            case FILL_BUCKET:
            case EMPTY_BUCKET:
            case TILL_SOIL:
            case PLANT_SEED:
            case HARVEST_CROP:
            case BONE_MEAL_USE:
            case STRIP_LOG:
            case WAX_OFF:
            case WAX_ON:
            case ITEM_REPAIR:
            case ITEM_RENAME:
            case ITEM_BREAK:
            case DYE_ITEM:
            case FILL_FUEL:
            case BURN_FUEL:
            case ENCHANT_TABLE_USE:
            case ANVIL_USE:
            case BREWING:
            case OPEN_CONTAINER:
            case WEAR_ARMOR: {
                list.add("amount");
                list.add("target-materials");
                break;
            }
            case KILL_ENTITY:
            case INTERACT_ENTITY:
            case ENTITY_RIDE:
            case TAME_ANIMAL:
            case BREED_ANIMALS:
            case MILK_COW:
            case SHEAR_SHEEP:
            case THROW_EGG:
            case DEAL_DAMAGE:
            case TAKE_DAMAGE: {
                list.add("amount");
                list.add("target-entities");
                break;
            }
            case TRAVEL_DISTANCE:
            case SPRINT_DISTANCE:
            case SWIM_DISTANCE:
            case CROUCH_DISTANCE:
            case FALL_DISTANCE:
            case BOAT_TRAVEL:
            case MINECART_TRAVEL:
            case ELYTRA_FLY: {
                list.add("distance");
                break;
            }
            case JUMP:
            case CROUCH:
            case POTION_SPLASH:
            case POTION_DRINK:
            case BEACON_ACTIVATE:
            case CONDUIT_ACTIVATE:
            case PLAYER_KILL:
            case ASSIST_KILL:
            case JOIN_SERVER:
            case PLAY_TIME:
            case SLEEP_IN_BED:
            case ENTER_BED:
            case LEVEL_UP_REACH:
            case LEVEL_UP_GAIN:
            case EXPERIENCE_ORB_PICKUP: {
                list.add("amount");
                break;
            }
            case CHANGE_DIMENSION: {
                list.add("amount");
                list.add("dimension");
                break;
            }
            case REACH_LOCATION: {
                list.add("world");
                list.add("x");
                list.add("y");
                list.add("z");
                list.add("radius");
                break;
            }
            case REACH_BIOME: {
                list.add("biome");
                break;
            }
            case ENTER_REGION:
            case LEAVE_REGION: {
                list.add("region");
                break;
            }
            case EXECUTE_COMMAND: {
                list.add("command");
                break;
            }
            case CHAT_MESSAGE: {
                list.add("message");
                break;
            }
            case NPC_INTERACT: {
                list.add("amount");
                list.add("npc");
                break;
            }
            case GIVE_ITEM_TO_NPC: {
                list.add("amount");
                list.add("target-materials");
                list.add("npc");
                break;
            }
            case VILLAGER_SELL_ITEM:
            case VILLAGER_BUY_ITEM: {
                list.add("amount");
                list.add("target-materials");
                break;
            }
        }
        return list;
    }

    public static List<String> getOptionalObjectiveProperties(QuestObjectiveType type) {
        ArrayList<String> list = new ArrayList<String>(List.of());
        if (type == null) {
            return list;
        }
        switch (type) {
            case GIVE_ITEM_TO_NPC: {
                list.add("required-materials");
                break;
            }
            case VILLAGER_SELL_ITEM:
            case VILLAGER_BUY_ITEM: {
                break;
            }
            case NPC_INTERACT: {
                list.add("npc");
                break;
            }
            case KILL_ENTITY:
            case DEAL_DAMAGE:
            case PLAYER_KILL: {
                list.add("weapon");
                break;
            }
            case ITEM_RENAME:
            case OPEN_CONTAINER:
            case RECEIVE_DAMAGE_TYPE: {
                list.add("message");
                break;
            }
            case CHAT_MESSAGE: {
                if (EditorUtils.getMandatoryObjectiveProperties(type).contains("message")) break;
                list.add("message");
                break;
            }
        }
        return list;
    }

    public static String formatGuiTitle(String prefix, Object idObj, String suffix) {
        Object displayId;
        int maxIdLen = 28 - prefix.length() - suffix.length();
        if (maxIdLen < 5) {
            maxIdLen = 5;
        }
        Object object = displayId = idObj == null ? "" : String.valueOf(idObj);
        if (((String)displayId).length() > maxIdLen) {
            displayId = ((String)displayId).substring(0, maxIdLen) + "..";
        }
        return "&0" + prefix + (String)displayId + suffix;
    }

    public static String truncateGuiTitle(String title) {
        if (title == null) {
            return "";
        }
        int maxLen = 30;
        StringBuilder result = new StringBuilder();
        int visibleCount = 0;
        for (int i = 0; i < title.length(); ++i) {
            char c = title.charAt(i);
            if ((c == '&' || c == '\u00a7' || c == '\u00a7') && i + 1 < title.length() && "0123456789a-fk-orA-FK-ORxX".indexOf(title.charAt(i + 1)) != -1) {
                result.append(c).append(title.charAt(i + 1));
                ++i;
                continue;
            }
            if (c == '<' && i + 8 < title.length() && title.charAt(i + 1) == '#' && title.charAt(i + 8) == '>') {
                result.append(title, i, i + 9);
                i += 8;
                continue;
            }
            if (visibleCount < maxLen) {
                result.append(c);
                ++visibleCount;
                continue;
            }
            if (visibleCount != maxLen) continue;
            result.append("..");
            break;
        }
        return result.toString();
    }

    public static Object parseYamlValue(String val) {
        if (val == null) {
            return null;
        }
        try {
            return Integer.parseInt(val);
        }
        catch (NumberFormatException e) {
            try {
                return Double.parseDouble(val);
            }
            catch (NumberFormatException e2) {
                if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false")) {
                    return Boolean.parseBoolean(val);
                }
                return val;
            }
        }
    }

    public static void populateDefaultObjectiveProperties(YamlConfiguration config, String path, QuestObjectiveType type) {
        if (type == null) {
            return;
        }
        List<String> props = EditorUtils.getMandatoryObjectiveProperties(type);
        for (String prop : props) {
            if (prop.equals("type") || config.contains(path + "." + prop)) continue;
            switch (prop) {
                case "amount": {
                    config.set(path + "." + prop, (Object)1);
                    break;
                }
                case "distance":
                case "min-distance": {
                    config.set(path + "." + prop, (Object)10);
                    break;
                }
                case "target-materials": {
                    config.set(path + "." + prop, List.of("STONE"));
                    break;
                }
                case "target-entities": {
                    config.set(path + "." + prop, List.of("ZOMBIE"));
                    break;
                }
                case "material":
                case "item":
                case "target-material": {
                    config.set(path + "." + prop, (Object)"STONE");
                    break;
                }
                case "entity":
                case "target-entity": {
                    config.set(path + "." + prop, (Object)"ZOMBIE");
                    break;
                }
                case "message": {
                    config.set(path + "." + prop, (Object)"Hello");
                    break;
                }
                case "npc": {
                    config.set(path + "." + prop, (Object)"npc_id");
                    break;
                }
                case "region": {
                    config.set(path + "." + prop, (Object)"region_name");
                    break;
                }
                case "biome": {
                    config.set(path + "." + prop, (Object)"PLAINS");
                    break;
                }
                case "dimension": {
                    config.set(path + "." + prop, (Object)"OVERWORLD");
                    break;
                }
                case "notify-interval": {
                    config.set(path + "." + prop, (Object)1);
                    break;
                }
                case "location": {
                    config.set(path + "." + prop + ".world", (Object)"world");
                    config.set(path + "." + prop + ".x", (Object)0);
                    config.set(path + "." + prop + ".y", (Object)64);
                    config.set(path + "." + prop + ".z", (Object)0);
                    config.set(path + "." + prop + ".radius", (Object)5);
                }
            }
        }
    }

    public static void handleNumericClick(InventoryClickEvent event, int currentValue, int min, int max, Player player, String promptMessage, ChatInputManager inputManager, Consumer<Integer> callback, Runnable openGui) {
        handleNumericClick(event, currentValue, min, max, 1, 10, null, player, promptMessage, inputManager, callback, openGui);
    }

    public static void handleNumericClick(InventoryClickEvent event, int currentValue, int min, int max, int step, int shiftStep, Integer defaultValue, Player player, String promptMessage, ChatInputManager inputManager, Consumer<Integer> callback, Runnable openGui) {
        if (event.getClick() == ClickType.DROP && defaultValue != null) {
            if (currentValue != defaultValue) {
                callback.accept(defaultValue);
                openGui.run();
            }
            return;
        }
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            inputManager.requestInputWithSuggestion(player, promptMessage, String.valueOf(currentValue), input -> {
                if (input != null) {
                    try {
                        int val = Integer.parseInt(input);
                        if (val >= min && val <= max) {
                            callback.accept(val);
                        }
                    }
                    catch (Exception exception) {
                    }
                }
                openGui.run();
            });
            return;
        }
        int change = 0;
        if (event.isLeftClick()) {
            change = event.isShiftClick() ? shiftStep : step;
        } else if (event.isRightClick()) {
            change = event.isShiftClick() ? -shiftStep : -step;
        }
        if (change != 0) {
            int newVal = currentValue + change;
            if (newVal < min) {
                newVal = min;
            }
            if (newVal > max) {
                newVal = max;
            }
            if (newVal != currentValue) {
                callback.accept(newVal);
                openGui.run();
            }
        }
    }

    public static void handleDoubleNumericClick(InventoryClickEvent event, double currentValue, double min, double max, Player player, String promptMessage, ChatInputManager inputManager, Consumer<Double> callback, Runnable openGui) {
        handleDoubleNumericClick(event, currentValue, min, max, 1.0, 10.0, null, player, promptMessage, inputManager, callback, openGui);
    }

    public static void handleDoubleNumericClick(InventoryClickEvent event, double currentValue, double min, double max, double step, double shiftStep, Double defaultValue, Player player, String promptMessage, ChatInputManager inputManager, Consumer<Double> callback, Runnable openGui) {
        if (event.getClick() == ClickType.DROP && defaultValue != null) {
            if (currentValue != defaultValue) {
                callback.accept(defaultValue);
                openGui.run();
            }
            return;
        }
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            inputManager.requestInputWithSuggestion(player, promptMessage, String.valueOf(currentValue), input -> {
                if (input != null) {
                    try {
                        double val = Double.parseDouble(input);
                        if (val >= min && val <= max) {
                            callback.accept(val);
                        }
                    }
                    catch (Exception exception) {
                    }
                }
                openGui.run();
            });
            return;
        }
        double change = 0.0;
        if (event.isLeftClick()) {
            change = event.isShiftClick() ? shiftStep : step;
        } else if (event.isRightClick()) {
            change = event.isShiftClick() ? -shiftStep : -step;
        }
        if (change != 0.0) {
            double newVal = currentValue + change;
            if (newVal < min) {
                newVal = min;
            }
            if (newVal > max) {
                newVal = max;
            }
            if (newVal != currentValue) {
                callback.accept(newVal);
                openGui.run();
            }
        }
    }

    public static String getCustomIdFromItem(ItemStack item, QuestbornPlugin plugin) {
        if (item == null || item.getType() == Material.AIR) {
            return "AIR";
        }
        if (plugin.getItemsAdderIntegration() != null && plugin.getItemsAdderIntegration().isEnabled() && plugin.getItemsAdderIntegration().isCustomItem(item)) {
            return "ia:" + plugin.getItemsAdderIntegration().getCustomItemId(item);
        }
        if (plugin.getCraftEngineIntegration() != null && plugin.getCraftEngineIntegration().isEnabled() && plugin.getCraftEngineIntegration().isCustomItem(item)) {
            return "ce:" + plugin.getCraftEngineIntegration().getCustomItemId(item);
        }
        return item.getType().name();
    }

    public static boolean isValidMaterial(String mat, QuestbornPlugin plugin) {
        if (mat == null || mat.isBlank()) {
            return false;
        }
        String m = mat.trim().toLowerCase(Locale.ROOT);
        if (m.startsWith("ia:") || m.startsWith("itemsadder:")) {
            return true;
        }
        if (m.startsWith("ce:") || m.startsWith("craftengine:")) {
            return true;
        }
        try {
            Material.valueOf((String)mat.trim().toUpperCase(Locale.ROOT));
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidEntity(String ent, QuestbornPlugin plugin) {
        if (ent == null || ent.isBlank()) {
            return false;
        }
        String e = ent.trim().toLowerCase(Locale.ROOT);
        if (e.startsWith("ia:") || e.startsWith("itemsadder:")) {
            return true;
        }
        if (e.startsWith("ce:") || e.startsWith("craftengine:")) {
            return true;
        }
        try {
            EntityType.valueOf((String)ent.trim().toUpperCase(Locale.ROOT));
            return true;
        }
        catch (Exception ex) {
            return false;
        }
    }

    public static ItemStack getGuiItemForString(String id, QuestbornPlugin plugin) {
        if (id == null || id.isBlank()) {
            return new ItemStack(Material.PAPER);
        }
        String s = id.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("ia:") || s.startsWith("itemsadder:")) {
            if (plugin.getItemsAdderIntegration() != null && plugin.getItemsAdderIntegration().isEnabled()) {
                String subId = s.startsWith("ia:") ? s.substring(3) : s.substring(11);
                ItemStack iaItem = plugin.getItemsAdderIntegration().getCustomItem(subId);
                if (iaItem != null) {
                    return iaItem;
                }
            }
            return new ItemStack(Material.PAPER);
        }
        if (s.startsWith("ce:") || s.startsWith("craftengine:")) {
            if (plugin.getCraftEngineIntegration() != null && plugin.getCraftEngineIntegration().isEnabled()) {
                String subId = s.startsWith("ce:") ? s.substring(3) : s.substring(12);
                ItemStack ceItem = plugin.getCraftEngineIntegration().getCustomItem(subId);
                if (ceItem != null) {
                    return ceItem;
                }
            }
            return new ItemStack(Material.PAPER);
        }
        try {
            Material mat = Material.valueOf((String)id.trim().toUpperCase(Locale.ROOT));
            return new ItemStack(mat);
        }
        catch (Exception e) {
            return new ItemStack(Material.PAPER);
        }
    }
}
