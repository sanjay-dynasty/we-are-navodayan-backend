package com.wearenavodayan.authservice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record RegisteredUserResponse(
    UUID id,
    String fullName,
    String email,
    String role,
    Instant createdAt
) {
}