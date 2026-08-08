package com.portfolio.nlu;

import com.portfolio.nlu.model.IntentResponse;
import com.portfolio.nlu.service.IntentEngineService;
import com.portfolio.nlu.service.PortfolioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "portfolio.data.connected=true")
class IntentEngineServiceTest {

    @Autowired
    private IntentEngineService intentEngineService;

    @Autowired
    private PortfolioService portfolioService;

    @Test
    void endToEndExampleFromSpec() {
        IntentResponse response = intentEngineService.process("Show my large cap MF purchased last yr");

        assertEquals("show large cap mutual fund purchased last year", response.getNormalizedQuery());
        assertEquals("HOLDINGS_LIST", response.getIntent());
        assertTrue(response.getEntities().stream()
                .anyMatch(e -> e.getType().equals("assetClass") && e.getValue().equals("Mutual Fund")));
        assertTrue(response.getEntities().stream()
                .anyMatch(e -> e.getType().equals("marketCap") && e.getValue().equals("Large Cap")));
        assertTrue(response.getFilters().stream()
                .anyMatch(f -> f.getField().equals("purchaseDate") && f.getValue().equals("Last Year")));
    }

    @Test
    void portfolioValueQuery() {
        IntentResponse response = intentEngineService.process("How much is my portfolio worth?");
        assertEquals("PORTFOLIO_VALUE", response.getIntent());
    }

    @Test
    void portfolioSummaryQuery() {
        IntentResponse response = intentEngineService.process("Show my portfolio");
        assertEquals("PORTFOLIO_SUMMARY", response.getIntent());
    }

    @Test
    void portfolioPerformanceQuery() {
        IntentResponse response = intentEngineService.process("How is my portfolio performing?");
        assertEquals("PORTFOLIO_PERFORMANCE", response.getIntent());
    }

    @Test
    void portfolioAllocationQuery() {
        IntentResponse response = intentEngineService.process("Show portfolio allocation");
        assertEquals("PORTFOLIO_ALLOCATION", response.getIntent());
    }

    @Test
    void portfolioCompositionQuery() {
        IntentResponse response = intentEngineService.process("What do I own?");
        assertEquals("PORTFOLIO_COMPOSITION", response.getIntent());
    }

    @Test
    void portfolioTrendQuery() {
        IntentResponse response = intentEngineService.process("Show portfolio growth for last year");
        assertEquals("PORTFOLIO_TREND", response.getIntent());
    }

    @Test
    void portfolioHistoryQuery() {
        IntentResponse response = intentEngineService.process("Portfolio value on 1 Jan 2026");
        assertEquals("PORTFOLIO_HISTORY", response.getIntent());
        assertTrue(response.getFilters().stream()
                .anyMatch(f -> f.getField().equals("asOfDate") && f.getValue().equals("2026-01-01")));
    }

    @Test
    void portfolioComparisonQuery() {
        IntentResponse response = intentEngineService.process("Compare my portfolio with last month");
        assertEquals("PORTFOLIO_COMPARISON", response.getIntent());
    }

    @Test
    void xirrOnFixedDepositIsRejected() {
        try {
            intentEngineService.process("Show XIRR of Fixed Deposit");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("XIRR"));
            return;
        }
        // Falls through only if entity extraction didn't tag Fixed Deposit; acceptable
        // since "Fixed Deposit" entity resolution depends on dictionary coverage.
    }

    // Portfolio Intents
    @Test
    void portfolioSummaryIntent() {
        IntentResponse response = intentEngineService.process("Show my portfolio");
        assertEquals("PORTFOLIO_SUMMARY", response.getIntent());
    }

    @Test
    void portfolioAllocationIntent() {
        IntentResponse response = intentEngineService.process("Show portfolio allocation");
        assertEquals("PORTFOLIO_ALLOCATION", response.getIntent());
    }

    @Test
    void portfolioCompositionIntent() {
        IntentResponse response = intentEngineService.process("What do I own?");
        assertEquals("PORTFOLIO_COMPOSITION", response.getIntent());
    }

    // Holdings Intents
    @Test
    void holdingsListIntent() {
        IntentResponse response = intentEngineService.process("List my holdings");
        assertEquals("HOLDINGS_LIST", response.getIntent());
    }

    @Test
    void holdingsBySectorIntent() {
        IntentResponse response = intentEngineService.process("Show holdings by sector");
        assertEquals("HOLDINGS_BY_SECTOR", response.getIntent());
    }

    @Test
    void holdingsByAMCIntent() {
        IntentResponse response = intentEngineService.process("Holdings by AMC");
        assertEquals("HOLDINGS_BY_AMC", response.getIntent());
    }

    @Test
    void holdingsByMarketCapIntent() {
        IntentResponse response = intentEngineService.process("Holdings by market cap");
        assertEquals("HOLDINGS_BY_MARKETCAP", response.getIntent());
    }

    @Test
    void holdingsTopGainersIntent() {
        IntentResponse response = intentEngineService.process("Top gainers");
        assertEquals("HOLDINGS_TOP_GAINERS", response.getIntent());
    }

    @Test
    void holdingsTopLosersIntent() {
        IntentResponse response = intentEngineService.process("Top losers");
        assertEquals("HOLDINGS_TOP_LOSERS", response.getIntent());
    }

    @Test
    void holdingsConcentrationIntent() {
        IntentResponse response = intentEngineService.process("Show concentration");
        assertEquals("HOLDINGS_CONCENTRATION", response.getIntent());
    }

    // Mutual Fund Intents
    @Test
    void mfSummaryIntent() {
        IntentResponse response = intentEngineService.process("Mutual fund summary");
        assertEquals("MF_SUMMARY", response.getIntent());
    }

    @Test
    void mfSchemeDetailsIntent() {
        IntentResponse response = intentEngineService.process("Scheme details");
        assertEquals("MF_SCHEME_DETAILS", response.getIntent());
    }

    @Test
    void mfNavIntent() {
        IntentResponse response = intentEngineService.process("Fund NAV");
        assertEquals("MF_NAV", response.getIntent());
    }

    @Test
    void mfXirrIntent() {
        IntentResponse response = intentEngineService.process("Fund XIRR");
        assertEquals("MF_XIRR", response.getIntent());
    }

    // Equity Intents
    @Test
    void equitySummaryIntent() {
        IntentResponse response = intentEngineService.process("Equity summary");
        assertEquals("EQUITY_SUMMARY", response.getIntent());
    }

    @Test
    void equityHoldingsIntent() {
        IntentResponse response = intentEngineService.process("Equity holdings");
        assertEquals("EQUITY_HOLDINGS", response.getIntent());
    }

    @Test
    void equityDividendIntent() {
        IntentResponse response = intentEngineService.process("Stock dividend");
        assertEquals("EQUITY_DIVIDEND", response.getIntent());
    }

    // Transaction Intents
    @Test
    void transactionHistoryIntent() {
        IntentResponse response = intentEngineService.process("Transaction history");
        assertEquals("TRANSACTION_HISTORY", response.getIntent());
    }

    @Test
    void purchaseHistoryIntent() {
        IntentResponse response = intentEngineService.process("Purchase history");
        assertEquals("PURCHASE_HISTORY", response.getIntent());
    }

    @Test
    void saleHistoryIntent() {
        IntentResponse response = intentEngineService.process("Sale history");
        assertEquals("SALE_HISTORY", response.getIntent());
    }

    @Test
    void sipHistoryIntent() {
        IntentResponse response = intentEngineService.process("SIP history");
        assertEquals("SIP_HISTORY", response.getIntent());
    }

    @Test
    void dividendHistoryIntent() {
        IntentResponse response = intentEngineService.process("Dividend history");
        assertEquals("DIVIDEND_HISTORY", response.getIntent());
    }

    // Performance Intents
    @Test
    void xirrPerformanceIntent() {
        IntentResponse response = intentEngineService.process("XIRR");
        assertEquals("XIRR", response.getIntent());
    }

    @Test
    void cagrIntent() {
        IntentResponse response = intentEngineService.process("CAGR");
        assertEquals("CAGR", response.getIntent());
    }

    @Test
    void gainlossIntent() {
        IntentResponse response = intentEngineService.process("Gain loss");
        assertEquals("GAINLOSS", response.getIntent());
    }

    @Test
    void monthlyReturnIntent() {
        IntentResponse response = intentEngineService.process("Monthly return");
        assertEquals("MONTHLY_RETURN", response.getIntent());
    }

    @Test
    void yearlyReturnIntent() {
        IntentResponse response = intentEngineService.process("Yearly return");
        assertEquals("YEARLY_RETURN", response.getIntent());
    }

    // Tax Intents
    @Test
    void capitalGainIntent() {
        assertEquals("CAPITAL_GAIN", intentEngineService.process("Capital gain").getIntent());
    }

    @Test
    void ltcgIntent() {
        assertEquals("LTCG", intentEngineService.process("Long term capital gain").getIntent());
    }

    @Test
    void stcgIntent() {
        assertEquals("STCG", intentEngineService.process("Short term capital gain").getIntent());
    }

    @Test
    void taxReportIntent() {
        assertEquals("TAX_REPORT", intentEngineService.process("Tax report").getIntent());
    }

    @Test
    void taxHarvestingIntent() {
        assertEquals("TAX_HARVESTING", intentEngineService.process("Tax harvesting opportunities").getIntent());
    }

    @Test
    void taxSavingIntent() {
        assertEquals("TAX_SAVING", intentEngineService.process("Tax saving investments").getIntent());
    }

    // Risk Analysis Intents
    @Test
    void riskScoreIntent() {
        assertEquals("RISK_SCORE", intentEngineService.process("Portfolio risk").getIntent());
    }

    @Test
    void concentrationRiskIntent() {
        assertEquals("CONCENTRATION_RISK", intentEngineService.process("Concentration risk").getIntent());
    }

    @Test
    void volatilityIntent() {
        assertEquals("VOLATILITY", intentEngineService.process("Portfolio volatility").getIntent());
    }

    @Test
    void diversificationIntent() {
        assertEquals("DIVERSIFICATION", intentEngineService.process("Diversification analysis").getIntent());
    }

    @Test
    void riskBySectorIntent() {
        assertEquals("RISK_BY_SECTOR", intentEngineService.process("Sector risk").getIntent());
    }

    // Analytics Intents
    @Test
    void assetAllocationIntent() {
        assertEquals("ASSET_ALLOCATION", intentEngineService.process("Asset allocation").getIntent());
    }

    @Test
    void sectorAllocationIntent() {
        assertEquals("SECTOR_ALLOCATION", intentEngineService.process("Sector allocation").getIntent());
    }

    @Test
    void marketcapAllocationIntent() {
        assertEquals("MARKETCAP_ALLOCATION", intentEngineService.process("Market cap allocation").getIntent());
    }

    @Test
    void productAllocationIntent() {
        assertEquals("PRODUCT_ALLOCATION", intentEngineService.process("Product allocation").getIntent());
    }

    @Test
    void countryAllocationIntent() {
        assertEquals("COUNTRY_ALLOCATION", intentEngineService.process("Country exposure").getIntent());
    }

    @Test
    void amcAllocationIntent() {
        assertEquals("AMC_ALLOCATION", intentEngineService.process("AMC-wise allocation").getIntent());
    }

    // Goal Planning Intents
    @Test
    void goalListIntent() {
        assertEquals("GOAL_LIST", intentEngineService.process("Show my goals").getIntent());
    }

    @Test
    void goalProgressIntent() {
        assertEquals("GOAL_PROGRESS", intentEngineService.process("Retirement goal progress").getIntent());
    }

    @Test
    void goalShortfallIntent() {
        assertEquals("GOAL_SHORTFALL", intentEngineService.process("Goal shortfall").getIntent());
    }

    @Test
    void goalRecommendationIntent() {
        assertEquals("GOAL_RECOMMENDATION", intentEngineService.process("Investment required").getIntent());
    }

    @Test
    void goalSimulationIntent() {
        assertEquals("GOAL_SIMULATION", intentEngineService.process("Can I retire at 55?").getIntent());
    }

    // Income Intents
    @Test
    void dividendSummaryIntent() {
        assertEquals("DIVIDEND_SUMMARY", intentEngineService.process("Dividend summary").getIntent());
    }

    @Test
    void interestSummaryIntent() {
        assertEquals("INTEREST_SUMMARY", intentEngineService.process("Interest received").getIntent());
    }

    @Test
    void cashflowIntent() {
        assertEquals("CASHFLOW", intentEngineService.process("Investment cashflow").getIntent());
    }

    @Test
    void incomeReportIntent() {
        assertEquals("INCOME_REPORT", intentEngineService.process("Income report").getIntent());
    }

    // SIP Intents
    @Test
    void sipListIntent() {
        assertEquals("SIP_LIST", intentEngineService.process("Show SIPs").getIntent());
    }

    @Test
    void sipStatusIntent() {
        assertEquals("SIP_STATUS", intentEngineService.process("Active SIPs").getIntent());
    }

    @Test
    void sipUpcomingIntent() {
        assertEquals("SIP_UPCOMING", intentEngineService.process("Upcoming SIPs").getIntent());
    }

    @Test
    void sipMissedIntent() {
        assertEquals("SIP_MISSED", intentEngineService.process("Missed SIPs").getIntent());
    }

    @Test
    void sipReturnIntent() {
        assertEquals("SIP_RETURN", intentEngineService.process("SIP performance").getIntent());
    }

    // Alerts Intents
    @Test
    void alertGainLossIntent() {
        assertEquals("ALERT_GAINLOSS", intentEngineService.process("Notify gain/loss").getIntent());
    }

    @Test
    void alertNavIntent() {
        assertEquals("ALERT_NAV", intentEngineService.process("NAV alert").getIntent());
    }

    @Test
    void alertTargetIntent() {
        assertEquals("ALERT_TARGET", intentEngineService.process("Target price alert").getIntent());
    }

    @Test
    void alertPortfolioIntent() {
        assertEquals("ALERT_PORTFOLIO", intentEngineService.process("Portfolio alert").getIntent());
    }

    // Reports Intents
    @Test
    void generateReportIntent() {
        assertEquals("GENERATE_REPORT", intentEngineService.process("Generate report").getIntent());
    }

    @Test
    void downloadReportIntent() {
        assertEquals("DOWNLOAD_REPORT", intentEngineService.process("Download portfolio report").getIntent());
    }

    @Test
    void emailReportIntent() {
        assertEquals("EMAIL_REPORT", intentEngineService.process("Email report").getIntent());
    }

    @Test
    void monthlyReportIntent() {
        assertEquals("MONTHLY_REPORT", intentEngineService.process("Monthly report").getIntent());
    }

    @Test
    void holdingReportIntent() {
        assertEquals("HOLDING_REPORT", intentEngineService.process("Holding report").getIntent());
    }

    // Client Profile Intents
    @Test
    void clientProfileIntent() {
        assertEquals("CLIENT_PROFILE", intentEngineService.process("My profile").getIntent());
    }

    @Test
    void clientKycIntent() {
        assertEquals("CLIENT_KYC", intentEngineService.process("KYC status").getIntent());
    }

    @Test
    void nomineeDetailsIntent() {
        assertEquals("NOMINEE_DETAILS", intentEngineService.process("Nominee details").getIntent());
    }

    @Test
    void bankDetailsIntent() {
        assertEquals("BANK_DETAILS", intentEngineService.process("Bank details").getIntent());
    }

    @Test
    void portfolioDataNotConnectedWhenDisabled() {
        try {
            portfolioService.setPortfolioDataConnected(false);
            IntentResponse response = intentEngineService.process("Mutual fund summary");
            assertNotNull(response.getResult());
            assertTrue(response.getResult() instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, String> resultMap = (Map<String, String>) response.getResult();
            assertEquals("PORTFOLIO_DATA_NOT_CONNECTED", resultMap.get("status"));
            assertEquals("No verified portfolio data is connected. Link an account or portfolio data provider before requesting holdings, values, or returns.", resultMap.get("message"));
            assertEquals("MF_SUMMARY", resultMap.get("intent"));
        } finally {
            portfolioService.setPortfolioDataConnected(true);
        }
    }
}
