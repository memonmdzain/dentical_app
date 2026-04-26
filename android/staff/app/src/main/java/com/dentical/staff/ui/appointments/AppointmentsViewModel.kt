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
import java.util.Calendar
import javax.inject.Inject

enum class AppointmentViewMode { LIST, CALENDAR }
enum class CalendarViewType { DAY, WEEK, MONTH }

data class AppointmentWithDetails(
    val appointment: AppointmentEntity,
    val patient: PatientEntity?,
    val dentist: UserEntity?
)

data class AppointmentsUiState(
    val viewMode: AppointmentViewMode = AppointmentViewMode.LIST,
    val calendarViewType: CalendarViewType = CalendarViewType.DAY,
    val selectedDate: Long = todayStartMillis(),
    val appointments: List<AppointmentWithDetails> = emptyList(),
    val isLoading: Boolean = true
)

fun todayStartMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppointmentsViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val patientRepository: PatientRepository
) : ViewModel() {

    private val _viewMode = MutableStateFlow(AppointmentViewMode.LIST)
    private val _calendarViewType = MutableStateFlow(CalendarViewType.DAY)
    private val _selectedDate = MutableStateFlow(todayStartMillis())
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<AppointmentsUiState> = combine(
        _viewMode, _calendarViewType, _selectedDate
    ) { mode, calType, date -> Triple(mode, calType, date) }
        .flatMapLatest { (mode, calType, date) ->
            val flow = if (mode == AppointmentViewMode.LIST) {
                appointmentRepository.getAppointmentsFrom(todayStartMillis())
            } else {
                when (calType) {
                    CalendarViewType.DAY -> appointmentRepository.getAppointmentsByDay(date)
                    CalendarViewType.WEEK -> {
                        val weekStart = date - (Calendar.getInstance().apply {
                            timeInMillis = date }.get(Calendar.DAY_OF_WEEK) - 1) * 86_400_000L
                        appointmentRepository.getAppointmentsByRange(weekStart, weekStart + 7 * 86_400_000L)
                    }
                    CalendarViewType.MONTH -> {
                        val cal = Calendar.getInstance().apply { timeInMillis = date }
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        val start = cal.timeInMillis
                        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                        val end = cal.timeInMillis + 86_400_000L
                        appointmentRepository.getAppointmentsByRange(start, end)
                    }
                }
            }
            flow.map { appointments ->
                appointments.map { appt ->
                    AppointmentWithDetails(
                        appointment = appt,
                        patient = patientRepository.getPatientById(appt.patientId),
                        dentist = null // loaded separately
                    )
                }
            }
        }
        .combine(_viewMode) { appts, mode -> Pair(appts, mode) }
        .combine(_calendarViewType) { (appts, mode), calType -> Triple(appts, mode, calType) }
        .combine(_selectedDate) { (appts, mode, calType), date ->
            _isLoading.value = false
            AppointmentsUiState(
                viewMode = mode,
                calendarViewType = calType,
                selectedDate = date,
                appointments = appts,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppointmentsUiState()
        )

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == AppointmentViewMode.LIST)
            AppointmentViewMode.CALENDAR else AppointmentViewMode.LIST
    }

    fun setCalendarViewType(type: CalendarViewType) { _calendarViewType.value = type }
    fun setSelectedDate(millis: Long) { _selectedDate.value = millis }

    fun updateStatus(appointmentId: Long, status: AppointmentStatus) {
        viewModelScope.launch {
            appointmentRepository.updateStatus(appointmentId, status)
        }
    }
}
