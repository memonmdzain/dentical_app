# 🦷 Dentical App

A multi-module project for a dental clinic management system.

## Modules

| Module | Description | Version | Status |
|--------|-------------|---------|--------|
| `android/staff/` | Kotlin Android app for staff & admin | v0.1.0-dev | 🚧 In Development |
| `android/patient/` | Kotlin Android app for patients | TBD | ⏳ Planned |
| `backend/` | API Server | TBD | ⏳ Planned |
| `website/` | Web frontend | TBD | ⏳ Planned |

## Architecture

Each module is independently versioned and deployed.
- Changes to `android/staff/**` only trigger the Staff App pipeline
- Changes to `android/patient/**` only trigger the Patient App pipeline
- Changes to `backend/**` only trigger the backend pipeline
- Changes to `website/**` only trigger the website pipeline

## Branch Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Production only. Protected. Never commit directly. |
| `develop` | Integration branch. All features merge here first. |
| `android/feature/*` | One branch per Android feature |
| `android/fix/*` | Android bug fixes |
| `backend/feature/*` | Backend features (later) |
| `website/feature/*` | Website features (later) |

## Version Tagging Convention

```
android-staff/v0.1.0-dev
android-patient/v0.1.0-dev
backend/v0.1.0-dev
website/v0.1.0-dev
```

## Deployment Flow

```
android/feature/xxx
        ↓ PR merge
     develop  ←── Auto builds debug APK for testing
        ↓ PR merge
       main   ←── Auto builds release APK + tags version
```

## Apps

### Staff & Admin App (`android/staff/`)
Internal app for clinic staff. Distributed as private APK.
- Appointment management
- Patient records
- Treatment history
- Billing & payments
- Push reminders

### Patient App (`android/patient/`) — Planned
Public app for patients. Distributed via Play Store.
- Book appointments
- View own records
- Payments
- Notifications

---

> Built with ❤️ for Dentical Clinic
