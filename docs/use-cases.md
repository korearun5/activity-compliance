# Use Case Guide

## Actors

```mermaid
flowchart LR
    Admin["Admin"]
    Supervisor["Supervisor"]
    Participant["Participant / Farmer"]
    Government["Government / Client Reviewer"]
    System["System"]

    Admin --> UserMgmt["Manage participant profiles"]
    Admin --> WorkflowMgmt["Configure workflows"]
    Admin --> ReportMgmt["Generate compliance reports"]
    Supervisor --> UserMgmt
    Supervisor --> WorkflowMgmt
    Supervisor --> EvidenceReview["Review evidence"]
    Participant --> ActivityTracking["Track assigned farming/process activity"]
    Participant --> EvidenceSubmit["Submit photo proof"]
    Government --> ReportView["Review exported reports"]
    System --> AuditTrail["Record audit trail"]
    System --> Notifications["Send notifications (future)"]
```

## Use Case Diagram

```mermaid
flowchart TB
    Admin["Admin"]
    Supervisor["Supervisor"]
    Participant["Participant / Farmer"]
    Reviewer["Government / Client Reviewer"]

    UC1["Login"]
    UC2["Create participant profile"]
    UC3["Activate / deactivate participant"]
    UC4["View own profile"]
    UC5["Create workflow definition"]
    UC6["Update workflow status"]
    UC7["Start activity"]
    UC8["View activity timeline"]
    UC9["Update task status"]
    UC10["Upload evidence photo"]
    UC11["Review evidence"]
    UC12["View audit trail"]
    UC13["Generate PDF / Excel report"]
    UC14["View analytics summary"]

    Admin --> UC1
    Supervisor --> UC1
    Participant --> UC1
    Admin --> UC2
    Supervisor --> UC2
    Admin --> UC3
    Supervisor --> UC3
    Participant --> UC4
    Admin --> UC5
    Supervisor --> UC5
    Admin --> UC6
    Supervisor --> UC6
    Admin --> UC7
    Supervisor --> UC7
    Participant --> UC7
    Admin --> UC8
    Supervisor --> UC8
    Participant --> UC8
    Participant --> UC9
    Participant --> UC10
    Supervisor --> UC11
    Admin --> UC11
    Admin --> UC12
    Supervisor --> UC12
    Admin --> UC13
    Supervisor --> UC13
    Reviewer --> UC13
    Admin --> UC14
    Supervisor --> UC14
```

## Primary Use Cases

### UC-01 Login

Actors:

- Admin
- Supervisor
- Participant

Goal:

- Authenticate into the platform with tenant, username, and password.

Preconditions:

- User exists.
- User status is `ACTIVE`.

Main flow:

1. User enters username and password.
2. Frontend sends `POST /api/v1/auth/login`.
3. Backend validates tenant, user status, password hash, and roles.
4. Backend returns access token, refresh token, user id, tenant id, and roles.
5. Frontend stores session data and routes user by role.

Failure cases:

- Invalid credentials return `401`.
- Invalid request body returns `400`.
- Inactive user cannot log in.

### UC-02 Create Participant Profile

Actors:

- Admin
- Supervisor

Goal:

- Create a tenant-scoped participant/farmer profile with login credentials.

Preconditions:

- Actor has `ADMIN` or `SUPERVISOR` role.

Main flow:

1. Admin opens participant management.
2. Admin enters full name, phone, region, village/site, username, and password.
3. Frontend calls `POST /api/v1/users`.
4. Backend validates request.
5. Backend creates user with `PARTICIPANT` role.
6. Backend records `USER_CREATED` audit event.
7. Frontend refreshes participant list.

Failure cases:

- Duplicate username returns `409`.
- Missing/invalid fields return `400`.
- Participant user attempting this action returns `403`.

### UC-03 Activate Or Deactivate Participant

Actors:

- Admin
- Supervisor

Goal:

- Control whether a participant can continue using the platform without deleting
  history.

Main flow:

1. Admin selects active/inactive action for a participant.
2. Frontend calls `PATCH /api/v1/users/{userId}/status`.
3. Backend updates status.
4. Backend records `USER_STATUS_CHANGED`.
5. Frontend updates the participant card.

### UC-04 View Own Profile

Actors:

- Participant

Goal:

- View backend-owned participant profile fields assigned by an admin or
  supervisor.

Main flow:

1. Participant logs in.
2. Participant opens profile tab.
3. Frontend calls `GET /api/v1/users/me`.
4. Backend returns display name, phone, region/location, site/village, status,
   roles, tenant id, and user id.
5. Frontend displays the profile fields.

Business rules:

- Public/self-service signup is disabled.
- Participant profile changes are handled by admin/supervisor management APIs.

### UC-05 Configure Workflow

Actors:

- Admin
- Supervisor

Goal:

- Define a reusable process such as a crop lifecycle or inspection checklist.

Main flow:

1. Admin creates workflow definition.
2. Admin adds ordered tasks/stages.
3. Backend stores workflow and task templates.
4. Backend records workflow audit event.
5. Workflow can be activated for use.

Business rules:

- Workflow code/version are unique per tenant.
- Task codes and sequence numbers are unique inside a workflow.
- Workflows with activities should not be edited destructively; create a new
  version instead.

### UC-06 Start Activity

Actors:

- Participant
- Admin
- Supervisor

Goal:

- Start one execution of a workflow for a participant, farm, plot, inspection
  site, or unit.

Main flow:

1. User selects workflow.
2. User enters unit/location details.
3. Frontend calls `POST /api/v1/activities`.
4. Backend creates activity and activity tasks from workflow template.
5. Backend records `ACTIVITY_CREATED`.

### UC-07 Submit Evidence

Actors:

- Participant

Goal:

- Submit photo/file proof for a task.

Main flow:

1. Participant opens task.
2. Participant selects/captures photo and optional note.
3. Frontend calls `POST /api/v1/evidence` as multipart request.
4. Backend stores file through storage adapter.
5. Backend persists evidence metadata.
6. Backend marks task progress where applicable.
7. Backend records `EVIDENCE_SUBMITTED`.

### UC-08 Review Evidence

Actors:

- Admin
- Supervisor

Goal:

- Approve or reject submitted proof.

Main flow:

1. Reviewer opens evidence list.
2. Reviewer checks proof and context.
3. Reviewer sends approved/rejected status.
4. Backend records reviewer and timestamp.
5. Backend records `EVIDENCE_REVIEWED`.

### UC-09 Generate Compliance Report

Actors:

- Admin
- Supervisor
- Government/client reviewer

Goal:

- Produce report-ready evidence that the defined process was followed.

Planned report contents:

- Participant coverage.
- Workflow/activity status.
- Ordered task completion.
- Evidence photos/files and timestamps.
- Review status.
- Audit trail summary.
- Region/crop/date filters.

Current status:

- Reporting summary, PDF export, and Excel export endpoints are implemented for
  the generic activity/evidence model.
- FPO-specific farmer, landholding, crop-plan, acreage, and input-demand report
  formats are still pending. See [FPO MVP Roadmap](fpo-mvp-roadmap.md).

## Role Permission Matrix

| Use Case                               | Admin | Supervisor | Participant |
| -------------------------------------- | ----- | ---------- | ----------- |
| Login                                  | Yes   | Yes        | Yes         |
| Create participant                     | Yes   | Yes        | No          |
| Activate/deactivate participant        | Yes   | Yes        | No          |
| View own profile                       | Yes   | Yes        | Yes         |
| Create/update workflow                 | Yes   | Yes        | No          |
| Start own activity                     | Yes   | Yes        | Yes         |
| Start activity for another participant | Yes   | Yes        | No          |
| View own activities                    | Yes   | Yes        | Yes         |
| View tenant activities                 | Yes   | Yes        | No          |
| Upload evidence                        | Yes   | Yes        | Yes         |
| Review evidence                        | Yes   | Yes        | No          |
| Generate reports                       | Yes   | Yes        | No          |

## End-To-End Agriculture Scenario

```mermaid
flowchart TB
    A["Admin logs in"] --> B["Admin creates participant/farmer profile"]
    B --> C["Admin configures crop lifecycle workflow"]
    C --> D["Participant logs in"]
    D --> E["Participant starts crop activity"]
    E --> F["Participant completes task"]
    F --> G["Participant uploads proof photo"]
    G --> H["Supervisor reviews evidence"]
    H --> I["System updates timeline and audit trail"]
    I --> J["Admin generates government report"]
```
