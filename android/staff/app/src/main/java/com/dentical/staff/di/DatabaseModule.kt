package com.dentical.staff.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dentical.staff.data.local.DenticalDatabase
import com.dentical.staff.util.PasswordUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DenticalDatabase {
        return Room.databaseBuilder(
            context,
            DenticalDatabase::class.java,
            "dentical_staff.db"
        )
        .addMigrations(
            DenticalDatabase.MIGRATION_1_2,
            DenticalDatabase.MIGRATION_2_3,
            DenticalDatabase.MIGRATION_3_4,
            DenticalDatabase.MIGRATION_4_5
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val now = System.currentTimeMillis()

                val adminHash = PasswordUtil.hash("admin123")
                db.execSQL(
                    "INSERT INTO users (username, passwordHash, fullName, role, isActive, createdAt) " +
                    "VALUES ('admin', '$adminHash', 'Administrator', 'ADMIN', 1, $now)"
                )

                val dentistHash = PasswordUtil.hash("dentist123")
                db.execSQL(
                    "INSERT INTO users (username, passwordHash, fullName, role, isActive, createdAt) " +
                    "VALUES ('dr.smith', '$dentistHash', 'Dr. John Smith', 'DENTIST', 1, $now)"
                )

                val dentist2Hash = PasswordUtil.hash("dentist123")
                db.execSQL(
                    "INSERT INTO users (username, passwordHash, fullName, role, isActive, createdAt) " +
                    "VALUES ('dr.jones', '$dentist2Hash', 'Dr. Sarah Jones', 'DENTIST', 1, $now)"
                )
            }
        })
        .build()
    }

    @Provides fun provideUserDao(db: DenticalDatabase) = db.userDao()
    @Provides fun providePatientDao(db: DenticalDatabase) = db.patientDao()
    @Provides fun provideAppointmentDao(db: DenticalDatabase) = db.appointmentDao()
    @Provides fun provideTreatmentDao(db: DenticalDatabase) = db.treatmentDao()
    @Provides fun provideVisitDao(db: DenticalDatabase) = db.visitDao()
    @Provides fun provideTreatmentVisitCrossRefDao(db: DenticalDatabase) = db.treatmentVisitCrossRefDao()
    @Provides fun provideInvoiceDao(db: DenticalDatabase) = db.invoiceDao()
}
