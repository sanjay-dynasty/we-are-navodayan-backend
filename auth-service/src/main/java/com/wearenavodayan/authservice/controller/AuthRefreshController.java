package com.wearenavodayan.authservice.controller;

import java.util.Map;

import com.wearenavodayan.authservice.dto.LoginResponse;
import com.wearenavodayan.authservice.dto.RefreshRequest;
import com.wearenavodayan.authservice.service.InvalidRefreshTokenException;
import com.wearenavodayan.authservice.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthRefreshController {

    private final RefreshTokenService refreshTokenService;

    public AuthRefreshController(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshRequest request) {

        return ResponseEntity.ok(
                refreshTokenService.refresh(request.refreshToken()));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRefreshToken(
            InvalidRefreshTokenException exception) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "code", "INVALID_REFRESH_TOKEN",
                        "message", exception.getMessage()));
    }
}