package ua.woody.questborn.model;

public class DialogueOption {
    private final String text;
    private final String material;
    private final String action;
    private final java.util.List<String> conditions;
    private final int slot;
    private final String sound;
    private final java.util.List<String> lore;

    public DialogueOption(String text, java.util.List<String> lore, String material, String action, java.util.List<String> conditions, int slot, String sound) {
        this.text = text;
        this.lore = lore != null ? lore : new java.util.ArrayList<>();
        this.material = material;
        this.action = action;
        this.conditions = conditions != null ? conditions : new java.util.ArrayList<>();
        this.slot = slot;
        this.sound = sound;
    }

    public String getText() { return text; }
    public java.util.List<String> getLore() { return lore; }
    public String getMaterial() { return material; }
    public String getAction() { return action; }
    public java.util.List<String> getConditions() { return conditions; }
    public int getSlot() { return slot; }
    public String getSound() { return sound; }
}
