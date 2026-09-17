package com.wearenavodayan.userservice.dto.response;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message
) {
}