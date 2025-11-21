-- Change raw_claims column from JSONB to TEXT
ALTER TABLE user_social_identity 
ALTER COLUMN raw_claims TYPE TEXT USING raw_claims::text;


