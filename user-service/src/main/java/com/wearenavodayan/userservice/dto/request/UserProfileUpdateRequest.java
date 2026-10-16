package com.wearenavodayan.userservice.dto.request;

import java.util.UUID;

public record UserProfileUpdateRequest(
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
        String country
) {
}
