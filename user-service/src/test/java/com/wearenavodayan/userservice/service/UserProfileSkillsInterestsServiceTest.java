package com.wearenavodayan.userservice.service;

import com.wearenavodayan.userservice.entity.UserProfile;
import com.wearenavodayan.userservice.entity.UserProfileInterest;
import com.wearenavodayan.userservice.entity.UserProfileSkill;
import com.wearenavodayan.userservice.repository.UserProfileInterestRepository;
import com.wearenavodayan.userservice.repository.UserProfileRepository;
import com.wearenavodayan.userservice.repository.UserProfileSkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileSkillsInterestsServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserProfileSkillRepository skillRepository;

    @Mock
    private UserProfileInterestRepository interestRepository;

    private UserProfileSkillsInterestsService service;

    private UUID authUserId;
    private UserProfile profile;

    @BeforeEach
    void setUp() {
        service = new UserProfileSkillsInterestsService(
                userProfileRepository, skillRepository, interestRepository);

        authUserId = UUID.randomUUID();
        profile = new UserProfile();
        profile.setAuthUserId(authUserId);
    }

    @Test
    void replaceSkillsTrimsValuesAndRemovesCaseInsensitiveDuplicates() {
        when(userProfileRepository.findByAuthUserId(authUserId))
                .thenReturn(Optional.of(profile));

        List<String> result = service.replaceSkills(
                authUserId, List.of(" Java ", "Spring Boot", "java", " Kafka "));

        assertEquals(List.of("Java", "Spring Boot", "Kafka"), result);
        verify(skillRepository).deleteByUserProfileId(profile.getId());
        verify(skillRepository).saveAll(anyList());
    }

    @Test
    void replaceInterestsTrimsValuesAndRemovesDuplicates() {
        when(userProfileRepository.findByAuthUserId(authUserId))
                .thenReturn(Optional.of(profile));

        List<String> result = service.replaceInterests(
                authUserId, List.of(" Reading ", "Travel", "reading"));

        assertEquals(List.of("Reading", "Travel"), result);
        verify(interestRepository).deleteByUserProfileId(profile.getId());
        verify(interestRepository).saveAll(anyList());
    }

    @Test
    void replaceSkillsRejectsNullListBeforeDeletingExistingValues() {
        when(userProfileRepository.findByAuthUserId(authUserId))
                .thenReturn(Optional.of(profile));

        assertThrows(IllegalArgumentException.class,
                () -> service.replaceSkills(authUserId, null));
    }

    @Test
    void replaceSkillsRejectsBlankValueBeforeDeletingExistingValues() {
        when(userProfileRepository.findByAuthUserId(authUserId))
                .thenReturn(Optional.of(profile));

        assertThrows(IllegalArgumentException.class,
                () -> service.replaceSkills(authUserId, List.of("Java", "  ")));
    }

    @Test
    void replaceSkillsRejectsValuesLongerThan100Characters() {
        when(userProfileRepository.findByAuthUserId(authUserId))
                .thenReturn(Optional.of(profile));

        assertThrows(IllegalArgumentException.class,
                () -> service.replaceSkills(authUserId, List.of("a".repeat(101))));
    }

    @Test
    void replaceSkillsRejectsMoreThan30Values() {
        when(userProfileRepository.findByAuthUserId(authUserId))
                .thenReturn(Optional.of(profile));

        List<String> skills = java.util.stream.IntStream.range(0, 31)
                .mapToObj(index -> "Skill " + index)
                .toList();

        assertThrows(IllegalArgumentException.class,
                () -> service.replaceSkills(authUserId, skills));
    }

    @Test
    void replaceInterestsRejectsNullElement() {
        when(userProfileRepository.findByAuthUserId(authUserId))
                .thenReturn(Optional.of(profile));

        List<String> interests = java.util.Arrays.asList("Reading", null);

        assertThrows(IllegalArgumentException.class,
                () -> service.replaceInterests(authUserId, interests));
    }

    @Test
    void replaceSkillsAllowsEmptyListToClearValues() {
        when(userProfileRepository.findByAuthUserId(authUserId))
                .thenReturn(Optional.of(profile));

        assertEquals(List.of(), service.replaceSkills(authUserId, List.of()));

        verify(skillRepository).deleteByUserProfileId(profile.getId());
        verify(skillRepository).saveAll(anyList());
    }

    @Test
    void getSkillsReturnsSortedRepositoryValues() {
        when(userProfileRepository.findByAuthUserId(authUserId))
                .thenReturn(Optional.of(profile));
        when(skillRepository.findByUserProfileIdOrderBySkillAsc(profile.getId()))
                .thenReturn(List.of(
                        new UserProfileSkill(profile, "Java"),
                        new UserProfileSkill(profile, "Spring Boot")));

        assertEquals(List.of("Java", "Spring Boot"), service.getSkills(authUserId));
    }

    @Test
    void getInterestsReturnsRepositoryValues() {
        when(userProfileRepository.findByAuthUserId(authUserId))
                .thenReturn(Optional.of(profile));
        when(interestRepository.findByUserProfileIdOrderByInterestAsc(profile.getId()))
                .thenReturn(List.of(
                        new UserProfileInterest(profile, "Reading"),
                        new UserProfileInterest(profile, "Travel")));

        assertEquals(List.of("Reading", "Travel"), service.getInterests(authUserId));
    }

    @Test
    void throwsWhenProfileDoesNotExist() {
        when(userProfileRepository.findByAuthUserId(authUserId))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.getSkills(authUserId));
    }
}
