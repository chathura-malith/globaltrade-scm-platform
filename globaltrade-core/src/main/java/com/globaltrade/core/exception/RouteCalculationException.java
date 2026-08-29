package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class RouteCalculationException extends BaseApplicationException {

    public RouteCalculationException(String message) {
        super(message, 422);
    }

    public RouteCalculationException(String message, int statusCode) {
        super(message, statusCode);
    }
}