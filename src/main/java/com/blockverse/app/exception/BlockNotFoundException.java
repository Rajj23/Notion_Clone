package com.blockverse.app.exception;

public class BlockNotFoundException extends RuntimeException{
    public BlockNotFoundException(String message) {
        super(message);
    }
}
