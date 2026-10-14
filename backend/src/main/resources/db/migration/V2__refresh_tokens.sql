-- =====================================================================
-- V2: Yenileme belirteçleri (refresh token)
--  * Belirtecin kendisi asla saklanmaz; yalnızca SHA-256 özeti tutulur.
--    Veritabanı sızsa bile belirteçler kullanılamaz.
--  * family_id: aynı girişten türeyen belirteç zinciri. Kullanılmış
--    (rotasyona uğramış) bir belirteç tekrar gelirse çalınma şüphesiyle
--    tüm aile iptal edilir.
-- =====================================================================
CREATE TABLE refresh_tokens (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    family_id      UUID        NOT NULL,
    token_hash     VARCHAR(64) NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at     TIMESTAMPTZ,
    replaced_by_id UUID        REFERENCES refresh_tokens (id) ON DELETE SET NULL,

    CONSTRAINT chk_refresh_tokens_expiry CHECK (expires_at > created_at)
);

CREATE UNIQUE INDEX ux_refresh_tokens_hash ON refresh_tokens (token_hash);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);
-- Süresi dolmuş kayıtları temizleyen periyodik iş için
CREATE INDEX ix_refresh_tokens_expires ON refresh_tokens (expires_at);
