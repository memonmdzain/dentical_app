package com.dentical.staff.data.local.dao

import androidx.room.*
import com.dentical.staff.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username AND isActive = 1 LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE isActive = 1")
    fun getAllActiveUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isActive = 0 WHERE id = :userId")
    suspend fun deactivateUser(userId: Long)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY patientCode ASC")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getPatientById(id: Long): PatientEntity?

    @Query("SELECT * FROM patients WHERE fullName LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR patientCode LIKE '%' || :query || '%'")
    fun searchPatients(query: String): Flow<List<PatientEntity>>

    @Query("SELECT MAX(CAST(patientCode AS INTEGER)) FROM patients")
    suspend fun getMaxPatientCode(): Int?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPatient(patient: PatientEntity): Long

    @Update
    suspend fun updatePatient(patient: PatientEntity)

    @Delete
    suspend fun deletePatient(patient: PatientEntity)

    @Query("SELECT COUNT(*) FROM patients")
    suspend fun getPatientCount(): Int
}

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments WHERE scheduledAt >= :startOfDay AND scheduledAt < :endOfDay ORDER BY scheduledAt ASC")
    fun getAppointmentsByDate(startOfDay: Long, endOfDay: Long): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE patientId = :patientId ORDER BY scheduledAt DESC")
    fun getAppointmentsByPatient(patientId: Long): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getAppointmentById(id: Long): AppointmentEntity?

    @Query("SELECT * FROM appointments WHERE scheduledAt >= :from ORDER BY scheduledAt ASC")
    fun getUpcomingAppointments(from: Long): Flow<List<AppointmentEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAppointment(appointment: AppointmentEntity): Long

    @Update
    suspend fun updateAppointment(appointment: AppointmentEntity)

    @Delete
    suspend fun deleteAppointment(appointment: AppointmentEntity)
}

@Dao
interface TreatmentDao {
    @Query("SELECT * FROM treatments WHERE patientId = :patientId ORDER BY performedAt DESC")
    fun getTreatmentsByPatient(patientId: Long): Flow<List<TreatmentEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTreatment(treatment: TreatmentEntity): Long

    @Update
    suspend fun updateTreatment(treatment: TreatmentEntity)

    @Delete
    suspend fun deleteTreatment(treatment: TreatmentEntity)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE patientId = :patientId ORDER BY createdAt DESC")
    fun getInvoicesByPatient(patientId: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE status != 'PAID' ORDER BY dueDate ASC")
    fun getUnpaidInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): InvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)
}
