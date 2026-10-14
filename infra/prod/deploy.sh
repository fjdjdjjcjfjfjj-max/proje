#!/usr/bin/env bash
# Sunucuda en son kodu çekip servisleri yeniden derler ve başlatır.
# Kullanım (depo kökünde, deploy kullanıcısıyla):  infra/prod/deploy.sh
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_DIR"

[ -f infra/prod/.env ] || { echo "infra/prod/.env yok. Önce: cp infra/prod/.env.example infra/prod/.env" >&2; exit 1; }
grep -qE '^API_DOMAIN=.+' infra/prod/.env || { echo "infra/prod/.env içinde API_DOMAIN boş" >&2; exit 1; }
grep -qE '^POSTGRES_PASSWORD=.+' infra/prod/.env || { echo "infra/prod/.env içinde POSTGRES_PASSWORD boş" >&2; exit 1; }

git pull --ff-only
docker compose -f infra/prod/docker-compose.yml --env-file infra/prod/.env up -d --build --remove-orphans
docker image prune -f > /dev/null

domain="$(grep -E '^API_DOMAIN=' infra/prod/.env | cut -d= -f2-)"
echo "Backend başlatılıyor (Flyway migrasyonları uygulanır)..."
for _ in $(seq 1 60); do
    if curl -fsS "https://$domain/actuator/health" 2>/dev/null | grep -q '"UP"'; then
        echo "Hazır: https://$domain/api/v1/system/status"
        exit 0
    fi
    sleep 3
done
echo "Backend 3 dakika içinde hazır olmadı. Loglar:" >&2
docker compose -f infra/prod/docker-compose.yml --env-file infra/prod/.env logs --tail 80 backend caddy >&2
exit 1
