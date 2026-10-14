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

## Kimlik doğrulama (Aşama 2)

Kimlik doğrulaması gerektiren isteklerde başlık: `Authorization: Bearer <accessToken>`.

| Belirteç | Ömür | Biçim | Saklama |
|----------|------|-------|---------|
| Erişim (`accessToken`) | 15 dakika | HS256 JWT; yalnızca kullanıcı UUID'si (`sub`) içerir | İstemcide |
| Yenileme (`refreshToken`) | 30 gün | Opak, 256 bit rastgele | Sunucuda yalnızca SHA-256 özeti |

Yenileme belirteci her kullanımda değişir (rotasyon). Daha önce kullanılmış bir yenileme belirteci
tekrar gönderilirse çalınma şüphesiyle o oturuma ait tüm belirteçler iptal edilir. İstemciler
yenilemeyi aynı anda tek istekle yapmalıdır.

### `POST /api/v1/auth/register` → `201`

```bash
curl -s -X POST https://API_ADRESI/api/v1/auth/register -H 'Content-Type: application/json' -d '{
  "username": "ayse.yilmaz",
  "displayName": "Ayşe Yılmaz",
  "email": "ayse@example.com",
  "password": "GucluParola123",
  "birthDate": "2000-05-20",
  "privacyNoticeAccepted": true
}'
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshToken": "Qm9...",
  "refreshExpiresIn": 2592000,
  "user": {
    "id": "6f1c...", "username": "ayse.yilmaz", "displayName": "Ayşe Yılmaz",
    "email": "ayse@example.com", "bio": null, "role": "USER",
    "ageGroup": "ADULT", "ageVerificationStatus": "UNVERIFIED",
    "profileVisibility": "PUBLIC", "createdAt": "2026-10-14T18:00:00Z"
  }
}
```

Kurallar:

- `username`: 3–30 karakter, harf/rakam/`_`/`.`; küçük harfe çevrilerek saklanır, büyük/küçük harf duyarsız benzersiz.
- `password`: 10–64 karakter (en fazla 72 bayt; BCrypt sınırı).
- `birthDate`: yalnızca yaş grubunu hesaplamak için kullanılır, **saklanmaz**. 13 yaş altı → `400 AGE_NOT_ELIGIBLE`.
  18 yaş altı → `ageGroup: MINOR`, `profileVisibility: PRIVATE`.
- `privacyNoticeAccepted` `true` olmalı.
- Hatalar: `400 VALIDATION_FAILED`, `409 USERNAME_TAKEN`, `409 EMAIL_TAKEN`, `429 TOO_MANY_REQUESTS`.

### `POST /api/v1/auth/login` → `200`

```json
{ "identifier": "ayse.yilmaz", "password": "GucluParola123" }
```

`identifier` kullanıcı adı veya e-posta olabilir. Yanıt `register` ile aynıdır.

- Yanlış parola ve olmayan kullanıcı **aynı** yanıtı alır: `401 INVALID_CREDENTIALS`.
- Aynı hesap için 15 dakikada 5 hatalı denemeden sonra `429 TOO_MANY_REQUESTS` (+ `Retry-After` başlığı).
- Askıya alınmış hesap (yalnızca parola doğruysa): `403 ACCOUNT_SUSPENDED`.

### `POST /api/v1/auth/refresh` → `200`

```json
{ "refreshToken": "Qm9..." }
```

Yeni belirteç çifti döner; eski yenileme belirteci artık geçersizdir. Geçersiz/süresi dolmuş/iptal
edilmiş belirteç: `401 INVALID_REFRESH_TOKEN`.

### `POST /api/v1/auth/logout` → `204`

```json
{ "refreshToken": "Qm9..." }
```

Oturumu (belirteç ailesini) iptal eder. Bilinmeyen belirteçte de `204` döner. Mevcut erişim
belirteci süresi dolana kadar (en çok 15 dakika) geçerli kalır.

### `GET /api/v1/users/me` → `200`

Oturum sahibinin bilgileri (`register` yanıtındaki `user` nesnesi). Kimlik yalnızca belirteçten
alınır. Askıya alınan/silinen hesabın belirteçleri hemen `401` alır.

### `PATCH /api/v1/users/me` → `200`

Kısmi güncelleme; gönderilmeyen alan değişmez. Yanıt `GET /users/me` ile aynıdır.

```json
{ "displayName": "Ayşe Y.", "bio": "Merhaba!", "profileVisibility": "PRIVATE" }
```

- `displayName` 1–50, `bio` en fazla 300 karakter (boş metin biyografiyi siler).
- 18 yaş altı hesap `PUBLIC` yapamaz: `403 PROFILE_VISIBILITY_NOT_ALLOWED`.
- `role`, `ageGroup`, `ageVerificationStatus` gibi alanlar gönderilse de yok sayılır.

`GET /users/me` yanıtı `stats` nesnesi de içerir: `{"postCount": 3, "likesReceived": 1362}`
(yalnızca yayındaki gönderiler sayılır).

### İstek sınırları

| Kapsam | Sınır |
|--------|-------|
| Kayıt/giriş/yenileme, IP başına | 30 istek / dakika |
| Hatalı giriş, hesap başına | 5 / 15 dakika |

Sayaçlar bellektedir (tek backend örneği). Birden fazla örneğe geçilirse paylaşımlı depoya taşınmalıdır.

## Planlanan uç noktalar (henüz YOK)

Aşağıdaki tasarım taslaktır; ilgili aşamada uygulanıp bu belge güncellenecek.

| Yöntem | Yol                                   | Aşama | Not |
|--------|---------------------------------------|-------|-----|
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
