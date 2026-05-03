package com.dentical.staff.data.repository

import androidx.room.withTransaction
import com.dentical.staff.data.local.DenticalDatabase
import com.dentical.staff.data.local.dao.TreatmentDao
import com.dentical.staff.data.local.dao.TreatmentVisitCrossRefDao
import com.dentical.staff.data.local.dao.VisitDao
import com.dentical.staff.data.local.entities.TreatmentEntity
import com.dentical.staff.data.local.entities.TreatmentStatus
import com.dentical.staff.data.local.entities.TreatmentVisitCrossRef
import com.dentical.staff.data.local.entities.VisitEntity
import android.util.Log
import com.dentical.staff.data.remote.SupabaseSyncHelper
import com.dentical.staff.data.remote.TreatmentDto
import com.dentical.staff.data.remote.TreatmentVisitCrossRefDto
import com.dentical.staff.data.remote.VisitDto
import com.dentical.staff.data.remote.toDto
import com.dentical.staff.data.remote.toEntity
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class PatientFinancialSummary(
    val totalQuoted: Double,
    val standaloneCharged: Double,
    val totalPaid: Double,
    val totalOutstanding: Double
)

@Singleton
class TreatmentRepository @Inject constructor(
    private val db: DenticalDatabase,
    private val treatmentDao: TreatmentDao,
    private val visitDao: VisitDao,
    private val crossRefDao: TreatmentVisitCrossRefDao,
    private val sync: SupabaseSyncHelper
) {
    fun getTreatmentsByPatient(patientId: Long): Flow<List<TreatmentEntity>> =
        treatmentDao.getTreatmentsByPatient(patientId)

    fun getOngoingTreatmentsByPatient(patientId: Long): Flow<List<TreatmentEntity>> =
        treatmentDao.getOngoingTreatmentsByPatient(patientId)

    suspend fun getTreatmentById(id: Long): TreatmentEntity? =
        treatmentDao.getTreatmentById(id)

    fun getVisitsByPatient(patientId: Long): Flow<List<VisitEntity>> =
        visitDao.getVisitsByPatient(patientId)

    fun getVisitsByTreatment(treatmentId: Long): Flow<List<VisitEntity>> =
        visitDao.getVisitsByTreatment(treatmentId)

    fun getCrossRefsForTreatment(treatmentId: Long): Flow<List<TreatmentVisitCrossRef>> =
        crossRefDao.getByTreatmentId(treatmentId)

    suspend fun getVisitsByTreatmentOnce(treatmentId: Long): List<VisitEntity> =
        visitDao.getVisitsByTreatmentOnce(treatmentId)

    suspend fun getCrossRefsForTreatmentOnce(treatmentId: Long): List<TreatmentVisitCrossRef> =
        crossRefDao.getByTreatmentIdOnce(treatmentId)

    fun getVisitCountForTreatment(treatmentId: Long): Flow<Int> =
        crossRefDao.getVisitCountForTreatment(treatmentId)

    suspend fun getCrossRefsForVisit(visitId: Long): List<TreatmentVisitCrossRef> =
        crossRefDao.getByVisitId(visitId)

    fun getPatientFinancialSummary(patientId: Long): Flow<PatientFinancialSummary> = combine(
        treatmentDao.getTotalQuotedCost(patientId),
        visitDao.getTotalAmountPaid(patientId),
        visitDao.getStandaloneVisitsTotalCharged(patientId)
    ) { totalQuoted, totalPaid, standaloneCharged ->
        PatientFinancialSummary(
            totalQuoted = totalQuoted,
            standaloneCharged = standaloneCharged,
            totalPaid = totalPaid,
            totalOutstanding = maxOf(0.0, (totalQuoted + standaloneCharged) - totalPaid)
        )
    }

    fun getOngoingTreatmentCount(): Flow<Int> =
        treatmentDao.getOngoingTreatmentCount()

    fun getPatientIdsWithOngoingTreatments(): Flow<List<Long>> =
        treatmentDao.getPatientIdsWithOngoingTreatments()

    fun getTodaysCollections(): Flow<Double> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        return visitDao.getTodaysCollections(startOfDay, startOfDay + 86_400_000L)
    }

    fun getTotalOutstandingBalance(): Flow<Double> = combine(
        treatmentDao.getTotalQuotedAll(),
        visitDao.getTotalPaidAll(),
        visitDao.getTotalStandaloneChargedAll()
    ) { quoted, paid, standalone ->
        maxOf(0.0, quoted + standalone - paid)
    }

    suspend fun computeOutstandingOnce(patientId: Long): Double {
        val quoted = treatmentDao.getTotalQuotedCostOnce(patientId)
        val paid = visitDao.getTotalAmountPaidOnce(patientId)
        val standalone = visitDao.getStandaloneVisitsTotalChargedOnce(patientId)
        return maxOf(0.0, quoted + standalone - paid)
    }

    suspend fun addTreatment(treatment: TreatmentEntity): Long {
        val id = treatmentDao.insertTreatment(treatment)
        sync.fireAndForget {
            sync.supabase.from("treatments").upsert(treatment.copy(id = id).toDto())
        }
        return id
    }

    suspend fun updateTreatment(treatment: TreatmentEntity) {
        treatmentDao.updateTreatment(treatment)
        sync.fireAndForget { sync.supabase.from("treatments").upsert(treatment.toDto()) }
    }

    suspend fun updateTreatmentStatus(id: Long, status: TreatmentStatus) {
        val treatment = treatmentDao.getTreatmentById(id) ?: return
        val completedDate = if (status == TreatmentStatus.COMPLETED) System.currentTimeMillis()
                            else treatment.completedDate
        val updated = treatment.copy(
            status = status,
            completedDate = completedDate,
            updatedAt = System.currentTimeMillis()
        )
        treatmentDao.updateTreatment(updated)
        sync.fireAndForget { sync.supabase.from("treatments").upsert(updated.toDto()) }
    }

    suspend fun getVisitById(visitId: Long): VisitEntity? = visitDao.getVisitById(visitId)

    suspend fun updateVisit(visit: VisitEntity) {
        visitDao.updateVisit(visit)
        sync.fireAndForget { sync.supabase.from("visits").upsert(visit.toDto()) }
    }

    /**
     * FIFO allocation: amountPaid on a visit is allocated to its linked treatments in
     * startDate order (earliest first). Returns unpaid balance for the given treatment,
     * or 0.0 if no quotedCost is set.
     */
    suspend fun calculateTreatmentOutstanding(treatmentId: Long): Double {
        val treatment = treatmentDao.getTreatmentById(treatmentId) ?: return 0.0
        val quotedCost = treatment.quotedCost ?: return 0.0
        val visits = visitDao.getVisitsByTreatmentOnce(treatmentId)
        var totalAllocated = 0.0
        for (visit in visits) {
            val visitCrossRefs = crossRefDao.getByVisitId(visit.id)
            val visitTreatments = visitCrossRefs
                .mapNotNull { treatmentDao.getTreatmentById(it.treatmentId) }
                .sortedWith(compareBy({ it.startDate }, { it.id }))
            var remaining = visit.amountPaid
            for (t in visitTreatments) {
                val cost = t.quotedCost ?: continue
                val allocated = minOf(remaining, cost)
                if (t.id == treatmentId) { totalAllocated += allocated; break }
                remaining -= allocated
                if (remaining <= 0.0) break
            }
        }
        return maxOf(0.0, quotedCost - totalAllocated)
    }

    /**
     * Signed patient balance after cancelling [treatmentId] with [partialCharge] as the charge
     * for work done so far. Negative = patient is owed a refund.
     */
    suspend fun computeCancellationBalance(treatmentId: Long, partialCharge: Double): Double {
        val treatment = treatmentDao.getTreatmentById(treatmentId) ?: return 0.0
        val patientId = treatment.patientId
        val totalQuoted = treatmentDao.getTotalQuotedCostOnce(patientId)
        val standaloneCharged = visitDao.getStandaloneVisitsTotalChargedOnce(patientId)
        val totalPaid = visitDao.getTotalAmountPaidOnce(patientId)
        val originalCost = treatment.quotedCost ?: 0.0
        return (totalQuoted - originalCost + partialCharge + standaloneCharged) - totalPaid
    }

    suspend fun pullAll() {
        if (!sync.isConnected) return
        try {
            val treatmentDtos = sync.supabase.from("treatments").select().decodeList<TreatmentDto>()
            treatmentDao.upsertAll(treatmentDtos.map { it.toEntity() })
            val visitDtos = sync.supabase.from("visits").select().decodeList<VisitDto>()
            visitDao.upsertAll(visitDtos.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Pull all treatments/visits failed", e)
        }
    }

    suspend fun pullForPatient(patientId: Long) {
        if (!sync.isConnected) return
        try {
            val treatmentDtos = sync.supabase.from("treatments").select {
                filter { eq("patient_id", patientId) }
            }.decodeList<TreatmentDto>()
            treatmentDao.upsertAll(treatmentDtos.map { it.toEntity() })

            val visitDtos = sync.supabase.from("visits").select {
                filter { eq("patient_id", patientId) }
            }.decodeList<VisitDto>()
            visitDao.upsertAll(visitDtos.map { it.toEntity() })

            treatmentDtos.forEach { treatment ->
                val crossRefDtos = sync.supabase.from("treatment_visit_cross_ref").select {
                    filter { eq("treatment_id", treatment.id) }
                }.decodeList<TreatmentVisitCrossRefDto>()
                crossRefDao.upsertAll(crossRefDtos.map { it.toEntity() })
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Pull for patient $patientId failed", e)
        }
    }

    suspend fun addVisit(visit: VisitEntity, treatmentLinks: List<Pair<Long, String>>): Long {
        val visitId = db.withTransaction {
            val id = visitDao.insertVisit(visit)
            treatmentLinks.forEach { (treatmentId, workDone) ->
                crossRefDao.insert(TreatmentVisitCrossRef(treatmentId, id, workDone))
            }
            id
        }
        sync.fireAndForget {
            sync.supabase.from("visits").upsert(visit.copy(id = visitId).toDto())
            treatmentLinks.forEach { (treatmentId, workDone) ->
                sync.supabase.from("treatment_visit_cross_ref").upsert(
                    TreatmentVisitCrossRefDto(treatmentId, visitId, workDone)
                )
            }
        }
        return visitId
    }
}
