package com.portfolio.nlu.service;

import com.portfolio.nlu.model.Filter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Step 5 - Filter Extraction.
 *
 * Answers "under what conditions?" by pattern-matching date and amount
 * expressions in the normalized query, e.g. "last year" -> purchaseDate =
 * Last Year, "above 1 lakh" -> amount > 100000.
 */
@Service
public class FilterExtractor {

    private static final Pattern LAST_N_MONTHS = Pattern.compile("last (\\d+) month");
    private static final Pattern BEFORE_YEAR = Pattern.compile("before (\\d{4})");
    private static final Pattern AFTER_YEAR = Pattern.compile("after (\\d{4})");
    private static final Pattern AMOUNT_ABOVE = Pattern.compile("(?:above|greater than|more than|over) (\\d+(?:\\.\\d+)?) ?(lakh|crore|k)?");
    private static final Pattern AMOUNT_BELOW = Pattern.compile("(?:below|less than|under) (\\d+(?:\\.\\d+)?) ?(lakh|crore|k)?");

    // Matches an explicit calendar date like "1 jan 2026" or "15 august 2025",
    // used by PORTFOLIO_HISTORY ("Portfolio value on 1 Jan 2026").
    private static final Pattern EXPLICIT_DATE = Pattern.compile(
            "(\\d{1,2}) (jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]* (\\d{4})");

    private static final java.util.Map<String, String> MONTH_NUMBERS = java.util.Map.ofEntries(
            java.util.Map.entry("jan", "01"), java.util.Map.entry("feb", "02"),
            java.util.Map.entry("mar", "03"), java.util.Map.entry("apr", "04"),
            java.util.Map.entry("may", "05"), java.util.Map.entry("jun", "06"),
            java.util.Map.entry("jul", "07"), java.util.Map.entry("aug", "08"),
            java.util.Map.entry("sep", "09"), java.util.Map.entry("oct", "10"),
            java.util.Map.entry("nov", "11"), java.util.Map.entry("dec", "12")
    );

    public List<Filter> extract(String normalizedQuery) {
        List<Filter> filters = new ArrayList<>();

        extractDateFilters(normalizedQuery, filters);
        extractAmountFilters(normalizedQuery, filters);

        return filters;
    }

    private void extractDateFilters(String text, List<Filter> filters) {
        // Explicit calendar date, e.g. "on 1 jan 2026" -> asOfDate = 2026-01-01
        // (used for PORTFOLIO_HISTORY point-in-time snapshots)
        Matcher explicitDate = EXPLICIT_DATE.matcher(text);
        if (explicitDate.find()) {
            String day = String.format("%02d", Integer.parseInt(explicitDate.group(1)));
            String month = MONTH_NUMBERS.get(explicitDate.group(2));
            String year = explicitDate.group(3);
            filters.add(new Filter("asOfDate", "=", year + "-" + month + "-" + day));
        }

        if (text.contains("last year")) {
            filters.add(new Filter("purchaseDate", "=", "Last Year"));
        } else if (text.contains("last month")) {
            // Distinguishes "last month" (comparison period) from "last N months"
            filters.add(new Filter("comparePeriod", "=", "Last Month"));
        } else if (text.contains("this month")) {
            filters.add(new Filter("purchaseDate", "=", "This Month"));
        } else if (text.contains("today")) {
            filters.add(new Filter("purchaseDate", "=", "Today"));
        } else {
            Matcher lastNMonths = LAST_N_MONTHS.matcher(text);
            if (lastNMonths.find()) {
                filters.add(new Filter("purchaseDate", "=", "Last " + lastNMonths.group(1) + " Months"));
            }
        }

        Matcher before = BEFORE_YEAR.matcher(text);
        if (before.find()) {
            filters.add(new Filter("purchaseDate", "<", before.group(1)));
        }

        Matcher after = AFTER_YEAR.matcher(text);
        if (after.find()) {
            filters.add(new Filter("purchaseDate", ">", after.group(1)));
        }
    }

    private void extractAmountFilters(String text, List<Filter> filters) {
        Matcher above = AMOUNT_ABOVE.matcher(text);
        if (above.find()) {
            filters.add(new Filter("amount", ">", resolveAmount(above.group(1), above.group(2))));
        }

        Matcher below = AMOUNT_BELOW.matcher(text);
        if (below.find()) {
            filters.add(new Filter("amount", "<", resolveAmount(below.group(1), below.group(2))));
        }
    }

    private String resolveAmount(String number, String unit) {
        double base = Double.parseDouble(number);
        if (unit == null) {
            return String.valueOf((long) base);
        }
        double multiplier = switch (unit) {
            case "lakh" -> 100_000d;
            case "crore" -> 10_000_000d;
            case "k" -> 1_000d;
            default -> 1d;
        };
        return String.valueOf((long) (base * multiplier));
    }
}
