package com.portfolio.nlu.model;

import java.util.List;

/**
 * Full trace of the pipeline: raw input, each intermediate stage, and the
 * final result from the Portfolio Service. Returning every stage (rather
 * than just the final answer) makes the pipeline easy to debug and demo.
 */
public class IntentResponse {

    private String rawQuery;
    private String normalizedQuery;
    private String intent;
    private List<Entity> entities;
    private List<Filter> filters;
    private ExecutionPlan executionPlan;
    private Object result;

    public String getRawQuery() {
        return rawQuery;
    }

    public void setRawQuery(String rawQuery) {
        this.rawQuery = rawQuery;
    }

    public String getNormalizedQuery() {
        return normalizedQuery;
    }

    public void setNormalizedQuery(String normalizedQuery) {
        this.normalizedQuery = normalizedQuery;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public void setEntities(List<Entity> entities) {
        this.entities = entities;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    public ExecutionPlan getExecutionPlan() {
        return executionPlan;
    }

    public void setExecutionPlan(ExecutionPlan executionPlan) {
        this.executionPlan = executionPlan;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
