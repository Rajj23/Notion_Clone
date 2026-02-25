package com.notion.demo.exception;

public class InsufficientPermissionException extends RuntimeException{
    public InsufficientPermissionException(String message) {
        super(message);
    }
}
