package com.wearenavodayan.authservice.dto.response;

import java.time.Instant;

public record ApiErrorResponse(
        boolean success,
        String code,
        String message,
        Instant timestamp,
        String path
) {
}