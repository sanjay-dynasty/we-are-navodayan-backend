package com.wearenavodayan.userservice.service;

import com.wearenavodayan.userservice.client.JnvServiceClient;
import com.wearenavodayan.userservice.dto.response.ApiResponse;
import com.wearenavodayan.userservice.entity.UserProfile;
import com.wearenavodayan.userservice.exception.JnvNotFoundException;
import com.wearenavodayan.userservice.exception.JnvServiceClientException;
import com.wearenavodayan.userservice.repository.UserProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final JnvServiceClient jnvServiceClient;

    public UserProfileService(
            UserProfileRepository userProfileRepository,
            JnvServiceClient jnvServiceClient) {
        this.userProfileRepository = userProfileRepository;
        this.jnvServiceClient = jnvServiceClient;
    }

    @Transactional
    public UserProfile create(UserProfile userProfile) {
        validateProfile(userProfile);

        if (userProfileRepository.existsByAuthUserId(userProfile.getAuthUserId())) {
            throw new IllegalStateException(
                    "A profile already exists for this user");
        }

        return userProfileRepository.save(userProfile);
    }

    public UserProfile getById(UUID id) {
        return userProfileRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("User profile not found"));
    }

    public UserProfile getByAuthUserId(UUID authUserId) {
        if (authUserId == null) {
            throw new IllegalArgumentException(
                    "Authenticated user ID is required");
        }

        return userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() ->
                        new EntityNotFoundException("User profile not found"));
    }

    public List<UserProfile> getByJnvId(UUID jnvId) {
        if (jnvId == null) {
            throw new IllegalArgumentException("JNV ID is required");
        }

        return userProfileRepository.findByJnvId(jnvId);
    }

    public List<UserProfile> getAll() {
        return userProfileRepository.findAll();
    }

    @Transactional
    public UserProfile update(UUID id, UserProfile updatedProfile) {
        if (id == null) {
            throw new IllegalArgumentException("Profile ID is required");
        }

        validateProfile(updatedProfile);

        UserProfile existingProfile = getById(id);

        if (!existingProfile.getAuthUserId()
                .equals(updatedProfile.getAuthUserId())) {
            throw new IllegalArgumentException(
                    "Profile ownership cannot be changed");
        }

        existingProfile.setFullName(updatedProfile.getFullName().trim());
        existingProfile.setBio(normalizeOptionalText(updatedProfile.getBio()));
        existingProfile.setBatchYear(updatedProfile.getBatchYear());
        existingProfile.setJnvId(updatedProfile.getJnvId());
        existingProfile.setProfileImageUrl(
                normalizeOptionalText(updatedProfile.getProfileImageUrl()));

        return userProfileRepository.save(existingProfile);
    }

    @Transactional
    public void delete(UUID id) {
        UserProfile userProfile = getById(id);
        userProfileRepository.delete(userProfile);
    }

    public boolean existsByAuthUserId(UUID authUserId) {
        if (authUserId == null) {
            return false;
        }

        return userProfileRepository.existsByAuthUserId(authUserId);
    }

    private void validateProfile(UserProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException(
                    "Profile request is required");
        }

        if (profile.getAuthUserId() == null) {
            throw new IllegalArgumentException(
                    "Authenticated user ID is required");
        }

        if (profile.getFullName() == null
                || profile.getFullName().isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }

        if (profile.getFullName().trim().length() > 150) {
            throw new IllegalArgumentException(
                    "Full name must not exceed 150 characters");
        }

        if (profile.getBio() != null && profile.getBio().length() > 1000) {
            throw new IllegalArgumentException(
                    "Bio must not exceed 1000 characters");
        }

        if (profile.getBatchYear() != null
                && (profile.getBatchYear() < 1900
                || profile.getBatchYear() > 2200)) {
            throw new IllegalArgumentException(
                    "Batch year must be between 1900 and 2200");
        }

        if (profile.getProfileImageUrl() != null
                && profile.getProfileImageUrl().length() > 1000) {
            throw new IllegalArgumentException(
                    "Profile image URL must not exceed 1000 characters");
        }

        if (profile.getJnvId() != null) {
            try {
                ApiResponse<Object> response =
                        jnvServiceClient.getJnvById(
                                profile.getJnvId().toString());

                if (response == null || !response.success()) {
                    throw new JnvServiceClientException(
                            "JNV service returned an invalid response");
                }
            } catch (JnvNotFoundException ex) {
                throw new EntityNotFoundException(
                        "JNV not found: " + profile.getJnvId());
            }
        }
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}