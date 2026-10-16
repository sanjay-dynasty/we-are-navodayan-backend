package com.wearenavodayan.userservice.service;

import com.wearenavodayan.userservice.entity.UserProfile;
import com.wearenavodayan.userservice.entity.UserProfileInterest;
import com.wearenavodayan.userservice.entity.UserProfileSkill;
import com.wearenavodayan.userservice.repository.UserProfileInterestRepository;
import com.wearenavodayan.userservice.repository.UserProfileRepository;
import com.wearenavodayan.userservice.repository.UserProfileSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class UserProfileSkillsInterestsService {

    private static final int MAX_VALUE_LENGTH = 100;
    private static final int MAX_SKILLS = 30;
    private static final int MAX_INTERESTS = 30;

    private final UserProfileRepository userProfileRepository;
    private final UserProfileSkillRepository skillRepository;
    private final UserProfileInterestRepository interestRepository;

    public UserProfileSkillsInterestsService(
            UserProfileRepository userProfileRepository,
            UserProfileSkillRepository skillRepository,
            UserProfileInterestRepository interestRepository) {
        this.userProfileRepository = userProfileRepository;
        this.skillRepository = skillRepository;
        this.interestRepository = interestRepository;
    }

    @Transactional(readOnly = true)
    public List<String> getSkills(UUID authUserId) {
        UserProfile profile = getProfile(authUserId);
        return skillRepository.findByUserProfileIdOrderBySkillAsc(profile.getId())
                .stream()
                .map(UserProfileSkill::getSkill)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getInterests(UUID authUserId) {
        UserProfile profile = getProfile(authUserId);
        return interestRepository.findByUserProfileIdOrderByInterestAsc(profile.getId())
                .stream()
                .map(UserProfileInterest::getInterest)
                .toList();
    }

    @Transactional
    public List<String> replaceSkills(UUID authUserId, List<String> skills) {
        UserProfile profile = getProfile(authUserId);
        List<String> normalized = normalizeValues(skills, "skills", MAX_SKILLS);

        skillRepository.deleteByUserProfileId(profile.getId());

        List<UserProfileSkill> entities = normalized.stream()
                .map(skill -> new UserProfileSkill(profile, skill))
                .toList();

        skillRepository.saveAll(entities);
        return normalized;
    }

    @Transactional
    public List<String> replaceInterests(UUID authUserId, List<String> interests) {
        UserProfile profile = getProfile(authUserId);
        List<String> normalized = normalizeValues(interests, "interests", MAX_INTERESTS);

        interestRepository.deleteByUserProfileId(profile.getId());

        List<UserProfileInterest> entities = normalized.stream()
                .map(interest -> new UserProfileInterest(profile, interest))
                .toList();

        interestRepository.saveAll(entities);
        return normalized;
    }

    private UserProfile getProfile(UUID authUserId) {
        return userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User profile not found"));
    }

    private List<String> normalizeValues(
            List<String> values, String fieldName, int maxCount) {

        if (values == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }

        if (values.size() > maxCount) {
            throw new IllegalArgumentException(
                    fieldName + " must contain at most " + maxCount + " values");
        }

        Map<String, String> uniqueValues = new LinkedHashMap<>();

        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        fieldName + " must not contain blank values");
            }

            String normalized = value.trim();

            if (normalized.length() > MAX_VALUE_LENGTH) {
                throw new IllegalArgumentException(
                        "Each " + fieldName + " value must be at most "
                                + MAX_VALUE_LENGTH + " characters");
            }

            uniqueValues.putIfAbsent(
                    normalized.toLowerCase(Locale.ROOT), normalized);
        }

        return new ArrayList<>(uniqueValues.values());
    }
}
