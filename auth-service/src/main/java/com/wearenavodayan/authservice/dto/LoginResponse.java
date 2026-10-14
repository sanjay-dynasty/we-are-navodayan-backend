package com.wearenavodayan.authservice.dto;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserDetails user
) {
    public record UserDetails(
            UUID id,
            String fullName,
            String email,
            String role
    ) {
    }
}