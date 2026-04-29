package com.dentical.staff.ui.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.entities.*
import com.dentical.staff.data.repository.AppointmentRepository
import com.dentical.staff.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EditAppointmentViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val patientRepository: PatientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAppointmentUiState())
    val uiState: StateFlow<AddAppointmentUiState> = _uiState.asStateFlow()

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private var appointmentId: Long = -1

    init {
        viewModelScope.launch {
            appointmentRepository.getAllActiveDentists().collect { dentists ->
                _uiState.update { it.copy(dentists = dentists) }
            }
        }
    }

    fun load(id: Long) {
        appointmentId = id
        viewModelScope.launch {
            val appt = appointmentRepository.getAppointmentById(id) ?: return@launch
            val patient = patientRepository.getPatientById(appt.patientId)
            val dentist = _uiState.value.dentists.find { it.id == appt.dentistId }
            val cal = Calendar.getInstance().apply { timeInMillis = appt.scheduledAt }

            _uiState.update { it.copy(
                selectedPatient = patient,
                patientQuery = patient?.fullName ?: "",
                selectedDentist = dentist,
                appointmentType = appt.type,
                dateMillis = appt.scheduledAt,
                dateDisplay = dateFormatter.format(Date(appt.scheduledAt)),
                timeHour = cal.get(Calendar.HOUR_OF_DAY),
                timeMinute = cal.get(Calendar.MINUTE),
                timeDisplay = timeFormatter.format(cal.time),
                durationMinutes = appt.durationMinutes,
                notes = appt.notes ?: ""
            )}
        }
    }

    fun onDentistSelected(dentist: UserEntity) =
        _uiState.update { it.copy(selectedDentist = dentist, dentistError = null) }
    fun onTypeSelected(type: AppointmentType) =
        _uiState.update { it.copy(appointmentType = type) }
    fun onDurationSelected(minutes: Int) =
        _uiState.update { it.copy(durationMinutes = minutes) }
    fun onNotesChange(v: String) = _uiState.update { it.copy(notes = v) }
    fun onShowDatePicker() = _uiState.update { it.copy(showDatePicker = true) }
    fun onDismissDatePicker() = _uiState.update { it.copy(showDatePicker = false) }
    fun onShowTimePicker() = _uiState.update { it.copy(showTimePicker = true) }
    fun onDismissTimePicker() = _uiState.update { it.copy(showTimePicker = false) }

    fun onDateSelected(millis: Long) {
        _uiState.update { it.copy(
            dateMillis = millis,
            dateDisplay = dateFormatter.format(Date(millis)),
            showDatePicker = false,
            dateError = null
        )}
    }

    fun onTimeSelected(hour: Int, minute: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
        }
        _uiState.update { it.copy(
            timeHour = hour,
            timeMinute = minute,
            timeDisplay = timeFormatter.format(cal.time),
            showTimePicker = false,
            timeError = null
        )}
    }

    fun onSave() {
        val state = _uiState.value
        var hasError = false
        if (state.dateMillis == null) {
            _uiState.update { it.copy(dateError = "Please select a date") }
            hasError = true
        }
        if (state.timeDisplay.isBlank()) {
            _uiState.update { it.copy(timeError = "Please select a time") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val existing = appointmentRepository.getAppointmentById(appointmentId) ?: return@launch
                val cal = Calendar.getInstance().apply {
                    timeInMillis = state.dateMillis!!
                    set(Calendar.HOUR_OF_DAY, state.timeHour)
                    set(Calendar.MINUTE, state.timeMinute)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                appointmentRepository.updateAppointment(existing.copy(
                    dentistId = state.selectedDentist?.id,
                    type = state.appointmentType,
                    scheduledAt = cal.timeInMillis,
                    durationMinutes = state.durationMinutes,
                    notes = state.notes.ifBlank { null }
                ))
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = "Failed to update. Please try again.") }
            }
        }
    }
}
