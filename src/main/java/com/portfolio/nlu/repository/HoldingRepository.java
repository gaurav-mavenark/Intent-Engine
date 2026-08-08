package com.portfolio.nlu.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.portfolio.nlu.model.Holding;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory stand-in for a real data-access layer (e.g. Spring Data JPA
 * over a "holdings" table). Backed by data/holdings.json so the rest of
 * the pipeline can be demoed without a database.
 */
@Repository
public class HoldingRepository {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private List<Holding> holdings = new ArrayList<>();

    @PostConstruct
    public void load() throws Exception {
        try (InputStream in = new ClassPathResource("data/holdings.json").getInputStream()) {
            Holding[] loaded = objectMapper.readValue(in, Holding[].class);
            holdings = new ArrayList<>(List.of(loaded));
        }
    }

    public List<Holding> findAll() {
        return holdings;
    }
}
