# Windows Kurulum Rehberi

Windows 10 (22H2) / Windows 11, PowerShell 5.1+ veya PowerShell 7.

## 1. Gereksinimler

| Araç | Sürüm | Kurulum |
|------|-------|---------|
| Git for Windows | güncel | `winget install --id Git.Git -e` |
| JDK | 21 (LTS) | `winget install --id EclipseAdoptium.Temurin.21.JDK -e` |
| Docker Desktop | güncel (WSL 2 arka ucu) | `winget install --id Docker.DockerDesktop -e` |
| Android Studio | güncel kararlı (Android SDK 36) | `winget install --id Google.AndroidStudio -e` |

Gradle kurmanıza gerek yoktur; projedeki `gradlew.bat` doğru sürümü (8.14.3) indirir.

Kurulumdan sonra yeni bir PowerShell penceresinde kontrol edin:

```powershell
git --version
java -version          # "21" görmelisiniz
docker version
docker compose version
```

`java -version` 21 değilse `JAVA_HOME` ortam değişkenini JDK 21 klasörüne ayarlayın:

```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-21...", "User")
```

### Docker Desktop

- İlk açılışta WSL 2 kurulumunu kabul edin (`wsl --install` gerekebilir; yeniden başlatma ister).
- BIOS'ta sanallaştırma (Intel VT-x / AMD-V) açık olmalıdır.

## 2. Projeyi al

```powershell
git clone <depo-adresi> sosyal
cd sosyal
git config core.autocrlf false   # .gitattributes satır sonlarını yönetir
```

## 3. Veritabanı

```powershell
Copy-Item infra\.env.example infra\.env
notepad infra\.env               # POSTGRES_PASSWORD'ü değiştirin
docker compose -f infra/docker-compose.yml --env-file infra/.env up -d
docker compose -f infra/docker-compose.yml --env-file infra/.env ps
```

5432 portu başka bir PostgreSQL tarafından kullanılıyorsa `infra\.env` içinde `DB_PORT=5433`
yapın; backend aynı dosyayı okuduğu için başka değişiklik gerekmez.

Durdurmak: `docker compose -f infra/docker-compose.yml --env-file infra/.env down`
Verileri de silmek: aynı komuta `-v` ekleyin.

## 4. Backend

```powershell
cd backend
.\gradlew.bat bootRun
```

Başka bir pencerede:

```powershell
curl.exe http://localhost:8080/api/v1/system/status
```

Testler (Docker Desktop açık olmalı):

```powershell
.\gradlew.bat test
start build\reports\tests\test\index.html
```

IntelliJ IDEA ile: `backend` klasörünü açın, `SosyalApplication.kt` çalıştırma yapılandırmasında
**Active profiles** alanına `dev` yazın ve **Working directory** olarak `backend` klasörünü seçin
(`../infra/.env` göreli yolu buna bağlıdır).

## 5. Android

1. Android Studio → **Open** → `sosyal\android` klasörünü seçin.
2. SDK Manager'dan **Android SDK Platform 36** kurulu olduğundan emin olun.
3. Device Manager'dan bir emülatör (API 26+) oluşturun.
4. Backend çalışırken **Run ▶ app**.

Komut satırı:

```powershell
cd android
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest   # emülatör açıkken
```

Gerçek cihaz kullanıyorsanız backend adresini `android\local.properties` içinde
`sosyal.apiBaseUrl=http://<bilgisayar-IP>:8080/` olarak verin ve aynı IP'yi
`app\src\debug\res\xml\network_security_config.xml` dosyasına ekleyin. Windows Güvenlik
Duvarı'nda 8080 portuna yerel ağdan izin vermeniz gerekebilir.

## Sık karşılaşılan sorunlar

| Belirti | Çözüm |
|---------|-------|
| `Could not find a valid Docker environment` (testlerde) | Docker Desktop'ı başlatın ve `docker ps` çalıştığını doğrulayın. |
| `password authentication failed` | `infra\.env` değiştirildikten sonra eski volume duruyor: `docker compose ... down -v` ve tekrar `up -d`. |
| `Port 8080 was already in use` | `$env:SERVER_PORT=8081; .\gradlew.bat bootRun` |
| Emülatör "Bağlantı kurulamadı" gösteriyor | Backend çalışıyor mu? Emülatörde `localhost` değil `10.0.2.2` kullanılır (varsayılan budur). |
| Türkçe karakterler bozuk | Dosyalar UTF-8'dir; IDE kodlamasını UTF-8 yapın. |
