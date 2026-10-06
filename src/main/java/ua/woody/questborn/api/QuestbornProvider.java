package ua.woody.questborn.api;

public class QuestbornProvider {
    private static QuestbornAPI instance;

    public static void set(QuestbornAPI api) {
        if (instance != null) {
            throw new IllegalStateException("QuestbornAPI is already initialized!");
        }
        instance = api;
    }

    public static QuestbornAPI get() {
        if (instance == null) {
            throw new IllegalStateException("QuestbornAPI is not initialized yet!");
        }
        return instance;
    }
}
