package com.dentical.staff.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.dao.UserDao
import com.dentical.staff.data.remote.SyncManager
import com.dentical.staff.data.session.SessionManager
import com.dentical.staff.util.PasswordUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onSync() = syncManager.syncAll()

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onLogin() {
        val username = _uiState.value.username.trim()
        val password = _uiState.value.password

        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter username and password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val user = userDao.getUserByUsername(username)

            if (user == null || !PasswordUtil.verify(password, user.passwordHash)) {
                val msg = if (user == null && syncManager.isSyncing.value)
                    "Still syncing from server — please wait and try again"
                else
                    "Invalid username or password"
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                return@launch
            }

            if (!user.isActive) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Your account has been deactivated") }
                return@launch
            }

            sessionManager.setSession(user.id)
            _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
        }
    }
}
