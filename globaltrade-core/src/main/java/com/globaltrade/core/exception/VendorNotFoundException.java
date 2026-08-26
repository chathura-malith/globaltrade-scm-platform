package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class VendorNotFoundException extends BaseApplicationException {

    public VendorNotFoundException(String message) {
        super(message, 404);
    }

    public VendorNotFoundException(String message, int statusCode) {
        super(message, statusCode);
    }
}