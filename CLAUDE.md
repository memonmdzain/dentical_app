# 🦷 Dentical App — Claude Context File

> Paste this file at the start of every new Claude session to restore full project context.

---

## Project Overview

A dental clinic management system built as a multi-module monorepo.
- **Repo:** github.com/memonmdzain/dentical_app
- **Owner:** memonmdzain (personal GitHub account)
- **Visibility:** Public during development, private after launch

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

### Version Tags
```
android-staff/v0.1.0-dev   ← current
android-patient/v0.1.0-dev ← future
backend/v0.1.0-dev         ← future
website/v0.1.0-dev         ← future
```

---

## CI/CD — GitHub Actions

- Each module has its own workflow file
- Path filters ensure only the changed module builds
- Android debug APK built on every push to `develop`
- APK uploaded as GitHub Actions artifact (downloadable 7 days)
- Build time: ~5-10 min per run
- Public repo = unlimited free CI/CD minutes

---

## Tech Stack — Confirmed ✅

| Layer | Choice | Notes |
|-------|--------|-------|
| Language | Kotlin | ✅ Final |
| UI | Jetpack Compose | ✅ Final |
| Architecture | MVVM | ✅ Final |
| Local DB | Room | ✅ MVP only, migrate to cloud later |
| Auth MVP | Local username/password + roles | ✅ MVP only |
| Auth Future | Google OAuth | Phase 2 |
| Backend Future | PostgreSQL | Phase 2 |

---

## Roles & Permissions

| Role | Permissions |
|------|-------------|
| `Admin` | Full access, manage staff, assign roles, add users |
| `Staff` | Limited access — appointments, patients, treatments |

- MVP: First admin account seeded in local DB on first launch
- Admin can add more users and assign roles locally
- Phase 2: Migrate to server-side roles with Google OAuth

---

## Screen Structure

```
Login Screen
└── Dashboard (Home)
    ├── Appointments
    │   ├── Appointment List
    │   ├── New Appointment
    │   └── Appointment Detail
    ├── Patients
    │   ├── Patient List
    │   ├── New Patient
    │   └── Patient Detail
    │       └── Treatment History
    ├── Billing
    │   ├── Invoice List
    │   └── Invoice Detail
    ├── Reminders
    │   └── Send Reminders
    └── Settings (Admin only)
        ├── Manage Staff
        ├── Add User
        └── Assign Roles
```

---

## Roadmap

### Phase 1 — MVP (Current)
- [x] Repo structure & CI/CD setup
- [ ] App scaffolding & architecture
- [ ] Local Room database
- [ ] Local username/password auth
- [ ] Staff & Admin roles
- [ ] Appointment management
- [ ] Patient records
- [ ] Treatment history
- [ ] Billing & invoices
- [ ] Push reminders

### Phase 2 — Cloud
- [ ] PostgreSQL backend (language TBD)
- [ ] Migrate Room to API calls
- [ ] Google OAuth login
- [ ] Server-side roles & permissions
- [ ] Push notifications via FCM

### Phase 3 — Patient App
- [ ] Separate Kotlin app in `android/patient/`
- [ ] Patients book appointments
- [ ] View own records & bills
- [ ] Online payments
- [ ] Shares same backend as staff app

---

## Android Staff App Structure

```
android/staff/
├── app/
│   ├── src/main/
│   │   ├── java/com/dentical/staff/
│   │   │   ├── data/
│   │   │   │   ├── local/        # Room DB, DAOs, Entities
│   │   │   │   └── repository/   # Repositories
│   │   │   ├── di/               # Dependency Injection (Hilt)
│   │   │   ├── domain/           # Use cases
│   │   │   ├── ui/
│   │   │   │   ├── theme/        # Compose theme
│   │   │   │   ├── login/        # Login screen
│   │   │   │   ├── dashboard/    # Dashboard screen
│   │   │   │   ├── appointments/ # Appointment screens
│   │   │   │   ├── patients/     # Patient screens
│   │   │   │   ├── billing/      # Billing screens
│   │   │   │   ├── reminders/    # Reminders screen
│   │   │   │   └── settings/     # Settings screen (Admin)
│   │   │   └── MainActivity.kt
│   │   └── res/
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew
```

---

## Dev Environment

- Developer currently **mobile only** (travelling)
- Tools: Termux + Git on Android, GitHub Mobile App
- Claude generates files → developer commits via Termux
- PRs reviewed and merged via GitHub Mobile App
- APK downloaded from GitHub Actions artifacts → installed directly on phone

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
| Room for MVP | Yes | Simple, offline, migrate to cloud later |
| Local auth for MVP | Yes | No backend needed for MVP |
| Separate roles | Yes | Admin & Staff with different permissions |

---

## Pending Decisions

- [ ] Backend language & framework (Phase 2)
- [ ] Cloud provider for PostgreSQL (Phase 2)
- [ ] FCM push notification setup (Phase 2)
- [ ] Google OAuth client ID setup (Phase 2)

---

## Current Status

**Phase 1 — MVP in progress**
- ✅ Repo structure created
- ✅ CI/CD workflows set up
- ✅ Tech stack decided
- 🚧 App scaffolding — IN PROGRESS
- ⏳ Feature development — pending

## What's Next

1. Scaffold `android/staff/` project structure
2. Fix `android-staff.yml` CI to build real project
3. First feature: Login screen + local auth

---

## How to Use This File

At the start of each new Claude session:
1. Tap + → Add from GitHub → select CLAUDE.md
2. Claude will have full context immediately
3. No re-explaining needed

---

> Last updated: April 2026 — Phase 1 scaffolding started
