package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class SecurityAuthenticationException extends BaseApplicationException {

    public SecurityAuthenticationException(String message) {
        super(message, 401);
    }

    public SecurityAuthenticationException(String message, int statusCode) {
        super(message, statusCode);
    }
}