# API Belgesi

- Taban yol: `/api/v1`
- Biçim: JSON (UTF-8). Zaman damgaları ISO-8601 UTC (`2026-10-14T15:48:41Z`).
- Canlı ve her zaman güncel belge (yalnızca dev profili): `http://localhost:8080/swagger-ui/index.html`

## Mevcut uç noktalar (Aşama 1)

### `GET /api/v1/system/status`

Kimlik doğrulaması gerektirmez. İstemcilerin sunucuya ulaşabildiğini ve API sürümünü doğrular.

```bash
curl -s http://localhost:8080/api/v1/system/status
```

```json
{ "status": "UP", "apiVersion": "v1", "serverTime": "2026-10-14T15:48:41.407Z" }
```

### `GET /actuator/health`

Altyapı sağlık kontrolü (veritabanı dahil). Ayrıntı içermez. `liveness` ve `readiness` grupları
konteyner orkestrasyonu için açıktır: `/actuator/health/liveness`, `/actuator/health/readiness`.

```json
{ "groups": ["liveness", "readiness"], "status": "UP" }
```

Diğer actuator uç noktaları dışarıya açık değildir.

## Hata biçimi

Tüm hatalar (filtre zinciri dahil) aynı gövdeyi döner:

```json
{
  "status": 401,
  "code": "UNAUTHORIZED",
  "message": "Bu işlem için giriş yapmanız gerekiyor.",
  "path": "/api/v1/posts",
  "timestamp": "2026-10-14T15:48:41.482Z",
  "fieldErrors": []
}
```

| `code`                   | HTTP | Anlamı                                      |
|--------------------------|------|---------------------------------------------|
| `VALIDATION_FAILED`      | 400  | Girdi doğrulaması başarısız; `fieldErrors` dolu |
| `MALFORMED_REQUEST`      | 400  | JSON okunamadı                              |
| `UNAUTHORIZED`           | 401  | Kimlik doğrulama yok veya geçersiz          |
| `FORBIDDEN`              | 403  | Yetki yok                                   |
| `NOT_FOUND`              | 404  | Kaynak yok                                  |
| `METHOD_NOT_ALLOWED`     | 405  | HTTP yöntemi desteklenmiyor                 |
| `UNSUPPORTED_MEDIA_TYPE` | 415  | İçerik türü desteklenmiyor                  |
| `CONFLICT`               | 409  | Benzersizlik vb. çakışma                    |
| `TOO_MANY_REQUESTS`      | 429  | İstek sınırı aşıldı (Aşama 2'de etkin)      |
| `INTERNAL_ERROR`         | 500  | Beklenmeyen hata; ayrıntı istemciye verilmez |

`fieldErrors` öğeleri `{ "field": "username", "message": "..." }` biçimindedir. Reddedilen değer
bilinçli olarak geri gönderilmez.

## Planlanan uç noktalar (henüz YOK)

Aşağıdaki tasarım taslaktır; ilgili aşamada uygulanıp bu belge güncellenecek.

| Yöntem | Yol                                   | Aşama | Not |
|--------|---------------------------------------|-------|-----|
| POST   | `/api/v1/auth/register`               | 2     | Kullanıcı adı, görünen ad, e-posta, parola, doğum tarihi (yalnızca yaş grubu hesaplamak için; saklanmaz) |
| POST   | `/api/v1/auth/login`                  | 2     | İstek sınırlı |
| POST   | `/api/v1/auth/refresh`                | 2     | Yenileme belirteci rotasyonu |
| POST   | `/api/v1/auth/logout`                 | 2     | Yenileme belirtecini iptal eder |
| GET    | `/api/v1/users/me`                    | 3     | |
| PATCH  | `/api/v1/users/me`                    | 3     | Görünen ad, biyografi, profil görünürlüğü |
| GET    | `/api/v1/users/{username}`            | 3     | Engelleme ve yaş görünürlük kuralları uygulanır |
| GET    | `/api/v1/users/{username}/posts`      | 3     | Keyset sayfalama |
| GET    | `/api/v1/feed?cursor=`                | 3     | Kronolojik; engellenenler hariç |
| POST   | `/api/v1/posts`                       | 3     | Metin, en fazla 2000 karakter |
| GET    | `/api/v1/posts/{id}`                  | 3     | |
| DELETE | `/api/v1/posts/{id}`                  | 3     | Yalnızca sahibi |
| PUT    | `/api/v1/posts/{id}/like`             | 4     | İdempotent |
| DELETE | `/api/v1/posts/{id}/like`             | 4     | İdempotent |
| GET    | `/api/v1/posts/{id}/comments?cursor=` | 4     | |
| POST   | `/api/v1/posts/{id}/comments`         | 4     | En fazla 1000 karakter |
| PUT    | `/api/v1/users/{username}/block`      | 4     | |
| DELETE | `/api/v1/users/{username}/block`      | 4     | |
| POST   | `/api/v1/reports`                     | 4     | Hedef: gönderi / yorum / kullanıcı |
| GET    | `/api/v1/admin/reports?status=OPEN`   | 4     | `MODERATOR`/`ADMIN` |
| POST   | `/api/v1/admin/moderation-actions`    | 4     | Denetim kaydına yazılır |
