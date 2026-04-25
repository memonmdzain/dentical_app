package com.dentical.staff.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dentical.staff.data.local.dao.AppointmentDao
import com.dentical.staff.data.local.dao.InvoiceDao
import com.dentical.staff.data.local.dao.PatientDao
import com.dentical.staff.data.local.dao.TreatmentDao
import com.dentical.staff.data.local.dao.UserDao
import com.dentical.staff.data.local.entities.AppointmentEntity
import com.dentical.staff.data.local.entities.InvoiceEntity
import com.dentical.staff.data.local.entities.PatientEntity
import com.dentical.staff.data.local.entities.TreatmentEntity
import com.dentical.staff.data.local.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        PatientEntity::class,
        AppointmentEntity::class,
        TreatmentEntity::class,
        InvoiceEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class DenticalDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun patientDao(): PatientDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun treatmentDao(): TreatmentDao
    abstract fun invoiceDao(): InvoiceDao
}
