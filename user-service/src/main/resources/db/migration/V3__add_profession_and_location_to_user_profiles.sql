ALTER TABLE user_profiles
    ADD COLUMN profession VARCHAR(100),
    ADD COLUMN organization VARCHAR(150),
    ADD COLUMN job_title VARCHAR(150),
    ADD COLUMN current_city VARCHAR(100),
    ADD COLUMN current_state VARCHAR(100),
    ADD COLUMN country VARCHAR(100);