package com.sabbir.ums_assignment.exception;

import com.sabbir.ums_assignment.dto.ApiError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handle(ApiException ex) {
        return error(ex.getStatus(), ex.getMessage());
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> badRequest(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid JSON body or resource ID");
    }
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> unauthorized(AuthenticationException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> conflict(DataIntegrityViolationException ex) {
        return error(HttpStatus.CONFLICT, "Username already exists or data conflicts with a stored record");
    }
    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), message));
    }
}
