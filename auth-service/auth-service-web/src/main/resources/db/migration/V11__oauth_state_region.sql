ALTER TABLE oauth_state
    ADD COLUMN locale VARCHAR(35),
    ADD COLUMN time_zone VARCHAR(64);
