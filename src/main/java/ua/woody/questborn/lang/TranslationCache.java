package ua.woody.questborn.lang;

import java.util.HashMap;
import java.util.Map;

public class TranslationCache {
    private final Map<String, String> cachedTranslations = new HashMap<>();
    private final Map<String, String[]> cachedLists = new HashMap<>();

    public String getTranslation(LanguageManager languageManager, String path) {
        String cacheKey = languageManager.getActiveLanguageCode() + ":" + path;

        if (cachedTranslations.containsKey(cacheKey)) {
            return cachedTranslations.get(cacheKey);
        }

        String translation = languageManager.tr(path);
        cachedTranslations.put(cacheKey, translation);

        return translation;
    }

    public String[] getTranslationList(LanguageManager languageManager, String path) {
        String cacheKey = languageManager.getActiveLanguageCode() + ":" + path;

        if (cachedLists.containsKey(cacheKey)) {
            return cachedLists.get(cacheKey);
        }

        java.util.List<String> list = languageManager.trList(path);
        String[] array = list.toArray(new String[0]);
        cachedLists.put(cacheKey, array);

        return array;
    }

    public void clear() {
        cachedTranslations.clear();
        cachedLists.clear();
    }

    public void clearForLanguage(String languageCode) {
        cachedTranslations.keySet().removeIf(key -> key.startsWith(languageCode + ":"));
        cachedLists.keySet().removeIf(key -> key.startsWith(languageCode + ":"));
    }
}
