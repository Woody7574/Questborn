package ua.woody.questborn.addons.api;

public interface QuestbornAddon {

    String id();

    void onLoad(AddonContext context);

    default void onEnable() {}

    default void onDisable() {}
}
