package com.wearenavodayan.userservice.dto.request;

import java.util.UUID;

public record InternalProfileCreateRequest(
        UUID authUserId,
        String fullName
) {
}