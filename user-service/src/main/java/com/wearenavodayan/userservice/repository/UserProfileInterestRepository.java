package com.wearenavodayan.userservice.repository;

import com.wearenavodayan.userservice.entity.UserProfileInterest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserProfileInterestRepository extends JpaRepository<UserProfileInterest, UUID> {

    List<UserProfileInterest> findByUserProfileIdOrderByInterestAsc(UUID userProfileId);

    void deleteByUserProfileId(UUID userProfileId);

    boolean existsByUserProfileIdAndInterestIgnoreCase(UUID userProfileId, String interest);
}
