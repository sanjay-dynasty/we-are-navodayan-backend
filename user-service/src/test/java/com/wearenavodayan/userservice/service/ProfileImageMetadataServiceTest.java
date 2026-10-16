package com.wearenavodayan.userservice.service;

import com.wearenavodayan.userservice.entity.UserProfile;
import com.wearenavodayan.userservice.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileImageMetadataServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    private ProfileImageMetadataService service;
    private UUID authUserId;
    private UserProfile profile;

    @BeforeEach
    void setUp() {
        service = new ProfileImageMetadataService(userProfileRepository);
        authUserId = UUID.randomUUID();
        profile = new UserProfile();
        profile.setAuthUserId(authUserId);
    }

    @Test
    void updateMetadataTrimsValuesAndNormalizesContentType() {
        when(userProfileRepository.findByAuthUserId(authUserId))
                .thenReturn(Optional.of(profile));
        when(userProfileRepository.save(profile)).thenReturn(profile);

        UserProfile result = service.updateMetadata(
                authUserId, " profiles/123/avatar.webp ",
                "IMAGE/WEBP", 1024L, " avatar.webp ");

        assertEquals("profiles/123/avatar.webp", result.getProfileImageStorageKey());
        assertEquals("image/webp", result.getProfileImageContentType());
        assertEquals(1024L, result.getProfileImageSizeBytes());
        assertEquals("avatar.webp", result.getProfileImageOriginalFilename());
        assertNotNull(result.getProfileImageUpdatedAt());
        verify(userProfileRepository).save(profile);
    }

    @Test
    void updateMetadataRejectsUnsupportedContentType() {
        when(userProfileRepository.findByAuthUserId(authUserId))
                .thenReturn(Optional.of(profile));

        assertThrows(IllegalArgumentException.class, () ->
                service.updateMetadata(
                        authUserId, "profiles/123/avatar.gif",
                        "image/gif", 1024L, "avatar.gif"));
    }

    @Test
    void updateMetadataRejectsEmptyStorageKey() {
        when(userProfileRepository.findByAuthUserId(authUserId))
                .thenReturn(Optional.of(profile));

        assertThrows(IllegalArgumentException.class, () ->
                service.updateMetadata(
                        authUserId, " ", "image/jpeg", 1024L, "avatar.jpg"));
    }

    @Test
    void updateMetadataRejectsNullOrNonPositiveSize() {
        when(userProfileRepository.findByAuthUserId(authUserId))
                .thenReturn(Optional.of(profile));

        assertThrows(IllegalArgumentException.class, () ->
                service.updateMetadata(
                        authUserId, "profiles/123/avatar.jpg",
                        "image/jpeg", null, "avatar.jpg"));

        assertThrows(IllegalArgumentException.class, () ->
                service.updateMetadata(
                        authUserId, "profiles/123/avatar.jpg",
                        "image/jpeg", 0L, "avatar.jpg"));
    }

    @Test
    void updateMetadataRejectsImagesLargerThanFiveMb() {
        when(userProfileRepository.findByAuthUserId(authUserId))
                .thenReturn(Optional.of(profile));

        assertThrows(IllegalArgumentException.class, () ->
                service.updateMetadata(
                        authUserId, "profiles/123/avatar.jpg",
                        "image/jpeg", 5L * 1024 * 1024 + 1, "avatar.jpg"));
    }

    @Test
    void updateMetadataRejectsMissingProfile() {
        when(userProfileRepository.findByAuthUserId(authUserId))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                service.updateMetadata(
                        authUserId, "profiles/123/avatar.jpg",
                        "image/jpeg", 1024L, "avatar.jpg"));
    }

    @Test
    void clearMetadataRemovesImageFields() {
        when(userProfileRepository.findByAuthUserId(authUserId))
                .thenReturn(Optional.of(profile));
        when(userProfileRepository.save(profile)).thenReturn(profile);

        profile.setProfileImageUrl("https://example.com/avatar.jpg");
        profile.setProfileImageStorageKey("profiles/123/avatar.jpg");
        profile.setProfileImageContentType("image/jpeg");
        profile.setProfileImageSizeBytes(1024L);
        profile.setProfileImageOriginalFilename("avatar.jpg");

        service.clearMetadata(authUserId);

        assertNull(profile.getProfileImageUrl());
        assertNull(profile.getProfileImageStorageKey());
        assertNull(profile.getProfileImageContentType());
        assertNull(profile.getProfileImageSizeBytes());
        assertNull(profile.getProfileImageOriginalFilename());
        assertNull(profile.getProfileImageUpdatedAt());
        verify(userProfileRepository).save(profile);
    }
}
