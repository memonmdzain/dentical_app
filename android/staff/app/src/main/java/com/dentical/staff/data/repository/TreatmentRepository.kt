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

/** Result of a direct Supabase fetch for non-default filters. */
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
    // ── Existing queries (used internally and by other screens) ────────────────

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

    // ── Filtered Room queries (default filter — Last 1M + Ongoing) ────────────

    /**
     * Treatments for PatientDetailScreen — always includes ONGOING,
     * applies [fromMs] date filter to closed treatments.
     */
    fun getTreatmentsByPatientFiltered(patientId: Long, fromMs: Long): Flow<List<TreatmentEntity>> =
        treatmentDao.getTreatmentsByPatientFiltered(patientId, fromMs)

    /**
     * Standalone visits for PatientDetailScreen filtered by [fromMs].
     */
    fun getStandaloneVisitsFiltered(patientId: Long, fromMs: Long): Flow<List<VisitEntity>> =
        visitDao.getStandaloneVisitsFiltered(patientId, fromMs)

    /**
     * Financial summary that mirrors exactly what is shown on screen (filtered).
     * ONGOING treatments are always included; closed ones respect [fromMs].
     */
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

    // ── Unfiltered financial summary (used by Dashboard, global calcs) ─────────

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

    // ── Direct Supabase fetch for non-default filters ─────────────────────────

    /**
     * Fetches treatments and standalone visits directly from Supabase for non-default filters.
     * Results are NOT stored in Room.
     *
     * Always includes ONGOING treatments regardless of [fromMs].
     * [toMs] is inclusive upper bound (null = no upper bound, i.e. up to now).
     */
    suspend fun fetchFromSupabase(
        patientId: Long,
        fromMs: Long,
        toMs: Long
    ): RemoteTreatmentData {
        val pageSize = 100

        // Fetch treatments: ONGOING or within date range
        var offset = 0L
        val treatmentDtos = mutableListOf<TreatmentDto>()
        while (true) {
            val page = sync.supabase.from("treatments").select {
                filter {
                    eq("patient_id", patientId)
                    or {
                        eq("status", "ONGOING")
                        and {
                            gte("start_date", fromMs)
                            lte("start_date", toMs)
                        }
                    }
                }
                range(offset, offset + pageSize - 1)
            }.decodeList<TreatmentDto>()
            treatmentDtos.addAll(page)
            if (page.size < pageSize) break
            offset += pageSize
        }
        val treatments = treatmentDtos.map { it.toEntity() }

        // Fetch standalone visits within date range
        offset = 0L
        val visitDtos = mutableListOf<VisitDto>()
        while (true) {
            val page = sync.supabase.from("visits").select {
                filter {
                    eq("patient_id", patientId)
                    gte("visit_date", fromMs)
                    lte("visit_date", toMs)
                }
                range(offset, offset + pageSize - 1)
            }.decodeList<VisitDto>()
            visitDtos.addAll(page)
            if (page.size < pageSize) break
            offset += pageSize
        }
        val allVisits = visitDtos.map { it.toEntity() }

        // Fetch cross refs for the treatments we loaded
        val crossRefMap = mutableMapOf<Long, List<TreatmentVisitCrossRef>>()
        val treatmentIds = treatments.map { it.id }
        if (treatmentIds.isNotEmpty()) {
            offset = 0L
            val allCrossRefs = mutableListOf<TreatmentVisitCrossRefDto>()
            // Batch in groups of 50 to avoid overly large IN clauses
            treatmentIds.chunked(50).forEach { chunk ->
                var chunkOffset = 0L
                while (true) {
                    val page = sync.supabase.from("treatment_visit_cross_ref").select {
                        filter {
                            isIn("treatment_id", chunk)
                        }
                        range(chunkOffset, chunkOffset + pageSize - 1)
                    }.decodeList<TreatmentVisitCrossRefDto>()
                    allCrossRefs.addAll(page)
                    if (page.size < pageSize) break
                    chunkOffset += pageSize
                }
            }
            allCrossRefs.forEach { ref ->
                val existing = crossRefMap.getOrDefault(ref.visitId, emptyList())
                crossRefMap[ref.visitId] = existing + ref.toEntity()
            }
        }

        // Standalone visits = visits not referenced in any cross ref
        val linkedVisitIds = crossRefMap.keys
        val standaloneVisits = allVisits.filter { it.id !in linkedVisitIds }

        return RemoteTreatmentData(
            treatments = treatments,
            standaloneVisits = standaloneVisits,
            crossRefs = crossRefMap
        )
    }

    // ── Purge old data from Room ───────────────────────────────────────────────

    /**
     * Removes closed treatments older than [cutoffMs] for [patientId] from Room,
     * along with their linked visits and cross refs.
     * Standalone visits older than [cutoffMs] are also removed.
     * Run in background on app start — safe to call multiple times.
     */
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

    // ── Mutation operations ────────────────────────────────────────────────────

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

    /**
     * Fast outstanding read: quotedCost minus the sum of stored allocatedAmount values.
     * No runtime FIFO — allocations are written at addVisit / updateVisit time.
     */
    suspend fun calculateTreatmentOutstanding(treatmentId: Long): Double {
        val treatment = treatmentDao.getTreatmentById(treatmentId) ?: return 0.0
        val quotedCost = treatment.quotedCost ?: return 0.0
        val totalAllocated = crossRefDao.getTotalAllocatedForTreatment(treatmentId)
        return maxOf(0.0, quotedCost - totalAllocated)
    }

    /**
     * Compute FIFO allocations for all treatments linked to [visitId] and persist them.
     */
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

        db.withTransaction {
            updatedCrossRefs.forEach { crossRefDao.insert(it) }
        }

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

    suspend fun pullAll() {
        if (!sync.isConnected) return
        try {
            val cutoffMs = oneMonthAgoMs()
            val pageSize = 100

            // Pull treatments: ONGOING or within last 1 month
            var offset = 0L
            val treatments = mutableListOf<TreatmentDto>()
            while (true) {
                val page = sync.supabase.from("treatments").select {
                    filter {
                        or {
                            eq("status", "ONGOING")
                            gte("start_date", cutoffMs)
                        }
                    }
                    range(offset, offset + pageSize - 1)
                }.decodeList<TreatmentDto>()
                treatments.addAll(page)
                if (page.size < pageSize) break
                offset += pageSize
            }
            treatmentDao.upsertAll(treatments.map { it.toEntity() })

            // Pull visits within last 1 month
            offset = 0L
            val visits = mutableListOf<VisitDto>()
            while (true) {
                val page = sync.supabase.from("visits").select {
                    filter { gte("visit_date", cutoffMs) }
                    range(offset, offset + pageSize - 1)
                }.decodeList<VisitDto>()
                visits.addAll(page)
                if (page.size < pageSize) break
                offset += pageSize
            }
            visitDao.upsertAll(visits.map { it.toEntity() })

            // Pull cross refs for the treatments we loaded
            val treatmentIds = treatments.map { it.id }
            if (treatmentIds.isNotEmpty()) {
                treatmentIds.chunked(50).forEach { chunk ->
                    offset = 0L
                    val crossRefs = mutableListOf<TreatmentVisitCrossRefDto>()
                    while (true) {
                        val page = sync.supabase.from("treatment_visit_cross_ref").select {
                            filter { isIn("treatment_id", chunk) }
                            range(offset, offset + pageSize - 1)
                        }.decodeList<TreatmentVisitCrossRefDto>()
                        crossRefs.addAll(page)
                        if (page.size < pageSize) break
                        offset += pageSize
                    }
                    crossRefDao.upsertAll(crossRefs.map { it.toEntity() })
                }
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

            // Pull treatments: ONGOING or within last 1 month
            var offset = 0L
            val treatmentDtos = mutableListOf<TreatmentDto>()
            while (true) {
                val page = sync.supabase.from("treatments").select {
                    filter {
                        eq("patient_id", patientId)
                        or {
                            eq("status", "ONGOING")
                            gte("start_date", cutoffMs)
                        }
                    }
                    range(offset, offset + pageSize - 1)
                }.decodeList<TreatmentDto>()
                treatmentDtos.addAll(page)
                if (page.size < pageSize) break
                offset += pageSize
            }
            treatmentDao.upsertAll(treatmentDtos.map { it.toEntity() })

            // Pull visits within last 1 month
            offset = 0L
            val visitDtos = mutableListOf<VisitDto>()
            while (true) {
                val page = sync.supabase.from("visits").select {
                    filter {
                        eq("patient_id", patientId)
                        gte("visit_date", cutoffMs)
                    }
                    range(offset, offset + pageSize - 1)
                }.decodeList<VisitDto>()
                visitDtos.addAll(page)
                if (page.size < pageSize) break
                offset += pageSize
            }
            visitDao.upsertAll(visitDtos.map { it.toEntity() })

            // Pull cross refs for treatments loaded
            val treatmentIds = treatmentDtos.map { it.id }
            if (treatmentIds.isNotEmpty()) {
                treatmentIds.chunked(50).forEach { chunk ->
                    offset = 0L
                    val crossRefs = mutableListOf<TreatmentVisitCrossRefDto>()
                    while (true) {
                        val page = sync.supabase.from("treatment_visit_cross_ref").select {
                            filter { isIn("treatment_id", chunk) }
                            range(offset, offset + pageSize - 1)
                        }.decodeList<TreatmentVisitCrossRefDto>()
                        crossRefs.addAll(page)
                        if (page.size < pageSize) break
                        offset += pageSize
                    }
                    crossRefDao.upsertAll(crossRefs.map { it.toEntity() })
                }
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
        recomputeAllocationsForVisit(visitId)
        return visitId
    }

    suspend fun updateVisit(visit: VisitEntity) {
        visitDao.updateVisit(visit)
        sync.fireAndForget { sync.supabase.from("visits").upsert(visit.toDto()) }
        recomputeAllocationsForVisit(visit.id)
    }

    companion object {
        /** Returns the timestamp for exactly 1 month ago (start of that day). */
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
