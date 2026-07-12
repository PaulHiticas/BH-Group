# Production readiness

Status as of 2026-07-12. Hosting target not yet decided — the items below are
grouped by what's already done vs. what depends on that decision.

## Already in place

**Security**
- Passwords hashed with BCrypt (cost 12); refresh tokens rotated on every use
  and stored hashed (SHA-256), never in clear text.
- JWT access + refresh tokens, TOTP-based 2FA (RFC 6238) for staff accounts.
- Role-based access control enforced on every endpoint (`@PreAuthorize`),
  verified via a scripted matrix across all roles this session.
- Rate limiting filter on authentication endpoints.
- IDOR protection: cross-tenant lookups (owner viewing another owner's
  property, etc.) return 404, not 403, so existence isn't leaked.
- All secrets (DB, JWT, SMTP) come from environment variables — nothing
  hardcoded in the repo.
- CORS restricted to the configured frontend origin.

**CI**
- `.github/workflows/ci.yml` already runs on every push/PR to `main`:
  backend (`mvn verify` — now runs the 51-test suite added this session
  against a real Postgres service container) and frontend (typecheck, lint,
  production build). Both verified passing locally with the current code.

**Backups**
- `scripts/backup-db.sh` — nightly `pg_dump` (compressed, `--clean --if-exists`
  so it restores cleanly over an existing DB), with automatic pruning after
  `BACKUP_RETENTION_DAYS` (default 14). Tested against the running stack.
  Not yet wired to a cron job or an off-site destination — see below.

**Operational basics already in docker-compose.yml**
- `restart: unless-stopped` on every service.
- Postgres healthcheck gating backend startup.
- Named volume for Postgres data (`pgdata`) and uploads, so a container
  recreate doesn't lose data.

## Deferred — needs a hosting decision first

- **Where it runs.** Not decided yet. Once there's a VPS or cloud target,
  the docker-compose setup here deploys as-is; the main additions needed at
  that point are a reverse proxy (Caddy/Nginx) for TLS and a domain.
- **Off-site backups.** The backup script currently writes to local disk
  only. What most small SaaS/PMS operators do once they have a real server:
  cron the script nightly, then sync the `backups/` folder to an
  S3-compatible bucket (Backblaze B2 and Hetzner Object Storage are the
  cheap options — a few cents/month for a few GB) with `rclone` or `aws s3
  sync`, and periodically test that a downloaded backup actually restores
  (`gunzip -c backup.sql.gz | psql ...` into a scratch DB — a backup nobody's
  restored is not a verified backup). Worth doing once there's a server to
  cron it on.
- **Observability / error tracking.** Nothing wired up yet. Cheapest
  reasonable stack once hosted: Sentry (free tier) for both backend and
  frontend error tracking, plus the Spring Boot Actuator endpoints already
  exposed (`/actuator/health`) hooked into a external uptime pinger
  (UptimeRobot free tier, or similar). Full metrics (Grafana/Prometheus) is
  overkill for the current scale — revisit if traffic grows.
- **CD (auto-deploy on merge).** Not set up — depends entirely on where the
  app ends up hosted (a VPS would need a deploy step added to `ci.yml` that
  SSHes in and runs `docker compose up -d --build`; a managed platform would
  use its own deploy hook instead).

## What to test yourself before considering this production-ready

See the final message in this conversation for the concrete manual QA
checklist covering every feature built this session.
