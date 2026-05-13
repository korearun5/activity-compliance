# Security Hardening Assessment

Last audited: 2026-05-13

This assessment summarizes the security posture visible in the current
repository. Use [Deployment Security](DEPLOYMENT_SECURITY.md) for the full
production hardening checklist.

## Implemented

| Control | Status | Evidence |
| ------- | ------ | -------- |
| Secret externalization | Done | Runtime secrets are read from environment variables such as `APP_JWT_SECRET`, `APP_DB_PASSWORD`, and MinIO credentials. |
| Production safety validation | Done | The `prod` profile fails startup for unsafe DB, JWT, CORS, and MinIO settings. |
| CORS configuration | Done | Allowed origins and credentials are configurable through application properties/environment. |
| Password hashing | Done | Spring Security BCrypt password encoder is configured. |
| JWT authentication | Done | Stateless JWT authentication, refresh flow, and current-user endpoint exist. |
| Role-based access | Done | Method/security configuration and role checks protect admin and participant behavior. |
| Input validation | Done | Request DTOs use Jakarta validation and controllers validate request bodies. |
| API error shape | Done | Expected business errors use stable error codes and response envelopes. |
| Audit logging | Done | Important state changes are recorded through the audit event service. |
| Request tracing | Done | `X-Request-Id` support and MDC logging are present. |
| Storage validation | Done | Upload path/key planning and provider abstraction reduce unsafe file handling. |
| Dependency scanning | Done | GitHub security workflow runs npm audit and OWASP Dependency Check. |

## Partially Implemented Or Pending

| Control | Status | Recommendation |
| ------- | ------ | -------------- |
| TLS/HTTPS | Deployment-dependent | Prefer TLS termination at a reverse proxy/load balancer; embedded SSL variables are available when needed. |
| Logout/token revocation | Pending | Add logout/revocation only if product requirements need immediate token invalidation before expiry. |
| Rate limiting | Pending | Add edge or application-level rate limits for login, OTP, and file upload endpoints before production. |
| Security headers | Partial | Backend security headers exist through Spring defaults; confirm final frontend CSP, frame, and HSTS policy in deployment. |
| Frontend automated security checks | Partial | npm audit exists; add UI/E2E checks for access-control regressions. |
| Penetration testing | Pending | Schedule before client production launch. |
| Backup/restore security drill | Pending | Verify database and object-store recovery, access controls, and retention. |

## Security Go-Live Checklist

Before production:

1. Run `npm audit --audit-level=high`.
2. Run `cd backend` then `.\mvnw.cmd -Psecurity-scan -DskipTests verify`.
3. Run backend unit and integration tests.
4. Confirm `SPRING_PROFILES_ACTIVE=prod`.
5. Confirm `APP_SEED_ENABLED=false`.
6. Confirm production DB URL is not localhost.
7. Confirm JWT secret is generated outside the repository and stored in a
   secret manager.
8. Confirm CORS allows only approved HTTPS origins.
9. Confirm MinIO/S3 credentials are stored outside source control.
10. Confirm backups, restore procedure, monitoring, alerting, and audit log
    retention are documented for the target environment.

## Future Security Work

- Add rate limiting for login and future OTP endpoints.
- Add token revocation/logout if required by the client.
- Add role matrix tests for every sensitive controller.
- Add MinIO/S3 integration tests for proof upload and report export storage.
- Add frontend E2E tests that verify disabled modules and restricted screens are
  not reachable by lower-privilege users.
- Schedule external security review before production handoff.
