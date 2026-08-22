package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class InvalidShipmentStateException extends BaseApplicationException {

    public InvalidShipmentStateException(String message) {
        super(message, 400);
    }

    public InvalidShipmentStateException(String message, int statusCode) {
        super(message, statusCode);
    }
}