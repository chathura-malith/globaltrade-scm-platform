package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;
import lombok.Getter;

@Getter
@ApplicationException(rollback = true)
public class BaseApplicationException extends RuntimeException {

    private final int statusCode;

    public BaseApplicationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public BaseApplicationException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

}