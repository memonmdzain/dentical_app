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

## Deployment Flow

```
android/feature/xxx
        ↓ PR merge
     develop  ←── Auto builds debug APK for testing
        ↓ PR merge
       main   ←── Auto builds release APK + tags version
```

## Tech Stack — Staff App

| Layer | Choice |
|-------|--------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Hilt + Room |
| Cloud DB | Supabase (PostgreSQL) |
| Sync | Offline-first — Room primary, Supabase cloud backup |
| Auth | Local username/password (Google OAuth planned) |

## Data Sync Strategy

The staff app is **offline-first**: Room is the primary data store and all UI reads from Room reactive Flows.

- **Writes** sync to Supabase immediately in the background (fire-and-forget)
- **Reads** sync from Supabase automatically every time the app opens, and on demand via the sync button available on every screen
- Multi-device: data is shared across devices through Supabase; any device can press the sync button to pull latest

## Apps

### Staff & Admin App (`android/staff/`)
Internal app for clinic staff. Distributed as private APK.
- **Dashboard**: ongoing treatments count, today's collections, total outstanding — each stat tappable to a drill-down patient list with Schedule / Call / WhatsApp actions
- **Appointments**: list view, calendar (day/week/month), add, edit, detail with status management
- **Patients**: records with financial summary (Total Billed / Paid / Outstanding)
- **Treatments**: add, edit, ongoing/past sections, standalone visits section, FIFO payment allocation
- **Visits**: add (Cash/GPay/Bank Transfer), edit; overpayment blocked at entry
- **Treatment detail**: full visit history, per-visit edit, mark complete (payment gate), cancel (partial charge + refund), reopen
- **Cloud sync**: auto-sync on every app open; manual sync button (with 30-sec cooldown) on every screen
- Billing & payments — planned
- Push reminders — planned

### Patient App (`android/patient/`) — Planned
Public app for patients. Distributed via Play Store.
- Book appointments
- View own records
- Payments
- Notifications

## Supabase Setup

Schema SQL (tables, RLS, indexes): `android/staff/supabase_schema.sql`

Required secrets:
- `local.properties` (gitignored, never committed): `SUPABASE_URL` and `SUPABASE_ANON_KEY`
- GitHub Actions: add both as separate repository secrets named `SUPABASE_URL` and `SUPABASE_ANON_KEY`

---

> Built with ❤️ for Dentical Clinic
