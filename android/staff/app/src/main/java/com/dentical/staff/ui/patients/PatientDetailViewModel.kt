package com.dentical.staff.ui.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.entities.PatientEntity
import com.dentical.staff.data.local.entities.TreatmentEntity
import com.dentical.staff.data.local.entities.TreatmentStatus
import com.dentical.staff.data.local.entities.TreatmentVisitCrossRef
import com.dentical.staff.data.local.entities.VisitEntity
import com.dentical.staff.data.remote.SyncManager
import com.dentical.staff.data.repository.PatientFinancialSummary
import com.dentical.staff.data.repository.PatientRepository
import com.dentical.staff.data.repository.TreatmentRepository
import com.dentical.staff.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class TreatmentDateFilter {
    LAST_1M,
    LAST_6M,
    LAST_12M,
    CUSTOM
}

data class PatientDetailUiState(
    val patient: PatientEntity? = null,
    val isLoading: Boolean = true,
    val selectedTab: Int = 0,
    val treatments: List<TreatmentEntity> = emptyList(),
    val treatmentOutstandings: Map<Long, Double> = emptyMap(),
    val standaloneVisits: List<VisitEntity> = emptyList(),
    val visitCrossRefs: Map<Long, List<TreatmentVisitCrossRef>> = emptyMap(),
    val financialSummary: PatientFinancialSummary = PatientFinancialSummary(0.0, 0.0, 0.0, 0.0),
    val selectedFilter: TreatmentDateFilter = TreatmentDateFilter.LAST_1M,
    val customFromMs: Long = defaultCustomFrom(),
    val customToMs: Long = System.currentTimeMillis(),
    val isFilterLoading: Boolean = false,
    val filterError: String? = null,
    val error: String? = null
)

private fun defaultCustomFrom(): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, -1)
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val treatmentRepository: TreatmentRepository,
    private val syncManager: SyncManager,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
    val canSync: StateFlow<Boolean> = syncManager.canSync
    fun onSyncClick() = syncManager.syncAll()

    private val _uiState = MutableStateFlow(PatientDetailUiState())
    val uiState: StateFlow<PatientDetailUiState> = _uiState.asStateFlow()

    private var loadedPatientId = -1L

    // Jobs for Room flow collection — cancelled and restarted when filter changes
    private var treatmentsJob: Job? = null
    private var visitsJob: Job? = null
    private var financialJob: Job? = null

    fun loadPatient(id: Long) {
        if (loadedPatientId == id) return
        loadedPatientId = id

        viewModelScope.launch { treatmentRepository.pullForPatient(id) }

        // Patient info — always unfiltered
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            patientRepository.getPatientByIdFlow(id)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = "patient: ${e.javaClass.simpleName}: ${e.message}") } }
                .collect { patient ->
                    _uiState.update { it.copy(patient = patient, isLoading = false) }
                }
        }

        // Start default filter (Last 1M) Room collection
        collectFromRoom(id, TreatmentRepository.oneMonthAgoMs())
    }

    /**
     * Called when the user selects a filter chip.
     * Default (LAST_1M) reads from Room.
     * Non-default filters query Supabase directly if online, show error if offline.
     */
    fun onFilterSelected(
        filter: TreatmentDateFilter,
        customFromMs: Long? = null,
        customToMs: Long? = null
    ) {
        val patientId = loadedPatientId
        if (patientId == -1L) return

        _uiState.update { state ->
            state.copy(
                selectedFilter = filter,
                customFromMs = customFromMs ?: state.customFromMs,
                customToMs = customToMs ?: state.customToMs,
                filterError = null
            )
        }

        when (filter) {
            TreatmentDateFilter.LAST_1M -> {
                // Cancel any remote fetch jobs, go back to Room
                collectFromRoom(patientId, TreatmentRepository.oneMonthAgoMs())
            }
            TreatmentDateFilter.LAST_6M -> fetchFromSupabase(patientId, monthsAgoMs(6), System.currentTimeMillis())
            TreatmentDateFilter.LAST_12M -> fetchFromSupabase(patientId, monthsAgoMs(12), System.currentTimeMillis())
            TreatmentDateFilter.CUSTOM -> {
                val from = customFromMs ?: _uiState.value.customFromMs
                val to = customToMs ?: _uiState.value.customToMs
                fetchFromSupabase(patientId, from, to)
            }
        }
    }

    /** Starts (or restarts) Room flow collection for the default filter. */
    private fun collectFromRoom(patientId: Long, fromMs: Long) {
        treatmentsJob?.cancel()
        visitsJob?.cancel()
        financialJob?.cancel()

        treatmentsJob = viewModelScope.launch {
            treatmentRepository.getTreatmentsByPatientFiltered(patientId, fromMs)
                .catch { e -> _uiState.update { it.copy(error = "treatments: ${e.javaClass.simpleName}: ${e.message}") } }
                .collect { treatments ->
                    val outstandings = treatments.associate { t ->
                        t.id to if (t.status == TreatmentStatus.ONGOING)
                            treatmentRepository.calculateTreatmentOutstanding(t.id)
                        else 0.0
                    }
                    _uiState.update { it.copy(treatments = treatments, treatmentOutstandings = outstandings, isFilterLoading = false) }
                }
        }

        visitsJob = viewModelScope.launch {
            treatmentRepository.getStandaloneVisitsFiltered(patientId, fromMs)
                .catch { e -> _uiState.update { it.copy(error = "visits: ${e.javaClass.simpleName}: ${e.message}") } }
                .collect { visits ->
                    _uiState.update { it.copy(standaloneVisits = visits) }
                }
        }

        financialJob = viewModelScope.launch {
            treatmentRepository.getPatientFinancialSummaryFiltered(patientId, fromMs)
                .catch { e -> _uiState.update { it.copy(error = "financial: ${e.javaClass.simpleName}: ${e.message}") } }
                .collect { summary ->
                    _uiState.update { it.copy(financialSummary = summary) }
                }
        }
    }

    /** Fetches data directly from Supabase for non-default filters. Does not write to Room. */
    private fun fetchFromSupabase(patientId: Long, fromMs: Long, toMs: Long) {
        if (!networkMonitor.isConnected) {
            _uiState.update { it.copy(filterError = "Extended history requires an internet connection.") }
            return
        }

        // Cancel Room flows — we're showing remote data
        treatmentsJob?.cancel()
        visitsJob?.cancel()
        financialJob?.cancel()

        viewModelScope.launch {
            _uiState.update { it.copy(isFilterLoading = true, filterError = null) }
            try {
                val remote = treatmentRepository.fetchFromSupabase(patientId, fromMs, toMs)

                val outstandings = remote.treatments.associate { t ->
                    t.id to if (t.status == TreatmentStatus.ONGOING)
                        treatmentRepository.calculateTreatmentOutstanding(t.id)
                    else 0.0
                }

                // Financial summary from remote data (in-memory calc)
                val totalQuoted = remote.treatments.sumOf { it.quotedCost ?: 0.0 }
                val standaloneCharged = remote.standaloneVisits.sumOf { it.costCharged }
                // All visits (treatment-linked + standalone) for total paid
                val allVisitIds = remote.crossRefs.values.flatten().map { it.visitId }.toSet()
                // We can't easily compute totalPaid from remote data without fetching all visits,
                // so we reuse the Room outstanding calc for ongoing treatments and sum it
                val totalOutstanding = outstandings.values.sum()
                val financialSummary = PatientFinancialSummary(
                    totalQuoted = totalQuoted,
                    standaloneCharged = standaloneCharged,
                    totalPaid = maxOf(0.0, totalQuoted + standaloneCharged - totalOutstanding),
                    totalOutstanding = totalOutstanding
                )

                _uiState.update {
                    it.copy(
                        treatments = remote.treatments,
                        standaloneVisits = remote.standaloneVisits,
                        visitCrossRefs = remote.crossRefs,
                        treatmentOutstandings = outstandings,
                        financialSummary = financialSummary,
                        isFilterLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isFilterLoading = false,
                        filterError = "Could not load data. Check your connection and try again."
                    )
                }
            }
        }
    }

    fun onTabSelected(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun monthsAgoMs(months: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -months)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
