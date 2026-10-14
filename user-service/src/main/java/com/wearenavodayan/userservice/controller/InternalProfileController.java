package com.wearenavodayan.userservice.controller;

import com.wearenavodayan.userservice.dto.request.InternalProfileCreateRequest;
import com.wearenavodayan.userservice.dto.response.ApiResponse;
import com.wearenavodayan.userservice.dto.response.InternalProfileResponse;
import com.wearenavodayan.userservice.entity.UserProfile;
import com.wearenavodayan.userservice.service.UserProfileService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/api/v1/user-profiles")
public class InternalProfileController {

    private static final String INTERNAL_KEY_HEADER =
            "X-Internal-Service-Key";

    private final UserProfileService userProfileService;
    private final String internalApiKey;

    public InternalProfileController(
            UserProfileService userProfileService,
            @Value("${services.internal.api-key}") String internalApiKey) {
        this.userProfileService = userProfileService;
        this.internalApiKey = internalApiKey;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InternalProfileResponse>> create(
            @RequestHeader(
                    name = INTERNAL_KEY_HEADER,
                    required = false) String suppliedKey,
            @RequestBody InternalProfileCreateRequest request) {

        if (suppliedKey == null
                || !MessageDigest.isEqual(
                        internalApiKey.getBytes(StandardCharsets.UTF_8),
                        suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid internal service credentials");
        }

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Profile request is required");
        }

        if (request.authUserId() == null
                || request.fullName() == null
                || request.fullName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Auth user ID and full name are required");
        }

        String fullName = request.fullName().trim();

        if (fullName.length() > 150) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Full name must not exceed 150 characters");
        }

        if (userProfileService.existsByAuthUserId(request.authUserId())) {
            UserProfile existing =
                    userProfileService.getByAuthUserId(request.authUserId());

            if (!existing.getFullName().equals(fullName)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A profile already exists with different details");
            }

            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    toResponse(existing),
                    "Profile already exists"));
        }

        UserProfile profile = new UserProfile();
        profile.setAuthUserId(request.authUserId());
        profile.setFullName(fullName);

        UserProfile saved = userProfileService.create(profile);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(
                        true,
                        toResponse(saved),
                        "Profile created successfully"));
    }

    private InternalProfileResponse toResponse(UserProfile profile) {
        return new InternalProfileResponse(
                profile.getId(),
                profile.getAuthUserId(),
                profile.getFullName(),
                profile.getCreatedAt());
    }
}