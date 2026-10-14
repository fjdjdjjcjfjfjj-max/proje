# KVKK ve Mevzuat Uyum Gereksinimleri

> **Önemli:** Bu belge, geliştirme ekibinin dikkate alması gereken gereksinimlerin bir **listesidir**.
> Platformun KVKK'ya veya başka bir mevzuata uyumlu olduğu **doğrulanmamıştır** ve bu belge hukuki
> görüş değildir. Yayına almadan önce Türk hukukunda uzman bir avukat tarafından incelenmelidir.
> Mevzuat ve Kurul kararları değişebilir; güncel metinler esas alınmalıdır.

## İlgili düzenlemeler (incelenecek)

- 6698 sayılı Kişisel Verilerin Korunması Kanunu (KVKK) ve ikincil düzenlemeler
- 5651 sayılı İnternet Ortamında Yapılan Yayınların Düzenlenmesi Kanunu (sosyal ağ sağlayıcı
  yükümlülükleri, temsilci atama, raporlama, içerik kaldırma süreleri, veri yerelleştirme hükümleri)
- 6502 sayılı Tüketicinin Korunması Hakkında Kanun (ileride ücretli üyelik için)
- 5809 sayılı Elektronik Haberleşme Kanunu ve 6563 sayılı Elektronik Ticaretin Düzenlenmesi Hakkında
  Kanun (ticari elektronik ileti/bildirim için)
- Çocukların sosyal medya kullanımına ilişkin yürürlükteki veya yürürlüğe girecek düzenlemeler
  (asgari yaş, ebeveyn onayı, yaş doğrulama yükümlülükleri) — **güncel durumu hukukçu ile teyit edin**
- Yurt dışına veri aktarımı hükümleri (KVKK md. 9) — barındırma ve üçüncü taraf servis seçimini etkiler

## Teknik gereksinim listesi

| # | Gereksinim | Durum |
|---|------------|-------|
| 1 | Aydınlatma metni: kayıt ekranından erişilebilir, hangi verinin hangi amaçla işlendiği | Yapılmadı (Aşama 2) |
| 2 | Açık rıza gerektiren işlemlerin ayrıştırılması; rızanın kaydı ve geri alınabilmesi | Yapılmadı |
| 3 | Veri en aza indirme: tam doğum tarihi saklanmaz, konum toplanmaz | Şemada uygulandı |
| 4 | İlgili kişi hakları (md. 11): verilere erişim, düzeltme, silme talepleri için API ve süreç | Yapılmadı |
| 5 | Hesap silme ve fiziksel silme işi; saklama süreleri politikası | Şema hazır, iş yapılmadı |
| 6 | Kişisel verilerin loglara ve hata mesajlarına yazılmaması | Hata yönetiminde uygulandı; log politikası her aşamada denetlenecek |
| 7 | Parolaların güvenli hash'lenmesi (BCrypt) | Aşama 2 |
| 8 | Aktarım güvenliği (TLS), Android release'de cleartext yasağı | Android'de uygulandı; sunucu TLS'i dağıtım aşamasında |
| 9 | Erişim yetkilendirmesi ve yönetici işlemlerinin denetim kaydı | Denetim tablosu hazır; yetki kontrolleri Aşama 4 |
| 10 | Veri ihlali müdahale planı ve Kurul'a bildirim süreci | Yapılmadı (organizasyonel) |
| 11 | VERBİS kayıt yükümlülüğünün değerlendirilmesi | Yapılmadı (organizasyonel) |
| 12 | Veri işleme envanteri | Yapılmadı (organizasyonel) |
| 13 | Barındırma konumu ve yurt dışı aktarım değerlendirmesi | Yapılmadı |
| 14 | 5651 kapsamındaki trafik/erişim kayıtlarının ayrı, erişimi kısıtlı depoda tutulması | Yapılmadı |
| 15 | İçerik kaldırma/erişim engelleme kararlarına yanıt süreci ve kaydı | Moderasyon altyapısı Aşama 4 |
| 16 | Reşit olmayanlar için ek korumalar ve gerekiyorsa veli onayı akışı | Tasarım: age-safety.md |

## Loglama politikası

- Loglanmaz: parola, belirteç, e-posta, doğum tarihi, istek/yanıt gövdesi, kısıt ihlali ayrıntısı,
  gönderi/yorum metni.
- Loglanabilir: istek yolu, HTTP durum kodu, istisna türü, iç kullanıcı UUID'si (gerektiğinde).
- Android: debug derlemede yalnızca `BASIC` HTTP logu (yöntem, URL, durum); release'de log yok.
