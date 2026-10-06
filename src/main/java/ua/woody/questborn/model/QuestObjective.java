package ua.woody.questborn.model;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import ua.woody.questborn.integration.ItemsAdderIntegration;
import ua.woody.questborn.integration.CraftEngineIntegration;

import java.lang.reflect.Method;
import java.util.*;

public class QuestObjective {
    private final QuestObjectiveType type;

    private final List<Material> targetMaterials;
    private final List<String> targetBlockIds;
    private final List<EntityType> targetEntities;
    private final List<String> targetCustomEntities;
    private final List<String> targetContainers;
    private final int amount;

    private final double distance;
    private final String region;
    private final String command;

    private final String message;
    private final String cause;

    private final double money;
    private final int xp;

    private final String item;
    private final String weapon;

    private final List<String> targetItems;

    private final Map<Material, String> potionTargets;

    private final double x, y, z;
    private final String world;
    private final String npc;
    private final ItemsAdderIntegration itemsAdderIntegration;
    private final CraftEngineIntegration craftEngineIntegration;

    private QuestObjective(Builder builder) {
        this.type = builder.type;
        this.targetMaterials = builder.targetMaterials;
        this.targetBlockIds = builder.targetBlockIds;
        this.targetEntities = builder.targetEntities;
        this.targetCustomEntities = builder.targetCustomEntities;
        this.targetItems = builder.targetItems;
        this.targetContainers = builder.targetContainers;
        this.potionTargets = builder.potionTargets;
        this.amount = builder.amount;
        this.distance = builder.distance;
        this.region = builder.region;
        this.command = builder.command;
        this.message = builder.message;
        this.cause = builder.cause;
        this.money = builder.money;
        this.xp = builder.xp;
        this.item = builder.item;
        this.weapon = builder.weapon;
        this.x = builder.x;
        this.y = builder.y;
        this.z = builder.z;
        this.world = builder.world;
        this.npc = builder.npc;
        this.itemsAdderIntegration = builder.itemsAdderIntegration;
        this.craftEngineIntegration = builder.craftEngineIntegration;
    }

    public static class Builder {
        private final QuestObjectiveType type;
        private int amount = 1;

        private List<Material> targetMaterials = new ArrayList<>();
        private List<String> targetBlockIds = new ArrayList<>();
        private List<EntityType> targetEntities = new ArrayList<>();
        private List<String> targetCustomEntities = new ArrayList<>();
        private List<String> targetItems = new ArrayList<>();
        private List<String> targetContainers = new ArrayList<>();
        private Map<Material, String> potionTargets = new HashMap<>();

        private double distance = 0;
        private String region = null;
        private String command = null;
        private String message = null;
        private String cause = null;
        private double money = 0;
        private int xp = 0;
        private String item = null;
        private String weapon = null;
        private double x = 0, y = 0, z = 0;
        private String world = null;
        private String npc = null;
        private ItemsAdderIntegration itemsAdderIntegration = null;
        private CraftEngineIntegration craftEngineIntegration = null;

        public Builder(QuestObjectiveType type) {
            this.type = type;
        }

        public Builder amount(int amount) {
            this.amount = amount;
            return this;
        }

        public Builder targetMaterials(List<Material> targetMaterials) {
            this.targetMaterials = targetMaterials != null ? targetMaterials : new ArrayList<>();
            return this;
        }

        public Builder targetBlockIds(List<String> targetBlockIds) {
            this.targetBlockIds = targetBlockIds != null ? targetBlockIds : new ArrayList<>();
            return this;
        }

        public Builder targetEntities(List<EntityType> targetEntities) {
            this.targetEntities = targetEntities != null ? targetEntities : new ArrayList<>();
            return this;
        }

        public Builder targetCustomEntities(List<String> targetCustomEntities) {
            this.targetCustomEntities = targetCustomEntities != null ? targetCustomEntities : new ArrayList<>();
            return this;
        }

        public Builder targetItems(List<String> targetItems) {
            this.targetItems = targetItems != null ? targetItems : new ArrayList<>();
            return this;
        }

        public Builder targetContainers(List<String> targetContainers) {
            this.targetContainers = targetContainers != null ? targetContainers : new ArrayList<>();
            return this;
        }

        public Builder potionTargets(Map<Material, String> potionTargets) {
            this.potionTargets = potionTargets != null ? potionTargets : new HashMap<>();
            return this;
        }

        public Builder distance(double distance) {
            this.distance = distance;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder command(String command) {
            this.command = command;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder cause(String cause) {
            this.cause = cause;
            return this;
        }

        public Builder money(double money) {
            this.money = money;
            return this;
        }

        public Builder xp(int xp) {
            this.xp = xp;
            return this;
        }

        public Builder item(String item) {
            this.item = item;
            return this;
        }

        public Builder weapon(String weapon) {
            this.weapon = weapon;
            return this;
        }

        public Builder location(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        public Builder world(String world) {
            this.world = world;
            return this;
        }

        public Builder npc(String npc) {
            this.npc = npc;
            return this;
        }

        public Builder itemsAdderIntegration(ItemsAdderIntegration itemsAdderIntegration) {
            this.itemsAdderIntegration = itemsAdderIntegration;
            return this;
        }

        public Builder craftEngineIntegration(CraftEngineIntegration craftEngineIntegration) {
            this.craftEngineIntegration = craftEngineIntegration;
            return this;
        }

        public QuestObjective build() {
            return new QuestObjective(this);
        }
    }

    public QuestObjectiveType getType() {
        return type;
    }

    public List<Material> getTargetMaterials() {
        return targetMaterials;
    }

    public List<String> getTargetBlockIds() {
        return targetBlockIds;
    }

    public List<EntityType> getTargetEntities() {
        return targetEntities;
    }

    public List<String> getTargetCustomEntities() {
        return targetCustomEntities;
    }

    public List<String> getTargetItems() {
        return targetItems;
    }

    public Map<Material, String> getPotionTargets() {
        return potionTargets;
    }

    public List<String> getTargetContainers() {
        return targetContainers;
    }

    public int getAmount() {
        return amount;
    }

    public double getDistance() {
        return distance;
    }

    public String getRegion() {
        return region;
    }

    public String getCommand() {
        return command;
    }

    public String getMessage() {
        return message;
    }

    public String getCause() {
        return cause;
    }

    public double getMoney() {
        return money;
    }

    public int getXp() {
        return xp;
    }

    public String getItem() {
        return item;
    }

    public String getWeapon() {
        return weapon;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String getWorld() {
        return world;
    }

    public String getNpc() {
        return npc;
    }

    public ItemsAdderIntegration getItemsAdderIntegration() {
        return itemsAdderIntegration;
    }

    public CraftEngineIntegration getCraftEngineIntegration() {
        return craftEngineIntegration;
    }

    public boolean isTargetMaterial(Material material) {
        if (material == null)
            return false;

        if (targetMaterials.isEmpty() && targetBlockIds.isEmpty())
            return true;

        if (targetMaterials.contains(material))
            return true;

        for (String raw : targetBlockIds) {
            if (raw == null || raw.isEmpty())
                continue;
            Material m = Material.matchMaterial(raw.toUpperCase(Locale.ROOT));
            if (m != null && m == material)
                return true;
        }

        return false;
    }

    public boolean isTargetBlock(org.bukkit.block.Block block) {
        if (block == null)
            return false;

        if (itemsAdderIntegration != null && itemsAdderIntegration.isEnabled()
                && itemsAdderIntegration.isCustomBlock(block)) {
            String iaId = itemsAdderIntegration.getCustomBlockId(block);
            if (iaId != null) {
                for (String expectedId : targetBlockIds) {
                    if (expectedId.equalsIgnoreCase("itemsadder:" + iaId) || expectedId.equalsIgnoreCase("ia:" + iaId) || expectedId.equalsIgnoreCase(iaId)) {
                        return true;
                    }
                }
                return false;
            }
        }

        if (craftEngineIntegration != null && craftEngineIntegration.isEnabled()
                && craftEngineIntegration.isCustomBlock(block)) {
            String ceId = craftEngineIntegration.getCustomBlockId(block);
            if (ceId != null) {
                for (String expectedId : targetBlockIds) {
                    if (expectedId.equalsIgnoreCase("craftengine:" + ceId) || expectedId.equalsIgnoreCase("ce:" + ceId)) {
                        return true;
                    }
                }
                return false;
            }
        }

        return isTargetMaterial(block.getType());
    }

    public boolean isTargetBlock(Material material) {
        return isTargetMaterial(material);
    }

    public boolean isTargetEntity(org.bukkit.entity.Entity entity) {
        if (entity == null) return false;

        boolean matchesVanilla = targetEntities.isEmpty() || targetEntities.contains(entity.getType());

        if (targetCustomEntities != null && !targetCustomEntities.isEmpty()) {
            String customId = null;

            if (itemsAdderIntegration != null && itemsAdderIntegration.isEnabled() && itemsAdderIntegration.isCustomMob(entity)) {
                customId = itemsAdderIntegration.getCustomMobId(entity);
            }

            else if (craftEngineIntegration != null && craftEngineIntegration.isEnabled() && craftEngineIntegration.isFurniture(entity)) {
                customId = craftEngineIntegration.getFurnitureId(entity);
            }

            if (customId != null) {
                for (String expectedId : targetCustomEntities) {
                    if (expectedId.equalsIgnoreCase(customId) ||
                        expectedId.equalsIgnoreCase("itemsadder:" + customId) ||
                        expectedId.equalsIgnoreCase("ia:" + customId) ||
                        expectedId.equalsIgnoreCase("craftengine:" + customId) ||
                        expectedId.equalsIgnoreCase("ce:" + customId)) {
                        return true;
                    }
                }
                return false;
            }
        }

        return matchesVanilla;
    }

    public boolean isTargetWeapon(Material weaponMat) {
        return weapon == null || weapon.isEmpty() || weapon.equalsIgnoreCase(weaponMat.name());
    }

    public boolean isTargetDamageCause(String damageCause) {
        if (message == null || message.isEmpty())
            return true;
        if (message.equalsIgnoreCase("ANY"))
            return true;

        return damageCause.equalsIgnoreCase(message) ||
                damageCause.toUpperCase(Locale.ROOT).contains(message.toUpperCase(Locale.ROOT)) ||
                message.toUpperCase(Locale.ROOT).contains(damageCause.toUpperCase(Locale.ROOT));
    }

    public boolean isTargetDimension(String dimension) {
        if (message == null || message.isEmpty())
            return true;
        return dimension.equalsIgnoreCase(message) ||
                dimension.toUpperCase(Locale.ROOT).contains(message.toUpperCase(Locale.ROOT)) ||
                message.toUpperCase(Locale.ROOT).contains(dimension.toUpperCase(Locale.ROOT));
    }

    public boolean isTargetContainerType(String containerType) {
        if (targetContainers == null || targetContainers.isEmpty()) {
            if (message == null || message.isEmpty())
                return true;
            if (message.equalsIgnoreCase("ANY"))
                return true;
            return containerType.equalsIgnoreCase(message) ||
                    containerType.toUpperCase(Locale.ROOT).contains(message.toUpperCase(Locale.ROOT));
        }

        for (String target : targetContainers) {
            if (target.equalsIgnoreCase("ANY"))
                return true;
            if (containerType.equalsIgnoreCase(target))
                return true;
        }
        return false;
    }

    public boolean meetsMinDamage(double damageDealt) {
        return damageDealt >= amount;
    }

    public boolean meetsMinFallDistance(float fallDistance) {
        return fallDistance >= amount;
    }

    public boolean isTargetItem(Material material) {
        if (material == null)
            return false;

        if (!targetMaterials.isEmpty() || !targetBlockIds.isEmpty()) {
            return isTargetMaterial(material);
        }

        if (targetItems != null && !targetItems.isEmpty()) {
            String materialName = material.name();
            boolean hadAnySimple = false;

            for (String raw : targetItems) {
                if (raw == null || raw.isBlank())
                    continue;
                String r = raw.trim();
                if (r.contains(":"))
                    continue;

                hadAnySimple = true;
                if (materialName.equalsIgnoreCase(r))
                    return true;
            }

            if (hadAnySimple)
                return false;
        }

        if (item != null && !item.isEmpty()) {
            return material.name().equalsIgnoreCase(item);
        }

        return true;
    }

    public boolean isTargetItem(ItemStack itemStack) {
        if (itemStack == null)
            return false;

        Material material = itemStack.getType();

        boolean hasAnyConstraints = (!targetMaterials.isEmpty() || !targetBlockIds.isEmpty()) ||
                (targetItems != null && !targetItems.isEmpty()) ||
                (item != null && !item.isEmpty()) ||
                (potionTargets != null && !potionTargets.isEmpty());

        if (!hasAnyConstraints)
            return true;

        if (itemsAdderIntegration != null && itemsAdderIntegration.isEnabled()
                && itemsAdderIntegration.isCustomItem(itemStack)) {
            String iaId = itemsAdderIntegration.getCustomItemId(itemStack);
            if (iaId != null) {
                for (String expectedId : targetBlockIds) {
                    if (expectedId.equalsIgnoreCase("itemsadder:" + iaId) || expectedId.equalsIgnoreCase("ia:" + iaId) || expectedId.equalsIgnoreCase(iaId)) {
                        return true;
                    }
                }
                if (targetItems != null) {
                    for (String expectedId : targetItems) {
                        if (expectedId.equalsIgnoreCase("itemsadder:" + iaId) || expectedId.equalsIgnoreCase("ia:" + iaId)
                                || expectedId.equalsIgnoreCase(iaId)) {
                            return true;
                        }
                    }
                }
                if (item != null && (item.equalsIgnoreCase("itemsadder:" + iaId) || item.equalsIgnoreCase("ia:" + iaId) || item.equalsIgnoreCase(iaId))) {
                    return true;
                }
                return false;
            }
        }

        if (craftEngineIntegration != null && craftEngineIntegration.isEnabled()
                && craftEngineIntegration.isCustomItem(itemStack)) {
            String ceId = craftEngineIntegration.getCustomItemId(itemStack);
            if (ceId != null) {
                for (String expectedId : targetBlockIds) {
                    if (expectedId.equalsIgnoreCase("craftengine:" + ceId) || expectedId.equalsIgnoreCase("ce:" + ceId)) {
                        return true;
                    }
                }
                if (targetItems != null) {
                    for (String expectedId : targetItems) {
                        if (expectedId.equalsIgnoreCase("craftengine:" + ceId) || expectedId.equalsIgnoreCase("ce:" + ceId)) {
                            return true;
                        }
                    }
                }
                if (item != null && (item.equalsIgnoreCase("craftengine:" + ceId) || item.equalsIgnoreCase("ce:" + ceId))) {
                    return true;
                }
                return false;
            }
        }

        if (!targetMaterials.isEmpty() || !targetBlockIds.isEmpty()) {
            if (!isTargetMaterial(material))
                return false;
        }

        if (isPotionMaterial(material)) {
            Set<Material> allowedPotionContainers = collectPotionContainersFromConstraints();
            if (!allowedPotionContainers.isEmpty() && !allowedPotionContainers.contains(material)) {
                return false;
            }
        }

        if (isPotionMaterial(material)) {
            List<String> expectedTypes = new ArrayList<>();

            if (potionTargets != null && !potionTargets.isEmpty()) {
                String byContainer = potionTargets.get(material);
                if (byContainer != null && !byContainer.isBlank())
                    expectedTypes.add(byContainer.trim());
            }

            expectedTypes.addAll(extractPotionSpecsFromTargetItems(material));

            if (!expectedTypes.isEmpty()) {
                for (String expected : expectedTypes) {
                    if (isPotionOfType(itemStack, expected))
                        return true;
                }
                return false;
            }
        }

        if ((targetMaterials == null || targetMaterials.isEmpty())
                && (targetBlockIds == null || targetBlockIds.isEmpty())) {
            if (targetItems != null && !targetItems.isEmpty()) {
                boolean hadAnySimple = false;

                for (String raw : targetItems) {
                    if (raw == null || raw.isBlank())
                        continue;
                    String r = raw.trim();
                    if (r.contains(":"))
                        continue;

                    hadAnySimple = true;
                    if (material.name().equalsIgnoreCase(r))
                        return true;
                }

                if (hadAnySimple)
                    return false;
            }
        }

        if (item != null && !item.isEmpty()) {
            return material.name().equalsIgnoreCase(item);
        }

        return true;
    }

    public boolean isTargetPotion(ItemStack potion) {
        return isTargetItem(potion);
    }

    private static final Set<Material> POTION_MATERIALS = EnumSet.of(
            Material.POTION,
            Material.SPLASH_POTION,
            Material.LINGERING_POTION,
            Material.TIPPED_ARROW);

    private boolean isPotionMaterial(Material m) {
        return m != null && POTION_MATERIALS.contains(m);
    }

    private Set<Material> collectPotionContainersFromConstraints() {
        Set<Material> out = EnumSet.noneOf(Material.class);

        if (potionTargets != null && !potionTargets.isEmpty()) {
            out.addAll(potionTargets.keySet());
        }

        if (targetItems != null && !targetItems.isEmpty()) {
            for (String raw : targetItems) {
                if (raw == null || raw.isBlank())
                    continue;
                String r = raw.trim();
                if (!r.contains(":"))
                    continue;

                String[] p = r.split(":", 2);
                String left = p[0].trim().toUpperCase(Locale.ROOT);
                Material m = Material.matchMaterial(left);
                if (m != null && isPotionMaterial(m))
                    out.add(m);
            }
        }

        return out;
    }

    private List<String> extractPotionSpecsFromTargetItems(Material containerMat) {
        if (targetItems == null || targetItems.isEmpty() || containerMat == null)
            return List.of();

        String matName = containerMat.name();
        List<String> out = new ArrayList<>();

        for (String raw : targetItems) {
            if (raw == null || raw.isBlank())
                continue;

            String r = raw.trim();
            if (!r.contains(":"))
                continue;

            String[] p = r.split(":", 2);
            String left = p[0].trim().toUpperCase(Locale.ROOT);
            String right = p[1].trim();

            if (left.equals(matName) && !right.isBlank()) {
                out.add(right);
            }
        }
        return out;
    }

    private enum Strength {
        NONE, STRONG, LONG
    }

    private static String normalizePotionKey(String raw) {
        if (raw == null)
            return "";
        return raw.trim()
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }

    private static class ParsedPotionSpec {
        final Strength strength;
        final String root;

        ParsedPotionSpec(Strength strength, String root) {
            this.strength = strength;
            this.root = root;
        }
    }

    private static final Map<String, String> POTION_ROOT_CANON = new HashMap<>();
    static {
        POTION_ROOT_CANON.put("HEAL", "HEALING");
        POTION_ROOT_CANON.put("HEALING", "HEALING");
        POTION_ROOT_CANON.put("INSTANT_HEAL", "HEALING");
        POTION_ROOT_CANON.put("INSTANT_HEALTH", "HEALING");

        POTION_ROOT_CANON.put("HARM", "HARMING");
        POTION_ROOT_CANON.put("HARMING", "HARMING");
        POTION_ROOT_CANON.put("INSTANT_DAMAGE", "HARMING");

        POTION_ROOT_CANON.put("REGEN", "REGENERATION");
        POTION_ROOT_CANON.put("REGENERATION", "REGENERATION");

        POTION_ROOT_CANON.put("SPEED", "SPEED");
        POTION_ROOT_CANON.put("SWIFTNESS", "SPEED");

        POTION_ROOT_CANON.put("JUMP", "JUMP");
        POTION_ROOT_CANON.put("LEAPING", "JUMP");
        POTION_ROOT_CANON.put("JUMP_BOOST", "JUMP");

        POTION_ROOT_CANON.put("NIGHTVISION", "NIGHT_VISION");
        POTION_ROOT_CANON.put("NIGHT_VISION", "NIGHT_VISION");

        POTION_ROOT_CANON.put("FIRE_RESIST", "FIRE_RESISTANCE");
        POTION_ROOT_CANON.put("FIRE_RESISTANCE", "FIRE_RESISTANCE");

        POTION_ROOT_CANON.put("WATER_BREATH", "WATER_BREATHING");
        POTION_ROOT_CANON.put("WATER_BREATHING", "WATER_BREATHING");

        POTION_ROOT_CANON.put("SLOW_FALL", "SLOW_FALLING");
        POTION_ROOT_CANON.put("SLOW_FALLING", "SLOW_FALLING");

        POTION_ROOT_CANON.put("INVISIBLE", "INVISIBILITY");
        POTION_ROOT_CANON.put("INVISIBILITY", "INVISIBILITY");

        POTION_ROOT_CANON.put("POISONED", "POISON");
        POTION_ROOT_CANON.put("POISON", "POISON");

        POTION_ROOT_CANON.put("WEAK", "WEAKNESS");
        POTION_ROOT_CANON.put("WEAKNESS", "WEAKNESS");

        POTION_ROOT_CANON.put("STRONG", "STRENGTH");
        POTION_ROOT_CANON.put("STRENGTH", "STRENGTH");

        POTION_ROOT_CANON.put("TURTLE", "TURTLE_MASTER");
        POTION_ROOT_CANON.put("TURTLE_MASTER", "TURTLE_MASTER");
    }

    private static String canonRoot(String maybeRoot) {
        String k = normalizePotionKey(maybeRoot);
        if (k.isEmpty())
            return "";
        return POTION_ROOT_CANON.getOrDefault(k, k);
    }

    private static ParsedPotionSpec parsePotionSpec(String raw) {
        String s = normalizePotionKey(raw);
        if (s.isEmpty() || s.equals("ANY"))
            return new ParsedPotionSpec(Strength.NONE, "ANY");

        Strength strength = Strength.NONE;

        if (s.startsWith("STRONG_")) {
            strength = Strength.STRONG;
            s = s.substring("STRONG_".length());
        } else if (s.startsWith("LONG_")) {
            strength = Strength.LONG;
            s = s.substring("LONG_".length());
        }

        s = s.replaceAll("(_II|_2|_III|_3)$", "");

        return new ParsedPotionSpec(strength, canonRoot(s));
    }

    private static class BasePotionInfo {
        final String typeName;
        final boolean extended;
        final boolean upgraded;

        BasePotionInfo(String typeName, boolean extended, boolean upgraded) {
            this.typeName = typeName;
            this.extended = extended;
            this.upgraded = upgraded;
        }
    }

    private static BasePotionInfo readBasePotionInfo(PotionMeta meta) {
        if (meta == null)
            return new BasePotionInfo("", false, false);

        String name = "";
        boolean ext = false;
        boolean upg = false;

        try {
            PotionType t = meta.getBasePotionData().getType();
            if (t != null)
                name = t.name();
        } catch (Throwable ignored) {
        }

        try {
            Method mGetData = meta.getClass().getMethod("getBasePotionData");
            Object data = mGetData.invoke(meta);
            if (data != null) {
                Method mGetType = data.getClass().getMethod("getType");
                Object typeObj = mGetType.invoke(data);
                if (typeObj instanceof Enum<?> en) {
                    if (name.isEmpty())
                        name = en.name();
                } else if (typeObj != null && name.isEmpty()) {
                    name = String.valueOf(typeObj);
                }

                Method mExt = data.getClass().getMethod("isExtended");
                Method mUpg = data.getClass().getMethod("isUpgraded");
                Object oExt = mExt.invoke(data);
                Object oUpg = mUpg.invoke(data);
                if (oExt instanceof Boolean b)
                    ext = b;
                if (oUpg instanceof Boolean b)
                    upg = b;
            }
        } catch (Throwable ignored) {
        }

        return new BasePotionInfo(name, ext, upg);
    }

    private static Strength inferStrength(BasePotionInfo info) {
        String n = normalizePotionKey(info.typeName);

        if (n.startsWith("STRONG_"))
            return Strength.STRONG;
        if (n.startsWith("LONG_"))
            return Strength.LONG;

        if (info.upgraded)
            return Strength.STRONG;
        if (info.extended)
            return Strength.LONG;

        return Strength.NONE;
    }

    private static String inferRoot(BasePotionInfo info) {
        String n = normalizePotionKey(info.typeName);
        if (n.startsWith("STRONG_"))
            n = n.substring("STRONG_".length());
        else if (n.startsWith("LONG_"))
            n = n.substring("LONG_".length());

        return canonRoot(n);
    }

    private boolean isPotionOfType(ItemStack stack, String target) {
        if (stack == null)
            return false;

        ParsedPotionSpec expected = parsePotionSpec(target);
        if ("ANY".equals(expected.root))
            return true;

        if (!(stack.getItemMeta() instanceof PotionMeta meta))
            return false;

        BasePotionInfo actualInfo = readBasePotionInfo(meta);
        String actualRoot = inferRoot(actualInfo);
        Strength actualStrength = inferStrength(actualInfo);

        if (actualRoot.isEmpty())
            return false;

        if (!actualRoot.equals(expected.root)) {
            String a = normalizePotionKey(actualRoot);
            String e = normalizePotionKey(expected.root);
            if (!(a.equals(e) || a.contains(e) || e.contains(a)))
                return false;
        }

        if (expected.strength == Strength.NONE)
            return true;
        return expected.strength == actualStrength;
    }

    @Override
    public String toString() {
        return "QuestObjective{" +
                "type=" + type +
                ", targetMaterials=" + targetMaterials +
                ", targetContainers=" + targetContainers +
                ", rawIds=" + targetBlockIds +
                ", entities=" + targetEntities +
                ", amount=" + amount +
                ", targetItems=" + targetItems +
                ", potionTargets=" + potionTargets +
                ", weapon='" + weapon + '\'' +
                ", message='" + message + '\'' +
                ", cause='" + cause + '\'' +
                '}';
    }
}
