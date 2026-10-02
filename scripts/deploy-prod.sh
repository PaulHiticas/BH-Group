#!/usr/bin/env bash
# Conservative production deploy for bhstays.ro.
#
# Run this ON THE PRODUCTION SERVER, from the repository root:
#
#   ./scripts/deploy-prod.sh
#
# It refuses to do anything until it is satisfied that it is on the right
# host and that the production .env is complete, takes a database backup
# before touching the running stack, and stops at the first failure instead
# of pressing on. Nothing here uses --force.
#
# WHAT IT DOES, IN ORDER
#   1. Identifies the host and refuses to continue if this machine does not
#      serve bhstays.ro (override only with CONFIRM_PROD=yes).
#   2. Reports what is running and which commit the working tree is on.
#   3. Backs up the database; aborts if the backup fails.
#   4. Validates the production .env. Prints SET or MISSING per variable and
#      never prints a value. Aborts if anything critical is missing or still
#      holds a development placeholder.
#   5. git fetch + git pull --ff-only origin main (aborts on a dirty tree).
#   6. docker compose up -d --build, waits for health, then verifies
#      containers, /actuator/health, and what the live site serves.
#
# IF IT ABORTS the running site is untouched: every check that can fail runs
# before the rebuild, except the post-deploy verification, which tells you to
# roll back and prints the backup path.
#
# ROLLBACK (the backup path is printed at step 3):
#   git -c advice.detachedHead=false checkout <previous-commit>
#   docker compose up -d --build
#   gunzip -c backups/<file>.sql.gz | docker exec -i bhstays-postgres psql -U <user> -d <db>

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."
REPO_DIR="$PWD"

DOMAIN="${DEPLOY_DOMAIN:-bhstays.ro}"
HEALTH_URL="${HEALTH_URL:-http://localhost:8080/actuator/health}"
SITE_URL="${SITE_URL:-https://${DOMAIN}/}"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-180}"

step()  { printf '\n\033[1m== %s\033[0m\n' "$*"; }
ok()    { printf '   \033[32mOK\033[0m      %s\n' "$*"; }
info()  { printf '   ·       %s\n' "$*"; }
warn()  { printf '   \033[33mATENTIE\033[0m %s\n' "$*"; }
die()   { printf '\n\033[31mOPRIT:\033[0m %s\n\n' "$*" >&2; exit 1; }

compose() { docker compose "$@"; }

# ---------------------------------------------------------------------------
# 1. Where are we?
# ---------------------------------------------------------------------------
step "1. Identificare host"

info "hostname:  $(hostname)"
info "sistem:    $(uname -srm 2>/dev/null || echo necunoscut)"
info "repo:      $REPO_DIR"

MY_IPS="$(hostname -I 2>/dev/null || true)"
PUBLIC_IP="$(curl -fsS -m 10 https://api.ipify.org 2>/dev/null || true)"
DOMAIN_IP="$(getent hosts "$DOMAIN" 2>/dev/null | awk '{print $1; exit}' || true)"

info "IP public: ${PUBLIC_IP:-necunoscut}"
info "$DOMAIN -> ${DOMAIN_IP:-nerezolvat}"

on_prod=no
for ip in $MY_IPS $PUBLIC_IP; do
    [ -n "$DOMAIN_IP" ] && [ "$ip" = "$DOMAIN_IP" ] && on_prod=yes
done

if [ "$on_prod" = yes ]; then
    ok "Aceasta masina serveste $DOMAIN."
elif [ "${CONFIRM_PROD:-}" = yes ]; then
    warn "Nu pot confirma ca aceasta masina serveste $DOMAIN, dar CONFIRM_PROD=yes."
    warn "Daca nu esti pe serverul de productie, opreste acum cu Ctrl-C."
    sleep 5
else
    die "Aceasta masina NU pare sa serveasca $DOMAIN (IP public ${PUBLIC_IP:-?} vs ${DOMAIN_IP:-?}).
       Un deploy de aici ar reconstrui alt stack si ar raporta succes degeaba.
       Daca stii sigur ca e serverul corect (de ex. e in spatele unui proxy/CDN),
       reia cu:  CONFIRM_PROD=yes ./scripts/deploy-prod.sh"
fi

# ---------------------------------------------------------------------------
# 2. What is running right now?
# ---------------------------------------------------------------------------
step "2. Ce ruleaza acum"

compose ps
echo
RUNNING_COMMIT="$(git rev-parse HEAD)"
info "commit in working tree: $(git rev-parse --short HEAD) $(git log -1 --format='%s')"
info "branch:                 $(git rev-parse --abbrev-ref HEAD)"
warn "Containerele pot fi construite dintr-un commit mai vechi decat acesta;"
warn "data din coloana CREATED de mai sus arata cand au fost construite."

# ---------------------------------------------------------------------------
# 3. Backup first
# ---------------------------------------------------------------------------
step "3. Backup baza de date (inainte de orice modificare)"

[ -f .env ] || die "Nu exista .env in $REPO_DIR - nu pot determina baza de date."

# backup-db.sh reads POSTGRES_USER/POSTGRES_DB from the environment and would
# otherwise fall back to the dev defaults, dumping the wrong database on an
# environment created before the rebrand. Values are read, never printed.
set -a
# shellcheck disable=SC1091
. ./.env
set +a

if [ -x scripts/backup-db.sh ]; then
    scripts/backup-db.sh || die "Backup-ul a esuat - nu continui fara plasa de siguranta."
else
    bash scripts/backup-db.sh || die "Backup-ul a esuat - nu continui fara plasa de siguranta."
fi

BACKUP_FILE="$(find ./backups -name '*.sql.gz' -newermt '-10 minutes' -print 2>/dev/null | sort | tail -1 || true)"
[ -n "$BACKUP_FILE" ] || die "Nu gasesc niciun backup proaspat in ./backups - opresc."
ok "Backup: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"

# ---------------------------------------------------------------------------
# 4. Validate the production .env
# ---------------------------------------------------------------------------
step "4. Validare .env de productie (fara a afisa valori)"

missing=()
placeholder=()
wrongval=()

# Reads the raw file rather than the environment, so a variable exported in the
# shell cannot mask one absent from .env - compose only reads the file.
envval() { sed -n "s/^[[:space:]]*$1=//p" .env | tail -1; }

require_set() {
    local name="$1" val
    val="$(envval "$name")"
    if [ -z "$val" ]; then
        missing+=("$name"); printf '   %-26s \033[31mLIPSA\033[0m\n' "$name"; return
    fi
    case "$val" in
        replace-me*|change-me*|*your-*|*TODO*|secret|password)
            placeholder+=("$name"); printf '   %-26s \033[31mPLACEHOLDER DEV\033[0m\n' "$name"; return;;
    esac
    printf '   %-26s \033[32mSETAT\033[0m\n' "$name"
}

require_exact() {
    local name="$1" want="$2" val
    val="$(envval "$name")"
    if [ -z "$val" ]; then
        missing+=("$name"); printf '   %-26s \033[31mLIPSA\033[0m (asteptat: %s)\n' "$name" "$want"
    elif [ "$val" != "$want" ]; then
        wrongval+=("$name"); printf '   %-26s \033[31mVALOARE GRESITA\033[0m (asteptat: %s)\n' "$name" "$want"
    else
        printf '   %-26s \033[32mOK\033[0m (%s)\n' "$name" "$want"
    fi
}

require_contains() {
    local name="$1" want="$2" val
    val="$(envval "$name")"
    if [ -z "$val" ]; then
        missing+=("$name"); printf '   %-26s \033[31mLIPSA\033[0m (trebuie sa contina: %s)\n' "$name" "$want"
    elif ! printf '%s' "$val" | grep -qF "$want"; then
        wrongval+=("$name"); printf '   %-26s \033[31mNU CONTINE %s\033[0m\n' "$name" "$want"
    else
        printf '   %-26s \033[32mOK\033[0m (contine %s)\n' "$name" "$want"
    fi
}

echo "   -- Secrete (doar SETAT/LIPSA, niciodata valoarea) --"
require_set JWT_SECRET
require_set POSTGRES_PASSWORD
require_set ANTHROPIC_API_KEY
require_set MAIL_USERNAME
require_set MAIL_PASSWORD
require_set STRIPE_SECRET_KEY
require_set STRIPE_PUBLISHABLE_KEY
require_set STRIPE_WEBHOOK_SECRET

echo "   -- Configurare cu valori fixe --"
require_exact MAIL_HOST              "smtp-relay.brevo.com"
require_exact MAIL_FROM              "no-reply@bhstays.ro"
require_exact APP_BASE_URL           "https://bhstays.ro"
require_exact REFRESH_COOKIE_SECURE  "true"
require_exact SPRING_PROFILES_ACTIVE "prod"
require_contains CORS_ALLOWED_ORIGINS "https://bhstays.ro"

echo "   -- Recomandate pentru HTTPS (nu blocheaza deploy-ul) --"
for n in MAIL_PORT NEXT_PUBLIC_API_BASE_URL UPLOAD_PUBLIC_BASE_URL; do
    if [ -n "$(envval "$n")" ]; then printf '   %-26s \033[32mSETAT\033[0m\n' "$n"
    else printf '   %-26s \033[33mLIPSA\033[0m (se foloseste default-ul de dev)\n' "$n"; fi
done

# The flag is only honoured if compose forwards it into the backend container.
if ! grep -q 'REFRESH_COOKIE_SECURE' docker-compose.yml; then
    die "docker-compose.yml nu paseaza REFRESH_COOKIE_SECURE in containerul backend.
       Fara linia aceasta, REFRESH_COOKIE_SECURE=true din .env NU are efect si
       cookie-ul de refresh pleaca fara Secure pe HTTPS. Adauga in serviciul backend:
         REFRESH_COOKIE_SECURE: \${REFRESH_COOKIE_SECURE:-false}"
fi
ok "docker-compose.yml paseaza REFRESH_COOKIE_SECURE catre backend."

if [ ${#missing[@]} -gt 0 ] || [ ${#placeholder[@]} -gt 0 ] || [ ${#wrongval[@]} -gt 0 ]; then
    echo
    [ ${#missing[@]}     -gt 0 ] && echo "   LIPSA:            ${missing[*]}"
    [ ${#placeholder[@]} -gt 0 ] && echo "   PLACEHOLDER DEV:  ${placeholder[*]}"
    [ ${#wrongval[@]}    -gt 0 ] && echo "   VALOARE GRESITA:  ${wrongval[*]}"
    die "Env incomplet - NU deployez. Completeaza .env si reia. Backup-ul este deja facut:
       $BACKUP_FILE"
fi
ok "Toate variabilele critice sunt prezente si corecte."

compose config --quiet || die "docker compose config invalid - opresc."
ok "docker compose config valid."

# ---------------------------------------------------------------------------
# 5. Bring the code up to date
# ---------------------------------------------------------------------------
step "5. Aducere cod la zi (main)"

[ -z "$(git status --porcelain)" ] || die "Working tree murdar pe server. Nu suprascriu modificari locale:
$(git status --short)"

git fetch origin || die "git fetch a esuat."
BRANCH="$(git rev-parse --abbrev-ref HEAD)"
[ "$BRANCH" = main ] || die "Esti pe '$BRANCH', nu pe main. Comut manual, nu automat."

git pull --ff-only origin main || die "git pull --ff-only a esuat (istoric divergent?). Nu fortez."
NEW_COMMIT="$(git rev-parse HEAD)"

if [ "$NEW_COMMIT" = "$RUNNING_COMMIT" ]; then
    info "Codul era deja la zi ($(git rev-parse --short HEAD)); continui cu rebuild."
else
    ok "Actualizat: $(git rev-parse --short "$RUNNING_COMMIT") -> $(git rev-parse --short "$NEW_COMMIT")"
    git --no-pager log --oneline "$RUNNING_COMMIT..$NEW_COMMIT" | sed 's/^/     /'
fi

# ---------------------------------------------------------------------------
# 6. Rebuild and verify
# ---------------------------------------------------------------------------
step "6. Rebuild si pornire"

compose up -d --build || die "docker compose up --build a esuat. Site-ul poate rula inca imaginile vechi.
       Verifica 'docker compose ps' si 'docker compose logs --tail=100'.
       Backup: $BACKUP_FILE"

step "7. Verificare dupa deploy"

info "Astept /actuator/health (maxim ${HEALTH_TIMEOUT}s)..."
healthy=no
for _ in $(seq 1 "$HEALTH_TIMEOUT"); do
    if [ "$(curl -fsS -o /dev/null -w '%{http_code}' -m 5 "$HEALTH_URL" 2>/dev/null || echo 000)" = 200 ]; then
        healthy=yes; break
    fi
    sleep 1
done
[ "$healthy" = yes ] || die "/actuator/health nu a raspuns 200 in ${HEALTH_TIMEOUT}s.
$(compose ps)
       Loguri:  docker compose logs --tail=100 backend
       Backup:  $BACKUP_FILE"
ok "/actuator/health = 200"

compose ps
for svc in postgres backend frontend; do
    cid="$(compose ps -q "$svc" 2>/dev/null || true)"
    [ -n "$cid" ] || die "Serviciul '$svc' nu are container. Backup: $BACKUP_FILE"
    state="$(docker inspect -f '{{.State.Status}}' "$cid")"
    restarts="$(docker inspect -f '{{.RestartCount}}' "$cid")"
    [ "$state" = running ] || die "'$svc' este '$state', nu 'running'. Backup: $BACKUP_FILE"
    if [ "$restarts" -gt 2 ]; then
        die "'$svc' a repornit $restarts ori - restart loop.
       docker compose logs --tail=100 $svc
       Backup: $BACKUP_FILE"
    fi
    ok "$svc: running (reporniri: $restarts)"
done

code="$(curl -fsS -o /tmp/deploy-live.html -w '%{http_code}' -m 20 "$SITE_URL" 2>/dev/null || echo 000)"
[ "$code" = 200 ] || die "$SITE_URL a raspuns $code. Backup: $BACKUP_FILE"
if grep -qi "BH Stays" /tmp/deploy-live.html; then
    ok "$SITE_URL = 200 si serveste 'BH Stays'"
    info "title: $(grep -oiE '<title>[^<]*</title>' /tmp/deploy-live.html | head -1)"
else
    die "$SITE_URL raspunde 200 dar nu contine 'BH Stays' - posibil cache sau build vechi.
       Backup: $BACKUP_FILE"
fi
rm -f /tmp/deploy-live.html

step "Deploy reusit"
ok "commit live: $(git rev-parse --short HEAD) $(git log -1 --format='%s')"
ok "backup:      $BACKUP_FILE"
echo
echo "   Login-ul trebuie verificat manual in browser pe https://${DOMAIN}/login"
echo "   (scriptul nu stocheaza credentiale si nu se autentifica singur)."
echo
