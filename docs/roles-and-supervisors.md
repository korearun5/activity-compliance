# Roles, FPO Managers, And Field Coordinators

Last updated: 2026-05-15

Use this document for Phase 1 role behavior. `SUPERVISOR` is not a Phase 1
role; use `FIELD_COORDINATOR` for field staff.

## Phase 1 Role Model

| Role                | Scope                                                                                                                                       |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| `ADMIN`             | Platform super user. Can see all FPOs, all farmers, all coordinators, all FPO managers, and future platform modules.                        |
| `FPO_MANAGER`       | One-FPO manager. Can manage only assigned FPO farmers, field coordinators, crop plans, input demand, reports, advisories, and soil records. |
| `FIELD_COORDINATOR` | Field staff. Can manage assigned villages/farmers and enter farmer, land, GPS, soil, and crop plan data on behalf of farmers.               |
| `FARMER`            | Farmer/member login. Can use username/password in Phase 1 for own workflow/self-view access only. OTP remains Phase 2.                      |

Data entry remains admin/coordinator-first, but farmers can login with
username/password in Phase 1 for their own workflow area.

## Permission Defaults

| Capability                         | `ADMIN`                                          | `FPO_MANAGER` | `FIELD_COORDINATOR`                         | `FARMER`                |
| ---------------------------------- | ------------------------------------------------ | ------------- | ------------------------------------------- | ----------------------- |
| Create FPO                         | Yes                                              | No            | No                                          | No                      |
| Create FPO manager login           | Yes                                              | No            | No                                          | No                      |
| Create field coordinator login     | Yes                                              | Yes, own FPO  | No                                          | No                      |
| Change staff roles                 | Yes                                              | No            | No                                          | No                      |
| View all FPOs                      | Yes                                              | No            | No                                          | No                      |
| View assigned FPO                  | Yes                                              | Yes           | Yes                                         | Own records only        |
| Create/edit farmers                | Yes                                              | Yes, own FPO  | Yes, assigned scope                         | No                      |
| Create/edit land/GPS records       | Yes                                              | Yes, own FPO  | Yes, assigned scope                         | View own only           |
| Enter soil profile values          | Yes                                              | Yes, own FPO  | Yes, assigned scope                         | View own only           |
| Create/edit crop plans             | Yes                                              | Yes, own FPO  | Yes, assigned scope                         | View own only           |
| Confirm crop plans                 | Yes                                              | Yes, own FPO  | Yes, assigned scope unless restricted later | No                      |
| Run input demand                   | Yes                                              | Yes, own FPO  | No Phase 1 UI                               | No                      |
| Export compliance reports          | Yes                                              | Yes, own FPO  | No                                          | No                      |
| View/export FPO report summary     | Yes                                              | Yes, own FPO  | Assigned scope if backend permits           | No                      |
| Create advisory                    | Yes                                              | Yes, own FPO  | No by default                               | No                      |
| View advisory                      | Yes                                              | Yes, own FPO  | Yes, assigned scope                         | Own relevant advisories |
| Start/update own workflow activity | Yes, for any allowed field coordinator or farmer | Yes, own FPO  | Own activity                                | Own activity            |

## Frontend Page And Action Policy

The React app uses [roleAccess.ts](../src/auth/roleAccess.ts) as the frontend
equivalent of a Mendix role-page map. Keep new Phase 1 pages and buttons wired
through that policy instead of scattering one-off role checks across screens.

| Dashboard area                  | `ADMIN`                                                                           | `FPO_MANAGER`                 | `FIELD_COORDINATOR`                                                 | `FARMER`                    |
| ------------------------------- | --------------------------------------------------------------------------------- | ----------------------------- | ------------------------------------------------------------------- | --------------------------- |
| Staff dashboard shell           | Yes                                                                               | Yes                           | Yes                                                                 | No                          |
| Farmer workflow shell           | No                                                                                | No                            | No                                                                  | Yes                         |
| Farmers tab                     | Yes                                                                               | Yes                           | Yes                                                                 | Own profile only            |
| Crop Planning tab               | Yes                                                                               | Yes                           | Yes                                                                 | No                          |
| Crop/season master-data actions | Yes                                                                               | Yes                           | Read-only                                                           | No                          |
| Input Demand tab                | Yes                                                                               | Yes                           | Hidden                                                              | No                          |
| Roles tab                       | Yes                                                                               | Yes, create coordinators only | Hidden                                                              | No                          |
| Workflow definition tab         | Yes                                                                               | Yes                           | Hidden                                                              | Farmer workflow screen only |
| Advisory tab                    | Yes                                                                               | Yes                           | Read-only list                                                      | Relevant advisories later   |
| Generic compliance export       | Yes                                                                               | Yes                           | Disabled                                                            | No                          |
| Carbon screens                  | Visible when Carbon client module and `SUSTAINABILITY` backend module are enabled | Visible when enabled          | Visible when enabled; can manage assigned Carbon farmer enrollments | Visible when enabled        |

## Account Setup Flow

1. `ADMIN` creates or seeds the pilot FPO.
2. `ADMIN` creates one `FPO_MANAGER` login for the pilot FPO from staff user
   management.
3. `ADMIN` or `FPO_MANAGER` creates one or more `FIELD_COORDINATOR` logins from
   staff user management.
4. `ADMIN`, `FPO_MANAGER`, or assigned `FIELD_COORDINATOR` creates farmer
   profiles as FPO member records linked to `FARMER` users.
5. Staff users sign out and sign in again after role changes so JWT claims are
   refreshed.

## Creation Boundaries

- `ADMIN` is seed/manual for Phase 1. Do not add normal UI-driven admin
  creation unless a future operations requirement asks for it.
- `/api/v1/users` is for staff logins only: `FPO_MANAGER` and
  `FIELD_COORDINATOR`.
- `/api/v1/fpo/members` is the farmer creation flow. It creates or links the
  farmer/member profile and the `FARMER` login together.
- `FARMER` must not be assigned through generic role management because that
  would create an orphan login with no farmer profile.

## API Role Examples

FPO manager:

```json
{
  "roles": ["FPO_MANAGER"]
}
```

Field coordinator:

```json
{
  "roles": ["FIELD_COORDINATOR"]
}
```

Platform admin:

```json
{
  "roles": ["ADMIN"]
}
```

Combined admin and FPO manager:

```json
{
  "roles": ["ADMIN", "FPO_MANAGER"]
}
```

Farmer:

```json
{
  "roles": ["FARMER"]
}
```

## Implementation Alignment Needed

Current alignment:

- `ADMIN`, `FPO_MANAGER`, `FIELD_COORDINATOR`, and `FARMER` role constants exist.
- `FARMER` is separate from `FIELD_COORDINATOR`; do not reuse coordinator users
  as farmer logins.
- `fpo_member_profiles.user_id` points to the `FARMER` login user.
- `fpo_member_profiles.coordinator_user_id` points to the assigned
  `FIELD_COORDINATOR`.
- FPO scoping and member ownership are enforced in backend services.
- Staff login creation is separate from farmer profile/login creation.
- Frontend routes staff roles to the admin dashboard and `FARMER` to the user
  workflow dashboard.
- Frontend role management locks farmer-role changes and directs farmer login
  management back to farmer profiles.
- Frontend tab/action visibility is centralized in `src/auth/roleAccess.ts`;
  Carbon/FPO package visibility is also controlled by app feature flags and
  backend enabled modules.
- Focused JUnit and Testcontainers integration tests cover this foundation.
