package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class InvalidRouteStateException extends BaseApplicationException {

    public InvalidRouteStateException(String message) {
        super(message, 409);
    }

    public InvalidRouteStateException(String message, int statusCode) {
        super(message, statusCode);
    }
}