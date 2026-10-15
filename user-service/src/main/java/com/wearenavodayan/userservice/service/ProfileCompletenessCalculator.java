package com.wearenavodayan.userservice.service;

import com.wearenavodayan.userservice.entity.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class ProfileCompletenessCalculator {

    private static final int FIELD_WEIGHT = 20;

    public int calculate(UserProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Profile is required");
        }

        int completedFields = 0;

        if (hasText(profile.getFullName())) {
            completedFields++;
        }

        if (hasText(profile.getBio())) {
            completedFields++;
        }

        if (profile.getBatchYear() != null) {
            completedFields++;
        }

        if (profile.getJnvId() != null) {
            completedFields++;
        }

        if (hasText(profile.getProfileImageUrl())) {
            completedFields++;
        }

        return completedFields * FIELD_WEIGHT;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}