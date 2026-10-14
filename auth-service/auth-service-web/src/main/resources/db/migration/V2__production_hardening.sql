-- Normalise emails, add account/token lifecycle columns, persist OAuth state in Postgres
-- (no Redis), and stop storing provider tokens.

ALTER TABLE users ADD COLUMN IF NOT EXISTS email_normalized VARCHAR(255);
UPDATE users SET email_normalized = lower(email) WHERE email_normalized IS NULL;
ALTER TABLE users ALTER COLUMN email_normalized SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS users_email_normalized_uidx ON users (email_normalized);

ALTER TABLE users ADD COLUMN IF NOT EXISTS roles VARCHAR(255) NOT NULL DEFAULT 'ROLE_USER';
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP WITH TIME ZONE;

-- Existing plaintext refresh tokens cannot be verified after this change; users re-login.
ALTER TABLE refresh_token RENAME TO refresh_token_legacy;

CREATE TABLE refresh_token
(
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash           TEXT   NOT NULL UNIQUE,
    family_id            UUID   NOT NULL,
    replaced_by_id       BIGINT REFERENCES refresh_token (id),
    provider             VARCHAR(64),
    external_identity_id BIGINT REFERENCES user_social_identity (id),
    revoked              BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at         TIMESTAMP WITH TIME ZONE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX refresh_token_user_idx ON refresh_token (user_id);
CREATE INDEX refresh_token_family_idx ON refresh_token (family_id);

DROP TABLE refresh_token_legacy;

ALTER TABLE user_social_identity DROP COLUMN IF EXISTS access_token_encrypted;
ALTER TABLE user_social_identity DROP COLUMN IF EXISTS refresh_token_encrypted;
ALTER TABLE user_social_identity DROP COLUMN IF EXISTS raw_claims;
ALTER TABLE user_social_identity DROP COLUMN IF EXISTS scopes;
ALTER TABLE user_social_identity DROP COLUMN IF EXISTS expires_at;

CREATE TABLE oauth_state
(
    state_value          VARCHAR(128) PRIMARY KEY,
    provider             VARCHAR(64)  NOT NULL,
    redirect_uri         TEXT         NOT NULL,
    code_verifier        TEXT         NOT NULL,
    nonce                TEXT         NOT NULL,
    browser_binding_hash TEXT         NOT NULL,
    code_challenge       TEXT,
    expires_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX oauth_state_expires_idx ON oauth_state (expires_at);

CREATE TABLE oauth_login_code
(
    code_hash            TEXT PRIMARY KEY,
    user_id              BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    external_identity_id BIGINT REFERENCES user_social_identity (id),
    code_challenge       TEXT,
    expires_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX oauth_login_code_expires_idx ON oauth_login_code (expires_at);

CREATE TABLE action_token
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose    VARCHAR(32) NOT NULL,
    token_hash TEXT        NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX action_token_user_idx ON action_token (user_id);

CREATE TABLE audit_event
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT,
    event_type VARCHAR(64) NOT NULL,
    detail     TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX audit_event_created_idx ON audit_event (created_at);
