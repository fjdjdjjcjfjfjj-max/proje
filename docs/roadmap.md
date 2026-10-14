# Yol Haritası ve Eksik Özellikler

## Aşama 1 — Altyapı ✔ (bu sürüm)

- [x] Klasör yapısı (`android/`, `backend/`, `docs/`, `infra/`)
- [x] Backend: Spring Boot 4.1, Kotlin, Security, JPA, Flyway, springdoc; Gradle Kotlin DSL + sürüm kataloğu
- [x] PostgreSQL (Docker Compose) ve V1 şeması: User, Post, Comment, Like, UserBlock, Report, ModerationAction
- [x] Merkezi hata yönetimi, güvenli varsayılan güvenlik yapılandırması, CORS, profiller (dev/test/prod)
- [x] `GET /api/v1/system/status`, `/actuator/health`
- [x] Backend testleri: Flyway + şema kısıtları (gerçek PostgreSQL), güvenlik/CORS, hata yönetimi
- [x] Android: Compose + Material 3, Hilt, Retrofit, tür güvenli navigasyon, uyarlanabilir alt navigasyon,
      açık/koyu tema, yükleniyor/boş/hata/yeniden dene durumları, sunucu bağlantı kontrolü
- [x] Android testleri yazıldı: ViewModel ve repository birim testleri (MockK), Compose UI testi

- [x] README, Windows kurulumu, mimari/güvenlik/KVKK/yaş güvenliği belgeleri

> **Doğrulama notu:** Android projesi, geliştirildiği ortamda Google Maven / Android SDK erişimi
> olmadığı için **derlenemedi**. Yalnızca Android'e bağımlı olmayan katmanlar (domain, data,
> ağ hata eşleme) ve `SystemRepositoryImplTest` ayrı bir JVM projesinde derlenip çalıştırıldı.
> İlk Android Studio senkronizasyonunda çıkabilecek sürüm/derleme hataları Aşama 2 başında giderilecek.

## Aşama 2 — Kimlik doğrulama

- Kayıt (yaş grubu hesaplama, 13 yaş altı reddi), giriş, yenileme, çıkış
- BCrypt, JWT erişim + hash'li yenileme belirteci (rotasyon), `refresh_tokens` migrasyonu
- Giriş/kayıt istek sınırı
- Android: giriş/kayıt ekranları, şifreli belirteç depolama, oturum durumu

## Aşama 3 — Profil ve gönderiler

- Profil görüntüleme/düzenleme, gönderi oluşturma/silme, kronolojik akış (keyset sayfalama)
- Engelleme ve yaş görünürlük filtrelerinin akışa uygulanması
- Yetişkinliğe geçiş planlı işi
- Android: gerçek akış, gönderi oluşturma, profil ekranları

## Aşama 4 — Etkileşim ve moderasyon

- Beğeni, yorum, engelleme, şikâyet API'leri
- Moderatör kuyruğu ve işlemleri, denetim kaydı
- IDOR ve yetki testleri

## Aşama 5 — Sağlamlaştırma

- Genel istek sınırlama, bağımlılık taraması, hata senaryoları, fiziksel silme işi
- Üretim dağıtım belgesi (TLS, ters vekil, yedekleme)

## Bilinçli olarak kapsam dışı (MVP)

Fotoğraf yükleme, özel mesajlaşma, canlı yayın, video akışı, öneri algoritması, ücretli üyelik.
Genişleme noktaları: [architecture.md](architecture.md).

## Bilinen eksikler / sınırlamalar (Aşama 1)

- Keşfet, Gönderi Oluştur, Bildirimler ve Profil sekmeleri "henüz hazır değil" bilgisi gösterir;
  sahte içerik göstermez.
- JPA entity'leri henüz yok; şema hazır, entity'ler kullanıldıkları aşamada eklenecek.
- İstek sınırlama henüz yok.
- Yaş doğrulama sağlayıcısı seçilmedi; tüm kullanıcılar `UNVERIFIED`.
- Bildirimler için veri modeli yok (Aşama 4+).
