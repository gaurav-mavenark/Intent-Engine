package com.portfolio.nlu.model;

import java.util.List;

public class ExecutionPlan {

    private String intent;
    private List<Entity> entities;
    private List<Filter> filters;

    public ExecutionPlan() {
    }

    public ExecutionPlan(String intent, List<Entity> entities, List<Filter> filters) {
        this.intent = intent;
        this.entities = entities;
        this.filters = filters;
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
}
