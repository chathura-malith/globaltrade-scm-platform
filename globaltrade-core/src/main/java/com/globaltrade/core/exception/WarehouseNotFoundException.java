package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class WarehouseNotFoundException extends BaseApplicationException {

    public WarehouseNotFoundException(String message) {
        super(message, 404);
    }

    public WarehouseNotFoundException(String message, int statusCode) {
        super(message, statusCode);
    }
}