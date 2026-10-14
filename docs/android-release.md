# Android Sürüm Dağıtımı (Play Store olmadan)

Her `android/` değişikliğinde GitHub Actions:

1. Birim testlerini çalıştırır.
2. Kalıcı anahtarla **imzalı release APK** derler (sürüm numarası her seferinde artar).
3. `android-v<sürüm>` etiketli bir **GitHub Release** yayınlar: `sosyal.apk` + `version.json`.

Uygulama açılışta `…/releases/latest/download/version.json` dosyasını okur; daha yeni bir sürüm
varsa "Yeni sürüm hazır" penceresi gösterir, APK'yı indirir, SHA-256 ile doğrular ve Android'in
kurulum ekranını açar. Kullanıcı yalnızca ilk seferde "Bu kaynaktan izin ver" seçeneğini açar.

Android yeni APK'yı **yalnızca aynı anahtarla imzalanmışsa** eskisinin üzerine kurar. Bu yüzden
imza anahtarı tek ve kalıcı olmalıdır.

Not: Backend değişiklikleri (kurallar, API) yeni APK gerektirmez; yalnızca sunucuda
`infra/prod/deploy.sh` çalıştırılır.

## Bir kerelik kurulum: imza anahtarı

Anahtarı sunucuda Docker ile üretebilirsiniz (bilgisayarınızda JDK gerekmez):

```bash
mkdir -p ~/imza && cd ~/imza
read -rsp "Anahtar parolası (en az 12 karakter): " KS_PASS; echo
export KS_PASS          # docker run -e KS_PASS yalnızca dışa aktarılmış değişkeni görür
echo ${#KS_PASS}        # parola uzunluğu; 0 ise tekrar girin
docker run --rm -e KS_PASS -v "$PWD":/out eclipse-temurin:21-jdk \
  sh -c 'keytool -genkeypair -keystore /out/sosyal-release.jks -storetype PKCS12 \
         -alias sosyal -keyalg RSA -keysize 4096 -validity 10000 \
         -dname "CN=Sosyal, C=TR" -storepass "$KS_PASS" -keypass "$KS_PASS"'
sudo chown "$USER" sosyal-release.jks
base64 -w0 sosyal-release.jks; echo
unset KS_PASS
```

Son komutun çıktısı (uzun tek satır) anahtarın Base64 hâlidir.

GitHub'da depo → **Settings → Secrets and variables → Actions → Secrets → New repository secret**:

| Ad | Değer |
|----|-------|
| `ANDROID_KEYSTORE_BASE64` | `base64` komutunun çıktısı |
| `ANDROID_KEYSTORE_PASSWORD` | Belirlediğiniz parola |

Ayrıca **Variables** sekmesinde `SOSYAL_API_BASE_URL` (ör. `https://203-0-113-10.sslip.io/`) tanımlı olmalıdır.

### Anahtarı yedekleyin

`sosyal-release.jks` dosyası ve parolası kaybolursa yeni sürümler eskisinin üzerine kurulamaz;
tüm kullanıcılar uygulamayı silip yeniden kurmak zorunda kalır. Dosyayı kendi bilgisayarınıza
alın (Windows cmd):

```
scp deploy@SUNUCU_IP:~/imza/sosyal-release.jks %USERPROFILE%\Documents\
```

Ardından sunucudaki kopyayı silin: `rm -rf ~/imza`. Parolayı bir parola yöneticisinde saklayın.
Bu anahtarı kimseyle paylaşmayın ve depoya eklemeyin; elinde bulunduran kişi uygulamanızın
"resmi" sürümünü taklit edebilir.

## İlk geçiş

Daha önce kurulan test APK'ları farklı (geçici) anahtarlarla imzalandı. İlk imzalı sürüm için
eski uygulamayı **bir kez silip** yeni APK'yı kurun. Sonraki sürümler uygulama içinden gelir.

İlk imzalı APK: depo → **Releases** → en üstteki sürüm → `sosyal.apk`.

## Play Store'a geçerken

Aynı anahtar Play'de "yükleme anahtarı" olarak kullanılabilir. Play'den dağıtılan uygulamalar
kendi kendini güncelleyemez (Play politikası); o aşamada uygulama içi güncelleyici Play
derlemesinde kapatılmalıdır (`sosyal.updateManifestUrl` boş bırakılarak).
