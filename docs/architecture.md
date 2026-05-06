# Architecture Guide

## Purpose

This project is a reusable activity-compliance platform. Agriculture is the
first client domain, but the core architecture should also support warehouse
inspection, field worker tracking, dairy operations, factory audit workflows,
NGO field activity tracking, and construction progress tracking.

The main design rule is:

```text
Generic core + client-specific configuration
```

Shared backend and frontend code should use platform language:

- `Tenant`: one client organization.
- `User`: admin, supervisor, participant, farmer, field worker, or inspector.
- `Workflow`: configurable process definition.
- `Task`: one ordered workflow step.
- `Activity`: one execution of a workflow.
- `Evidence`: photo, file, note, or proof metadata for a task.
- `AuditEvent`: immutable record of important system actions.
- `Report`: generated dashboard, PDF, or Excel output.

Agriculture words such as farmer, crop, plot, harvest, village, and government
report belong in UI copy, seed data, workflow configuration, and exported report
labels. They should not be hardcoded into the reusable core.

## System Context

For the deeper component-by-component view and database class diagrams, see
[Component And Data Model Diagrams](component-and-data-model-diagrams.md).

```mermaid
flowchart LR
    Admin["Admin / Supervisor"] --> App["Expo React Native App"]
    Participant["Participant / Farmer"] --> App
    App --> Api["Spring Boot REST API"]
    Api --> Db["PostgreSQL"]
    Api --> Storage["File Storage Adapter"]
    Storage --> LocalDisk["Local Disk (Development)"]
    Storage --> Minio["MinIO / S3 Compatible Storage"]
    Api --> Reports["PDF / Excel Report Generator"]
    Api --> Notifications["Notification Status Tracker"]
```

## Runtime Architecture

```mermaid
flowchart TB
    subgraph Frontend["Expo React Native + TypeScript"]
        Screens["Screens"]
        AuthFacade["Auth Facade"]
        ApiClient["API Client"]
        LocalFallback["Local Prototype Storage"]
    end

    subgraph Backend["Spring Boot Backend"]
        Security["Security + JWT"]
        Auth["Auth Module"]
        User["User Module"]
        RoleMgmt["Role Management Module"]
        Workflow["Workflow Module"]
        Activity["Activity Module"]
        Evidence["Evidence Module"]
        StorageSvc["Storage Service Interface"]
        Audit["Audit Module"]
        Reporting["Reporting Module"]
        Notification["Notification Module"]
        Common["Common API / Errors / Logging"]
    end

    Screens --> AuthFacade
    Screens --> ApiClient
    AuthFacade --> ApiClient
    AuthFacade -. temporary fallback .-> LocalFallback
    ApiClient --> Security
    Security --> Auth
    Security --> User
    Security --> RoleMgmt
    Security --> Workflow
    Security --> Activity
    Security --> Evidence
    Security --> Notification
    User --> Audit
    RoleMgmt --> User
    RoleMgmt --> Audit
    Workflow --> Audit
    Activity --> Audit
    Evidence --> Audit
    Evidence --> StorageSvc
    Reporting --> Activity
    Reporting --> Evidence
    Notification --> Audit
    Notification --> User
```

## Backend Module Boundaries

```mermaid
flowchart LR
    Common["common"] --> Auth["auth"]
    Common --> User["user"]
    Common --> RoleMgmt["role"]
    Common --> Workflow["workflow"]
    Common --> Activity["activity"]
    Common --> Evidence["evidence"]
    Common --> Notification["notification"]
    Security["security"] --> Auth
    Security --> User
    Security --> RoleMgmt
    Security --> Notification
    User --> Audit["audit"]
    RoleMgmt --> User
    RoleMgmt --> Audit
    Workflow --> Audit
    Activity --> Audit
    Evidence --> Audit
    Activity --> Workflow
    Activity --> User
    Evidence --> Activity
    Evidence --> Storage["storage"]
    Reporting["reporting"] --> Activity
    Reporting --> Evidence
    Notification --> User
    Notification --> Audit
```

Module responsibilities:

- `common`: API envelope, page response, exception handling, request tracing.
- `security`: Spring Security, JWT resource server, role conversion.
- `auth`: login, refresh, current user, seed users, tenant/user/role entities.
- `user`: admin profile management for participant/farmer users.
- `role`: tenant role catalog and admin-controlled user role assignment.
- `workflow`: reusable workflow definitions and task templates.
- `activity`: workflow execution, participant timeline, task status.
- `evidence`: proof upload metadata and evidence review status.
- `storage`: shared file validation/key planning with local disk for dev/test and MinIO/S3-compatible storage for production.
- `audit`: append-only compliance trail.
- `reporting`: tenant-scoped summary metrics plus PDF and XLSX exports built from activity/evidence data.
- `notification`: notification status and future delivery framework.

## Request Flow

```mermaid
sequenceDiagram
    participant UI as Expo App
    participant API as Spring Boot API
    participant SEC as Spring Security
    participant SVC as Service Layer
    participant DB as PostgreSQL
    participant AUD as Audit Event

    UI->>API: REST request with Bearer token
    API->>SEC: Validate JWT and roles
    SEC-->>API: Current user context
    API->>SVC: Validated request DTO
    SVC->>DB: Read/write tenant-scoped data
    SVC->>AUD: Record state-changing action
    SVC-->>API: Response DTO
    API-->>UI: ApiResponse<T>
```

## Core Data Model

```mermaid
erDiagram
    TENANTS ||--o{ USERS : owns
    TENANTS ||--o{ ROLES : owns
    USERS }o--o{ ROLES : has
    TENANTS ||--o{ WORKFLOW_DEFINITIONS : owns
    WORKFLOW_DEFINITIONS ||--o{ WORKFLOW_TASKS : contains
    TENANTS ||--o{ ACTIVITIES : owns
    WORKFLOW_DEFINITIONS ||--o{ ACTIVITIES : instantiates
    USERS ||--o{ ACTIVITIES : participant
    ACTIVITIES ||--o{ ACTIVITY_TASKS : contains
    WORKFLOW_TASKS ||--o{ ACTIVITY_TASKS : template
    ACTIVITY_TASKS ||--o{ EVIDENCE : has
    TENANTS ||--o{ AUDIT_EVENTS : owns
    USERS ||--o{ AUDIT_EVENTS : actor
    TENANTS ||--o{ REPORT_EXPORTS : owns
    TENANTS ||--o{ NOTIFICATION_EVENTS : owns
```

## Roles

Current platform roles:

- `ADMIN`: full tenant administration, user/profile creation, workflow setup.
- `SUPERVISOR`: operational management and review.
- `PARTICIPANT`: field/farmer user who executes activities and uploads proof.

Role checks are enforced at controller and service boundaries. JWT roles are
mapped to Spring authorities as `ROLE_ADMIN`, `ROLE_SUPERVISOR`, and
`ROLE_PARTICIPANT`.

## Key Workflows

### Admin Creates Participant

```mermaid
sequenceDiagram
    participant Admin
    participant UI as Admin Dashboard
    participant API as User API
    participant DB as PostgreSQL
    participant AUD as Audit

    Admin->>UI: Enter participant profile
    UI->>API: POST /api/v1/users
    API->>DB: Create tenant-scoped user
    API->>DB: Assign PARTICIPANT role
    API->>AUD: USER_CREATED
    API-->>UI: UserResponse
```

### Participant Loads Own Profile

```mermaid
sequenceDiagram
    participant Participant
    participant UI as Participant App
    participant API as User API
    participant DB as PostgreSQL

    Participant->>UI: Open profile tab
    UI->>API: GET /api/v1/users/me
    API->>DB: Load current tenant-scoped user
    API-->>UI: UserResponse with profile fields
```

### Participant Tracks Activity

```mermaid
sequenceDiagram
    participant Participant
    participant UI as Participant App
    participant API as Activity API
    participant DB as PostgreSQL
    participant AUD as Audit

    Participant->>UI: Start workflow activity
    UI->>API: POST /api/v1/activities
    API->>DB: Create activity and task timeline
    API->>AUD: ACTIVITY_CREATED
    Participant->>UI: Mark task / submit proof
    UI->>API: POST /api/v1/evidence
    API->>DB: Store evidence metadata
    API->>AUD: EVIDENCE_SUBMITTED
```

## Frontend Architecture

Frontend folders:

- `src/auth`: login facade, backend auth, session storage.
- `src/core/api`: API client, endpoint registry, response contracts.
- `src/core/config`: environment-style app constants.
- `src/core/errors`: frontend error model.
- `src/core/model`: reusable frontend types.
- `src/core/storage`: AsyncStorage JSON helpers.
- `src/core/workflow`: frontend workflow helpers.
- `src/data`: local prototype stores and agriculture configuration.
- `src/screens`: app screens.
- `src/ui`: shared UI components.

The frontend is backend-first for login, admin participant management,
workflow/activity timelines, proof upload, evidence review, reports, role
management, notifications, and participant profile display. Local storage
fallback remains only for development/offline prototype use.

## Backend Architecture

Backend package root:

```text
com.activityplatform.backend
```

Controller responsibilities:

- Map HTTP endpoints.
- Validate request DTOs.
- Return `ApiResponse<T>`.
- Avoid business rules.

Service responsibilities:

- Own transaction boundaries.
- Enforce business rules.
- Enforce tenant access.
- Emit audit events for state changes.

Repository responsibilities:

- Persist entities.
- Express tenant-scoped queries.
- Avoid business decisions.

Entity responsibilities:

- Represent durable state.
- Provide narrow mutation methods for meaningful state changes.

## Storage Architecture

Current storage abstraction:

```text
FileStorageService
```

Current implementations:

```text
LocalFileStorageService
MinioFileStorageService
```

The evidence and reporting modules depend on the interface, not on the local
filesystem or MinIO SDK. Provider selection is handled by `app.storage.provider`.

## API Contract Standards

All backend responses use the envelope:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "meta": null
}
```

Paged responses use:

```json
{
  "content": [],
  "page": {
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

Expected errors use `ApplicationException` and return stable error codes. The
backend never returns stack traces in API responses.

## Local Development Topology

```mermaid
flowchart LR
    Browser["Browser / Expo Web :19006"] --> Backend["Spring Boot :8080"]
    Backend --> Postgres["PostgreSQL Container :5432 / Host :55432"]
    Backend --> Files["./backend/storage/local or MinIO"]
    Backend --> Minio["MinIO Container :9000"]
```

Default local ports:

- Expo web: `19006`
- Spring Boot: `8080`
- PostgreSQL host port: `55432`
- MinIO API: `9000`
- MinIO console: `9001`

These ports can change as long as the frontend API base URL and backend CORS
settings are updated together.

## Design Rules For Reuse

- Keep workflows data-driven in database tables.
- Do not hardcode crop lifecycle stages in Java or TypeScript.
- Keep participant/farmer as a role/use case, not as the core user model.
- Keep storage behind an interface.
- Keep reports generated from reusable activity/evidence data.
- Add tenant ids to durable business tables.
- Keep audit events append-only.
- Prefer clear module boundaries over premature framework abstraction.
