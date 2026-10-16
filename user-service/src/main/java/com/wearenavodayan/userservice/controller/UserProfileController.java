package com.wearenavodayan.userservice.controller;

import com.wearenavodayan.userservice.dto.request.UserProfileUpdateRequest;
import com.wearenavodayan.userservice.dto.response.ApiResponse;
import com.wearenavodayan.userservice.dto.response.UserProfileResponse;
import com.wearenavodayan.userservice.entity.UserProfile;
import com.wearenavodayan.userservice.service.ProfileCompletenessCalculator;
import com.wearenavodayan.userservice.service.UserProfileService;
import com.wearenavodayan.userservice.service.UserProfileSkillsInterestsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user-profiles")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserProfileSkillsInterestsService skillsInterestsService;
    private final ProfileCompletenessCalculator completenessCalculator;

    public UserProfileController(
            UserProfileService userProfileService,
            UserProfileSkillsInterestsService skillsInterestsService,
            ProfileCompletenessCalculator completenessCalculator) {
        this.userProfileService = userProfileService;
        this.skillsInterestsService = skillsInterestsService;
        this.completenessCalculator = completenessCalculator;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal Jwt jwt) {
        UUID authUserId = authenticatedUserId(jwt);
        UserProfile profile = userProfileService.getByAuthUserId(authUserId);

        return ResponseEntity.ok(new ApiResponse<>(
                true, toResponse(profile), "Profile retrieved successfully"));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UserProfileUpdateRequest request) {
        UUID authUserId = authenticatedUserId(jwt);

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Profile request is required");
        }

        UserProfile existing = userProfileService.getByAuthUserId(authUserId);

        UserProfile candidate = new UserProfile();
        candidate.setAuthUserId(existing.getAuthUserId());
        candidate.setFullName(request.fullName());
        candidate.setBio(request.bio());
        candidate.setBatchYear(request.batchYear());
        candidate.setJnvId(request.jnvId());
        candidate.setProfileImageUrl(request.profileImageUrl());
        candidate.setProfession(request.profession());
        candidate.setOrganization(request.organization());
        candidate.setJobTitle(request.jobTitle());
        candidate.setCurrentCity(request.currentCity());
        candidate.setCurrentState(request.currentState());
        candidate.setCountry(request.country());

        UserProfile saved = userProfileService.update(existing.getId(), candidate);

        return ResponseEntity.ok(new ApiResponse<>(
                true, toResponse(saved), "Profile updated successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfileById(
            @PathVariable UUID id) {
        UserProfile profile = userProfileService.getById(id);

        return ResponseEntity.ok(new ApiResponse<>(
                true, toResponse(profile), "Profile retrieved successfully"));
    }

    private UUID authenticatedUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid authenticated user ID");
        }
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        UUID authUserId = profile.getAuthUserId();

        return new UserProfileResponse(
                profile.getId(),
                profile.getFullName(),
                profile.getBio(),
                profile.getBatchYear(),
                profile.getJnvId(),
                profile.getProfileImageUrl(),
                profile.getProfession(),
                profile.getOrganization(),
                profile.getJobTitle(),
                profile.getCurrentCity(),
                profile.getCurrentState(),
                profile.getCountry(),
                skillsInterestsService.getSkills(authUserId),
                skillsInterestsService.getInterests(authUserId),
                completenessCalculator.calculate(profile),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}
