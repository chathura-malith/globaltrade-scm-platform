package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class ItemStockNotFoundException extends BaseApplicationException {

    public ItemStockNotFoundException(String message) {
        super(message, 404);
    }

    public ItemStockNotFoundException(String message, int statusCode) {
        super(message, statusCode);
    }
}