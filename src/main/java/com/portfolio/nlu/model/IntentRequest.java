package com.portfolio.nlu.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class IntentRequest {

    @Schema(description = "Natural language portfolio query", example = "Show my large cap MF purchased last yr")
    @NotBlank(message = "query must not be blank")
    private String query;

    public IntentRequest() {
    }

    public IntentRequest(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
