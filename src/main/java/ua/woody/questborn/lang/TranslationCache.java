package ua.woody.questborn.lang;

import java.util.HashMap;
import java.util.Map;

public class TranslationCache {

    private final Map<String, String> cachedTranslations = new HashMap<>();
    private final Map<String, String[]> cachedLists = new HashMap<>();

    /**
     * Отримує переклад з кешу або завантажує його
     */
    public String getTranslation(LanguageManager languageManager, String path) {
        String cacheKey = languageManager.getActiveLanguageCode() + ":" + path;

        if (cachedTranslations.containsKey(cacheKey)) {
            return cachedTranslations.get(cacheKey);
        }

        String translation = languageManager.tr(path);
        cachedTranslations.put(cacheKey, translation);

        return translation;
    }

    /**
     * Отримує список перекладів з кешу
     */
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

    /**
     * Очищає кеш
     */
    public void clear() {
        cachedTranslations.clear();
        cachedLists.clear();
    }

    /**
     * Очищає кеш для конкретної мови
     */
    public void clearForLanguage(String languageCode) {
        cachedTranslations.keySet().removeIf(key -> key.startsWith(languageCode + ":"));
        cachedLists.keySet().removeIf(key -> key.startsWith(languageCode + ":"));
    }
}