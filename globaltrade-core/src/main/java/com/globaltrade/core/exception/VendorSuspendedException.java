package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class VendorSuspendedException extends BaseApplicationException {

    public VendorSuspendedException(String message) {
        super(message, 403);
    }

    public VendorSuspendedException(String message, int statusCode) {
        super(message, statusCode);
    }
}