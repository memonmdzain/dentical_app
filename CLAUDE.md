# 🦷 Dentical App — Claude Context File

> Paste this file at the start of every new Claude session to restore full project context.
> Tip: Tap + → Add from GitHub → select this file to load it instantly.

---

## Project Overview

A dental clinic management system built as a multi-module monorepo.
- **Repo:** github.com/memonmdzain/dentical_app
- **Owner:** memonmdzain (personal GitHub account)
- **Visibility:** Public during development, private after launch
- **Dev environment:** Mobile only (travelling) — Termux + Git on Android
- **Repo location on device:** ~/storage/dentical_app

---

## Repo Structure

```
dentical_app/
├── android/
│   ├── staff/              # Staff & Admin Kotlin app (CURRENT FOCUS)
│   └── patient/            # Patient app (future)
├── backend/                # API server (future, language TBD)
├── website/                # Web frontend (future)
├── .github/
│   └── workflows/
│       ├── android-staff.yml
│       ├── android-patient.yml  # Placeholder
│       ├── backend.yml          # Placeholder
│       └── website.yml          # Placeholder
├── CLAUDE.md
└── README.md
```

---

## Android Staff App Structure

```
android/staff/app/src/main/java/com/dentical/staff/
├── data/
│   ├── local/
│   │   ├── dao/Daos.kt
│   │   ├── entities/
│   │   │   ├── UserEntity.kt        (roles: ADMIN, DENTIST, STAFF)
│   │   │   ├── PatientEntity.kt
│   │   │   ├── AppointmentEntity.kt (type enum + dentistId)
│   │   │   └── TreatmentAndInvoiceEntities.kt
│   │   ├── Converters.kt
│   │   └── DenticalDatabase.kt     (version 6, exportSchema=false)
│   ├── remote/
│   │   ├── RemoteDtos.kt           (7 @Serializable DTOs with @SerialName snake_case)
│   │   ├── RemoteMappers.kt        (Entity↔DTO extension functions)
│   │   ├── SupabaseSyncHelper.kt   (fireAndForget write-sync, delete)
│   │   └── SyncManager.kt          (@Singleton — orchestrates full pull; auto on app open + sync button)
│   └── repository/
│       ├── PatientRepository.kt    (pullFromSupabase)
│       ├── AppointmentRepository.kt (pullFromSupabase)
│       └── TreatmentRepository.kt  (pullAll, pullForPatient)
├── di/
│   ├── DatabaseModule.kt
│   └── SupabaseModule.kt           (SupabaseClient + ApplicationScope)
├── ui/
│   ├── theme/
│   ├── navigation/DenticalNavHost.kt
│   ├── login/                       ✅ Done
│   ├── dashboard/                   ✅ Done
│   │   ├── DashboardScreen.kt
│   │   ├── DashboardViewModel.kt
│   │   ├── DashboardPatientListScreen.kt
│   │   └── DashboardPatientListViewModel.kt
│   ├── patients/                    ✅ Done
│   ├── appointments/                ✅ Done
│   ├── billing/                     ⏳ Planned
│   ├── reminders/                   ⏳ Planned
│   └── settings/                    ⏳ Planned
└── util/
    ├── NetworkMonitor.kt            (ConnectivityManager wrapper)
    ├── PasswordUtil.kt
    └── PhoneUtil.kt
```

---

## Branch & Git Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Production only. Never commit directly. |
| `develop` | Integration branch. Merge here after each working feature. |
| `android/feature/*` | One branch per feature |
| `android/fix/*` | Bug fixes |

### Flow
```
android/feature/xxx → develop (PR) → main (PR + release tag)
```

### Branch Naming Convention
**IMPORTANT for Claude sessions:** Always use the project convention — never use `claude/*` auto-generated names.
- Features: `android/feature/<short-description>` (e.g. `android/feature/user-role-management`)
- Bug fixes: `android/fix/<short-description>` (e.g. `android/fix/login-crash`)
- If a session starts on a `claude/*` branch, immediately rename it to follow the convention above.

### IMPORTANT — Code Access Between Sessions
- Merge working features to `develop` after each test
- New session: tap + → Add from GitHub → select files needed
- Claude reads from whatever branch you share

### Current active branch
`android/feature/user-role-management` — User & Roles management module (in progress)

---

## CI/CD — GitHub Actions

- Path filters per module
- Android debug APK on every push to develop
- APK artifact downloadable for 7 days
- Public repo = unlimited free minutes
- Requires two GitHub repository secrets: `SUPABASE_URL` and `SUPABASE_ANON_KEY`
- Workflow validates secrets before building (fails fast if missing)

---

## Tech Stack ✅

| Layer | Choice |
|-------|--------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM |
| Local DB | Room v3 (v2.6.1) |
| DI | Hilt |
| Navigation | Jetpack Navigation Compose |
| Cloud DB | Supabase (PostgreSQL) via supabase-kt v3.1.4 + postgrest-kt |
| HTTP | Ktor Android engine v3.1.3 |
| Serialization | kotlinx.serialization |
| Auth MVP | Local username/password + roles |
| Auth Phase 2 | Google OAuth via Supabase Auth |

---

## Supabase Setup

- Schema SQL: `android/staff/supabase_schema.sql` (tables + RLS + notes)
- `dentist_id` FK removed from appointments and treatments — dentists are seeded locally in Room only
- RLS enabled on all tables; transitional `anon_all` policy until Google OAuth is added
- `local.properties` (gitignored): `SUPABASE_URL` and `SUPABASE_ANON_KEY`
- GitHub Actions: add both as separate repository secrets

---

## Seeded Users (fresh install)

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN |
| dr.smith | dentist123 | DENTIST |
| dr.jones | dentist123 | DENTIST |

> Dummy dentists for testing — removed when Settings feature is built.

---

## Roles

| Role | Permissions |
|------|-------------|
| ADMIN | Full access, manage staff, roles, delete |
| DENTIST | Own appointments, treatments, patients |
| STAFF | Appointments, patients, billing |

---

## Database

- Version: 6, exportSchema: false
- `fallbackToDestructiveMigration()` enabled — wipes DB if no migration path found (dev phase only; remove before launch)
- Migration 1→2: patients table rebuilt
- Migration 2→3: appointments table rebuilt with type + dentistId
- Migration 3→4: treatments + visits + treatment_visit_cross_ref tables added
- Migration 4→5: tables recreated without SQL DEFAULT clauses (Room schema fix) + paymentMode column on visits
- Migration 5→6: force clean slate for any device with a broken version-5 schema (same drop+recreate)

---

## Sync Architecture

**Offline-first.** Room is the primary store. UI always reads from Room reactive Flows.

### Write sync
Every Room mutation fires a background `upsert`/`delete` to Supabase via `SupabaseSyncHelper.fireAndForget`. Failures are logged, never block the user.

### Read sync — `SyncManager` (@Singleton)
- **Auto on app open**: observes `ProcessLifecycleOwner` ON_START — fires a full pull every time the app comes to the foreground
- **Manual sync button**: `Icons.Default.Sync` in every screen's TopAppBar; calls `syncManager.syncAll()` with a 30-second cooldown; shows spinner while syncing, greyed-out during cooldown
- **Full pull order** (FK-safe): patients → appointments → treatments → visits → treatment_visit_cross_ref
- **Per-patient pull**: `TreatmentRepository.pullForPatient(id)` fires the first time a `PatientDetailScreen` opens for a given patient

---

## Screen Structure

```
Login ✅
└── Dashboard ✅
    ├── Ongoing Treatments card ✅ (count, tappable)
    │   └── Ongoing Patients List ✅
    │       └── Patient card: outstanding balance, Schedule / Call / WhatsApp icons, tap → Patient Detail
    ├── Today's Collections card ✅ (₹ sum, informational)
    ├── Total Outstanding card ✅ (₹ sum, tappable)
    │   └── Outstanding Patients List ✅
    │       └── Patient card: same format as above
    ├── Appointments ✅
    │   ├── List View ✅
    │   ├── Calendar — Day View ✅
    │   ├── Calendar — Week View ✅ (date strip + badge counts + day appointments)
    │   ├── Calendar — Month View ✅ (grid + badge counts + day appointments)
    │   ├── Add Appointment ✅
    │   ├── Appointment Detail ✅ (Call, WhatsApp, status update)
    │   └── Edit Appointment ✅ (disabled for Completed/Cancelled/No Show)
    ├── Patients ✅
    │   ├── Patient List ✅
    │   ├── Add Patient ✅
    │   └── Patient Detail ✅ (Overview, Treatments+Visits sectioned, Invoices placeholder)
    │       ├── Treatments tab: Ongoing / Past sections + Standalone Visits section ✅
    │       ├── Add Treatment ✅
    │       ├── Edit Treatment ✅
    │       ├── Add Visit ✅ (payment mode: Cash/GPay/Bank Transfer; blocks overpayment)
    │       └── Treatment Detail ✅
    │           ├── Visits list with edit per visit ✅
    │           ├── Edit Visit ✅
    │           ├── Mark Complete (confirmation dialog + payment gate + FIFO) ✅
    │           ├── Cancel Treatment (partial charge dialog + live balance + refund checkbox) ✅
    │           │   └── Refund auto-recorded as negative amountPaid visit
    │           └── Reopen Treatment (quoted cost dialog as confirmation) ✅
    ├── Billing ⏳
    ├── Reminders ⏳
    └── Settings ⏳ (Admin only)
```

All screens have a Sync button (TopAppBar) tied to the shared `SyncManager`.

---

## Patient Spec ✅

- patientCode: starts at 10001, auto-incremented
- Phone optional via checkbox "phone not available"
- Guardian fields for minors (age < 18 from DOB)
- Referral: dropdown + conditional detail field
- Medical conditions, allergies optional

## Appointments Spec ✅

- Patient search: real-time by name, phone, ID
- Dentist: dropdown of active DENTIST role users
- Types: Consultation, Cleaning, Filling, Root Canal, Extraction, Braces, X-Ray, Whitening, Crown/Bridge, Other
- Duration: 15/30/45/60/90 min
- Statuses: Scheduled → Confirmed → In Progress → Completed/Cancelled/No Show
- Edit disabled for closed statuses (Completed, Cancelled, No Show)
- Call/WhatsApp: patient phone → guardian phone → disabled
- Phone formatting: + prefix kept, 00→+, 0→+91, else prepend +91

---

## Roadmap

### Phase 1 — MVP
- [x] Repo + CI/CD
- [x] Scaffolding
- [x] Login + auth + roles
- [x] Patients
- [x] Appointments (list, calendar, add, edit, detail)
- [x] Treatments + Visits (add, detail, status, payment mode, financial summary)
- [x] Dashboard stats (ongoing count, today's collections, total outstanding, drill-down patient lists)
- [x] Supabase cloud sync (offline-first, write on mutation, full read sync on app open + sync button)
- [ ] Billing & invoices
- [ ] Push reminders
- [ ] Settings — staff management

### Phase 2 — Auth & Notifications
- [ ] Google OAuth via Supabase Auth (replaces anon RLS policies with per-user policies)
- [ ] FCM push notifications

### Phase 3 — Patient App
- [ ] android/patient/ Kotlin app
- [ ] Book, view records, pay

---

## Key Decisions

| Decision | Choice |
|----------|--------|
| Monorepo | Yes |
| Single master branch | Yes — CI path filters handle isolation |
| Staff/patient apps separate | Yes |
| Jetpack Compose | Yes |
| MVVM + Hilt + Room | Yes |
| Patient code from 10001 | Yes |
| Merge to develop after each feature | Yes — so Claude can read code |
| Week view: date strip + count badges | Yes |
| Month view: grid + count badges | Yes |
| Edit disabled on closed status | Yes |
| Treatment sections: Ongoing / Past | Yes |
| fallbackToDestructiveMigration (dev) | Yes — remove before Play Store launch |
| Edit visits | Yes — date, dentist, amount, payment mode, notes |
| Edit treatments | Yes — all fields editable |
| Reopen treatment | Yes — works for both Completed and Cancelled |
| Payment gate on Mark Complete | Yes — FIFO allocation across linked treatments; blocks if outstanding > ₹0 |
| FIFO payment allocation | Allocate visit payment to treatments sorted by startDate, then id |
| Visits shown only in Treatment Detail | Yes — removed from PatientDetail TreatmentsTab |
| Standalone visits in PatientDetail | Yes — shown in a dedicated section in the Treatments tab |
| Add Visit overpayment prevention | Yes — amountPaid blocked if it exceeds remaining outstanding across linked treatments |
| Cancel treatment flow | Partial charge dialog (blank default) → live signed balance preview → refund checkbox if balance < 0 |
| Refund recording | Negative amountPaid visit linked to cancelled treatment; reduces SUM(amountPaid) in financial summary |
| Cancelled treatments in Total Billed | Yes — quotedCost included regardless of status (no status filter on SUM) |
| FIFO includes completed/cancelled | Yes — skips only null-cost (cancelled at ₹0); all others participate in allocation |
| Outstanding badge recomputation | Triggered by both treatment and visit changes in PatientDetailViewModel |
| Mark Complete confirmation | Yes — confirmation dialog before executing; payment gate runs after confirm |
| Reopen Treatment confirmation | Yes — quoted cost dialog acts as the confirmation step |
| Reopen prompts for quoted cost | Yes — pre-filled with current value (blank if none); staff can update before reopening |
| Dashboard stat cards | 3 cards: Ongoing Treatments (tappable), Today's Collections, Total Outstanding (tappable) |
| Dashboard outstanding formula | Global: max(0, SUM(quotedCost) + SUM(standalone costCharged) − SUM(amountPaid)) across all patients |
| Dashboard ongoing patients list | Patients with ≥ 1 treatment in ONGOING status; reactive to treatment status changes |
| Dashboard outstanding patients list | Patients where per-patient outstanding > ₹0; reactive to patient add/remove |
| Dashboard patient card | Icon-only action row (Schedule, Call, WhatsApp); tap card → Patient Detail |
| AddAppointment patientId pre-fill | Optional `?patientId=` nav arg; pre-selects patient when navigated from dashboard |
| Cloud sync strategy | Offline-first; Room is primary; Supabase is cloud backup |
| Write sync | fireAndForget on every Room mutation; failures logged silently |
| Read sync trigger | Auto: ProcessLifecycleOwner ON_START (every app foreground); Manual: sync button per screen |
| Sync button cooldown | 30 seconds after last sync; shared across all screens via SyncManager singleton |
| Full sync pull order | patients → appointments → treatments → visits → cross_refs (FK-safe) |
| dentist_id FK in Supabase | Removed — dentists seeded locally only, FK would break every write |
| Supabase RLS | Enabled; transitional anon_all policies until Google OAuth replaces them |
| Cross-ref sync fix | Visit and cross_refs sequenced in one fireAndForget block (prevents FK race condition) |
| Supabase secrets | local.properties (gitignored) locally; two separate GitHub repo secrets for CI |

---

## Termux Commands

```bash
cd ~/storage/dentical_app

# Commit & push
git add .
git commit -m "feat: description"
git push origin android/feature/xxx

# Merge to develop (after testing)
git checkout develop
git merge android/feature/xxx
git push origin develop
```

---

## Session Tips (Token Saving)

- Start each session by sharing CLAUDE.md + relevant code files via + → Add from GitHub
- State the feature and decisions upfront — no need to re-discuss
- One session = one feature
- For bug fixes just paste the error — say "fix it"

---

> Last updated: May 2026 — android/feature/user-role-management: User & Roles management module (dynamic RBAC — roles table, permissions per entity/screen, many-to-many user↔role via cross-ref, session persistence via DataStore, profile screen, admin user/role CRUD UI)
