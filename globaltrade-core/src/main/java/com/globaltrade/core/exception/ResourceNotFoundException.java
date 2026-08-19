package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class ResourceNotFoundException extends BaseApplicationException {

    public ResourceNotFoundException(String message) {
        super(message, 404);
    }
}