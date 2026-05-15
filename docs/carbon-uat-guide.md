# Carbon UAT Guide

Last updated: 2026-05-16

This guide defines the manual acceptance path for the Carbon-first app package
based on `App Flow.docx`. It is separate from the FPO Phase 1 UAT guide because
Carbon is now a client-facing package, while FPO can remain hidden or separately
licensed.

## Entry Criteria

- `npm run typecheck` passes.
- `npm run lint` passes.
- `npm run test:module-visibility` passes.
- `cd backend` then `.\mvnw.cmd test` passes.
- `cd backend` then `.\mvnw.cmd -Pintegration-test verify` passes with Docker
  running.
- Docker Compose config is valid.
- Frontend package config includes `carbon`:
  `EXPO_PUBLIC_ENABLED_CLIENT_MODULES=carbon`.
- Backend tenant has `SUSTAINABILITY` enabled.
- Carbon methodology remains marked provisional or unavailable until approved.

## UAT Owners

- Client project owner.
- Pilot FPO/FPC representative if Carbon is demonstrated through an FPO.
- IT company product/technical owner.
- Agronomist/carbon domain reviewer when soil and activity wording is reviewed.

## UAT Test Data

Use dummy data only during UAT rehearsal.

| Data Area | Suggested Value |
| --- | --- |
| State | Maharashtra |
| District | Pune |
| Taluka | Haveli |
| Village | Wagholi |
| Farmer | Carbon Farmer 001 |
| Mobile | 9000000101 |
| Carbon identity | CCI-MH-2026-001 |
| Farm area | 4.50 acres |
| Crop | Paddy or Wheat |
| Soil values | SOC 0.72, pH 6.8, EC 0.42, N 214, P 18, K 278 |
| Activity examples | Compost Addition, Biological Application, Irrigation |
| Dealer examples | Biological input dealer, soil testing lab |

## Acceptance Scenarios

| ID | Scenario | Expected Result | Status |
| --- | --- | --- | --- |
| CARBON-UAT-01 | Open Carbon-first app package | FPO operations are hidden when package config is `carbon`. | Pending |
| CARBON-UAT-02 | Verify module guard | Carbon tab appears only when frontend package includes `carbon` and backend `SUSTAINABILITY` is enabled. | Ready |
| CARBON-UAT-03 | View Carbon dashboard widgets | Dashboard shows total farm area, soil carbon score, credit potential, pending activities, advisory alerts, weather snapshot, and nearby dealers. | Ready |
| CARBON-UAT-04 | View Carbon identity | Carbon identity ID, farmer name, village/taluka, landholding, tillage, bank status, and document status are visible. | Pending |
| CARBON-UAT-05 | Review farm/GPS data | Farm area and GPS point are captured; map boundary drawing is not required. | Pending |
| CARBON-UAT-06 | Review soil profile values | SOC, pH, EC, N, P, K, bulk density, texture, and optional biological fields are accepted as stored data only. | Pending |
| CARBON-UAT-07 | Upload or link soil report | PDF/image metadata can be stored without triggering carbon calculation. | Pending |
| CARBON-UAT-08 | View activity categories | Land preparation, sowing, fertigation, irrigation, biological application, compost addition, pruning biomass incorporation, and harvesting categories are available. | Ready |
| CARBON-UAT-09 | View activity evidence placeholder | Activity rows show category, crop/input/quantity, evidence count, and verification status. | Ready |
| CARBON-UAT-10 | Confirm no verified credit claim | UI does not claim verified credits or final payable value before methodology approval. | Pending |
| CARBON-UAT-11 | View advisory alerts | Published advisory title/message appears in the Carbon dashboard. | Ready |
| CARBON-UAT-12 | View weather snapshot | Weather widget appears as placeholder/demo data until provider integration is approved. | Ready |
| CARBON-UAT-13 | View nearby dealers/labs | Dealer/lab list appears with category, distance, rating, stock status, and products. | Ready |
| CARBON-UAT-14 | Disable Carbon package | Carbon tab hides cleanly when frontend package excludes `carbon`. | Ready |
| CARBON-UAT-15 | FPO-only upsell path | FPO-only package shows "Get Carbon Credits" upsell instead of broken Carbon screens. | Ready |
| CARBON-UAT-16 | Source-handover review | If source handover is planned, confirm Carbon distribution excludes unlicensed FPO implementation. | Pending |

## API And Data Checks

Current Carbon durable schema is database-only foundation. Until Carbon APIs are
implemented, validate through Flyway/integration startup and direct schema
inspection in staging/local database.

Expected tables after migration:

- `carbon_profiles`
- `carbon_farm_plots`
- `carbon_soil_profiles`
- `carbon_activity_categories`

Expected seeded category count:

- 8 active rows in `carbon_activity_categories`.

## Exit Criteria

Carbon UAT can move to client demo/sign-off when:

- All entry criteria pass.
- Carbon-first package hides FPO operations cleanly.
- Dashboard widgets match the App Flow shell.
- Carbon identity, farm, soil, activity-category, advisory, weather, and dealer
  flows are visible with coherent dummy or staged data.
- Carbon methodology, OTP, maps, AI, satellite, dealer ordering, and payments
  are explicitly marked future/provider-dependent.
- All defects are logged with severity and owner.
