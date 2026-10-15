package com.wearenavodayan.userservice.service;

import com.wearenavodayan.userservice.entity.UserProfile;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileCompletenessCalculatorTest {

    private final ProfileCompletenessCalculator calculator =
            new ProfileCompletenessCalculator();

    @Test
    void shouldCalculateTwentyPercentForNameOnlyProfile() {
        UserProfile profile = new UserProfile();
        profile.setFullName("Test Navodayan");

        assertThat(calculator.calculate(profile)).isEqualTo(20);
    }

    @Test
    void shouldCalculateSixtyPercentForThreeCompletedFields() {
        UserProfile profile = new UserProfile();
        profile.setFullName("Test Navodayan");
        profile.setBio("Alumni");
        profile.setBatchYear(2018);

        assertThat(calculator.calculate(profile)).isEqualTo(60);
    }

    @Test
    void shouldCalculateOneHundredPercentForCompleteProfile() {
        UserProfile profile = new UserProfile();
        profile.setFullName("Test Navodayan");
        profile.setBio("Alumni");
        profile.setBatchYear(2018);
        profile.setJnvId(UUID.randomUUID());
        profile.setProfileImageUrl("https://example.com/profile.jpg");

        assertThat(calculator.calculate(profile)).isEqualTo(100);
    }

    @Test
    void shouldIgnoreBlankOptionalText() {
        UserProfile profile = new UserProfile();
        profile.setFullName("Test Navodayan");
        profile.setBio("   ");
        profile.setProfileImageUrl(" ");

        assertThat(calculator.calculate(profile)).isEqualTo(20);
    }

    @Test
    void shouldRejectNullProfile() {
        assertThatThrownBy(() -> calculator.calculate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Profile is required");
    }
}