# Carbon Flow Screen Status And Task Breakdown

Last audited: 2026-05-19

Source workbook: `C:\Users\korea\Documents\Codex\i-want-to-create-a-app\docs\Carbon Flow.xlsx`

This document compares the latest client Excel against the current repository.
It is planning documentation only; it does not introduce implementation work.

The workbook says this is a 76-screen flow, but the file currently contains 77
numbered rows. Screen 77 is `Logout Confirmation`, so it is included in this
review and should be clarified with the client before final scope sign-off.

## Current Baseline Used For This Review

Known implemented foundation:

- `farmer_profiles` canonical identity table.
- `FarmerService` and platform participant registry.
- `carbon_farm_plots` with GPS point and `boundary_geojson`.
- `carbon_soil_profiles` with SOC, pH, EC, NPK, bulk density, texture, and
  report upload metadata.
- `carbon_activity_categories` and `carbon_activity_records` for simple Carbon
  logs.
- Generic `WorkflowDefinition`, `Activity`, `Task`, and `Evidence`.
- Roles: `ADMIN`, `FPO_MANAGER`, `FIELD_COORDINATOR`, `FARMER`.
- JWT username/password auth. OTP is not implemented.
- Frontend modules: `carbon` and `fpo`, controlled by the central visibility
  registry.

Repository areas checked:

- Excel source: `docs/Carbon Flow.xlsx`.
- Backend migrations: `V8__carbon_foundation_schema.sql`,
  `V11__carbon_activity_records.sql`, `V13__farmer_profile_foundation.sql`,
  `V14__fpo_farmer_profile_link_guard.sql`, `V15__farmer_profile_backfill.sql`.
- Backend services/controllers: Carbon profile APIs, farmer profile service,
  workflow/activity/evidence services, reporting services, notification APIs.
- Frontend modules: `src/modules/carbon`, `src/modules/fpo`,
  `src/auth/roleAccess.ts`, `src/core/api/endpoints.ts`, shared farmer fields,
  workflow/evidence screens.
- Package dependencies: no `react-native-maps` dependency is currently present,
  so polygon drawing and satellite map overlays are not implemented.

Important interpretation:

- In the client workbook, `vineyard` means the farm plot/block domain.
- Do not create a separate `vineyard` frontend module unless the business later
  wants it licensed independently.
- The current Carbon package should use vineyard/plot/block wording where
  appropriate.

Legend:

- ✅ Done: backend and frontend are implemented as described.
- 🟡 Partial: useful model/API/UI exists, but the dedicated screen, flow, or
  critical behavior is missing.
- ❌ Not started: no meaningful code exists, or only unrelated placeholder/demo
  data exists.

## Screen Status Matrix

| Screen | Name | Epic | Status | Current Evidence / Gap |
| -----: | ---- | ---- | ------ | ---------------------- |
| 1 | Splash Screen | Onboarding | ❌ Not started | No client splash with logo, tagline, vineyard animation, offline/loading states. |
| 2 | Language Selection | Onboarding | ❌ Not started | No English/Marathi/Hindi language selection or localization store. |
| 3 | Onboarding 1 | Onboarding | ❌ Not started | No onboarding carousel/page flow. |
| 4 | Onboarding 2 | Onboarding | ❌ Not started | No vineyard tracking onboarding page. |
| 5 | Onboarding 3 | Onboarding | ❌ Not started | No carbon rewards onboarding page. |
| 6 | Login Screen | Auth | 🟡 Partial | JWT username/password login exists; mobile number/user type/Send OTP flow does not. |
| 7 | OTP Verification | Auth | ❌ Not started | No OTP table, mock SMS, resend timer, or verify endpoint. |
| 8 | Registration Screen | Farmer Profile | 🟡 Partial | Farmer identity and admin-created farmer flows exist; self/mobile registration screen is missing. |
| 9 | Consent Screen | Onboarding | ❌ Not started | No GPS/camera/storage/notification consent screen. |
| 10 | Import Existing Data | Onboarding | ❌ Not started | No import options, record preview, or sync-import API. |
| 11 | Home Dashboard | Farmer Profile | 🟡 Partial | Carbon farmer dashboard has KPI cards and dummy/weather/dealer widgets; not the full home with quick actions/alerts. |
| 12 | Notifications Panel | Onboarding | 🟡 Partial | Notification backend/frontend exists, but no farmer-focused notification panel with filters. |
| 13 | Farmer Profile | Farmer Profile | 🟡 Partial | Canonical farmer profile exists; Carbon farmer profile UX is not the full client profile screen. |
| 14 | Bank Details | Farmer Profile | 🟡 Partial | Carbon has bank status only; no bank details table/API/form. |
| 15 | Document Upload | Farmer Profile | 🟡 Partial | Storage and soil/evidence uploads exist; no KYC/carbon document table and upload flow. |
| 16 | Farm List Screen | Plots | 🟡 Partial | Carbon plots can be listed; no dedicated vineyard block list with map thumbnails. |
| 17 | Vineyard Mapping | Plots | 🟡 Partial | `boundary_geojson` exists; polygon drawing/map UI is missing. |
| 18 | Add Vineyard Block | Plots | 🟡 Partial | Carbon plot create/edit exists; vineyard variety/rootstock/block fields are missing. |
| 19 | Farm Detail Screen | Plots | 🟡 Partial | Plot cards/details exist; full detail/edit with soil score summary is missing. |
| 20 | Soil Profile Dashboard | Soil | 🟡 Partial | Soil values display exists; no dedicated soil dashboard/trend graph. |
| 21 | Soil Report Upload | Soil | 🟡 Partial | PDF/image report upload exists; OCR processing state is missing. |
| 22 | Soil Manual Entry | Soil | ✅ Done | Carbon soil API and frontend form support SOC, pH, EC, NPK, texture, report metadata, and validation. |
| 23 | Soil Satellite Layer | Soil | ❌ Not started | No NDVI/NDRE/NDMI provider, map layer, or tile overlay. |
| 24 | Soil Recommendation | Soil | 🟡 Partial | Dummy recommended inputs/advisory text exists; no persisted recommendation workflow. |
| 25 | Crop Stage Dashboard | Activities | ❌ Not started | No vineyard lifecycle/crop-stage dashboard. |
| 26 | Season Calendar | Activities | ❌ Not started | No vineyard activity calendar or reminder scheduling. |
| 27 | Activity Dashboard | Activities | 🟡 Partial | Generic/Carbon activity lists exist; filters/charts/client activity dashboard are missing. |
| 28 | Activity Wizard Step 1 | Activities | 🟡 Partial | Activity can start with a unit/plot name; no vineyard block selector wizard step. |
| 29 | Activity Wizard Step 2 | Activities | 🟡 Partial | Workflow selection exists; no crop/variety cards tied to vineyard blocks. |
| 30 | Activity Wizard Step 3 | Activities | 🟡 Partial | Workflow/category data exists; no activity grid wizard step. |
| 31 | Activity Wizard Step 4 | Activities | 🟡 Partial | Carbon activity log has date/input/quantity remarks; no type-specific details/labor step. |
| 32 | Activity Wizard Step 5 | Activities | 🟡 Partial | Evidence upload exists; no wizard camera/gallery/invoice step. |
| 33 | AI Verification Screen | Activities | ❌ Not started | No AI/mock verification screen or API. |
| 34 | Activity Result Screen | Activities | 🟡 Partial | Dummy carbon score/reduction values exist; no result screen after activity submission. |
| 35 | Pruning Activity Screen | Activities | 🟡 Partial | Pruning category exists; no specialized pruning/biomass form. |
| 36 | Irrigation Activity Screen | Activities | 🟡 Partial | Irrigation category exists; no duration/moisture specialized form. |
| 37 | Spray Activity Screen | Activities | 🟡 Partial | Generic input activity can log spray-like data; no spray dosage/weather-risk form. |
| 38 | Compost Activity Screen | Activities | 🟡 Partial | Compost category exists; no source/method specialized form. |
| 39 | Cover Crop Activity | Activities | 🟡 Partial | Cover crop can fit generic workflow/category data; no seed/area/biomass form. |
| 40 | Advisory Dashboard | Advisory | 🟡 Partial | Published advisory list exists; no dedicated advisory dashboard/query categories. |
| 41 | Ask Expert Screen | Advisory | ❌ Not started | No query table/API or text/image/voice/video query form. |
| 42 | Advisory Chat Screen | Advisory | ❌ Not started | No chat thread, expert reply, or attachment timeline. |
| 43 | AI Diagnosis Screen | Advisory | ❌ Not started | No disease prediction/mock AI diagnosis. |
| 44 | Recommendation Screen | Advisory | 🟡 Partial | Recommendation/advisory concepts exist; no expert recommendation screen and resolution flow. |
| 45 | Marketplace Dashboard | Marketplace | 🟡 Partial | Carbon farmer screen has dummy dealer/lab list; no backend marketplace. |
| 46 | Dealer List Screen | Marketplace | 🟡 Partial | Dummy nearby dealers exist; no searchable dealer API/list screen. |
| 47 | Dealer Profile | Marketplace | ❌ Not started | No dealer profile/catalog/reviews/offers. |
| 48 | Product Detail Screen | Marketplace | ❌ Not started | No product details, stock, dosage, or add-to-cart. |
| 49 | Cart Screen | Marketplace | ❌ Not started | No cart model or UI. |
| 50 | Checkout Screen | Marketplace | ❌ Not started | No checkout/order/payment summary. |
| 51 | Carbon Calculator Dashboard | Carbon Calculator | ❌ Not started | Carbon potential is dummy; no calculator dashboard or calculation API. |
| 52 | Carbon Practices Screen | Carbon Calculator | 🟡 Partial | Activity/practice records exist; no practice checklist tied to calculator. |
| 53 | Carbon Result Screen | Carbon Calculator | ❌ Not started | No final calculator result, eligibility badge, or certificate generation flow. |
| 54 | Reports Dashboard | Reports | 🟡 Partial | Generic/FPO reporting exists; no Carbon/vineyard report dashboard. |
| 55 | Soil Trend Report | Reports | ❌ Not started | Soil records exist, but no SOC/pH trend report/download. |
| 56 | Yield Analytics Screen | Reports | ❌ Not started | No harvest/yield dataset or comparison screen. |
| 57 | Carbon Trend Report | Reports | ❌ Not started | No yearly carbon trend export. |
| 58 | Wallet Dashboard | Wallet | ❌ Not started | No wallet/credits/payout model. |
| 59 | Transaction History | Wallet | ❌ Not started | No transaction table or statement export. |
| 60 | Carbon Certificate Screen | Wallet | ❌ Not started | No certificate record, preview, or download. |
| 61 | Weather Dashboard | Onboarding | 🟡 Partial | Dummy weather snapshot exists; no weather provider/API dashboard. |
| 62 | Weather Alert Screen | Onboarding | ❌ Not started | No weather risk alerts or advisory handoff. |
| 63 | Notification Center | Onboarding | 🟡 Partial | Notification APIs exist; farmer notification center is missing. |
| 64 | Settings Screen | Onboarding | ❌ Not started | No settings screen for language/theme/sync. |
| 65 | Help & Support | Onboarding | ❌ Not started | No FAQ/call/WhatsApp/support entry screen. |
| 66 | Support Ticket Screen | Onboarding | ❌ Not started | No support ticket table/API/form. |
| 67 | Offline Sync Screen | Onboarding | ❌ Not started | No offline queue, conflict resolution, or sync UI. |
| 68 | Admin Dashboard | Admin | 🟡 Partial | Admin shell and Carbon overview exist; not the client admin overview. |
| 69 | Farmer Management | Admin | 🟡 Partial | User/carbon profile lists exist; no farmer verification status queue. |
| 70 | Activity Verification Admin | Admin | 🟡 Partial | Evidence approve/reject exists; no Carbon activity verification queue as specified. |
| 71 | Soil Verification Admin | Admin | ❌ Not started | No soil verification queue/OCR review. |
| 72 | Carbon Project Management | Admin | ❌ Not started | No carbon project model or admin module. |
| 73 | Dealer Management | Admin | ❌ Not started | No dealer approval model or admin module. |
| 74 | Agronomist Dashboard | Admin | ❌ Not started | No agronomist advisory queue. |
| 75 | Success Screen | Onboarding | 🟡 Partial | Some success/confirmation UI patterns exist; no shared success screen. |
| 76 | Error Screen | Onboarding | 🟡 Partial | Inline error states exist; no shared generic error screen. |
| 77 | Logout Confirmation | Onboarding | ❌ Not started | Current logout is direct; no confirmation modal. |

Status count:

- ✅ Done: 1 screen.
- 🟡 Partial: 41 screens.
- ❌ Not started: 35 screens.

## Epic Task Breakdown

| Epic | Screen Numbers | Status | Backend Work | Frontend Work | Effort |
| ---- | -------------- | ------ | ------------ | ------------- | ------ |
| Onboarding / App Shell | 1-5, 9-10, 12, 61-67, 75-77 | 1-5 ❌, 9-10 ❌, 12 🟡, 61 🟡, 62 ❌, 63 🟡, 64-67 ❌, 75-76 🟡, 77 ❌ | Add optional user preference table for language/theme if persisted; add consent/import endpoints only if client needs server audit; add weather provider/mock API; add support-ticket and offline-sync queue later; keep notification module reusable. Module guards: Carbon package only, no FPO dependency. Reuse: notifications, storage, auth session. | Build splash, language selection, onboarding carousel, permission/consent flow, import preview, farmer notification center, weather dashboard/alerts, settings, help/support, support ticket form, offline sync screen, shared success/error/logout confirmation components. | L. Dependencies: copy/assets, language scope, weather provider, offline policy, support process. Reuse: notification/store/storage/session events. |
| Auth | 6-7 | 6 🟡, 7 ❌ | Add OTP request/verify tables and APIs; store hashed OTP, expiry, resend count, attempt count; mock SMS first; after verification issue existing JWT/refresh token; rate-limit mobile OTP; keep username/password for staff until client confirms OTP-only. Module guards: common auth. Reuse: `AuthService`, JWT, users, roles. | Replace/extend login with mobile field, user type selector, OTP boxes, timer, resend, invalid/expired states; preserve session restore/logout behavior. | M. Dependencies: OTP provider decision, mobile uniqueness rules, staff-login policy. Reuse: JWT auth and user roles. |
| Farmer Profile / Dashboard | 8, 11, 13-15 | 8 🟡, 11 🟡, 13 🟡, 14 🟡, 15 🟡 | Add/finish APIs for farmer self-registration through `FarmerService`; add `farmer_bank_details`; add `farmer_documents`; add profile completion/verification status; use storage adapter for document files; keep `farmer_profiles` canonical. Module guards: Carbon/Sustainability tenant module. Reuse: `FarmerService`, storage, user APIs. | Build registration/profile wizard, farmer home KPI dashboard, quick actions, alerts, profile edit, bank form, document upload with preview/status. Use shared farmer identity fields; avoid duplicate Carbon-only identity forms. | L. Dependencies: required KYC documents, bank validation/verification rules, dashboard KPI definitions. Reuse: `FarmerIdentityFields`, storage, Carbon snapshot cards. |
| Plots | 16-19 | 16 🟡, 17 🟡, 18 🟡, 19 🟡 | Extend `carbon_farm_plots` instead of creating duplicate vineyard tables: add variety, rootstock, planting date/year, block code, spacing, row count, soil score summary if needed; add GeoJSON list/detail endpoint if current response is not enough; keep plot linked to Carbon profile/farmer profile. Module guards: Carbon/Sustainability. Reuse: `carbon_farm_plots`, storage for map snapshots if needed. | Build farm/vineyard block list with map thumbnail, polygon drawing, add/edit block form, block detail screen, soil score card. Use `react-native-maps` or chosen map library after provider decision. | L. Dependencies: map provider, polygon offline/online rule, vineyard field list. Reuse: existing Carbon plot APIs and `boundary_geojson`. |
| Soil | 20-24 | 20 🟡, 21 🟡, 22 ✅, 23 ❌, 24 🟡 | Keep `carbon_soil_profiles`; add OCR placeholder status/result fields if needed; add soil score/recommendation calculation table or endpoint; add satellite layer metadata/provider integration later; add admin verification in Admin epic. Module guards: Carbon/Sustainability. Reuse: storage adapter and soil profile APIs. | Promote current soil form into dedicated dashboard/upload/manual-entry flow; add upload processing/OCR states; add trend graph; add NDVI/NDRE/NDMI map overlay; add recommendation cards. | L. Dependencies: OCR provider, satellite provider, soil score formula. Reuse: `carbon_soil_profiles`, report upload, storage. |
| Activities | 25-39 | 25-26 ❌, 27-32 🟡, 33 ❌, 34-39 🟡 | Seed vineyard workflow definitions for pruning, irrigation, spray, compost, cover crop; link activities to farmer/plot; add type-specific detail payloads via extension table or JSON metadata; add mock AI verification endpoint; decide whether `carbon_activity_records` becomes a summary/projection of workflow activities. Module guards: Activity Compliance + Carbon/Sustainability. Reuse: workflow, activity, task, evidence, storage. | Build crop-stage dashboard, season calendar, activity dashboard filters/charts, wizard steps 1-6, specialized forms for pruning/irrigation/spray/compost/cover crop, evidence upload step, AI verification/result screens. | L. Dependencies: final workflow definitions, type-specific fields, AI mock behavior. Reuse: `WorkflowDefinition`, `Activity`, `Evidence`, `carbon_activity_categories`. |
| Advisory | 40-44 | 40 🟡, 41-43 ❌, 44 🟡 | Decide whether to generalize FPO advisory or add Carbon advisory/query tables; add ask-expert query, chat thread, attachments, expert/admin response, mock AI diagnosis, recommendation status. Module guards: Advisory + Carbon/Sustainability. Reuse: storage for attachments, notifications. | Build advisory dashboard, ask expert form, chat timeline, diagnosis cards, recommendation screen, mark-resolved/follow-up states. | L. Dependencies: expert workflow owner, AI mock/provider, attachment rules. Reuse: FPO advisory concepts, storage, notifications. |
| Marketplace | 45-50 | 45-46 🟡, 47-50 ❌ | Add dealer, dealer profile, product/catalog, cart, order/checkout tables or mock APIs; decide if checkout is real or UI-only; add admin dealer approval in Admin epic. Module guards: future Marketplace module or Carbon package flag. Reuse: users/tenants/storage for product/dealer images. | Build marketplace dashboard, dealer list, dealer profile, product detail, cart, checkout, success/failure states. Replace dummy dealer list with backend data. | L. Dependencies: real vs mock ordering, payment scope, dealer data ownership. Reuse: storage and module visibility only. |
| Carbon Calculator | 51-53 | 51 ❌, 52 🟡, 53 ❌ | Add calculator formula/version table, practice inputs, calculation run/results, eligibility status, dummy formulas until methodology is approved; avoid verified credit claims. Module guards: Carbon/Sustainability. Reuse: activity/soil data, reports later. | Build calculator dashboard, practices checklist, calculate action, carbon result screen, eligibility badge, certificate CTA. | M/L. Dependencies: methodology approval; can start with explicit dummy formulas. Reuse: `carbon_activity_records`, soil profiles, reporting later. |
| Reports | 54-57 | 54 🟡, 55-57 ❌ | Add Carbon/vineyard report APIs and exports for soil trend, yield analytics, carbon trend; add PDF/XLSX templates; add aggregation queries over soil/activity/calculator data. Module guards: Report Export + Carbon/Sustainability. Reuse: existing report/export services and XLSX/PDF builders. | Build reports dashboard, detailed report screens, filters, download/share actions. | M. Dependencies: report columns/templates and whether PDF is required. Reuse: reporting foundation, soil/activity/calculator data. |
| Wallet | 58-60 | 58-60 ❌ | Add wallet account, credit balance, transaction, statement, certificate tables or mock endpoints; integrate with calculator/project only after business rules are clear. Module guards: future Wallet/Carbon module. Reuse: reports/storage for statements/certificates. | Build wallet dashboard, transaction history, statement download, carbon certificate preview/download. | M/L. Dependencies: payout/credit ownership, certificate format, payment/carbon-credit integration. Reuse: reporting/storage. |
| Admin | 68-74 | 68-70 🟡, 71-74 ❌ | Add admin queues for farmer verification, activity/evidence review, soil report/OCR verification, carbon project management, dealer approvals, agronomist advisory queue; reuse existing evidence review where possible; add APIs with role guards for `ADMIN`, `FPO_MANAGER`, `FIELD_COORDINATOR` as appropriate. Module guards: Carbon/Sustainability plus feature modules. Reuse: users, farmer profiles, evidence, notifications. | Build admin overview, farmer management verification status, activity verification queue, evidence viewer approve/reject, soil verification, carbon project management, dealer management, agronomist dashboard. | L. Dependencies: review ownership, approval statuses, SLA/workflow rules. Reuse: admin shell, evidence review, role visibility registry. |

## Keep / Rework / Avoid

Keep:

- `farmer_profiles` and `FarmerService`.
- `carbon_farm_plots`, extending it for vineyard plot/block fields.
- `carbon_soil_profiles`.
- Generic workflow/activity/task/evidence for activity tracking.
- Carbon and FPO modules, with visibility controlled centrally.

Rework:

- Carbon UI navigation so it follows the Excel flow.
- Carbon profile screens so farmer identity uses shared farmer profile fields
  and Carbon enrollment stays module-specific.
- Activity tracking so the wizard uses workflow/evidence instead of only simple
  Carbon logs.

Avoid:

- Creating duplicate farmer identity tables.
- Creating a separate `vineyard` module only because the Excel uses the word
  vineyard.
- Replacing workflow/activity/evidence with another activity engine.
- Deleting FPO module code.

## Clarifications To Ask Before Implementation

1. The workbook has 77 rows, but the requirement says 76 screens. Should Logout
   Confirmation be counted separately?
2. Should OTP be farmer-only, or should staff/admin also move to OTP?
3. Is mock OTP acceptable for UAT?
4. Which map provider should be used for polygon drawing?
5. Must polygon drawing/offline sync work offline in the first release?
6. Which KYC document types are required?
7. Are bank details only stored, or must they be verified?
8. Are weather, OCR, satellite, and AI diagnosis mock placeholders for the first
   release?
9. Is marketplace checkout real ordering/payment or UI-only first?
10. Is wallet/certificate data mock until carbon methodology is approved?
