package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class InvalidVendorStateException extends BaseApplicationException {

    public InvalidVendorStateException(String message) {
        super(message, 409);
    }

    public InvalidVendorStateException(String message, int statusCode) {
        super(message, statusCode);
    }
}