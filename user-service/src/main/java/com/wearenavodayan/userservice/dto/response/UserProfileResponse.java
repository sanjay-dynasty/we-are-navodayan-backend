package com.wearenavodayan.userservice.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String fullName,
        String bio,
        Integer batchYear,
        UUID jnvId,
        String profileImageUrl,
        String profession,
        String organization,
        String jobTitle,
        String currentCity,
        String currentState,
        String country,
        List<String> skills,
        List<String> interests,
        int profileCompleteness,
        Instant createdAt,
        Instant updatedAt
) {
}
