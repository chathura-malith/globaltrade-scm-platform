package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class TradeComplianceException extends BaseApplicationException {

    public TradeComplianceException(String message) {
        super(message, 422); // Unprocessable Entity - Compliance validation failure
    }

    public TradeComplianceException(String message, int statusCode) {
        super(message, statusCode);
    }
}