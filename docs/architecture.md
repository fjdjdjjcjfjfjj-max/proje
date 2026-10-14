# Mimari Kararlar

## 1. Genel yapı

```
Android (Compose)  ─┐
                    ├──  HTTPS / JSON  ──►  Spring Boot REST API  ──►  PostgreSQL
Web (ileride)      ─┘                       (tüm iş kuralları)        (Flyway)
```

- **Tek backend, çok istemci.** Android ve ileride geliştirilecek web sitesi aynı `/api/v1` REST API'sini
  kullanır. Yetkilendirme, yaş kuralları, engelleme filtreleri ve moderasyon **yalnızca backend'de**
  uygulanır; istemciler bu kuralların kopyasını içermez, sadece sonucu gösterir.
- **Modüler monolit.** Backend tek bir dağıtım birimidir ancak paketler özellik bazında ayrılmıştır
  (`auth`, `user`, `post`, `feed`, `moderation`). Mikroservise ihtiyaç doğarsa sınırlar hazırdır;
  MVP için gereksiz operasyonel karmaşıklık eklenmez.
- **Sürümlü API.** Tüm uç noktalar `/api/v1` altındadır. Kırıcı değişiklikler `/api/v2` ile yapılır;
  eski mobil sürümler bozulmaz. Android istemcisi bilinmeyen JSON alanlarını yok sayar.

## 2. Backend

Paket: `com.proje.sosyal`

| Paket         | Sorumluluk                                                                  | Durum     |
|---------------|------------------------------------------------------------------------------|-----------|
| `config`      | Saat, OpenAPI, CORS özellikleri                                              | Aşama 1 ✔ |
| `security`    | Filtre zinciri, 401/403 JSON yanıtları, (Aşama 2) JWT doğrulama              | Aşama 1 ✔ (kısmi) |
| `common`      | Ortak hata modeli, merkezi hata yönetimi, sistem durumu                      | Aşama 1 ✔ |
| `auth`        | Kayıt, giriş, belirteç yenileme, çıkış                                       | Aşama 2   |
| `user`        | Profil, kullanıcı adı, gizlilik, engelleme                                   | Aşama 3–4 |
| `post`        | Gönderi, yorum, beğeni                                                       | Aşama 3–4 |
| `feed`        | Ana sayfa akışı (kronolojik, engelleme ve yaş filtreli)                      | Aşama 3   |
| `moderation`  | Şikâyetler, moderatör işlemleri, denetim kaydı                               | Aşama 4   |

Henüz kod içermeyen paketler için boş dosya/sahte sınıf oluşturulmadı.

Her özellik paketinde katmanlar şöyle ayrılacak:

```
post/
  PostController.kt     HTTP, doğrulama, DTO <-> servis çağrısı (iş kuralı yok)
  PostService.kt        İş kuralları, yetki kontrolleri, transaction sınırı
  PostRepository.kt     Spring Data JPA
  Post.kt               JPA entity (API'ye ASLA doğrudan dönmez)
  dto/                  Request/Response sınıfları
```

Kararlar:

- **Entity'ler API'ye açılmaz.** Controller yalnızca DTO döner; böylece `password_hash`, `email`,
  `adult_from` gibi alanlar yanlışlıkla sızmaz.
- **Şema yalnızca Flyway ile değişir.** `ddl-auto=validate`; Hibernate şema oluşturmaz.
- **UUID birincil anahtarlar.** Sıralı kimlikler kayıt sayımına ve IDOR denemelerine kolaylık sağlar.
- **Zaman UTC.** `TIMESTAMPTZ` + `hibernate.jdbc.time_zone=UTC`; istemci yerel saate çevirir.
- **`open-in-view=false`.** Lazy yükleme controller'a sızmaz; N+1 sorunları serviste görünür olur.
- **Sanal thread'ler** (Java 21) etkin; bloklayan JDBC çağrıları için basit ve ölçeklenebilir.
- **Ortak hata modeli** (`ApiError`): `status`, `code`, `message`, `path`, `timestamp`, `fieldErrors`.
  İstemciler `code` alanına göre davranır; `message` kullanıcıya gösterilebilir Türkçe metindir.

### Gelecek özellikler için genişleme noktaları (MVP'de geliştirilmeyecek)

| Özellik                | Hazırlık                                                                                      |
|------------------------|-----------------------------------------------------------------------------------------------|
| Fotoğraf/video         | Ayrı `media` paketi + nesne depolama (S3 uyumlu); `posts` tablosuna `post_media` ilişki tablosu eklenecek. İçerik taraması (CSAM hash eşleştirme dahil) yükleme hattının zorunlu adımı olacak. |
| Özel mesajlaşma        | Ayrı `messaging` modülü. Reşit olmayanlar için varsayılan kapalı; yetişkin→reşit olmayan mesajı sunucu tarafında engellenecek (bkz. age-safety.md). |
| Canlı yayın            | Harici medya sunucusu; backend yalnızca yetki ve oturum belirteci üretir.                    |
| Öneri algoritması      | `feed` paketi arayüz arkasında; kronolojik akış ilk uygulama. Reşit olmayanlar için profilleme kısıtları uygulanacak. |
| Ücretli üyelik         | Ayrı `billing` modülü; ödeme sağlayıcı web kancaları. Reşit olmayanlara satış kuralları ayrıca değerlendirilecek. |
| Yaş doğrulama          | `AgeVerificationProvider` arayüzü; sağlayıcı bağımsız. Bkz. age-safety.md.                    |

## 3. Android

Paket: `com.proje.sosyal.android` — tek `app` modülü, katmanlar paketlerle ayrılmıştır.
Modül sayısı büyüdükçe `:core:*`, `:feature:*` Gradle modüllerine bölünebilir.

| Paket          | Sorumluluk                                                                  |
|----------------|------------------------------------------------------------------------------|
| `core`         | Ağ (Retrofit/OkHttp), sonuç/hata modeli, tema, ortak UI durum bileşenleri     |
| `data`         | API arayüzleri, DTO'lar, repository uygulamaları, Hilt bağlamaları           |
| `domain`       | Saf Kotlin modeller, repository arayüzleri, use case'ler (Android bağımlılığı yok) |
| `presentation` | Ekranlar (Compose) ve ViewModel'ler                                          |
| `navigation`   | Tür güvenli rotalar, uygulama iskeleti, alt navigasyon                       |

Kararlar:

- **MVVM + tek yönlü veri akışı.** ViewModel `StateFlow<UiState>` yayınlar; ekran durumsuzdur ve
  önizlenebilir/test edilebilir.
- **Hata soyutlaması.** Retrofit/OkHttp istisnaları `AppError`'a çevrilir; UI ağ kütüphanesini bilmez.
- **Uyarlanabilir navigasyon.** `NavigationSuiteScaffold`: telefonda alt çubuk, geniş ekranda yan ray.
- **Tema.** Açık/koyu tema sistem ayarını izler; sade lacivert/gri palet. Dinamik renk kapalı.
- **Room** yalnızca gerçekten yerel veri gerektiğinde (ör. gönderi taslakları, çevrimdışı önbellek)
  eklenecek; Aşama 1'de kullanılmıyor. Sürüm kataloğunda tanımlı.
- **Oturum belirteçleri** (Aşama 2) Android Keystore ile şifrelenmiş depolamada tutulacak;
  `allowBackup=false` ve veri çıkarma kuralları bunların yedeğe girmesini engeller.
- **Sahte veri** yalnızca `@Preview` fonksiyonlarında ve testlerde kullanılır.
