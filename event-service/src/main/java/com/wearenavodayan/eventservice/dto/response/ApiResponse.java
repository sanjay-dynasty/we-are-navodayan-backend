package com.wearenavodayan.eventservice.dto.response;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message
) {
}