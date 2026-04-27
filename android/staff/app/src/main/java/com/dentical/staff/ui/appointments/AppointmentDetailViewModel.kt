package com.dentical.staff.ui.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.entities.*
import com.dentical.staff.data.repository.AppointmentRepository
import com.dentical.staff.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppointmentDetailUiState(
    val appointment: AppointmentEntity? = null,
    val patient: PatientEntity? = null,
    val dentist: UserEntity? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class AppointmentDetailViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val patientRepository: PatientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentDetailUiState())
    val uiState: StateFlow<AppointmentDetailUiState> = _uiState.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val appt = appointmentRepository.getAppointmentById(id)
            val patient = appt?.let { patientRepository.getPatientById(it.patientId) }
            val dentist = appt?.dentistId?.let { appointmentRepository.getDentistById(it) }
            _uiState.update { it.copy(
                appointment = appt,
                patient = patient,
                dentist = dentist,
                isLoading = false
            )}
        }
    }

    fun updateStatus(id: Long, status: AppointmentStatus) {
        viewModelScope.launch {
            appointmentRepository.updateStatus(id, status)
            val updated = appointmentRepository.getAppointmentById(id)
            _uiState.update { it.copy(appointment = updated) }
        }
    }
}
