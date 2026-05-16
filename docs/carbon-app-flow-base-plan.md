# Carbon Accounting App Flow Base Plan

This document translates the client-provided `App Flow.docx` into a practical
development base. It is intentionally implementation-oriented and uses dummy
data where client/provider inputs are still pending.

Source reviewed:

- `C:\Users\korea\Downloads\App Flow.docx`
- Client theme: regenerative agriculture and carbon farming platform.
- Key product promise: "Measure. Improve. Earn from Soil Carbon."

## Build Direction

The current repository already has a strong activity-compliance and FPO
foundation: users, roles, activity timelines, evidence upload, review, FPO
members, farm plots, crop planning, input demand, report exports, module flags,
and advisory backend APIs.

For the carbon accounting app, the safest base is to layer carbon-specific
screens and data contracts over the existing foundation first, then add durable
backend tables once the client freezes the carbon methodology and third-party
providers.

## What Can Proceed Without Client Intervention

The following areas can be built with dummy data now:

| Client Flow Area            | Base We Can Build Now                                               | Notes                                                             |
| --------------------------- | ------------------------------------------------------------------- | ----------------------------------------------------------------- |
| Splash and UI language      | Store app UI language preference and prepare labels                  | English, Hindi, and Marathi are app-level UI choices, not Carbon profile fields. |
| Farmer/FPO/agronomist roles | Reuse current admin, supervisor, participant roles                  | Add agronomist role only if client wants separate permissions.    |
| Farmer profile              | Extend FPO member profile with carbon identity fields               | Dummy Aadhaar/bank/document status can be shown now.              |
| Farm GPS location           | Reuse plot latitude/longitude fields                                | Boundary drawing needs map provider later.                        |
| Soil profile                | Add dummy soil score, SOC, pH, EC, NPK, texture, biological metrics | Backend tables should wait for final soil report fields.          |
| Activity tracking           | Reuse current activity/evidence workflow                            | Carbon activity categories can be seeded now.                     |
| Activity evidence           | Reuse proof photo upload/review                                     | AI verification remains provider-dependent.                       |
| Advisory                    | Use existing FPO advisory backend and new frontend UI               | SMS/WhatsApp/voice delivery remains provider-dependent.           |
| Nearby dealers              | Dummy directory and filters                                         | Real dealer onboarding and GPS search need client data/provider.  |
| Carbon calculator           | Show estimated CO2e, score, and potential from dummy metrics        | Final formula/methodology must be approved before production use. |
| Reports                     | Reuse report/export foundation                                      | Add carbon-specific export after schema is finalized.             |
| Admin portal                | Add carbon operations dashboard                                     | Current admin shell is suitable.                                  |

## Current Update

Implemented in the codebase now:

- Added a typed dummy carbon data layer in
  `src/modules/carbon/data/carbonStore.ts`.
- Added a generic client package switch with Carbon enabled by default and FPO
  operations hidden by default.
- Added a `src/modules` registry and moved Carbon screens/data behind the
  `src/modules/carbon` module entry point.
- Added durable Carbon foundation schema for Carbon profiles, farm plots, soil
  profiles, and activity categories.
- Added [Carbon Data Dictionary](carbon-data-dictionary.md) and
  [Carbon UAT Guide](carbon-uat-guide.md).
- Added an admin `Carbon` tab showing:
  - farm area,
  - carbon credit potential,
  - pending verification,
  - App Flow dashboard widgets for soil carbon score, advisory alerts, weather
    snapshot, and nearby dealers,
  - farmer carbon identity,
  - soil profile pipeline,
  - activity verification queue,
  - dealer/lab directory.
- Added a farmer `Carbon` tab showing:
  - carbon identity,
  - soil score and SOC,
  - carbon potential,
  - pending farm activities,
  - weather snapshot,
  - activity scoring,
  - advisory alerts,
  - nearby dealers/labs.
- Added frontend advisory management backed by the existing backend advisory
  APIs with local dummy fallback:
  - create advisory,
  - target all members, village, or a member,
  - attach crop/season context,
  - publish/archive advisory.

Current verification includes frontend typecheck, lint, npm audit, Docker
Compose config validation, and local Docker stack startup checks.

## Working Dummy Assumptions

Until client data is supplied, the base uses:

- UI languages: English, Hindi, Marathi. This controls app copy and labels for
  the logged-in user; it is not farmer/profile data.
- Farmer profile fields: carbon identity ID, village, taluka, district, GPS,
  landholding, irrigation, cropping pattern, livestock count, tillage status,
  bank status, and document status.
- Soil fields: SOC, pH, EC, NPK, bulk density, texture, microbial count, NDVI,
  NDRE, NDMI, soil health score, and estimated CO2e potential.
- Activity categories: land preparation, sowing, fertigation, irrigation,
  biological application, compost addition, pruning biomass incorporation, and
  harvesting.
- Advisory categories: pest issue, nutrient deficiency, irrigation advice,
  carbon farming practice, and biological dosage.
- Dealer categories: biological inputs, carbon farming inputs, and soil testing
  labs.

## Provider Or Client Decisions Still Needed

These should not be hardcoded yet:

- OTP/SMS provider and sender template rules.
- Map provider and whether Phase 1 needs boundary drawing or GPS points only.
- AI image verification provider and acceptable confidence rules.
- Satellite/NDVI/NDRE/NDMI provider and refresh frequency.
- Carbon methodology, formula, buffer/rounding, and credit eligibility rules.
- Soil report format and whether lab PDFs need parsing.
- Dealer onboarding source, stock sync, rating policy, and order booking rules.
- Voice advisory and local language content workflow.
- Bank payout and carbon credit payment flow.
- AWS vs Azure production hosting preference.

## Detailed Task Roadmap

Task IDs, phase sequencing, and next sprint order now live in
[Carbon App Flow Task Roadmap](carbon-app-flow-task-roadmap.md). Keep execution
status there so this base plan remains focused on assumptions and build
direction.

## Phase 1 Definition Of Done For Carbon Base

The carbon base is useful when:

- Farmer carbon identity can be created and viewed.
- Farm/plot GPS is captured.
- Soil profile can be recorded.
- Activity evidence can be submitted and reviewed.
- Carbon score/potential is visible with an approved formula.
- Advisories can be published and viewed by farmers.
- Dealer/lab directory is searchable.
- Admin dashboard summarizes participation, soil health, activities, and carbon
  potential.
- Report export is available for FPO/admin review.
- Provider-dependent items are clearly isolated behind replaceable adapters.
