package com.wearenavodayan.jnvservice.controller;

import com.wearenavodayan.jnvservice.dto.response.ApiResponse;
import com.wearenavodayan.jnvservice.dto.response.JnvResponse;
import com.wearenavodayan.jnvservice.service.JnvService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jnvs")
public class JnvController {

    private final JnvService jnvService;

    public JnvController(JnvService jnvService) {
        this.jnvService = jnvService;
    }

    @GetMapping
    public ApiResponse<List<JnvResponse>> getAllActive() {
        List<JnvResponse> responses = jnvService.getActiveJnvs()
                .stream()
                .map(JnvResponse::from)
                .toList();

        return new ApiResponse<>(
                true,
                responses,
                "Active JNVs retrieved successfully"
        );
    }

    @GetMapping("/{jnvId}")
    public ApiResponse<JnvResponse> getById(
            @PathVariable UUID jnvId) {

        JnvResponse response =
                JnvResponse.from(jnvService.getById(jnvId));

        return new ApiResponse<>(
                true,
                response,
                "JNV retrieved successfully"
        );
    }
}
