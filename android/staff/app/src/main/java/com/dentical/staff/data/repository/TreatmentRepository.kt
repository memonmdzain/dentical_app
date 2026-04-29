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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    private val crossRefDao: TreatmentVisitCrossRefDao
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

    suspend fun addTreatment(treatment: TreatmentEntity): Long =
        treatmentDao.insertTreatment(treatment)

    suspend fun updateTreatment(treatment: TreatmentEntity) =
        treatmentDao.updateTreatment(treatment)

    suspend fun updateTreatmentStatus(id: Long, status: TreatmentStatus) {
        val treatment = treatmentDao.getTreatmentById(id) ?: return
        val completedDate = if (status == TreatmentStatus.COMPLETED) System.currentTimeMillis()
                            else treatment.completedDate
        treatmentDao.updateTreatment(
            treatment.copy(
                status = status,
                completedDate = completedDate,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getVisitById(visitId: Long): VisitEntity? = visitDao.getVisitById(visitId)

    suspend fun updateVisit(visit: VisitEntity) = visitDao.updateVisit(visit)

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

    suspend fun addVisit(visit: VisitEntity, treatmentLinks: List<Pair<Long, String>>): Long {
        return db.withTransaction {
            val visitId = visitDao.insertVisit(visit)
            treatmentLinks.forEach { (treatmentId, workDone) ->
                crossRefDao.insert(TreatmentVisitCrossRef(treatmentId, visitId, workDone))
            }
            visitId
        }
    }
}
