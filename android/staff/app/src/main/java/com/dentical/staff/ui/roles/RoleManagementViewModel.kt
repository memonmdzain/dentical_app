package com.dentical.staff.ui.roles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.entities.RoleEntity
import com.dentical.staff.data.repository.RoleRepository
import com.dentical.staff.data.session.CurrentUserProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoleListUiState(
    val roles: List<RoleEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val canCreate: Boolean = false,
    val canUpdate: Boolean = false,
    val canDelete: Boolean = false
)

@HiltViewModel
class RoleManagementViewModel @Inject constructor(
    private val roleRepository: RoleRepository,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    private val _currentUser = currentUserProvider.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val uiState: StateFlow<RoleListUiState> = combine(
        roleRepository.getAllRoles(),
        _currentUser
    ) { roles, currentUser ->
        RoleListUiState(
            roles     = roles,
            isLoading = false,
            canCreate = currentUser?.canCreate("role") == true,
            canUpdate = currentUser?.canUpdate("role") == true,
            canDelete = currentUser?.canDelete("role") == true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoleListUiState())

    fun deleteRole(role: RoleEntity) {
        if (role.isSystem) return
        if (_currentUser.value?.canDelete("role") != true) return
        viewModelScope.launch { roleRepository.deleteRole(role) }
    }
}
