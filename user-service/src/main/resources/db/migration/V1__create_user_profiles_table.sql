CREATE TABLE user_profiles (
    id UUID PRIMARY KEY,
    auth_user_id UUID NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    bio VARCHAR(1000),
    batch_year INTEGER,
    jnv_id UUID,
    profile_image_url VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
