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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TreatmentDetailUiState(
    val treatment: TreatmentEntity? = null,
    val dentist: UserEntity? = null,
    val visits: List<VisitEntity> = emptyList(),
    val crossRefs: List<TreatmentVisitCrossRef> = emptyList(),
    val visitCount: Int = 0,
    val isLoading: Boolean = true
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
            treatmentRepository.getVisitsByTreatment(treatmentId).collect { visits ->
                _uiState.update { it.copy(visits = visits, visitCount = visits.size) }
            }
        }

        viewModelScope.launch {
            treatmentRepository.getCrossRefsForTreatment(treatmentId).collect { crossRefs ->
                _uiState.update { it.copy(crossRefs = crossRefs) }
            }
        }
    }

    fun markComplete(treatmentId: Long) {
        viewModelScope.launch {
            treatmentRepository.updateTreatmentStatus(treatmentId, TreatmentStatus.COMPLETED)
            // Refresh
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
            _uiState.update { it.copy(treatment = updated) }
        }
    }
}
