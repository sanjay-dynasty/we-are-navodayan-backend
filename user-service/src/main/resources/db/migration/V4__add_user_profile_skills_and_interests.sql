CREATE TABLE user_profile_skills (
    id UUID PRIMARY KEY,
    user_profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    skill VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_profile_skills_profile_skill UNIQUE (user_profile_id, skill),
    CONSTRAINT chk_user_profile_skills_nonblank CHECK (LENGTH(TRIM(skill)) > 0)
);

CREATE INDEX idx_user_profile_skills_skill
    ON user_profile_skills(skill);

CREATE INDEX idx_user_profile_skills_profile
    ON user_profile_skills(user_profile_id);

CREATE TABLE user_profile_interests (
    id UUID PRIMARY KEY,
    user_profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    interest VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_profile_interests_profile_interest UNIQUE (user_profile_id, interest),
    CONSTRAINT chk_user_profile_interests_nonblank CHECK (LENGTH(TRIM(interest)) > 0)
);

CREATE INDEX idx_user_profile_interests_interest
    ON user_profile_interests(interest);

CREATE INDEX idx_user_profile_interests_profile
    ON user_profile_interests(user_profile_id);
