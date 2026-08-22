package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class ShipmentNotFoundException extends BaseApplicationException {

    public ShipmentNotFoundException(String message) {
        super(message, 404);
    }

    public ShipmentNotFoundException(String message, int statusCode) {
        super(message, statusCode);
    }
}