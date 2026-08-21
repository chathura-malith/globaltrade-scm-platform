package com.globaltrade.core.exception;

public class InvalidInputException extends BaseApplicationException {

    public InvalidInputException(String message) {
        super(message, 400); // Default HTTP 400 Bad Request
    }

    public InvalidInputException(String message, int statusCode) {
        super(message, statusCode);
    }

    public InvalidInputException(String message, Throwable cause, int statusCode) {
        super(message, cause, statusCode);
    }
}