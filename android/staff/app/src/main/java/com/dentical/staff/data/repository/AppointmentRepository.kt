package com.dentical.staff.data.repository

import com.dentical.staff.data.local.dao.AppointmentDao
import com.dentical.staff.data.local.dao.UserDao
import com.dentical.staff.data.local.entities.AppointmentEntity
import com.dentical.staff.data.local.entities.AppointmentStatus
import com.dentical.staff.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppointmentRepository @Inject constructor(
    private val appointmentDao: AppointmentDao,
    private val userDao: UserDao
) {
    fun getAppointmentsByRange(start: Long, end: Long): Flow<List<AppointmentEntity>> =
        appointmentDao.getAppointmentsByRange(start, end)

    fun getAppointmentsByPatient(patientId: Long): Flow<List<AppointmentEntity>> =
        appointmentDao.getAppointmentsByPatient(patientId)

    fun getAllActiveDentists(): Flow<List<UserEntity>> =
        userDao.getAllActiveDentists()

    suspend fun getAppointmentById(id: Long): AppointmentEntity? =
        appointmentDao.getAppointmentById(id)

    suspend fun getDentistById(id: Long): UserEntity? =
        userDao.getUserById(id)

    suspend fun addAppointment(appointment: AppointmentEntity): Long =
        appointmentDao.insertAppointment(appointment)

    suspend fun updateStatus(id: Long, status: AppointmentStatus) {
        val appt = appointmentDao.getAppointmentById(id) ?: return
        appointmentDao.updateAppointment(
            appt.copy(status = status, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun updateAppointment(appointment: AppointmentEntity) {
        appointmentDao.updateAppointment(
            appointment.copy(updatedAt = System.currentTimeMillis())
        )
    }
}
