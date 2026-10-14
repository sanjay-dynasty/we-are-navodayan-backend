package com.wearenavodayan.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 150, message = "Full name must contain 2 to 150 characters")
    String fullName,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 320, message = "Email must not exceed 320 characters")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 12, max = 72, message = "Password must contain 12 to 72 characters")
    String password

) {
}