package com.dentical.staff.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.entities.PatientEntity
import com.dentical.staff.data.repository.PatientRepository
import com.dentical.staff.data.repository.TreatmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class DashboardPatient(
    val patient: PatientEntity,
    val outstandingBalance: Double
)

data class PatientListUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val patients: List<DashboardPatient> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardPatientListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val treatmentRepository: TreatmentRepository,
    private val patientRepository: PatientRepository
) : ViewModel() {

    private val filter: String = savedStateHandle["filter"] ?: "ongoing"

    val uiState: StateFlow<PatientListUiState> = when (filter) {
        "ongoing" -> buildOngoingFlow()
        else      -> buildOutstandingFlow()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PatientListUiState()
    )

    private fun buildOngoingFlow(): Flow<PatientListUiState> =
        treatmentRepository.getPatientIdsWithOngoingTreatments()
            .flatMapLatest { ids ->
                flow {
                    emit(PatientListUiState(isLoading = true, title = "Patients with Ongoing Treatments"))
                    val patients = ids.mapNotNull { id ->
                        val patient = patientRepository.getPatientById(id) ?: return@mapNotNull null
                        DashboardPatient(patient, treatmentRepository.computeOutstandingOnce(id))
                    }
                    emit(PatientListUiState(isLoading = false, title = "Patients with Ongoing Treatments", patients = patients))
                }
            }

    private fun buildOutstandingFlow(): Flow<PatientListUiState> =
        patientRepository.getAllPatients()
            .flatMapLatest { allPatients ->
                flow {
                    emit(PatientListUiState(isLoading = true, title = "Patients with Outstanding Balance"))
                    val withOutstanding = allPatients.mapNotNull { patient ->
                        val outstanding = treatmentRepository.computeOutstandingOnce(patient.id)
                        if (outstanding > 0.0) DashboardPatient(patient, outstanding) else null
                    }
                    emit(PatientListUiState(isLoading = false, title = "Patients with Outstanding Balance", patients = withOutstanding))
                }
            }
}
