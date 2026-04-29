package com.dentical.staff.ui.treatments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.entities.TreatmentEntity
import com.dentical.staff.data.local.entities.TreatmentStatus
import com.dentical.staff.data.local.entities.TreatmentVisitCrossRef
import com.dentical.staff.data.local.entities.UserEntity
import com.dentical.staff.data.local.entities.VisitEntity
import com.dentical.staff.data.repository.AppointmentRepository
import com.dentical.staff.data.repository.TreatmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TreatmentDetailUiState(
    val treatment: TreatmentEntity? = null,
    val dentist: UserEntity? = null,
    val visits: List<VisitEntity> = emptyList(),
    val crossRefs: List<TreatmentVisitCrossRef> = emptyList(),
    val visitCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class TreatmentDetailViewModel @Inject constructor(
    private val treatmentRepository: TreatmentRepository,
    private val appointmentRepository: AppointmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TreatmentDetailUiState())
    val uiState: StateFlow<TreatmentDetailUiState> = _uiState.asStateFlow()

    fun load(treatmentId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val treatment = treatmentRepository.getTreatmentById(treatmentId)
            val dentist = treatment?.dentistId?.let { appointmentRepository.getDentistById(it) }
            _uiState.update { it.copy(treatment = treatment, dentist = dentist, isLoading = false) }
        }

        viewModelScope.launch {
            treatmentRepository.getVisitsByTreatment(treatmentId)
                .catch { /* ignore — show empty list */ }
                .collect { visits ->
                    _uiState.update { it.copy(visits = visits, visitCount = visits.size) }
                }
        }

        viewModelScope.launch {
            treatmentRepository.getCrossRefsForTreatment(treatmentId)
                .catch { /* ignore */ }
                .collect { crossRefs ->
                    _uiState.update { it.copy(crossRefs = crossRefs) }
                }
        }
    }

    fun markComplete(treatmentId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val outstanding = treatmentRepository.calculateTreatmentOutstanding(treatmentId)
            if (outstanding > 0.01) {
                val name = _uiState.value.treatment?.procedure?.displayName ?: "treatment"
                _uiState.update {
                    it.copy(error = "Cannot complete '$name': ₹${outstanding.toLong()} still outstanding. Settle the balance or increase the quoted cost first.")
                }
                return@launch
            }
            treatmentRepository.updateTreatmentStatus(treatmentId, TreatmentStatus.COMPLETED)
            val updated = treatmentRepository.getTreatmentById(treatmentId)
            _uiState.update { it.copy(treatment = updated) }
        }
    }

    fun cancelTreatment(treatmentId: Long) {
        viewModelScope.launch {
            treatmentRepository.updateTreatmentStatus(treatmentId, TreatmentStatus.CANCELLED)
            val updated = treatmentRepository.getTreatmentById(treatmentId)
            _uiState.update { it.copy(treatment = updated) }
        }
    }

    fun reactivateTreatment(treatmentId: Long) {
        viewModelScope.launch {
            treatmentRepository.updateTreatmentStatus(treatmentId, TreatmentStatus.ONGOING)
            val updated = treatmentRepository.getTreatmentById(treatmentId)
            _uiState.update { it.copy(treatment = updated, error = null) }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
}
