# Sunucuya Kurulum (Ubuntu 24.04)

Bu rehber test sunucusunu sıfırdan yayına alır. Önkoşul olarak [server-setup.md](server-setup.md) adımları
yapılmış olmalıdır: `deploy` kullanıcısı, yalnızca SSH anahtarıyla giriş, root girişi kapalı,
ufw (22/80/443), fail2ban, otomatik güvenlik güncellemeleri, Docker.

```
İnternet ──► Caddy :80/:443 (otomatik HTTPS) ──► backend :8080 ──► postgres :5432
             (tek açık servis)                   (iç ağ)           (iç ağ, internete çıkışı yok)
```

## 1. Adres (alan adı olmadan)

Aşağıdaki örneklerde `203.0.113.10` örnek bir IP'dir; kendi sunucu IP'nizi yazın.

Alan adı yokken [sslip.io](https://sslip.io) kullanılır: `203-0-113-10.sslip.io` adresi otomatik
olarak `203.0.113.10` IP'sine çözülür ve Let's Encrypt bu adrese gerçek bir sertifika verir.

Sunucuda doğrulayın:

```bash
getent hosts 203-0-113-10.sslip.io     # 203.0.113.10 dönmeli
```

Dönmezse aynı biçimde `203-0-113-10.nip.io` deneyin ve aşağıda onu kullanın.

Gerçek alan adı alındığında: alan adının DNS'inde `api` için bu IP'ye bir **A kaydı** açın,
`infra/prod/.env` içindeki `API_DOMAIN` değerini değiştirin ve `infra/prod/deploy.sh` çalıştırın.

## 2. Kodu sunucuya al

```bash
ssh deploy@203.0.113.10
sudo apt install -y git
git clone https://github.com/fjdjdjjcjfjfjj-max/proje.git ~/sosyal
cd ~/sosyal
git checkout claude/amazing-lamport-jwe4bg      # ana dala birleştirilene kadar
```

## 3. Ortam dosyası

```bash
cp infra/prod/.env.example infra/prod/.env
sed -i "s/^API_DOMAIN=.*/API_DOMAIN=203-0-113-10.sslip.io/" infra/prod/.env     # kendi IP'niz
sed -i "s/^POSTGRES_PASSWORD=.*/POSTGRES_PASSWORD=$(openssl rand -hex 24)/" infra/prod/.env
chmod 600 infra/prod/.env
cat infra/prod/.env          # API_DOMAIN doğru mu kontrol edin
```

`JWT_SECRET` boş bırakılırsa `deploy.sh` ilk çalıştırmada rastgele üretir. Değiştirilirse
tüm kullanıcıların oturumu kapanır.

Parolayı bir yere not etmeniz gerekmez; yedekten geri dönüşte de bu dosya kullanılır.
**Bu dosyayı kaybederseniz** veritabanı volume'ü hâlâ eski parolayı bekler; dosyayı yedekleyin.

## 4. Başlat

```bash
infra/prod/deploy.sh
```

İlk çalıştırma birkaç dakika sürer (imaj derleme + sertifika alma). Sonunda
`Hazır: https://203-0-113-10.sslip.io/api/v1/system/status` yazar. Tarayıcıdan açıp
`{"status":"UP",...}` gördüğünüzde kurulum tamamdır.

Sonraki her güncellemede yalnızca:

```bash
cd ~/sosyal && infra/prod/deploy.sh
```

## 5. Otomatik yedek

```bash
sudo crontab -e
```

En alta ekleyin (her gece 03:30, sunucu saatiyle):

```
30 3 * * * /home/deploy/sosyal/infra/prod/backup.sh >> /var/log/sosyal-backup.log 2>&1
```

Elle denemek: `sudo infra/prod/backup.sh` → `/var/backups/sosyal/` altında `.sql.gz` dosyası oluşur.

**Yedekleri sunucu dışına kopyalayın.** Kendi Windows bilgisayarınızdan (cmd):

```
scp deploy@203.0.113.10:/var/backups/sosyal/*.sql.gz C:\Users\%USERNAME%\Documents\sosyal-yedek\
```

`/var/backups/sosyal` yalnızca root tarafından okunabilir; `scp` için önce sunucuda
`sudo cp /var/backups/sosyal/<dosya> ~ && sudo chown deploy ~/<dosya>` yapın veya yedek
dizinini `deploy` grubuna açın.

### Geri yükleme

```bash
cd ~/sosyal
gzip -dc /var/backups/sosyal/sosyal-YYYYmmdd-HHMMSS.sql.gz | \
  docker compose -f infra/prod/docker-compose.yml --env-file infra/prod/.env exec -T postgres \
  sh -c 'psql -q -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Yedek `--clean` ile alındığı için mevcut tabloları silip yedekteki hâliyle yeniden oluşturur.

## İşletim komutları

```bash
C="docker compose -f infra/prod/docker-compose.yml --env-file infra/prod/.env"
$C ps                       # durum
$C logs -f backend          # backend logları
$C logs -f caddy            # sertifika / erişim logları
$C restart backend
$C down                     # durdur (veriler korunur)
```

`$C down -v` **veritabanını siler**; kullanmayın.

## Sorun giderme

| Belirti | Neden / çözüm |
|---------|---------------|
| Tarayıcıda sertifika hatası, Caddy logunda `challenge failed` | 80/443 dışarıdan erişilemiyor: `sudo ufw status`, sağlayıcı panelinde ek güvenlik duvarı var mı? `getent hosts` doğru IP'yi dönüyor mu? |
| Caddy logunda `rateLimited` | Let's Encrypt istek sınırı. `nip.io` adresine geçin veya bir süre bekleyin. |
| `deploy.sh` "3 dakika içinde hazır olmadı" | `$C logs backend` — genelde `.env` parolası ile mevcut volume uyuşmazlığı. |
| Disk doluyor | `docker system df`; eski imajlar `docker image prune` ile temizlenir. |

## Daha büyük sunucuya taşıma

1. Eski sunucuda `sudo infra/prod/backup.sh`, yedeği ve `infra/prod/.env`'i yeni sunucuya kopyalayın.
2. Yeni sunucuda bu rehberin 2–4. adımları (`.env` olarak eski dosyayı kullanın).
3. Geri yükleme komutuyla yedeği yükleyin.
4. Alan adının A kaydını yeni IP'ye çevirin (sslip.io kullanıyorsanız `API_DOMAIN`'i ve
   Android'deki `SOSYAL_API_BASE_URL`'i yeni IP'ye göre güncelleyin).

## Android APK'yı sunucuya bağlama

Depo herkese açık olduğu için sunucu adresi koda yazılmaz. GitHub'da depo sayfasında
**Settings → Secrets and variables → Actions → Variables → New repository variable**:

- Name: `SOSYAL_API_BASE_URL`
- Value: `https://203-0-113-10.sslip.io/` (kendi adresiniz, sonunda `/` ile)

Sonra **Actions → Android APK → Run workflow** ile yeni APK derleyin; indirilen APK bu
adrese bağlanır.
