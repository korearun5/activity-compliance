# Backend Start Plan

## First Sprint

1. Create Spring Boot project with Java 21, Gradle or Maven, PostgreSQL, Flyway, Spring Security, validation, and testcontainers.
2. Add `common` package: response envelope, error codes, global exception handler, request logging, audit context.
3. Add `auth` package: login endpoint, JWT issue/refresh, password hashing, role model.
4. Add database baseline: tenants, users, roles, workflow definitions, workflow tasks, activities, activity tasks, evidence, audit events.
5. Add storage abstraction: local filesystem implementation first, MinIO implementation later.
6. Implement activity tracking: start workflow, get timeline, submit task evidence, advance status.
7. Implement admin reporting query model before PDF/Excel rendering.

## Quality Gates

- Controller tests for API contracts.
- Service tests for workflow/task progression.
- Repository tests for important queries.
- Security tests for role access.
- Upload tests for file type/size validation.
- Audit tests for evidence submission and status changes.

