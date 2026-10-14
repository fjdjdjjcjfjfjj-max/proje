-- =====================================================================
-- V1: Temel şema (MVP)
-- Tablolar: users, posts, comments, post_likes, user_blocks, reports,
--           moderation_actions
-- Notlar:
--  * Birincil anahtarlar UUID'dir; sıralı tamsayı kimlikler tahmin
--    edilebilir olduğu için IDOR / kayıt sayımı riskini artırır.
--  * Tüm zaman damgaları TIMESTAMPTZ (UTC) olarak tutulur.
--  * Kullanıcı içeriği (gönderi, yorum, biyografi, şikâyet açıklaması)
--    güvenilmeyen veridir; uzunluk sınırları burada da zorlanır.
--  * Enum benzeri alanlar VARCHAR + CHECK ile tutulur; yeni değer
--    eklemek bir migrasyonla CHECK kısıtını güncellemeyi gerektirir.
-- =====================================================================

-- updated_at alanını her UPDATE'te otomatik güncelleyen yardımcı fonksiyon
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username                VARCHAR(30)  NOT NULL,
    display_name            VARCHAR(50)  NOT NULL,
    email                   VARCHAR(254) NOT NULL,
    password_hash           VARCHAR(255) NOT NULL,
    bio                     VARCHAR(300),
    role                    VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status                  VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    -- Yaş verisi en aza indirilir: tam doğum tarihi saklanmaz.
    -- age_group: beyan edilen yaş grubu (MINOR = 13-17, ADULT = 18+)
    -- adult_from: yalnızca MINOR için; kullanıcının 18 yaşına girdiği ayı
    --             İZLEYEN ayın ilk günü (gün bilgisi saklanmaz, geçiş asla
    --             erken olmaz). ADULT'a otomatik geçiş için gereklidir.
    age_group               VARCHAR(10)  NOT NULL,
    adult_from              DATE,
    -- Yaş doğrulaması ayrı bir servis tarafından yapılır. Doğrulama
    -- yapılmadıkça değer UNVERIFIED kalır ve "doğrulanmış yaş" iddia edilmez.
    age_verification_status VARCHAR(20)  NOT NULL DEFAULT 'UNVERIFIED',
    profile_visibility      VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE',
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at              TIMESTAMPTZ,
    version                 BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_users_username_format
        CHECK (username ~ '^[a-z0-9_.]{3,30}$'),
    CONSTRAINT chk_users_display_name_not_blank
        CHECK (length(btrim(display_name)) > 0),
    CONSTRAINT chk_users_role
        CHECK (role IN ('USER', 'MODERATOR', 'ADMIN')),
    CONSTRAINT chk_users_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
    CONSTRAINT chk_users_age_group
        CHECK (age_group IN ('MINOR', 'ADULT')),
    CONSTRAINT chk_users_adult_from
        CHECK ((age_group = 'MINOR' AND adult_from IS NOT NULL)
            OR (age_group = 'ADULT' AND adult_from IS NULL)),
    CONSTRAINT chk_users_age_verification_status
        CHECK (age_verification_status IN ('UNVERIFIED', 'PENDING', 'VERIFIED', 'REJECTED')),
    CONSTRAINT chk_users_profile_visibility
        CHECK (profile_visibility IN ('PUBLIC', 'PRIVATE'))
);

-- Kullanıcı adı ve e-posta büyük/küçük harf duyarsız benzersizdir.
-- Uygulama kullanıcı adını küçük harfe çevirerek saklar (CHECK bunu zorlar).
CREATE UNIQUE INDEX ux_users_username ON users (username);
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));
CREATE INDEX ix_users_status ON users (status);
-- Reşit olma tarihi gelen kullanıcıları bulan periyodik iş için
CREATE INDEX ix_users_adult_from ON users (adult_from) WHERE adult_from IS NOT NULL;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- posts
-- ---------------------------------------------------------------------
CREATE TABLE posts (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id     UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    content       VARCHAR(2000) NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PUBLISHED',
    like_count    INTEGER       NOT NULL DEFAULT 0,
    comment_count INTEGER       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ,
    version       BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT chk_posts_content_not_blank CHECK (length(btrim(content)) > 0),
    CONSTRAINT chk_posts_status CHECK (status IN ('PUBLISHED', 'HIDDEN', 'DELETED')),
    CONSTRAINT chk_posts_like_count CHECK (like_count >= 0),
    CONSTRAINT chk_posts_comment_count CHECK (comment_count >= 0)
);

-- Profil sayfası: bir kullanıcının gönderileri, yeniden eskiye
CREATE INDEX ix_posts_author_created ON posts (author_id, created_at DESC, id DESC);
-- Ana akış: yayındaki gönderiler, yeniden eskiye (keyset sayfalama)
CREATE INDEX ix_posts_published_created ON posts (created_at DESC, id DESC)
    WHERE status = 'PUBLISHED';

CREATE TRIGGER trg_posts_updated_at
    BEFORE UPDATE ON posts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- comments
-- ---------------------------------------------------------------------
CREATE TABLE comments (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     UUID          NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    author_id   UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    content     VARCHAR(1000) NOT NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'PUBLISHED',
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    version     BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT chk_comments_content_not_blank CHECK (length(btrim(content)) > 0),
    CONSTRAINT chk_comments_status CHECK (status IN ('PUBLISHED', 'HIDDEN', 'DELETED'))
);

CREATE INDEX ix_comments_post_created ON comments (post_id, created_at, id);
CREATE INDEX ix_comments_author ON comments (author_id);

CREATE TRIGGER trg_comments_updated_at
    BEFORE UPDATE ON comments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- post_likes  ("like" SQL'de anahtar kelime olduğundan tablo adı post_likes)
-- ---------------------------------------------------------------------
CREATE TABLE post_likes (
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    post_id    UUID        NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Bir kullanıcı bir gönderiyi yalnızca bir kez beğenebilir
    CONSTRAINT pk_post_likes PRIMARY KEY (user_id, post_id)
);

CREATE INDEX ix_post_likes_post ON post_likes (post_id);

-- ---------------------------------------------------------------------
-- user_blocks
-- ---------------------------------------------------------------------
CREATE TABLE user_blocks (
    blocker_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    blocked_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_user_blocks PRIMARY KEY (blocker_id, blocked_id),
    CONSTRAINT chk_user_blocks_not_self CHECK (blocker_id <> blocked_id)
);

-- "Beni kim engelledi?" sorgusu (akış filtreleme) için ters indeks
CREATE INDEX ix_user_blocks_blocked ON user_blocks (blocked_id);

-- ---------------------------------------------------------------------
-- reports (şikâyetler)
-- Hedef tam olarak bir tanedir: gönderi, yorum veya kullanıcı.
-- Ayrı nullable FK'lar polimorfik "target_id" yerine referans
-- bütünlüğünü veritabanı seviyesinde korur.
-- ---------------------------------------------------------------------
CREATE TABLE reports (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id         UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    target_type         VARCHAR(20)   NOT NULL,
    reported_post_id    UUID          REFERENCES posts (id) ON DELETE SET NULL,
    reported_comment_id UUID          REFERENCES comments (id) ON DELETE SET NULL,
    reported_user_id    UUID          REFERENCES users (id) ON DELETE SET NULL,
    reason              VARCHAR(30)   NOT NULL,
    details             VARCHAR(1000),
    status              VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    handled_by_id       UUID          REFERENCES users (id) ON DELETE SET NULL,
    resolved_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version             BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT chk_reports_target_type
        CHECK (target_type IN ('POST', 'COMMENT', 'USER')),
    -- Hedef silinmişse (SET NULL) en fazla bir hedef kalır; oluşturma
    -- anında tam olarak bir hedef olmasını uygulama + aşağıdaki kısıt sağlar.
    CONSTRAINT chk_reports_single_target
        CHECK (num_nonnulls(reported_post_id, reported_comment_id, reported_user_id) <= 1),
    CONSTRAINT chk_reports_target_matches_type
        CHECK ((target_type = 'POST'    AND reported_comment_id IS NULL AND reported_user_id IS NULL)
            OR (target_type = 'COMMENT' AND reported_post_id IS NULL    AND reported_user_id IS NULL)
            OR (target_type = 'USER'    AND reported_post_id IS NULL    AND reported_comment_id IS NULL)),
    CONSTRAINT chk_reports_reason
        CHECK (reason IN ('SPAM', 'HARASSMENT', 'HATE_SPEECH', 'VIOLENCE', 'SEXUAL_CONTENT',
                          'CHILD_SAFETY', 'SELF_HARM', 'MISINFORMATION', 'IMPERSONATION', 'OTHER')),
    CONSTRAINT chk_reports_status
        CHECK (status IN ('OPEN', 'IN_REVIEW', 'RESOLVED', 'DISMISSED'))
);

-- Yeni şikâyet oluşturulurken hedefin dolu olması zorunludur
CREATE OR REPLACE FUNCTION reports_require_target_on_insert() RETURNS trigger AS $$
BEGIN
    IF num_nonnulls(NEW.reported_post_id, NEW.reported_comment_id, NEW.reported_user_id) <> 1 THEN
        RAISE EXCEPTION 'report must reference exactly one target' USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reports_require_target
    BEFORE INSERT ON reports
    FOR EACH ROW EXECUTE FUNCTION reports_require_target_on_insert();

CREATE TRIGGER trg_reports_updated_at
    BEFORE UPDATE ON reports
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Moderasyon kuyruğu: açık şikâyetler, eskiden yeniye
CREATE INDEX ix_reports_status_created ON reports (status, created_at);
CREATE INDEX ix_reports_reporter ON reports (reporter_id);
CREATE INDEX ix_reports_post ON reports (reported_post_id) WHERE reported_post_id IS NOT NULL;
CREATE INDEX ix_reports_comment ON reports (reported_comment_id) WHERE reported_comment_id IS NOT NULL;
CREATE INDEX ix_reports_user ON reports (reported_user_id) WHERE reported_user_id IS NOT NULL;

-- Aynı kullanıcının aynı hedefi açıkken tekrar tekrar şikâyet etmesini
-- (spam) engeller.
CREATE UNIQUE INDEX ux_reports_open_post ON reports (reporter_id, reported_post_id)
    WHERE status IN ('OPEN', 'IN_REVIEW') AND reported_post_id IS NOT NULL;
CREATE UNIQUE INDEX ux_reports_open_comment ON reports (reporter_id, reported_comment_id)
    WHERE status IN ('OPEN', 'IN_REVIEW') AND reported_comment_id IS NOT NULL;
CREATE UNIQUE INDEX ux_reports_open_user ON reports (reporter_id, reported_user_id)
    WHERE status IN ('OPEN', 'IN_REVIEW') AND reported_user_id IS NOT NULL;

-- ---------------------------------------------------------------------
-- moderation_actions (yönetici/moderatör denetim kaydı)
-- Yalnızca ekleme yapılabilir; UPDATE ve DELETE tetikleyiciyle engellenir.
-- moderator_id RESTRICT: işlem yapmış bir moderatör hesabı, denetim
-- kaydı korunmadan fiziksel olarak silinemez.
-- ---------------------------------------------------------------------
CREATE TABLE moderation_actions (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    moderator_id      UUID          NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    action_type       VARCHAR(30)   NOT NULL,
    target_type       VARCHAR(20)   NOT NULL,
    -- Hedef kimliği bilinçli olarak FK değildir: içerik daha sonra
    -- fiziksel olarak silinse bile denetim kaydı bozulmadan kalmalıdır.
    target_id         UUID          NOT NULL,
    report_id         UUID          REFERENCES reports (id) ON DELETE SET NULL,
    reason            VARCHAR(1000) NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT chk_moderation_actions_type
        CHECK (action_type IN ('HIDE_POST', 'RESTORE_POST', 'HIDE_COMMENT', 'RESTORE_COMMENT',
                               'SUSPEND_USER', 'UNSUSPEND_USER', 'DISMISS_REPORT', 'RESOLVE_REPORT')),
    CONSTRAINT chk_moderation_actions_target_type
        CHECK (target_type IN ('POST', 'COMMENT', 'USER', 'REPORT')),
    CONSTRAINT chk_moderation_actions_reason_not_blank
        CHECK (length(btrim(reason)) > 0)
);

CREATE INDEX ix_moderation_actions_target ON moderation_actions (target_type, target_id, created_at);
CREATE INDEX ix_moderation_actions_moderator ON moderation_actions (moderator_id, created_at);
CREATE INDEX ix_moderation_actions_report ON moderation_actions (report_id) WHERE report_id IS NOT NULL;

CREATE OR REPLACE FUNCTION moderation_actions_append_only() RETURNS trigger AS $$
BEGIN
    -- ON DELETE SET NULL ile report_id'nin temizlenmesine izin ver;
    -- bunun dışındaki her değişiklik reddedilir.
    IF TG_OP = 'UPDATE'
       AND NEW.report_id IS NULL AND OLD.report_id IS NOT NULL
       AND (NEW.id, NEW.moderator_id, NEW.action_type, NEW.target_type, NEW.target_id, NEW.reason, NEW.created_at)
           IS NOT DISTINCT FROM
           (OLD.id, OLD.moderator_id, OLD.action_type, OLD.target_type, OLD.target_id, OLD.reason, OLD.created_at)
    THEN
        RETURN NEW;
    END IF;
    RAISE EXCEPTION 'moderation_actions is append-only' USING ERRCODE = 'insufficient_privilege';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_moderation_actions_append_only
    BEFORE UPDATE OR DELETE ON moderation_actions
    FOR EACH ROW EXECUTE FUNCTION moderation_actions_append_only();
