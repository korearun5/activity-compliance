# FPO Phase 1 Data Dictionary

Last updated: 2026-05-15

This data dictionary reflects the client-approved Phase 1 scope. It should be
used with the [Phase 1 Client Decision Register](phase1-client-decision-register.md)
when implementing backend fields, frontend forms, validation, reports, and UAT
checks.

## Implementation Status

| Area | Phase 1 Decision | Current Implementation Status |
| ---- | ---------------- | ----------------------------- |
| Roles | `ADMIN`, `FPO_MANAGER`, `FIELD_COORDINATOR`, `FARMER`; farmer username/password login included, OTP excluded | Role foundation, farmer workflow routing, member ownership, coordinator assignment, and focused role isolation tests are aligned. |
| Farmer profile | Final mandatory fields implemented | Taluka, state, optional Aadhaar, approved gender/category values, Indian mobile normalization, suspended status, and farmer user linkage are aligned. |
| Soil profile | Required for existing lab reports; blank allowed | Phase 1 backend API, schema, admin entry UI, optional report link/metadata, and tests are implemented without carbon calculation. |
| Land/GPS | GPS point only, acres, approved ownership/irrigation values | Phase 1 schema, API validation, admin UI controls, and tests are aligned. |
| Crop planning | Crop list, seasons, crop year, and statuses approved | Phase 1 crop year label, optional expected yield, confirmation timestamp, UI fields, API contracts, and tests are aligned. |
| Input demand | Confirmed plans only, fixed per-acre defaults, 5% buffer | Calculation now uses confirmed plans only, stores total demand, 5% buffer, and rounded final demand; exact three-sheet report output remains in the report alignment task. |
| Advisory | Text plus multiple images; all members or crop target | Foundation exists; image attachments and crop targeting need alignment. |
| Reports | Excel only, three approved sheets, filters, footer/branding | Workbook emits the approved three sheets and columns; village/crop/season/coordinator/date filters plus header/footer branding are wired. |
| UAT | Pilot data and sign-off owners approved | UAT guide created; execution pending. |

## Roles

| Role | Required Scope |
| ---- | -------------- |
| `ADMIN` | Platform super user. Can see all FPOs, all users, and future modules. |
| `FPO_MANAGER` | One-FPO staff role. Can manage only assigned FPO data. |
| `FIELD_COORDINATOR` | Field staff role. Can manage assigned villages/farmers and enter data on farmer behalf. |
| `FARMER` | Farmer/member login role. Can use username/password in Phase 1 to view own member data and use own workflow/activity area. OTP remains Phase 2. |

Creation rules:

- `ADMIN` remains seed/manual in Phase 1.
- Staff user creation creates `FPO_MANAGER` or `FIELD_COORDINATOR` logins only.
- Farmer profile creation creates or links the `FARMER` login and the
  farmer/FPO member profile together.
- Do not assign `FARMER` through generic role management because it would bypass
  mandatory farmer profile fields.

## Farmer Profile

| Field | Required | Validation / Values | Notes |
| ----- | -------- | ------------------- | ----- |
| FPO | Yes | Existing FPO reference | Required for FPO manager isolation. |
| Full name | Yes | Non-empty text | Display name in UI and reports. |
| Mobile number | Yes | 10 digit Indian mobile number | Unique within FPO/tenant. |
| Aadhaar number | No | 12 digits if provided | Mask in UI except last 4 digits and exclude from Phase 1 exports. |
| Village | Yes | Text/dropdown | Report filter and grouping. |
| Taluka | Yes | Text/dropdown | Preferred label; do not use separate block label. |
| District | Yes | Text/dropdown | Required for reports. |
| State | Yes | Text/dropdown | Required for reports. |
| Gender | Yes | `Male`, `Female`, `Other` | Exact display labels. |
| Farmer category | Yes | `Marginal`, `Small`, `Semi-medium`, `Medium`, `Large` | Based on landholding size. |
| Status | Yes | `Active`, `Inactive`, `Suspended` | Default `Active`. |
| Login user | Yes | `FARMER` user reference | Reusable account link for username/password login now and OTP/mobile login later. |
| Coordinator | No | `FIELD_COORDINATOR` reference | Used for filters and operational ownership. |

Farmer category definitions:

| Category | Definition |
| -------- | ---------- |
| `Marginal` | Less than 1 ha |
| `Small` | 1 to 2 ha |
| `Semi-medium` | 2 to 4 ha |
| `Medium` | 4 to 10 ha |
| `Large` | More than 10 ha |

## Soil Profile

Soil data is collected only when a farmer already has a soil test report. It is
stored for future use and must not drive carbon calculations in Phase 1.

| Field | Required | Validation / Values | Notes |
| ----- | -------- | ------------------- | ----- |
| SOC | No | Non-negative decimal | Soil organic carbon from lab report. |
| pH | No | Decimal from 0 to 14 | Lab report value. |
| Nitrogen (N) | No | Non-negative decimal | Lab report value. |
| Phosphorus (P) | No | Non-negative decimal | Lab report value. |
| Potassium (K) | No | Non-negative decimal | Lab report value. |
| Report file name | No | Text | Optional attachment metadata. |
| Report URL | No | HTTP/HTTPS URL | Stores a cloud link when a report is uploaded or available. |
| Notes | No | Text | Internal context for existing lab reports. |

## Landholding And GPS

| Field | Required | Validation / Values | Notes |
| ----- | -------- | ------------------- | ----- |
| Farmer | Yes | Farmer reference | Land belongs to one farmer. |
| Survey number / Khasra number | Yes | Text | Required land identifier. |
| Area | Yes | Decimal acres | Accept decimals. |
| Ownership type | Yes | `Self-owned`, `Leased-in`, `Sharecropper` | Exact values approved. |
| Irrigation source | Yes | `Canal`, `Borewell`, `Open well`, `Pond`, `Rainfed`, `Drip` | Exact values approved. |
| GPS latitude | Yes | Decimal | Point capture. |
| GPS longitude | Yes | Decimal | Point capture. |

GPS precision within 10 meters is acceptable. Offline GPS capture and polygon
boundary drawing are not part of Phase 1.

## Crop Catalog

| Field | Required | Validation / Values | Notes |
| ----- | -------- | ------------------- | ----- |
| Crop name | Yes | Approved crop list or admin-created | Sample list below. |
| Crop category | Yes | Approved category list | Used for filtering. |
| Status | Yes | Active/inactive lifecycle | Admin-managed master data. |

Approved crop categories:

- Cereals
- Pulses
- Oilseeds
- Vegetables
- Fruits
- Cash crops

Phase 1 sample crops:

- Paddy
- Wheat
- Maize
- Soybean
- Groundnut
- Cotton
- Tomato
- Onion
- Chilli
- Mango

## Seasons

| Season | Start Month | End Month |
| ------ | ----------- | --------- |
| Kharif | June | October |
| Rabi | November | February |
| Summer / Zaid | March | May |

Use crop year labels such as `2025-26`.

## Crop History

| Field | Required | Validation / Values | Notes |
| ----- | -------- | ------------------- | ----- |
| Farmer | Yes | Farmer reference | Owner of history record. |
| Previous crop | Yes | Text or crop reference | Last season crop. |
| Yield | No | Decimal | Optional. |
| Yield unit | No | `Quintal/acre`, `kg/acre` | Default `Quintal/acre`. |

## Seasonal Crop Plan

| Field | Required | Validation / Values | Notes |
| ----- | -------- | ------------------- | ----- |
| Farmer | Yes | Farmer reference | Crop plan owner. |
| Land/plot | Yes | Land or plot reference | Drives area and reports. |
| Crop | Yes | Crop reference | Used for demand and filters. |
| Season | Yes | Season reference | Kharif/Rabi/Summer-Zaid. |
| Crop year | Yes | Text such as `2025-26` | Do not use only calendar year. |
| Planned area | Yes | Decimal acres | Used for input demand. |
| Expected yield | No | Decimal quintals | Implementation default: optional manual value; reports sum entered values and leave blanks blank. |
| Status | Yes | `DRAFT`, `CONFIRMED`, `COMPLETED`, `CANCELLED` | `CONFIRMED` drives demand. |
| Confirmed at | Yes when confirmed | Timestamp | Used by input demand report date filter. |

## Input Catalog And Rules

| Field | Required | Validation / Values | Notes |
| ----- | -------- | ------------------- | ----- |
| Input name | Yes | Admin-managed | Examples: Urea, DAP, Paddy seeds. |
| Input type | Yes | `Seed`, `Fertilizer` | Phase 1 required types. |
| Unit | Yes | `Kg`, `bag (50kg)` | Biologicals/micronutrients can be added later. |
| Crop | Yes | Crop reference | Rule is crop-specific. |
| Recommended quantity per acre | Yes | Decimal | Admin-editable default. |
| Rounding mode | Yes | Round up | Round to nearest kg or bag. |
| Buffer percent | Yes | 5% | Applied to total demand. |

## Input Demand Estimate

| Field | Required | Validation / Values | Notes |
| ----- | -------- | ------------------- | ----- |
| Crop | Yes | Crop reference | Grouping dimension. |
| Season | Yes | Season reference | Grouping dimension. |
| Total area | Yes | Decimal acres | Sum of confirmed crop plans. |
| Recommended quantity per acre | Yes | Decimal | From active crop input rule. |
| Total demand | Yes | Decimal | Area times recommended quantity. |
| Buffer 5% | Yes | Decimal | Total demand times 5%. |
| Final demand | Yes | Rounded decimal | Total plus buffer, rounded up. |
| Unit | Yes | `Kg` or `bag (50kg)` | From input catalog. |

## Advisory

| Field | Required | Validation / Values | Notes |
| ----- | -------- | ------------------- | ----- |
| Title | Yes | Text | Dashboard heading. |
| Message | Yes | Text | Advisory body. |
| Category | Yes | `Agronomy`, `Pest and disease management`, `Soil health`, `Weather alert` | Approved categories. |
| Target type | Yes | `All members`, `Crop` | No individual targeting in Phase 1. |
| Target crop | Required for crop target | Crop reference | Send/display to farmers with matching crop plans. |
| Images | No | One or more image links | Cloud storage links; thumbnails in dashboard. |
| Status | Yes | Draft/published/archive lifecycle | No SMS/WhatsApp in Phase 1. |

## Report Filters

| Filter | Required | Notes |
| ------ | -------- | ----- |
| Village | Yes | Dropdown. |
| Crop | Yes | Dropdown. |
| Season | Yes | Dropdown. |
| Coordinator | Yes | Field coordinator name. |
| Date range | Optional | Uses created/updated/confirmed timestamps by report type. |
