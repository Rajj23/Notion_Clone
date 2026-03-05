package com.blockverse.app.exception;

public class InsufficientPermissionException extends RuntimeException{
    public InsufficientPermissionException(String message) {
        super(message);
    }
}
