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
│   └── repository/
│       ├── PatientRepository.kt
│       ├── AppointmentRepository.kt
│       └── TreatmentRepository.kt
├── di/DatabaseModule.kt
├── ui/
│   ├── theme/
│   ├── navigation/DenticalNavHost.kt
│   ├── login/                       ✅ Done
│   ├── dashboard/                   🚧 Placeholder
│   ├── patients/                    ✅ Done
│   ├── appointments/                ✅ Done
│   ├── billing/                     ⏳ Planned
│   ├── reminders/                   ⏳ Planned
│   └── settings/                    ⏳ Planned
└── util/
    ├── PasswordUtil.kt
    └── PhoneUtil.kt
```

---

## Branch & Git Strategy

| Branch | Purpose |
|--------|---------|
| `master` | Production only. Never commit directly. |
| `develop` | Integration branch. Merge here after each working feature. |
| `android/feature/*` | One branch per feature |
| `android/fix/*` | Bug fixes |

### Flow
```
android/feature/xxx → develop (PR) → master (PR + release tag)
```

### IMPORTANT — Code Access Between Sessions
- Merge working features to `develop` after each test
- New session: tap + → Add from GitHub → select files needed
- Claude reads from whatever branch you share

### Current active branch
`treatment-and-visit-bugfix` — pushed, ready to merge into `develop`

---

## CI/CD — GitHub Actions

- Path filters per module
- Android debug APK on every push to develop
- APK artifact downloadable for 7 days
- Public repo = unlimited free minutes
- Uninstall old APK before installing new one

---

## Tech Stack ✅

| Layer | Choice |
|-------|--------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM |
| Local DB | Room v3 |
| DI | Hilt |
| Navigation | Jetpack Navigation Compose |
| Auth MVP | Local username/password + roles |
| Auth Phase 2 | Google OAuth |
| Backend Phase 2 | PostgreSQL |

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

## Data Loading Strategy

### Phase 1 — Local (current)
- Room + Kotlin Flow: reactive streams, emit only on DB writes → already "live cache"
- ViewModels collect flows into StateFlow — data retained across recompositions
- UI screens lazy-load via LazyColumn; data fetched only for the active screen
- No explicit TTL needed: Room invalidates its query cache on writes, not time

### Phase 2 — Backend
- Repository layer will add an in-memory cache (Map + timestamp) per entity type
- TTL default: 5 minutes. Stale check: `System.currentTimeMillis() - cachedAt > ttlMs`
- Cache hit → return cached Flow. Cache miss / stale → network fetch → write to Room → Room flow emits
- Room DB continues to serve as the persistent offline cache between sessions
- `CacheManager` singleton (Hilt @Singleton) will hold the TTL map, injected into repositories

---

## Screen Structure

```
Login ✅
└── Dashboard 🚧
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
- [ ] Dashboard stats
- [ ] Billing & invoices
- [ ] Push reminders
- [ ] Settings — staff management

### Phase 2 — Cloud
- [ ] PostgreSQL backend
- [ ] Google OAuth
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
| Phase 2 TTL cache default | 5 minutes, in-memory, per repository |
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

---

## Termux Commands

```bash
cd ~/storage/dentical_app

# Unzip Claude output
unzip ~/storage/downloads/filename.zip -d ~/scaffold_temp
cp -r ~/scaffold_temp/dentical_app/* ~/storage/dentical_app/
rm -rf ~/scaffold_temp

# Commit & push
git add .
git commit -m "feat: description"
git push origin android/feature/scaffold

# Merge to develop (after testing)
git checkout develop
git merge android/feature/scaffold
git push origin develop
```

---

## Session Tips (Token Saving)

- Start each session by sharing CLAUDE.md + relevant code files via + → Add from GitHub
- State the feature and decisions upfront — no need to re-discuss
- One session = one feature
- For bug fixes just paste the error — say "fix it"
- str_replace edits are cheaper than full file rewrites

---

> Last updated: April 2026 — treatment-and-visit-bugfix: overpayment prevention on Add Visit; standalone visits section in PatientDetail; cancel treatment with partial charge, live balance, refund dialog, and auto negative-payment visit; cancelled treatments included in Total Billed; outstanding badge reactive to visit changes; Mark Complete confirmation dialog; Reopen Treatment prompts for quoted cost
