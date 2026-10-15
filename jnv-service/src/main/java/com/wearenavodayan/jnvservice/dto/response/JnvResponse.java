package com.wearenavodayan.jnvservice.dto.response;

import com.wearenavodayan.jnvservice.entity.Jnv;

import java.util.UUID;

public record JnvResponse(
        UUID id,
        String schoolCode,
        String schoolName,
        String state,
        String district,
        String address,
        String pinCode,
        boolean active
) {
    public static JnvResponse from(Jnv jnv) {
        return new JnvResponse(
                jnv.getId(),
                jnv.getSchoolCode(),
                jnv.getSchoolName(),
                jnv.getState(),
                jnv.getDistrict(),
                jnv.getAddress(),
                jnv.getPinCode(),
                jnv.isActive()
        );
    }
}