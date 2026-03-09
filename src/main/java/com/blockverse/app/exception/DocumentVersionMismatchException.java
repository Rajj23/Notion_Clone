package com.blockverse.app.exception;

public class DocumentVersionMismatchException extends RuntimeException{
    public DocumentVersionMismatchException(String message) {
        super(message);
    }
}
