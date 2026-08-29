package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class RouteOptimizationNotFoundException extends BaseApplicationException {

    public RouteOptimizationNotFoundException(String message) {
        super(message, 404);
    }

    public RouteOptimizationNotFoundException(String message, int statusCode) {
        super(message, statusCode);
    }
}