-- Mail that has not been accepted by email-service yet. A row exists only until the send succeeds
-- (it holds the recipient and a link with a raw token), and is dropped after a day either way.

CREATE TABLE mail_outbox
(
    id              BIGSERIAL PRIMARY KEY,
    from_address    VARCHAR(320)  NOT NULL,
    to_email        VARCHAR(320)  NOT NULL,
    template        VARCHAR(64)   NOT NULL,
    variables       TEXT          NOT NULL,
    idempotency_key VARCHAR(256)  NOT NULL UNIQUE,
    category        VARCHAR(64)   NOT NULL,
    attempts        INTEGER       NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_error      VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX mail_outbox_due_idx ON mail_outbox (next_attempt_at);
CREATE INDEX mail_outbox_created_idx ON mail_outbox (created_at);

-- Cleanup of old rows filters on these.
CREATE INDEX IF NOT EXISTS refresh_token_expires_idx ON refresh_token (expires_at);
CREATE INDEX IF NOT EXISTS action_token_expires_idx ON action_token (expires_at);
-- Device history lookups at login.
CREATE INDEX IF NOT EXISTS refresh_token_user_agent_idx ON refresh_token (user_id, user_agent);
