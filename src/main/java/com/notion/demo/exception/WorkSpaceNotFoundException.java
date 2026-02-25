package com.notion.demo.exception;

public class WorkSpaceNotFoundException extends RuntimeException{
    public WorkSpaceNotFoundException(String message) {
        super(message);
    }
}
