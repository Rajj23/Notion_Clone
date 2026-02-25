package com.notion.demo.exception;

import com.notion.demo.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(InsufficientPermissionException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientPermission(InsufficientPermissionException ex){
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage());
    }
    
    @ExceptionHandler(WorkSpaceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkspaceNotFound(WorkSpaceNotFoundException ex){
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }
    
    @ExceptionHandler(NotWorkSpaceMemberException.class)
    public ResponseEntity<ErrorResponse> handleNotWorkSpaceMember(NotWorkSpaceMemberException ex){
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage());
    }
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex){
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }
    
    @ExceptionHandler(ActiveMemberException.class)
    public ResponseEntity<ErrorResponse> handleActiveMember(ActiveMemberException ex){
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
    
    @ExceptionHandler(OwnerLevelException.class)
    public ResponseEntity<ErrorResponse> handleOwnerLevel(OwnerLevelException ex){
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex){
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }
    
    @ExceptionHandler(EmailAlreadyExistException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExist(EmailAlreadyExistException ex){
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }
    
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex){
        return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {

        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();

        return buildError(HttpStatus.BAD_REQUEST, errorMessage);
    }

    
    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message){
        ErrorResponse error = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                LocalDateTime.now()
        );
        
        return new ResponseEntity<>(error, status);
    }
}
