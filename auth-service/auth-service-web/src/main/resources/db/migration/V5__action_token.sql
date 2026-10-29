CREATE TABLE action_token
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT                   NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose     VARCHAR(32)              NOT NULL,
    token_hash  TEXT                     NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX action_token_user_purpose_idx ON action_token (user_id, purpose);
