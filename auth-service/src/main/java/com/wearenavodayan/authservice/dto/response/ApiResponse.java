package com.wearenavodayan.authservice.dto.response;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message
) {
}