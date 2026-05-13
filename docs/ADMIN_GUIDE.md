# Activity Platform Administrator Guide

Last audited: 2026-05-13

This guide is for administrators who operate the platform after it is running.
For setup, reset, deployment, and security hardening, use the canonical
runbooks:

- [Clean Start Runbook](clean-start-runbook.md)
- [Developer Guide](developer-guide.md)
- [Deployment Guide](deployment-guide.md)
- [Deployment Security](DEPLOYMENT_SECURITY.md)
- [Project Status And Gap Register](project-status-and-gap-register.md)

## Platform Overview

The Activity Platform supports reusable activity-compliance workflows with a
current FPO/agriculture module. Core capabilities include:

- Tenant-aware login and user management.
- Admin, supervisor, and participant roles.
- Configurable workflows and activity task tracking.
- Evidence upload and review.
- Audit logging.
- Report summary and export foundations.
- FPO member, land/plot, crop planning, input demand, advisory, and report
  export workflows.
- Module subscriptions for enabling or hiding product areas by tenant.

## Local Admin URLs

| Area | URL |
| ---- | --- |
| Frontend | `http://localhost:19006` |
| Backend health | `http://localhost:8080/actuator/health` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| MinIO console | `http://localhost:9001` |

Default local ports are documented in
[Clean Start Runbook](clean-start-runbook.md).

## User And Role Management

Roles:

- `ADMIN`: full administrative access.
- `SUPERVISOR`: operational management access for assigned workflows/users.
- `PARTICIPANT`: field/farmer-facing access.

Admin tasks:

1. Create users from the admin interface or user API.
2. Assign roles using the role management screen/API.
3. Activate or deactivate users instead of deleting historical records.
4. Use tenant-specific module subscriptions to control which product areas are
   available.

Rules:

- Do not share seed credentials outside local development.
- Do not store real passwords, tokens, or client personal data in docs,
  screenshots, commits, or issue comments.
- Use test prefixes such as `qa-*` for local QA data.

## FPO Administration

Current admin-facing FPO areas:

- Member profiles and status management.
- Landholding and farm plot records with GPS fields.
- Crop catalog, seasons, crop history, and seasonal crop plans.
- Input catalog, crop input rules, demand calculations, and demand summaries.
- Advisory creation and status management.
- FPO report summary and Excel export foundation.

Before production handoff, confirm these client-owned items:

- Final member, land, plot, crop, input, and advisory data dictionary.
- Final input formula units, buffers, and rounding rules.
- Final Excel/PDF report template and columns.
- UAT scenarios and acceptance owners.

Track open gaps in
[Project Status And Gap Register](project-status-and-gap-register.md).

## Routine Operations

Health check:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"
```

Start local full stack:

```powershell
docker compose up --build
```

Stop local full stack:

```powershell
docker compose down --remove-orphans
```

Reset local data:

```powershell
docker compose down -v --remove-orphans
```

Run verification:

```powershell
npm run typecheck
npm run lint
cd backend
.\mvnw.cmd test
.\mvnw.cmd -Pintegration-test verify
```

## Backup And Recovery

Production backup policy is target-environment specific and must be finalized
before go-live.

Minimum requirements:

- Scheduled PostgreSQL backups.
- Scheduled object storage backups or lifecycle/replication policy.
- Tested restore procedure.
- Documented retention period.
- Access restricted to approved operators.
- Restore drill completed before launch.

Local development data can be recreated from migrations and seed settings, so
local Docker volumes are disposable.

## Monitoring And Audit

Monitor at minimum:

- Backend health endpoint.
- API error rate and latency.
- Database CPU, memory, storage, connections, and replication/backup status.
- MinIO/S3 storage availability and capacity.
- Authentication failures.
- Report export failures.
- Audit log growth and retention.

Every support ticket should include:

- Environment.
- Build/branch.
- User role.
- Tenant.
- Approximate time.
- Request ID if available.
- Steps to reproduce.
- Expected and actual result.

## Troubleshooting

API will not start:

- Confirm PostgreSQL is running.
- Confirm `APP_DB_URL`, username, and password match the active environment.
- If `activity_app` authentication fails on `localhost:5432`, another local
  PostgreSQL may be answering on the default port. Use
  [Clean Start Runbook](clean-start-runbook.md#postgresql-password-failure-after-port-cleanup).
- Confirm port `8080` is free or set `APP_PORT`.
- Check Flyway migration errors in backend logs.

Frontend cannot login:

- Confirm backend health is `UP`.
- Confirm `EXPO_PUBLIC_API_BASE_URL` points at the backend origin.
- Confirm CORS allows the frontend origin.
- Confirm the user exists, is active, and has the expected tenant/roles.

Database looks stale:

- For local development, follow [Clean Start Runbook](clean-start-runbook.md).
- For production, do not reset data. Use backup/restore and migration runbooks.

Uploads or report exports fail:

- Confirm storage provider settings.
- Confirm MinIO/S3 credentials and bucket.
- Confirm upload size limits.
- Check backend logs using the request ID.

## Handoff Checklist

Before client production handoff:

- Current QA suite passes.
- UAT catalog is signed off.
- Production environment variables are stored in a secret manager.
- Backups and restore are tested.
- Monitoring and alerting are live.
- First admin/supervisor accounts are created through an approved process.
- Client workflow definitions and FPO master data are loaded.
- Report export output is verified against approved templates.
- Open risks are recorded in
  [Project Status And Gap Register](project-status-and-gap-register.md).
