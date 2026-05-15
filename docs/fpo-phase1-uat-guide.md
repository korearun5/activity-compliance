# FPO Phase 1 UAT Guide

Last updated: 2026-05-15

This guide defines the acceptance path for the client-approved Phase 1 FPO MVP.
Run it after backend, frontend, Docker dependencies, and seed data are started
from a clean local or staging environment.

## Entry Criteria

- `npm run typecheck` passes.
- `npm run lint` passes.
- `cd backend` then `.\mvnw.cmd test` passes.
- `cd backend` then `.\mvnw.cmd -Pintegration-test verify` passes with Docker
  running.
- Backend health is `UP`.
- `ADMIN`, `FPO_MANAGER`, and `FIELD_COORDINATOR` users can login.
- The pilot FPO is created and the FPO module subscriptions are enabled.
- The clean-start runbook has been rehearsed at least once.

Current technical readiness as of 2026-05-15:

- Frontend typecheck and lint pass.
- Frontend npm audit passes with zero moderate-or-higher vulnerabilities.
- Backend unit tests pass with 40 tests.
- Backend integration profile passes with 87 Testcontainers-backed tests.
- Phase 1 UAT smoke test passes and verifies pilot data, authentication,
  role access, farmer self access, and disabled-module rejection.
- Docker compose default ports and PostgreSQL credentials have been checked.
- Full Docker stack is running locally: backend health is `UP` and frontend
  responds on `http://localhost:19006`.
- Local seed admin login succeeds and returns enabled Phase 1 modules.

## UAT Owners

- Client project owner.
- One nominated FPO manager from the pilot FPO.
- IT company technical owner for defect triage and environment support.

## UAT Test Data

Use only dummy farmer personal data during UAT rehearsal.

| Data Area | Required Value |
| --------- | -------------- |
| Village | Wagholi |
| Taluka | Haveli |
| District | Pune |
| State | Maharashtra |
| Crops | Paddy (Kharif), Wheat (Rabi) |
| Inputs | Urea, DAP, Paddy seeds (var. local) |
| Farmers | Minimum 5 dummy farmers with 10 digit mobile numbers |

Minimum dummy farmers:

| Name | Mobile | Village | Taluka | District | State | Category | Status |
| ---- | ------ | ------- | ------ | -------- | ----- | -------- | ------ |
| UAT Farmer 001 | 9000000001 | Wagholi | Haveli | Pune | Maharashtra | Marginal | Active |
| UAT Farmer 002 | 9000000002 | Wagholi | Haveli | Pune | Maharashtra | Small | Active |
| UAT Farmer 003 | 9000000003 | Wagholi | Haveli | Pune | Maharashtra | Semi-medium | Active |
| UAT Farmer 004 | 9000000004 | Wagholi | Haveli | Pune | Maharashtra | Medium | Active |
| UAT Farmer 005 | 9000000005 | Wagholi | Haveli | Pune | Maharashtra | Large | Active |

## Acceptance Scenarios

| ID | Scenario | Expected Result | Status |
| -- | -------- | --------------- | ------ |
| UAT-01 | Login as `ADMIN` | Admin can access platform-level data and pilot FPO data. | Pending |
| UAT-02 | Login as `FPO_MANAGER` | Manager can access only assigned FPO data. | Pending |
| UAT-03 | Login as `FIELD_COORDINATOR` | Coordinator can work only with assigned FPO/villages/farmers. | Pending |
| UAT-04 | Create five dummy farmers | Farmers save with full name, mobile, village, taluka, district, state, gender, category, and active status. | Pending |
| UAT-05 | Validate farmer mobile | Invalid non-10-digit Indian mobile number is rejected. | Pending |
| UAT-06 | Add optional Aadhaar | Aadhaar can be left blank; invalid value is rejected if entered. | Pending |
| UAT-07 | Add soil profile values | SOC, pH, N, P, K can be entered when a soil report exists; blank values are allowed. | Ready |
| UAT-08 | Add optional soil report link | PDF/image report link and metadata can be saved and does not trigger carbon calculation. | Ready |
| UAT-09 | Create land/GPS record | Survey number, acres, ownership, irrigation, latitude, and longitude persist. | Ready |
| UAT-10 | Verify GPS point only | No polygon/boundary drawing is required or shown as a blocker. | Pending |
| UAT-11 | Configure crop master data | Paddy, Wheat, and approved categories are available. | Pending |
| UAT-12 | Configure seasons | Kharif, Rabi, and Summer/Zaid with approved months and crop year labels are available. | Pending |
| UAT-13 | Add crop history | Previous crop is required; yield is optional with supported units. | Pending |
| UAT-14 | Add draft crop plan | Draft crop plan does not affect input demand. | Pending |
| UAT-15 | Confirm crop plan | Confirmed crop plan records a confirmed timestamp and becomes eligible for input demand. | Pending |
| UAT-16 | Complete or cancel crop plan | Completed and cancelled statuses persist; cancelled plans are excluded from demand. | Pending |
| UAT-17 | Configure inputs | Urea, DAP, and Paddy seeds exist with Seed/Fertilizer type and approved units. | Pending |
| UAT-18 | Configure per-acre defaults | Admin/FPO manager can edit fixed per-acre recommendations. | Pending |
| UAT-19 | Run input demand | Only confirmed plans are included; final demand includes 5% buffer and round-up. | Pending |
| UAT-20 | Export Farmer Register | Excel sheet contains the approved columns and optional date range uses farmer `created_at`. | Pending |
| UAT-21 | Export Crop Plan Summary | Excel sheet contains approved columns and optional date range uses crop plan `created_at` or `updated_at`. | Pending |
| UAT-22 | Export Input Demand | Excel sheet contains approved columns and optional date range uses crop plan `confirmed_at`. | Pending |
| UAT-23 | Verify report branding | Workbook shows logo when provided or approved text, plus confidential footer. | Pending |
| UAT-24 | Create text advisory | Advisory title, message, category, and all-member target persist. | Pending |
| UAT-25 | Create crop-targeted advisory | Advisory can target Paddy or Wheat farmers through crop context. | Pending |
| UAT-26 | Attach advisory images | One or more images upload to storage and display as thumbnails or links. | Pending |
| UAT-27 | Verify no Phase 2 channels | SMS, WhatsApp, video, and AI analysis are not required in Phase 1. | Pending |
| UAT-28 | Disabled module check | Disabled module returns `MODULE_NOT_ENABLED` and UI hides it. | Pending |
| UAT-29 | Clean restart rehearsal | Stop, clean, restart, and login flow works from the clean-start runbook. | Pending |

## API Smoke Checks

Run these against a staging/local backend with an admin token:

- `GET /api/v1/platform/modules/enabled`
- `GET /api/v1/fpo/members`
- `GET /api/v1/fpo/crops`
- `GET /api/v1/fpo/seasons`
- `GET /api/v1/fpo/crop-plans`
- `GET /api/v1/fpo/demand-estimates/summary`
- `GET /api/v1/fpo/advisories`
- `GET /api/v1/fpo/reports/summary`

Expected:

- Authenticated staff receives `200`.
- Unauthenticated requests receive `401`.
- Unauthorized roles receive `403`.
- Disabled modules receive `MODULE_NOT_ENABLED`.
- FPO manager and field coordinator requests cannot cross assigned FPO scope.

Automated coverage:

- Backend happy-path smoke coverage exists in `FpoPhase1UatSmokeIT`. It seeds
  the approved pilot shape with five Wagholi dummy farmers, Paddy/Wheat,
  Kharif/Rabi, land/GPS, one confirmed crop plan, input demand, advisory, and
  verifies the core Phase 1 API smoke endpoints, unauthenticated rejection,
  `ADMIN`/`FPO_MANAGER`/`FIELD_COORDINATOR`/`FARMER` role access, and disabled
  module rejection.
- The 2026-05-15 smoke run passed locally after fixing the fixture to issue the
  farmer JWT from the concrete seeded user instead of a lazy member relation.
- Focused Testcontainers coverage also exists for farmer profile validation,
  land/GPS, soil profiles, crop planning, input demand, report export, and
  advisory crop targeting/image links. Manual UAT still needs to be executed
  with the client and nominated FPO manager.

## Exit Criteria

Phase 1 can move from implementation-ready to UAT-ready when:

- All implementation gaps in the Phase 1 decision register are closed or have
  accepted sign-off notes.
- All automated checks pass.
- All UAT scenarios above pass or have accepted client sign-off notes.
- Excel reports match the approved sheets, columns, filters, branding, and
  footer.
- Production/staging backup and restore process is rehearsed.
- Monitoring and alerting owners are assigned.
- Open issues are logged with severity and owner.
