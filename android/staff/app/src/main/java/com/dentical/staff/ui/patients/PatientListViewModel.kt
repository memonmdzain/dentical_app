package com.dentical.staff.ui.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.entities.PatientEntity
import com.dentical.staff.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientListUiState(
    val patients: List<PatientEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class PatientListViewModel @Inject constructor(
    private val repository: PatientRepository
) : ViewModel() {

    init {
        viewModelScope.launch { repository.pullFromSupabase() }
    }

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<PatientListUiState> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            _isLoading.value = false
            if (query.isBlank()) repository.getAllPatients()
            else repository.searchPatients(query)
        }
        .combine(_searchQuery) { patients, query ->
            PatientListUiState(
                patients = patients,
                searchQuery = query,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PatientListUiState()
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}
