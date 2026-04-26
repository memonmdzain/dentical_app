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
        InvoiceEntity::class
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class DenticalDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun patientDao(): PatientDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun treatmentDao(): TreatmentDao
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
                // Recreate appointments with new fields
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
    }
}
