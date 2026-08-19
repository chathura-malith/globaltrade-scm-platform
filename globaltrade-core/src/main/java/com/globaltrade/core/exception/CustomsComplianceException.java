package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class CustomsComplianceException extends BaseApplicationException {

    public CustomsComplianceException(String message) {
        super(message, 422);
    }
}