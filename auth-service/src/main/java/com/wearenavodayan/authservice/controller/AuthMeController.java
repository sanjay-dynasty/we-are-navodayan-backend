package com.wearenavodayan.authservice.controller;

import com.wearenavodayan.authservice.dto.AuthenticatedUserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthMeController {

    @GetMapping("/me")
    public AuthenticatedUserResponse getCurrentUser(
            @AuthenticationPrincipal Jwt jwt) {

        return new AuthenticatedUserResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("role")
        );
    }
}