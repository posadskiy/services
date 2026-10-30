-- Answering a refresh token that was rotated a moment ago (two tabs, an app resuming) adds a second live token
-- to the family, so the one-live-token-per-family guard from V7 no longer holds. Reuse after the grace window
-- still revokes the whole family. The plain family index from V2 keeps lookups fast.

DROP INDEX IF EXISTS refresh_token_one_live_family_idx;
