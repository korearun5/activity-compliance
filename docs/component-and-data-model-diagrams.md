# Component And Data Model Diagrams

This document gives a visual map of the current platform. It is meant for
handoff, onboarding, debugging, and deciding where a future feature belongs.

## Component Architecture

```mermaid
flowchart TB
    subgraph Clients["Client Layer"]
        AdminUser["Admin"]
        FPO_MANAGERUser["FPO_MANAGER"]
        FIELD_COORDINATORUser["FIELD_COORDINATOR / Farmer"]
    end

    subgraph ExpoApp["Expo React Native App"]
        Login["Login Screen"]
        AdminScreens["Admin Screens"]
        FIELD_COORDINATORScreens["FIELD_COORDINATOR Screens"]
        AuthFacade["Auth Facade"]
        ApiClient["API Client"]
        Stores["Frontend Data Stores"]
        LocalCache["AsyncStorage Prototype Cache"]
    end

    subgraph BackendApi["Spring Boot REST API"]
        Security["Security Filter Chain / JWT"]
        AuthController["Auth Controller"]
        PlatformController["Platform Controller"]
        UserController["User Controller"]
        RoleController["Role Controller"]
        FpoMemberController["FPO Member Controller"]
        FarmAssetController["Farm Asset Controller"]
        CropPlanningController["Crop Planning Controller"]
        InputDemandController["Input Demand Controller"]
        FpoReportController["FPO Report Controller"]
        WorkflowController["Workflow Controller"]
        ActivityController["Activity Controller"]
        EvidenceController["Evidence Controller"]
        ReportController["Report Controller"]
        NotificationController["Notification Controller"]
        ApiEnvelope["ApiResponse / PageResponse / Error Handling"]
    end

    subgraph Services["Backend Service Layer"]
        AuthService["Auth Service"]
        TenantModuleService["Tenant Module Service"]
        UserService["User Service"]
        RoleService["Role Service"]
        FpoMemberService["FPO Member Service"]
        FarmAssetService["Farm Asset Service"]
        CropPlanningService["Crop Planning Service"]
        InputDemandService["Input Demand Service"]
        FpoDashboardSummaryService["FPO Dashboard Summary Service"]
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
    FPO_MANAGERUser --> AdminScreens
    FIELD_COORDINATORUser --> FIELD_COORDINATORScreens
    Login --> AuthFacade
    AdminScreens --> Stores
    FIELD_COORDINATORScreens --> Stores
    Stores --> ApiClient
    Stores -. development fallback .-> LocalCache
    AuthFacade --> ApiClient
    ApiClient --> Security
    Security --> ApiEnvelope
    ApiEnvelope --> AuthController
    ApiEnvelope --> PlatformController
    ApiEnvelope --> UserController
    ApiEnvelope --> RoleController
    ApiEnvelope --> FpoMemberController
    ApiEnvelope --> FarmAssetController
    ApiEnvelope --> CropPlanningController
    ApiEnvelope --> InputDemandController
    ApiEnvelope --> FpoReportController
    ApiEnvelope --> WorkflowController
    ApiEnvelope --> ActivityController
    ApiEnvelope --> EvidenceController
    ApiEnvelope --> ReportController
    ApiEnvelope --> NotificationController

    AuthController --> AuthService
    PlatformController --> TenantModuleService
    UserController --> UserService
    RoleController --> RoleService
    FpoMemberController --> FpoMemberService
    FarmAssetController --> FarmAssetService
    CropPlanningController --> CropPlanningService
    InputDemandController --> InputDemandService
    FpoReportController --> FpoDashboardSummaryService
    WorkflowController --> WorkflowService
    ActivityController --> ActivityService
    EvidenceController --> EvidenceService
    ReportController --> ReportService
    NotificationController --> NotificationService

    AuthService --> Repositories
    TenantModuleService --> Repositories
    UserService --> Repositories
    RoleService --> Repositories
    FpoMemberService --> Repositories
    FarmAssetService --> Repositories
    CropPlanningService --> Repositories
    InputDemandService --> Repositories
    FpoDashboardSummaryService --> Repositories
    WorkflowService --> Repositories
    ActivityService --> Repositories
    EvidenceService --> Repositories
    ReportService --> Repositories
    NotificationService --> Repositories

    UserService --> AuditService
    RoleService --> AuditService
    TenantModuleService --> AuditService
    FpoMemberService --> AuditService
    FarmAssetService --> AuditService
    CropPlanningService --> AuditService
    InputDemandService --> AuditService
    FpoDashboardSummaryService --> AuditService
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
    Platform["platform\nmodule catalog and tenant subscriptions"]
    FPO["fpo\nmember profiles and Phase 1 FPO domain model"]
    User["user\nadmin FIELD_COORDINATOR profile management"]
    Role["role\nrole catalog and role assignment"]
    Workflow["workflow\nworkflow definitions and task templates"]
    Activity["activity\nworkflow execution and task status"]
    Evidence["evidence\nproof metadata, upload, review"]
    Storage["storage\nvalidation, object key planning, local/MinIO"]
    Reporting["reporting\nsummary, PDF export, XLSX export"]
    Notification["notification\nnotification event/status foundation"]
    Audit["audit\nappend-only compliance event trail"]

    Common --> Auth
    Common --> Platform
    Common --> FPO
    Common --> User
    Common --> Role
    Common --> Workflow
    Common --> Activity
    Common --> Evidence
    Common --> Reporting
    Common --> Notification
    Security --> Auth
    Security --> Platform
    Security --> FPO
    Security --> User
    Security --> Role
    Security --> Workflow
    Security --> Activity
    Security --> Evidence
    Security --> Reporting
    Security --> Notification
    Platform --> Auth
    Platform --> Audit
    FPO --> Auth
    FPO --> Platform
    FPO --> Audit
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
    DbHost["PostgreSQL Docker Host Port\nlocalhost:5432"]
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

    class PlatformModuleEntity {
        table platform_modules
        UUID id PK
        ModuleCode code UNIQUE
        String name
        String description
        PlatformModuleStatus status
        Instant createdAt
        Instant updatedAt
    }

    class TenantModuleSubscriptionEntity {
        table tenant_module_subscriptions
        UUID id PK
        UUID tenantId FK
        UUID moduleId FK
        TenantModuleSubscriptionStatus status
        Instant enabledAt
        Instant disabledAt
        Instant expiresAt
        Instant createdAt
        Instant updatedAt
    }

    class FpoMemberProfileEntity {
        table fpo_member_profiles
        UUID id PK
        UUID tenantId FK
        UUID userId FK
        String memberNumber
        String displayName
        String mobileNumber
        String village
        String blockName
        String districtName
        FpoMemberStatus status
        Instant createdAt
        Instant updatedAt
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
        UUID FIELD_COORDINATORUserId FK
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
        UUID FIELD_COORDINATORUserId FK
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
    TenantEntity "1" --> "0..*" TenantModuleSubscriptionEntity : owns
    PlatformModuleEntity "1" --> "0..*" TenantModuleSubscriptionEntity : subscribed
    TenantEntity "1" --> "0..*" FpoMemberProfileEntity : owns
    UserEntity "1" --> "0..1" FpoMemberProfileEntity : profile
    UserEntity "0..1" --> "0..*" FpoMemberProfileEntity : coordinator
    UserEntity "1" --> "0..*" UserRole : has
    RoleEntity "1" --> "0..*" UserRole : assigned
    TenantEntity "1" --> "0..*" WorkflowDefinitionEntity : owns
    WorkflowDefinitionEntity "1" --> "1..*" WorkflowTaskEntity : contains
    TenantEntity "1" --> "0..*" ActivityEntity : owns
    WorkflowDefinitionEntity "1" --> "0..*" ActivityEntity : instantiates
    UserEntity "0..1" --> "0..*" ActivityEntity : FIELD_COORDINATOR
    ActivityEntity "1" --> "1..*" ActivityTaskEntity : contains
    WorkflowTaskEntity "1" --> "0..*" ActivityTaskEntity : template
    ActivityTaskEntity "1" --> "0..*" EvidenceEntity : proof
    UserEntity "0..1" --> "0..*" EvidenceEntity : FIELD_COORDINATOR
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
- Current role codes: `ADMIN`, `FPO_MANAGER`, `FIELD_COORDINATOR`.

## Module Subscription Tables

```mermaid
classDiagram
    class platform_modules {
        UUID id PK
        varchar code UNIQUE
        varchar name
        text description
        varchar status
        timestamptz created_at
        timestamptz updated_at
    }

    class tenant_module_subscriptions {
        UUID id PK
        UUID tenant_id FK
        UUID module_id FK
        varchar status
        timestamptz enabled_at
        timestamptz disabled_at
        timestamptz expires_at
        timestamptz created_at
        timestamptz updated_at
    }

    class tenants {
        UUID id PK
    }

    tenants "1" --> "0..*" tenant_module_subscriptions : tenant_id
    platform_modules "1" --> "0..*" tenant_module_subscriptions : module_id
```

Important rules:

- `platform_modules.code` is the stable product module key.
- `tenant_module_subscriptions`: unique `(tenant_id, module_id)`.
- Disabled modules return `MODULE_NOT_ENABLED` from guarded backend APIs.

## FPO Member And Farm Asset Tables

```mermaid
classDiagram
    class fpo_member_profiles {
        UUID id PK
        UUID tenant_id FK
        UUID user_id FK
        varchar member_number
        varchar display_name
        varchar mobile_number
        varchar alternate_mobile_number
        varchar village
        varchar block_name
        varchar district_name
        varchar gender
        date date_of_birth
        int age
        varchar farmer_category
        UUID coordinator_user_id FK
        varchar status
        timestamptz created_at
        timestamptz updated_at
    }

    class farm_landholdings {
        UUID id PK
        UUID tenant_id FK
        UUID member_profile_id FK
        varchar survey_number
        decimal total_area_acres
        decimal cultivable_area_acres
        varchar ownership_type
        varchar irrigation_source
        varchar status
        timestamptz created_at
        timestamptz updated_at
    }

    class farm_plots {
        UUID id PK
        UUID tenant_id FK
        UUID member_profile_id FK
        UUID landholding_id FK
        varchar plot_name
        decimal area_acres
        decimal latitude
        decimal longitude
        varchar soil_type
        varchar status
        timestamptz created_at
        timestamptz updated_at
    }

    class users {
        UUID id PK
    }

    class tenants {
        UUID id PK
    }

    tenants "1" --> "0..*" fpo_member_profiles : tenant_id
    users "1" --> "0..1" fpo_member_profiles : user_id
    users "0..1" --> "0..*" fpo_member_profiles : coordinator_user_id
    tenants "1" --> "0..*" farm_landholdings : tenant_id
    fpo_member_profiles "1" --> "0..*" farm_landholdings : member_profile_id
    tenants "1" --> "0..*" farm_plots : tenant_id
    fpo_member_profiles "1" --> "0..*" farm_plots : member_profile_id
    farm_landholdings "0..1" --> "0..*" farm_plots : landholding_id
```

Important constraints:

- `fpo_member_profiles`: unique `(tenant_id, user_id)`.
- `fpo_member_profiles`: unique `(tenant_id, member_number)`.
- `fpo_member_profiles`: unique `(tenant_id, mobile_number)`.
- Member profile APIs are guarded by the `MEMBER_DATA` module.
- Landholding and plot APIs are guarded by the `LAND_RECORDS` module.
- Farm asset status values are `ACTIVE`, `INACTIVE`, and `ARCHIVED`.

## FPO Crop Planning Tables

```mermaid
classDiagram
    class crop_catalog {
        UUID id PK
        UUID tenant_id FK
        varchar crop_code
        varchar crop_name
        varchar category
        varchar status
        timestamptz created_at
        timestamptz updated_at
    }

    class crop_seasons {
        UUID id PK
        UUID tenant_id FK
        varchar code
        varchar name
        int start_month
        int end_month
        int season_year
        varchar status
        timestamptz created_at
        timestamptz updated_at
    }

    class farmer_crop_history {
        UUID id PK
        UUID tenant_id FK
        UUID member_profile_id FK
        UUID crop_id FK
        UUID season_id FK
        int crop_year
        decimal area_acres
        decimal yield_quantity
        varchar yield_unit
        text notes
        timestamptz created_at
        timestamptz updated_at
    }

    class seasonal_crop_plans {
        UUID id PK
        UUID tenant_id FK
        UUID member_profile_id FK
        UUID plot_id FK
        UUID crop_id FK
        UUID season_id FK
        decimal planned_area_acres
        date planned_sowing_date
        date expected_harvest_date
        varchar status
        timestamptz created_at
        timestamptz updated_at
    }

    class tenants {
        UUID id PK
    }

    class fpo_member_profiles {
        UUID id PK
    }

    class farm_plots {
        UUID id PK
    }

    tenants "1" --> "0..*" crop_catalog : tenant_id
    tenants "1" --> "0..*" crop_seasons : tenant_id
    tenants "1" --> "0..*" farmer_crop_history : tenant_id
    tenants "1" --> "0..*" seasonal_crop_plans : tenant_id
    fpo_member_profiles "1" --> "0..*" farmer_crop_history : member_profile_id
    crop_catalog "1" --> "0..*" farmer_crop_history : crop_id
    crop_seasons "0..1" --> "0..*" farmer_crop_history : season_id
    fpo_member_profiles "1" --> "0..*" seasonal_crop_plans : member_profile_id
    farm_plots "0..1" --> "0..*" seasonal_crop_plans : plot_id
    crop_catalog "1" --> "0..*" seasonal_crop_plans : crop_id
    crop_seasons "1" --> "0..*" seasonal_crop_plans : season_id
```

Important constraints:

- `crop_catalog`: unique `(tenant_id, crop_code)`.
- `crop_seasons`: unique `(tenant_id, code, season_year)`.
- Crop and season APIs are guarded by the `CROP_PLANNING` module.
- Crop and season status values are `ACTIVE`, `INACTIVE`, and `ARCHIVED`.
- Crop plan status values are `DRAFT`, `CONFIRMED`, `CANCELLED`, and
  `COMPLETED`.
- Seasonal plans may reference a plot; when they do, the plot must be active
  and owned by the selected FPO member.

## FPO Input Demand Tables

```mermaid
classDiagram
    class input_catalog {
        UUID id PK
        UUID tenant_id FK
        varchar input_code
        varchar name
        varchar category
        varchar unit
        varchar status
        timestamptz created_at
        timestamptz updated_at
    }

    class crop_input_rules {
        UUID id PK
        UUID tenant_id FK
        UUID crop_id FK
        UUID input_id FK
        decimal quantity_per_acre
        varchar application_stage
        text notes
        varchar status
        timestamptz created_at
        timestamptz updated_at
    }

    class input_demand_estimates {
        UUID id PK
        UUID tenant_id FK
        UUID crop_plan_id FK
        UUID input_id FK
        decimal estimated_quantity
        varchar unit
        varchar status
        timestamptz created_at
        timestamptz updated_at
    }

    class tenants {
        UUID id PK
    }

    class crop_catalog {
        UUID id PK
    }

    class seasonal_crop_plans {
        UUID id PK
    }

    tenants "1" --> "0..*" input_catalog : tenant_id
    tenants "1" --> "0..*" crop_input_rules : tenant_id
    tenants "1" --> "0..*" input_demand_estimates : tenant_id
    crop_catalog "1" --> "0..*" crop_input_rules : crop_id
    input_catalog "1" --> "0..*" crop_input_rules : input_id
    seasonal_crop_plans "1" --> "0..*" input_demand_estimates : crop_plan_id
    input_catalog "1" --> "0..*" input_demand_estimates : input_id
```

Important constraints:

- `input_catalog`: unique `(tenant_id, input_code)`.
- `crop_input_rules`: unique `(tenant_id, crop_id, input_id, application_stage)`.
- `input_demand_estimates`: unique `(crop_plan_id, input_id)`.
- Input demand APIs are guarded by the `INPUT_DEMAND` module.
- Input and rule status values are `ACTIVE`, `INACTIVE`, and `ARCHIVED`.
- Demand estimate status values currently use `ESTIMATED` and `SUPERSEDED`.
- Demand calculation defaults to `CONFIRMED` seasonal crop plans and sums
  multiple active stages for the same crop/input into one estimate row.

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
        UUID FIELD_COORDINATOR_user_id FK
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
    users "0..1" --> "0..*" activities : FIELD_COORDINATOR_user_id
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
        UUID FIELD_COORDINATOR_user_id FK
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
    users "0..1" --> "0..*" evidence : FIELD_COORDINATOR_user_id
    users "0..1" --> "0..*" evidence : reviewed_by_user_id
```

Important rules:

- File bytes live in the configured storage provider; `evidence` stores metadata
  and the stable storage key.
- Evidence status values: `SUBMITTED`, `PENDING_REVIEW`, `APPROVED`,
  `REJECTED`.
- Reviewer fields are nullable until an admin/FPO_MANAGER reviews the proof.

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
| `platform_modules` | `platform` | `/api/v1/platform/modules` |
| `tenant_module_subscriptions` | `platform` | `/api/v1/platform/module-subscriptions`, `/api/v1/platform/modules/enabled` |
| `fpo_member_profiles` | `fpo` | `/api/v1/fpo/members` |
| `farm_landholdings` | `fpo` | `/api/v1/fpo/members/{memberId}/landholdings`, `/api/v1/fpo/landholdings/{id}` |
| `farm_plots` | `fpo` | `/api/v1/fpo/members/{memberId}/plots`, `/api/v1/fpo/plots/{id}` |
| `crop_catalog` | `fpo` | `/api/v1/fpo/crops` |
| `crop_seasons` | `fpo` | `/api/v1/fpo/seasons` |
| `farmer_crop_history` | `fpo` | `/api/v1/fpo/members/{memberId}/crop-history`, `/api/v1/fpo/crop-history/{id}` |
| `seasonal_crop_plans` | `fpo` | `/api/v1/fpo/crop-plans` |
| `input_catalog` | `fpo` | `/api/v1/fpo/inputs` |
| `crop_input_rules` | `fpo` | `/api/v1/fpo/input-rules` |
| `input_demand_estimates` | `fpo` | `/api/v1/fpo/demand-estimates`, `/api/v1/fpo/demand-estimates/summary` |
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
