# Roles, FPO Managers, And Field Coordinators

Last updated: 2026-05-13

Use this document for Phase 1 role behavior. The older generic `SUPERVISOR`
label is a legacy platform term and must not be used for FPO Phase 1 screens,
documentation, UAT, or new code paths.

## Phase 1 Role Model

| Role | Scope |
| ---- | ----- |
| `ADMIN` | Platform super user. Can see all FPOs, all farmers, all coordinators, all FPO managers, and future platform modules. |
| `FPO_MANAGER` | One-FPO manager. Can manage only assigned FPO farmers, field coordinators, crop plans, input demand, reports, advisories, and soil records. |
| `FIELD_COORDINATOR` | Field staff. Can manage assigned villages/farmers and enter farmer, land, GPS, soil, and crop plan data on behalf of farmers. |

Farmers do not need login in Phase 1. Data entry is admin/coordinator-first.

## Permission Defaults

| Capability | `ADMIN` | `FPO_MANAGER` | `FIELD_COORDINATOR` |
| ---------- | ------- | ------------- | ------------------- |
| Create FPO | Yes | No | No |
| Manage users and roles | Yes | Limited to own FPO staff/farmers if implemented | No |
| View all FPOs | Yes | No | No |
| View assigned FPO | Yes | Yes | Yes |
| Create/edit farmers | Yes | Yes, own FPO | Yes, assigned scope |
| Create/edit land/GPS records | Yes | Yes, own FPO | Yes, assigned scope |
| Enter soil profile values | Yes | Yes, own FPO | Yes, assigned scope |
| Create/edit crop plans | Yes | Yes, own FPO | Yes, assigned scope |
| Confirm crop plans | Yes | Yes, own FPO | Yes, assigned scope unless restricted later |
| Run input demand | Yes | Yes, own FPO | View/assist unless product owner restricts |
| Export reports | Yes | Yes, own FPO | View/export assigned scope if enabled |
| Create advisory | Yes | Yes, own FPO | No by default |
| View advisory | Yes | Yes, own FPO | Yes, assigned scope |

## Account Setup Flow

1. `ADMIN` creates or seeds the pilot FPO.
2. `ADMIN` creates one `FPO_MANAGER` for the pilot FPO.
3. `ADMIN` or `FPO_MANAGER` creates one or more `FIELD_COORDINATOR` users.
4. Farmer profiles are created as FPO member records, not as login users, unless
   a later Phase 2 change request enables farmer login.
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

## Implementation Alignment Needed

Current code still contains legacy `SUPERVISOR` and `PARTICIPANT` assumptions.
For Phase 1 completion, replace or map those assumptions carefully:

- Add `FPO_MANAGER` and `FIELD_COORDINATOR` role constants.
- Keep `ADMIN` as platform-wide super user.
- Do not rely on `PARTICIPANT` for farmer login in Phase 1.
- Enforce FPO scoping for `FPO_MANAGER` and `FIELD_COORDINATOR`.
- Update backend authorization annotations and service checks.
- Update frontend role labels, role ordering, and dashboard access rules.
- Update JUnit and integration tests to cover role isolation.
