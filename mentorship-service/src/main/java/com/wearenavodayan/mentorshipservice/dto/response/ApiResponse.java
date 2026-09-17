package com.wearenavodayan.mentorshipservice.dto.response;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message
) {
}