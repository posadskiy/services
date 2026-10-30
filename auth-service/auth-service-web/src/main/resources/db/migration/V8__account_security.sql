ALTER TABLE refresh_token ADD COLUMN IF NOT EXISTS user_agent VARCHAR(512);
ALTER TABLE refresh_token ADD COLUMN IF NOT EXISTS client_ip VARCHAR(64);

ALTER TABLE action_token ADD COLUMN IF NOT EXISTS payload TEXT;

ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name VARCHAR(120);
ALTER TABLE users ADD COLUMN IF NOT EXISTS locale VARCHAR(35);
ALTER TABLE users ADD COLUMN IF NOT EXISTS time_zone VARCHAR(64);
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url TEXT;

CREATE TABLE IF NOT EXISTS passkey_credential
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    credential_id   TEXT         NOT NULL UNIQUE,
    public_key_cose TEXT         NOT NULL,
    signature_count BIGINT       NOT NULL DEFAULT 0,
    user_handle     TEXT         NOT NULL,
    label           VARCHAR(120),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS passkey_credential_user_idx ON passkey_credential (user_id);

CREATE TABLE IF NOT EXISTS webauthn_challenge
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT,
    purpose      VARCHAR(32) NOT NULL,
    request_json TEXT        NOT NULL,
    expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS webauthn_challenge_expires_idx ON webauthn_challenge (expires_at);

CREATE TABLE IF NOT EXISTS email_suppression
(
    email_normalized VARCHAR(255) PRIMARY KEY,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_preference
(
    user_id          BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product          VARCHAR(32) NOT NULL,
    marketing_email  BOOLEAN     NOT NULL DEFAULT FALSE,
    settings         TEXT        NOT NULL DEFAULT '{}',
    PRIMARY KEY (user_id, product)
);
