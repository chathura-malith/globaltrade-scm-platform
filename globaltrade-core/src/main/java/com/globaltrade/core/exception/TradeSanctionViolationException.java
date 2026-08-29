package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class TradeSanctionViolationException extends BaseApplicationException {

    public TradeSanctionViolationException(String message) {
        super(message, 403);
    }

    public TradeSanctionViolationException(String message, int statusCode) {
        super(message, statusCode);
    }
}