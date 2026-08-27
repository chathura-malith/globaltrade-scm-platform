package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class CustomsDeclarationNotFoundException extends BaseApplicationException {

    public CustomsDeclarationNotFoundException(String message) {
        super(message, 404);
    }

    public CustomsDeclarationNotFoundException(String message, int statusCode) {
        super(message, statusCode);
    }
}