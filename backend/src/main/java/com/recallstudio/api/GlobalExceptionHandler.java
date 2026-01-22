package com.recallstudio.api;

import com.recallstudio.exception.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(AppException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        ErrorResponse response = new ErrorResponse(ex.getCode(), ex.getMessage(), null);
        return org.springframework.http.ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        FieldError fieldError = ex.getBindingResult().getFieldError();
        if (fieldError != null) {
            details.put("field", fieldError.getField());
            details.put("rejectedValue", fieldError.getRejectedValue());
        }
        ErrorResponse response = new ErrorResponse("VALIDATION_ERROR", "validation failed", details);
        return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        ErrorResponse response = new ErrorResponse("VALIDATION_ERROR", "invalid request body", null);
        return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleOther(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorResponse response = new ErrorResponse("INTERNAL_ERROR",
                ex.getMessage() == null ? "internal error" : ex.getMessage(), null);
        return org.springframework.http.ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
