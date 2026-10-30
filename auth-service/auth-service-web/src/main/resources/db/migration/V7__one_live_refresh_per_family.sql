-- A family may have only one unrevoked refresh token. Older duplicates, if a race left any, are revoked.

UPDATE refresh_token AS older
SET revoked = TRUE
WHERE revoked = FALSE
  AND EXISTS (SELECT 1
              FROM refresh_token AS newer
              WHERE newer.family_id = older.family_id
                AND newer.revoked = FALSE
                AND (newer.created_at, newer.id) > (older.created_at, older.id));

CREATE UNIQUE INDEX refresh_token_one_live_family_idx
    ON refresh_token (family_id)
    WHERE revoked = FALSE;
