package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class InsufficientStockException extends BaseApplicationException {

    public InsufficientStockException(String message) {
        super(message, 400);
    }

    public InsufficientStockException(String message, int statusCode) {
        super(message, statusCode);
    }
}