package com.wearenavodayan.userservice.controller;

import com.wearenavodayan.userservice.dto.request.UserProfileUpdateRequest;
import com.wearenavodayan.userservice.entity.UserProfile;
import com.wearenavodayan.userservice.service.ProfileCompletenessCalculator;
import com.wearenavodayan.userservice.service.UserProfileService;
import com.wearenavodayan.userservice.service.UserProfileSkillsInterestsService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private UserProfileSkillsInterestsService skillsInterestsService;

    @Mock
    private ProfileCompletenessCalculator completenessCalculator;

    @InjectMocks
    private UserProfileController controller;

    private UUID authUserId;
    private UUID profileId;
    private UserProfile profile;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        authUserId = UUID.randomUUID();
        profileId = UUID.randomUUID();

        jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(authUserId.toString())
                .build();

        profile = new UserProfile();
        ReflectionTestUtils.setField(profile, "id", profileId);
        profile.setAuthUserId(authUserId);
        profile.setFullName("Test Navodayan");
        profile.setBio("Java developer");
    }

    @Test
    void shouldReturnAuthenticatedUsersProfile() {
        when(userProfileService.getByAuthUserId(authUserId))
                .thenReturn(profile);
        when(skillsInterestsService.getSkills(authUserId))
                .thenReturn(List.of("Java"));
        when(skillsInterestsService.getInterests(authUserId))
                .thenReturn(List.of("Mentoring"));
        when(completenessCalculator.calculate(profile)).thenReturn(40);

        var response = controller.getMyProfile(jwt);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().fullName())
                .isEqualTo("Test Navodayan");
        assertThat(response.getBody().data().skills()).containsExactly("Java");
        assertThat(response.getBody().data().interests())
                .containsExactly("Mentoring");
        assertThat(response.getBody().data().profileCompleteness()).isEqualTo(40);
        verify(userProfileService).getByAuthUserId(authUserId);
    }

    @Test
    void shouldRejectMissingJwt() {
        assertThatThrownBy(() -> controller.getMyProfile(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED");
    }

    @Test
    void shouldRejectInvalidJwtSubject() {
        Jwt invalidJwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("not-a-uuid")
                .build();

        assertThatThrownBy(() -> controller.getMyProfile(invalidJwt))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED");
    }

    @Test
    void shouldReturnProfileById() {
        profile.setFullName("Alumni Member");
        when(userProfileService.getById(profileId)).thenReturn(profile);
        when(skillsInterestsService.getSkills(authUserId)).thenReturn(List.of());
        when(skillsInterestsService.getInterests(authUserId)).thenReturn(List.of());
        when(completenessCalculator.calculate(profile)).thenReturn(20);

        var response = controller.getProfileById(profileId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().fullName())
                .isEqualTo("Alumni Member");
        verify(userProfileService).getById(profileId);
    }

    @Test
    void shouldUpdateAuthenticatedUsersProfile() {
        when(userProfileService.getByAuthUserId(authUserId))
                .thenReturn(profile);
        when(userProfileService.update(eq(profileId), any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(skillsInterestsService.getSkills(authUserId))
                .thenReturn(List.of("Java"));
        when(skillsInterestsService.getInterests(authUserId))
                .thenReturn(List.of("Mentoring"));
        when(completenessCalculator.calculate(any(UserProfile.class)))
                .thenReturn(80);

        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                "Updated Name",
                "Updated bio",
                2010,
                null,
                null,
                "Engineer",
                "Example Organization",
                "Software Engineer",
                "Lucknow",
                "Uttar Pradesh",
                "India");

        var response = controller.updateMyProfile(jwt, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().fullName())
                .isEqualTo("Updated Name");
        assertThat(response.getBody().data().batchYear()).isEqualTo(2010);
        assertThat(response.getBody().data().profession()).isEqualTo("Engineer");
        assertThat(response.getBody().data().currentCity()).isEqualTo("Lucknow");

        org.mockito.ArgumentCaptor<UserProfile> profileCaptor =
                org.mockito.ArgumentCaptor.forClass(UserProfile.class);

        verify(userProfileService).update(eq(profileId), profileCaptor.capture());

        UserProfile candidate = profileCaptor.getValue();
        assertThat(candidate.getAuthUserId()).isEqualTo(authUserId);
        assertThat(candidate.getFullName()).isEqualTo("Updated Name");
        assertThat(candidate.getBatchYear()).isEqualTo(2010);
        assertThat(candidate.getProfession()).isEqualTo("Engineer");
        assertThat(candidate.getCurrentCity()).isEqualTo("Lucknow");

        assertThat(profile.getFullName()).isEqualTo("Test Navodayan");
    }
    @Test
    void shouldRejectInvalidProfileUpdate() {
        when(userProfileService.getByAuthUserId(authUserId))
                .thenReturn(profile);
        when(userProfileService.update(eq(profileId), any(UserProfile.class)))
                .thenThrow(new IllegalArgumentException(
                        "Full name is required"));

        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                " ",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThatThrownBy(() -> controller.updateMyProfile(jwt, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Full name is required");
    }

    @Test
    void shouldPropagateMissingProfileForLookup() {
        when(userProfileService.getById(profileId))
                .thenThrow(new EntityNotFoundException("User profile not found"));

        assertThatThrownBy(() -> controller.getProfileById(profileId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User profile not found");
    }
}
