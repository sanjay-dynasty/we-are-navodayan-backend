ALTER TABLE user_profiles
    ADD COLUMN profile_image_storage_key VARCHAR(500),
    ADD COLUMN profile_image_content_type VARCHAR(100),
    ADD COLUMN profile_image_size_bytes BIGINT,
    ADD COLUMN profile_image_original_filename VARCHAR(255),
    ADD COLUMN profile_image_updated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE user_profiles
    ADD CONSTRAINT chk_user_profiles_profile_image_size
    CHECK (profile_image_size_bytes IS NULL OR profile_image_size_bytes > 0);
