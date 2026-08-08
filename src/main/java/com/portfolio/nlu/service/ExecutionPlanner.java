package com.portfolio.nlu.service;

import com.portfolio.nlu.model.Entity;
import com.portfolio.nlu.model.ExecutionPlan;
import com.portfolio.nlu.model.Filter;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Step 7 - Execution Plan.
 *
 * Assembles the validated intent + entities + filters into a single
 * machine-readable ExecutionPlan that PortfolioService knows how to
 * execute (translate into a query against the holdings store).
 */
@Service
public class ExecutionPlanner {

    public ExecutionPlan buildPlan(String intent, List<Entity> entities, List<Filter> filters) {
        return new ExecutionPlan(intent, entities, filters);
    }
}
