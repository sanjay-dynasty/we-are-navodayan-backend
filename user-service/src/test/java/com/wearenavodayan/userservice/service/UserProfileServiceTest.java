package com.wearenavodayan.userservice.service;

import com.wearenavodayan.userservice.entity.UserProfile;
import com.wearenavodayan.userservice.repository.UserProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository repository;

    @InjectMocks
    private UserProfileService service;

    @Test
    void shouldCreateProfileWhenUserHasNoExistingProfile() {
        UUID authUserId = UUID.randomUUID();
        UserProfile profile = validProfile(authUserId);

        when(repository.existsByAuthUserId(authUserId)).thenReturn(false);
        when(repository.save(profile)).thenReturn(profile);

        UserProfile result = service.create(profile);

        assertThat(result).isSameAs(profile);
        verify(repository).save(profile);
    }

    @Test
    void shouldRejectDuplicateProfile() {
        UUID authUserId = UUID.randomUUID();

        when(repository.existsByAuthUserId(authUserId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(validProfile(authUserId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("A profile already exists for this user");

        verify(repository, never()).save(any(UserProfile.class));
    }

    @Test
    void shouldRejectBlankFullName() {
        UserProfile profile = validProfile(UUID.randomUUID());
        profile.setFullName("  ");

        assertThatThrownBy(() -> service.create(profile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Full name is required");

        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectInvalidBatchYear() {
        UserProfile profile = validProfile(UUID.randomUUID());
        profile.setBatchYear(1800);

        assertThatThrownBy(() -> service.create(profile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Batch year must be between 1900 and 2200");

        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectNullAuthenticatedUserId() {
        UserProfile profile = validProfile(null);

        assertThatThrownBy(() -> service.create(profile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Authenticated user ID is required");

        verifyNoInteractions(repository);
    }

    @Test
    void shouldNotAllowChangingProfileOwnership() {
        UUID profileId = UUID.randomUUID();
        UUID originalOwner = UUID.randomUUID();
        UUID anotherOwner = UUID.randomUUID();

        UserProfile existing = validProfile(originalOwner);
        UserProfile update = validProfile(anotherOwner);

        when(repository.findById(profileId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(profileId, update))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Profile ownership cannot be changed");

        verify(repository, never()).save(any(UserProfile.class));
    }

    @Test
    void shouldUpdateProfileWithoutChangingOwner() {
        UUID profileId = UUID.randomUUID();
        UUID authUserId = UUID.randomUUID();

        UserProfile existing = validProfile(authUserId);
        UserProfile update = validProfile(authUserId);
        update.setFullName(" Updated Navodayan ");
        update.setBio("  New bio  ");

        when(repository.findById(profileId)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        UserProfile result = service.update(profileId, update);

        assertThat(result.getAuthUserId()).isEqualTo(authUserId);
        assertThat(result.getFullName()).isEqualTo("Updated Navodayan");
        assertThat(result.getBio()).isEqualTo("New bio");

        verify(repository).save(existing);
    }

    @Test
    void shouldThrowWhenProfileDoesNotExist() {
        UUID profileId = UUID.randomUUID();

        when(repository.findById(profileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(profileId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User profile not found");
    }

    private UserProfile validProfile(UUID authUserId) {
        UserProfile profile = new UserProfile();
        profile.setAuthUserId(authUserId);
        profile.setFullName("Test Navodayan");
        profile.setBio("Test bio");
        profile.setBatchYear(2018);
        profile.setJnvId(UUID.randomUUID());
        profile.setProfileImageUrl("https://example.com/profile.jpg");
        return profile;
    }
}