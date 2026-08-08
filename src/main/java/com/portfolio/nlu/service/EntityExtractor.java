package com.portfolio.nlu.service;

import com.portfolio.nlu.model.Entity;
import com.portfolio.nlu.util.DictionaryLoader;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Step 4 - Entity Extraction.
 *
 * Scans the normalized query for known business objects (asset class,
 * market cap, metric, company) using dictionary lookups from entities.json.
 * Longer phrases are matched first so e.g. "small cap" is not shadowed by
 * a shorter overlapping entry.
 */
@Service
public class EntityExtractor {

    private final DictionaryLoader dictionaryLoader;

    public EntityExtractor(DictionaryLoader dictionaryLoader) {
        this.dictionaryLoader = dictionaryLoader;
    }

    public List<Entity> extract(String normalizedQuery) {
        List<Entity> found = new ArrayList<>();

        for (Map.Entry<String, Map<String, String>> typeEntry : dictionaryLoader.getEntities().entrySet()) {
            String entityType = typeEntry.getKey();

            typeEntry.getValue().entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()))
                    .filter(candidate -> containsPhrase(normalizedQuery, candidate.getKey()))
                    .findFirst()
                    .ifPresent(match -> found.add(new Entity(entityType, match.getValue())));
        }

        return found;
    }

    private boolean containsPhrase(String text, String phrase) {
        return (" " + text + " ").contains(" " + phrase + " ");
    }
}
