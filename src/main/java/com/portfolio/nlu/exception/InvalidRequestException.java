package com.portfolio.nlu.exception;

/**
 * Thrown by the RuleEngine when a query is well-formed (intent + entities
 * + filters all resolved) but violates a business rule, e.g. asking for
 * XIRR on a Fixed Deposit.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
