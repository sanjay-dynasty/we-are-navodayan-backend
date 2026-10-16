package com.wearenavodayan.userservice.repository;

import com.wearenavodayan.userservice.entity.UserProfileSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserProfileSkillRepository extends JpaRepository<UserProfileSkill, UUID> {

    List<UserProfileSkill> findByUserProfileIdOrderBySkillAsc(UUID userProfileId);

    void deleteByUserProfileId(UUID userProfileId);

    boolean existsByUserProfileIdAndSkillIgnoreCase(UUID userProfileId, String skill);
}
