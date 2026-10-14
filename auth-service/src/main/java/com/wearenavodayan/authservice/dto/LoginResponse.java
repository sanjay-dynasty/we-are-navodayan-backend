package com.wearenavodayan.authservice.dto;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        long refreshTokenExpiresIn,
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