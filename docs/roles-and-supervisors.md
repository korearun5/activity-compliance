# Roles, FPO Managers, And Field Coordinators

Last updated: 2026-05-14

Use this document for Phase 1 role behavior. `SUPERVISOR` is not a Phase 1
role; use `FIELD_COORDINATOR` for field staff.

## Phase 1 Role Model

| Role | Scope |
| ---- | ----- |
| `ADMIN` | Platform super user. Can see all FPOs, all farmers, all coordinators, all FPO managers, and future platform modules. |
| `FPO_MANAGER` | One-FPO manager. Can manage only assigned FPO farmers, field coordinators, crop plans, input demand, reports, advisories, and soil records. |
| `FIELD_COORDINATOR` | Field staff. Can manage assigned villages/farmers and enter farmer, land, GPS, soil, and crop plan data on behalf of farmers. |
| `FARMER` | Farmer/member login. Can use username/password in Phase 1 for own workflow/self-view access only. OTP remains Phase 2. |

Data entry remains admin/coordinator-first, but farmers can login with
username/password in Phase 1 for their own workflow area.

## Permission Defaults

| Capability | `ADMIN` | `FPO_MANAGER` | `FIELD_COORDINATOR` | `FARMER` |
| ---------- | ------- | ------------- | ------------------- | -------- |
| Create FPO | Yes | No | No | No |
| Manage users and roles | Yes | Limited to own FPO staff/farmers if implemented | No | No |
| View all FPOs | Yes | No | No | No |
| View assigned FPO | Yes | Yes | Yes | Own records only |
| Create/edit farmers | Yes | Yes, own FPO | Yes, assigned scope | No |
| Create/edit land/GPS records | Yes | Yes, own FPO | Yes, assigned scope | View own only |
| Enter soil profile values | Yes | Yes, own FPO | Yes, assigned scope | View own only |
| Create/edit crop plans | Yes | Yes, own FPO | Yes, assigned scope | View own only |
| Confirm crop plans | Yes | Yes, own FPO | Yes, assigned scope unless restricted later | No |
| Run input demand | Yes | Yes, own FPO | View/assist unless product owner restricts | No |
| Export reports | Yes | Yes, own FPO | View/export assigned scope if enabled | No |
| Create advisory | Yes | Yes, own FPO | No by default | No |
| View advisory | Yes | Yes, own FPO | Yes, assigned scope | Own relevant advisories |
| Start/update own workflow activity | Yes, for any allowed field coordinator or farmer | Yes, own FPO | Own activity | Own activity |

## Account Setup Flow

1. `ADMIN` creates or seeds the pilot FPO.
2. `ADMIN` creates one `FPO_MANAGER` for the pilot FPO.
3. `ADMIN` or `FPO_MANAGER` creates one or more `FIELD_COORDINATOR` users.
4. `ADMIN`, `FPO_MANAGER`, or assigned `FIELD_COORDINATOR` creates farmer
   profiles as FPO member records linked to `FARMER` users.
5. Staff users sign out and sign in again after role changes so JWT claims are
   refreshed.

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
- Frontend routes staff roles to the admin dashboard and `FARMER` to the user
  workflow dashboard.
- Focused JUnit and Testcontainers integration tests cover this foundation.
