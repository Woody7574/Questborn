package ua.woody.questborn.model;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QuestObjective {

    /** Тип завдання (BREAK, PLACE, KILL...) */
    private final QuestObjectiveType type;

    /**
     * 🎯 Єдине джерело правди для "цільового елемента":
     * це список Material, який ми будемо наповнювати з YAML ключа target-materials
     *
     * (назва поля лишилась targetBlocks для сумісності з твоїм кодом)
     */
    private final List<Material> targetBlocks;

    /** 🔥 Підтримка raw-id тільки як fallback */
    private final List<String> targetBlockIds;

    /** Цільові моби */
    private final List<EntityType> targetEntities;

    /** Контейнери для OPEN_CONTAINER */
    private final List<String> targetContainers;

    /** Скільки потрібно виконати */
    private final int amount;

    // ДОДАТКОВІ ПАРАМЕТРИ
    private final double distance;
    private final String region;
    private final String command;

    /** message — універсальний текст (sign, book, container…) */
    private final String message;

    /** cause — окремий фільтр для TELEPORT (COMMAND, PLUGIN, ENDER_PEARL…) */
    private final String cause;

    private final double money;
    private final int xp;

    /** Старий item target (legacy) */
    private final String item;

    /** Яка зброя повинна бути в руках */
    private final String weapon;

    /**
     * Спец-цілі (переважно для potion):
     *  - POTION:HEALING
     *  - SPLASH_POTION:REGEN
     *
     * Важливо: ти можеш зробити так, щоб парсер заповнював це теж з target-materials
     * (якщо рядок містить ":")
     */
    private final List<String> targetItems;

    private final double x, y, z;

    private QuestObjective(Builder builder) {
        this.type = builder.type;
        this.targetBlocks = builder.targetBlocks;
        this.targetBlockIds = builder.targetBlockIds;
        this.targetEntities = builder.targetEntities;
        this.targetItems = builder.targetItems;
        this.targetContainers = builder.targetContainers;
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
    }

    /* -------------------------------------------------------
     * Builder
     * ------------------------------------------------------- */
    public static class Builder {
        private final QuestObjectiveType type;
        private int amount = 1;

        private List<Material> targetBlocks = new ArrayList<>();
        private List<String> targetBlockIds = new ArrayList<>();
        private List<EntityType> targetEntities = new ArrayList<>();
        private List<String> targetItems = new ArrayList<>();
        private List<String> targetContainers = new ArrayList<>();

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

        public Builder(QuestObjectiveType type) {
            this.type = type;
        }

        public Builder amount(int amount) { this.amount = amount; return this; }

        /** ✅ Це і є "target-materials" у майбутньому сенсі */
        public Builder targetBlocks(List<Material> targetBlocks) {
            this.targetBlocks = targetBlocks != null ? targetBlocks : new ArrayList<>();
            return this;
        }

        /** raw-id fallback */
        public Builder targetBlockIds(List<String> targetBlockIds) {
            this.targetBlockIds = targetBlockIds != null ? targetBlockIds : new ArrayList<>();
            return this;
        }

        public Builder targetEntities(List<EntityType> targetEntities) {
            this.targetEntities = targetEntities != null ? targetEntities : new ArrayList<>();
            return this;
        }

        /** Спец-рядки (переважно potion типи) */
        public Builder targetItems(List<String> targetItems) {
            this.targetItems = targetItems != null ? targetItems : new ArrayList<>();
            return this;
        }

        public Builder targetContainers(List<String> targetContainers) {
            this.targetContainers = targetContainers != null ? targetContainers : new ArrayList<>();
            return this;
        }

        public Builder distance(double distance) { this.distance = distance; return this; }
        public Builder region(String region) { this.region = region; return this; }
        public Builder command(String command) { this.command = command; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder cause(String cause) { this.cause = cause; return this; }
        public Builder money(double money) { this.money = money; return this; }
        public Builder xp(int xp) { this.xp = xp; return this; }
        public Builder item(String item) { this.item = item; return this; }
        public Builder weapon(String weapon) { this.weapon = weapon; return this; }

        public Builder location(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
            return this;
        }

        public QuestObjective build() { return new QuestObjective(this); }
    }

    /* -------------------------------------------------------
     * Getters
     * ------------------------------------------------------- */
    public QuestObjectiveType getType(){return type;}

    /** ✅ Рекомендовано використовувати як "target-materials" */
    public List<Material> getTargetMaterials(){ return targetBlocks; }

    /** legacy */
    public List<Material> getTargetBlocks(){return targetBlocks;}
    public List<String> getTargetBlockIds(){return targetBlockIds;}

    public List<EntityType> getTargetEntities(){return targetEntities;}
    public List<String> getTargetItems(){return targetItems;}
    public List<String> getTargetContainers() {return targetContainers;}
    public int getAmount(){return amount;}

    public double getDistance(){return distance;}
    public String getRegion(){return region;}
    public String getCommand(){return command;}
    public String getMessage(){return message;}
    public String getCause(){return cause;}
    public double getMoney(){return money;}
    public int getXp(){return xp;}

    public String getItem(){return item;}
    public String getWeapon(){return weapon;}

    public double getX(){return x;}
    public double getY(){return y;}
    public double getZ(){return z;}

    /* -------------------------------------------------------
     * Unified "element" checks
     * ------------------------------------------------------- */

    /**
     * ✅ Універсальна перевірка Material для будь-яких типів,
     * де потрібен "елемент" (блок/предмет).
     */
    public boolean isTargetMaterial(Material material){
        if(material == null) return false;

        // якщо нічого не задано — приймаємо будь-що
        if(targetBlocks.isEmpty() && targetBlockIds.isEmpty())
            return true;

        if(targetBlocks.contains(material))
            return true;

        // fallback raw ids
        for(String raw : targetBlockIds){
            if(raw==null||raw.isEmpty()) continue;
            Material m = Material.matchMaterial(raw.toUpperCase(Locale.ROOT));
            if(m!=null && m==material) return true;
        }

        return false;
    }

    /** legacy alias */
    public boolean isTargetBlock(Material material){
        return isTargetMaterial(material);
    }

    /** Перевірка моба */
    public boolean isTargetEntity(EntityType type){
        return targetEntities.isEmpty() || targetEntities.contains(type);
    }

    /** Перевірка зброї */
    public boolean isTargetWeapon(Material weaponMat){
        return weapon==null || weapon.isEmpty() || weapon.equalsIgnoreCase(weaponMat.name());
    }

    /* -------------------------------------------------------
     * Special checks
     * ------------------------------------------------------- */

    public boolean isTargetDamageCause(String damageCause) {
        if (message == null || message.isEmpty()) return true;
        if (message.equalsIgnoreCase("ANY")) return true;

        return damageCause.equalsIgnoreCase(message) ||
                damageCause.toUpperCase(Locale.ROOT).contains(message.toUpperCase(Locale.ROOT)) ||
                message.toUpperCase(Locale.ROOT).contains(damageCause.toUpperCase(Locale.ROOT));
    }

    public boolean isTargetTeleportCause(String teleportCause) {
        String filter = (cause != null && !cause.isEmpty()) ? cause : message;

        if (filter == null || filter.isEmpty()) return true;
        if (filter.equalsIgnoreCase("ANY")) return true;

        String up = filter.toUpperCase(Locale.ROOT);
        String actual = teleportCause != null ? teleportCause.toUpperCase(Locale.ROOT) : "";
        return actual.equals(up) || actual.contains(up);
    }

    public boolean isTargetDimension(String dimension) {
        if (message == null || message.isEmpty()) return true;
        return dimension.equalsIgnoreCase(message) ||
                dimension.toUpperCase(Locale.ROOT).contains(message.toUpperCase(Locale.ROOT)) ||
                message.toUpperCase(Locale.ROOT).contains(dimension.toUpperCase(Locale.ROOT));
    }

    public boolean isTargetContainerType(String containerType) {
        if (targetContainers == null || targetContainers.isEmpty()) {
            if (message == null || message.isEmpty()) return true;
            if (message.equalsIgnoreCase("ANY")) return true;
            return containerType.equalsIgnoreCase(message) ||
                    containerType.toUpperCase(Locale.ROOT).contains(message.toUpperCase(Locale.ROOT));
        }

        for (String target : targetContainers) {
            if (target.equalsIgnoreCase("ANY")) return true;
            if (containerType.equalsIgnoreCase(target)) return true;
        }
        return false;
    }

    public boolean meetsMinDamage(double damageDealt) { return damageDealt >= amount; }
    public boolean meetsMinFallDistance(float fallDistance) { return fallDistance >= amount; }

    /* -------------------------------------------------------
     * Item targeting (Material + potions)
     * ------------------------------------------------------- */

    /**
     * ✅ Для будь-яких item/objective:
     * - спершу дивимось target-materials (Material list)
     * - потім legacy targetItems (якщо там лежать прості material назви)
     * - потім legacy item поле
     */
    public boolean isTargetItem(Material material) {
        if (material == null) return false;

        // 1) універсально через target-materials
        if (!targetBlocks.isEmpty() || !targetBlockIds.isEmpty()) {
            return isTargetMaterial(material);
        }

        // 2) legacy targetItems (простий матеріал)
        if (targetItems != null && !targetItems.isEmpty()) {
            String materialName = material.name();
            for (String raw : targetItems) {
                if (raw == null || raw.isBlank()) continue;
                if (!raw.contains(":") && materialName.equalsIgnoreCase(raw.trim())) return true;
            }
            // якщо були targetItems але жоден не підійшов — false
            return false;
        }

        // 3) legacy item
        if (item != null && !item.isEmpty()) {
            try {
                Material target = Material.valueOf(item.toUpperCase(Locale.ROOT));
                return material == target;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        return true;
    }

    /**
     * ✅ Повна перевірка ItemStack:
     * - target-materials (Material) працює завжди
     * - targetItems із форматом POTION:TYPE теж підтримується (але їх можна заповнювати з target-materials у парсері)
     */
    public boolean isTargetItem(ItemStack itemStack){
        if(itemStack==null) return false;

        Material material = itemStack.getType();

        // 1) якщо задано target-materials → це головний фільтр
        if (!targetBlocks.isEmpty() || !targetBlockIds.isEmpty()) {
            // якщо це potion-квест і ти хочеш "тільки potions", це має робити тип/логіка квесту,
            // але по матеріалу ми все одно фільтруємо тут
            if (!isTargetMaterial(material)) return false;
        }

        boolean potion = isPotionMaterial(material);

        // 2) спец-рядки (переважно potion типи)
        if (targetItems != null && !targetItems.isEmpty()) {
            boolean matchedAny = false;

            for (String raw : targetItems) {
                if (raw == null || raw.isBlank()) continue;

                String r = raw.trim();
                if (r.contains(":")) {
                    String[] p = r.split(":", 2);
                    String matName = p[0].trim().toUpperCase(Locale.ROOT);
                    String potionType = p[1].trim();

                    if (material.name().equals(matName)) {
                        matchedAny = true;
                        if (isPotionOfType(itemStack, potionType)) return true;
                    }
                } else {
                    if (material.name().equalsIgnoreCase(r)) return true;
                }
            }

            // якщо були спец-таргети — і жоден не співпав
            if (matchedAny || containsAnyPotionSpec(targetItems)) return false;
        }

        // 3) legacy item-target
        if(item!=null && !item.isEmpty())
            return material.name().equalsIgnoreCase(item);

        // Якщо нічого не задано — приймаємо
        return true;
    }

    private boolean containsAnyPotionSpec(List<String> list) {
        for (String s : list) {
            if (s != null && s.contains(":")) return true;
        }
        return false;
    }

    private boolean isPotionMaterial(Material m){
        String n = m.name();
        return n.contains("POTION") || n.contains("SPLASH") || n.contains("LINGERING");
    }

    private boolean isPotionOfType(ItemStack stack, String target){
        try {
            if(stack.getItemMeta() instanceof PotionMeta meta){
                PotionType t = meta.getBasePotionType();
                if(t == null) return false;

                String A = t.name();
                String B = target.toUpperCase(Locale.ROOT);
                if(A.equals(B)) return true;

                return switch(B){
                    case "NIGHT_VISION" -> A.equals("NIGHT_VISION");
                    case "STRENGTH" -> A.equals("STRENGTH");
                    case "HEALING","INSTANT_HEAL" -> A.equals("INSTANT_HEAL");
                    case "REGEN","REGENERATION" -> A.equals("REGENERATION");
                    case "SPEED","SWIFTNESS" -> A.equals("SPEED");
                    case "FIRE_RESISTANCE" -> A.equals("FIRE_RESISTANCE");
                    case "POISON" -> A.equals("POISON");
                    case "WEAKNESS" -> A.equals("WEAKNESS");
                    case "SLOWNESS" -> A.equals("SLOWNESS");
                    case "WATER_BREATHING" -> A.equals("WATER_BREATHING");
                    case "INVISIBILITY" -> A.equals("INVISIBILITY");
                    case "JUMP_BOOST","LEAPING" -> A.equals("JUMP_BOOST");
                    case "LUCK" -> A.equals("LUCK");
                    case "TURTLE_MASTER" -> A.equals("TURTLE_MASTER");
                    case "SLOW_FALLING" -> A.equals("SLOW_FALLING");
                    default -> A.contains(B) || B.contains(A);
                };
            }
        } catch(Exception ignored){}
        return false;
    }

    @Override public String toString(){
        return "QuestObjective{" +
                "type="+type+
                ", targetMaterials="+targetBlocks+
                ", targetContainers="+targetContainers+
                ", rawIds="+targetBlockIds+
                ", entities="+targetEntities+
                ", amount="+amount+
                ", targetItems="+targetItems+
                ", weapon='"+weapon+'\''+
                ", message='"+message+'\''+
                ", cause='"+cause+'\''+
                '}';
    }
}
