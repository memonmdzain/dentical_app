package com.dentical.staff.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
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

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "dentical_session")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.sessionDataStore

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
            DenticalDatabase.MIGRATION_4_5,
            DenticalDatabase.MIGRATION_5_6,
            DenticalDatabase.MIGRATION_6_7
        )
        .fallbackToDestructiveMigration()
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val now = System.currentTimeMillis()

                // Seed system roles
                db.execSQL("INSERT INTO roles (id, name, description, isSystem, createdAt) VALUES (1, 'ADMIN', 'Full system access', 1, $now)")
                db.execSQL("INSERT INTO roles (id, name, description, isSystem, createdAt) VALUES (2, 'DENTIST', 'Access to clinical data', 1, $now)")
                db.execSQL("INSERT INTO roles (id, name, description, isSystem, createdAt) VALUES (3, 'STAFF', 'Basic patient and appointment management', 1, $now)")

                // Seed permissions
                val allRes = listOf("patient", "appointment", "treatment", "visit", "invoice", "user", "role")
                allRes.forEach { res ->
                    db.execSQL("INSERT INTO permissions (roleId, resource, canCreate, canRead, canUpdate, canDelete) VALUES (1, '$res', 1, 1, 1, 1)")
                }
                listOf("patient", "appointment", "treatment", "visit").forEach { res ->
                    db.execSQL("INSERT INTO permissions (roleId, resource, canCreate, canRead, canUpdate, canDelete) VALUES (2, '$res', 1, 1, 1, 0)")
                }
                listOf("invoice", "user").forEach { res ->
                    db.execSQL("INSERT INTO permissions (roleId, resource, canCreate, canRead, canUpdate, canDelete) VALUES (2, '$res', 0, 1, 0, 0)")
                }
                listOf("patient", "appointment").forEach { res ->
                    db.execSQL("INSERT INTO permissions (roleId, resource, canCreate, canRead, canUpdate, canDelete) VALUES (3, '$res', 1, 1, 0, 0)")
                }
                listOf("treatment", "visit", "invoice").forEach { res ->
                    db.execSQL("INSERT INTO permissions (roleId, resource, canCreate, canRead, canUpdate, canDelete) VALUES (3, '$res', 0, 1, 0, 0)")
                }

                // Seed demo users and assign roles
                val adminHash = PasswordUtil.hash("admin123")
                db.execSQL("INSERT INTO users (id, username, passwordHash, fullName, isActive, createdAt) VALUES (1, 'admin', '$adminHash', 'Administrator', 1, $now)")
                db.execSQL("INSERT INTO user_role_cross_ref (userId, roleId) VALUES (1, 1)")

                val dentistHash = PasswordUtil.hash("dentist123")
                db.execSQL("INSERT INTO users (id, username, passwordHash, fullName, isActive, createdAt) VALUES (2, 'dr.smith', '$dentistHash', 'Dr. John Smith', 1, $now)")
                db.execSQL("INSERT INTO user_role_cross_ref (userId, roleId) VALUES (2, 2)")

                db.execSQL("INSERT INTO users (id, username, passwordHash, fullName, isActive, createdAt) VALUES (3, 'dr.jones', '$dentistHash', 'Dr. Sarah Jones', 1, $now)")
                db.execSQL("INSERT INTO user_role_cross_ref (userId, roleId) VALUES (3, 2)")
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
    @Provides fun provideRoleDao(db: DenticalDatabase) = db.roleDao()
}
