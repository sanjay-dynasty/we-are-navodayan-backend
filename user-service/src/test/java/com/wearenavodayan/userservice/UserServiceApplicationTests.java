
package com.wearenavodayan.userservice;

import com.wearenavodayan.userservice.entity.UserProfile;
import com.wearenavodayan.userservice.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class UserServiceApplicationTests {

	@Autowired
	private UserProfileRepository userProfileRepository;

	@Test
	void contextLoads() {
		assertThat(userProfileRepository).isNotNull();
	}

	@Test
	void shouldSaveAndRetrieveUserProfile() {
		UUID authUserId = UUID.randomUUID();

		UserProfile savedProfile = userProfileRepository.saveAndFlush(
				createProfile(authUserId));

		assertThat(savedProfile.getId()).isNotNull();
		assertThat(savedProfile.getCreatedAt()).isNotNull();
		assertThat(savedProfile.getUpdatedAt()).isNotNull();

		UserProfile retrievedProfile = userProfileRepository.findById(savedProfile.getId())
				.orElseThrow();

		assertThat(retrievedProfile.getAuthUserId())
				.isEqualTo(authUserId);
		assertThat(retrievedProfile.getFullName())
				.isEqualTo("Test Navodayan");
		assertThat(retrievedProfile.getBatchYear())
				.isEqualTo(2018);
	}

	@Test
	void shouldFindProfileByAuthUserId() {
		UUID authUserId = UUID.randomUUID();

		userProfileRepository.saveAndFlush(
				createProfile(authUserId));

		assertThat(userProfileRepository.findByAuthUserId(authUserId))
				.isPresent()
				.get()
				.extracting(UserProfile::getFullName)
				.isEqualTo("Test Navodayan");
	}

	@Test
	void shouldCheckWhetherAuthUserIdExists() {
		UUID authUserId = UUID.randomUUID();

		userProfileRepository.saveAndFlush(
				createProfile(authUserId));

		assertThat(userProfileRepository.existsByAuthUserId(authUserId))
				.isTrue();

		assertThat(userProfileRepository.existsByAuthUserId(
				UUID.randomUUID()))
				.isFalse();
	}

	@Test
	void shouldFindProfilesByJnvId() {
		UUID jnvId = UUID.randomUUID();
		UserProfile profile = createProfile(UUID.randomUUID());
		profile.setJnvId(jnvId);

		userProfileRepository.saveAndFlush(profile);

		assertThat(userProfileRepository.findByJnvId(jnvId))
				.hasSize(1)
				.allSatisfy(result -> assertThat(result.getJnvId()).isEqualTo(jnvId));
	}

	@Test
	void shouldRejectDuplicateAuthUserId() {
		UUID authUserId = UUID.randomUUID();

		userProfileRepository.saveAndFlush(
				createProfile(authUserId));

		assertThatThrownBy(() -> userProfileRepository.saveAndFlush(
				createProfile(authUserId)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private UserProfile createProfile(UUID authUserId) {
		UserProfile profile = new UserProfile();

		profile.setAuthUserId(authUserId);
		profile.setFullName("Test Navodayan");
		profile.setBio("Integration test profile");
		profile.setBatchYear(2018);
		profile.setProfileImageUrl(
				"https://example.com/test-profile.jpg");

		return profile;
	}
}
