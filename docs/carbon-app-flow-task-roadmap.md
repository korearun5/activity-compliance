# Carbon App Flow Task Roadmap

Last updated: 2026-05-16

This document is the task-wise execution roadmap for the client-provided
`App Flow.docx`. It should be used for Carbon app sequencing only.

Related documents:

- [Carbon App Flow Base Plan](carbon-app-flow-base-plan.md): source flow,
  assumptions, and provider/client decisions.
- [Modular Platform Strategy](modular-platform-strategy.md): module packaging,
  tenant subscriptions, and feature-toggle rules.
- [Project Status And Gap Register](project-status-and-gap-register.md):
  completion percentages, go-live confidence, and open gaps.

## Product Direction

The next client-facing package is Carbon-first.

- Carbon UI is enabled by default.
- FPO operations remain in the codebase as foundation, but are hidden by default
  through `EXPO_PUBLIC_ENABLED_CLIENT_MODULES=carbon`.
- FPO can be enabled later for clients who buy the FPO operations package.
- If source code is handed over to a Carbon-only client, do not rely on this
  flag for protection; prepare a Carbon distribution with shared core plus
  Carbon module only.
- New Phase 2 features must live behind module checks and must degrade cleanly
  when the module is disabled.
- FPO-only clients should see a "Get Carbon Credits" upsell instead of broken or
  empty Carbon tabs.

## Execution Rules

- New Carbon work starts under `src/modules/carbon` whenever practical.
- New FPO work starts under `src/modules/fpo` whenever practical.
- Existing screens can move gradually; avoid a risky folder-only refactor.
- Every new screen, API adapter, store, and navigation entry must check both
  frontend feature flags and backend enabled modules where backend data is used.
- Disabled modules must not leave dead tabs, blocked buttons, or confusing empty
  states.
- Frontend flags are not license protection. Source distributions must exclude
  unlicensed modules or replace them with stubs.
- Any carbon calculation before methodology approval must be clearly marked
  provisional and must not be presented as verified credit issuance.
- OTP, maps, AI verification, satellite layers, dealer stock sync, and payments
  stay provider-dependent until their vendor/provider decisions are locked.

## Consequences To Be Aware Of

- Carbon-first packaging is correct for the first client, but the codebase must
  stay a reusable platform for future apps.
- `EXPO_PUBLIC_ENABLED_CLIENT_MODULES=carbon` hides FPO from the UI; it does not
  remove FPO source code from the internal repository.
- The backend tenant must enable `SUSTAINABILITY` for Carbon screens backed by
  tenant modules to appear and behave consistently.
- If source code is shared, create a Carbon distribution package. Do not share
  the full internal platform repository for a Carbon-only license.
- The next technical cleanup should move Carbon-owned code into
  `src/modules/carbon` so future apps can follow the same pattern.

## Phase C0: Module Packaging Foundation

Goal: Make the app safe to package as Carbon-only, FPO-only, or full platform.

| ID | Status | Task | Acceptance |
| --- | --- | --- | --- |
| CARBON-MOD-001 | Done | Add generic frontend client-module package switch | `EXPO_PUBLIC_ENABLED_CLIENT_MODULES=carbon`, `fpo`, or `carbon,fpo` controls visible app areas and can grow for future apps. |
| CARBON-MOD-002 | Next | Move Carbon-owned store/API/screen code into `src/modules/carbon` | Carbon exports are owned from one module entry point; existing imports still work during transition. |
| CARBON-MOD-003 | Pending | Move FPO-owned screen/data/API boundaries into `src/modules/fpo` when next touched | FPO remains independently loadable without blocking current Carbon work. |
| CARBON-MOD-004 | Next | Add module visibility smoke tests | Tests prove FPO tabs are hidden in Carbon-first mode and Carbon tabs hide cleanly when disabled. |
| CARBON-MOD-005 | Pending | Add tenant/module fallback UX states | Disabled modules show upgrade or unavailable states instead of runtime errors. |
| CARBON-MOD-006 | Pending | Define Carbon source distribution package | Carbon-only handover includes shared core plus Carbon, excludes unlicensed FPO implementation, and documents license boundaries. |

## Phase C1: Client-Visible Carbon App Shell

Goal: Match the main screens from `App Flow.docx` so the client can see the
Carbon journey end to end with safe dummy/provisional data.

| ID | Status | Task | Acceptance |
| --- | --- | --- | --- |
| CARBON-SHELL-001 | Pending | Add splash and language-selection shell | Logo/tagline area exists with English, Marathi, and Hindi choices stored locally. |
| CARBON-SHELL-002 | Pending | Align login/entry screen with user types | Farmer, FPO/FPC, and Agronomist paths are visible; OTP remains marked Phase 2/provider-dependent. |
| CARBON-SHELL-003 | Pending | Carbon dashboard widgets | Shows farm area, soil carbon score, credit potential, pending activities, advisories, weather placeholder, and dealers. |
| CARBON-SHELL-004 | Pending | Carbon bottom navigation model | Home, Farm, Activities, Advisory, and Marketplace can be represented without exposing disabled FPO operations. |
| CARBON-SHELL-005 | Pending | Carbon demo seed dataset | Demo includes at least one farmer, farm, soil profile, advisory, activity, dealer, and carbon summary. |

## Phase C2: Carbon Identity, Farm, And Soil Profile

Goal: Replace dummy profile and soil records with durable backend data that can
support later verification.

| ID | Status | Task | Acceptance |
| --- | --- | --- | --- |
| CARBON-PROFILE-001 | Pending | Draft carbon data dictionary | Defines carbon identity, farmer profile extension, farm profile, documents, and bank-status fields. |
| CARBON-PROFILE-002 | Pending | Add backend carbon identity/profile APIs | Carbon identity can be created, viewed, and scoped by tenant/user role. |
| CARBON-PROFILE-003 | Pending | Add frontend carbon profile forms | Farmer/FPO/admin can view and maintain carbon profile fields based on role. |
| CARBON-SOIL-001 | Pending | Add durable soil profile schema | Stores SOC, pH, EC, NPK, bulk density, texture, and optional biological fields without calculating credits. |
| CARBON-SOIL-002 | Pending | Add soil report upload support | PDF/image report links are stored through the storage adapter with metadata. |
| CARBON-GEO-001 | Pending | Add Carbon farm GPS capture | Point capture works now; boundary drawing remains blocked on map provider. |

## Phase C3: Farm Activity Tracking And Evidence

Goal: Turn regenerative practices into auditable activity records.

| ID | Status | Task | Acceptance |
| --- | --- | --- | --- |
| CARBON-ACT-001 | Pending | Seed carbon activity categories | Land preparation, sowing, irrigation, fertilizer, biologicals, compost, biomass, and harvest categories exist. |
| CARBON-ACT-002 | Pending | Add carbon activity entry flow | User selects farm, crop, activity, date, input quantity, remarks, and evidence. |
| CARBON-ACT-003 | Pending | Reuse evidence upload and review | Photos/documents can be uploaded, reviewed, approved, or rejected. |
| CARBON-ACT-004 | Pending | Add verification queue for admins/agronomists | Reviewers can see pending carbon activity evidence with filters and status changes. |
| CARBON-ACT-005 | Pending | Add provisional practice score | Score is shown as provisional until methodology is approved. |

## Phase C4: Advisory And Expert Query Workflow

Goal: Let farmers/coordinators ask advisory questions and let experts respond.

| ID | Status | Task | Acceptance |
| --- | --- | --- | --- |
| CARBON-ADV-001 | Pending | Add farmer query submission | Text plus image attachment is supported first; voice/video remain future/provider-dependent. |
| CARBON-ADV-002 | Pending | Add query categories | Pest, nutrient, irrigation, carbon practice, and biological dosage categories are available. |
| CARBON-ADV-003 | Pending | Add expert/agronomist inbox | Agronomist can receive, filter, and respond to queries. |
| CARBON-ADV-004 | Pending | Preserve advisory history | Farmer/coordinator can see previous questions and recommendations. |
| CARBON-ADV-005 | Future | Add push/local-language voice delivery | Blocked on notification and language workflow decisions. |

## Phase C5: Marketplace And Dealer Directory

Goal: Provide the dealer/lab discovery path from the app flow without depending
on live dealer integrations at first.

| ID | Status | Task | Acceptance |
| --- | --- | --- | --- |
| CARBON-MKT-001 | Pending | Add dealer/lab backend directory | Stores dealer/lab name, category, location, contact, services, and status. |
| CARBON-MKT-002 | Pending | Add marketplace browse UI | Filters for biological inputs, carbon inputs, soil labs, distance, and rating are available. |
| CARBON-MKT-003 | Pending | Add dealer profile screen | Shows products/services, contact details, location, and basic rating placeholder. |
| CARBON-MKT-004 | Future | Add order booking/request flow | Can remain inquiry-only until stock/order rules are approved. |
| CARBON-MKT-005 | Future | Add live stock sync | Blocked on dealer onboarding and integration decisions. |

## Phase C6: Carbon Calculator, Reports, And Admin Operations

Goal: Convert stored profile, soil, and activity data into management reports and
eventual credit-readiness workflows.

| ID | Status | Task | Acceptance |
| --- | --- | --- | --- |
| CARBON-CALC-001 | Blocked | Lock carbon methodology | Requires approved formula, baseline, eligibility, buffer, leakage, and verification rules. |
| CARBON-CALC-002 | Pending | Add provisional calculator service | Uses clearly marked provisional logic until `CARBON-CALC-001` is approved. |
| CARBON-CALC-003 | Pending | Add calculator UI | Shows CO2e estimate, score, practice impact, and assumptions. |
| CARBON-REP-001 | Pending | Add farmer carbon report | Farmer view summarizes profile, soil, activities, advisories, and estimated potential. |
| CARBON-REP-002 | Pending | Add FPO/admin carbon dashboard | Aggregates farmers, hectares, soil score, pending verifications, and potential credits. |
| CARBON-REP-003 | Pending | Add carbon Excel export | Export is scoped by tenant, role, date range, village, crop, and activity status. |

## Phase C7: External Integrations And Production Go-Live

Goal: Replace placeholders with production providers and operational controls.

| ID | Status | Task | Acceptance |
| --- | --- | --- | --- |
| CARBON-INT-001 | Blocked | OTP/SMS login | Needs provider, sender ID, message templates, pricing, and compliance approval. |
| CARBON-INT-002 | Blocked | Farm boundary map drawing | Needs map provider, cost model, offline needs, and accuracy expectations. |
| CARBON-INT-003 | Blocked | Satellite layers | Needs NDVI/NDRE/NDMI provider, refresh frequency, and storage rules. |
| CARBON-INT-004 | Blocked | AI image verification | Needs provider, confidence thresholds, rejection process, and audit policy. |
| CARBON-OPS-001 | Pending | Add production object storage setup | S3-compatible storage configured for advisory, soil, and activity evidence. |
| CARBON-OPS-002 | Pending | Add monitoring and backup runbooks | Uptime, logs, storage health, database backup, and restore drill are documented and tested. |
| CARBON-UAT-001 | Pending | Create Carbon UAT guide | End-to-end UAT scenarios cover login, profile, soil, activity, advisory, marketplace, calculator, and reports. |

## Recommended Next Sprint

Work in this order unless a client demo date changes priority:

1. `CARBON-MOD-002`: move Carbon-owned code into `src/modules/carbon`.
2. `CARBON-MOD-004`: add module visibility smoke tests.
3. `CARBON-SHELL-003`: complete the dashboard widgets from `App Flow.docx`.
4. `CARBON-PROFILE-001`: draft the durable carbon data dictionary.
5. `CARBON-SOIL-001`: add durable soil profile schema for Carbon.
6. `CARBON-ACT-001`: seed carbon activity categories.
7. `CARBON-UAT-001`: create Carbon UAT scenarios before client review.

## Demo-Ready Definition

The Carbon app is demo-ready when:

- Carbon-first navigation works without FPO operations visible.
- Farmer/FPO/admin users can see the Carbon journey end to end.
- Profile, soil, activity, advisory, dealer, calculator, and report screens have
  coherent data.
- Placeholder/provisional areas are labeled internally and do not claim verified
  credit issuance.
- Disabled modules hide cleanly and show no broken tabs.

## Production-Ready Definition

The Carbon app is production-ready when:

- Carbon data is stored in durable backend tables with tenant and role scoping.
- Soil reports and activity evidence use production object storage.
- Calculation methodology is approved and versioned.
- Reports and exports are generated from backend data.
- UAT is signed off by the client.
- Monitoring, backups, restore drill, and security checks are complete.
- Provider-dependent features have approved vendors and tested integrations.
