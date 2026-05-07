package com.dentical.staff.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.entities.UserWithRoles
import com.dentical.staff.data.repository.UserRepository
import com.dentical.staff.data.session.CurrentUserProvider
import com.dentical.staff.data.session.SessionManager
import com.dentical.staff.util.PasswordUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserProfileUiState(
    val currentUser: UserWithRoles? = null,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val currentUserProvider: CurrentUserProvider,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _extras = MutableStateFlow(UserProfileUiState())

    val uiState: StateFlow<UserProfileUiState> = combine(
        currentUserProvider.currentUser, _extras
    ) { user, extras ->
        extras.copy(currentUser = user)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileUiState())

    fun onCurrentPasswordChange(v: String) = _extras.update { it.copy(currentPassword = v, errorMessage = null) }
    fun onNewPasswordChange(v: String)     = _extras.update { it.copy(newPassword = v, errorMessage = null) }
    fun onConfirmPasswordChange(v: String) = _extras.update { it.copy(confirmPassword = v, errorMessage = null) }

    fun changePassword() {
        val state = _extras.value
        val user = uiState.value.currentUser?.user ?: return

        if (state.newPassword.length < 6) {
            _extras.update { it.copy(errorMessage = "New password must be at least 6 characters") }
            return
        }
        if (state.newPassword != state.confirmPassword) {
            _extras.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }
        if (!PasswordUtil.verify(state.currentPassword, user.passwordHash)) {
            _extras.update { it.copy(errorMessage = "Current password is incorrect") }
            return
        }

        viewModelScope.launch {
            _extras.update { it.copy(isLoading = true, errorMessage = null) }
            userRepository.changePassword(user.id, state.newPassword)
            _extras.update {
                it.copy(
                    isLoading       = false,
                    successMessage  = "Password changed successfully",
                    currentPassword = "",
                    newPassword     = "",
                    confirmPassword = ""
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch { sessionManager.clearSession() }
    }

    fun clearMessages() {
        _extras.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
