# Veritabanı Modeli

PostgreSQL 16. Şema yalnızca Flyway migrasyonlarıyla değişir:
`backend/src/main/resources/db/migration/V1__initial_schema.sql`.

Uygulanmış bir migrasyon dosyası **asla değiştirilmez**; her değişiklik yeni bir `V{n}__aciklama.sql`
dosyasıdır (`validate-on-migrate=true` değişikliği yakalar).

## Tablolar

```
users ─┬─< posts ─┬─< comments
       │          ├─< post_likes >─ users
       │          └─< reports (reported_post_id)
       ├─< user_blocks (blocker_id, blocked_id)
       ├─< reports (reporter_id, reported_user_id, handled_by_id)
       └─< moderation_actions (moderator_id)    reports ─< moderation_actions (report_id)
```

| Tablo                | Birincil anahtar          | Önemli kısıtlar / indeksler |
|----------------------|---------------------------|-----------------------------|
| `users`              | `id UUID`                 | `username` benzersiz, `^[a-z0-9_.]{3,30}$` (küçük harf); `lower(email)` benzersiz; `MINOR` ⇔ `adult_from` dolu |
| `posts`              | `id UUID`                 | İçerik 1–2000 karakter, boş olamaz; `(author_id, created_at DESC)`; yayındakiler için kısmi indeks `(created_at DESC, id DESC)` |
| `comments`           | `id UUID`                 | İçerik 1–1000 karakter; `(post_id, created_at)` |
| `post_likes`         | `(user_id, post_id)`      | Bir kullanıcı bir gönderiyi bir kez beğenir; `post_id` indeksi |
| `user_blocks`        | `(blocker_id, blocked_id)`| Kendini engelleme yasak; ters yön indeksi `blocked_id` |
| `reports`            | `id UUID`                 | Tam olarak bir hedef (gönderi/yorum/kullanıcı), tür ile uyumlu; aynı kişi aynı hedefi açıkken tekrar şikâyet edemez |
| `moderation_actions` | `id UUID`                 | Yalnızca ekleme (UPDATE/DELETE tetikleyiciyle engelli); moderatör hesabı `RESTRICT` |

Ortak alanlar: `created_at`, `updated_at` (tetikleyiciyle otomatik), gerektiğinde `deleted_at`
(yumuşak silme) ve `version` (iyimser kilitleme).

Enum alanları `VARCHAR + CHECK` ile tutulur. Yeni değer eklemek bir migrasyonla CHECK kısıtını
güncellemeyi gerektirir; bu bilinçli bir tercihtir (PostgreSQL `ENUM` tipinden değer çıkarmak zordur).

### Yaş verisi

Tam doğum tarihi **saklanmaz**. Kayıt sırasında doğum tarihi yalnızca yaş grubunu hesaplamak için
kullanılır ve atılır (Aşama 2). Saklananlar:

- `age_group`: `MINOR` (13–17) veya `ADULT` (18+) — kullanıcı beyanıdır.
- `adult_from`: yalnızca `MINOR` için, 18 yaşına girilen ayı **izleyen ayın ilk günü**. Gün bilgisi
  tutulmaz; yetişkin grubuna geçiş asla erken olmaz, en fazla bir ay gecikir (güvenli tarafta kalan
  yuvarlama).
- `age_verification_status`: `UNVERIFIED` varsayılandır. Bağımsız doğrulama servisi sonuç vermeden
  `VERIFIED` yapılmaz; uygulama "doğrulanmış yaş" iddiasını yalnızca bu alana dayandırır.

## Kullanıcı içeriği

Gönderi, yorum, biyografi, görünen ad ve şikâyet açıklaması **güvenilmeyen veridir**:

- Uzunluk sınırları hem API doğrulamasında hem veritabanı kısıtlarında zorlanır.
- Tüm sorgular parametrelidir (JPA / `JdbcTemplate` yer tutucuları); SQL birleştirme yapılmaz.
- Metin olduğu gibi saklanır; HTML olarak yorumlanmaz. Web istemcisi çıktıyı kaçışlamakla
  (escape) yükümlüdür; Android Compose `Text` HTML yorumlamaz.

## Silme ve saklama kuralları

Aşağıdaki süreler **taslaktır** ve hukuk danışmanıyla kesinleştirilmelidir (bkz. compliance-kvkk.md).

| Olay                         | Davranış |
|------------------------------|----------|
| Kullanıcı gönderisini siler  | `status='DELETED'`, `deleted_at` dolar; hemen görünmez olur. 30 gün sonra fiziksel silme işi (planlı). |
| Moderatör içeriği gizler     | `status='HIDDEN'`; `moderation_actions` kaydı oluşur. İçerik itiraz/inceleme süresince saklanır. |
| Kullanıcı hesabını siler     | `status='DELETED'`, oturumlar iptal, profil görünmez. Bekleme süresinden (taslak: 30 gün) sonra fiziksel silme: `users` satırı silinir; `posts`, `comments`, `post_likes`, `user_blocks`, kullanıcının oluşturduğu `reports` **CASCADE** ile silinir. |
| Kullanıcı hakkında şikâyet   | Hedef silinirse `reported_*_id` `SET NULL` olur; şikâyet kaydı istatistik/denetim için kalır. |
| Moderatör hesabı             | Denetim kaydı varken fiziksel silinemez (`RESTRICT`). Önce anonimleştirme politikası uygulanmalıdır. |
| Moderasyon denetim kaydı     | Değiştirilemez. Saklama süresi yasal yükümlülüklere göre belirlenecek. |
| Yasal saklama yükümlülüğü    | 5651 sayılı Kanun vb. kapsamındaki trafik/erişim kayıtları uygulama veritabanında değil, ayrı ve erişimi sınırlı log deposunda tutulacak (henüz kurulmadı). |

Fiziksel silme ve yaş grubu geçişi (`adult_from <= bugün` olan `MINOR` → `ADULT`) için planlı işler
Aşama 3–5'te eklenecek.
