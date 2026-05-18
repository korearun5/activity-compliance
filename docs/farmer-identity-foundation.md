# Farmer Identity Foundation

Last updated: 2026-05-19

This is the source of truth for the platform farmer identity refactor.

Core rule:

```text
users -> farmer_profiles -> module extension records
```

FPO, Carbon, and future modules must not own separate farmer identities. They
may store module-specific extension records, but every farmer-facing capability
must resolve the farmer through the canonical `farmer_profiles` row and its
linked `users` row.

## FOUNDATION-FARMER-001 Scope

Status: Done

This first slice adds the non-breaking foundation:

- Create the `farmer_profiles` table.
- Add nullable `farmer_profile_id` links to `fpo_member_profiles` and
  `carbon_profiles`.
- Add the backend `FarmerService` contract and default implementation.
- Keep existing FPO and Carbon reads working until the migration/backfill sprint
  links old records to canonical farmer profiles.

Implemented by:

- `V13__farmer_profile_foundation.sql`
- `FarmerProfileEntity`, `FarmerProfileRepository`, and `FarmerService`
- `FarmerProfileServiceTest`

## Table Schema

Table: `farmer_profiles`

| Column                    | Required | Purpose                                              |
| ------------------------- | -------- | ---------------------------------------------------- |
| `id`                      | Yes      | Primary key.                                         |
| `tenant_id`               | Yes      | Tenant boundary, references `tenants(id)`.           |
| `user_id`                 | Yes      | Farmer login identity, references `users(id)`.       |
| `display_name`            | Yes      | Farmer full name.                                    |
| `mobile_number`           | Yes      | Normalized 10 digit Indian mobile number.            |
| `alternate_mobile_number` | No       | Optional normalized 10 digit Indian mobile number.   |
| `aadhaar_number`          | No       | Optional 12 digit Aadhaar number.                    |
| `village`                 | Yes      | Farmer village.                                      |
| `taluka`                  | Yes      | Preferred geography label.                           |
| `district_name`           | Yes      | Farmer district.                                     |
| `state_name`              | Yes      | Farmer state.                                        |
| `gender`                  | Yes      | `MALE`, `FEMALE`, or `OTHER`.                        |
| `date_of_birth`           | No       | Optional birth date.                                 |
| `age`                     | No       | Optional age, constrained to 0 through 120.           |
| `farmer_category`         | Yes      | `MARGINAL`, `SMALL`, `SEMI_MEDIUM`, `MEDIUM`, `LARGE`. |
| `status`                  | Yes      | `ACTIVE`, `INACTIVE`, or `SUSPENDED`.                |
| `created_by_user_id`      | No       | Staff/admin user that created the profile.           |
| `created_at`              | Yes      | Creation timestamp.                                  |
| `updated_at`              | Yes      | Last update timestamp.                               |

Constraints and indexes:

- Unique `(tenant_id, user_id)` so one farmer login maps to one farmer profile
  per tenant.
- Partial unique `(tenant_id, aadhaar_number)` where Aadhaar is present.
- Search indexes for `(tenant_id, status)`, `(tenant_id, mobile_number)`,
  `(tenant_id, village, taluka, district_name)`, and `(tenant_id, created_at)`.
- `mobile_number` is intentionally not unique because shared family phones and
  data-entry review cases must not be silently merged.

Nullable extension links added in this slice:

- `fpo_member_profiles.farmer_profile_id -> farmer_profiles.id`
- `carbon_profiles.farmer_profile_id -> farmer_profiles.id`

These links remain nullable until the backfill has been verified in the target
environment and a later hardening migration can safely enforce required module
links.

## FarmerService Contract

Backend package:

```text
com.activityplatform.backend.farmer
```

Primary service interface:

```java
public interface FarmerService {
  FarmerProfileEntity createFarmerProfile(
      CurrentUser currentUser,
      UserEntity farmerUser,
      FarmerProfileCommand command
  );

  FarmerProfileEntity ensureFarmerProfileForUser(
      CurrentUser currentUser,
      UserEntity farmerUser,
      FarmerProfileCommand command
  );

  FarmerProfileEntity updateFarmerProfile(
      CurrentUser currentUser,
      UUID farmerProfileId,
      FarmerProfileCommand command
  );

  Optional<FarmerProfileEntity> findByUserId(UUID tenantId, UUID userId);

  FarmerProfileEntity requireById(UUID tenantId, UUID farmerProfileId);

  FarmerProfileEntity requireByUserId(UUID tenantId, UUID userId);

  List<FarmerParticipant> findParticipants(UUID tenantId);
}
```

Service rules:

- A farmer profile must link to a user in the same tenant.
- The linked user must be a farmer-only user with role `FARMER`.
- `ensureFarmerProfileForUser` is idempotent. It returns the existing canonical
  profile when one already exists and does not silently overwrite details.
- `findParticipants` returns active canonical farmers and exposes the platform
  user id that workflow assignment must use.
- FPO and Carbon services should call this service before creating or linking
  module-specific farmer extension records.

## Migration Outline

Use this order to avoid breaking current reads:

1. `FOUNDATION-FARMER-001`: Create `farmer_profiles`, add nullable
   `farmer_profile_id` columns, and add the `FarmerService` contract.
2. `FOUNDATION-FARMER-002`: Map FPO member profiles to `farmer_profile_id` in
   the entity/service layer, but keep legacy fields readable during transition.
3. `FOUNDATION-FARMER-003`: Map Carbon profiles to `farmer_profile_id` in the
   entity/service layer and require new Carbon farmer creation to resolve
   through `FarmerService`.
4. `FOUNDATION-MIGRATION-001`: Backfill existing records.
   - Auto-link exact shared `user_id` matches.
   - Link Carbon records through selected FPO members when that member already
     has a canonical farmer profile and there is no conflicting Carbon user.
   - Link Carbon records by mobile only when the mobile is unique on both sides.
   - Create or link canonical farmer profiles only for users whose only role is
     `FARMER`, matching the runtime `FarmerService` contract.
   - Create audit records for every automatic decision and every unresolved
     candidate.
   - Do not silently merge ambiguous farmers.
   - Do not delete old users automatically.
5. `FOUNDATION-PARTICIPANT-001`: Make workflow participant selection resolve
   through canonical farmer users from `farmer_profiles`.
6. `FOUNDATION-UI-001`: Replace FPO-gated farmer activity UI with a common
   farmer dashboard that queries activities by current `users.id`.
7. `FOUNDATION-QA-001`: Add tests for Carbon-only, FPO-only, and combined
   module configurations.
8. Cleanup after validation: make module `farmer_profile_id` links required
   where business rules require one, and remove old identity-resolution paths.

## Non-Breaking Read Rules

Until participant and farmer-dashboard cleanup is complete:

- Do not remove existing FPO or Carbon farmer-shaped columns.
- Do not change existing API response fields solely for this foundation slice.
- Do not make `fpo_member_profiles.farmer_profile_id` or
  `carbon_profiles.farmer_profile_id` non-null until target-environment audit
  rows are reviewed.
- New code that needs a farmer participant should prefer `FarmerService`.

## FOUNDATION-FARMER-002 Status

Status: Done

Implemented by:

- `V14__fpo_farmer_profile_link_guard.sql`
- `FpoMemberProfileEntity.farmerProfile`
- `FpoMemberService` calling `FarmerService` during create, update, and status
  changes
- `FpoMemberControllerIT` assertions proving new and existing farmer users get
  linked to canonical farmer profiles

Behavior:

- New FPO member creation creates or reuses the canonical farmer profile for
  the linked farmer user.
- FPO member updates keep the canonical farmer profile details aligned.
- FPO member status changes keep the canonical farmer profile status aligned,
  so participant lookup can later filter inactive/suspended farmers correctly.
- Existing API responses and read queries remain compatible.
- Historical rows can still have `farmer_profile_id = NULL` until
  `FOUNDATION-MIGRATION-001` backfills them.

## FOUNDATION-FARMER-003 Status

Status: Done

Implemented by:

- `CarbonProfileEntity.farmerProfile`
- `CarbonProfileService` calling `FarmerService` during Carbon farmer profile
  create and update paths
- `CarbonProfileControllerIT` assertions proving existing farmer users and
  newly created Carbon farmer logins get linked to canonical farmer profiles

Behavior:

- Carbon profiles with `participant_type = FARMER` link to the canonical farmer
  profile for the selected or newly created farmer user.
- Carbon updates keep canonical farmer identity fields aligned when the Carbon
  profile is already linked to the same farmer.
- Carbon enrollment status remains module-specific. Carbon updates preserve the
  existing canonical farmer status instead of globally suspending/reactivating a
  farmer who may also belong to FPO or future modules.
- Non-farmer Carbon participant types keep `farmer_profile_id = NULL`.
- Existing Carbon API responses and read queries remain compatible.

Carbon uniqueness decision:

- FPO membership has a unique `farmer_profile_id` guard because one farmer
  should have one member profile per tenant.
- Carbon keeps the existing non-unique `(tenant_id, farmer_profile_id)` index
  from `V13` because current Carbon flows already allow multiple Carbon profiles
  for the same farmer, and future carbon project/enrollment modelling may need
  `carbon_project_id` or an equivalent field before enforcing uniqueness.

## FOUNDATION-FARMER-004 Status

Status: Done

Implemented by:

- `FarmerService`
- `FarmerProfileService`
- `FarmerProfileCommand`
- `FarmerParticipant`
- `FarmerProfileServiceTest`

Behavior:

- `FarmerService` is the single runtime contract for creating, ensuring,
  updating, and resolving canonical farmer profiles.
- FPO and Carbon creation/update paths already call the shared service instead
  of owning separate farmer identity logic.
- `ensureFarmerProfileForUser` is idempotent and keeps existing canonical
  profile details stable unless an explicit update path is used.
- Participant lookup exposes the canonical farmer `user_id` that workflow
  assignment will use in `FOUNDATION-PARTICIPANT-001`.

## FOUNDATION-MIGRATION-001 Status

Status: Done

Implemented by:

- `V15__farmer_profile_backfill.sql`
- `farmer_profile_migration_audit`
- `FarmerProfileBackfillMigrationTest`

Behavior:

- Creates one audit table for backfill decisions and unresolved records.
- Backfills FPO member profiles by exact tenant/user match.
- Backfills Carbon farmer profiles by exact tenant/user match, selected FPO
  member linkage, or unique tenant/mobile match when deterministic.
- Matches the runtime service contract by creating and linking canonical farmer
  profiles only for farmer-only users.
- Flags conflicting or unresolved Carbon records instead of creating users,
  deleting users, or silently merging ambiguous farmers.
- Keeps `farmer_profile_id` nullable after backfill so the team can review
  target-environment audit rows before a future `NOT NULL` hardening migration.

Audit actions:

- `FPO_FARMER_PROFILE_CREATED_BY_USER`
- `FPO_AUTO_LINKED_BY_USER`
- `FPO_FLAGGED_UNLINKED`
- `CARBON_FLAGGED_USER_MEMBER_MISMATCH`
- `CARBON_FARMER_PROFILE_CREATED_BY_USER`
- `CARBON_AUTO_LINKED_BY_USER`
- `CARBON_AUTO_LINKED_BY_FPO_MEMBER`
- `CARBON_AUTO_LINKED_BY_UNIQUE_MOBILE`
- `CARBON_FLAGGED_NO_SAFE_USER_OR_MOBILE_MATCH`
- `CARBON_FLAGGED_MISSING_REQUIRED_IDENTITY`

## FOUNDATION-PARTICIPANT-001 Status

Status: Done

Implemented by:

- `FarmerParticipantController`
- `FarmerParticipantResponse`
- `ActivityService` canonical farmer participant validation
- `FarmerParticipantControllerIT`
- `ActivityControllerIT` canonical farmer assignment/list coverage
- Frontend `activityParticipantStore` backend participant loading

Behavior:

- Staff participant pickers can load active canonical farmers from
  `/api/v1/farmers/participants`.
- The backend participant id used to start an activity is always the canonical
  farmer `users.id` exposed by `farmer_profiles.user_id`.
- Activity assignment still supports field coordinators where existing workflow
  rules allow them, but farmer assignment now requires an active canonical
  `farmer_profiles` row.
- Farmer users cannot list all participant records.
- When a backend session exists, the frontend workflow participant picker uses
  the platform participant endpoint instead of composing participants from FPO
  and Carbon extension records.

## FOUNDATION-UI-001 Status

Status: Done

Implemented by:

- `roleAccess.ts` farmer tab visibility rules
- `UserHomeScreen` common farmer activity dashboard startup and backend loading
- `module-visibility-smoke.mjs` Carbon-only/FPO-only/combined visibility
  expectations

Behavior:

- Farmer activity, dashboard, and history tabs belong to the common
  `ACTIVITY_COMPLIANCE` module instead of the FPO client module.
- A Carbon-only farmer package still opens on the common activity list first,
  so assigned activities are visible without enabling FPO UI.
- The Carbon tab remains Carbon-scoped and the FPO profile tab remains
  FPO-scoped.
- When a backend session exists, farmer activities are loaded from the backend
  activity API for the current user. If that backend load fails, the UI shows
  the error and does not fall back to stale local/FPO-shaped activity records.
- Local activity fallback remains only for no-backend local/demo sessions.

## Future Extension Rule

When a new module needs farmer data, it must create an extension table:

```text
future_module_farmer_records
  id
  tenant_id
  farmer_profile_id
  module_specific_fields...
```

It must not create another farmer login/profile table. This keeps activity,
reports, notifications, access control, and future mobile login consistent
across Carbon-only, FPO-only, combined, and future module packages.
