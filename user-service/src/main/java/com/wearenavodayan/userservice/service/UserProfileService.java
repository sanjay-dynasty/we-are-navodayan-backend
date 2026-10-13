package com.wearenavodayan.userservice.service;

import com.wearenavodayan.userservice.entity.UserProfile;
import com.wearenavodayan.userservice.repository.UserProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public UserProfile create(UserProfile userProfile) {
        return userProfileRepository.save(userProfile);
    }

    public UserProfile getById(UUID id) {
        return userProfileRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "User profile not found: " + id));
    }

    public UserProfile getByAuthUserId(UUID authUserId) {
        return userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "User profile not found for auth user: "
                                        + authUserId));
    }

    public List<UserProfile> getByJnvId(UUID jnvId) {
        return userProfileRepository.findByJnvId(jnvId);
    }

    public List<UserProfile> getAll() {
        return userProfileRepository.findAll();
    }

    @Transactional
    public UserProfile update(UUID id, UserProfile updatedProfile) {
        UserProfile existingProfile = getById(id);

        existingProfile.setAuthUserId(updatedProfile.getAuthUserId());
        existingProfile.setFullName(updatedProfile.getFullName());
        existingProfile.setBio(updatedProfile.getBio());
        existingProfile.setBatchYear(updatedProfile.getBatchYear());
        existingProfile.setJnvId(updatedProfile.getJnvId());
        existingProfile.setProfileImageUrl(
                updatedProfile.getProfileImageUrl());

        return userProfileRepository.save(existingProfile);
    }

    @Transactional
    public void delete(UUID id) {
        UserProfile userProfile = getById(id);
        userProfileRepository.delete(userProfile);
    }

    public boolean existsByAuthUserId(UUID authUserId) {
        return userProfileRepository.existsByAuthUserId(authUserId);
    }
}