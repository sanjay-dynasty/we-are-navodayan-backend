package com.wearenavodayan.authservice.dto.internal;

import java.util.UUID;

public record InternalProfileCreateRequest(
        UUID authUserId,
        String fullName
) {
}