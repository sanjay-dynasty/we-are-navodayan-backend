package com.wearenavodayan.userservice.exception;

public class JnvServiceClientException extends RuntimeException {

    public JnvServiceClientException(String message) {
        super(message);
    }

    public JnvServiceClientException(String message, Throwable cause) {
        super(message, cause);
    }
}