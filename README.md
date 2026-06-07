# Resume Generator

Resume Generator is a Spring Boot + React resume builder. The current version has moved the main UI to a React/Vite frontend while keeping Spring Boot as the REST API, session authentication, data persistence, and PDF rendering backend.

## Current Architecture

- Frontend: React 18 + Vite, located in `frontend/`.
- Backend: Spring Boot 3.3.x, Java 17, Spring MVC REST APIs.
- Authentication: Spring Security session + cookie.
- Persistence: Spring Data JPA + MySQL 8.
- Database migrations: Flyway versioned SQL migrations.
- Public resume cache: local Caffeine cache by default, optional Redis backend.
- PDF export: Thymeleaf resume templates rendered to HTML, then converted to PDF with OpenHTMLToPDF.
- Production static entry: React build output is served from `/app/`.
- API prefix: backend business APIs stay under `/api/**`.

## Main Features

- User registration, login, logout, and current session check.
- Resume profile editing for basic info, work experience, education, skills, and theme.
- Public share link generation, revocation, password protection, expiration, view limits, and token-based public resume view.
- PDF export based on existing resume templates.
- React frontend for login, register, editor, preview, share link management, and public share page.
- H2-backed MockMvc tests for core API behavior.

## Local Development

Start MySQL:

```bash
docker run --name mysql-standalone -p 6603:3306 -e MYSQL_ROOT_PASSWORD=password -e MYSQL_DATABASE=resume-portal -d mysql
```

The dev profile connects to `localhost:6603` by default. If your MySQL uses another port, set `DB_PORT` or `DB_URL`.

Start backend:

```bash
.\mvnw.cmd spring-boot:run
```

Backend default URL:

```text
http://localhost:5000
```

Install frontend dependencies:

```bash
cd frontend
npm install
```

If the default npm registry is slow, use:

```bash
npm install --registry=https://registry.npmmirror.com
```

Start frontend dev server:

```bash
npm run dev
```

Frontend dev URL:

```text
http://localhost:5173/app/
```

Build frontend into Spring Boot static resources:

```bash
cd frontend
npm run build
```

The build output is written to:

```text
src/main/resources/static/app
```

After building, Spring Boot can serve the React app from:

```text
http://localhost:5000/app/
```

## Configuration

The app now uses Spring profiles:

```text
dev   local development, active by default
test  H2-backed automated tests
prod  production deployment
```

Production should be started with:

```text
SPRING_PROFILES_ACTIVE=prod
```

Common environment variables:

```text
SPRING_PROFILES_ACTIVE
SERVER_PORT
DB_URL
DB_PORT
DB_USERNAME
DB_PASSWORD
CORS_ALLOWED_ORIGINS
SQL_INIT_MODE
DB_POOL_MAX_SIZE
DB_POOL_MIN_IDLE
DB_CONNECTION_TIMEOUT_MS
DB_IDLE_TIMEOUT_MS
DB_MAX_LIFETIME_MS
SESSION_COOKIE_SECURE
APP_SECURITY_REQUIRE_HTTPS
FLYWAY_ENABLED
FLYWAY_BASELINE_ON_MIGRATE
FLYWAY_VALIDATE_ON_MIGRATE
LOG_FILE
ACCESS_LOG_ENABLED
PUBLIC_RESUME_CACHE_ENABLED
PUBLIC_RESUME_CACHE_BACKEND
PUBLIC_RESUME_CACHE_TTL
PUBLIC_RESUME_CACHE_MAX_SIZE
REDIS_HOST
REDIS_PORT
REDIS_TIMEOUT
REDIS_HEALTH_ENABLED
```

Default CORS origins for separated frontend development:

```text
http://localhost:3000
http://localhost:5173
```

Public share cache defaults to local memory:

```text
PUBLIC_RESUME_CACHE_ENABLED=true
PUBLIC_RESUME_CACHE_BACKEND=local
PUBLIC_RESUME_CACHE_TTL=10m
PUBLIC_RESUME_CACHE_MAX_SIZE=10000
```

To use Redis for shared cache across backend instances, start Redis and set:

```text
PUBLIC_RESUME_CACHE_BACKEND=redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_TIMEOUT=1s
REDIS_HEALTH_ENABLED=true
```

See `docs/DEPLOYMENT_PROFILES.md` for production profile details and recommended environment variables.

## Docker Compose

Copy the sample environment file and start the full local stack:

```bash
copy .env.example .env
docker compose up -d --build
```

This starts MySQL, Redis, and the Spring Boot app. The React build is packaged into the backend image and served from:

```text
http://localhost:5000/app/
```

## Main APIs

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
POST /api/auth/logout

GET  /api/profile
PUT  /api/profile
GET  /api/profile/export/pdf

GET  /api/profile/share
POST /api/profile/share/generate
PUT  /api/profile/share/settings
POST /api/profile/share/revoke
GET  /api/public/{shareToken}
POST /api/public/{shareToken}/access
```

## Verification

Backend tests:

```bash
.\mvnw.cmd test
```

Frontend production build:

```bash
cd frontend
npm run build
```

Optional load-test baseline:

```bash
k6 run perf/k6/resume-baseline.js
```

See `docs/PERFORMANCE_BASELINE.md` for the load-test workflow.

CI verification is defined in `.github/workflows/ci.yml`. It runs the frontend build, backend tests, and backend package step.

## Next Iterations

The current priority is to keep the architecture stable before adding heavier high-concurrency infrastructure:

- Keep React as the main UI and Spring Boot as the API backend.
- Use Flyway for all future schema changes instead of relying on Hibernate `ddl-auto=update`.
- Re-run the load-test baseline after enabling Redis-backed public resume cache.
- Add cache hit/miss metrics for `/api/public/{shareToken}`.
- Move high-volume public view counting to Redis or asynchronous batch persistence if share traffic grows.
- Move PDF export to asynchronous jobs only after measuring actual latency and concurrency pressure.

See `docs/CURRENT_STABILIZATION_PLAN.md` and `docs/FEATURE_ROADMAP.md` for details.
