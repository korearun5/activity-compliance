# Clean Start Runbook

Last audited: 2026-05-15

Use this when the local environment feels stale, ports are confused, Docker
volumes contain old data, or a developer wants to restart from a known-good
state.

## Default Local Ports

| Service         | Default                       | Notes                                      |
| --------------- | ----------------------------- | ------------------------------------------ |
| Expo web        | `http://localhost:19006`      | Frontend Docker and local web target.      |
| Expo/Metro      | `http://localhost:8081`       | Bundler/dev tooling port.                  |
| Spring Boot API | `http://localhost:8080`       | `APP_PORT` if an override is required.     |
| PostgreSQL      | `localhost:5432`              | `APP_POSTGRES_PORT` if an override is required. |
| MinIO API       | `http://localhost:9000`       | Object storage API.                        |
| MinIO console   | `http://localhost:9001`       | Local storage console.                     |
| Swagger UI      | `http://localhost:8080/swagger-ui.html` | Available when backend is running. |

If a default port is already used by another local service, override only that
port in a private `.env` file and keep the matching app URL consistent.

## Latest Local Rehearsal

Rehearsed on 2026-05-15 from the repository root on Windows with Docker Desktop:

- `docker --version` returned Docker `28.3.2`.
- `docker info` reached the Docker Desktop engine.
- `docker compose config` rendered successfully with default ports.
- No project default ports were occupied before startup.
- `docker compose up -d postgres minio` started both dependency services.
- `docker compose up -d --build backend frontend` built and started the full
  stack.
- `http://localhost:8080/actuator/health` returned `UP`.
- `http://localhost:19006` returned HTTP `200`.
- Local seed admin login succeeded and `/api/v1/platform/modules/enabled`
  returned the expected Phase 1 modules.
- PostgreSQL accepted:

```powershell
docker compose exec -T postgres psql "postgresql://activity_app:activity_app@localhost:5432/activity_platform" -c "select current_user, current_database();"
```

Expected output includes `activity_app` and `activity_platform`. This confirms
the earlier `password authentication failed for user "activity_app"` issue is
resolved when the local compose PostgreSQL owns port `5432` and uses the
default volume credentials.

## Stop Everything

From the repository root:

```powershell
docker compose down --remove-orphans
```

If the backend-only database compose was started from `backend/`, stop it too:

```powershell
cd backend
docker compose down --remove-orphans
cd ..
```

Stop any foreground terminals running these commands with `Ctrl+C`:

```text
npm run web
npm start
.\mvnw.cmd spring-boot:run
```

Optional port check:

```powershell
Get-NetTCPConnection -LocalPort 19006,8081,8080,5432,9000,9001 -ErrorAction SilentlyContinue |
  Select-Object LocalAddress,LocalPort,State,OwningProcess
```

Only stop a process by port after confirming it belongs to this project:

```powershell
Stop-Process -Id <PID> -Force
```

## Clean Local Data

Remove Docker volumes when you want a truly fresh database and object store:

```powershell
docker compose down -v --remove-orphans
cd backend
docker compose down -v --remove-orphans
cd ..
```

Remove generated local artifacts:

```powershell
Remove-Item -Recurse -Force node_modules -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force .expo -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force backend\target -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force backend\storage -ErrorAction SilentlyContinue
Remove-Item -Force expo-web*.log -ErrorAction SilentlyContinue
```

Keep `.env.example` and `.env.production.example`. Private `.env` files are
ignored by Git and can be recreated from `.env.example` when needed.

## Local PostgreSQL Volume Safety

Both the root Docker Compose stack and `backend/compose.yaml` must point to the
same local PostgreSQL Docker volume:

```text
backend_activity-platform-postgres-data
```

This volume is persistent across normal Docker restarts. Data is removed only
when the volume is deleted, for example with `docker compose down -v`, Docker
Desktop volume cleanup, or a manual volume delete.

If Carbon or FPO records appear to vanish after a restart, first check that only
one project PostgreSQL container is running and that it is using the shared
volume:

```powershell
docker ps -a
docker volume ls
docker compose exec -T postgres psql "postgresql://activity_app:activity_app@localhost:5432/activity_platform" -c "select count(*) from carbon_profiles;"
```

Do not start an older stopped PostgreSQL container from Docker Desktop by hand.
Use one of the compose commands in this runbook so the container is recreated
with the shared volume.

## Fresh Start: Full Docker Stack

From the repository root:

```powershell
npm ci
docker compose config
docker compose up --build
```

Expected services:

- Frontend: `http://localhost:19006`
- Backend health: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- MinIO console: `http://localhost:9001`

## Fresh Start: Split Local Development

Use this when you want Docker for dependencies but local terminals for frontend
and backend development.

Start dependencies:

```powershell
docker compose up -d postgres minio
```

Start backend:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="local"
.\mvnw.cmd spring-boot:run
```

Start frontend from another terminal:

```powershell
npm ci
npm run web
```

## Verification Checklist

Run before declaring the environment clean:

```powershell
npm run typecheck
npm run lint
npm audit --audit-level=moderate
cd backend
.\mvnw.cmd test
.\mvnw.cmd -Pintegration-test verify
```

The integration profile uses JUnit, Spring MockMvc, Flyway, and PostgreSQL
Testcontainers. Docker must be available for that command.

Latest local result on 2026-05-15:

- `npm run typecheck`: passed.
- `npm run lint`: passed.
- `npm audit --audit-level=moderate`: passed with zero vulnerabilities.
- `.\mvnw.cmd test`: passed with 40 unit tests.
- `.\mvnw.cmd -Pintegration-test verify`: passed with 87 integration tests.
- `.\mvnw.cmd "-Dtest=FpoPhase1UatSmokeIT" test`: passed.
- Full Docker stack: backend health `UP`, frontend HTTP `200`.

## When Ports Conflict

Prefer keeping defaults. If a conflict is unavoidable, create or edit a private
`.env` file and override the smallest possible surface:

```text
APP_POSTGRES_PORT=<free-postgres-port>
APP_DB_URL=jdbc:postgresql://localhost:<free-postgres-port>/activity_platform
APP_PORT=<free-api-port>
EXPO_PUBLIC_API_BASE_URL=http://localhost:<free-api-port>
APP_CORS_ALLOWED_ORIGIN_WEB=http://localhost:19006
```

After changing ports, restart the affected services and rerun the verification
checklist.

## PostgreSQL Password Failure After Port Cleanup

If the backend fails with:

```text
FATAL: password authentication failed for user "activity_app"
```

first check whether another PostgreSQL is already answering on the default
port:

```powershell
Get-NetTCPConnection -LocalPort 5432 -ErrorAction SilentlyContinue |
  Select-Object LocalAddress,LocalPort,State,OwningProcess
Get-Process -Id <OwningProcess>
```

If the process is not this project's Docker container, choose one path:

Use default port `5432`:

```powershell
docker compose down --remove-orphans
# Stop or disable the other local PostgreSQL only if it is safe for your machine.
docker compose up -d postgres
cd backend
$env:SPRING_PROFILES_ACTIVE="local"
Remove-Item Env:APP_DB_URL -ErrorAction SilentlyContinue
.\mvnw.cmd spring-boot:run
```

Keep the other PostgreSQL running and use a project-only override:

```powershell
docker compose down --remove-orphans
$env:APP_POSTGRES_PORT="55432"
docker compose up -d postgres
cd backend
$env:SPRING_PROFILES_ACTIVE="local"
$env:APP_DB_URL="jdbc:postgresql://localhost:55432/activity_platform"
.\mvnw.cmd spring-boot:run
```
