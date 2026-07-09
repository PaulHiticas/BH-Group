# BH Group — Property Management Platform

Platformă de Property Management pentru închirieri pe termen scurt (Airbnb, Booking.com
și rezervări directe), construită pentru scalare internațională.

## Stack tehnologic

**Frontend**: Next.js 15 (App Router), React 19, TypeScript, Tailwind CSS v4, shadcn/ui,
Framer Motion, TanStack React Query, React Hook Form + Zod, Zustand.

**Backend**: Java 21, Spring Boot 3, Spring Security, JWT + Refresh Token, PostgreSQL,
Flyway, MapStruct, Maven, Docker, springdoc-openapi (Swagger).

**Arhitectură**: Clean Architecture / layered (entity → repository → service →
controller), DTO pattern, global exception handling, audit logging, environment-based
configuration, Docker Compose.

## Status: Etapa 1 — Fundație & Autentificare

Implementat în această etapă:

- Structură monorepo (`backend/`, `frontend/`), Docker Compose, configurare `.env`
- Schema PostgreSQL (Flyway): `users`, `refresh_tokens`, `verification_tokens`, `audit_logs`
- Spring Security + JWT (access token + refresh token cu rotație), roluri (`SUPER_ADMIN`,
  `ADMINISTRATOR`, `OWNER`, `EMPLOYEE`, `CLEANER`, `MAINTENANCE`, `GUEST`)
- Autentificare completă: register, login, refresh, logout, verificare email, resend
  verificare, forgot/reset password, autentificare în doi pași (TOTP / 2FA)
- Audit logging pentru evenimentele de autentificare
- Global exception handler + envelope de răspuns standard (`ApiResponse` / `ApiError`)
- Swagger UI la `/swagger-ui.html`
- Frontend: pagini de autentificare (login, register, forgot/reset password, verify
  email, verificare 2FA), dashboard minimal protejat, dark mode, temă premium
  (Stripe/Linear/Notion inspired)

Modulele de business (Property Management, Rezervări, Curățenie, Mentenanță, Owner/
Employee Portal, Booking Engine, Plăți, Chat, AI, Mobile) urmează în etapele următoare,
conform planului de implementare.

## Rulare locală

### Cu Docker Compose (recomandat)

```bash
cp .env.example .env
# editează .env și completează SUPER_ADMIN_EMAIL / SUPER_ADMIN_PASSWORD, JWT_SECRET, MAIL_*
docker compose up --build
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api/v1
- Swagger UI: http://localhost:8080/swagger-ui.html

La primul start, dacă `SUPER_ADMIN_EMAIL` / `SUPER_ADMIN_PASSWORD` sunt setate și nu
există încă niciun cont `SUPER_ADMIN`, backend-ul creează automat primul cont de
administrator al platformei (înregistrarea publică creează doar conturi `GUEST`).

### Rulare separată (dezvoltare)

**Backend**:

```bash
cd backend
./mvnw spring-boot:run
```

Necesită o instanță PostgreSQL locală (vezi `docker-compose.yml` pentru variabilele de
mediu așteptate) sau `docker compose up postgres`.

**Frontend**:

```bash
cd frontend
cp .env.local.example .env.local
npm install
npm run dev
```

## Structura proiectului

```
backend/    Spring Boot API (Java 21, Maven)
frontend/   Next.js 15 App Router (TypeScript)
docs/       Documentație tehnică
```

## Note de securitate

- Parolele sunt hash-uite cu BCrypt (cost factor 12)
- Refresh token-urile sunt rotite la fiecare folosire și stocate hash-uit (SHA-256) în
  baza de date, nu în clar
- 2FA folosește TOTP (RFC 6238), compatibil cu Google Authenticator / Authy
- Toate secretele (JWT, DB, SMTP) se configurează exclusiv prin variabile de mediu —
  nu există secrete hardcodate în cod
