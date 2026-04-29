package com.dentical.staff.data.local.dao

import androidx.room.*
import com.dentical.staff.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username AND isActive = 1 LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE isActive = 1")
    fun getAllActiveUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = 'DENTIST' AND isActive = 1 ORDER BY fullName ASC")
    fun getAllActiveDentists(): Flow<List<UserEntity>>

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
    @Query("SELECT * FROM appointments WHERE scheduledAt >= :from ORDER BY scheduledAt ASC")
    fun getAppointmentsFrom(from: Long): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE scheduledAt >= :startOfDay AND scheduledAt < :endOfDay ORDER BY scheduledAt ASC")
    fun getAppointmentsByDay(startOfDay: Long, endOfDay: Long): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE scheduledAt >= :startOfRange AND scheduledAt < :endOfRange ORDER BY scheduledAt ASC")
    fun getAppointmentsByRange(startOfRange: Long, endOfRange: Long): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE patientId = :patientId ORDER BY scheduledAt DESC")
    fun getAppointmentsByPatient(patientId: Long): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getAppointmentById(id: Long): AppointmentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAppointment(appointment: AppointmentEntity): Long

    @Update
    suspend fun updateAppointment(appointment: AppointmentEntity)

    @Delete
    suspend fun deleteAppointment(appointment: AppointmentEntity)
}

@Dao
interface TreatmentDao {
    @Query("SELECT * FROM treatments WHERE patientId = :patientId ORDER BY startDate DESC")
    fun getTreatmentsByPatient(patientId: Long): Flow<List<TreatmentEntity>>

    @Query("SELECT * FROM treatments WHERE patientId = :patientId AND status = 'ONGOING' ORDER BY startDate DESC")
    fun getOngoingTreatmentsByPatient(patientId: Long): Flow<List<TreatmentEntity>>

    @Query("SELECT * FROM treatments WHERE id = :id")
    suspend fun getTreatmentById(id: Long): TreatmentEntity?

    @Query("SELECT COALESCE(SUM(quotedCost), 0.0) FROM treatments WHERE patientId = :patientId")
    fun getTotalQuotedCost(patientId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(quotedCost), 0.0) FROM treatments WHERE patientId = :patientId")
    suspend fun getTotalQuotedCostOnce(patientId: Long): Double

    @Query("SELECT COUNT(*) FROM treatments WHERE status = 'ONGOING'")
    fun getOngoingTreatmentCount(): Flow<Int>

    @Query("SELECT DISTINCT patientId FROM treatments WHERE status = 'ONGOING'")
    fun getPatientIdsWithOngoingTreatments(): Flow<List<Long>>

    @Query("SELECT COALESCE(SUM(quotedCost), 0.0) FROM treatments")
    fun getTotalQuotedAll(): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTreatment(treatment: TreatmentEntity): Long

    @Update
    suspend fun updateTreatment(treatment: TreatmentEntity)

    @Delete
    suspend fun deleteTreatment(treatment: TreatmentEntity)
}

@Dao
interface VisitDao {
    @Query("SELECT * FROM visits WHERE patientId = :patientId ORDER BY visitDate DESC")
    fun getVisitsByPatient(patientId: Long): Flow<List<VisitEntity>>

    @Query("""
        SELECT v.* FROM visits v
        INNER JOIN treatment_visit_cross_ref tvr ON v.id = tvr.visitId
        WHERE tvr.treatmentId = :treatmentId
        ORDER BY v.visitDate DESC
    """)
    fun getVisitsByTreatment(treatmentId: Long): Flow<List<VisitEntity>>

    @Query("""
        SELECT v.* FROM visits v
        INNER JOIN treatment_visit_cross_ref tvr ON v.id = tvr.visitId
        WHERE tvr.treatmentId = :treatmentId
        ORDER BY v.visitDate ASC
    """)
    suspend fun getVisitsByTreatmentOnce(treatmentId: Long): List<VisitEntity>

    @Query("SELECT * FROM visits WHERE id = :id")
    suspend fun getVisitById(id: Long): VisitEntity?

    @Query("SELECT COALESCE(SUM(amountPaid), 0.0) FROM visits WHERE patientId = :patientId")
    fun getTotalAmountPaid(patientId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amountPaid), 0.0) FROM visits WHERE patientId = :patientId")
    suspend fun getTotalAmountPaidOnce(patientId: Long): Double

    @Query("""
        SELECT COALESCE(SUM(costCharged), 0.0) FROM visits
        WHERE patientId = :patientId
        AND id NOT IN (SELECT DISTINCT visitId FROM treatment_visit_cross_ref)
    """)
    fun getStandaloneVisitsTotalCharged(patientId: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(costCharged), 0.0) FROM visits
        WHERE patientId = :patientId
        AND id NOT IN (SELECT DISTINCT visitId FROM treatment_visit_cross_ref)
    """)
    suspend fun getStandaloneVisitsTotalChargedOnce(patientId: Long): Double

    @Query("SELECT COALESCE(SUM(amountPaid), 0.0) FROM visits WHERE visitDate >= :startOfDay AND visitDate < :endOfDay")
    fun getTodaysCollections(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amountPaid), 0.0) FROM visits")
    fun getTotalPaidAll(): Flow<Double>

    @Query("SELECT COALESCE(SUM(costCharged), 0.0) FROM visits WHERE id NOT IN (SELECT visitId FROM treatment_visit_cross_ref)")
    fun getTotalStandaloneChargedAll(): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVisit(visit: VisitEntity): Long

    @Update
    suspend fun updateVisit(visit: VisitEntity)

    @Delete
    suspend fun deleteVisit(visit: VisitEntity)
}

@Dao
interface TreatmentVisitCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(crossRef: TreatmentVisitCrossRef)

    @Delete
    suspend fun delete(crossRef: TreatmentVisitCrossRef)

    @Query("SELECT * FROM treatment_visit_cross_ref WHERE visitId = :visitId")
    suspend fun getByVisitId(visitId: Long): List<TreatmentVisitCrossRef>

    @Query("SELECT * FROM treatment_visit_cross_ref WHERE treatmentId = :treatmentId ORDER BY visitId ASC")
    suspend fun getByTreatmentIdOnce(treatmentId: Long): List<TreatmentVisitCrossRef>

    @Query("SELECT * FROM treatment_visit_cross_ref WHERE treatmentId = :treatmentId ORDER BY visitId DESC")
    fun getByTreatmentId(treatmentId: Long): Flow<List<TreatmentVisitCrossRef>>

    @Query("SELECT COUNT(*) FROM treatment_visit_cross_ref WHERE treatmentId = :treatmentId")
    fun getVisitCountForTreatment(treatmentId: Long): Flow<Int>

    @Query("DELETE FROM treatment_visit_cross_ref WHERE visitId = :visitId")
    suspend fun deleteByVisitId(visitId: Long)
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
