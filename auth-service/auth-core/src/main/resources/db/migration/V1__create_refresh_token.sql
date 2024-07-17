CREATE TABLE refresh_token
(
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(255) NOT NULL,
    refresh_token TEXT         NOT NULL,
    revoked       BOOLEAN      NOT NULL,
    date_created  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
