package com.dentical.staff.ui.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.entities.PatientEntity
import com.dentical.staff.data.local.entities.TreatmentEntity
import com.dentical.staff.data.local.entities.TreatmentVisitCrossRef
import com.dentical.staff.data.local.entities.VisitEntity
import com.dentical.staff.data.repository.PatientFinancialSummary
import com.dentical.staff.data.repository.PatientRepository
import com.dentical.staff.data.repository.TreatmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientDetailUiState(
    val patient: PatientEntity? = null,
    val isLoading: Boolean = true,
    val selectedTab: Int = 0,
    val treatments: List<TreatmentEntity> = emptyList(),
    val visits: List<VisitEntity> = emptyList(),
    val visitCrossRefs: Map<Long, List<TreatmentVisitCrossRef>> = emptyMap(),
    val financialSummary: PatientFinancialSummary = PatientFinancialSummary(0.0, 0.0, 0.0, 0.0)
)

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val treatmentRepository: TreatmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientDetailUiState())
    val uiState: StateFlow<PatientDetailUiState> = _uiState.asStateFlow()

    private var loadedPatientId = -1L

    fun loadPatient(id: Long) {
        if (loadedPatientId == id) return
        loadedPatientId = id

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val patient = patientRepository.getPatientById(id)
                _uiState.update { it.copy(patient = patient, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        viewModelScope.launch {
            treatmentRepository.getTreatmentsByPatient(id)
                .catch { /* ignore — show empty list */ }
                .collect { treatments ->
                    _uiState.update { it.copy(treatments = treatments) }
                }
        }

        viewModelScope.launch {
            treatmentRepository.getVisitsByPatient(id)
                .catch { /* ignore — show empty list */ }
                .collect { visits ->
                    val crossRefMap = mutableMapOf<Long, List<TreatmentVisitCrossRef>>()
                    try {
                        visits.forEach { visit ->
                            crossRefMap[visit.id] = treatmentRepository.getCrossRefsForVisit(visit.id)
                        }
                    } catch (e: Exception) {
                        // crossRef lookup failed — show visits without linked treatment detail
                    }
                    _uiState.update { it.copy(visits = visits, visitCrossRefs = crossRefMap) }
                }
        }

        viewModelScope.launch {
            treatmentRepository.getPatientFinancialSummary(id)
                .catch { emit(PatientFinancialSummary(0.0, 0.0, 0.0, 0.0)) }
                .collect { summary ->
                    _uiState.update { it.copy(financialSummary = summary) }
                }
        }
    }

    fun onTabSelected(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}
