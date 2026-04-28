package com.dentical.staff.ui.treatments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.entities.PaymentMode
import com.dentical.staff.data.local.entities.TreatmentEntity
import com.dentical.staff.data.local.entities.UserEntity
import com.dentical.staff.data.local.entities.VisitEntity
import com.dentical.staff.data.repository.AppointmentRepository
import com.dentical.staff.data.repository.TreatmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class TreatmentSelection(
    val treatment: TreatmentEntity,
    val isSelected: Boolean = false,
    val workDone: String = ""
)

data class AddVisitUiState(
    val visitDateMillis: Long = todayMillis(),
    val selectedDentistId: Long? = null,
    val amountPaid: String = "",
    val paymentMode: PaymentMode? = null,
    val costCharged: String = "",
    val notes: String = "",
    val treatmentSelections: List<TreatmentSelection> = emptyList(),
    val dentists: List<UserEntity> = emptyList(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

private fun todayMillis(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

@HiltViewModel
class AddVisitViewModel @Inject constructor(
    private val treatmentRepository: TreatmentRepository,
    private val appointmentRepository: AppointmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddVisitUiState())
    val uiState: StateFlow<AddVisitUiState> = _uiState.asStateFlow()

    private val loadErrorHandler = CoroutineExceptionHandler { _, throwable ->
        _uiState.update { it.copy(error = "Load error: ${throwable.javaClass.simpleName}: ${throwable.message}") }
    }

    fun load(patientId: Long, preSelectedTreatmentId: Long) {
        viewModelScope.launch(loadErrorHandler) {
            appointmentRepository.getAllActiveDentists().collect { dentists ->
                _uiState.update { it.copy(dentists = dentists) }
            }
        }

        viewModelScope.launch(loadErrorHandler) {
            treatmentRepository.getOngoingTreatmentsByPatient(patientId).collect { treatments ->
                _uiState.update { state ->
                    val selections = treatments.map { treatment ->
                        val existing = state.treatmentSelections.find { it.treatment.id == treatment.id }
                        TreatmentSelection(
                            treatment = treatment,
                            isSelected = existing?.isSelected ?: (treatment.id == preSelectedTreatmentId),
                            workDone = existing?.workDone ?: ""
                        )
                    }
                    state.copy(treatmentSelections = selections)
                }
            }
        }
    }

    fun onVisitDateChange(millis: Long) = _uiState.update { it.copy(visitDateMillis = millis) }
    fun onDentistSelected(id: Long?) = _uiState.update { it.copy(selectedDentistId = id) }
    fun onAmountPaidChange(v: String) = _uiState.update { it.copy(amountPaid = v) }
    fun onPaymentModeChange(mode: PaymentMode?) = _uiState.update { it.copy(paymentMode = mode) }
    fun onCostChargedChange(v: String) = _uiState.update { it.copy(costCharged = v) }
    fun onNotesChange(v: String) = _uiState.update { it.copy(notes = v) }

    fun onTreatmentToggle(treatmentId: Long, selected: Boolean) {
        _uiState.update { state ->
            state.copy(
                treatmentSelections = state.treatmentSelections.map {
                    if (it.treatment.id == treatmentId) it.copy(isSelected = selected) else it
                }
            )
        }
    }

    fun onWorkDoneChange(treatmentId: Long, workDone: String) {
        _uiState.update { state ->
            state.copy(
                treatmentSelections = state.treatmentSelections.map {
                    if (it.treatment.id == treatmentId) it.copy(workDone = workDone) else it
                }
            )
        }
    }

    fun save(patientId: Long) {
        val state = _uiState.value
        val dentistName = state.dentists.find { it.id == state.selectedDentistId }?.fullName
            ?: return Unit.also {
                _uiState.update { it.copy(error = "Please select who performed the visit") }
            }

        val selectedTreatments = state.treatmentSelections.filter { it.isSelected }
        val isStandalone = selectedTreatments.isEmpty()

        if (isStandalone && state.costCharged.isBlank()) {
            _uiState.update { it.copy(error = "Please enter the cost charged for this visit") }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            try {
                val visit = VisitEntity(
                    patientId = patientId,
                    visitDate = state.visitDateMillis,
                    performedBy = dentistName,
                    amountPaid = state.amountPaid.toDoubleOrNull() ?: 0.0,
                    paymentMode = state.paymentMode,
                    costCharged = if (isStandalone) state.costCharged.toDoubleOrNull() ?: 0.0 else 0.0,
                    notes = state.notes.trim().ifBlank { null },
                    createdAt = System.currentTimeMillis()
                )
                val treatmentLinks = selectedTreatments.map { selection ->
                    Pair(selection.treatment.id, selection.workDone.trim())
                }
                treatmentRepository.addVisit(visit, treatmentLinks)
                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Throwable) {
                _uiState.update { it.copy(isSaving = false, error = "${e.javaClass.simpleName}: ${e.message}") }
            }
        }
    }
}
