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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        .addMigrations(DenticalDatabase.MIGRATION_1_2)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    db.execSQL(
                        """
                        INSERT INTO users (username, passwordHash, fullName, role, isActive, createdAt)
                        VALUES ('admin', '${PasswordUtil.hash("admin123")}', 'Administrator', 'ADMIN', 1, ${System.currentTimeMillis()})
                        """.trimIndent()
                    )
                }
            }
        })
        .build()
    }

    @Provides fun provideUserDao(db: DenticalDatabase) = db.userDao()
    @Provides fun providePatientDao(db: DenticalDatabase) = db.patientDao()
    @Provides fun provideAppointmentDao(db: DenticalDatabase) = db.appointmentDao()
    @Provides fun provideTreatmentDao(db: DenticalDatabase) = db.treatmentDao()
    @Provides fun provideInvoiceDao(db: DenticalDatabase) = db.invoiceDao()
}
