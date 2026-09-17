package com.wearenavodayan.alumniservice.dto.response;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message
) {
}