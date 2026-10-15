package com.wearenavodayan.userservice;

import com.wearenavodayan.userservice.client.JnvServiceClient;
import com.wearenavodayan.userservice.entity.UserProfile;
import com.wearenavodayan.userservice.repository.UserProfileRepository;
import com.wearenavodayan.userservice.service.UserProfileService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("local")
class UserProfileTransactionTests {

        @Autowired
        private UserProfileService userProfileService;

        @Autowired
        private UserProfileRepository userProfileRepository;

        @MockitoBean
        private JnvServiceClient jnvServiceClient;

        private final List<UUID> createdAuthUserIds = new ArrayList<>();

        @AfterEach
        void cleanUp() {
                createdAuthUserIds.forEach(authUserId -> userProfileRepository.findByAuthUserId(authUserId)
                                .ifPresent(userProfileRepository::delete));
        }

        @Test
        void shouldCreateAndRetrieveProfileThroughService() {
                mockValidJnv();

                UUID authUserId = UUID.randomUUID();
                UserProfile saved = userProfileService.create(createProfile(authUserId));

                assertThat(saved.getId()).isNotNull();
                assertThat(userProfileService.getById(saved.getId()).getFullName())
                                .isEqualTo("Test Navodayan");
                assertThat(userProfileService.getByAuthUserId(authUserId).getId())
                                .isEqualTo(saved.getId());
                assertThat(userProfileService.existsByAuthUserId(authUserId))
                                .isTrue();
        }

        @Test
        void shouldUpdateAndDeleteProfileThroughService() {
                mockValidJnv();

                UUID authUserId = UUID.randomUUID();
                UserProfile saved = userProfileService.create(createProfile(authUserId));

                UserProfile changes = createProfile(authUserId);
                changes.setFullName("Updated Navodayan");

                UserProfile updated = userProfileService.update(saved.getId(), changes);

                assertThat(updated.getFullName()).isEqualTo("Updated Navodayan");

                userProfileService.delete(saved.getId());

                assertThatThrownBy(
                                () -> userProfileService.getById(saved.getId()))
                                .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        void shouldDeclareReadOnlyTransactionsForReadMethods()
                        throws NoSuchMethodException {
                AnnotationTransactionAttributeSource source = new AnnotationTransactionAttributeSource();

                Method readMethod = UserProfileService.class.getMethod("getById", UUID.class);
                TransactionAttribute readAttribute = source.getTransactionAttribute(
                                readMethod, UserProfileService.class);

                Method writeMethod = UserProfileService.class.getMethod(
                                "create", UserProfile.class);
                TransactionAttribute writeAttribute = source.getTransactionAttribute(
                                writeMethod, UserProfileService.class);

                assertThat(readAttribute).isNotNull();
                assertThat(readAttribute.isReadOnly()).isTrue();

                assertThat(writeAttribute).isNotNull();
                assertThat(writeAttribute.isReadOnly()).isFalse();
        }

        private void mockValidJnv() {
                when(jnvServiceClient.getJnvById(anyString()))
                                .thenReturn(new com.wearenavodayan.userservice.dto.response.ApiResponse<>(
                                                true, null, "JNV found"));
        }

        private UserProfile createProfile(UUID authUserId) {
                createdAuthUserIds.add(authUserId);

                UserProfile profile = new UserProfile();
                profile.setAuthUserId(authUserId);
                profile.setFullName("Test Navodayan");
                profile.setBio("Transaction integration test");
                profile.setBatchYear(2018);
                profile.setJnvId(UUID.randomUUID());
                profile.setProfileImageUrl(
                                "https://example.com/test-profile.jpg");

                return profile;
        }
}