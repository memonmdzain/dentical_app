package com.dentical.staff.ui.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.entities.*
import com.dentical.staff.data.repository.AppointmentRepository
import com.dentical.staff.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class AddAppointmentUiState(
    // Patient search
    val patientQuery: String = "",
    val patientResults: List<PatientEntity> = emptyList(),
    val selectedPatient: PatientEntity? = null,
    val patientError: String? = null,
    // Dentist
    val dentists: List<UserEntity> = emptyList(),
    val selectedDentist: UserEntity? = null,
    val dentistError: String? = null,
    // Type
    val appointmentType: AppointmentType = AppointmentType.CONSULTATION,
    // Date & Time
    val dateMillis: Long? = null,
    val dateDisplay: String = "",
    val timeHour: Int = 9,
    val timeMinute: Int = 0,
    val timeDisplay: String = "",
    val dateError: String? = null,
    val timeError: String? = null,
    // Duration
    val durationMinutes: Int = 30,
    // Notes
    val notes: String = "",
    // UI state
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveError: String? = null
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddAppointmentViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val patientRepository: PatientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAppointmentUiState())
    val uiState: StateFlow<AddAppointmentUiState> = _uiState.asStateFlow()

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    init {
        // Load dentists
        viewModelScope.launch {
            appointmentRepository.getAllActiveDentists().collect { dentists ->
                _uiState.update { it.copy(dentists = dentists) }
            }
        }
        // Real-time patient search
        viewModelScope.launch {
            _uiState.map { it.patientQuery }
                .debounce(300)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isBlank()) flowOf(emptyList())
                    else patientRepository.searchPatients(query)
                }
                .collect { results ->
                    _uiState.update { it.copy(patientResults = results) }
                }
        }
    }

    fun onPatientQueryChange(q: String) {
        _uiState.update { it.copy(patientQuery = q, selectedPatient = null, patientError = null) }
    }

    fun onPatientSelected(patient: PatientEntity) {
        _uiState.update { it.copy(
            selectedPatient = patient,
            patientQuery = patient.fullName,
            patientResults = emptyList(),
            patientError = null
        )}
    }

    fun onDentistSelected(dentist: UserEntity) {
        _uiState.update { it.copy(selectedDentist = dentist, dentistError = null) }
    }

    fun onTypeSelected(type: AppointmentType) {
        _uiState.update { it.copy(appointmentType = type) }
    }

    fun onDurationSelected(minutes: Int) {
        _uiState.update { it.copy(durationMinutes = minutes) }
    }

    fun onNotesChange(v: String) { _uiState.update { it.copy(notes = v) } }
    fun onShowDatePicker() { _uiState.update { it.copy(showDatePicker = true) } }
    fun onDismissDatePicker() { _uiState.update { it.copy(showDatePicker = false) } }
    fun onShowTimePicker() { _uiState.update { it.copy(showTimePicker = true) } }
    fun onDismissTimePicker() { _uiState.update { it.copy(showTimePicker = false) } }

    fun onDateSelected(millis: Long) {
        _uiState.update { it.copy(
            dateMillis = millis,
            dateDisplay = dateFormatter.format(Date(millis)),
            showDatePicker = false,
            dateError = null
        )}
    }

    fun onTimeSelected(hour: Int, minute: Int) {
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute) }
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

        if (state.selectedPatient == null) {
            _uiState.update { it.copy(patientError = "Please select a patient") }
            hasError = true
        }
        if (state.selectedDentist == null) {
            _uiState.update { it.copy(dentistError = "Please select a dentist") }
            hasError = true
        }
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
                val cal = Calendar.getInstance().apply {
                    timeInMillis = state.dateMillis!!
                    set(Calendar.HOUR_OF_DAY, state.timeHour)
                    set(Calendar.MINUTE, state.timeMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                appointmentRepository.addAppointment(
                    AppointmentEntity(
                        patientId = state.selectedPatient!!.id,
                        dentistId = state.selectedDentist!!.id,
                        type = state.appointmentType,
                        scheduledAt = cal.timeInMillis,
                        durationMinutes = state.durationMinutes,
                        notes = state.notes.ifBlank { null }
                    )
                )
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = "Failed to save. Please try again.") }
            }
        }
    }
}
