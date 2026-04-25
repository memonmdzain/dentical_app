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
    version = 2,
    exportSchema = true
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
                // Recreate patients table with new fields
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
    }
}
