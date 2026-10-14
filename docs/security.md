# Güvenlik Değerlendirmesi

Durum sütunu Aşama 1 sonundaki gerçek durumu gösterir.

## Uygulanan kontroller (Aşama 1)

| Kontrol | Nerede |
|---------|--------|
| Varsayılan olarak her uç nokta kimlik doğrulaması ister; açık olanlar tek tek listelenir | `security/SecurityConfig.kt` |
| Durumsuz API: oturum çerezi yok, form/basic giriş kapalı | `SecurityConfig.kt` |
| Spring'in rastgele parolalı varsayılan kullanıcısı devre dışı | `application.yml` (`UserDetailsServiceAutoConfiguration` hariç) |
| 401/403 dahil tüm hatalar aynı JSON biçiminde; yığın izi, istisna mesajı, SQL ayrıntısı istemciye gitmez | `common/error/*`, `security/JsonSecurityErrorHandlers.kt`, `server.error.*` |
| Kısıt ihlallerinde kişisel veri içerebilecek ayrıntı loglanmaz | `GlobalExceptionHandler.handleIntegrity` |
| CORS yalnızca yapılandırılmış origin'ler; joker karakter reddedilir; kimlik bilgisi (çerez) kapalı | `config/CorsProperties.kt`, `SecurityConfig.corsConfigurationSource` |
| Güvenlik başlıkları: `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, HSTS (HTTPS'te) | Spring Security |
| Actuator yalnızca `health` ve `info`; ayrıntı gösterilmez | `application.yml` |
| Swagger/OpenAPI yalnızca dev profilinde | `application.yml`, `application-dev.yml` |
| Gizli değerler ortam değişkenlerinde; prod profili eksik değerde başlamaz | `application-prod.yml`, `infra/.env.example` |
| Veritabanı portu yalnızca `127.0.0.1`'e bağlı | `infra/docker-compose.yml` |
| Flyway `clean` devre dışı; şema `validate` | `application.yml` |
| Veritabanı kısıtları: benzersizlik, uzunluk, kendini engelleme, tekrarlı şikâyet, değiştirilemez denetim kaydı | `V1__initial_schema.sql` |
| Android release: yalnızca HTTPS; debug'da cleartext yalnızca `10.0.2.2`/`localhost` | `network_security_config.xml` |
| Android: `allowBackup=false`, veri çıkarma kuralları; API adresi koda sabit değil | `AndroidManifest.xml`, `app/build.gradle.kts` |

## Risk değerlendirmesi ve planlanan önlemler

| Risk | Önlem | Aşama |
|------|-------|-------|
| **SQL injection** | Yalnızca JPA/parametreli sorgular; string birleştirme ile SQL yasak (kod incelemesi kuralı). | Sürekli |
| **IDOR** | UUID kimlikler (tahmin zorluğu) **yetmez**: her servis metodu kaynağın sahibini/görünürlüğünü kimliği doğrulanmış kullanıcıya göre kontrol eder. Kimlik istemciden gelen gövdeden değil, belirteçten alınır. Her uç nokta için "başka kullanıcının kaynağı" entegrasyon testi yazılacak. | 3–4 |
| **Yetkisiz erişim** | Varsayılan `authenticated`; yönetici uç noktaları `/api/v1/admin/**` + `@PreAuthorize("hasRole('MODERATOR')")`. Rol belirteçten değil, her istekte veritabanındaki kullanıcı durumundan doğrulanır (askıya alınan moderatör anında yetkisini kaybeder). | 2, 4 |
| **Brute-force** | Giriş için IP + hesap bazlı istek sınırı; ardışık başarısız denemelerde artan gecikme; hata mesajı "e-posta veya parola hatalı" (hesap varlığı sızdırılmaz). | 2 |
| **Spam** | Gönderi/yorum/şikâyet/kayıt için kullanıcı bazlı istek sınırı; yeni hesaplara daha düşük kota; tekrarlı açık şikâyet DB'de engelli. | 2–4 |
| **JWT yaşam döngüsü** | Kısa ömürlü erişim belirteci (≈15 dk), uzun ömürlü yenileme belirteci (≈30 gün) veritabanında **hash'lenmiş** saklanır, her kullanımda rotasyon, yeniden kullanım tespitinde tüm aile iptal. İmza anahtarı ortam değişkeninden; HS256 için ≥256 bit. Çıkışta iptal. | 2 |
| **Parola güvenliği** | `BCryptPasswordEncoder` (maliyet 12) `DelegatingPasswordEncoder` ile (ileride algoritma geçişi). Asgari uzunluk 10, üst sınır 128 (BCrypt 72 bayt sınırına karşı). | 2 |
| **Hesap sayımı** | Kayıt/giriş/parola sıfırlama yanıtları hesap varlığını ele vermez. | 2 |
| **XSS (web)** | API metni ham döner; web istemcisi kaçışlar. API JSON `Content-Type` ile döner. | Web |
| **Kitle ataması** | Ayrı request DTO'ları; `role`, `status`, `age_verification_status` asla istemciden alınmaz. | 2+ |
| **Yönetici işlemleri** | Her moderasyon işlemi aynı transaction'da `moderation_actions` kaydı oluşturur; kayıt değiştirilemez. | 4 |
| **Bağımlılık açıkları** | Sürümler sabit; düzenli güncelleme ve CI'da bağımlılık taraması (ör. OWASP Dependency-Check / GitHub Dependabot). | 5 |
| **Ters vekil başlıkları** | Prod'da `forward-headers-strategy: native` yalnızca güvenilir proxy arkasında; istek sınırı gerçek istemci IP'sine göre. | Dağıtım |

## Kimlik doğrulama (Aşama 2'de uygulandı)

| Kontrol | Uygulama |
|---------|----------|
| Parola hash'i | `DelegatingPasswordEncoder` + BCrypt maliyet 12; 10–64 karakter, en fazla 72 bayt |
| Erişim belirteci | HS256 JWT, 15 dk, yalnızca kullanıcı UUID'si; anahtar `JWT_SECRET` (≥ 32 bayt) |
| Yenileme belirteci | 256 bit rastgele, sunucuda yalnızca SHA-256; her kullanımda rotasyon; tekrar kullanımda aile iptali; satır kilidiyle eşzamanlı yenileme engeli |
| Hesap durumu | Her istekte veritabanından: askıya alınan/silinen hesabın belirteçleri hemen geçersiz; rol belirteçten değil DB'den |
| Hesap sayımı | Yanlış parola ve olmayan kullanıcı aynı yanıt ve benzer süre (sahte hash karşılaştırması) |
| Kaba kuvvet | Hesap başına 5 hatalı giriş / 15 dk; IP başına 30 istek / dk (kayıt, giriş, yenileme) |
| Kitle ataması | `role`, `status`, `ageVerificationStatus` istekten alınmaz |
| Android | Belirteçler Android Keystore AES-GCM anahtarıyla şifreli; yedeğe girmez; 401'de tekil yenileme |

Bilinen kısıtlar:

- Kayıt, e-postanın kullanımda olduğunu `EMAIL_TAKEN` ile bildirir (hesap varlığı sızar). E-posta
  doğrulama akışı eklendiğinde "e-postanızı kontrol edin" yanıtına çevrilmelidir. Kayıt IP başına
  sınırlıdır.
- Hesap kilidi, saldırganın bir kullanıcıyı 15 dk dışarıda bırakmasına izin verir (bilinçli ödünleşim).
- Çıkıştan sonra erişim belirteci en fazla 15 dk geçerli kalır.
- İstek sınırı sayaçları bellektedir; çoklu örnekte paylaşımlı depoya taşınmalı. Ters vekilde
  genel bir sınır da önerilir.

## CSRF

API durumsuzdur ve kimlik bilgisi `Authorization` başlığıyla taşınır; tarayıcı bunu otomatik
eklemediği için CSRF koruması kapalıdır. Web istemcisi için çerez tabanlı oturuma geçilirse
CSRF koruması yeniden açılmalı ve `SameSite` çerez politikası uygulanmalıdır.
