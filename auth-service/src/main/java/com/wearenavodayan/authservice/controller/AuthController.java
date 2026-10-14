package com.wearenavodayan.authservice.controller;

import com.wearenavodayan.authservice.dto.request.RegistrationRequest;
import com.wearenavodayan.authservice.dto.response.ApiResponse;
import com.wearenavodayan.authservice.dto.response.RegisteredUserResponse;
import com.wearenavodayan.authservice.service.AuthRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthRegistrationService authRegistrationService;

    public AuthController(AuthRegistrationService authRegistrationService) {
        this.authRegistrationService = authRegistrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisteredUserResponse>> register(
            @Valid @RequestBody RegistrationRequest request) {

        RegisteredUserResponse registeredUser =
            authRegistrationService.register(request);

        ApiResponse<RegisteredUserResponse> response = new ApiResponse<>(
            true,
            registeredUser,
            "User registered successfully"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}