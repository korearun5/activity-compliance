# Activity Compliance App

Expo React Native frontend and Spring Boot backend for a reusable activity-compliance platform. The first client domain is farmer crop-process tracking, but the base architecture should support future workflows like inspections, field activity, warehouse checks, NGO reporting, dairy operations, and construction progress.

## Current Scope

- Backend-first login for admin, supervisor, and participant roles.
- Configurable backend workflow definitions for crop/activity task lists.
- Backend activity tracking APIs for participant work.
- Backend evidence APIs for proof upload metadata, local storage, and admin review.
- Admin overview for users, workflow definitions, assigned activities, proof records,
  and report-ready metrics.
- FPO module foundation for members, land/plots, crop planning, input demand,
  advisories, and FPO report exports.

## Run Frontend

```powershell
npm install
npm run web
```

The frontend runs at:

```text
http://localhost:19006
```

## Local Users

Default credentials are not displayed in the app and are not hardcoded in the UI. For local development, seed users are configured from `backend/src/main/resources/application-local.yml` or environment variables.

## Scripts

- `npm start`: start Expo.
- `npm run web`: start Expo web.
- `npm run android`: start Android target.
- `npm run ios`: start iOS target.
- `npm run typecheck`: run TypeScript without emitting files.

## Project Structure

- `backend`: Spring Boot backend for auth, workflow, activity, evidence, audit, reporting, storage, and notifications.
- `src/core`: reusable platform contracts, model types, workflow helpers, logging, errors, and storage helpers.
- `src/auth`: Expo auth facade, backend-first with local fallback.
- `src/data`: temporary local prototype stores and client configuration placeholders.
- `src/screens`: current app screens.
- `src/ui`: shared UI components.
- `docs`: architecture, API standards, use cases, QA guide, and developer guide.

## Architecture Notes

Core code should prefer generic names like `User`, `Workflow`, `Activity`, `Task`, `Evidence`, and `Report`. Domain names like farmer, crop, plot, and harvest are allowed at the UI/configuration edge for this client.

## Backend

```powershell
cd backend
docker compose up -d
mvn test
mvn spring-boot:run
```

The backend defaults to Docker PostgreSQL at
`jdbc:postgresql://localhost:5432/activity_platform` for host-side local runs.

## Full Docker Stack

From the repository root:

```powershell
docker compose up --build
```

This starts PostgreSQL, MinIO, the Spring Boot backend, and Expo web. See
[Deployment Guide](docs/deployment-guide.md) for ports, production settings, and
the release checklist. Use `.env.example` for local Docker defaults and
`.env.production.example` as the production environment template.

Frontend login tries the Spring Boot backend at `http://localhost:8080` first.
Public self-signup is disabled; admins and supervisors create participant
accounts. Participant profile display uses `GET /api/v1/users/me` with local
fallback only for development/offline prototype use.

See:

- [Documentation Index](docs/README.md)
- [Clean Start Runbook](docs/clean-start-runbook.md)
- [Project Status And Gap Register](docs/project-status-and-gap-register.md)
- [Architecture](docs/architecture.md)
- [API Standards](docs/api-standards.md)
- [Use Cases](docs/use-cases.md)
- [Developer Guide](docs/developer-guide.md)
- [QA Guide](docs/qa-guide.md)
- [Database Guide](docs/database.md)
- [Client Admin Workflow Guide](docs/client-admin-workflow-guide.md)
- [Roles And Supervisors](docs/roles-and-supervisors.md)
- [Deployment Guide](docs/deployment-guide.md)
- [Backend README](backend/README.md)
