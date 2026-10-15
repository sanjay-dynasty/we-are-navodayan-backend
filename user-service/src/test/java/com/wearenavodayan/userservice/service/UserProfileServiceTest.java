package com.wearenavodayan.userservice.service;

import com.wearenavodayan.userservice.client.JnvServiceClient;
import com.wearenavodayan.userservice.dto.response.ApiResponse;
import com.wearenavodayan.userservice.entity.UserProfile;
import com.wearenavodayan.userservice.exception.JnvNotFoundException;
import com.wearenavodayan.userservice.exception.JnvServiceClientException;
import com.wearenavodayan.userservice.repository.UserProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository repository;

    @Mock
    private JnvServiceClient jnvServiceClient;

    @InjectMocks
    private UserProfileService service;

    @BeforeEach
    void setUp() {
        lenient().when(jnvServiceClient.getJnvById(anyString()))
                .thenReturn(new ApiResponse<>(
                        true, new Object(), "JNV retrieved successfully"));
    }

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
        verifyNoInteractions(jnvServiceClient);
    }

    @Test
    void shouldRejectInvalidBatchYear() {
        UserProfile profile = validProfile(UUID.randomUUID());
        profile.setBatchYear(1800);

        assertThatThrownBy(() -> service.create(profile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Batch year must be between 1900 and 2200");

        verifyNoInteractions(repository);
        verifyNoInteractions(jnvServiceClient);
    }

    @Test
    void shouldRejectNullAuthenticatedUserId() {
        UserProfile profile = validProfile(null);

        assertThatThrownBy(() -> service.create(profile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Authenticated user ID is required");

        verifyNoInteractions(repository);
        verifyNoInteractions(jnvServiceClient);
    }

    @Test
    void shouldRejectUnknownJnv() {
        UserProfile profile = validProfile(UUID.randomUUID());
        String jnvId = profile.getJnvId().toString();

        when(jnvServiceClient.getJnvById(jnvId))
                .thenThrow(new JnvNotFoundException("JNV not found: " + jnvId));

        assertThatThrownBy(() -> service.create(profile))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("JNV not found: " + jnvId);

        verify(repository, never()).save(any(UserProfile.class));
        verify(repository, never()).existsByAuthUserId(any(UUID.class));
    }

    @Test
    void shouldPropagateJnvServiceCommunicationFailure() {
        UserProfile profile = validProfile(UUID.randomUUID());

        when(jnvServiceClient.getJnvById(profile.getJnvId().toString()))
                .thenThrow(new JnvServiceClientException(
                        "Unable to communicate with JNV service"));

        assertThatThrownBy(() -> service.create(profile))
                .isInstanceOf(JnvServiceClientException.class)
                .hasMessage("Unable to communicate with JNV service");

        verify(repository, never()).save(any(UserProfile.class));
        verify(repository, never()).existsByAuthUserId(any(UUID.class));
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

    @Test
    void shouldCreateProfileWithProfessionAndLocationDetails() {
        UUID authUserId = UUID.randomUUID();
        UserProfile profile = validProfile(authUserId);

        profile.setProfession("Software Engineer");
        profile.setOrganization("EPAM Systems");
        profile.setJobTitle("Software Engineer A2");
        profile.setCurrentCity("Pune");
        profile.setCurrentState("Maharashtra");
        profile.setCountry("India");

        when(repository.existsByAuthUserId(authUserId)).thenReturn(false);
        when(repository.save(profile)).thenReturn(profile);

        UserProfile result = service.create(profile);

        assertThat(result.getProfession()).isEqualTo("Software Engineer");
        assertThat(result.getOrganization()).isEqualTo("EPAM Systems");
        assertThat(result.getJobTitle()).isEqualTo("Software Engineer A2");
        assertThat(result.getCurrentCity()).isEqualTo("Pune");
        assertThat(result.getCurrentState()).isEqualTo("Maharashtra");
        assertThat(result.getCountry()).isEqualTo("India");

        verify(repository).save(profile);
    }

    @Test
    void shouldNormalizeProfessionAndLocationFieldsWhenCreatingProfile() {
        UUID authUserId = UUID.randomUUID();
        UserProfile profile = validProfile(authUserId);

        profile.setProfession("  Software Engineer  ");
        profile.setOrganization("  EPAM Systems  ");
        profile.setJobTitle("  Backend Developer  ");
        profile.setCurrentCity("  Pune  ");
        profile.setCurrentState("  Maharashtra  ");
        profile.setCountry("  India  ");

        when(repository.existsByAuthUserId(authUserId)).thenReturn(false);
        when(repository.save(profile)).thenReturn(profile);

        service.create(profile);

        assertThat(profile.getProfession()).isEqualTo("Software Engineer");
        assertThat(profile.getOrganization()).isEqualTo("EPAM Systems");
        assertThat(profile.getJobTitle()).isEqualTo("Backend Developer");
        assertThat(profile.getCurrentCity()).isEqualTo("Pune");
        assertThat(profile.getCurrentState()).isEqualTo("Maharashtra");
        assertThat(profile.getCountry()).isEqualTo("India");
    }

    @Test
    void shouldNormalizeBlankOptionalProfessionAndLocationFieldsToNull() {
        UUID authUserId = UUID.randomUUID();
        UserProfile profile = validProfile(authUserId);

        profile.setProfession("  ");
        profile.setOrganization("");
        profile.setJobTitle(" ");
        profile.setCurrentCity("  ");
        profile.setCurrentState("");
        profile.setCountry(" ");

        when(repository.existsByAuthUserId(authUserId)).thenReturn(false);
        when(repository.save(profile)).thenReturn(profile);

        service.create(profile);

        assertThat(profile.getProfession()).isNull();
        assertThat(profile.getOrganization()).isNull();
        assertThat(profile.getJobTitle()).isNull();
        assertThat(profile.getCurrentCity()).isNull();
        assertThat(profile.getCurrentState()).isNull();
        assertThat(profile.getCountry()).isNull();
    }

    @Test
    void shouldRejectProfessionExceedingMaximumLength() {
        UserProfile profile = validProfile(UUID.randomUUID());
        profile.setProfession("P".repeat(101));

        assertThatThrownBy(() -> service.create(profile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Profession must not exceed 100 characters");

        verifyNoInteractions(repository);
        verifyNoInteractions(jnvServiceClient);
    }

    @Test
    void shouldUpdateProfessionAndLocationDetails() {
        UUID profileId = UUID.randomUUID();
        UUID authUserId = UUID.randomUUID();

        UserProfile existing = validProfile(authUserId);
        UserProfile update = validProfile(authUserId);

        update.setProfession("Data Engineer");
        update.setOrganization("Example Corp");
        update.setJobTitle("Senior Engineer");
        update.setCurrentCity("Bengaluru");
        update.setCurrentState("Karnataka");
        update.setCountry("India");

        when(repository.findById(profileId)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        UserProfile result = service.update(profileId, update);

        assertThat(result.getProfession()).isEqualTo("Data Engineer");
        assertThat(result.getOrganization()).isEqualTo("Example Corp");
        assertThat(result.getJobTitle()).isEqualTo("Senior Engineer");
        assertThat(result.getCurrentCity()).isEqualTo("Bengaluru");
        assertThat(result.getCurrentState()).isEqualTo("Karnataka");
        assertThat(result.getCountry()).isEqualTo("India");

        verify(repository).save(existing);
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