package com.portfolio.nlu.controller;

import com.portfolio.nlu.exception.InvalidRequestException;
import com.portfolio.nlu.model.IntentRequest;
import com.portfolio.nlu.model.IntentResponse;
import com.portfolio.nlu.service.IntentEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/nlu")
@Tag(name = "Intent Engine", description = "Natural Language Understanding Pipeline Endpoints for Portfolio Queries")
public class IntentController {

    private final IntentEngineService intentEngineService;

    public IntentController(IntentEngineService intentEngineService) {
        this.intentEngineService = intentEngineService;
    }

    /**
     * Runs a natural-language portfolio query through the full pipeline
     * and returns every stage of the trace plus the final result.
     *
     * Example: POST /api/nlu/query { "query": "Show me my MF purchased last yr" }
     */
    @Operation(summary = "Process Natural Language Query", description = "Executes full NLU pipeline (Normalization, Intent Classification, Entity Extraction, Filter Extraction, Rule Validation, Execution Planning, and Query Execution)")
    @PostMapping("/query")
    public ResponseEntity<IntentResponse> query(@Valid @RequestBody IntentRequest request) {
        IntentResponse response = intentEngineService.process(request.getQuery());
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRequest(InvalidRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_REQUEST", "message", ex.getMessage()));
    }
}
