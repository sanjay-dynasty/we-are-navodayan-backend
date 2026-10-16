package com.wearenavodayan.userservice.service;

import com.wearenavodayan.userservice.entity.UserProfile;
import com.wearenavodayan.userservice.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ProfileImageMetadataService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final UserProfileRepository userProfileRepository;

    public ProfileImageMetadataService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public UserProfile updateMetadata(
            UUID authUserId,
            String storageKey,
            String contentType,
            Long sizeBytes,
            String originalFilename) {

        UserProfile profile = userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User profile not found"));

        String normalizedStorageKey = requireText(
                storageKey, 500, "storageKey");
        String normalizedContentType = requireText(
                contentType, 100, "contentType").toLowerCase(Locale.ROOT);
        String normalizedFilename = requireText(
                originalFilename, 255, "originalFilename");

        if (!ALLOWED_CONTENT_TYPES.contains(normalizedContentType)) {
            throw new IllegalArgumentException(
                    "Only JPEG, PNG, and WebP images are supported");
        }

        if (sizeBytes == null || sizeBytes <= 0) {
            throw new IllegalArgumentException(
                    "Image size must be greater than zero");
        }

        if (sizeBytes > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "Image size must not exceed 5 MB");
        }

        profile.setProfileImageStorageKey(normalizedStorageKey);
        profile.setProfileImageContentType(normalizedContentType);
        profile.setProfileImageSizeBytes(sizeBytes);
        profile.setProfileImageOriginalFilename(normalizedFilename);
        profile.setProfileImageUpdatedAt(Instant.now());

        return userProfileRepository.save(profile);
    }

    @Transactional
    public void clearMetadata(UUID authUserId) {
        UserProfile profile = userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User profile not found"));

        profile.setProfileImageStorageKey(null);
        profile.setProfileImageContentType(null);
        profile.setProfileImageSizeBytes(null);
        profile.setProfileImageOriginalFilename(null);
        profile.setProfileImageUpdatedAt(null);
        profile.setProfileImageUrl(null);

        userProfileRepository.save(profile);
    }

    private String requireText(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed " + maxLength + " characters");
        }

        return normalized;
    }
}
