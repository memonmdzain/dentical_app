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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditVisitUiState(
    val patientId: Long = 0,
    val originalCreatedAt: Long = 0,
    val originalAmountPaid: Double = 0.0,
    val visitDateMillis: Long = System.currentTimeMillis(),
    val selectedDentistId: Long? = null,
    val performedByOriginal: String = "",
    val amountPaid: String = "",
    val paymentMode: PaymentMode? = null,
    val costCharged: String = "",
    val notes: String = "",
    val isStandalone: Boolean = true,
    val linkedTreatments: List<TreatmentEntity> = emptyList(),
    val dentists: List<UserEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EditVisitViewModel @Inject constructor(
    private val treatmentRepository: TreatmentRepository,
    private val appointmentRepository: AppointmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditVisitUiState())
    val uiState: StateFlow<EditVisitUiState> = _uiState.asStateFlow()

    private var loadedVisitId = -1L

    fun load(visitId: Long) {
        if (loadedVisitId == visitId) return
        loadedVisitId = visitId

        viewModelScope.launch {
            appointmentRepository.getAllActiveDentists().collect { dentists ->
                _uiState.update { it.copy(dentists = dentists) }
            }
        }

        viewModelScope.launch {
            try {
                val dentists = appointmentRepository.getAllActiveDentists().first()
                val visit = treatmentRepository.getVisitById(visitId) ?: return@launch
                val crossRefs = treatmentRepository.getCrossRefsForVisit(visitId)
                val linkedTreatments = crossRefs.mapNotNull {
                    treatmentRepository.getTreatmentById(it.treatmentId)
                }
                val dentistId = dentists.find { it.fullName == visit.performedBy }?.id

                _uiState.update {
                    it.copy(
                        patientId = visit.patientId,
                        originalCreatedAt = visit.createdAt,
                        originalAmountPaid = visit.amountPaid,
                        visitDateMillis = visit.visitDate,
                        selectedDentistId = dentistId,
                        performedByOriginal = visit.performedBy,
                        amountPaid = if (visit.amountPaid > 0)
                            visit.amountPaid.toBigDecimal().stripTrailingZeros().toPlainString()
                        else "",
                        paymentMode = visit.paymentMode,
                        costCharged = if (visit.costCharged > 0)
                            visit.costCharged.toBigDecimal().stripTrailingZeros().toPlainString()
                        else "",
                        notes = visit.notes ?: "",
                        isStandalone = crossRefs.isEmpty(),
                        linkedTreatments = linkedTreatments,
                        dentists = dentists,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load visit") }
            }
        }
    }

    fun onVisitDateChange(millis: Long) = _uiState.update { it.copy(visitDateMillis = millis) }
    fun onDentistSelected(id: Long?) = _uiState.update { it.copy(selectedDentistId = id) }
    fun onAmountPaidChange(v: String) = _uiState.update { it.copy(amountPaid = v) }
    fun onPaymentModeChange(mode: PaymentMode?) = _uiState.update { it.copy(paymentMode = mode) }
    fun onCostChargedChange(v: String) = _uiState.update { it.copy(costCharged = v) }
    fun onNotesChange(v: String) = _uiState.update { it.copy(notes = v) }

    fun save(visitId: Long) {
        val state = _uiState.value
        val dentistName = state.dentists.find { it.id == state.selectedDentistId }?.fullName
            ?: return Unit.also {
                _uiState.update { it.copy(error = "Please select who performed the visit") }
            }

        if (state.isStandalone && state.costCharged.isBlank()) {
            _uiState.update { it.copy(error = "Please enter the cost charged") }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            try {
                val amountPaidVal = state.amountPaid.toDoubleOrNull() ?: 0.0
                if (amountPaidVal > 0.0) {
                    val maxAllowed = if (state.isStandalone) {
                        state.costCharged.toDoubleOrNull() ?: 0.0
                    } else {
                        state.linkedTreatments.sumOf {
                            treatmentRepository.calculateTreatmentOutstanding(it.id)
                        } + state.originalAmountPaid
                    }
                    if (amountPaidVal > maxAllowed + 0.01) {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                error = "Amount paid ₹${amountPaidVal.toLong()} exceeds outstanding ₹${maxAllowed.toLong()}. Reduce payment or update the treatment cost first."
                            )
                        }
                        return@launch
                    }
                }

                val visit = VisitEntity(
                    id = visitId,
                    patientId = state.patientId,
                    visitDate = state.visitDateMillis,
                    performedBy = dentistName,
                    amountPaid = state.amountPaid.toDoubleOrNull() ?: 0.0,
                    paymentMode = state.paymentMode,
                    costCharged = if (state.isStandalone) state.costCharged.toDoubleOrNull() ?: 0.0 else 0.0,
                    notes = state.notes.trim().ifBlank { null },
                    createdAt = state.originalCreatedAt
                )
                treatmentRepository.updateVisit(visit)
                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isSaving = false, error = "${e.javaClass.simpleName}: ${e.message}") }
            }
        }
    }
}
