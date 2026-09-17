package com.wearenavodayan.opportunityservice.dto.response;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message
) {
}