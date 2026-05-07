package com.dentical.staff.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dentical.staff.data.local.dao.*
import com.dentical.staff.data.local.entities.*

@Database(
    entities = [
        UserEntity::class,
        PatientEntity::class,
        AppointmentEntity::class,
        TreatmentEntity::class,
        VisitEntity::class,
        TreatmentVisitCrossRef::class,
        InvoiceEntity::class,
        RoleEntity::class,
        PermissionEntity::class,
        UserRoleCrossRef::class
    ],
    version = 7,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class DenticalDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun patientDao(): PatientDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun treatmentDao(): TreatmentDao
    abstract fun visitDao(): VisitDao
    abstract fun treatmentVisitCrossRefDao(): TreatmentVisitCrossRefDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun roleDao(): RoleDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS patients")
                db.execSQL("""
                    CREATE TABLE patients (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientCode TEXT NOT NULL DEFAULT '',
                        fullName TEXT NOT NULL,
                        dateOfBirth INTEGER NOT NULL,
                        gender TEXT NOT NULL,
                        phone TEXT,
                        isPhoneAvailable INTEGER NOT NULL DEFAULT 1,
                        guardianName TEXT,
                        guardianPhone TEXT,
                        referralSource TEXT NOT NULL DEFAULT '',
                        referralDetail TEXT,
                        email TEXT,
                        address TEXT,
                        medicalConditions TEXT,
                        allergies TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX index_patients_patientCode ON patients(patientCode)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS appointments")
                db.execSQL("""
                    CREATE TABLE appointments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        dentistId INTEGER,
                        type TEXT NOT NULL DEFAULT 'CONSULTATION',
                        scheduledAt INTEGER NOT NULL,
                        durationMinutes INTEGER NOT NULL DEFAULT 30,
                        status TEXT NOT NULL DEFAULT 'SCHEDULED',
                        notes TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY (patientId) REFERENCES patients(id) ON DELETE CASCADE,
                        FOREIGN KEY (dentistId) REFERENCES users(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX index_appointments_patientId ON appointments(patientId)")
                db.execSQL("CREATE INDEX index_appointments_dentistId ON appointments(dentistId)")
                db.execSQL("CREATE INDEX index_appointments_scheduledAt ON appointments(scheduledAt)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS treatments")
                db.execSQL("""
                    CREATE TABLE treatments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        dentistId INTEGER,
                        procedure TEXT NOT NULL DEFAULT 'CONSULTATION',
                        toothNumber TEXT,
                        description TEXT,
                        quotedCost REAL,
                        visitsRequired INTEGER,
                        status TEXT NOT NULL DEFAULT 'ONGOING',
                        startDate INTEGER NOT NULL,
                        completedDate INTEGER,
                        notes TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY (patientId) REFERENCES patients(id) ON DELETE CASCADE,
                        FOREIGN KEY (dentistId) REFERENCES users(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX index_treatments_patientId ON treatments(patientId)")
                db.execSQL("CREATE INDEX index_treatments_dentistId ON treatments(dentistId)")

                db.execSQL("""
                    CREATE TABLE visits (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        visitDate INTEGER NOT NULL,
                        performedBy TEXT NOT NULL,
                        amountPaid REAL NOT NULL DEFAULT 0.0,
                        costCharged REAL NOT NULL DEFAULT 0.0,
                        notes TEXT,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY (patientId) REFERENCES patients(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX index_visits_patientId ON visits(patientId)")

                db.execSQL("""
                    CREATE TABLE treatment_visit_cross_ref (
                        treatmentId INTEGER NOT NULL,
                        visitId INTEGER NOT NULL,
                        workDone TEXT NOT NULL,
                        PRIMARY KEY (treatmentId, visitId),
                        FOREIGN KEY (treatmentId) REFERENCES treatments(id) ON DELETE CASCADE,
                        FOREIGN KEY (visitId) REFERENCES visits(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX index_treatment_visit_cross_ref_visitId ON treatment_visit_cross_ref(visitId)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS treatment_visit_cross_ref")
                db.execSQL("DROP TABLE IF EXISTS visits")
                db.execSQL("DROP TABLE IF EXISTS treatments")

                db.execSQL("""
                    CREATE TABLE treatments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        dentistId INTEGER,
                        procedure TEXT NOT NULL,
                        toothNumber TEXT,
                        description TEXT,
                        quotedCost REAL,
                        visitsRequired INTEGER,
                        status TEXT NOT NULL,
                        startDate INTEGER NOT NULL,
                        completedDate INTEGER,
                        notes TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY (patientId) REFERENCES patients(id) ON DELETE CASCADE,
                        FOREIGN KEY (dentistId) REFERENCES users(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX index_treatments_patientId ON treatments(patientId)")
                db.execSQL("CREATE INDEX index_treatments_dentistId ON treatments(dentistId)")

                db.execSQL("""
                    CREATE TABLE visits (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        visitDate INTEGER NOT NULL,
                        performedBy TEXT NOT NULL,
                        amountPaid REAL NOT NULL,
                        costCharged REAL NOT NULL,
                        paymentMode TEXT,
                        notes TEXT,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY (patientId) REFERENCES patients(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX index_visits_patientId ON visits(patientId)")

                db.execSQL("""
                    CREATE TABLE treatment_visit_cross_ref (
                        treatmentId INTEGER NOT NULL,
                        visitId INTEGER NOT NULL,
                        workDone TEXT NOT NULL,
                        PRIMARY KEY (treatmentId, visitId),
                        FOREIGN KEY (treatmentId) REFERENCES treatments(id) ON DELETE CASCADE,
                        FOREIGN KEY (visitId) REFERENCES visits(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX index_treatment_visit_cross_ref_visitId ON treatment_visit_cross_ref(visitId)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS treatment_visit_cross_ref")
                db.execSQL("DROP TABLE IF EXISTS visits")
                db.execSQL("DROP TABLE IF EXISTS treatments")

                db.execSQL("""
                    CREATE TABLE treatments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        dentistId INTEGER,
                        procedure TEXT NOT NULL,
                        toothNumber TEXT,
                        description TEXT,
                        quotedCost REAL,
                        visitsRequired INTEGER,
                        status TEXT NOT NULL,
                        startDate INTEGER NOT NULL,
                        completedDate INTEGER,
                        notes TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY (patientId) REFERENCES patients(id) ON DELETE CASCADE,
                        FOREIGN KEY (dentistId) REFERENCES users(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX index_treatments_patientId ON treatments(patientId)")
                db.execSQL("CREATE INDEX index_treatments_dentistId ON treatments(dentistId)")

                db.execSQL("""
                    CREATE TABLE visits (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        visitDate INTEGER NOT NULL,
                        performedBy TEXT NOT NULL,
                        amountPaid REAL NOT NULL,
                        costCharged REAL NOT NULL,
                        paymentMode TEXT,
                        notes TEXT,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY (patientId) REFERENCES patients(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX index_visits_patientId ON visits(patientId)")

                db.execSQL("""
                    CREATE TABLE treatment_visit_cross_ref (
                        treatmentId INTEGER NOT NULL,
                        visitId INTEGER NOT NULL,
                        workDone TEXT NOT NULL,
                        PRIMARY KEY (treatmentId, visitId),
                        FOREIGN KEY (treatmentId) REFERENCES treatments(id) ON DELETE CASCADE,
                        FOREIGN KEY (visitId) REFERENCES visits(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX index_treatment_visit_cross_ref_visitId ON treatment_visit_cross_ref(visitId)")
            }
        }

        // Introduces dynamic roles/permissions system and session support.
        // New tables: roles, permissions, user_role_cross_ref.
        // Backfills existing users' roles from the legacy `role` column.
        // Adds googleId column to users for future OAuth.
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()

                // Create roles table
                db.execSQL("""
                    CREATE TABLE roles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL UNIQUE,
                        description TEXT,
                        isSystem INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Seed system roles with explicit IDs so backfill below is deterministic
                db.execSQL("INSERT INTO roles (id, name, description, isSystem, createdAt) VALUES (1, 'ADMIN', 'Full system access', 1, $now)")
                db.execSQL("INSERT INTO roles (id, name, description, isSystem, createdAt) VALUES (2, 'DENTIST', 'Access to clinical data', 1, $now)")
                db.execSQL("INSERT INTO roles (id, name, description, isSystem, createdAt) VALUES (3, 'STAFF', 'Basic patient and appointment management', 1, $now)")

                // Create permissions table
                db.execSQL("""
                    CREATE TABLE permissions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        roleId INTEGER NOT NULL,
                        resource TEXT NOT NULL,
                        canCreate INTEGER NOT NULL DEFAULT 0,
                        canRead INTEGER NOT NULL DEFAULT 0,
                        canUpdate INTEGER NOT NULL DEFAULT 0,
                        canDelete INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE,
                        UNIQUE(roleId, resource)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX index_permissions_roleId ON permissions(roleId)")

                // Admin: full CRUD on all resources
                val allResources = listOf("patient", "appointment", "treatment", "visit", "invoice", "user", "role")
                allResources.forEach { res ->
                    db.execSQL("INSERT INTO permissions (roleId, resource, canCreate, canRead, canUpdate, canDelete) VALUES (1, '$res', 1, 1, 1, 1)")
                }

                // Dentist: CRU on clinical resources; R on invoice and user
                listOf("patient", "appointment", "treatment", "visit").forEach { res ->
                    db.execSQL("INSERT INTO permissions (roleId, resource, canCreate, canRead, canUpdate, canDelete) VALUES (2, '$res', 1, 1, 1, 0)")
                }
                listOf("invoice", "user").forEach { res ->
                    db.execSQL("INSERT INTO permissions (roleId, resource, canCreate, canRead, canUpdate, canDelete) VALUES (2, '$res', 0, 1, 0, 0)")
                }

                // Staff: CR on patient and appointment; R on treatment, visit, invoice
                listOf("patient", "appointment").forEach { res ->
                    db.execSQL("INSERT INTO permissions (roleId, resource, canCreate, canRead, canUpdate, canDelete) VALUES (3, '$res', 1, 1, 0, 0)")
                }
                listOf("treatment", "visit", "invoice").forEach { res ->
                    db.execSQL("INSERT INTO permissions (roleId, resource, canCreate, canRead, canUpdate, canDelete) VALUES (3, '$res', 0, 1, 0, 0)")
                }

                // Create user_role_cross_ref junction table
                db.execSQL("""
                    CREATE TABLE user_role_cross_ref (
                        userId INTEGER NOT NULL,
                        roleId INTEGER NOT NULL,
                        PRIMARY KEY (userId, roleId),
                        FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX index_user_role_cross_ref_userId ON user_role_cross_ref(userId)")
                db.execSQL("CREATE INDEX index_user_role_cross_ref_roleId ON user_role_cross_ref(roleId)")

                // Backfill cross-refs from legacy `role` column
                db.execSQL("INSERT INTO user_role_cross_ref (userId, roleId) SELECT id, 1 FROM users WHERE role = 'ADMIN'")
                db.execSQL("INSERT INTO user_role_cross_ref (userId, roleId) SELECT id, 2 FROM users WHERE role = 'DENTIST'")
                db.execSQL("INSERT INTO user_role_cross_ref (userId, roleId) SELECT id, 3 FROM users WHERE role = 'STAFF'")

                // Add googleId to users (placeholder for future OAuth)
                db.execSQL("ALTER TABLE users ADD COLUMN googleId TEXT")
            }
        }
    }
}
