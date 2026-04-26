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
│       ├── android-staff.yml    # Triggers on android/staff/** only
│       ├── android-patient.yml  # Placeholder
│       ├── backend.yml          # Placeholder
│       └── website.yml          # Placeholder
├── CLAUDE.md               # This file
└── README.md
```

---

## Android Staff App Structure

```
android/staff/
├── app/src/main/java/com/dentical/staff/
│   ├── data/
│   │   ├── local/
│   │   │   ├── dao/          # Daos.kt — all DAOs in one file
│   │   │   ├── entities/     # One file per entity
│   │   │   ├── Converters.kt
│   │   │   └── DenticalDatabase.kt (version 3, exportSchema = false)
│   │   └── repository/
│   │       ├── PatientRepository.kt
│   │       └── AppointmentRepository.kt
│   ├── di/
│   │   └── DatabaseModule.kt
│   ├── ui/
│   │   ├── theme/
│   │   ├── navigation/       # DenticalNavHost.kt
│   │   ├── login/            # ✅ Done
│   │   ├── dashboard/        # 🚧 Placeholder (nav wired)
│   │   ├── patients/         # ✅ Done
│   │   ├── appointments/     # ✅ Done
│   │   ├── billing/          # ⏳ Planned
│   │   ├── reminders/        # ⏳ Planned
│   │   └── settings/         # ⏳ Planned
│   ├── util/
│   │   ├── PasswordUtil.kt
│   │   └── PhoneUtil.kt
│   ├── DenticalApplication.kt
│   └── MainActivity.kt
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## Branch & Git Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Production only. Protected. Never commit directly. |
| `develop` | Integration branch. Merge here after each working feature. |
| `android/feature/*` | One branch per feature |
| `android/fix/*` | Bug fixes |

### Flow
```
android/feature/xxx → develop (PR) → main (PR + release tag)
```

### IMPORTANT — Code Access Between Sessions
- Claude can only read files from branches visible via GitHub connector
- Always merge working features to `develop` after testing
- At start of new session: tap + → Add from GitHub → select files needed
- For code edits, share the specific file via GitHub connector

### Current active branch
`android/feature/scaffold` — merge to `develop` when appointments build passes

### Version Tags
```
android-staff/v0.1.0-dev   ← current
android-patient/v0.1.0-dev ← future
backend/v0.1.0-dev         ← future
website/v0.1.0-dev         ← future
```

---

## CI/CD — GitHub Actions

- Path filters — only changed module triggers build
- Android debug APK built on every push to `develop`
- APK uploaded as artifact — downloadable for 7 days
- Build time: ~3-5 min (cached)
- Public repo = unlimited free minutes
- **To test:** Download APK from Actions → uninstall old → install new

---

## Tech Stack — Confirmed ✅

| Layer | Choice | Notes |
|-------|--------|-------|
| Language | Kotlin | ✅ Final |
| UI | Jetpack Compose | ✅ Final |
| Architecture | MVVM | ✅ Final |
| Local DB | Room (v3) | ✅ MVP, migrate to cloud later |
| DI | Hilt | ✅ Final |
| Navigation | Jetpack Navigation Compose | ✅ Final |
| Auth MVP | Local username/password + roles | ✅ Working |
| Auth Future | Google OAuth | Phase 2 |
| Backend Future | PostgreSQL | Phase 2 |

---

## Default Seeded Users (on fresh install)
| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN |
| dr.smith | dentist123 | DENTIST |
| dr.jones | dentist123 | DENTIST |

> Dummy dentists are for testing only — will be removed when Settings/user management is built.

---

## Roles & Permissions

| Role | Permissions |
|------|-------------|
| `ADMIN` | Full access, manage staff, assign roles, add users, delete patients |
| `DENTIST` | View own appointments, update treatment, view patients |
| `STAFF` | Appointments, patients, treatments, billing (no delete, no settings) |

---

## Database

- **Version:** 3
- **exportSchema:** false
- **Migration 1→2:** Recreates patients table with new fields
- **Migration 2→3:** Recreates appointments table with type enum and dentistId
- **Entities:** UserEntity, PatientEntity, AppointmentEntity, TreatmentEntity, InvoiceEntity

---

## Screen Structure

```
Login Screen ✅
└── Dashboard (Home) 🚧
    ├── Appointments ✅
    │   ├── Appointment List (list view) ✅
    │   ├── Appointment List (calendar view - day/week/month) ✅
    │   ├── Add Appointment ✅
    │   └── Appointment Detail ✅
    │       ├── Status update buttons ✅
    │       ├── Call button ✅
    │       └── WhatsApp button ✅
    ├── Patients ✅
    │   ├── Patient List ✅
    │   ├── Add New Patient ✅
    │   └── Patient Detail ✅
    │       ├── Overview Tab ✅
    │       ├── Treatments Tab (placeholder)
    │       └── Invoices Tab (placeholder)
    ├── Billing ⏳
    │   ├── Invoice List
    │   └── Invoice Detail
    ├── Reminders ⏳
    └── Settings ⏳ (Admin only)
        ├── Manage Staff
        ├── Add User
        └── Assign Roles
```

---

## Patient Feature — Spec ✅

### Patient Entity Fields
- `patientCode` — starts at 10001, incremental, unique
- `fullName`, `dateOfBirth`, `gender`
- `phone` — optional if "phone not available" checked
- `isPhoneAvailable` — checkbox
- `guardianName`, `guardianPhone` — required if minor (age < 18)
- `referralSource` — Walk-in, Referral from Doctor, Friend/Family, Social Media, Other
- `referralDetail` — conditional, required if not Walk-in
- `email`, `address`, `medicalConditions`, `allergies` — optional

---

## Appointments Feature — Spec ✅

### Fields
- Patient (real-time search by name, phone, ID)
- Dentist (dropdown of active dentists)
- Type (Consultation, Cleaning, Filling, Root Canal, Extraction, Braces, X-Ray, Whitening, Crown/Bridge, Other)
- Date + Time (pickers)
- Duration (15, 30, 45, 60, 90 min)
- Notes (optional)
- Status (auto = Scheduled)

### Statuses
Scheduled → Confirmed → In Progress → Completed / Cancelled / No Show

### Phone Number Formatting (PhoneUtil)
- Starts with + → use as is
- Starts with 00 → replace with +
- Starts with 0 → replace with +91
- Else → prepend +91

### Call & WhatsApp
- Priority: patient phone → guardian phone → disabled
- WhatsApp opens wa.me URL

---

## Roadmap

### Phase 1 — MVP (Current)
- [x] Repo structure & CI/CD
- [x] App scaffolding
- [x] Login + local auth + roles
- [x] Patient management
- [x] Appointment management
- [ ] Dashboard with real stats
- [ ] Treatment history
- [ ] Billing & invoices
- [ ] Push reminders
- [ ] Settings — manage staff & roles

### Phase 2 — Cloud
- [ ] PostgreSQL backend (language TBD)
- [ ] Migrate Room to API calls
- [ ] Google OAuth login
- [ ] Server-side roles & permissions
- [ ] Push notifications via FCM

### Phase 3 — Patient App
- [ ] Separate Kotlin app in `android/patient/`
- [ ] Book appointments, view records, payments
- [ ] Shares backend with staff app

---

## Key Decisions Made

| Decision | Choice | Reason |
|----------|--------|--------|
| Monorepo | Yes | Simpler, CI/CD handles isolation |
| Single `main` branch | Yes | Path filters handle module isolation |
| Staff & patient apps separate | Yes | Different users, security, distribution |
| Jetpack Compose | Yes | Modern standard |
| MVVM | Yes | Google standard |
| Room for MVP | Yes | Simple, offline first |
| Local auth for MVP | Yes | No backend needed yet |
| Patient code starts at 10001 | Yes | Business requirement |
| Dummy dentists seeded | Yes | Testing until Settings built |
| Merge to develop after each feature | Yes | Allows Claude to read code in new sessions |

---

## Pending Decisions

- [ ] Backend language & framework (Phase 2)
- [ ] Cloud provider for PostgreSQL (Phase 2)
- [ ] Google OAuth client ID (Phase 2)

---

## Working Commands (Termux)

```bash
# Navigate to repo
cd ~/storage/dentical_app

# Unzip Claude's files
unzip ~/storage/downloads/filename.zip -d ~/scaffold_temp
cp -r ~/scaffold_temp/dentical_app/* ~/storage/dentical_app/
rm -rf ~/scaffold_temp

# Commit and push
git add .
git commit -m "feat: description"
git push origin android/feature/branch-name

# Merge feature to develop (do after each working feature)
git checkout develop
git merge android/feature/scaffold
git push origin develop
```

---

## How to Use This File

**Start of each new Claude session:**
1. Tap + → Add from GitHub → select CLAUDE.md
2. Also add any specific code files you want Claude to read/edit
3. Claude has full context immediately

---

> Last updated: April 2026 — Appointments feature complete ✅
