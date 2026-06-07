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
     * Fetches treatments and visits directly from Supabase for non-default filters.
     * Results are NOT stored in Room.
     *
     * Key rule: linked visits are fetched by treatment ID (not by date).
     * Date filter only applies to standalone visits.
     */
    suspend fun fetchFromSupabase(
        patientId: Long,
        fromMs: Long,
        toMs: Long
    ): RemoteTreatmentData {
        val pageSize = 100

        // 1. Fetch treatments: ONGOING or within date range
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
        val treatmentIds = treatments.map { it.id }

        // 2. Fetch cross refs for these treatments — determines which visits are linked
        val crossRefMap = mutableMapOf<Long, MutableList<TreatmentVisitCrossRef>>()
        val linkedVisitIds = mutableSetOf<Long>()
        if (treatmentIds.isNotEmpty()) {
            treatmentIds.chunked(50).forEach { chunk ->
                var chunkOffset = 0L
                while (true) {
                    val page = sync.supabase.from("treatment_visit_cross_ref").select {
                        filter { isIn("treatment_id", chunk) }
                        range(chunkOffset, chunkOffset + pageSize - 1)
                    }.decodeList<TreatmentVisitCrossRefDto>()
                    page.forEach { ref ->
                        crossRefMap.getOrPut(ref.visitId) { mutableListOf() }.add(ref.toEntity())
                        linkedVisitIds.add(ref.visitId)
                    }
                    if (page.size < pageSize) break
                    chunkOffset += pageSize
                }
            }
        }

        // 3. Fetch linked visits by visit ID — date is irrelevant, they belong to shown treatments
        val linkedVisitEntities = mutableListOf<VisitEntity>()
        if (linkedVisitIds.isNotEmpty()) {
            linkedVisitIds.toList().chunked(50).forEach { chunk ->
                var chunkOffset = 0L
                while (true) {
                    val page = sync.supabase.from("visits").select {
                        filter { isIn("id", chunk) }
                        range(chunkOffset, chunkOffset + pageSize - 1)
                    }.decodeList<VisitDto>()
                    linkedVisitEntities.addAll(page.map { it.toEntity() })
                    if (page.size < pageSize) break
                    chunkOffset += pageSize
                }
            }
        }

        // 4. Fetch standalone visits by date range — date filter is correct here
        offset = 0L
        val standaloneVisits = mutableListOf<VisitEntity>()
        while (true) {
            val page = sync.supabase.from("visits").select {
                filter {
                    eq("patient_id", patientId)
                    gte("visit_date", fromMs)
                    lte("visit_date", toMs)
                }
                range(offset, offset + pageSize - 1)
            }.decodeList<VisitDto>()
            // Only keep visits with no treatment link
            standaloneVisits.addAll(page.filter { it.id !in linkedVisitIds }.map { it.toEntity() })
            if (page.size < pageSize) break
            offset += pageSize
        }

        return RemoteTreatmentData(
            treatments = treatments,
            standaloneVisits = standaloneVisits,
            crossRefs = crossRefMap
        )
    }

    /**
     * Fetches a single treatment directly from Supabase.
     * Used when the treatment is not in Room (e.g. purged old closed treatment).
     * Returns null if not found or offline.
     */
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

    /**
     * Fetches visits and cross refs for a treatment directly from Supabase.
     * Used when the treatment is not in Room.
     * Returns Pair(visits, crossRefs).
     */
    suspend fun fetchVisitsForTreatment(treatmentId: Long): Pair<List<VisitEntity>, List<TreatmentVisitCrossRef>> {
        if (!sync.isConnected) return Pair(emptyList(), emptyList())
        return try {
            val pageSize = 100

            // Fetch cross refs for this treatment
            var offset = 0L
            val crossRefDtos = mutableListOf<TreatmentVisitCrossRefDto>()
            while (true) {
                val page = sync.supabase.from("treatment_visit_cross_ref").select {
                    filter { eq("treatment_id", treatmentId) }
                    range(offset, offset + pageSize - 1)
                }.decodeList<TreatmentVisitCrossRefDto>()
                crossRefDtos.addAll(page)
                if (page.size < pageSize) break
                offset += pageSize
            }

            val visitIds = crossRefDtos.map { it.visitId }
            val visits = mutableListOf<VisitEntity>()
            if (visitIds.isNotEmpty()) {
                visitIds.chunked(50).forEach { chunk ->
                    offset = 0L
                    while (true) {
                        val page = sync.supabase.from("visits").select {
                            filter { isIn("id", chunk) }
                            range(offset, offset + pageSize - 1)
                        }.decodeList<VisitDto>()
                        visits.addAll(page.map { it.toEntity() })
                        if (page.size < pageSize) break
                        offset += pageSize
                    }
                }
            }

            Pair(visits, crossRefDtos.map { it.toEntity() })
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

            // 1. Pull treatments: ONGOING or within last 1 month
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

            // 2. Pull cross refs for loaded treatments — determines which visits are linked
            val treatmentIds = treatments.map { it.id }
            val linkedVisitIds = mutableSetOf<Long>()
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
                    crossRefs.forEach { linkedVisitIds.add(it.visitId) }
                }
            }

            // 3. Pull linked visits by ID — date is irrelevant, they belong to loaded treatments
            if (linkedVisitIds.isNotEmpty()) {
                linkedVisitIds.toList().chunked(50).forEach { chunk ->
                    offset = 0L
                    val linkedVisits = mutableListOf<VisitDto>()
                    while (true) {
                        val page = sync.supabase.from("visits").select {
                            filter { isIn("id", chunk) }
                            range(offset, offset + pageSize - 1)
                        }.decodeList<VisitDto>()
                        linkedVisits.addAll(page)
                        if (page.size < pageSize) break
                        offset += pageSize
                    }
                    visitDao.upsertAll(linkedVisits.map { it.toEntity() })
                }
            }

            // 4. Pull standalone visits by date — date filter is correct here
            offset = 0L
            while (true) {
                val page = sync.supabase.from("visits").select {
                    filter { gte("visit_date", cutoffMs) }
                    range(offset, offset + pageSize - 1)
                }.decodeList<VisitDto>()
                // Only upsert visits not already loaded as linked
                val standaloneOnly = page.filter { it.id !in linkedVisitIds }
                if (standaloneOnly.isNotEmpty()) visitDao.upsertAll(standaloneOnly.map { it.toEntity() })
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

            // 1. Pull treatments: ONGOING or within last 1 month
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

            // 2. Pull cross refs for loaded treatments
            val treatmentIds = treatmentDtos.map { it.id }
            val linkedVisitIds = mutableSetOf<Long>()
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
                    crossRefs.forEach { linkedVisitIds.add(it.visitId) }
                }
            }

            // 3. Pull linked visits by ID — date is irrelevant, they belong to loaded treatments
            if (linkedVisitIds.isNotEmpty()) {
                linkedVisitIds.toList().chunked(50).forEach { chunk ->
                    offset = 0L
                    val linkedVisits = mutableListOf<VisitDto>()
                    while (true) {
                        val page = sync.supabase.from("visits").select {
                            filter { isIn("id", chunk) }
                            range(offset, offset + pageSize - 1)
                        }.decodeList<VisitDto>()
                        linkedVisits.addAll(page)
                        if (page.size < pageSize) break
                        offset += pageSize
                    }
                    visitDao.upsertAll(linkedVisits.map { it.toEntity() })
                }
            }

            // 4. Pull standalone visits by date
            offset = 0L
            while (true) {
                val page = sync.supabase.from("visits").select {
                    filter {
                        eq("patient_id", patientId)
                        gte("visit_date", cutoffMs)
                    }
                    range(offset, offset + pageSize - 1)
                }.decodeList<VisitDto>()
                val standaloneOnly = page.filter { it.id !in linkedVisitIds }
                if (standaloneOnly.isNotEmpty()) visitDao.upsertAll(standaloneOnly.map { it.toEntity() })
                if (page.size < pageSize) break
                offset += pageSize
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
