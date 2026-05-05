package com.dentical.staff.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.dao.UserDao
import com.dentical.staff.data.local.entities.RoleEntity
import com.dentical.staff.data.repository.RoleRepository
import com.dentical.staff.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditUserUiState(
    val fullName: String = "",
    val username: String = "",
    val password: String = "",
    val selectedRoleIds: Set<Long> = emptySet(),
    val isActive: Boolean = true,
    val availableRoles: List<RoleEntity> = emptyList(),
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddEditUserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUserUiState())
    val uiState: StateFlow<AddEditUserUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            roleRepository.getAllRoles().collect { roles ->
                _uiState.update { it.copy(availableRoles = roles) }
            }
        }
    }

    fun loadUser(userId: Long) {
        viewModelScope.launch {
            val withRoles = userRepository.getUserWithRoles(userId) ?: return@launch
            _uiState.update {
                it.copy(
                    fullName        = withRoles.user.fullName,
                    username        = withRoles.user.username,
                    selectedRoleIds = withRoles.roles.map { r -> r.id }.toSet(),
                    isActive        = withRoles.user.isActive,
                    isEditMode      = true
                )
            }
        }
    }

    fun onFullNameChange(v: String) = _uiState.update { it.copy(fullName = v, errorMessage = null) }
    fun onUsernameChange(v: String) = _uiState.update { it.copy(username = v, errorMessage = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, errorMessage = null) }
    fun onActiveChange(v: Boolean)  = _uiState.update { it.copy(isActive = v) }

    fun toggleRole(roleId: Long) {
        _uiState.update {
            val updated = if (roleId in it.selectedRoleIds)
                it.selectedRoleIds - roleId
            else
                it.selectedRoleIds + roleId
            it.copy(selectedRoleIds = updated)
        }
    }

    fun save(editingUserId: Long = -1L) {
        val state = _uiState.value
        val fullName = state.fullName.trim()
        val username = state.username.trim()

        if (fullName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Full name is required") }
            return
        }
        if (username.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Username is required") }
            return
        }
        if (!state.isEditMode && state.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
            return
        }
        if (state.selectedRoleIds.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Assign at least one role") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val taken = userRepository.isUsernameTaken(username, excludeUserId = editingUserId)
            if (taken) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Username already taken") }
                return@launch
            }

            if (state.isEditMode) {
                userRepository.updateUser(
                    userId   = editingUserId,
                    fullName = fullName,
                    roleIds  = state.selectedRoleIds.toList(),
                    isActive = state.isActive
                )
            } else {
                userRepository.createUser(
                    username  = username,
                    password  = state.password,
                    fullName  = fullName,
                    roleIds   = state.selectedRoleIds.toList(),
                    isActive  = state.isActive
                )
            }
            _uiState.update { it.copy(isLoading = false, isSaved = true) }
        }
    }
}
