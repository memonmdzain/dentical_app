package com.dentical.staff.data.repository

import com.dentical.staff.data.local.dao.AppointmentDao
import com.dentical.staff.data.local.dao.UserDao
import com.dentical.staff.data.local.entities.AppointmentEntity
import com.dentical.staff.data.local.entities.AppointmentStatus
import com.dentical.staff.data.local.entities.UserEntity
import com.dentical.staff.data.remote.SupabaseSyncHelper
import com.dentical.staff.data.remote.toDto
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppointmentRepository @Inject constructor(
    private val appointmentDao: AppointmentDao,
    private val userDao: UserDao,
    private val sync: SupabaseSyncHelper
) {
    // List view — from today onwards
    fun getAppointmentsFrom(from: Long): Flow<List<AppointmentEntity>> =
        appointmentDao.getAppointmentsFrom(from)

    // Day view
    fun getAppointmentsByDay(dateMillis: Long): Flow<List<AppointmentEntity>> {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val end = start + 86_400_000L
        return appointmentDao.getAppointmentsByDay(start, end)
    }

    // Week/Month view
    fun getAppointmentsByRange(startMillis: Long, endMillis: Long): Flow<List<AppointmentEntity>> =
        appointmentDao.getAppointmentsByRange(startMillis, endMillis)

    fun getAppointmentsByPatient(patientId: Long): Flow<List<AppointmentEntity>> =
        appointmentDao.getAppointmentsByPatient(patientId)

    fun getAllActiveDentists(): Flow<List<UserEntity>> =
        userDao.getAllActiveDentists()

    suspend fun getAppointmentById(id: Long): AppointmentEntity? =
        appointmentDao.getAppointmentById(id)

    suspend fun getDentistById(id: Long): UserEntity? =
        userDao.getUserById(id)

    suspend fun addAppointment(appointment: AppointmentEntity): Long {
        val id = appointmentDao.insertAppointment(appointment)
        sync.fireAndForget {
            sync.supabase.from("appointments").upsert(appointment.copy(id = id).toDto())
        }
        return id
    }

    suspend fun updateStatus(id: Long, status: AppointmentStatus) {
        val appt = appointmentDao.getAppointmentById(id) ?: return
        val updated = appt.copy(status = status, updatedAt = System.currentTimeMillis())
        appointmentDao.updateAppointment(updated)
        sync.fireAndForget { sync.supabase.from("appointments").upsert(updated.toDto()) }
    }

    suspend fun updateAppointment(appointment: AppointmentEntity) {
        val updated = appointment.copy(updatedAt = System.currentTimeMillis())
        appointmentDao.updateAppointment(updated)
        sync.fireAndForget { sync.supabase.from("appointments").upsert(updated.toDto()) }
    }
}
