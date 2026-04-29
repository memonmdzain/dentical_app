package com.dentical.staff.ui.treatments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.entities.AppointmentType
import com.dentical.staff.data.local.entities.TreatmentEntity
import com.dentical.staff.data.local.entities.UserEntity
import com.dentical.staff.data.repository.AppointmentRepository
import com.dentical.staff.data.repository.TreatmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class AddTreatmentUiState(
    val procedure: AppointmentType = AppointmentType.CONSULTATION,
    val toothNumber: String = "",
    val description: String = "",
    val quotedCost: String = "",
    val visitsRequired: String = "",
    val notes: String = "",
    val selectedDentistId: Long? = null,
    val startDateMillis: Long = todayStartMillis(),
    val dentists: List<UserEntity> = emptyList(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

private fun todayStartMillis(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

@HiltViewModel
class AddTreatmentViewModel @Inject constructor(
    private val treatmentRepository: TreatmentRepository,
    private val appointmentRepository: AppointmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTreatmentUiState())
    val uiState: StateFlow<AddTreatmentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appointmentRepository.getAllActiveDentists().collect { dentists ->
                _uiState.update { it.copy(dentists = dentists) }
            }
        }
    }

    fun onProcedureChange(procedure: AppointmentType) = _uiState.update { it.copy(procedure = procedure) }
    fun onToothNumberChange(v: String) = _uiState.update { it.copy(toothNumber = v) }
    fun onDescriptionChange(v: String) = _uiState.update { it.copy(description = v) }
    fun onQuotedCostChange(v: String) = _uiState.update { it.copy(quotedCost = v) }
    fun onVisitsRequiredChange(v: String) = _uiState.update { it.copy(visitsRequired = v) }
    fun onNotesChange(v: String) = _uiState.update { it.copy(notes = v) }
    fun onDentistSelected(id: Long?) = _uiState.update { it.copy(selectedDentistId = id) }
    fun onStartDateChange(millis: Long) = _uiState.update { it.copy(startDateMillis = millis) }

    fun save(patientId: Long) {
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val treatment = TreatmentEntity(
                    patientId = patientId,
                    dentistId = state.selectedDentistId,
                    procedure = state.procedure,
                    toothNumber = state.toothNumber.trim().ifBlank { null },
                    description = state.description.trim().ifBlank { null },
                    quotedCost = state.quotedCost.trim().toDoubleOrNull(),
                    visitsRequired = state.visitsRequired.trim().toIntOrNull(),
                    notes = state.notes.trim().ifBlank { null },
                    startDate = state.startDateMillis,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                treatmentRepository.addTreatment(treatment)
                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Failed to save treatment") }
            }
        }
    }
}
