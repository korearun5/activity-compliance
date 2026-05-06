# Component And Data Model Diagrams

This document gives a visual map of the current platform. It is meant for
handoff, onboarding, debugging, and deciding where a future feature belongs.

## Component Architecture

```mermaid
flowchart TB
    subgraph Clients["Client Layer"]
        AdminUser["Admin"]
        SupervisorUser["Supervisor"]
        ParticipantUser["Participant / Farmer"]
    end

    subgraph ExpoApp["Expo React Native App"]
        Login["Login Screen"]
        AdminScreens["Admin Screens"]
        ParticipantScreens["Participant Screens"]
        AuthFacade["Auth Facade"]
        ApiClient["API Client"]
        Stores["Frontend Data Stores"]
        LocalCache["AsyncStorage Prototype Cache"]
    end

    subgraph BackendApi["Spring Boot REST API"]
        Security["Security Filter Chain / JWT"]
        AuthController["Auth Controller"]
        UserController["User Controller"]
        RoleController["Role Controller"]
        WorkflowController["Workflow Controller"]
        ActivityController["Activity Controller"]
        EvidenceController["Evidence Controller"]
        ReportController["Report Controller"]
        NotificationController["Notification Controller"]
        ApiEnvelope["ApiResponse / PageResponse / Error Handling"]
    end

    subgraph Services["Backend Service Layer"]
        AuthService["Auth Service"]
        UserService["User Service"]
        RoleService["Role Service"]
        WorkflowService["Workflow Definition Service"]
        ActivityService["Activity Service"]
        EvidenceService["Evidence Service"]
        ReportService["Reporting Service"]
        NotificationService["Notification Service"]
        AuditService["Audit Service"]
        StorageService["FileStorageService"]
    end

    subgraph Persistence["Persistence And Infrastructure"]
        Repositories["Spring Data JPA Repositories"]
        Postgres["PostgreSQL + Flyway"]
        StorageAdapter["Storage Adapter"]
        LocalDisk["Local Disk"]
        Minio["MinIO / S3 Compatible Storage"]
    end

    AdminUser --> AdminScreens
    SupervisorUser --> AdminScreens
    ParticipantUser --> ParticipantScreens
    Login --> AuthFacade
    AdminScreens --> Stores
    ParticipantScreens --> Stores
    Stores --> ApiClient
    Stores -. development fallback .-> LocalCache
    AuthFacade --> ApiClient
    ApiClient --> Security
    Security --> ApiEnvelope
    ApiEnvelope --> AuthController
    ApiEnvelope --> UserController
    ApiEnvelope --> RoleController
    ApiEnvelope --> WorkflowController
    ApiEnvelope --> ActivityController
    ApiEnvelope --> EvidenceController
    ApiEnvelope --> ReportController
    ApiEnvelope --> NotificationController

    AuthController --> AuthService
    UserController --> UserService
    RoleController --> RoleService
    WorkflowController --> WorkflowService
    ActivityController --> ActivityService
    EvidenceController --> EvidenceService
    ReportController --> ReportService
    NotificationController --> NotificationService

    AuthService --> Repositories
    UserService --> Repositories
    RoleService --> Repositories
    WorkflowService --> Repositories
    ActivityService --> Repositories
    EvidenceService --> Repositories
    ReportService --> Repositories
    NotificationService --> Repositories

    UserService --> AuditService
    RoleService --> AuditService
    WorkflowService --> AuditService
    ActivityService --> AuditService
    EvidenceService --> AuditService
    ReportService --> AuditService
    NotificationService --> AuditService
    AuditService --> Repositories

    EvidenceService --> StorageService
    ReportService --> StorageService
    StorageService --> StorageAdapter
    StorageAdapter --> LocalDisk
    StorageAdapter --> Minio
    Repositories --> Postgres
```

## Backend Module Components

```mermaid
flowchart LR
    Common["common\nAPI envelope, paging, errors, tracing"]
    Security["security\nJWT, CORS, method security"]
    Auth["auth\nlogin, refresh, tenant, base user/role entities"]
    User["user\nadmin participant profile management"]
    Role["role\nrole catalog and role assignment"]
    Workflow["workflow\nworkflow definitions and task templates"]
    Activity["activity\nworkflow execution and task status"]
    Evidence["evidence\nproof metadata, upload, review"]
    Storage["storage\nvalidation, object key planning, local/MinIO"]
    Reporting["reporting\nsummary, PDF export, XLSX export"]
    Notification["notification\nnotification event/status foundation"]
    Audit["audit\nappend-only compliance event trail"]

    Common --> Auth
    Common --> User
    Common --> Role
    Common --> Workflow
    Common --> Activity
    Common --> Evidence
    Common --> Reporting
    Common --> Notification
    Security --> Auth
    Security --> User
    Security --> Role
    Security --> Workflow
    Security --> Activity
    Security --> Evidence
    Security --> Reporting
    Security --> Notification
    User --> Auth
    Role --> Auth
    Workflow --> Audit
    Activity --> Workflow
    Activity --> User
    Activity --> Audit
    Evidence --> Activity
    Evidence --> Storage
    Evidence --> Audit
    Reporting --> Activity
    Reporting --> Evidence
    Reporting --> Storage
    Reporting --> Audit
    Notification --> User
    Notification --> Audit
```

## Deployment Components

```mermaid
flowchart TB
    Browser["Browser / Expo Web\nlocalhost:19006"]
    Mobile["Expo Mobile Runtime"]
    Backend["Spring Boot Backend\nlocalhost:8080"]
    DbHost["PostgreSQL Docker Host Port\nlocalhost:55432"]
    DbContainer["PostgreSQL Container\npostgres:5432"]
    MinioApi["MinIO API\nlocalhost:9000"]
    MinioConsole["MinIO Console\nlocalhost:9001"]
    LocalFiles["Local File Storage\nbackend/storage/local"]

    Browser --> Backend
    Mobile --> Backend
    Backend --> DbHost
    DbHost --> DbContainer
    Backend --> MinioApi
    MinioApi --> MinioConsole
    Backend -. local provider .-> LocalFiles
```

## Database Class Diagram: Full Model

This is the whole durable model at a glance. Class names match JPA entities and
comments show the backing PostgreSQL table.

```mermaid
classDiagram
    class TenantEntity {
        table tenants
        UUID id PK
        String code UNIQUE
        String name
        String status
        Instant createdAt
        Instant updatedAt
    }

    class UserEntity {
        table users
        UUID id PK
        UUID tenantId FK
        String username
        String passwordHash
        String displayName
        String phone
        String locationName
        String siteName
        String status
        Instant createdAt
        Instant updatedAt
    }

    class RoleEntity {
        table roles
        UUID id PK
        UUID tenantId FK
        String code
        String name
        Instant createdAt
    }

    class UserRole {
        table user_roles
        UUID userId PK, FK
        UUID roleId PK, FK
        Instant createdAt
    }

    class WorkflowDefinitionEntity {
        table workflow_definitions
        UUID id PK
        UUID tenantId FK
        String code
        String name
        String domainKey
        int durationDays
        int version
        WorkflowDefinitionStatus status
        Instant createdAt
        Instant updatedAt
    }

    class WorkflowTaskEntity {
        table workflow_tasks
        UUID id PK
        UUID workflowDefinitionId FK
        String code
        String title
        int sequenceNumber
        int offsetDays
        boolean evidenceRequired
        Instant createdAt
    }

    class ActivityEntity {
        table activities
        UUID id PK
        UUID tenantId FK
        UUID workflowDefinitionId FK
        UUID participantUserId FK
        String unitName
        String locationName
        ActivityStatus status
        LocalDate startedOn
        LocalDate expectedCompletion
        Instant completedAt
        int progressPercent
        Instant createdAt
        Instant updatedAt
    }

    class ActivityTaskEntity {
        table activity_tasks
        UUID id PK
        UUID activityId FK
        UUID workflowTaskId FK
        TaskStatus status
        LocalDate dueOn
        Instant completedAt
        Instant createdAt
        Instant updatedAt
    }

    class EvidenceEntity {
        table evidence
        UUID id PK
        UUID tenantId FK
        UUID activityTaskId FK
        UUID participantUserId FK
        String storageKey
        String originalFilename
        String contentType
        long sizeBytes
        String note
        EvidenceStatus status
        Instant submittedAt
        UUID reviewedByUserId FK
        Instant reviewedAt
    }

    class AuditEventEntity {
        table audit_events
        UUID id PK
        UUID tenantId FK
        UUID actorUserId FK
        String aggregateType
        UUID aggregateId
        AuditAction action
        JSON metadata
        String requestId
        Instant occurredAt
    }

    class ReportExportEntity {
        table report_exports
        UUID id PK
        UUID tenantId FK
        UUID requestedByUserId FK
        String reportType
        ReportFormat format
        JSON filterJson
        String storageKey
        ReportStatus status
        Instant requestedAt
        Instant completedAt
    }

    class NotificationEventEntity {
        table notification_events
        UUID id PK
        UUID tenantId FK
        UUID recipientUserId FK
        NotificationChannel channel
        String templateCode
        JSON payload
        NotificationStatus status
        Instant queuedAt
        Instant sentAt
    }

    TenantEntity "1" --> "0..*" UserEntity : owns
    TenantEntity "1" --> "0..*" RoleEntity : owns
    UserEntity "1" --> "0..*" UserRole : has
    RoleEntity "1" --> "0..*" UserRole : assigned
    TenantEntity "1" --> "0..*" WorkflowDefinitionEntity : owns
    WorkflowDefinitionEntity "1" --> "1..*" WorkflowTaskEntity : contains
    TenantEntity "1" --> "0..*" ActivityEntity : owns
    WorkflowDefinitionEntity "1" --> "0..*" ActivityEntity : instantiates
    UserEntity "0..1" --> "0..*" ActivityEntity : participant
    ActivityEntity "1" --> "1..*" ActivityTaskEntity : contains
    WorkflowTaskEntity "1" --> "0..*" ActivityTaskEntity : template
    ActivityTaskEntity "1" --> "0..*" EvidenceEntity : proof
    UserEntity "0..1" --> "0..*" EvidenceEntity : participant
    UserEntity "0..1" --> "0..*" EvidenceEntity : reviewer
    TenantEntity "1" --> "0..*" AuditEventEntity : owns
    UserEntity "0..1" --> "0..*" AuditEventEntity : actor
    TenantEntity "1" --> "0..*" ReportExportEntity : owns
    UserEntity "0..1" --> "0..*" ReportExportEntity : requestedBy
    TenantEntity "1" --> "0..*" NotificationEventEntity : owns
    UserEntity "0..1" --> "0..*" NotificationEventEntity : recipient
```

## Identity And Access Tables

```mermaid
classDiagram
    class tenants {
        UUID id PK
        varchar code UNIQUE
        varchar name
        varchar status
        timestamptz created_at
        timestamptz updated_at
    }

    class users {
        UUID id PK
        UUID tenant_id FK
        varchar username
        varchar password_hash
        varchar display_name
        varchar phone
        varchar location_name
        varchar site_name
        varchar status
        timestamptz created_at
        timestamptz updated_at
    }

    class roles {
        UUID id PK
        UUID tenant_id FK
        varchar code
        varchar name
        timestamptz created_at
    }

    class user_roles {
        UUID user_id PK, FK
        UUID role_id PK, FK
        timestamptz created_at
    }

    tenants "1" --> "0..*" users : tenant_id
    tenants "1" --> "0..*" roles : tenant_id
    users "1" --> "0..*" user_roles : user_id
    roles "1" --> "0..*" user_roles : role_id
```

Important constraints:

- `users`: unique `(tenant_id, username)`.
- `roles`: unique `(tenant_id, code)`.
- `user_roles`: composite primary key `(user_id, role_id)`.
- Current role codes: `ADMIN`, `SUPERVISOR`, `PARTICIPANT`.

## Workflow Definition Tables

```mermaid
classDiagram
    class workflow_definitions {
        UUID id PK
        UUID tenant_id FK
        varchar code
        varchar name
        varchar domain_key
        int duration_days
        int version
        varchar status
        timestamptz created_at
        timestamptz updated_at
    }

    class workflow_tasks {
        UUID id PK
        UUID workflow_definition_id FK
        varchar code
        varchar title
        int sequence_number
        int offset_days
        boolean evidence_required
        timestamptz created_at
    }

    workflow_definitions "1" --> "1..*" workflow_tasks : workflow_definition_id
```

Important constraints:

- `workflow_definitions`: unique `(tenant_id, code, version)`.
- `workflow_tasks`: unique `(workflow_definition_id, code)`.
- `workflow_tasks`: unique `(workflow_definition_id, sequence_number)`.
- Definition status values: `DRAFT`, `ACTIVE`, `ARCHIVED`.

## Activity Execution Tables

```mermaid
classDiagram
    class activities {
        UUID id PK
        UUID tenant_id FK
        UUID workflow_definition_id FK
        UUID participant_user_id FK
        varchar unit_name
        varchar location_name
        varchar status
        date started_on
        date expected_completion
        timestamptz completed_at
        int progress_percent
        timestamptz created_at
        timestamptz updated_at
    }

    class activity_tasks {
        UUID id PK
        UUID activity_id FK
        UUID workflow_task_id FK
        varchar status
        date due_on
        timestamptz completed_at
        timestamptz created_at
        timestamptz updated_at
    }

    class workflow_definitions {
        UUID id PK
    }

    class workflow_tasks {
        UUID id PK
    }

    class users {
        UUID id PK
    }

    workflow_definitions "1" --> "0..*" activities : workflow_definition_id
    users "0..1" --> "0..*" activities : participant_user_id
    activities "1" --> "1..*" activity_tasks : activity_id
    workflow_tasks "1" --> "0..*" activity_tasks : workflow_task_id
```

Important constraints:

- `activity_tasks`: unique `(activity_id, workflow_task_id)`.
- Activity status values: `RUNNING`, `COMPLETED`, `CANCELLED`.
- Task status values: `PENDING`, `NEXT`, `DONE`, `SKIPPED`.
- `activities.progress_percent` is derived from activity task completion.

## Evidence Review Tables

```mermaid
classDiagram
    class evidence {
        UUID id PK
        UUID tenant_id FK
        UUID activity_task_id FK
        UUID participant_user_id FK
        text storage_key
        varchar original_filename
        varchar content_type
        bigint size_bytes
        text note
        varchar status
        timestamptz submitted_at
        UUID reviewed_by_user_id FK
        timestamptz reviewed_at
    }

    class tenants {
        UUID id PK
    }

    class activity_tasks {
        UUID id PK
    }

    class users {
        UUID id PK
    }

    tenants "1" --> "0..*" evidence : tenant_id
    activity_tasks "1" --> "0..*" evidence : activity_task_id
    users "0..1" --> "0..*" evidence : participant_user_id
    users "0..1" --> "0..*" evidence : reviewed_by_user_id
```

Important rules:

- File bytes live in the configured storage provider; `evidence` stores metadata
  and the stable storage key.
- Evidence status values: `SUBMITTED`, `PENDING_REVIEW`, `APPROVED`,
  `REJECTED`.
- Reviewer fields are nullable until an admin/supervisor reviews the proof.

## Audit, Report, And Notification Tables

```mermaid
classDiagram
    class audit_events {
        UUID id PK
        UUID tenant_id FK
        UUID actor_user_id FK
        varchar aggregate_type
        UUID aggregate_id
        varchar action
        jsonb metadata
        varchar request_id
        timestamptz occurred_at
    }

    class report_exports {
        UUID id PK
        UUID tenant_id FK
        UUID requested_by_user_id FK
        varchar report_type
        varchar format
        jsonb filter_json
        text storage_key
        varchar status
        timestamptz requested_at
        timestamptz completed_at
    }

    class notification_events {
        UUID id PK
        UUID tenant_id FK
        UUID recipient_user_id FK
        varchar channel
        varchar template_code
        jsonb payload
        varchar status
        timestamptz queued_at
        timestamptz sent_at
    }

    class tenants {
        UUID id PK
    }

    class users {
        UUID id PK
    }

    tenants "1" --> "0..*" audit_events : tenant_id
    users "0..1" --> "0..*" audit_events : actor_user_id
    tenants "1" --> "0..*" report_exports : tenant_id
    users "0..1" --> "0..*" report_exports : requested_by_user_id
    tenants "1" --> "0..*" notification_events : tenant_id
    users "0..1" --> "0..*" notification_events : recipient_user_id
```

Important rules:

- `audit_events` is append-only application history.
- `report_exports.storage_key` points to generated PDF/XLSX output.
- Report formats: `PDF`, `XLSX`.
- Report statuses: `QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`.
- Notification channels: `IN_APP`, `EMAIL`, `SMS`, `PUSH`.
- Notification statuses: `QUEUED`, `SENT`, `FAILED`, `SKIPPED`.

## Table To Module Ownership

| Table | Owner Module | Main API Surface |
| --- | --- | --- |
| `tenants` | `auth` | Seed/platform setup |
| `users` | `auth`, `user` | `/api/v1/auth/*`, `/api/v1/users/*` |
| `roles` | `auth`, `role` | `/api/v1/roles`, `/api/v1/users/{id}/roles` |
| `user_roles` | `role` | `/api/v1/users/{id}/roles` |
| `workflow_definitions` | `workflow` | `/api/v1/workflows` |
| `workflow_tasks` | `workflow` | `/api/v1/workflows` |
| `activities` | `activity` | `/api/v1/activities` |
| `activity_tasks` | `activity` | `/api/v1/activities/{id}/tasks/{taskId}/status` |
| `evidence` | `evidence` | `/api/v1/evidence` |
| `audit_events` | `audit` | Internal service writes |
| `report_exports` | `reporting` | `/api/v1/reports/export` |
| `notification_events` | `notification` | `/api/v1/notifications` |

## Storage Object Key Shape

Database rows never store local absolute paths. Evidence and report rows store a
provider-neutral object key.

```mermaid
flowchart LR
    EvidenceRow["evidence.storage_key"]
    ReportRow["report_exports.storage_key"]
    FileStorage["FileStorageService"]
    Planner["StorageFilePlanner"]
    Local["LocalFileStorageService"]
    Minio["MinioFileStorageService"]

    EvidenceRow --> FileStorage
    ReportRow --> FileStorage
    FileStorage --> Planner
    Planner --> Local
    Planner --> Minio
```

Object keys are planned from the same reusable structure for local and MinIO
providers:

```text
tenant/{tenantId}/{ownerType}/{ownerId}/{uuid}-{safeFilename}
```

