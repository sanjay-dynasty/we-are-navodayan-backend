package com.wearenavodayan.authservice.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @GetMapping("/access")
    public Map<String, String> verifyAdminAccess() {
        return Map.of("message", "Admin access granted");
    }
}