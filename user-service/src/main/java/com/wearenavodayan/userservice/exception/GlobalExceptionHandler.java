package com.wearenavodayan.userservice.exception;

import com.wearenavodayan.userservice.dto.response.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            EntityNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(
            IllegalStateException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(
            ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getReason() == null
                ? status.getReasonPhrase()
                : exception.getReason();

        return response(status, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableRequest(
            HttpMessageNotReadableException exception) {
        return response(
                HttpStatus.BAD_REQUEST,
                "Request body is missing or contains invalid JSON");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            NoResourceFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(
            NoHandlerFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "Endpoint not found");
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception exception) {
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
    }

    private ResponseEntity<ApiResponse<Void>> response(
            HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(false, null, message));
    }
}
