package com.blockverse.app.exception;

public class S3FileUploadException extends RuntimeException{
    public S3FileUploadException(String message) {
        super(message);
    }

    public S3FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }

    public S3FileUploadException(Throwable cause) {
        super(cause);
    }
}
