#!/usr/bin/env bash
# Nightly PostgreSQL backup for the BH Group PMS database.
#
# Run this from the same host as docker-compose.yml (e.g. via a cron job)
# so it can reach the `bhgroup-postgres` container. Dumps are compressed and
# kept for BACKUP_RETENTION_DAYS days, then pruned automatically.
#
# Usage:
#   ./scripts/backup-db.sh
#
# Suggested crontab entry (daily at 03:00 server time):
#   0 3 * * * cd /path/to/BH-Group && ./scripts/backup-db.sh >> /var/log/bhgroup-backup.log 2>&1
#
# To restore from a backup:
#   gunzip -c backups/bhgroup_pms_2026-07-12_0300.sql.gz | \
#     docker exec -i bhgroup-postgres psql -U bhgroup -d bhgroup_pms

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

BACKUP_DIR="${BACKUP_DIR:-./backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
POSTGRES_USER="${POSTGRES_USER:-bhgroup}"
POSTGRES_DB="${POSTGRES_DB:-bhgroup_pms}"
CONTAINER_NAME="${POSTGRES_CONTAINER:-bhgroup-postgres}"
TIMESTAMP="$(date +%Y-%m-%d_%H%M)"
OUT_FILE="$BACKUP_DIR/${POSTGRES_DB}_${TIMESTAMP}.sql.gz"

mkdir -p "$BACKUP_DIR"

echo "[$(date)] Starting backup of $POSTGRES_DB from $CONTAINER_NAME..."
docker exec "$CONTAINER_NAME" pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --clean --if-exists \
    | gzip > "$OUT_FILE"

if [ ! -s "$OUT_FILE" ]; then
    echo "[$(date)] ERROR: backup file is empty, something went wrong." >&2
    rm -f "$OUT_FILE"
    exit 1
fi

echo "[$(date)] Backup written to $OUT_FILE ($(du -h "$OUT_FILE" | cut -f1))"

echo "[$(date)] Pruning backups older than $RETENTION_DAYS days..."
find "$BACKUP_DIR" -name "${POSTGRES_DB}_*.sql.gz" -mtime "+$RETENTION_DAYS" -delete

echo "[$(date)] Done. $(find "$BACKUP_DIR" -name "${POSTGRES_DB}_*.sql.gz" | wc -l) backup(s) retained."
