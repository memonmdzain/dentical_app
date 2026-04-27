package com.dentical.staff.ui.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.entities.PatientEntity
import com.dentical.staff.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientDetailUiState(
    val patient: PatientEntity? = null,
    val isLoading: Boolean = true,
    val selectedTab: Int = 0
)

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val repository: PatientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientDetailUiState())
    val uiState: StateFlow<PatientDetailUiState> = _uiState.asStateFlow()

    fun loadPatient(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val patient = repository.getPatientById(id)
            _uiState.update { it.copy(patient = patient, isLoading = false) }
        }
    }

    fun onTabSelected(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}
