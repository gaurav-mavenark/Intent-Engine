package com.portfolio.nlu.service;

import com.portfolio.nlu.util.DictionaryLoader;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Step 2 - Text Normalization.
 *
 * Turns free-form user text into a clean, canonical sentence that every
 * later stage (intent classification, entity/filter extraction) can match
 * against reliably.
 *
 * Pipeline, in order:
 *   1. lowercase
 *   2. strip punctuation
 *   3. expand abbreviations (mf -> mutual fund, yr -> year, ...)
 *   4. lightweight spell-correction against a known vocabulary
 *   5. remove stopwords (please, can, kindly, show me, ...)
 */
@Service
public class NormalizationService {

    // Known-good vocabulary used for spell correction. In a production
    // system this would be generated from the dictionaries themselves;
    // kept small and explicit here for predictability.
    private static final Set<String> VOCABULARY = Set.of(
            "portfolio", "value", "worth", "investment", "wealth", "holdings",
            "balance", "net", "mutual", "fund", "stock", "stocks", "large",
            "cap", "mid", "small", "multi", "flexi", "purchase", "purchased", "purchases", "buying", "last",
            "year", "month", "today", "show", "list", "my", "xirr", "cagr",
            "returns", "generate", "report", "download", "statement", "above",
            "below", "before", "after", "amount", "own", "composition",
            "allocation", "asset", "performance", "performing", "growth",
            "trend", "compare", "comparison", "summary", "current", "on",
            "sector", "sectors", "amc", "dividend", "dividends", "sale", "sales", "sip", "history", "by",
            "capital", "gain", "gains", "ltcg", "stcg", "tax", "taxable", "harvesting", "saving", "saver", "elss",
            "risk", "volatility", "beta", "diversification", "product", "instrument", "country", "geographic",
            "goal", "goals", "progress", "shortfall", "recommendation", "simulation", "retire", "income", "interest",
            "cashflow", "sips", "active", "upcoming", "missed", "notify", "alert", "alerts", "target", "email",
            "kyc", "nominee", "bank", "profile", "user", "account"
    );

    private static final int SPELL_CORRECTION_MAX_DISTANCE = 1;

    private final DictionaryLoader dictionaryLoader;
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();

    public NormalizationService(DictionaryLoader dictionaryLoader) {
        this.dictionaryLoader = dictionaryLoader;
    }

    public String normalize(String rawQuery) {
        String text = toLowerCase(rawQuery);
        text = removePunctuation(text);
        text = expandAbbreviations(text);
        text = correctSpelling(text);
        text = removeStopwords(text);
        return text.trim().replaceAll("\\s+", " ");
    }

    private String toLowerCase(String text) {
        return text == null ? "" : text.toLowerCase();
    }

    private String removePunctuation(String text) {
        return text.replaceAll("[^a-z0-9\\s]", " ");
    }

    private String expandAbbreviations(String text) {
        String[] tokens = text.trim().split("\\s+");
        return Arrays.stream(tokens)
                .map(token -> dictionaryLoader.getAbbreviations().getOrDefault(token, token))
                .collect(Collectors.joining(" "));
    }

    /**
     * Corrects small typos by snapping any out-of-vocabulary token to the
     * closest vocabulary word, but only within a distance of 1 so we don't
     * accidentally rewrite entity/company names (e.g. "hdfc") that simply
     * aren't in the general vocabulary.
     */
    private String correctSpelling(String text) {
        String[] tokens = text.trim().split("\\s+");
        StringBuilder corrected = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            corrected.append(VOCABULARY.contains(token) ? token : closestMatch(token)).append(" ");
        }
        return corrected.toString();
    }

    private String closestMatch(String token) {
        String best = token;
        int bestDistance = SPELL_CORRECTION_MAX_DISTANCE + 1;
        for (String candidate : VOCABULARY) {
            int distance = levenshtein.apply(token, candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return bestDistance <= SPELL_CORRECTION_MAX_DISTANCE ? best : token;
    }

    private String removeStopwords(String text) {
        String result = text;
        // remove multi-word stopword phrases first ("show me", "give me")
        List<String> phrases = dictionaryLoader.getStopwords().stream()
                .filter(w -> w.contains(" "))
                .toList();
        for (String phrase : phrases) {
            result = result.replaceAll("\\b" + phrase + "\\b", " ");
        }

        Set<String> singleWordStopwords = dictionaryLoader.getStopwords().stream()
                .filter(w -> !w.contains(" "))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        String[] tokens = result.trim().split("\\s+");
        return Arrays.stream(tokens)
                .filter(token -> !token.isBlank() && !singleWordStopwords.contains(token))
                .collect(Collectors.joining(" "));
    }
}
