package ua.woody.questborn.model;

import java.util.UUID;

public class TopEntry implements Comparable<TopEntry> {
    private final UUID uuid;
    private final String name;
    private final int value;

    public TopEntry(UUID uuid, String name, int value) {
        this.uuid = uuid;
        this.name = name;
        this.value = value;
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

    @Override
    public int compareTo(TopEntry o) {
        return Integer.compare(o.value, this.value); // DESC
    }
}
