# Database Guide

The backend uses PostgreSQL with Flyway migrations. The schema is reusable: agriculture is one configuration of the workflow model, not a separate hardcoded database design.

For visual class/table diagrams, see
[Component And Data Model Diagrams](component-and-data-model-diagrams.md).

## Tables In Use Now

- `tenants`: client/organization boundary.
- `users`: reusable login accounts for staff and farmers.
- `roles`: tenant-scoped role records.
- `user_roles`: user-to-role assignments.
- `platform_modules`: sellable module catalog for modular packaging/pricing.
- `tenant_module_subscriptions`: enabled/disabled module subscriptions per tenant.
- `carbon_profiles`: Carbon identity and participant/farmer profile foundation.
- `carbon_farm_plots`: Carbon farm/plot GPS point and optional boundary data.
- `carbon_soil_profiles`: Carbon soil test values and optional report metadata.
- `carbon_activity_categories`: seeded regenerative activity category catalog.
- `fpo_member_profiles`: first-class FPO farmer/member profile linked to `users`.
- `farm_landholdings`: Phase 1 landholding records for each FPO member.
- `farm_plots`: GPS-ready farm plot records for each member/landholding.
- `crop_catalog`: tenant crop master data.
- `crop_seasons`: tenant season master data.
- `farmer_crop_history`: historical crop records by member.
- `seasonal_crop_plans`: planned crops by member, plot, crop, and season.
- `input_catalog`: tenant input master data.
- `crop_input_rules`: crop/input quantity-per-acre planning rules.
- `input_demand_estimates`: generated or stored input demand estimates.
- `fpo_advisories`: in-app advisory records targeted to all members, a village,
  or one member profile.
- `workflow_definitions`: admin-defined process templates, such as a crop cycle.
- `workflow_tasks`: ordered task/stage list for each workflow definition.
- `activities`: one field coordinator or farmer executing one workflow.
- `activity_tasks`: runtime task status for one activity.
- `evidence`: uploaded proof file metadata, notes, review status, and storage key.
- `audit_events`: append-only compliance trail for user/activity/evidence/report events.
- `report_exports`: generated PDF/Excel export history.
- `notification_events`: queued notification/status messages.

## Data Ownership

- Backend owns users, roles, workflows, activities, activity tasks, evidence, reports, and audit history.
- Frontend may keep session tokens, active form state, selected local photo before upload, and temporary prototype cache only.
- Crop/task definitions must be configured through workflow records. They should not be hardcoded in the UI.

## Workflow Pattern

1. Admin creates a row in `workflow_definitions`.
2. Admin adds ordered rows in `workflow_tasks`.
3. FPO member records link farmer users to member numbers and village/block/district metadata.
4. Admin/FPO manager or assigned field coordinator records member landholdings in `farm_landholdings`.
5. Admin/FPO manager or assigned field coordinator records GPS-ready plots in `farm_plots`.
6. Admin/FPO manager maintains crop and season masters.
7. Admin/FPO manager or assigned field coordinator records member crop history.
8. Admin/FPO manager or assigned field coordinator creates seasonal crop plans.
9. Admin/FPO manager maintains input catalog and crop input rules.
10. Admin/FPO manager generates input demand estimates from confirmed seasonal
    crop plans.
11. Admin/FPO manager publishes advisory records through `fpo_advisories`.
12. Farmer or field coordinator starts an `activity` from the active workflow.
13. Backend creates `activity_tasks` from the configured task list.
14. Farmer or field coordinator uploads proof to `evidence`.
15. Backend updates task/activity progress.
16. Admin reviews proof and later generates reports from these records.
17. Admin reads `/api/v1/fpo/reports/summary` for consolidated FPO dashboard
    metrics across member, land, plot, crop plan, and input demand records.

## FPO Member And Land Record Pattern

The FPO member model is now explicit:

1. `users` stores login identity, password hash, status, and roles.
2. `fpo_member_profiles.user_id` links to a `FARMER` login account.
3. `fpo_member_profiles.coordinator_user_id` links to the assigned
   `FIELD_COORDINATOR`.
4. `fpo_member_profiles` stores farmer/member number, mobile number, village,
   taluka, district, state, category, and profile status.
5. `farm_landholdings` stores member-level operated/owned area summaries with
   required survey/khasra, ownership, and irrigation values.
6. `farm_plots` stores individual plot area and required GPS coordinates.

Landholding and plot records are protected by the `LAND_RECORDS` module. They
are managed through `/api/v1/fpo/members/{memberId}/landholdings` and
`/api/v1/fpo/members/{memberId}/plots`, with status changes handled through
`PATCH` endpoints rather than hard deletes.

## Carbon Profile Pattern

The Carbon profile model is separate from FPO member records so Carbon can be
packaged as its own client app:

1. `carbon_profiles` stores Carbon identity, participant type, location, farm
   summary, practice baseline, document readiness, linked user, optional FPO
   member, and optional assigned field coordinator.
2. `carbon_farm_plots` stores Carbon plot/farm units with required GPS point
   capture and plot-level baseline context.
3. `carbon_soil_profiles` stores existing soil test values and optional report
   metadata. These values do not calculate carbon credits.
4. Field coordinators can only read or mutate Carbon profiles assigned through
   `carbon_profiles.coordinator_user_id`.
5. Farmers can read their linked profile and related plot/soil records through
   their login; OTP remains future/provider-dependent.

Carbon records are protected by the `SUSTAINABILITY` module and managed through:

- `/api/v1/carbon/profiles`
- `/api/v1/carbon/profiles/me`
- `/api/v1/carbon/profiles/{profileId}/plots`
- `/api/v1/carbon/plots/{plotId}`
- `/api/v1/carbon/profiles/{profileId}/soil-profiles`
- `/api/v1/carbon/soil-profiles/{soilProfileId}`

The schema intentionally stores profile, plot, and soil data only. Methodology,
verified carbon calculation, AI verification, satellite layers, payments, and
OTP/SMS remain outside the current implementation.

## FPO Crop Planning Pattern

The crop planning model is explicit and tenant-scoped:

1. `crop_catalog` stores tenant crop master records.
2. `crop_seasons` stores season windows by code and year.
3. `farmer_crop_history` stores historical crop records per FPO member.
4. `seasonal_crop_plans` stores current/future planned acreage by member,
   optional plot, crop, and season.

Crop planning records are protected by the `CROP_PLANNING` module and managed
through:

- `/api/v1/fpo/crops`
- `/api/v1/fpo/seasons`
- `/api/v1/fpo/members/{memberId}/crop-history`
- `/api/v1/fpo/crop-plans`

Crop and season lifecycle values use `ACTIVE`, `INACTIVE`, and `ARCHIVED`.
Seasonal crop plan status values use `DRAFT`, `CONFIRMED`, `CANCELLED`, and
`COMPLETED`. If a crop plan references a plot, the selected planned acreage
cannot exceed that active plot's acreage.

## FPO Input Demand Pattern

The input demand model is explicit and tenant-scoped:

1. `input_catalog` stores tenant input master records such as seed,
   fertilizer, micronutrient, biological, or other input categories.
2. `crop_input_rules` stores quantity-per-acre rules by crop, input, and
   optional application stage.
3. `input_demand_estimates` stores generated calculation snapshots per crop
   plan and input.

Input demand records are protected by the `INPUT_DEMAND` module and managed
through:

- `/api/v1/fpo/inputs`
- `/api/v1/fpo/input-rules`
- `/api/v1/fpo/demand-estimates/run`
- `/api/v1/fpo/demand-estimates`
- `/api/v1/fpo/demand-estimates/summary`

Calculation currently uses confirmed seasonal crop plans by default:

```text
estimated_quantity = planned_area_acres * quantity_per_acre
```

If multiple active rules exist for the same crop and input at different
application stages, the generated estimate sums those stages into one row per
crop plan and input. Missing active rules are counted and skipped so a bad rule
does not create misleading demand.

## FPO Advisory Pattern

The advisory model is tenant-scoped and intentionally delivery-light for Phase
1:

1. `fpo_advisories` stores title, message, category, in-app channel,
   crop/season context, target type, and lifecycle status.
2. `target_type` is `ALL_MEMBERS` or `CROP`; crop-targeted advisories require
   `crop_id`.
3. `fpo_advisory_images` stores one or more object-storage image links and
   optional image metadata per advisory.
4. Published farmer reads return records targeted to all members or to crops
   where the farmer has a confirmed crop plan.

Advisory records are protected by the `ADVISORY` module and managed through:

- `/api/v1/fpo/advisories`
- `/api/v1/fpo/advisories/{advisoryId}`
- `/api/v1/fpo/advisories/{advisoryId}/status`

Advisory status values use `DRAFT`, `PUBLISHED`, and `ARCHIVED`. Phase 1 allows
`IN_APP` advisories only; SMS, WhatsApp, push, and email delivery require a
separate provider integration later.

## FPO Dashboard Summary Pattern

The FPO dashboard summary API does not store a new summary table. It reads the
current durable FPO records and returns a point-in-time aggregate:

- member counts from `fpo_member_profiles`
- landholding area from `farm_landholdings`
- plot area and geo-tagged count from `farm_plots`
- confirmed crop plan coverage from `seasonal_crop_plans`
- input totals from `input_demand_estimates`

The API is protected by the `REPORT_EXPORT` module and emits
`FPO_REPORT_SUMMARY_VIEWED`. FPO Excel/PDF exports should reuse the same source
tables so dashboard totals and export totals stay aligned.

## Module Subscription Pattern

Module gating is tenant-scoped:

1. `platform_modules` stores the known catalog, such as `MEMBER_DATA`,
   `LAND_RECORDS`, `ACTIVITY_COMPLIANCE`, and `REPORT_EXPORT`.
2. `tenant_module_subscriptions` stores whether a tenant currently has that
   module enabled.
3. Backend module APIs call the module guard before business logic. Disabled
   modules return `MODULE_NOT_ENABLED` using the normal API envelope.
4. Frontend navigation reads `/api/v1/platform/modules/enabled` after login and
   hides disabled tabs for cleaner UX.
