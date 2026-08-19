package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class BusinessRuleViolationException extends BaseApplicationException {

    public BusinessRuleViolationException(String message) {
        super(message, 400);
    }

    public BusinessRuleViolationException(String message, int statusCode) {
        super(message, statusCode);
    }
}