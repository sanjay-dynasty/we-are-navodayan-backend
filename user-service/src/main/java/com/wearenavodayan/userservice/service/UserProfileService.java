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

        normalizeProfile(userProfile);
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
        existingProfile.setProfession(
                normalizeOptionalText(updatedProfile.getProfession()));
        existingProfile.setOrganization(
                normalizeOptionalText(updatedProfile.getOrganization()));
        existingProfile.setJobTitle(
                normalizeOptionalText(updatedProfile.getJobTitle()));
        existingProfile.setCurrentCity(
                normalizeOptionalText(updatedProfile.getCurrentCity()));
        existingProfile.setCurrentState(
                normalizeOptionalText(updatedProfile.getCurrentState()));
        existingProfile.setCountry(
                normalizeOptionalText(updatedProfile.getCountry()));

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

        validateRequiredText(profile.getFullName(), 150, "Full name", true);
        validateOptionalText(profile.getBio(), 1000, "Bio");
        validateOptionalText(
                profile.getProfileImageUrl(), 1000, "Profile image URL");
        validateOptionalText(profile.getProfession(), 100, "Profession");
        validateOptionalText(profile.getOrganization(), 150, "Organization");
        validateOptionalText(profile.getJobTitle(), 150, "Job title");
        validateOptionalText(profile.getCurrentCity(), 100, "Current city");
        validateOptionalText(profile.getCurrentState(), 100, "Current state");
        validateOptionalText(profile.getCountry(), 100, "Country");

        if (profile.getBatchYear() != null
                && (profile.getBatchYear() < 1900
                || profile.getBatchYear() > 2200)) {
            throw new IllegalArgumentException(
                    "Batch year must be between 1900 and 2200");
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

    private void validateRequiredText(
            String value, int maxLength, String fieldName, boolean required) {
        if (required && (value == null || value.isBlank())) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        if (value != null && value.trim().length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed " + maxLength
                            + " characters");
        }
    }

    private void validateOptionalText(
            String value, int maxLength, String fieldName) {
        validateRequiredText(value, maxLength, fieldName, false);
    }

    private void normalizeProfile(UserProfile profile) {
        profile.setFullName(profile.getFullName().trim());
        profile.setBio(normalizeOptionalText(profile.getBio()));
        profile.setProfileImageUrl(
                normalizeOptionalText(profile.getProfileImageUrl()));
        profile.setProfession(normalizeOptionalText(profile.getProfession()));
        profile.setOrganization(
                normalizeOptionalText(profile.getOrganization()));
        profile.setJobTitle(normalizeOptionalText(profile.getJobTitle()));
        profile.setCurrentCity(
                normalizeOptionalText(profile.getCurrentCity()));
        profile.setCurrentState(
                normalizeOptionalText(profile.getCurrentState()));
        profile.setCountry(normalizeOptionalText(profile.getCountry()));
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}