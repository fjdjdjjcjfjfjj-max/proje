# Yaşa Uygun Güvenlik Tasarımı

Bu belge teknik tasarımı açıklar. Hukuki yeterliliği **doğrulanmamıştır**; uygulanmadan önce
çocuk hakları ve KVKK konusunda uzman bir hukukçu tarafından incelenmelidir.

## İlkeler

1. **Veri en aza indirme.** Tam doğum tarihi saklanmaz; yalnızca yaş grubu ve (reşit olmayanlar için)
   yetişkinliğe geçiş ayı tutulur (bkz. database.md).
2. **Beyan ≠ doğrulama.** Kayıtta verilen yaş bir *beyandır*. `age_verification_status = VERIFIED`
   olmadıkça arayüzde, API'de veya pazarlamada "yaşı doğrulanmış" ifadesi kullanılmaz.
3. **Güvenli varsayılanlar.** Reşit olmayan kullanıcılar için en kısıtlayıcı ayarlar varsayılandır;
   kullanıcı yalnızca izin verilen sınırlar içinde gevşetebilir.
4. **Sunucu tarafı zorlama.** Tüm yaş kuralları backend servis katmanında uygulanır. İstemcide
   bir düğmeyi gizlemek kontrol sayılmaz.
5. **Belirsizlikte koruyucu davranış.** Yaş grubu bilinmiyorsa veya çelişkiliyse kullanıcı `MINOR`
   kurallarına tabi tutulur.

## Yaş grupları

| Grup       | Kural |
|------------|-------|
| 13 yaş altı| Kayıt kabul edilmez (Aşama 2). Asgari yaş, yürürlükteki mevzuata göre hukukçu ile teyit edilecek. |
| `MINOR` (13–17) | Aşağıdaki kısıtlamalar |
| `ADULT` (18+)   | Standart kurallar |

## `MINOR` kullanıcılar için kurallar (planlanan, Aşama 3–4'te uygulanacak)

| Alan                         | Kural |
|------------------------------|-------|
| Profil görünürlüğü           | Varsayılan `PRIVATE` (şemada tüm kullanıcılar için varsayılan zaten `PRIVATE`). |
| Keşfet / arama               | Reşit olmayanların profilleri yetişkinlerin arama ve keşfet sonuçlarında gösterilmez. |
| Yetişkinlerle etkileşim      | Yetişkinler, takip/izin ilişkisi olmayan reşit olmayan kullanıcıların gönderilerine yorum yapamaz. |
| Özel mesajlaşma (gelecek)    | Varsayılan kapalı. Yetişkinden reşit olmayana mesaj başlatma sunucu tarafında reddedilir. |
| Şikâyet önceliği             | `CHILD_SAFETY` nedeni ve reşit olmayan kullanıcıyı içeren şikâyetler moderasyon kuyruğunda önceliklidir. |
| Kişiselleştirilmiş reklam    | Uygulanmaz (MVP'de reklam yok). |
| Konum                        | Toplanmaz. |

Yetişkinliğe geçiş: `adult_from <= bugün` olan kullanıcıları `ADULT` grubuna taşıyan planlı iş
(Aşama 3). Geçişte gizlilik ayarları otomatik gevşetilmez; kullanıcıya bildirilir.

## Bağımsız yaş doğrulama servisi

Yaş doğrulama yöntemi (ör. e-Devlet entegrasyonu, kimlik belgesi doğrulayan üçüncü taraf, ebeveyn
onayı) henüz seçilmedi. Mimari sağlayıcıdan bağımsızdır:

```kotlin
interface AgeVerificationProvider {
    val id: String
    fun startVerification(userId: UUID): VerificationSession   // yönlendirme URL'i vb.
    fun handleCallback(payload: ProviderCallback): VerificationResult // imzası doğrulanmış sonuç
}
```

- Sağlayıcıdan yalnızca **sonuç** (`yaş ≥ 18`, `yaş ≥ 13`) alınır; kimlik belgesi görüntüsü veya
  TC kimlik numarası platform veritabanında saklanmaz.
- Sonuç `age_verification_status` ve ayrı bir `age_verifications` denetim tablosuna (sağlayıcı,
  zaman, sonuç; kişisel belge yok) yazılır. Bu tablo ilgili aşamada migrasyonla eklenecek.
- Bu arayüz Aşama 1'de **kod olarak eklenmedi**; tasarım belgesidir.
