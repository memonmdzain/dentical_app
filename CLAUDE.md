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
│   │   │   └── DenticalDatabase.kt
│   │   └── repository/
│   │       └── PatientRepository.kt
│   ├── di/
│   │   └── DatabaseModule.kt
│   ├── ui/
│   │   ├── theme/
│   │   ├── login/            # ✅ Done
│   │   ├── dashboard/        # 🚧 Placeholder
│   │   ├── patients/         # ✅ Done
│   │   ├── appointments/     # ⏳ Next
│   │   ├── billing/          # ⏳ Planned
│   │   ├── reminders/        # ⏳ Planned
│   │   └── settings/         # ⏳ Planned
│   ├── util/
│   │   └── PasswordUtil.kt
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
| `develop` | Integration branch. All features merge here first. |
| `android/feature/*` | One branch per feature |
| `android/fix/*` | Bug fixes |

### Flow
```
android/feature/xxx → develop (PR) → main (PR + release tag)
```

### Current active branch
`android/feature/scaffold`

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
| Local DB | Room (v2) | ✅ MVP, migrate to cloud later |
| DI | Hilt | ✅ Final |
| Navigation | Jetpack Navigation Compose | ✅ Final |
| Auth MVP | Local username/password + roles | ✅ Working |
| Auth Future | Google OAuth | Phase 2 |
| Backend Future | PostgreSQL | Phase 2 |

---

## Default Admin Credentials
- **Username:** admin
- **Password:** admin123
- Seeded on first app launch via Room DB callback

---

## Roles & Permissions

| Role | Permissions |
|------|-------------|
| `ADMIN` | Full access, manage staff, assign roles, add users, delete patients |
| `STAFF` | Appointments, patients, treatments, billing (no delete, no settings) |

- MVP: First admin seeded locally on first launch
- Phase 2: Server-side roles with Google OAuth

---

## Database

- **Version:** 2
- **Migration:** 1→2 drops and recreates patients table with new fields
- **Entities:** UserEntity, PatientEntity, AppointmentEntity, TreatmentEntity, InvoiceEntity

---

## Screen Structure

```
Login Screen ✅
└── Dashboard (Home) 🚧
    ├── Appointments ⏳
    │   ├── Appointment List
    │   ├── New Appointment
    │   └── Appointment Detail
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

## Patient Feature — Spec (Completed ✅)

### Patient Entity Fields
- `id` — auto increment PK
- `patientCode` — starts at 10001, incremental, unique
- `fullName`, `dateOfBirth`, `gender`
- `phone` — optional if checkbox "phone not available" checked
- `isPhoneAvailable` — checkbox
- `guardianName`, `guardianPhone` — required if patient is minor (age < 18)
- `referralSource` — dropdown: Walk-in, Referral from Doctor, Friend/Family, Social Media, Other
- `referralDetail` — conditional text, required if not Walk-in. Label changes by source
- `email`, `address`, `medicalConditions`, `allergies` — optional

### Dynamic Form Rules
| Condition | Behaviour |
|-----------|-----------|
| DOB < 18 | Guardian fields appear and become required |
| "Phone not available" checked | Phone disabled, not required |
| Minor + phone not available | Guardian phone becomes required |
| Referral ≠ Walk-in | Detail field appears, required |

---

## Roadmap

### Phase 1 — MVP (Current)
- [x] Repo structure & CI/CD
- [x] App scaffolding
- [x] Login + local auth + roles
- [x] Patient management (list, add, detail)
- [ ] Appointment management
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
- [ ] Book appointments
- [ ] View own records & bills
- [ ] Online payments
- [ ] Shares backend with staff app

---

## Key Decisions Made

| Decision | Choice | Reason |
|----------|--------|--------|
| Monorepo | Yes | Simpler, CI/CD handles isolation |
| Single `main` branch | Yes | Path filters handle module isolation |
| Staff & patient apps separate | Yes | Different users, security, distribution |
| Start with staff app | Yes | Core clinic operations first |
| Public repo during dev | Yes | Unlimited free CI/CD minutes |
| Jetpack Compose | Yes | Modern, recommended for new apps |
| MVVM | Yes | Google standard, clean architecture |
| Room for MVP | Yes | Simple, offline first |
| Local auth for MVP | Yes | No backend needed yet |
| Separate roles | Yes | Admin & Staff with different permissions |
| Patient code starts at 10001 | Yes | Business requirement |
| Staff & patient apps separate | Yes | Different security, distribution |

---

## Pending Decisions

- [ ] Appointments — linked to specific dentist? (likely yes)
- [ ] Appointments — calendar view or list view?
- [ ] Backend language & framework (Phase 2)
- [ ] Cloud provider for PostgreSQL (Phase 2)
- [ ] Google OAuth client ID (Phase 2)

---

## How to Use This File

**Start of each new Claude session:**
1. Tap + → Add from GitHub → select CLAUDE.md
2. Paste content as first message
3. Claude has full context immediately

---

## Working Commands (Termux)

```bash
# Navigate to repo
cd ~/storage/dentical_app

# Check status
git status
git log --oneline

# New feature branch
git checkout -b android/feature/feature-name

# After downloading files from Claude
unzip ~/storage/downloads/filename.zip -d ~/scaffold_temp
cp -r ~/scaffold_temp/dentical_app/* ~/storage/dentical_app/
rm -rf ~/scaffold_temp

# Commit and push
git add .
git commit -m "feat: description"
git push origin android/feature/feature-name
```

---

> Last updated: April 2026 — Patients feature complete ✅
