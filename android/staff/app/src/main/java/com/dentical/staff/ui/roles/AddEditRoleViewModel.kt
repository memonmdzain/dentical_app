package com.dentical.staff.ui.roles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.entities.PermissionFlags
import com.dentical.staff.data.repository.RoleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

val ALL_RESOURCES = listOf("patient", "appointment", "treatment", "visit", "invoice", "user", "role")

data class AddEditRoleUiState(
    val name: String = "",
    val description: String = "",
    val permissions: Map<String, PermissionFlags> = ALL_RESOURCES.associateWith { PermissionFlags() },
    val isSystem: Boolean = false,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddEditRoleViewModel @Inject constructor(
    private val roleRepository: RoleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditRoleUiState())
    val uiState: StateFlow<AddEditRoleUiState> = _uiState.asStateFlow()

    fun loadRole(roleId: Long) {
        viewModelScope.launch {
            val withPerms = roleRepository.getRoleWithPermissions(roleId) ?: return@launch
            // Merge loaded permissions with defaults for any missing resources
            val merged = ALL_RESOURCES.associateWith { res ->
                withPerms.permissions[res] ?: PermissionFlags()
            }
            _uiState.update {
                it.copy(
                    name        = withPerms.role.name,
                    description = withPerms.role.description ?: "",
                    permissions = merged,
                    isSystem    = withPerms.role.isSystem,
                    isEditMode  = true
                )
            }
        }
    }

    fun onNameChange(v: String)        = _uiState.update { it.copy(name = v, errorMessage = null) }
    fun onDescriptionChange(v: String) = _uiState.update { it.copy(description = v) }

    fun togglePermission(resource: String, flag: String) {
        _uiState.update { state ->
            val current = state.permissions[resource] ?: PermissionFlags()
            val updated = when (flag) {
                "create" -> current.copy(canCreate = !current.canCreate)
                "read"   -> current.copy(canRead   = !current.canRead)
                "update" -> current.copy(canUpdate = !current.canUpdate)
                "delete" -> current.copy(canDelete = !current.canDelete)
                else     -> current
            }
            state.copy(permissions = state.permissions + (resource to updated))
        }
    }

    fun save(editingRoleId: Long = -1L) {
        val state = _uiState.value
        if (state.isSystem) return
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Role name is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                if (state.isEditMode) {
                    roleRepository.updateRole(
                        roleId      = editingRoleId,
                        name        = state.name.trim(),
                        description = state.description.ifBlank { null },
                        permissions = state.permissions
                    )
                } else {
                    roleRepository.createRole(
                        name        = state.name.trim(),
                        description = state.description.ifBlank { null },
                        permissions = state.permissions
                    )
                }
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Save failed") }
            }
        }
    }
}
