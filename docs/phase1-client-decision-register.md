# Phase 1 Client Decision Register

Last updated: 2026-05-13

This file records the client-approved Phase 1 scope for the FPO digitization
release. Treat this as the source of truth for implementation alignment unless
a later signed change request supersedes it.

## Scope Status

Phase 1 is product-scope locked for development.

The remaining work is implementation alignment, UAT, production setup, and a few
small implementation defaults listed at the end of this document. Do not ask the
client again for any decision already captured in this file unless a later
change request explicitly contradicts it.

## Client Query Policy

Use this table to avoid reconnecting with the client for the same questions.

| Topic | Decision Status | Development Instruction |
| ----- | --------------- | ----------------------- |
| Phase 1 scope | Answered | Build only the included Phase 1 items and keep Phase 2 items out. |
| Role model | Answered | Use `ADMIN`, `FPO_MANAGER`, and `FIELD_COORDINATOR`; do not use `SUPERVISOR` for FPO Phase 1. |
| Farmer login | Answered | Do not build farmer login/mobile screens in Phase 1. Staff enter data. |
| OTP login | Answered | Do not build OTP in Phase 1. Use username and password. |
| Farmer fields | Answered | Use the approved farmer profile fields in this file. |
| Geography labels | Answered | Use village, taluka, district, state. Do not ask about block again. |
| Farmer categories | Answered | Use the approved five categories and definitions. |
| Land fields | Answered | Use survey/khasra, area in acres, ownership, irrigation, latitude, longitude. |
| GPS scope | Answered | GPS point capture only; no polygon, map boundary, or offline capture. |
| Crop list/categories | Answered | Use approved categories and starter crop list. |
| Season names/months | Answered | Use Kharif, Rabi, Summer/Zaid with approved month ranges. |
| Crop plan statuses | Answered | Use `DRAFT`, `CONFIRMED`, `COMPLETED`, `CANCELLED`. |
| Crop history | Answered | Previous crop required; yield optional with quintal/acre default. |
| Input demand formula | Answered | Fixed per-acre values, confirmed plans only, 5% buffer, round up. |
| Report sheets/columns | Answered | Use the three approved Excel sheets and columns. |
| Report filters/date range | Answered | Use village, crop, season, coordinator, optional date range with meanings below. |
| PDF reports | Answered | Do not build PDF for Phase 1 FPO reports. |
| Advisory categories | Answered | Use approved four categories. |
| Advisory targeting | Answered | Support all-members and crop targeting only. |
| Advisory images | Answered | Support one or more image attachments; no video or AI analysis. |
| Language | Answered | English only in Phase 1. |
| UAT data/sign-off | Answered | Use Wagholi/Haveli/Pune/Maharashtra test data and agreed sign-off owners. |
| Hosting preference | Answered | Plan AWS India Mumbai; IT company manages technical operations initially. |
| Support/SLA | Answered | Client owns user support/training; IT company owns technical operations and critical bug SLA. |

## Included In Phase 1

- Farmer/FPO member registration.
- Land and plot management with GPS point capture.
- Crop planning, seasonal planning, and crop history.
- Input demand aggregation from confirmed crop plans.
- Basic in-app advisory with text and images.
- Excel reports with agreed sheets, filters, branding text, and footer.
- Admin web dashboard.
- Role-based access for `ADMIN`, `FPO_MANAGER`, and `FIELD_COORDINATOR`.
- Optional farmer login is excluded; farmers are view-only/no-login in Phase 1.
- Basic soil test data entry for existing lab reports.

## Excluded Until Phase 2

- Inventory management.
- Input distribution tracking.
- Procurement and produce aggregation.
- Payments and farmer ledger.
- Marketplace and dealer discovery.
- Satellite/NDVI layers.
- AI image verification or crop stage verification.
- Carbon credit calculator and full carbon accounting.
- WhatsApp integration.
- Drone images.
- Advanced soil biological parameters such as microbial count and necromass.
- Farmer mobile app and OTP login.
- Plot polygon drawing or offline GPS capture.

## Roles

| Role | Phase 1 Meaning |
| ---- | --------------- |
| `ADMIN` | Platform super user. Can see all FPOs, all farmers, all coordinators, and future platform modules. |
| `FPO_MANAGER` | Limited to one FPO. Can manage that FPO's farmers, coordinators, crop plans, input demand, reports, and advisories. |
| `FIELD_COORDINATOR` | Limited to assigned FPO/villages/farmers. Enters farmer profile, land, soil, and crop plan data on behalf of farmers. |

Notes:

- `SUPERVISOR` is not used for Phase 1 FPO scope.
- A person may hold both `ADMIN` and `FPO_MANAGER`, but the roles remain
  logically separate.
- Phase 1 uses username and password login for staff roles.
- Farmers do not need login in Phase 1.

## Farmer Profile

| Field | Required | Notes |
| ----- | -------- | ----- |
| Full name | Yes | Farmer/member display name. |
| Mobile number | Yes | Must be a 10 digit Indian mobile number. |
| Aadhaar number | No | Optional; apply the Aadhaar privacy implementation default below. |
| Village | Yes | Label is `village`. |
| Taluka | Yes | Use `taluka`; do not create a separate block field for Phase 1. |
| District | Yes | Label is `district`. |
| State | Yes | Label is `state`. |
| Gender | Yes | `Male`, `Female`, `Other`. |
| Farmer category | Yes | `Marginal`, `Small`, `Semi-medium`, `Medium`, `Large`. |
| Status | Yes | `Active`, `Inactive`, `Suspended`; default `Active`. |
| FPO | Yes | Required for FPO manager isolation and reports. |
| Field coordinator | No | Optional assignment; used for filters and operational ownership. |

Farmer category definitions:

- `Marginal`: less than 1 ha.
- `Small`: 1 to 2 ha.
- `Semi-medium`: 2 to 4 ha.
- `Medium`: 4 to 10 ha.
- `Large`: more than 10 ha.

## Soil Profile

Basic soil test entry is required when an existing soil test report is available.
If no soil test exists, fields may be blank.

| Field | Required | Notes |
| ----- | -------- | ----- |
| SOC | No | Manual entry from lab report. |
| pH | No | Manual entry from lab report. |
| Nitrogen (N) | No | Manual entry from lab report. |
| Phosphorus (P) | No | Manual entry from lab report. |
| Potassium (K) | No | Manual entry from lab report. |
| Report attachment | No | PDF/image upload allowed. |

Carbon calculation must not be performed from these values in Phase 1.

## Land And GPS

| Field | Required | Notes |
| ----- | -------- | ----- |
| Survey number / Khasra number | Yes | Text field. |
| Area | Yes | Decimal, in acres. |
| Ownership type | Yes | `Self-owned`, `Leased-in`, `Sharecropper`. |
| Irrigation source | Yes | `Canal`, `Borewell`, `Open well`, `Pond`, `Rainfed`, `Drip`. |
| GPS latitude | Yes | Point capture is enough. |
| GPS longitude | Yes | Point capture is enough. |

GPS expectations:

- Point precision within 10 meters is acceptable.
- Offline capture is not required.
- Polygon/boundary drawing is Phase 2.

## Crop Planning

Crop categories:

- `Cereals`
- `Pulses`
- `Oilseeds`
- `Vegetables`
- `Fruits`
- `Cash crops`

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

Seasons:

| Season | Start Month | End Month |
| ------ | ----------- | --------- |
| Kharif | June | October |
| Rabi | November | February |
| Summer / Zaid | March | May |

Crop year format:

- Use a crop year string such as `2025-26`.

Crop history:

| Field | Required | Notes |
| ----- | -------- | ----- |
| Previous crop | Yes | Last season, text or crop reference. |
| Yield | No | Optional. |
| Yield unit | No | Default `Quintal/acre`; also accept `kg/acre`. |

Crop plan statuses:

- `DRAFT`
- `CONFIRMED`
- `COMPLETED`
- `CANCELLED`

Only `CONFIRMED` crop plans are used for input demand.

## Input Demand

Phase 1 input scope:

- Seeds: `Kg`.
- Fertilizers: `Kg` or `bag (50kg)`.

Optional/future input types:

- Biological liquid: `Litre`.
- Biological powder: `Kg`.
- Micronutrients: `Kg` or `Litre`.

Rules:

- Use fixed default quantity per acre per crop.
- Admin can edit default values.
- No dynamic formula engine is required in Phase 1.
- Round up to the nearest kg or bag.
- Apply 5% buffer.
- Ignore 2% wastage in Phase 1.
- Calculate demand only from `CONFIRMED` crop plans.

Demand formula:

```text
total_demand = total_confirmed_area_acres * recommended_quantity_per_acre
buffer = total_demand * 0.05
final_demand = round_up(total_demand + buffer)
```

## Reports

Excel is required. PDF is not required for Phase 1.

Sheet 1: `Farmer Register`

| Column |
| ------ |
| Name |
| Mobile |
| Village |
| Taluka |
| District |
| Survey No |
| Area (acres) |
| Category |
| Status |

Sheet 2: `Crop Plan Summary`

| Column |
| ------ |
| Season |
| Year |
| Crop |
| Village |
| No. of Farmers |
| Total Area (acres) |
| Expected Yield (quintals) |

Sheet 3: `Input Demand`

| Column |
| ------ |
| Input Type (Seed/Fertilizer) |
| Crop |
| Season |
| Total Area (acres) |
| Recommended Qty/acre |
| Total Demand |
| Buffer 5% |
| Final Demand |
| Unit |

Report filters:

- Village.
- Crop.
- Season.
- Coordinator.
- Optional date range.

Date range meanings:

- Farmer Register: farmer profile `created_at`.
- Crop Plan Summary: crop plan `created_at` or `updated_at`.
- Input Demand: timestamp when the crop plan status changed to `CONFIRMED`.

Report branding:

- Top right: logo when provided, otherwise text
  `Carbon Farming Platform - FPO Digitization`.
- Footer: `Confidential - For internal FPO use`.

## Advisory

Categories:

- Agronomy.
- Pest and disease management.
- Soil health.
- Weather alert.

Targeting:

- Broadcast to all members.
- Filter by crop.
- Individual farmer targeting is not required in Phase 1.

Content:

- Title and message.
- One or more image attachments.
- No video upload.
- No AI analysis.
- Store images in cloud object storage and save links in advisory records.
- Display advisories on the admin/FPO manager dashboard with image thumbnails
  or click-to-view links.

## UX And Language

- English only.
- Admin web dashboard first.
- Coordinators use the same dashboard on laptop or tablet.
- Farmer mobile screens are Phase 2.

## UAT

Sign-off:

- Client project owner.
- One nominated FPO manager from the pilot FPO.

Location:

- Client office or pilot village.

Sample data:

- Village: `Wagholi`
- Taluka: `Haveli`
- District: `Pune`
- State: `Maharashtra`
- Crops: `Paddy` for Kharif, `Wheat` for Rabi.
- Inputs: `Urea`, `DAP`, `Paddy seeds (var. local)`.
- Farmers: minimum 5 dummy farmers with mobile numbers.

## Hosting And Operations

- Preferred hosting: AWS India, Mumbai region.
- IT company proposes and manages initial production setup.
- Client shares access after UAT sign-off.
- IT company owns backups, monitoring, security patches, and technical
  operations during Phase 1 and Phase 2.
- Client owns user support and training.
- Critical bug SLA: 24 hour response, 3 day fix.

## Implementation Defaults

These defaults let developers continue without reconnecting with the client for
minor details. They may be changed later only through implementation review or a
new client change request.

| Item | Default To Use | Reason |
| ---- | -------------- | ------ |
| Expected yield source | Add an optional manual `expected yield (quintals)` field on crop plans. In summary reports, sum entered values and leave blanks blank where no value exists. | Avoids inventing agronomic yield formulas in Phase 1. |
| Aadhaar privacy | Validate 12 digits if entered, mask in UI except last 4 digits, exclude from Phase 1 exports, and treat as sensitive data. | Keeps optional Aadhaar low-risk and avoids unnecessary exposure. |
| Logo asset missing | Use plain text `Carbon Farming Platform - FPO Digitization` in the Excel header until the logo file is provided. | Client already approved text fallback. |
| Pilot FPO name/code missing | Use editable seed values `Pilot FPO` and `PILOT-FPO` for UAT. | Lets UAT proceed; real name can be edited later. |
| Report date range boundaries | Treat `from` as start of day and `to` as end of day in the user's local timezone. | Matches business expectation for calendar date filters. |
| Crop plan report date filter | Include plans where `created_at` or `updated_at` falls inside the range. | Client said creation or modification date. |
| Input demand report date filter | Use `confirmed_at`; add this timestamp when a plan first moves to `CONFIRMED`. | Client explicitly tied input demand date range to confirmation date. |
| Image storage in development | Use existing local/MinIO storage adapter. | Matches current project setup. |
| Image storage in production | Use AWS S3 or S3-compatible storage with saved object URL/key. | Matches AWS Mumbai hosting preference. |
| Biologicals/micronutrients | Keep catalog extensible but do not seed or require them in Phase 1. | Client marked these optional/later. |

## Do Not Re-Ask For Phase 1

The following are confirmed out of scope for Phase 1 and should not be raised
again as blockers:

- OTP login.
- Farmer mobile app.
- WhatsApp/SMS advisory delivery.
- PDF report export for FPO Phase 1.
- Offline data capture.
- GPS polygon/boundary drawing.
- Satellite/NDVI layers.
- AI image/crop-stage verification.
- Carbon accounting or carbon credit calculation.
- Inventory, distribution, procurement, payments, marketplace, or dealer
  discovery.
