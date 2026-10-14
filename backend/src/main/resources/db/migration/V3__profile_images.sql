-- =====================================================================
-- V3: Profil fotoğrafı ve kapak görseli
-- Dosyalar veritabanında değil, medya klasöründe tutulur; burada yalnızca
-- tahmin edilemeyen dosya anahtarı (<uuid>.jpg) saklanır.
-- =====================================================================
ALTER TABLE users ADD COLUMN avatar_key VARCHAR(64);
ALTER TABLE users ADD COLUMN banner_key VARCHAR(64);

ALTER TABLE users ADD CONSTRAINT chk_users_avatar_key
    CHECK (avatar_key IS NULL OR avatar_key ~ '^[0-9a-f-]{36}\.jpg$');
ALTER TABLE users ADD CONSTRAINT chk_users_banner_key
    CHECK (banner_key IS NULL OR banner_key ~ '^[0-9a-f-]{36}\.jpg$');
