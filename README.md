# Sosyal — Yerli Sosyal Medya Platformu (MVP)

Türkiye'deki kullanıcılar için, 18 yaş altı ve üstü kullanıcılara yaşa uygun kurallar uygulayan
sosyal medya platformunun temel altyapısı.

| Klasör     | İçerik                                                                 |
|------------|------------------------------------------------------------------------|
| `backend/` | Kotlin + Spring Boot 4 REST API, PostgreSQL, Flyway                     |
| `android/` | Kotlin + Jetpack Compose (Material 3, MVVM, Hilt, Retrofit)             |
| `docs/`    | Mimari, API, veritabanı, güvenlik, yaş güvenliği, KVKK, kurulum belgeleri |
| `infra/`   | Yerel geliştirme için Docker Compose (PostgreSQL)                      |

> **Durum: Aşama 1 (altyapı).** Kayıt/giriş, gönderi, yorum, beğeni, engelleme ve şikâyet
> **henüz geliştirilmedi**. Veritabanı şeması bu özellikler için hazırdır; API ve ekranlar
> sonraki aşamalarda eklenecek. Ayrıntı: [docs/roadmap.md](docs/roadmap.md).

## Hızlı başlangıç

Ayrıntılı Windows talimatları: [docs/windows-setup.md](docs/windows-setup.md).

### Gereksinimler

- JDK 21
- Docker Desktop (Docker Compose v2)
- Android Studio (Android SDK 36) — yalnızca Android uygulaması için
- Git

### 1. Ortam dosyasını hazırla

```powershell
# Windows PowerShell (proje kökünde)
Copy-Item infra\.env.example infra\.env
notepad infra\.env    # POSTGRES_PASSWORD değerini değiştirin
```

```bash
# macOS / Linux
cp infra/.env.example infra/.env
```

`infra/.env` Git'e eklenmez (`.gitignore`).

### 2. PostgreSQL'i başlat

```powershell
docker compose -f infra/docker-compose.yml --env-file infra/.env up -d
docker compose -f infra/docker-compose.yml --env-file infra/.env ps   # STATUS: healthy
```

### 3. Backend'i çalıştır

```powershell
cd backend
.\gradlew.bat bootRun        # Windows
./gradlew bootRun            # macOS / Linux
```

`bootRun` varsayılan olarak `dev` profiliyle başlar ve `../infra/.env` dosyasını okur. Flyway
migrasyonları açılışta otomatik uygulanır.

Kontrol:

```powershell
curl.exe http://localhost:8080/api/v1/system/status
curl.exe http://localhost:8080/actuator/health
```

- Swagger UI (yalnızca dev): <http://localhost:8080/swagger-ui/index.html>
- OpenAPI JSON (yalnızca dev): <http://localhost:8080/v3/api-docs>

### 4. Backend testleri

Entegrasyon testleri **Testcontainers** ile gerçek bir PostgreSQL 16 konteyneri başlatır; Docker
Desktop çalışıyor olmalıdır.

```powershell
cd backend
.\gradlew.bat test
```

Test raporu: `backend/build/reports/tests/test/index.html`

### 5. Android uygulaması

> Android projesi bu depoyu hazırlayan ortamda Android SDK'ya erişilemediği için henüz derlenip
> çalıştırılmadı; ilk Android Studio senkronizasyonunda sürüm uyumsuzluğu çıkarsa lütfen bildirin.

1. Android Studio ile `android/` klasörünü açın (proje kökünü değil).
2. Gradle senkronizasyonunun bitmesini bekleyin.
3. Backend çalışırken bir emülatörde `app` yapılandırmasını çalıştırın. Emülatör, geliştirme
   makinesindeki backend'e `http://10.0.2.2:8080/` üzerinden ulaşır.

Komut satırından:

```powershell
cd android
.\gradlew.bat assembleDebug          # APK: app\build\outputs\apk\debug\
.\gradlew.bat testDebugUnitTest      # JVM birim testleri
.\gradlew.bat connectedDebugAndroidTest   # Compose UI testleri (emülatör/cihaz gerekir)
```

API adresi koda sabitlenmez; değiştirmek için `android/local.properties.example` dosyasına bakın.

## Ortamlar

| Profil | Nasıl seçilir                                | Notlar                                                          |
|--------|----------------------------------------------|-----------------------------------------------------------------|
| `dev`  | `bootRun` varsayılanı                         | Swagger açık, `infra/.env` okunur, localhost CORS               |
| `test` | Testlerde otomatik                            | Testcontainers PostgreSQL                                        |
| `prod` | `SPRING_PROFILES_ACTIVE=prod`                 | Swagger kapalı; `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ALLOWED_ORIGINS` zorunlu |

Gizli değerler yalnızca ortam değişkenleriyle verilir. Depoda parola, anahtar veya üretim adresi yoktur.

## Belgeler

- [Mimari kararlar](docs/architecture.md)
- [API uç noktaları ve örnek istekler](docs/api.md)
- [Veritabanı modeli, silme ve saklama kuralları](docs/database.md)
- [Güvenlik değerlendirmesi](docs/security.md)
- [Yaşa uygun güvenlik tasarımı](docs/age-safety.md)
- [KVKK ve mevzuat uyum gereksinimleri](docs/compliance-kvkk.md)
- [Windows kurulumu](docs/windows-setup.md)
- [Sunucu hazırlığı (Ubuntu)](docs/server-setup.md)
- [Sunucuya kurulum ve yedekleme](docs/deploy.md)
- [Android sürüm dağıtımı ve uygulama içi güncelleme](docs/android-release.md)
- [Yol haritası ve eksik özellikler](docs/roadmap.md)
