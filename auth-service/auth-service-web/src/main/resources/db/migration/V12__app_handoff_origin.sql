-- Site that minted a native-app handoff code; the new-device email links back to it.
ALTER TABLE oauth_login_code
    ADD COLUMN link_origin VARCHAR(255);
