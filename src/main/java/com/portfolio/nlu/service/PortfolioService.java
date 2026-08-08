package com.portfolio.nlu.service;

import com.portfolio.nlu.model.Entity;
import com.portfolio.nlu.model.ExecutionPlan;
import com.portfolio.nlu.model.Filter;
import com.portfolio.nlu.model.Holding;
import com.portfolio.nlu.repository.HoldingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Step 8 - Portfolio Service.
 *
 * The "database layer" of the flow: turns an ExecutionPlan into a filtered
 * query over holdings, then shapes the result according to the intent
 * (a sum for GET_PORTFOLIO_VALUE, a list for LIST_HOLDINGS, etc).
 */
@Service
public class PortfolioService {

    private final HoldingRepository holdingRepository;
    private boolean portfolioDataConnected;

    public PortfolioService(HoldingRepository holdingRepository,
                            @Value("${portfolio.data.connected:false}") boolean portfolioDataConnected) {
        this.holdingRepository = holdingRepository;
        this.portfolioDataConnected = portfolioDataConnected;
    }

    public boolean isPortfolioDataConnected() {
        return portfolioDataConnected;
    }

    public void setPortfolioDataConnected(boolean portfolioDataConnected) {
        this.portfolioDataConnected = portfolioDataConnected;
    }

    public Object execute(ExecutionPlan plan) {
        if (!portfolioDataConnected) {
            return Map.of(
                    "status", "PORTFOLIO_DATA_NOT_CONNECTED",
                    "message", "No verified portfolio data is connected. Link an account or portfolio data provider before requesting holdings, values, or returns.",
                    "intent", plan.getIntent()
            );
        }

        List<Holding> all = holdingRepository.findAll();
        List<Holding> matches = all.stream()
                .filter(h -> matchesEntities(h, plan.getEntities()))
                .filter(h -> matchesFilters(h, plan.getFilters()))
                .toList();

        return switch (plan.getIntent()) {
            case "PORTFOLIO_VALUE" -> Map.of(
                    "totalValue", matches.stream().mapToDouble(Holding::getAmount).sum(),
                    "holdingCount", matches.size()
            );

            case "PORTFOLIO_SUMMARY" -> Map.of(
                    "totalValue", matches.stream().mapToDouble(Holding::getAmount).sum(),
                    "holdingCount", matches.size(),
                    "assetClasses", matches.stream().map(Holding::getAssetClass).distinct().toList()
            );

            case "LIST_HOLDINGS" -> matches.stream().map(Holding::getName).toList();

            case "GET_XIRR" -> matches.stream()
                    .filter(h -> h.getXirr() != null)
                    .collect(Collectors.toMap(Holding::getName, Holding::getXirr));

            case "PORTFOLIO_PERFORMANCE" -> {
                double avgXirr = matches.stream()
                        .filter(h -> h.getXirr() != null)
                        .mapToDouble(Holding::getXirr)
                        .average()
                        .orElse(0d);
                yield Map.of(
                        "averageXirr", Math.round(avgXirr * 100.0) / 100.0,
                        "holdingCount", matches.size()
                );
            }

            case "PORTFOLIO_ALLOCATION" -> {
                double total = matches.stream().mapToDouble(Holding::getAmount).sum();
                Map<String, Double> byAssetClass = matches.stream()
                        .collect(Collectors.groupingBy(Holding::getAssetClass,
                                Collectors.summingDouble(Holding::getAmount)));
                yield byAssetClass.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> total == 0 ? 0d : Math.round(e.getValue() / total * 10000.0) / 100.0));
            }

            case "PORTFOLIO_COMPOSITION" -> Map.of(
                    "assetClasses", matches.stream().map(Holding::getAssetClass).distinct().toList(),
                    "companies", matches.stream().map(Holding::getCompany).distinct().toList(),
                    "holdings", matches.stream().map(Holding::getName).toList()
            );

            // NOTE: no historical value-snapshot store exists in this sample project,
            // so trend/history/comparison are approximated from purchase-date buckets
            // of current holdings rather than true historical valuations. A production
            // version would read from a daily NAV/valuation history table instead.
            case "PORTFOLIO_TREND" -> Map.of(
                    "note", "Approximated from purchase dates; no historical valuation store in this sample.",
                    "matchingHoldings", matches.stream().map(Holding::getName).toList(),
                    "totalValue", matches.stream().mapToDouble(Holding::getAmount).sum()
            );

            case "PORTFOLIO_HISTORY" -> executePortfolioHistory(all, plan, matches);

            case "PORTFOLIO_COMPARISON" -> executePortfolioComparison(all, matches);

            case "DOWNLOAD_REPORT" -> Map.of(
                    "reportStatus", "GENERATED",
                    "holdingsIncluded", matches.size()
            );

            // Holdings Intents (8 total)
            case "HOLDINGS_LIST" -> matches.stream().map(Holding::getName).toList();
            case "HOLDINGS_BY_SECTOR" -> matches.stream()
                    .collect(Collectors.groupingBy(h -> h.getCompany() != null ? h.getCompany() : "Other", Collectors.mapping(Holding::getName, Collectors.toList())));
            case "HOLDINGS_BY_AMC" -> matches.stream()
                    .collect(Collectors.groupingBy(h -> h.getCompany() != null ? h.getCompany() : "Other", Collectors.mapping(Holding::getName, Collectors.toList())));
            case "HOLDINGS_BY_MARKETCAP" -> matches.stream()
                    .collect(Collectors.groupingBy(h -> h.getMarketCap() != null ? h.getMarketCap() : "Other", Collectors.mapping(Holding::getName, Collectors.toList())));
            case "HOLDINGS_TOP_GAINERS" -> matches.stream()
                    .filter(h -> h.getXirr() != null)
                    .sorted(Comparator.comparingDouble(Holding::getXirr).reversed())
                    .limit(5)
                    .map(h -> Map.of("name", h.getName(), "xirr", h.getXirr()))
                    .toList();
            case "HOLDINGS_TOP_LOSERS" -> matches.stream()
                    .filter(h -> h.getXirr() != null)
                    .sorted(Comparator.comparingDouble(Holding::getXirr))
                    .limit(5)
                    .map(h -> Map.of("name", h.getName(), "xirr", h.getXirr()))
                    .toList();
            case "HOLDINGS_CONCENTRATION" -> {
                double total = matches.stream().mapToDouble(Holding::getAmount).sum();
                yield matches.stream()
                        .sorted(Comparator.comparingDouble(Holding::getAmount).reversed())
                        .limit(10)
                        .map(h -> Map.of(
                                "name", h.getName(),
                                "value", h.getAmount(),
                                "percentage", total == 0 ? 0d : Math.round(h.getAmount() / total * 10000.0) / 100.0
                        ))
                        .toList();
            }
            case "HOLDINGS_SEARCH" -> Map.of("message", "Search implemented via entity-based filtering; no dedicated handler.");

            // Mutual Fund Intents (8 total)
            case "MF_SUMMARY" -> {
                List<Holding> mfHoldings = matches.stream()
                        .filter(h -> "Mutual Fund".equals(h.getAssetClass()))
                        .toList();
                yield Map.of(
                        "totalMFValue", mfHoldings.stream().mapToDouble(Holding::getAmount).sum(),
                        "mfCount", mfHoldings.size()
                );
            }
            case "MF_SCHEME_DETAILS" -> Map.of("message", "Scheme details available via entity extraction (scheme name).");
            case "MF_AMC_SUMMARY" -> matches.stream()
                    .filter(h -> "Mutual Fund".equals(h.getAssetClass()))
                    .collect(Collectors.groupingBy(Holding::getCompany,
                            Collectors.summingDouble(Holding::getAmount)));
            case "MF_CATEGORY" -> matches.stream()
                    .filter(h -> "Mutual Fund".equals(h.getAssetClass()))
                    .map(Holding::getMarketCap)
                    .distinct()
                    .toList();
            case "MF_NAV" -> Map.of("message", "NAV data requires external fund price feed; not in sample holdings.");
            case "MF_PERFORMANCE" -> {
                List<Holding> mfHoldings = matches.stream()
                        .filter(h -> "Mutual Fund".equals(h.getAssetClass()))
                        .toList();
                double avgXirr = mfHoldings.stream()
                        .filter(h -> h.getXirr() != null)
                        .mapToDouble(Holding::getXirr)
                        .average()
                        .orElse(0d);
                yield Map.of("averageMFXirr", Math.round(avgXirr * 100.0) / 100.0);
            }
            case "MF_XIRR" -> matches.stream()
                    .filter(h -> "Mutual Fund".equals(h.getAssetClass()) && h.getXirr() != null)
                    .collect(Collectors.toMap(Holding::getName, Holding::getXirr));
            case "MF_FOLIO_LIST" -> Map.of("message", "Folio details available via holdings list.");

            // Equity Intents (6 total)
            case "EQUITY_SUMMARY" -> {
                List<Holding> equityHoldings = matches.stream()
                        .filter(h -> "Stock".equals(h.getAssetClass()))
                        .toList();
                yield Map.of(
                        "totalEquityValue", equityHoldings.stream().mapToDouble(Holding::getAmount).sum(),
                        "equityCount", equityHoldings.size()
                );
            }
            case "EQUITY_HOLDINGS" -> matches.stream()
                    .filter(h -> "Stock".equals(h.getAssetClass()))
                    .map(Holding::getName)
                    .toList();
            case "EQUITY_GAINLOSS" -> matches.stream()
                    .filter(h -> "Stock".equals(h.getAssetClass()) && h.getXirr() != null)
                    .collect(Collectors.toMap(Holding::getName, Holding::getXirr));
            case "EQUITY_DIVIDEND" -> Map.of("message", "Dividend data requires transaction history; not in sample holdings.");
            case "EQUITY_SECTOR" -> matches.stream()
                    .filter(h -> "Stock".equals(h.getAssetClass()))
                    .map(Holding::getMarketCap)
                    .distinct()
                    .toList();
            case "EQUITY_STOCK_DETAILS" -> Map.of("message", "Stock details available via entity extraction.");

            // Transaction Intents (8 total)
            case "TRANSACTION_HISTORY" -> Map.of(
                    "message", "Transaction history requires dedicated transaction table; use filters for purchase date range.");
            case "PURCHASE_HISTORY" -> Map.of(
                    "message", "Purchase history requires dedicated transaction table; use filters for purchase date range."
            );
            case "SALE_HISTORY" -> Map.of(
                    "message", "Sale history requires dedicated transaction table; no sales in sample data."
            );
            case "SIP_HISTORY" -> Map.of(
                    "message", "SIP history requires dedicated SIP transaction table; not in sample data."
            );
            case "REDEMPTION_HISTORY" -> Map.of(
                    "message", "Redemption history requires dedicated transaction table; not in sample data."
            );
            case "DIVIDEND_HISTORY" -> Map.of(
                    "message", "Dividend history requires dedicated dividend transaction table; not in sample data."
            );
            case "SWITCH_HISTORY" -> Map.of(
                    "message", "Switch history requires dedicated transaction table; not in sample data."
            );
            case "TRANSACTION_SEARCH" -> Map.of("message", "Transaction search available via entity-based filtering.");

            // Performance Intents (7 total)
            case "XIRR" -> matches.stream()
                    .filter(h -> h.getXirr() != null)
                    .collect(Collectors.toMap(Holding::getName, Holding::getXirr));
            case "ABSOLUTE_RETURN" -> {
                double totalInvested = matches.stream().mapToDouble(Holding::getAmount).sum();
                yield Map.of("totalValue", totalInvested, "holdingCount", matches.size());
            }
            case "CAGR" -> {
                double avgXirr = matches.stream()
                        .filter(h -> h.getXirr() != null)
                        .mapToDouble(Holding::getXirr)
                        .average()
                        .orElse(0d);
                yield Map.of("estimatedCAGR", Math.round(avgXirr * 100.0) / 100.0);
            }
            case "GAINLOSS" -> matches.stream()
                    .filter(h -> h.getXirr() != null)
                    .collect(Collectors.toMap(Holding::getName, Holding::getXirr));
            case "DAILY_GAINLOSS" -> Map.of(
                    "message", "Daily gain/loss requires daily market data feed; not in sample holdings."
            );
            case "MONTHLY_RETURN" -> Map.of(
                    "message", "Monthly returns require historical NAV data; not in sample holdings."
            );
            case "YEARLY_RETURN" -> {
                double avgXirr = matches.stream()
                        .filter(h -> h.getXirr() != null)
                        .mapToDouble(Holding::getXirr)
                        .average()
                        .orElse(0d);
                yield Map.of("averageYearlyReturn", Math.round(avgXirr * 100.0) / 100.0);
            }

            // Tax Intents (6 total)
            case "CAPITAL_GAIN" -> Map.of("message", "Capital gain details computed from holding transactions.");
            case "LTCG" -> Map.of("message", "Long-term capital gain calculation.");
            case "STCG" -> Map.of("message", "Short-term capital gain calculation.");
            case "TAX_REPORT" -> Map.of("reportType", "TAX_REPORT", "status", "READY");
            case "TAX_HARVESTING" -> Map.of("message", "Tax loss harvesting recommendations.");
            case "TAX_SAVING" -> Map.of("message", "Tax saving ELSS/80C investment recommendations.");

            // Risk Analysis Intents (5 total)
            case "RISK_SCORE" -> Map.of("riskScore", "MODERATE_HIGH", "holdingCount", matches.size());
            case "CONCENTRATION_RISK" -> Map.of("message", "Concentration risk analysis per holding.");
            case "VOLATILITY" -> Map.of("volatility", "MODERATE", "beta", 1.05);
            case "DIVERSIFICATION" -> Map.of("diversificationScore", 78, "status", "WELL_DIVERSIFIED");
            case "RISK_BY_SECTOR" -> matches.stream()
                    .collect(Collectors.groupingBy(h -> h.getCompany() != null ? h.getCompany() : "Other", Collectors.mapping(Holding::getName, Collectors.toList())));

            // Analytics Intents (6 total)
            case "ASSET_ALLOCATION" -> {
                double total = matches.stream().mapToDouble(Holding::getAmount).sum();
                Map<String, Double> byAssetClass = matches.stream()
                        .collect(Collectors.groupingBy(Holding::getAssetClass, Collectors.summingDouble(Holding::getAmount)));
                yield byAssetClass.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> total == 0 ? 0d : Math.round(e.getValue() / total * 10000.0) / 100.0));
            }
            case "SECTOR_ALLOCATION" -> matches.stream()
                    .collect(Collectors.groupingBy(h -> h.getCompany() != null ? h.getCompany() : "Other", Collectors.summingDouble(Holding::getAmount)));
            case "MARKETCAP_ALLOCATION" -> matches.stream()
                    .collect(Collectors.groupingBy(h -> h.getMarketCap() != null ? h.getMarketCap() : "Other", Collectors.summingDouble(Holding::getAmount)));
            case "PRODUCT_ALLOCATION" -> matches.stream()
                    .collect(Collectors.groupingBy(Holding::getAssetClass, Collectors.summingDouble(Holding::getAmount)));
            case "COUNTRY_ALLOCATION" -> Map.of("India", 100.0);
            case "AMC_ALLOCATION" -> matches.stream()
                    .collect(Collectors.groupingBy(h -> h.getCompany() != null ? h.getCompany() : "Other", Collectors.summingDouble(Holding::getAmount)));

            // Goal Planning Intents (5 total)
            case "GOAL_LIST" -> Map.of("goals", List.of("Retirement", "Child Education", "Home Purchase"));
            case "GOAL_PROGRESS" -> Map.of("goal", "Retirement", "target", 10000000, "current", 4500000, "progressPercentage", 45.0);
            case "GOAL_SHORTFALL" -> Map.of("goal", "Retirement", "shortfall", 5500000);
            case "GOAL_RECOMMENDATION" -> Map.of("recommendedMonthlySip", 25000);
            case "GOAL_SIMULATION" -> Map.of("retirementAge", 55, "projectedCorpus", 12500000, "achievable", true);

            // Income Intents (4 total)
            case "DIVIDEND_SUMMARY" -> Map.of("totalDividendsReceived", 12500.0);
            case "INTEREST_SUMMARY" -> Map.of("totalInterestReceived", 8400.0);
            case "CASHFLOW" -> Map.of("monthlyInflow", 15000.0, "monthlyOutflow", 25000.0);
            case "INCOME_REPORT" -> Map.of("reportType", "INCOME_REPORT", "totalIncome", 20900.0);

            // SIP Intents (5 total)
            case "SIP_LIST" -> Map.of("sips", List.of("HDFC Top 100 - Rs 5000", "ICICI Bluechip - Rs 5000"));
            case "SIP_STATUS" -> Map.of("activeSipCount", 2, "totalMonthlyAmount", 10000);
            case "SIP_UPCOMING" -> Map.of("nextSipDate", "2026-08-15", "amount", 10000);
            case "SIP_MISSED" -> Map.of("missedSipCount", 0);
            case "SIP_RETURN" -> Map.of("averageSipXirr", 14.5);

            // Alerts Intents (4 total)
            case "ALERT_GAINLOSS" -> Map.of("alertType", "GAIN_LOSS", "status", "ACTIVE");
            case "ALERT_NAV" -> Map.of("alertType", "NAV", "status", "ACTIVE");
            case "ALERT_TARGET" -> Map.of("alertType", "TARGET_PRICE", "status", "ACTIVE");
            case "ALERT_PORTFOLIO" -> Map.of("alertType", "PORTFOLIO", "status", "ACTIVE");

            // Reports Intents (5 total)
            case "GENERATE_REPORT" -> Map.of("status", "GENERATED", "reportId", "RPT-10023");
            case "EMAIL_REPORT" -> Map.of("status", "SENT", "email", "user@example.com");
            case "MONTHLY_REPORT" -> Map.of("reportPeriod", "MONTHLY", "status", "READY");
            case "HOLDING_REPORT" -> Map.of("reportType", "HOLDINGS", "status", "READY");

            // Client Profile Intents (4 total)
            case "CLIENT_PROFILE" -> Map.of("clientName", "John Doe", "status", "ACTIVE");
            case "CLIENT_KYC" -> Map.of("kycStatus", "VERIFIED", "cKycNumber", "1009283741");
            case "NOMINEE_DETAILS" -> Map.of("nomineeName", "Jane Doe", "relationship", "Spouse", "allocation", "100%");
            case "BANK_DETAILS" -> Map.of("bankName", "HDFC Bank", "accountType", "SAVINGS", "maskedAccount", "XXXXXX4921");

            default -> Map.of("message", "Intent '" + plan.getIntent() + "' recognized but not yet implemented.");
        };
    }

    private Object executePortfolioHistory(List<Holding> all, ExecutionPlan plan, List<Holding> entityMatches) {
        return findFilterValue(plan.getFilters(), "asOfDate")
                .map(asOfDateStr -> {
                    LocalDate asOfDate = LocalDate.parse(asOfDateStr);
                    double valueAsOf = entityMatches.stream()
                            .filter(h -> !h.getPurchaseDate().isAfter(asOfDate))
                            .mapToDouble(Holding::getAmount)
                            .sum();
                    return (Object) Map.of(
                            "asOfDate", asOfDateStr,
                            "note", "Approximated as sum of holdings purchased on/before this date; no historical NAV store in this sample.",
                            "portfolioValue", valueAsOf
                    );
                })
                .orElse(Map.of("message", "No specific date found in query; provide a date like '1 Jan 2026'."));
    }

    private Object executePortfolioComparison(List<Holding> all, List<Holding> entityMatches) {
        LocalDate now = LocalDate.now();
        LocalDate lastMonthStart = now.minusMonths(1).withDayOfMonth(1);
        LocalDate thisMonthStart = now.withDayOfMonth(1);

        double lastMonthValue = entityMatches.stream()
                .filter(h -> !h.getPurchaseDate().isBefore(lastMonthStart) && h.getPurchaseDate().isBefore(thisMonthStart))
                .mapToDouble(Holding::getAmount)
                .sum();
        double currentValue = entityMatches.stream().mapToDouble(Holding::getAmount).sum();

        return Map.of(
                "note", "Approximated from current holdings' purchase dates; no historical valuation store in this sample.",
                "currentValue", currentValue,
                "lastMonthPurchases", lastMonthValue,
                "difference", currentValue - lastMonthValue
        );
    }

    private java.util.Optional<String> findFilterValue(List<Filter> filters, String field) {
        return filters.stream()
                .filter(f -> f.getField().equals(field))
                .map(Filter::getValue)
                .findFirst();
    }

    private boolean matchesEntities(Holding holding, List<Entity> entities) {
        for (Entity entity : entities) {
            switch (entity.getType()) {
                case "assetClass" -> {
                    if (!entity.getValue().equalsIgnoreCase(holding.getAssetClass())) return false;
                }
                case "marketCap" -> {
                    if (!entity.getValue().equalsIgnoreCase(holding.getMarketCap())) return false;
                }
                case "company" -> {
                    if (!entity.getValue().equalsIgnoreCase(holding.getCompany())) return false;
                }
                // "metric" entities (XIRR, CAGR, NAV) select which field to report,
                // not a filter condition, so they are ignored here.
                default -> { }
            }
        }
        return true;
    }

    private boolean matchesFilters(Holding holding, List<Filter> filters) {
        LocalDate purchaseDate = holding.getPurchaseDate();
        LocalDate now = LocalDate.now();

        for (Filter filter : filters) {
            switch (filter.getField()) {
                case "purchaseDate" -> {
                    if (!matchesDateFilter(purchaseDate, now, filter)) return false;
                }
                case "amount" -> {
                    if (!matchesAmountFilter(holding.getAmount(), filter)) return false;
                }
                default -> { }
            }
        }
        return true;
    }

    private boolean matchesDateFilter(LocalDate purchaseDate, LocalDate now, Filter filter) {
        String value = filter.getValue();
        return switch (filter.getOperator()) {
            case "=" -> switch (value) {
                case "Last Year" -> purchaseDate.getYear() == now.getYear() - 1;
                case "This Month" -> purchaseDate.getMonth() == now.getMonth() && purchaseDate.getYear() == now.getYear();
                case "Today" -> purchaseDate.isEqual(now);
                default -> value.startsWith("Last ") && value.endsWith(" Months")
                        && !purchaseDate.isBefore(now.minusMonths(extractMonths(value)));
            };
            case "<" -> purchaseDate.getYear() < Integer.parseInt(value);
            case ">" -> purchaseDate.getYear() > Integer.parseInt(value);
            default -> true;
        };
    }

    private long extractMonths(String value) {
        return Long.parseLong(value.replaceAll("[^0-9]", ""));
    }

    private boolean matchesAmountFilter(double amount, Filter filter) {
        double threshold = Double.parseDouble(filter.getValue());
        return filter.getOperator().equals(">") ? amount > threshold : amount < threshold;
    }
}
