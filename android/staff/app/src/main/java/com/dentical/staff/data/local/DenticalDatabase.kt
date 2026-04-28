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
        InvoiceEntity::class
    ],
    version = 5,
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
                // Recreate treatments with new schema
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

                // Create visits table
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

                // Create treatment_visit_cross_ref table
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

        // Fixes schema mismatch from 3→4 (removed SQL DEFAULTs) and adds paymentMode to visits
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
    }
}
