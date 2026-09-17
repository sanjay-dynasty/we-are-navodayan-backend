package com.wearenavodayan.notificationservice.dto.response;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message
) {
}