package com.wearenavodayan.jnvservice.dto.response;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message
) {
}