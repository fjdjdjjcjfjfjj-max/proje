#!/usr/bin/env bash
# PostgreSQL yedeği alır: /var/backups/sosyal/sosyal-YYYYmmdd-HHMMSS.sql.gz
# Son KEEP_DAYS günün yedekleri tutulur.
#
# ÖNEMLİ: Yalnızca aynı sunucuda duran yedek, disk arızasında veya sunucu kaybında işe yaramaz.
# Yedekleri düzenli olarak başka bir yere (ör. kendi bilgisayarınıza, nesne depolamaya) kopyalayın.
#
# Elle çalıştırma (depo kökünde):  sudo infra/prod/backup.sh
# Zamanlama: docs/deploy.md -> "Otomatik yedek"
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE=(docker compose -f "$REPO_DIR/infra/prod/docker-compose.yml" --env-file "$REPO_DIR/infra/prod/.env")
BACKUP_DIR="${BACKUP_DIR:-/var/backups/sosyal}"
KEEP_DAYS="${KEEP_DAYS:-14}"

umask 077
mkdir -p "$BACKUP_DIR"
target="$BACKUP_DIR/sosyal-$(date -u +%Y%m%d-%H%M%S).sql.gz"
tmp="$target.partial"

# Kimlik bilgileri konteynerin kendi ortam değişkenlerinden okunur; komut satırına parola yazılmaz.
"${COMPOSE[@]}" exec -T postgres sh -c 'pg_dump --no-owner --clean --if-exists -U "$POSTGRES_USER" "$POSTGRES_DB"' \
    | gzip -9 > "$tmp"

# Boş/bozuk yedek bırakma
gzip -t "$tmp"
[ "$(gzip -dc "$tmp" | head -c 1 | wc -c)" -eq 1 ] || { echo "Yedek boş" >&2; rm -f "$tmp"; exit 1; }
mv "$tmp" "$target"

find "$BACKUP_DIR" -name 'sosyal-*.sql.gz' -type f -mtime "+$KEEP_DAYS" -delete
echo "Yedek alındı: $target ($(du -h "$target" | cut -f1))"
