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
- Android debug APK is built on every push to `develop`
- APK is uploaded as a GitHub Actions artifact (downloadable for 7 days)
- Build time: ~5-10 min per run
- Free tier: 2,000 min/month (public repo = unlimited)

---

## Android Staff App

### Purpose
Internal app for dental clinic staff and admins.
Distributed as a private APK (not on Play Store).

### Features
- [ ] Appointment booking & management
- [ ] Patient records
- [ ] Treatment history
- [ ] Billing & payments
- [ ] Push reminders

### Tech Stack — Pending Decisions
- **Language:** Kotlin ✅
- **UI Framework:** TBD (Jetpack Compose vs XML)
- **Architecture:** TBD (MVVM recommended)
- **Local DB:** TBD (Room recommended)
- **Backend/API:** TBD
- **Auth:** TBD

### Progress
- [ ] Project scaffolding
- [ ] GitHub Actions setup
- [ ] App architecture setup
- [ ] Feature development

---

## Patient App (Future)

- Separate Kotlin app in `android/patient/`
- Public Play Store distribution
- Features: book appointments, view own records, payments, notifications
- Will share same backend as staff app

---

## Dev Environment

- Developer is currently **mobile only** (travelling)
- Tools: Termux + Git on Android, GitHub Mobile App
- Claude generates files → developer commits via Termux
- PRs reviewed and merged via GitHub Mobile App
- APK downloaded from GitHub Actions artifacts and installed directly

---

## Key Decisions Made

| Decision | Choice | Reason |
|----------|--------|--------|
| Monorepo | Yes | Simpler to manage, CI/CD handles isolation |
| Single `main` branch | Yes | Path filters in CI/CD handle module isolation |
| Staff & patient apps separate | Yes | Different users, security, distribution |
| Start with staff app | Yes | Core clinic operations first |
| Public repo during dev | Yes | Unlimited free CI/CD minutes |

---

## Pending Decisions

- [ ] UI framework: Jetpack Compose vs XML Views
- [ ] Architecture pattern: MVVM (recommended)
- [ ] Local database: Room (recommended)
- [ ] Backend language & framework
- [ ] Authentication method
- [ ] Cloud provider for backend

---

## What's Next

1. Decide Android tech stack (Compose vs XML, architecture)
2. Scaffold the staff app project structure
3. Set up GitHub Actions Android build
4. Begin first feature: Appointment Management

---

## How to Use This File

At the start of each new Claude session:
1. Open this file
2. Copy all content
3. Paste as your first message to Claude
4. Claude will have full context immediately

---

> Last updated: April 2026
