package com.portfolio.nlu.service;

import com.portfolio.nlu.exception.InvalidRequestException;
import com.portfolio.nlu.model.Entity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Step 6 - Business Rule Engine.
 *
 * Validates that the resolved intent + entities make sense together
 * *before* an execution plan is built, e.g.:
 *   - SIP only exists for Mutual Funds
 *   - XIRR is not a valid metric for a Fixed Deposit
 *
 * Throws InvalidRequestException (mapped to HTTP 400 by the controller)
 * when a rule is violated.
 */
@Service
public class RuleEngine {

    public void validate(String intent, List<Entity> entities) {
        Optional<String> assetClass = findEntityValue(entities, "assetClass");
        Optional<String> metric = findEntityValue(entities, "metric");

        // Rule: SIP only exists for Mutual Funds
        assetClass.ifPresent(asset -> {
            if (asset.equals("Stock") && intentImpliesSip(intent)) {
                throw new InvalidRequestException("SIP is only applicable to Mutual Funds, not Stocks.");
            }
        });

        // Rule: XIRR / CAGR do not apply to Fixed Deposits
        if ((intent.equals("GET_XIRR") || metric.filter(m -> m.equals("XIRR") || m.equals("CAGR")).isPresent())
                && assetClass.filter("Fixed Deposit"::equals).isPresent()) {
            throw new InvalidRequestException("XIRR/CAGR is not supported for Fixed Deposits.");
        }
    }

    private boolean intentImpliesSip(String intent) {
        // placeholder hook: extend when a dedicated GET_SIP_STATUS intent is added
        return false;
    }

    private Optional<String> findEntityValue(List<Entity> entities, String type) {
        return entities.stream()
                .filter(e -> e.getType().equals(type))
                .map(Entity::getValue)
                .findFirst();
    }
}
