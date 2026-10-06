package ua.woody.questborn.model;

import java.util.UUID;

public class TopEntry implements Comparable<TopEntry> {
    private final UUID uuid;
    private final String name;
    private final int value;
    private final String skinTexture;
    private final String skinSignature;

    public TopEntry(UUID uuid, String name, int value) {
        this(uuid, name, value, null, null);
    }

    public TopEntry(UUID uuid, String name, int value, String skinTexture, String skinSignature) {
        this.uuid = uuid;
        this.name = name;
        this.value = value;
        this.skinTexture = skinTexture;
        this.skinSignature = skinSignature;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public String getSkinTexture() {
        return skinTexture;
    }

    public String getSkinSignature() {
        return skinSignature;
    }

    @Override
    public int compareTo(TopEntry o) {
        return Integer.compare(o.value, this.value);
    }
}
