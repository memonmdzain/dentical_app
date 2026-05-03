package com.dentical.staff.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.repository.TreatmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val ongoingTreatmentCount: Int = 0,
    val todaysCollections: Double = 0.0,
    val totalOutstanding: Double = 0.0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val treatmentRepository: TreatmentRepository
) : ViewModel() {

    private var lastPulledAt = 0L
    private val cooldownMs = 2 * 60 * 1000L

    fun refresh() {
        val now = System.currentTimeMillis()
        if (now - lastPulledAt < cooldownMs) return
        lastPulledAt = now
        viewModelScope.launch { treatmentRepository.pullAll() }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        treatmentRepository.getOngoingTreatmentCount(),
        treatmentRepository.getTodaysCollections(),
        treatmentRepository.getTotalOutstandingBalance()
    ) { count, collections, outstanding ->
        DashboardUiState(
            isLoading = false,
            ongoingTreatmentCount = count,
            todaysCollections = collections,
            totalOutstanding = outstanding
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}
