# Carbon Flow Client Excel Impact Analysis

Last audited: 2026-05-19

Source file: `C:\Users\korea\Downloads\Carbon Flow.xlsx`

Purpose: document what the latest client Excel changes for our current project
direction before we do more implementation. This is an analysis document only.

## Important Interpretation

The word `Vineyard` in the Excel should be treated as the farmer's plot/block
context for this carbon app, not automatically as a separate frontend module.

Recommended direction:

- Keep the active client package as the Carbon app.
- Use `vineyard plot`, `vineyard block`, or `farm plot` as product/domain
  wording inside the Carbon app.
- Do not create a separate `vineyard` module toggle unless the business later
  wants Vineyard as an independently licensed module.
- Keep FPO code intact and hidden through the existing package switch when the
  Carbon client does not need FPO screens.

## Excel Summary

Workbook structure:

- Sheets: `Sheet1`
- Columns: `Screen No.`, `Screen Name`, `Purpose`, `Main UI Components`,
  `Primary Actions`, `Navigation Flow`, `Important States`
- Numbered rows found: 77

Client mismatch to clarify:

- The note says 76 screens.
- The workbook has 77 numbered screens.
- Screen 77 is `Logout Confirmation`.
- Treat this as a clarification item, not a blocker.

## Executive Position

We should not restart the whole project.

The existing foundation is still useful and should be kept:

- JWT auth and platform roles.
- Canonical farmer identity:
  `users -> farmer_profiles -> module extension records`.
- `FarmerService`.
- Workflow/activity/evidence engine.
- Storage adapter for report/evidence uploads.
- Carbon profile, plot, soil, and activity-record foundation.
- Central module/role visibility registry.
- FPO module code for future clients.

The client Excel is much broader than our current Carbon prototype. It turns
the app into a full farmer mobile experience plus admin operations:

- Mobile/OTP onboarding.
- Plot/block mapping.
- Soil, satellite, weather, advisory, marketplace, wallet, carbon calculator,
  and admin verification screens.

So the correct response is not "delete everything"; it is:

1. Keep the shared foundation.
2. Reuse existing Carbon backend pieces where they fit.
3. Rework the Carbon UI around the Excel flow.
4. Add missing domain tables only where current tables are not enough.
5. Avoid creating duplicate farmer identity or duplicate plot identity.

## What We Already Have

| Area | Current State | Keep? | Notes |
| ---- | ------------- | ----- | ----- |
| Auth/JWT | Username/password backend login exists. | Yes | OTP can be added as a login initiation/verification layer, then continue issuing JWT. |
| Roles | `ADMIN`, `FPO_MANAGER`, `FIELD_COORDINATOR`, `FARMER` exist. | Yes | Map client names onto these roles instead of creating new role names. |
| Farmer identity | `farmer_profiles`, `FarmerService`, FPO/Carbon links, participant registry exist. | Yes | This is the strongest foundation and must remain. |
| FPO module | Full FPO Phase 1 module exists. | Yes | Keep for future; hide for Carbon-only clients. Do not delete. |
| Module visibility | `carbon`, `fpo`, `carbon,fpo` package switch exists. | Yes | Do not add `vineyard` yet unless it becomes a separate licensed product module. |
| Carbon profile | `carbon_profiles` API/UI exists with farmer enrollment fields. | Rework carefully | New farmer identity should resolve through `farmer_profiles`; do not add more duplicated identity fields. |
| Plot/block | `carbon_farm_plots` exists with GPS point and `boundary_geojson`. | Reuse | Add vineyard-specific fields like variety/rootstock if needed; do not create a duplicate plot table unless current table cannot evolve safely. |
| Soil | `carbon_soil_profiles` exists with SOC, pH, EC, NPK, bulk density, texture, report metadata/upload. | Reuse | Good match for soil screens; OCR/satellite/recommendation are missing. |
| Carbon activity categories/records | `carbon_activity_categories` and `carbon_activity_records` exist. | Partial reuse | Excel wants workflow/wizard plus specialized activity forms; use workflow engine for actual activity tracking. |
| Workflow/activity/evidence | Generic workflow definitions, activities, tasks, proof/evidence upload/review exist. | Yes | Use this for pruning, irrigation, spray, compost, cover crop workflows. |
| Reports | Report/export foundation exists; FPO Excel exports are strong. | Reuse | Carbon PDF/Excel reports need new templates and data sources. |
| Notifications | Basic notification backend/frontend exists. | Partial reuse | Weather alerts/support/offline sync are not complete. |
| Admin shell | Admin dashboard, users, workflows, evidence review, Carbon/FPO tabs exist. | Reuse | Needs client-specific admin modules and queues. |

## What We Do Not Have Yet

Major missing items from the Excel:

- Splash/language/onboarding flow.
- Real English/Hindi/Marathi localization.
- Mobile OTP login and mock SMS flow.
- Consent/permissions screens.
- Import existing data flow.
- Separate bank-details table and UI.
- Document upload/KYC records linked to farmer profile.
- Polygon drawing UI with map provider.
- React Native maps integration.
- Vineyard variety/rootstock/planting fields.
- Crop-stage dashboard and season calendar.
- Multi-step activity wizard.
- Specialized pruning/irrigation/spray/compost/cover-crop forms.
- Mock AI verification result flow.
- Advisory chat / Ask Expert / AI diagnosis.
- Marketplace/dealer/cart/checkout.
- Carbon calculator formulas.
- Wallet and transaction history.
- Certificate generation.
- Weather provider and spray-window logic.
- Satellite NDVI/NDRE/NDMI layers.
- Settings/help/support/offline sync flows.
- Admin farmer verification queue.
- Soil verification admin queue.
- Carbon project/dealer/agronomist admin modules.

## What Should Be Reused

Reuse without hesitation:

- `farmer_profiles` and `FarmerService`.
- `carbon_farm_plots` as the starting point for vineyard plots/blocks.
- `carbon_soil_profiles` as the starting point for soil report/manual entry.
- Existing storage adapter for soil reports, documents, and evidence.
- Workflow definitions and activities for recurring farming tasks.
- Evidence upload/review for activity proof and admin verification.
- Tenant/module visibility checks.
- Existing FPO module as dormant future functionality.

Reuse with changes:

- `carbon_profiles`: keep as Carbon enrollment, but avoid making it the new
  farmer identity source. Farmer identity belongs in `farmer_profiles`.
- `carbon_activity_records`: useful for simple Carbon logs, but the Excel
  activity wizard should probably create workflow activities/evidence and then
  optionally write Carbon-specific summary records.
- Admin Carbon tab: keep the backend wiring, but reorganize the UI to match the
  client flow after the UX is approved.

## What Needs To Change

### 1. Product Shape

Current app:

```text
Carbon module + FPO module + shared platform
```

Client Excel wants:

```text
Carbon farmer app
  + vineyard/plot/block management
  + workflow activity wizard
  + evidence verification
  + admin verification
  + future marketplace/wallet/satellite/weather
```

Recommended implementation shape:

```text
src/modules/carbon/
  screens/
    onboarding/
    farmer/
    plots/
    soil/
    activities/
    advisory/
    marketplace/
    wallet/
    reports/
    admin/
```

Do not create `src/modules/vineyard` unless Vineyard becomes its own
independently sold package.

### 2. Plot/Vineyard Data

Current `carbon_farm_plots` already has:

- farm name
- survey number
- area
- latitude/longitude
- `boundary_geojson`
- irrigation source
- primary crop
- tillage status
- status

Likely add fields:

- variety
- rootstock
- planting year/date
- spacing/row details
- block code
- soil score summary
- polygon capture metadata

Recommendation:

- Extend `carbon_farm_plots` rather than creating a separate duplicate
  `vineyard_blocks` table.
- UI can call it `Vineyard Block` or `Farm Plot`.

### 3. Bank And Documents

Current Carbon profile has status-only fields:

- bank status
- Aadhaar status
- document status

Client Excel needs real screens:

- Bank Details
- Document Upload
- KYC/verification

Recommendation:

- Add new tables linked to `farmer_profiles`:
  - `farmer_bank_details`
  - `farmer_documents`
- Do not store bank/document data inside `carbon_profiles`.

### 4. OTP Login

Current state:

- Username/password login works.
- JWT sessions work.

Client Excel needs:

- Mobile number login.
- OTP verification.

Recommendation:

- Add OTP request/verify endpoints.
- Use mock OTP first.
- After OTP verification, issue the same JWT session.
- Do not remove username/password until client confirms OTP-only behavior for
  every role.

### 5. Activity Wizard

Current state:

- Workflow definitions exist.
- Activities and tasks exist.
- Evidence upload/review exists.
- Carbon activity records exist.

Client Excel needs:

- Wizard steps 28-34.
- Specialized screens 35-39.

Recommendation:

- Use workflow definitions for:
  - Pruning
  - Irrigation
  - Spray
  - Compost
  - Cover Crop
- Add Carbon/vineyard-specific wizard UI that starts workflow activities and
  uploads evidence.
- Avoid a second activity engine.

## What Needs To Be Reworked

| Current Item | Why Rework | Direction |
| ------------ | ---------- | --------- |
| Carbon UI layout | Current UI is admin/profile-form focused, not 77-screen farmer app flow. | Redesign Carbon frontend around Excel navigation. |
| Carbon profile identity fields | Some farmer identity fields exist in Carbon profile; canonical identity now belongs in `farmer_profiles`. | Treat Carbon fields as enrollment/module fields or snapshots; do not add more identity duplication. |
| Carbon activity records | Good for simple logs, but not enough for wizard/evidence/verification. | Use workflow/activity/evidence as source of truth for client activity tracking. |
| Farmer login UX | Current login is username/password. | Add mobile OTP flow while keeping JWT session. |
| Plot UI | Current forms support plot data, but not polygon drawing. | Add map/polygon UI after provider decision. |
| Reports | Existing reports are FPO-oriented and generic. | Add Carbon/vineyard reports based on activity, soil, and calculator data. |

## What Might Need Revert

Do not revert:

- `farmer_profiles`
- `FarmerService`
- Farmer participant registry
- Common farmer activity dashboard
- Carbon profile/plot/soil/activity backend foundation
- FPO module

Revert or avoid if present in any branch:

- Any code that creates `vineyard` as a separate `ClientModuleId` only because
  the Excel says vineyard.
- Any env default like `EXPO_PUBLIC_ENABLED_CLIENT_MODULES=vineyard,carbon`
  unless the business explicitly decides Vineyard is a separate package module.
- Any `src/modules/vineyard` folder created before this analysis is approved.
- Any duplicated farmer creation flow that bypasses `FarmerService`.
- Any new plot/block table that duplicates `carbon_farm_plots` without a clear
  reason.

Current checked repo appears to be back to:

- `ClientModuleId = "carbon" | "fpo"`
- `.env.example` using `EXPO_PUBLIC_ENABLED_CLIENT_MODULES=carbon`
- no separate vineyard module required for this analysis

## Screen Group Mapping

| Excel Screens | Area | Current Coverage | Decision |
| ------------- | ---- | ---------------- | -------- |
| 1-5 | Splash, language, onboarding | Not implemented as client flow. | Build later in Carbon frontend. |
| 6-7 | Mobile login and OTP | Not implemented; username/password exists. | Add OTP layer, keep JWT. |
| 8-10 | Registration, consent, import | Registration/profile partial; consent/import missing. | Rework onboarding. |
| 11-12 | Home dashboard, notifications | Dashboard and notifications partial/generic. | Build client-specific dashboard. |
| 13-15 | Farmer profile, bank, documents | Farmer identity exists; bank/docs only status-level. | Add bank/document tables and screens. |
| 16-19 | Farm/vineyard plot list, mapping, detail | `carbon_farm_plots` exists with GPS and boundary JSON; map UI missing. | Extend plot fields and add map UI. |
| 20-24 | Soil dashboard/upload/manual/satellite/recommendation | Soil API/UI/upload partial; satellite/recommendation missing. | Reuse soil, add satellite later. |
| 25-27 | Crop stage/calendar/activity dashboard | Workflow/activity foundation exists; crop-stage/calendar UI missing. | Build around workflows. |
| 28-34 | Activity wizard | Not implemented as wizard. | Build using workflow/activity/evidence. |
| 35-39 | Specialized activity forms | Categories partially overlap; forms missing. | Build module-specific form templates. |
| 40-44 | Advisory/Ask Expert/chat/AI diagnosis | FPO advisory exists; chat/AI missing. | Add mock advisory/chat backend later. |
| 45-50 | Marketplace/dealer/cart/checkout | Not implemented. | Future module/table set. |
| 51-53 | Carbon calculator/practices/result | Not implemented; methodology not fixed. | Placeholder only until methodology. |
| 54-57 | Reports | Reporting foundation exists; Carbon reports missing. | Add Carbon/vineyard report templates. |
| 58-60 | Wallet/transactions/certificate | Not implemented. | Future, likely mock first. |
| 61-63 | Weather/alerts/notification center | Notifications partial; weather missing. | Need provider/mock. |
| 64-67 | Settings/help/support/offline sync | Mostly missing. | Later mobile hardening. |
| 68-74 | Admin modules | Admin shell/evidence review partial; domain queues missing. | Build after farmer/domain APIs. |
| 75-77 | Success/error/logout | Generic patterns exist, not formal screens. | Add shared UI states as needed. |

## Completion Estimate Against This Excel

These are rough engineering confidence estimates, not client acceptance:

| Area | Estimate | Reason |
| ---- | -------: | ------ |
| Shared platform foundation | 70-75% | Identity, auth, workflow, evidence, storage, reporting, roles exist. |
| Carbon backend data foundation for this Excel | 35-45% | Profile, plot, soil, activity records exist; bank/docs/OTP/marketplace/wallet/satellite missing. |
| Carbon farmer mobile UX for this Excel | 10-15% | Current UI is not the 77-screen onboarding/dashboard/wizard flow. |
| Admin operations for this Excel | 20-30% | Admin shell exists, but verification/project/dealer/agronomist queues are missing. |
| Full Excel delivery | 20-25% | Strong foundation, but most client-facing screens and integrations remain. |

## Recommended Next Steps

Do this before writing more code:

1. Share/confirm this interpretation internally:
   - Vineyard means plot/block context.
   - Not a separate module unless business decides later.
2. Ask the client only the high-impact clarification questions below.
3. Convert this document into task tickets after answers are known.
4. Start with backend-safe Phase 1 deltas:
   - plot/block missing fields
   - bank details
   - document records
   - OTP placeholder
5. Then redesign frontend around the Excel flow.

## Client Clarifications Needed

Ask before locking implementation:

1. The workbook says 77 numbered screens but the note says 76. Should Logout
   Confirmation be counted as a separate screen?
2. Should login be OTP-only for farmers, or should username/password remain for
   staff/admin?
3. Which SMS/OTP provider should be used after mock OTP?
4. Which map provider should be used for polygon drawing?
5. Is polygon drawing required offline, or online-only is acceptable?
6. Which document types are required for KYC/carbon verification?
7. Are bank details only stored for future payouts, or do they need verification?
8. Are weather, satellite, OCR, and AI diagnosis expected to be mock in first
   delivery?
9. Is marketplace checkout real ordering/payment or UI-only for first release?
10. Is wallet/transaction history mock, manually entered, or integrated with
    payment/carbon-credit systems?
11. Is carbon calculator allowed to use dummy formulas until methodology is
    fixed?
12. What exact admin users will perform farmer, activity, soil, project, dealer,
    and agronomist review?

## Bottom Line

We are not on the wrong road, but the client Excel is bigger and more specific
than the earlier Carbon prototype.

The right technical move is:

- keep the foundation,
- keep FPO intact,
- do not create a separate Vineyard module yet,
- treat vineyard as plot/block domain wording inside Carbon,
- reuse Carbon plot/soil/activity foundations,
- add missing OTP, bank, documents, polygon, wizard, marketplace, wallet,
  satellite, weather, and admin queues in planned phases.
