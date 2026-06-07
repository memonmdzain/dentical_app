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
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
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

data class RemoteTreatmentData(
    val treatments: List<TreatmentEntity>,
    val standaloneVisits: List<VisitEntity>,
    val crossRefs: Map<Long, List<TreatmentVisitCrossRef>>
)

@Singleton
class TreatmentRepository @Inject constructor(
    private val db: DenticalDatabase,
    private val treatmentDao: TreatmentDao,
    private val visitDao: VisitDao,
    private val crossRefDao: TreatmentVisitCrossRefDao,
    private val sync: SupabaseSyncHelper
) {
    // ── Existing queries ───────────────────────────────────────────────────────

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

    // ── Filtered Room queries ──────────────────────────────────────────────────

    fun getTreatmentsByPatientFiltered(patientId: Long, fromMs: Long): Flow<List<TreatmentEntity>> =
        treatmentDao.getTreatmentsByPatientFiltered(patientId, fromMs)

    fun getStandaloneVisitsFiltered(patientId: Long, fromMs: Long): Flow<List<VisitEntity>> =
        visitDao.getStandaloneVisitsFiltered(patientId, fromMs)

    fun getPatientFinancialSummaryFiltered(patientId: Long, fromMs: Long): Flow<PatientFinancialSummary> =
        combine(
            treatmentDao.getTotalQuotedCostFiltered(patientId, fromMs),
            visitDao.getTotalAmountPaidFiltered(patientId, fromMs),
            visitDao.getStandaloneVisitsTotalChargedFiltered(patientId, fromMs)
        ) { totalQuoted, totalPaid, standaloneCharged ->
            PatientFinancialSummary(
                totalQuoted = totalQuoted,
                standaloneCharged = standaloneCharged,
                totalPaid = totalPaid,
                totalOutstanding = maxOf(0.0, (totalQuoted + standaloneCharged) - totalPaid)
            )
        }

    // ── Unfiltered financial summary (Dashboard, global calcs) ────────────────

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

    fun getOngoingTreatmentCount(): Flow<Int> = treatmentDao.getOngoingTreatmentCount()

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
    ) { quoted, paid, standalone -> maxOf(0.0, quoted + standalone - paid) }

    suspend fun computeOutstandingOnce(patientId: Long): Double {
        val quoted = treatmentDao.getTotalQuotedCostOnce(patientId)
        val paid = visitDao.getTotalAmountPaidOnce(patientId)
        val standalone = visitDao.getStandaloneVisitsTotalChargedOnce(patientId)
        return maxOf(0.0, quoted + standalone - paid)
    }

    // ── Supabase: fetch a page of treatments by patient with status+date filter ─

    /**
     * Fetches one page of treatments from Supabase matching:
     *   status = 'ONGOING'  OR  start_date >= fromMs  (AND start_date <= toMs if provided)
     *
     * Uses two separate queries and merges, because nested OR+AND is unreliable across
     * supabase-kt versions. Simple eq/gte/lte on each query is the safe approach.
     */
    private suspend fun fetchTreatmentPage(
        patientId: Long?,
        fromMs: Long,
        toMs: Long?,
        pageSize: Int
    ): List<TreatmentDto> {
        // Query 1: all ONGOING for this patient
        val ongoingBuilder = sync.supabase.from("treatments").select {
            if (patientId != null) filter { eq("patient_id", patientId) }
            filter { eq("status", "ONGOING") }
        }
        val ongoing = ongoingBuilder.decodeList<TreatmentDto>()

        // Query 2: closed treatments within date range (paginated)
        var offset = 0L
        val closed = mutableListOf<TreatmentDto>()
        while (true) {
            val page = sync.supabase.from("treatments").select {
                filter {
                    if (patientId != null) eq("patient_id", patientId)
                    filter("status", FilterOperator.NEQ, "ONGOING")
                    gte("start_date", fromMs)
                    if (toMs != null) lte("start_date", toMs)
                }
                range(offset, offset + pageSize - 1)
            }.decodeList<TreatmentDto>()
            closed.addAll(page)
            if (page.size < pageSize) break
            offset += pageSize
        }

        // Merge, dedup by id
        val seen = mutableSetOf<Long>()
        return (ongoing + closed).filter { seen.add(it.id) }
    }

    // ── Supabase: fetch visits and cross refs for a list of treatment IDs ──────

    private suspend fun fetchVisitsAndCrossRefsForTreatments(
        treatmentIds: List<Long>,
        pageSize: Int
    ): Pair<List<VisitEntity>, List<TreatmentVisitCrossRef>> {
        if (treatmentIds.isEmpty()) return Pair(emptyList(), emptyList())

        // Fetch cross refs per treatment (single eq per treatment — safe, proven to work)
        val allCrossRefs = mutableListOf<TreatmentVisitCrossRef>()
        val linkedVisitIds = mutableSetOf<Long>()
        for (treatmentId in treatmentIds) {
            var offset = 0L
            while (true) {
                val page = sync.supabase.from("treatment_visit_cross_ref").select {
                    filter { eq("treatment_id", treatmentId) }
                    range(offset, offset + pageSize - 1)
                }.decodeList<TreatmentVisitCrossRefDto>()
                allCrossRefs.addAll(page.map { it.toEntity() })
                page.forEach { linkedVisitIds.add(it.visitId) }
                if (page.size < pageSize) break
                offset += pageSize
            }
        }

        // Fetch visits per visit ID (single eq per visit — safe)
        val allVisits = mutableListOf<VisitEntity>()
        for (visitId in linkedVisitIds) {
            val page = sync.supabase.from("visits").select {
                filter { eq("id", visitId) }
            }.decodeList<VisitDto>()
            allVisits.addAll(page.map { it.toEntity() })
        }

        return Pair(allVisits, allCrossRefs)
    }

    // ── Direct Supabase fetch for non-default filters ─────────────────────────

    suspend fun fetchFromSupabase(patientId: Long, fromMs: Long, toMs: Long): RemoteTreatmentData {
        val pageSize = 100

        val treatments = fetchTreatmentPage(patientId, fromMs, toMs, pageSize)
        val treatmentIds = treatments.map { it.id }

        val (linkedVisits, crossRefs) = fetchVisitsAndCrossRefsForTreatments(treatmentIds, pageSize)

        val linkedVisitIds = linkedVisits.map { it.id }.toSet()

        // Standalone visits by date (visits not linked to any treatment)
        val allVisitsInRange = mutableListOf<VisitDto>()
        var offset = 0L
        while (true) {
            val page = sync.supabase.from("visits").select {
                filter {
                    eq("patient_id", patientId)
                    gte("visit_date", fromMs)
                    lte("visit_date", toMs)
                }
                range(offset, offset + pageSize - 1)
            }.decodeList<VisitDto>()
            allVisitsInRange.addAll(page)
            if (page.size < pageSize) break
            offset += pageSize
        }
        val standaloneVisits = allVisitsInRange
            .filter { it.id !in linkedVisitIds }
            .map { it.toEntity() }

        // Build crossRef map keyed by visitId
        val crossRefMap = crossRefs.groupBy { it.visitId }

        return RemoteTreatmentData(
            treatments = treatments.map { it.toEntity() },
            standaloneVisits = standaloneVisits,
            crossRefs = crossRefMap
        )
    }

    // ── Supabase fallback for TreatmentDetailScreen ───────────────────────────

    suspend fun fetchTreatmentFromSupabase(treatmentId: Long): TreatmentEntity? {
        if (!sync.isConnected) return null
        return try {
            sync.supabase.from("treatments").select {
                filter { eq("id", treatmentId) }
            }.decodeList<TreatmentDto>().firstOrNull()?.toEntity()
        } catch (e: Exception) {
            Log.e("TreatmentRepository", "fetchTreatmentFromSupabase failed", e)
            null
        }
    }

    suspend fun fetchVisitsForTreatment(treatmentId: Long): Pair<List<VisitEntity>, List<TreatmentVisitCrossRef>> {
        if (!sync.isConnected) return Pair(emptyList(), emptyList())
        return try {
            fetchVisitsAndCrossRefsForTreatments(listOf(treatmentId), 100)
        } catch (e: Exception) {
            Log.e("TreatmentRepository", "fetchVisitsForTreatment failed", e)
            Pair(emptyList(), emptyList())
        }
    }

    // ── Purge old data from Room ───────────────────────────────────────────────

    suspend fun purgeOldDataForPatient(patientId: Long, cutoffMs: Long) {
        try {
            val oldTreatmentIds = treatmentDao.getOldClosedTreatmentIds(patientId, cutoffMs)
            if (oldTreatmentIds.isNotEmpty()) {
                db.withTransaction {
                    crossRefDao.deleteByTreatmentIds(oldTreatmentIds)
                    visitDao.deleteVisitsForTreatments(oldTreatmentIds)
                    treatmentDao.deleteByIds(oldTreatmentIds)
                }
            }
            val oldStandaloneIds = visitDao.getOldStandaloneVisitIds(patientId, cutoffMs)
            if (oldStandaloneIds.isNotEmpty()) {
                visitDao.deleteByIds(oldStandaloneIds)
            }
        } catch (e: Exception) {
            Log.e("TreatmentRepository", "Purge failed for patient $patientId", e)
        }
    }

    // ── Sync: pull from Supabase into Room ────────────────────────────────────

    suspend fun pullAll() {
        if (!sync.isConnected) return
        try {
            val cutoffMs = oneMonthAgoMs()
            val pageSize = 100

            // Treatments: ONGOING + closed within last 1 month
            val treatments = fetchTreatmentPage(null, cutoffMs, null, pageSize)
            treatmentDao.upsertAll(treatments.map { it.toEntity() })

            // Linked visits (fetched by treatment ID — no date filter)
            val (linkedVisits, crossRefs) = fetchVisitsAndCrossRefsForTreatments(
                treatments.map { it.id }, pageSize
            )
            crossRefDao.upsertAll(crossRefs)
            visitDao.upsertAll(linkedVisits)

            // Standalone visits by date
            val linkedVisitIds = linkedVisits.map { it.id }.toSet()
            var offset = 0L
            while (true) {
                val page = sync.supabase.from("visits").select {
                    filter { gte("visit_date", cutoffMs) }
                    range(offset, offset + pageSize - 1)
                }.decodeList<VisitDto>()
                val standalone = page.filter { it.id !in linkedVisitIds }
                if (standalone.isNotEmpty()) visitDao.upsertAll(standalone.map { it.toEntity() })
                if (page.size < pageSize) break
                offset += pageSize
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Pull all failed", e)
        }
    }

    suspend fun pullForPatient(patientId: Long) {
        if (!sync.isConnected) return
        try {
            val cutoffMs = oneMonthAgoMs()
            val pageSize = 100

            // Treatments: ONGOING + closed within last 1 month for this patient
            val treatments = fetchTreatmentPage(patientId, cutoffMs, null, pageSize)
            treatmentDao.upsertAll(treatments.map { it.toEntity() })

            // Linked visits (fetched by treatment ID — no date filter)
            val (linkedVisits, crossRefs) = fetchVisitsAndCrossRefsForTreatments(
                treatments.map { it.id }, pageSize
            )
            crossRefDao.upsertAll(crossRefs)
            visitDao.upsertAll(linkedVisits)

            // Standalone visits by date for this patient
            val linkedVisitIds = linkedVisits.map { it.id }.toSet()
            var offset = 0L
            while (true) {
                val page = sync.supabase.from("visits").select {
                    filter {
                        eq("patient_id", patientId)
                        gte("visit_date", cutoffMs)
                    }
                    range(offset, offset + pageSize - 1)
                }.decodeList<VisitDto>()
                val standalone = page.filter { it.id !in linkedVisitIds }
                if (standalone.isNotEmpty()) visitDao.upsertAll(standalone.map { it.toEntity() })
                if (page.size < pageSize) break
                offset += pageSize
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Pull for patient $patientId failed", e)
        }
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

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

    suspend fun calculateTreatmentOutstanding(treatmentId: Long): Double {
        val treatment = treatmentDao.getTreatmentById(treatmentId) ?: return 0.0
        val quotedCost = treatment.quotedCost ?: return 0.0
        val totalAllocated = crossRefDao.getTotalAllocatedForTreatment(treatmentId)
        return maxOf(0.0, quotedCost - totalAllocated)
    }

    private suspend fun recomputeAllocationsForVisit(visitId: Long) {
        val visit = visitDao.getVisitById(visitId) ?: return
        val crossRefs = crossRefDao.getByVisitId(visitId)
        if (crossRefs.isEmpty()) return

        val treatments = crossRefs
            .mapNotNull { treatmentDao.getTreatmentById(it.treatmentId) }
            .sortedWith(compareBy({ it.startDate }, { it.id }))

        var remaining = visit.amountPaid

        val updatedCrossRefs = treatments.map { treatment ->
            val crossRef = crossRefs.first { it.treatmentId == treatment.id }
            val quotedCost = treatment.quotedCost
            if (quotedCost == null || remaining <= 0.0) {
                crossRef.copy(allocatedAmount = 0.0)
            } else {
                val alreadyAllocated = crossRefDao.getByTreatmentIdOnce(treatment.id)
                    .filter { it.visitId != visitId }
                    .sumOf { it.allocatedAmount }
                val stillNeeded = maxOf(0.0, quotedCost - alreadyAllocated)
                val allocation = minOf(remaining, stillNeeded)
                remaining -= allocation
                crossRef.copy(allocatedAmount = allocation)
            }
        }

        db.withTransaction { updatedCrossRefs.forEach { crossRefDao.insert(it) } }
        sync.fireAndForget {
            updatedCrossRefs.forEach { ref ->
                sync.supabase.from("treatment_visit_cross_ref").upsert(ref.toDto())
            }
        }
    }

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
        val visitId = db.withTransaction {
            val id = visitDao.insertVisit(visit)
            treatmentLinks.forEach { (treatmentId, workDone) ->
                crossRefDao.insert(TreatmentVisitCrossRef(treatmentId, id, workDone))
            }
            id
        }
        recomputeAllocationsForVisit(visitId)
        return visitId
    }

    suspend fun updateVisit(visit: VisitEntity) {
        visitDao.updateVisit(visit)
        sync.fireAndForget { sync.supabase.from("visits").upsert(visit.toDto()) }
        recomputeAllocationsForVisit(visit.id)
    }

    companion object {
        fun oneMonthAgoMs(): Long {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}
