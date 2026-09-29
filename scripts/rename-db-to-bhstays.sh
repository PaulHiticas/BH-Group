#!/usr/bin/env bash
# One-time migration: rename the database and role from the old bhgroup_pms /
# bhgroup names to bhstays_pms / bhstays.
#
# WHY THIS SCRIPT EXISTS
# ----------------------
# POSTGRES_DB and POSTGRES_USER in docker-compose.yml only do anything the
# first time Postgres initialises an empty data directory. On a deployment
# that already has data, editing those values does NOT rename anything - the
# app simply starts pointing at a database and role that do not exist, and
# every request fails. This script performs the actual rename.
#
# Run it ONCE per environment that was created before the rebrand. A fresh
# environment (empty volume) needs nothing: compose creates the new names
# directly.
#
# USAGE
#   ./scripts/rename-db-to-bhstays.sh
#
# It stops the app containers first (the rename needs no other session
# connected to the database), renames, then leaves the containers stopped so
# you can start them again with the new configuration:
#   docker compose up -d
#
# SAFETY
#   - Takes a full dump before touching anything; the path is printed and the
#     script aborts if the dump fails.
#   - Exits successfully and changes nothing if the old names are already gone
#     (so re-running it is harmless).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKUP_DIR="${PROJECT_DIR}/backups"

# POSTGRES_PASSWORD is read from .env (if present) so the role password can be
# re-applied after the rename; it is never printed.
if [ -f "${PROJECT_DIR}/.env" ]; then
    POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-$(grep -E '^POSTGRES_PASSWORD=' "${PROJECT_DIR}/.env" | head -1 | cut -d= -f2-)}"
fi

OLD_DB="${OLD_DB:-bhgroup_pms}"
OLD_USER="${OLD_USER:-bhgroup}"
NEW_DB="${NEW_DB:-bhstays_pms}"
NEW_USER="${NEW_USER:-bhstays}"

# The container is still running under its pre-rebrand name at this point,
# because the rename has to happen before the new compose file is applied.
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-bhgroup-postgres}"

log() { printf '%s %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }

if ! docker ps --format '{{.Names}}' | grep -qx "${POSTGRES_CONTAINER}"; then
    log "ERROR: container '${POSTGRES_CONTAINER}' is not running."
    log "       Start it first, or set POSTGRES_CONTAINER=<name>."
    exit 1
fi

# psql connects to the maintenance database: you cannot rename a database you
# are currently connected to.
psql_postgres() {
    docker exec -i "${POSTGRES_CONTAINER}" psql -U "${OLD_USER}" -d postgres -tAc "$1"
}

db_exists() {
    [ "$(psql_postgres "SELECT 1 FROM pg_database WHERE datname='$1'")" = "1" ]
}

role_exists() {
    [ "$(psql_postgres "SELECT 1 FROM pg_roles WHERE rolname='$1'")" = "1" ]
}

if ! db_exists "${OLD_DB}" && ! role_exists "${OLD_USER}"; then
    log "Nothing to do: '${OLD_DB}' and '${OLD_USER}' no longer exist (already renamed)."
    exit 0
fi

if db_exists "${OLD_DB}"; then
    mkdir -p "${BACKUP_DIR}"
    DUMP_FILE="${BACKUP_DIR}/pre-rebrand_${OLD_DB}_$(date '+%Y-%m-%d_%H%M').sql.gz"
    log "Backing up '${OLD_DB}' to ${DUMP_FILE} ..."
    if ! docker exec "${POSTGRES_CONTAINER}" pg_dump -U "${OLD_USER}" -d "${OLD_DB}" | gzip > "${DUMP_FILE}"; then
        log "ERROR: backup failed - aborting without changing anything."
        rm -f "${DUMP_FILE}"
        exit 1
    fi
    log "Backup written ($(du -h "${DUMP_FILE}" | cut -f1))."
else
    log "Database '${OLD_DB}' already renamed - skipping backup."
fi

log "Stopping app containers so nothing holds a connection open ..."
(cd "${PROJECT_DIR}" && docker compose stop backend frontend >/dev/null 2>&1) || true

# Any leftover session (a psql shell, a crashed app) would make ALTER DATABASE
# fail with "database is being accessed by other users".
log "Terminating remaining connections to '${OLD_DB}' ..."
psql_postgres "SELECT pg_terminate_backend(pid) FROM pg_stat_activity
               WHERE datname='${OLD_DB}' AND pid <> pg_backend_pid()" >/dev/null

if db_exists "${OLD_DB}"; then
    log "Renaming database ${OLD_DB} -> ${NEW_DB} ..."
    psql_postgres "ALTER DATABASE \"${OLD_DB}\" RENAME TO \"${NEW_DB}\"" >/dev/null
else
    log "Database '${OLD_DB}' already renamed - skipping."
fi

if role_exists "${OLD_USER}"; then
    log "Renaming role ${OLD_USER} -> ${NEW_USER} ..."
    # Postgres refuses to rename the role the current session is logged in as
    # ("session user cannot be renamed"), and this image has no second
    # superuser - POSTGRES_USER is the only one. So borrow a throwaway
    # superuser for the single statement, then drop it again.
    TMP_ROLE="rename_tmp_$$"
    TMP_PASS="$(head -c 18 /dev/urandom | base64 | tr -dc 'A-Za-z0-9')"

    psql_postgres "CREATE ROLE \"${TMP_ROLE}\" LOGIN SUPERUSER PASSWORD '${TMP_PASS}'" >/dev/null
    docker exec -i -e PGPASSWORD="${TMP_PASS}" "${POSTGRES_CONTAINER}" \
        psql -U "${TMP_ROLE}" -d postgres -tAc \
        "ALTER ROLE \"${OLD_USER}\" RENAME TO \"${NEW_USER}\"" >/dev/null

    # Renaming a role clears an MD5-hashed password (the hash is salted with
    # the role name), so set it again from the configured value, then clean up.
    if [ -n "${POSTGRES_PASSWORD:-}" ]; then
        docker exec -i -e PGPASSWORD="${TMP_PASS}" "${POSTGRES_CONTAINER}" \
            psql -U "${TMP_ROLE}" -d postgres -tAc \
            "ALTER ROLE \"${NEW_USER}\" WITH PASSWORD '${POSTGRES_PASSWORD}'" >/dev/null
        log "Password for '${NEW_USER}' re-applied from POSTGRES_PASSWORD."
    else
        log "WARNING: POSTGRES_PASSWORD not exported, so the role password was left as-is."
        log "         If the app cannot log in, set it manually:"
        log "         docker exec -i ${POSTGRES_CONTAINER} psql -U ${NEW_USER} -d postgres \\"
        log "           -c \"ALTER ROLE ${NEW_USER} WITH PASSWORD '<POSTGRES_PASSWORD>'\""
    fi

    docker exec -i -e PGPASSWORD="${TMP_PASS}" "${POSTGRES_CONTAINER}" \
        psql -U "${TMP_ROLE}" -d postgres -tAc "DROP ROLE \"${TMP_ROLE}\"" >/dev/null 2>&1 \
        || docker exec -i "${POSTGRES_CONTAINER}" psql -U "${NEW_USER}" -d postgres -tAc \
             "DROP ROLE IF EXISTS \"${TMP_ROLE}\"" >/dev/null
    log "Temporary superuser removed."
else
    log "Role '${OLD_USER}' already renamed - skipping."
fi

log "Done. Now bring the stack up on the new names:"
log "    docker compose up -d --build"
