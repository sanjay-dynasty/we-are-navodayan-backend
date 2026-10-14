package com.wearenavodayan.authservice.dto;

public record AuthenticatedUserResponse(
        String userId,
        String role
) {
}