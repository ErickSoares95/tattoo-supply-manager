-- U11: emails are compared case-insensitively. The app now lowercases them on the way in;
-- existing rows are normalized here and the CHECK keeps any other writer honest, so the
-- existing UNIQUE(email) is enough to stop "Ana@x.com" and "ana@x.com" being two accounts.
-- Fails on purpose if two rows differ only by case: merge them by hand first.
UPDATE users SET email = lower(trim(email)) WHERE email <> lower(trim(email));
ALTER TABLE users ADD CONSTRAINT users_email_lowercase CHECK (email = lower(email));

-- U8: tokens issued before the last password change are refused (JwtService.isTokenValid).
ALTER TABLE users ADD COLUMN password_changed_at timestamp(6) with time zone;
