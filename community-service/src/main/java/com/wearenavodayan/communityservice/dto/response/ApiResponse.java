package com.wearenavodayan.communityservice.dto.response;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message
) {
}