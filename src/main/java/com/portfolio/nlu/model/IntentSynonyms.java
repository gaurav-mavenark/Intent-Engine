package com.portfolio.nlu.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Reusable synonym groups to avoid duplication in intent-keywords.json.
 * Follows DRY principle: define common terms once, reference in dictionaries.
 */
public class IntentSynonyms {

    // Asset/Instrument synonyms
    public static final Set<String> FUND_SYNONYMS = new HashSet<>(Arrays.asList(
            "fund", "mutual fund", "mf", "scheme", "mutual", "fund scheme"
    ));

    public static final Set<String> EQUITY_SYNONYMS = new HashSet<>(Arrays.asList(
            "equity", "stock", "stocks", "share", "shares"
    ));

    public static final Set<String> TRANSACTION_SYNONYMS = new HashSet<>(Arrays.asList(
            "transaction", "txn", "activity", "event"
    ));

    // Action synonyms
    public static final Set<String> SHOW_SYNONYMS = new HashSet<>(Arrays.asList(
            "show", "display", "list", "view", "see", "get", "fetch"
    ));

    public static final Set<String> COMPARE_SYNONYMS = new HashSet<>(Arrays.asList(
            "compare", "comparison", "versus", "vs", "against"
    ));

    public static final Set<String> SEARCH_SYNONYMS = new HashSet<>(Arrays.asList(
            "search", "find", "look", "filter"
    ));

    public static final Set<String> HISTORY_SYNONYMS = new HashSet<>(Arrays.asList(
            "history", "historical", "past", "record", "log"
    ));

    // Time period synonyms
    public static final Set<String> SUMMARY_SYNONYMS = new HashSet<>(Arrays.asList(
            "summary", "overview", "snapshot", "brief"
    ));

    public static final Set<String> DETAILS_SYNONYMS = new HashSet<>(Arrays.asList(
            "details", "detailed", "info", "information", "breakdown"
    ));

    public static final Set<String> PERFORMANCE_SYNONYMS = new HashSet<>(Arrays.asList(
            "performance", "performing", "returns", "yield"
    ));

    // Metric synonyms
    public static final Set<String> RETURN_SYNONYMS = new HashSet<>(Arrays.asList(
            "return", "returns", "yield", "income"
    ));

    public static final Set<String> GAIN_LOSS_SYNONYMS = new HashSet<>(Arrays.asList(
            "gain", "loss", "gainloss", "pnl", "profit", "loss", "unrealized"
    ));

    public static final Set<String> TOP_SYNONYMS = new HashSet<>(Arrays.asList(
            "top", "highest", "best", "maximum", "max"
    ));

    public static final Set<String> BOTTOM_SYNONYMS = new HashSet<>(Arrays.asList(
            "bottom", "lowest", "worst", "minimum", "min"
    ));

    // Organization/Category synonyms
    public static final Set<String> SECTOR_SYNONYMS = new HashSet<>(Arrays.asList(
            "sector", "sector wise", "sectoral", "industry"
    ));

    public static final Set<String> AMC_SYNONYMS = new HashSet<>(Arrays.asList(
            "amc", "fund house", "mutual fund company", "asset management"
    ));

    public static final Set<String> ALLOCATION_SYNONYMS = new HashSet<>(Arrays.asList(
            "allocation", "allocate", "distribute", "break down"
    ));

    public static final Set<String> CONCENTRATION_SYNONYMS = new HashSet<>(Arrays.asList(
            "concentration", "concentrated", "exposure"
    ));
}
