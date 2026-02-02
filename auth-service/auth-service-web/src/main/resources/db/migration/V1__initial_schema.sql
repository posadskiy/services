-- Create users table with all columns (includes deactivation fields from former V3)
CREATE TABLE users
(
    id             BIGSERIAL PRIMARY KEY,
    username       VARCHAR(255) NOT NULL UNIQUE,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  TEXT,
    email_verified BOOLEAN DEFAULT FALSE,
    picture_url    TEXT,
    last_login_at  TIMESTAMP WITH TIME ZONE,
    version        BIGINT DEFAULT 0 NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    deactivated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_users_active ON users (active) WHERE active = TRUE;

-- Create user_social_identity table (raw_claims as TEXT per former V2)
CREATE TABLE user_social_identity
(
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT                      NOT NULL REFERENCES users (id),
    provider               VARCHAR(64)                 NOT NULL,
    provider_user_id       VARCHAR(255)                NOT NULL,
    email                  VARCHAR(255),
    display_name           VARCHAR(255),
    picture_url            TEXT,
    access_token_encrypted TEXT,
    refresh_token_encrypted TEXT,
    expires_at             TIMESTAMP WITH TIME ZONE,
    scopes                 TEXT,
    raw_claims             TEXT,
    last_login_at          TIMESTAMP WITH TIME ZONE,
    created_at             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    revoked                BOOLEAN DEFAULT FALSE NOT NULL,
    CONSTRAINT user_identity_provider_subject UNIQUE (provider, provider_user_id)
);

CREATE INDEX user_social_identity_user_idx ON user_social_identity (user_id);

-- Create refresh_token table with all columns
CREATE TABLE refresh_token
(
    id                     BIGSERIAL PRIMARY KEY,
    username                VARCHAR(255) NOT NULL,
    refresh_token           TEXT         NOT NULL,
    revoked                 BOOLEAN      NOT NULL,
    external_identity_id    BIGINT REFERENCES user_social_identity (id),
    provider                VARCHAR(64),
    date_created            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX refresh_token_external_identity_idx ON refresh_token (external_identity_id);
