package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class InvalidCustomsStateException extends BaseApplicationException {

    public InvalidCustomsStateException(String message) {
        super(message, 409);
    }

    public InvalidCustomsStateException(String message, int statusCode) {
        super(message, statusCode);
    }
}