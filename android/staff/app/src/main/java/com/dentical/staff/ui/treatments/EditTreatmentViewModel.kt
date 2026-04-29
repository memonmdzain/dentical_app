package com.dentical.staff.ui.treatments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.entities.AppointmentType
import com.dentical.staff.data.local.entities.UserEntity
import com.dentical.staff.data.repository.AppointmentRepository
import com.dentical.staff.data.repository.TreatmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditTreatmentUiState(
    val procedure: AppointmentType = AppointmentType.CONSULTATION,
    val toothNumber: String = "",
    val description: String = "",
    val quotedCost: String = "",
    val visitsRequired: String = "",
    val notes: String = "",
    val selectedDentistId: Long? = null,
    val startDateMillis: Long = System.currentTimeMillis(),
    val dentists: List<UserEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EditTreatmentViewModel @Inject constructor(
    private val treatmentRepository: TreatmentRepository,
    private val appointmentRepository: AppointmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditTreatmentUiState())
    val uiState: StateFlow<EditTreatmentUiState> = _uiState.asStateFlow()

    private var loadedTreatmentId = -1L

    fun load(treatmentId: Long) {
        if (loadedTreatmentId == treatmentId) return
        loadedTreatmentId = treatmentId

        viewModelScope.launch {
            appointmentRepository.getAllActiveDentists().collect { dentists ->
                _uiState.update { it.copy(dentists = dentists) }
            }
        }

        viewModelScope.launch {
            try {
                val treatment = treatmentRepository.getTreatmentById(treatmentId) ?: return@launch
                _uiState.update {
                    it.copy(
                        procedure = treatment.procedure,
                        toothNumber = treatment.toothNumber ?: "",
                        description = treatment.description ?: "",
                        quotedCost = treatment.quotedCost
                            ?.toBigDecimal()?.stripTrailingZeros()?.toPlainString() ?: "",
                        visitsRequired = treatment.visitsRequired?.toString() ?: "",
                        notes = treatment.notes ?: "",
                        selectedDentistId = treatment.dentistId,
                        startDateMillis = treatment.startDate,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load treatment") }
            }
        }
    }

    fun onProcedureChange(v: AppointmentType) = _uiState.update { it.copy(procedure = v) }
    fun onToothNumberChange(v: String) = _uiState.update { it.copy(toothNumber = v) }
    fun onDescriptionChange(v: String) = _uiState.update { it.copy(description = v) }
    fun onQuotedCostChange(v: String) = _uiState.update { it.copy(quotedCost = v) }
    fun onVisitsRequiredChange(v: String) = _uiState.update { it.copy(visitsRequired = v) }
    fun onNotesChange(v: String) = _uiState.update { it.copy(notes = v) }
    fun onDentistSelected(id: Long?) = _uiState.update { it.copy(selectedDentistId = id) }
    fun onStartDateChange(millis: Long) = _uiState.update { it.copy(startDateMillis = millis) }

    fun save(treatmentId: Long) {
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val existing = treatmentRepository.getTreatmentById(treatmentId) ?: return@launch
                val state = _uiState.value
                treatmentRepository.updateTreatment(
                    existing.copy(
                        dentistId = state.selectedDentistId,
                        procedure = state.procedure,
                        toothNumber = state.toothNumber.trim().ifBlank { null },
                        description = state.description.trim().ifBlank { null },
                        quotedCost = state.quotedCost.trim().toDoubleOrNull(),
                        visitsRequired = state.visitsRequired.trim().toIntOrNull(),
                        notes = state.notes.trim().ifBlank { null },
                        startDate = state.startDateMillis,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isSaving = false, error = "${e.javaClass.simpleName}: ${e.message}") }
            }
        }
    }
}
