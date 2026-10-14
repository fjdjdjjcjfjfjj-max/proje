# Sunucu Hazırlığı (Ubuntu 24.04 LTS)

Yeni bir VDS'te bir kez yapılır. Sağlayıcı panelinde işletim sistemi olarak **Ubuntu 24.04** seçin
(LTS; kısa destekli ara sürümleri, ör. 25.04, seçmeyin).

Komutlarda `SUNUCU_IP` yerine sunucunun IP adresini yazın.

## 1. İlk giriş ve güncelleme (root)

```bash
ssh root@SUNUCU_IP
passwd                                   # sağlayıcının verdiği parolayı hemen değiştirin
apt update && apt -y upgrade             # sshd_config sorusu gelirse: "keep the local version"
echo "127.0.1.1 $(hostname)" >> /etc/hosts
reboot
```

## 2. Yönetici kullanıcı

```bash
adduser deploy
usermod -aG sudo deploy
```

## 3. SSH anahtarı (kendi Windows bilgisayarınızda, cmd)

```
ssh-keygen -t ed25519
type %USERPROFILE%\.ssh\id_ed25519.pub | ssh deploy@SUNUCU_IP "mkdir -p ~/.ssh && chmod 700 ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"
ssh deploy@SUNUCU_IP                     # parola SORMADAN girmeli
```

## 4. Parola ve root girişini kapat (deploy olarak)

Yalnızca 3. adımda anahtarla giriş çalıştıysa:

```bash
sudo tee /etc/ssh/sshd_config.d/00-guvenlik.conf <<'CONF'
PermitRootLogin no
PasswordAuthentication no
KbdInteractiveAuthentication no
CONF
sudo sshd -t && sudo systemctl restart ssh && echo TAMAM
sudo sshd -T | grep -Ei '^(permitrootlogin|passwordauthentication|kbdinteractiveauthentication) '   # hepsi "no"
```

Dosya adı `00-` ile başlar: sshd ilk bulduğu değeri kullanır ve sağlayıcının `50-cloud-init.conf`
dosyası parolayı açık tutuyor olabilir. Mevcut oturumu kapatmadan yeni pencerede
`ssh deploy@SUNUCU_IP` (girmeli) ve `ssh root@SUNUCU_IP` (`Permission denied`) test edin.

## 5. Güvenlik duvarı, fail2ban, otomatik güncellemeler

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80,443/tcp
sudo ufw --force enable
sudo apt install -y unattended-upgrades fail2ban
sudo dpkg-reconfigure -plow unattended-upgrades
cat /etc/apt/apt.conf.d/20auto-upgrades   # Unattended-Upgrade "1" olmalı
```

## 6. Docker

```bash
sudo apt install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | sudo tee /etc/apt/sources.list.d/docker.list
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker deploy
exit                                      # yeniden girince docker grubu geçerli olur
```

```bash
docker run --rm hello-world
```

Not: Docker'ın yayınladığı portlar ufw kurallarını atlayabilir. Bu yüzden üretim kurulumunda
yalnızca Caddy (80/443) port yayınlar; veritabanı ve backend hiçbir host portuna bağlanmaz.

Sonraki adım: [deploy.md](deploy.md).
