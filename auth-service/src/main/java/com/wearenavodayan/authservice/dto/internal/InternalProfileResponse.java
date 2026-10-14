package com.wearenavodayan.authservice.dto.internal;

import java.time.Instant;
import java.util.UUID;

public record InternalProfileResponse(
        UUID id,
        UUID authUserId,
        String fullName,
        Instant createdAt
) {
}