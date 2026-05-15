# Carbon Data Dictionary

Last updated: 2026-05-16

This document defines the durable Carbon foundation fields derived from
`App Flow.docx`. It is intentionally foundation-focused: it stores Carbon
identity, farm, soil, and activity-category data, but it does not define a
carbon credit methodology, verified credit calculation, OTP provider, map
provider, AI verification provider, satellite provider, or payment flow.

Use with:

- [Carbon App Flow Task Roadmap](carbon-app-flow-task-roadmap.md)
- [Carbon App Flow Base Plan](carbon-app-flow-base-plan.md)
- [Modular Platform Strategy](modular-platform-strategy.md)

## Implementation Status

| Area | Status | Notes |
| --- | --- | --- |
| Carbon profile identity | API/UI ready | `carbon_profiles` stores the Carbon identity and participant/farmer profile foundation; backend APIs are verified by `CarbonProfileControllerIT` and frontend forms are wired. |
| Carbon farm/plot | API/UI ready | `carbon_farm_plots` stores GPS point capture and optional boundary JSON for later map work; backend APIs are verified by `CarbonProfileControllerIT` and frontend forms are wired. |
| Carbon soil profile | API/UI metadata ready | `carbon_soil_profiles` stores SOC, pH, EC, NPK, bulk density, texture, and report metadata; backend APIs and frontend metadata/link forms are wired, while direct file upload remains. |
| Carbon activity categories | Seeded | `carbon_activity_categories` stores App Flow activity categories. |
| Carbon score/calculation | Not implemented | Methodology, eligibility, buffer, leakage, and verification rules must be approved before implementation. |
| Provider integrations | Not implemented | OTP, map boundary drawing, AI verification, satellite layers, weather API, and payment integrations remain future/provider-dependent. |

## Module And Package Rules

| Rule | Required Behavior |
| --- | --- |
| Frontend package | Carbon UI appears when `EXPO_PUBLIC_ENABLED_CLIENT_MODULES` includes `carbon`. |
| Backend tenant module | Carbon backend-backed screens require the tenant `SUSTAINABILITY` module. |
| FPO reuse | Carbon may reference FPO member profiles where useful, but Carbon identity is stored separately. |
| Source handover | Carbon-only source distribution must include shared core plus Carbon, not complete unlicensed FPO workflows. |

## Carbon Profile

Table: `carbon_profiles`

Purpose: create a Carbon identity record for a farmer or Carbon-program
participant. This is separate from FPO member profile so Carbon can be sold as a
standalone app package.

| Field | Required | Validation / Values | Notes |
| --- | --- | --- | --- |
| Tenant | Yes | Existing tenant | Scope for all Carbon data. |
| User | No | Existing user | Optional until login and OTP flows are finalized. |
| FPO member profile | No | Existing FPO member | Optional link when Carbon is used with FPO package. |
| Coordinator user | No | Existing `FIELD_COORDINATOR` user | Scopes field-coordinator access; admins/FPO managers can assign or clear it. |
| Carbon identity ID | Yes | Unique within tenant | Example: `CCI-MH-2026-001`. |
| Participant type | Yes | `FARMER`, `FPO_FPC`, `AGRONOMIST` | Based on App Flow login/user types. |
| Display name | Yes | Text | Farmer or participant name. |
| Mobile number | No | Phone text | OTP validation waits for Phase 2/provider decision. |
| Language preference | No | `English`, `Hindi`, `Marathi` | App Flow language choices. |
| Village | No | Text | Used for filters and grouping. |
| Taluka | No | Text | Preferred over block. |
| District | No | Text | Location grouping. |
| State | No | Text | Location grouping. |
| GPS latitude | No | -90 to 90 | Point capture; required later when farm capture UI is finalized. |
| GPS longitude | No | -180 to 180 | Point capture; required later when farm capture UI is finalized. |
| Total landholding acres | No | Non-negative decimal | Carbon dashboard farm area input. |
| Cropping pattern | No | Text | Summary of crops/practices. |
| Livestock count | No | Non-negative integer | App Flow farming detail. |
| Tillage status | No | `Conventional`, `Reduced tillage`, `No tillage` | Carbon practice baseline context. |
| Bank status | No | `Linked`, `Pending`, `Not required` | Payout flow is future. |
| Aadhaar status | No | `Provided`, `Optional not captured` | No Aadhaar number stored in Carbon schema. |
| Document status | No | `Not started`, `Partial`, `Ready` | Summary state; detailed documents can use storage/evidence later. |
| Status | Yes | Lifecycle text | Recommended values: `ACTIVE`, `INACTIVE`, `SUSPENDED`. |

## Carbon Farm Plot

Table: `carbon_farm_plots`

Purpose: store the farm/plot level unit used for GPS capture, soil profile, and
activity tracking.

| Field | Required | Validation / Values | Notes |
| --- | --- | --- | --- |
| Tenant | Yes | Existing tenant | Scope. |
| Carbon profile | Yes | Existing Carbon profile | Owner of plot. |
| Farm name | Yes | Text | Example: `Plot A`, `Pomegranate block`. |
| Survey number | No | Text | Optional until Carbon farm form is finalized. |
| Area acres | Yes | Non-negative decimal | Dashboard farm area and activity context. |
| Latitude | Yes | -90 to 90 | GPS point capture. |
| Longitude | Yes | -180 to 180 | GPS point capture. |
| Boundary GeoJSON | No | JSON | Placeholder for future map boundary drawing. |
| Irrigation source | No | Text | Can align with FPO approved values later. |
| Primary crop | No | Text | Free text until Carbon crop catalog is finalized. |
| Tillage status | No | `Conventional`, `Reduced tillage`, `No tillage` | Plot-level baseline/practice context. |
| Status | Yes | Lifecycle text | Recommended values: `ACTIVE`, `INACTIVE`. |

## Carbon Soil Profile

Table: `carbon_soil_profiles`

Purpose: store soil test and soil health values needed for future Carbon
verification. These fields must not drive verified credit calculation until the
methodology is approved and versioned.

| Field | Required | Validation / Values | Notes |
| --- | --- | --- | --- |
| Tenant | Yes | Existing tenant | Scope. |
| Carbon profile | Yes | Existing Carbon profile | Owner of soil record. |
| Carbon farm plot | No | Existing Carbon plot | Optional until plot capture is complete. |
| Test date | No | Date | Lab or manual entry date. |
| Lab name | No | Text | Optional. |
| SOC percent | No | Non-negative decimal | Soil organic carbon. |
| pH | No | 0 to 14 | Lab value. |
| EC | No | Non-negative decimal | Electrical conductivity. |
| Nitrogen kg/ha | No | Non-negative decimal | N value. |
| Phosphorus kg/ha | No | Non-negative decimal | P value. |
| Potassium kg/ha | No | Non-negative decimal | K value. |
| Bulk density g/cm3 | No | Non-negative decimal | Required by many carbon methodologies but optional until report exists. |
| Texture | No | Text | Example: clay loam, sandy clay. |
| Microbial count | No | Non-negative decimal | Schema-only future field; not exposed in current Phase 1 Carbon APIs. |
| Microbial count unit | No | Text | Schema-only future field; not exposed in current Phase 1 Carbon APIs. |
| Biological notes | No | Text | Schema-only future field; not exposed in current Phase 1 Carbon APIs. |
| Report file name | No | Text | Optional PDF/image metadata. |
| Report content type | No | PDF or image MIME type | `application/pdf` or `image/*`. |
| Report storage key | No | Text | Object storage key. |
| Report URL | No | URL text | External/cloud link when available. |
| Status | Yes | Lifecycle text | Current API values: `ACTIVE`, `INACTIVE`, `SUSPENDED`, `ARCHIVED`. |

## Carbon Activity Categories

Table: `carbon_activity_categories`

Seeded categories:

| Code | Name | Evidence Required |
| --- | --- | --- |
| `LAND_PREPARATION` | Land Preparation | Yes |
| `SOWING` | Sowing | Yes |
| `FERTIGATION` | Fertigation | Yes |
| `IRRIGATION` | Irrigation | Yes |
| `BIOLOGICAL_APPLICATION` | Biological Application | Yes |
| `COMPOST_ADDITION` | Compost Addition | Yes |
| `PRUNING_BIOMASS_INCORPORATION` | Pruning Biomass Incorporation | Yes |
| `HARVESTING` | Harvesting | Yes |

These are category master records only. Actual Carbon activity entry records,
evidence verification, AI scoring, and emission-reduction calculations are
separate future tasks.

## Not In This Data Dictionary Yet

Do not implement these as production behavior until decisions are locked:

- OTP/SMS login.
- Farm boundary drawing provider and offline map rules.
- AI image verification provider and confidence thresholds.
- Satellite NDVI/NDRE/NDMI provider and refresh frequency.
- Carbon calculation methodology and credit eligibility.
- Dealer stock sync and marketplace ordering.
- Farmer payouts, ledgers, or carbon-credit payments.
- Verified carbon credit issuance reports.
