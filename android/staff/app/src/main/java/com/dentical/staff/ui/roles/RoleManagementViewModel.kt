package com.dentical.staff.ui.roles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.entities.RoleEntity
import com.dentical.staff.data.repository.RoleRepository
import com.dentical.staff.data.repository.RoleWithPermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoleListUiState(
    val roles: List<RoleEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class RoleManagementViewModel @Inject constructor(
    private val roleRepository: RoleRepository
) : ViewModel() {

    val uiState: StateFlow<RoleListUiState> = roleRepository.getAllRoles()
        .map { RoleListUiState(roles = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoleListUiState())

    fun deleteRole(role: RoleEntity) {
        if (role.isSystem) return
        viewModelScope.launch { roleRepository.deleteRole(role) }
    }
}
