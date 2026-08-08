package com.portfolio.nlu.service;

import com.portfolio.nlu.model.Entity;
import com.portfolio.nlu.model.ExecutionPlan;
import com.portfolio.nlu.model.Filter;
import com.portfolio.nlu.model.IntentResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the full pipeline, in order:
 *
 *   raw query
 *     -> NormalizationService   (Step 2)
 *     -> IntentClassifier       (Step 3)
 *     -> EntityExtractor        (Step 4)
 *     -> FilterExtractor        (Step 5)
 *     -> RuleEngine             (Step 6, throws on violation)
 *     -> ExecutionPlanner       (Step 7)
 *     -> PortfolioService       (Step 8)
 *
 * Each stage is a separate, independently testable Spring bean; this class
 * only wires them together and assembles the full trace into IntentResponse.
 */
@Service
public class IntentEngineService {

    private final NormalizationService normalizationService;
    private final IntentClassifier intentClassifier;
    private final EntityExtractor entityExtractor;
    private final FilterExtractor filterExtractor;
    private final RuleEngine ruleEngine;
    private final ExecutionPlanner executionPlanner;
    private final PortfolioService portfolioService;

    public IntentEngineService(NormalizationService normalizationService,
                                IntentClassifier intentClassifier,
                                EntityExtractor entityExtractor,
                                FilterExtractor filterExtractor,
                                RuleEngine ruleEngine,
                                ExecutionPlanner executionPlanner,
                                PortfolioService portfolioService) {
        this.normalizationService = normalizationService;
        this.intentClassifier = intentClassifier;
        this.entityExtractor = entityExtractor;
        this.filterExtractor = filterExtractor;
        this.ruleEngine = ruleEngine;
        this.executionPlanner = executionPlanner;
        this.portfolioService = portfolioService;
    }

    public IntentResponse process(String rawQuery) {
        String normalized = normalizationService.normalize(rawQuery);
        String intent = intentClassifier.classify(normalized);
        List<Entity> entities = entityExtractor.extract(normalized);
        List<Filter> filters = filterExtractor.extract(normalized);

        ruleEngine.validate(intent, entities);

        ExecutionPlan plan = executionPlanner.buildPlan(intent, entities, filters);
        Object result = portfolioService.execute(plan);

        IntentResponse response = new IntentResponse();
        response.setRawQuery(rawQuery);
        response.setNormalizedQuery(normalized);
        response.setIntent(intent);
        response.setEntities(entities);
        response.setFilters(filters);
        response.setExecutionPlan(plan);
        response.setResult(result);
        return response;
    }
}
