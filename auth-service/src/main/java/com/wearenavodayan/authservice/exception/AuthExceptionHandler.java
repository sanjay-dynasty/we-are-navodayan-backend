package com.wearenavodayan.authservice.exception;

import com.wearenavodayan.authservice.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(
            DuplicateEmailException exception,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            new ApiErrorResponse(
                false,
                "EMAIL_ALREADY_EXISTS",
                exception.getMessage(),
                Instant.now(),
                request.getRequestURI()
            )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        return ResponseEntity.badRequest().body(
            new ApiErrorResponse(
                false,
                "VALIDATION_ERROR",
                "Request validation failed",
                Instant.now(),
                request.getRequestURI()
            )
        );
    }
}