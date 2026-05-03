-- ============================================================
-- Dentical Staff App — Supabase Schema
-- Run in: Supabase Dashboard → SQL Editor → New Query
-- ============================================================

-- ── 1. Create Tables ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS users (
  id            bigserial PRIMARY KEY,
  username      text NOT NULL UNIQUE,
  password_hash text NOT NULL,
  full_name     text NOT NULL,
  role          text NOT NULL,        -- ADMIN | DENTIST | STAFF
  is_active     boolean NOT NULL DEFAULT true,
  created_at    bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS patients (
  id                  bigserial PRIMARY KEY,
  patient_code        text NOT NULL UNIQUE,
  full_name           text NOT NULL,
  date_of_birth       bigint,         -- nullable: not all patients have a known DOB
  gender              text NOT NULL,
  phone               text,
  is_phone_available  boolean NOT NULL DEFAULT true,
  guardian_name       text,
  guardian_phone      text,
  referral_source     text NOT NULL,
  referral_detail     text,
  email               text,
  address             text,
  medical_conditions  text,
  allergies           text,
  created_at          bigint NOT NULL,
  updated_at          bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS appointments (
  id                bigserial PRIMARY KEY,
  patient_id        bigint NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  dentist_id        bigint,           -- no FK: dentists seeded locally only
  type              text NOT NULL,
  scheduled_at      bigint NOT NULL,
  duration_minutes  integer NOT NULL DEFAULT 30,
  status            text NOT NULL DEFAULT 'SCHEDULED',
  notes             text,
  created_at        bigint NOT NULL,
  updated_at        bigint NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_appt_patient ON appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appt_time    ON appointments(scheduled_at);

CREATE TABLE IF NOT EXISTS treatments (
  id              bigserial PRIMARY KEY,
  patient_id      bigint NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  dentist_id      bigint,             -- no FK: dentists seeded locally only
  procedure       text NOT NULL,
  tooth_number    text,
  description     text,
  quoted_cost     numeric,
  visits_required integer,
  status          text NOT NULL DEFAULT 'ONGOING',
  start_date      bigint NOT NULL,
  completed_date  bigint,
  notes           text,
  created_at      bigint NOT NULL,
  updated_at      bigint NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_treat_patient ON treatments(patient_id);

CREATE TABLE IF NOT EXISTS visits (
  id            bigserial PRIMARY KEY,
  patient_id    bigint NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  visit_date    bigint NOT NULL,
  performed_by  text NOT NULL,
  amount_paid   numeric NOT NULL DEFAULT 0,
  cost_charged  numeric NOT NULL DEFAULT 0,
  payment_mode  text,
  notes         text,
  created_at    bigint NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_visit_patient ON visits(patient_id);

CREATE TABLE IF NOT EXISTS treatment_visit_cross_ref (
  treatment_id  bigint NOT NULL REFERENCES treatments(id) ON DELETE CASCADE,
  visit_id      bigint NOT NULL REFERENCES visits(id)     ON DELETE CASCADE,
  work_done     text NOT NULL,
  PRIMARY KEY (treatment_id, visit_id)
);
CREATE INDEX IF NOT EXISTS idx_tvcr_visit ON treatment_visit_cross_ref(visit_id);

CREATE TABLE IF NOT EXISTS invoices (
  id            bigserial PRIMARY KEY,
  patient_id    bigint NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  total_amount  numeric NOT NULL,
  paid_amount   numeric NOT NULL DEFAULT 0,
  status        text NOT NULL DEFAULT 'UNPAID',
  due_date      bigint NOT NULL,
  notes         text,
  created_at    bigint NOT NULL,
  updated_at    bigint NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_invoice_patient ON invoices(patient_id);


-- ── 2. Enable Row Level Security ─────────────────────────────

ALTER TABLE users                     ENABLE ROW LEVEL SECURITY;
ALTER TABLE patients                  ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointments              ENABLE ROW LEVEL SECURITY;
ALTER TABLE treatments                ENABLE ROW LEVEL SECURITY;
ALTER TABLE visits                    ENABLE ROW LEVEL SECURITY;
ALTER TABLE treatment_visit_cross_ref ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoices                  ENABLE ROW LEVEL SECURITY;

-- Transitional policies: full CRUD via anon key (internal staff app).
-- Replace with per-user policies when Google OAuth is added.
CREATE POLICY "anon_all" ON users                     FOR ALL TO anon USING (true) WITH CHECK (true);
CREATE POLICY "anon_all" ON patients                  FOR ALL TO anon USING (true) WITH CHECK (true);
CREATE POLICY "anon_all" ON appointments              FOR ALL TO anon USING (true) WITH CHECK (true);
CREATE POLICY "anon_all" ON treatments                FOR ALL TO anon USING (true) WITH CHECK (true);
CREATE POLICY "anon_all" ON visits                    FOR ALL TO anon USING (true) WITH CHECK (true);
CREATE POLICY "anon_all" ON treatment_visit_cross_ref FOR ALL TO anon USING (true) WITH CHECK (true);
CREATE POLICY "anon_all" ON invoices                  FOR ALL TO anon USING (true) WITH CHECK (true);


-- ── 3. Notes ─────────────────────────────────────────────────
--
-- dentist_id columns in appointments and treatments intentionally
-- have NO foreign key constraint. Dentists are seeded locally in
-- Room only and are not synced to Supabase. A FK would cause every
-- appointment/treatment insert to fail with a constraint violation.
--
-- local.properties (gitignored) must contain:
--   SUPABASE_URL=https://<project-ref>.supabase.co
--   SUPABASE_ANON_KEY=eyJ...
--
-- GitHub Actions requires two separate repository secrets:
--   SUPABASE_URL
--   SUPABASE_ANON_KEY
--
-- Future: when Google OAuth is added, drop the anon_all policies
-- above and replace with auth.uid()-based row-level policies.
