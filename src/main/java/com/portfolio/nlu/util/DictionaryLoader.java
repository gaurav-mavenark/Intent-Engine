package com.portfolio.nlu.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads every dictionary the pipeline needs (abbreviations, stopwords,
 * entity lookups, intent keyword sets) from src/main/resources/dictionaries
 * exactly once at startup, so business analysts can edit the JSON files
 * without touching Java code.
 */
@Component
public class DictionaryLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, String> abbreviations;
    private List<String> stopwords;
    private Map<String, Map<String, String>> entities;
    private Map<String, List<List<String>>> intentKeywords;

    @PostConstruct
    public void load() throws IOException {
        abbreviations = readJson("dictionaries/abbreviations.json",
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));

        stopwords = readJson("dictionaries/stopwords.json",
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

        entities = readJson("dictionaries/entities.json",
                objectMapper.getTypeFactory().constructMapType(Map.class,
                        objectMapper.getTypeFactory().constructType(String.class),
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class)));

        // Read the nested JSON structure: outer Map<String, Object> allows both
        // direct intent arrays (_PORTFOLIO_CATEGORY -> intent arrays) and
        // metadata strings (_comment, _description, etc.)
        Map<String, Object> rawIntentMap = readJson("dictionaries/intent-keywords.json",
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));

        // Flatten nested structure: extract intents from category groups + direct intents
        intentKeywords = flattenIntentKeywords(rawIntentMap);
    }

    private <T> T readJson(String classpathLocation, com.fasterxml.jackson.databind.JavaType type) throws IOException {
        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
            return objectMapper.readValue(in, type);
        }
    }

    public Map<String, String> getAbbreviations() {
        return abbreviations;
    }

    public List<String> getStopwords() {
        return stopwords;
    }

    public Map<String, Map<String, String>> getEntities() {
        return entities;
    }

    /**
     * Flattens the nested intent-keywords structure:
     * skips metadata keys (start with _) and merges category groups into a flat Map.
     *
     * Input structure (nested):
     *   {
     *     "_PORTFOLIO_CATEGORY": { "PORTFOLIO_VALUE": [...], "PORTFOLIO_SUMMARY": [...] },
     *     "_HOLDINGS_CATEGORY": { "HOLDINGS_LIST": [...], ... },
     *     "LEGACY_INTENT": [...]  // direct intent (no category group)
     *   }
     *
     * Output (flattened):
     *   {
     *     "PORTFOLIO_VALUE": [...],
     *     "PORTFOLIO_SUMMARY": [...],
     *     "HOLDINGS_LIST": [...],
     *     "LEGACY_INTENT": [...]
     *   }
     */
    private Map<String, List<List<String>>> flattenIntentKeywords(Map<String, Object> rawIntentMap) {
        // LinkedHashMap preserves the order intents are declared in the JSON.
        // IntentClassifier relies on this order to break scoring ties in favor
        // of whichever intent is declared first (more specific intents should
        // be declared before generic/catch-all ones) — a plain HashMap would
        // silently discard that order and make tie-breaking non-deterministic.
        Map<String, List<List<String>>> flattened = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : rawIntentMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Skip metadata (comments, descriptions)
            if (key.startsWith("_comment") || key.startsWith("_description")) {
                continue;
            }

            // If value is a Map, it's a category group: extract intents from it
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> categoryGroup = (Map<String, Object>) value;
                for (Map.Entry<String, Object> catEntry : categoryGroup.entrySet()) {
                    String intentName = catEntry.getKey();
                    Object intentValue = catEntry.getValue();
                    if (intentValue instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<List<String>> phraseSets = (List<List<String>>) intentValue;
                        flattened.put(intentName, phraseSets);
                    }
                }
            }
            // If value is a List, it's a direct intent (legacy structure)
            else if (value instanceof List && key.startsWith("_")) {
                // Skip metadata category groups
                continue;
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<List<String>> phraseSets = (List<List<String>>) value;
                flattened.put(key, phraseSets);
            }
        }

        return flattened;
    }

    public Map<String, List<List<String>>> getIntentKeywords() {
        return intentKeywords;
    }
}
