package com.portfolio.nlu.service;

import com.portfolio.nlu.util.DictionaryLoader;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Step 3 - Intent Classification.
 *
 * Enhanced rule-based / keyword matching with scoring:
 *   - Each intent has multiple phrase-sets (keyword combinations)
 *   - Score matches based on keyword overlap and specificity
 *   - Return the intent with the highest score
 *   - Return UNKNOWN_INTENT if no match exceeds threshold
 *
 * Scoring logic:
 *   - Exact phrase match: 3 points per keyword in phrase-set
 *   - Partial phrase match: 1 point per keyword found
 *   - Highest score wins (deterministic, no ML/LLM)
 *
 * Advantages:
 *   - Handles ambiguous queries better (picks best match vs. first match)
 *   - Supports synonyms without duplicating intent definitions
 *   - Extensible: add new intents without modifying code
 */
@Service
public class IntentClassifier {

    public static final String UNKNOWN_INTENT = "UNKNOWN";
    private static final double MATCH_THRESHOLD = 0.6; // 60% keyword match required
    private static final Pattern WORD_BOUNDARY = Pattern.compile("\\b");

    private final DictionaryLoader dictionaryLoader;

    public IntentClassifier(DictionaryLoader dictionaryLoader) {
        this.dictionaryLoader = dictionaryLoader;
    }

    /**
     * Classifies a normalized query into the best matching intent.
     * Uses scoring to rank all potential matches; returns the highest-scoring intent.
     */
    public String classify(String normalizedQuery) {
        Map<String, List<List<String>>> intentKeywords = dictionaryLoader.getIntentKeywords();

        // Flatten the nested JSON structure: skip metadata keys (start with "_").
        // Collectors.toMap() defaults to HashMap, which would discard the
        // declaration order DictionaryLoader worked to preserve — use a
        // LinkedHashMap so that order survives into scoring below.
        Map<String, List<List<String>>> intents = intentKeywords.entrySet().stream()
                .filter(e -> !e.getKey().startsWith("_"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> b, LinkedHashMap::new));

        // LinkedHashMap (not HashMap): when two intents score identically,
        // Stream.max() below keeps whichever was encountered first, so the
        // first-declared (more specific) intent wins ties over a later,
        // more generic/catch-all intent. With a HashMap this tie-break would
        // depend on String#hashCode bucket order instead — effectively
        // arbitrary, and the reason some intents "randomly" never won.
        Map<String, Double> scores = new LinkedHashMap<>();

        for (Map.Entry<String, List<List<String>>> intentEntry : intents.entrySet()) {
            String intent = intentEntry.getKey();
            List<List<String>> phraseSets = intentEntry.getValue();

            for (List<String> phraseSet : phraseSets) {
                double score = scorePhrase(normalizedQuery, phraseSet);
                scores.put(intent, Math.max(scores.getOrDefault(intent, 0d), score));
            }
        }

        // Find best match
        return scores.entrySet().stream()
                .filter(e -> e.getValue() >= MATCH_THRESHOLD)
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(UNKNOWN_INTENT);
    }

    /**
     * Score a single phrase-set: how many of its keywords are present in the query?
     *
     * Full match (all keywords present): phrase.size() * 3 points
     * Partial match: 1 point per keyword found, normalized by phrase length
     */
    private double scorePhrase(String normalizedQuery, List<String> phraseSet) {
        if (phraseSet == null || phraseSet.isEmpty()) {
            return 0d;
        }

        long matchCount = phraseSet.stream()
                .filter(keyword -> containsWholeWord(normalizedQuery, keyword))
                .count();

        // Full match: all keywords present
        if (matchCount == phraseSet.size()) {
            return phraseSet.size() * 3.0;
        }

        // Partial match: normalize by phrase length
        return (double) matchCount / phraseSet.size();
    }

    /**
     * Whole-word containment: "on" does NOT match inside "months".
     * Uses word-boundary regex to ensure exact token matching.
     */
    private boolean containsWholeWord(String text, String phrase) {
        return Pattern.compile("\\b" + Pattern.quote(phrase) + "\\b")
                .matcher(text)
                .find();
    }
}
