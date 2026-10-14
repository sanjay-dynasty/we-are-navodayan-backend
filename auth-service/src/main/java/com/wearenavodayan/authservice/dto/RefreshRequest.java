package com.wearenavodayan.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
        @NotBlank
        @Size(max = 200)
        String refreshToken
) {
}