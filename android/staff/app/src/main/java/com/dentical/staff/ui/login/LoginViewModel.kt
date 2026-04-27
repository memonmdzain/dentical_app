package com.dentical.staff.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.dao.UserDao
import com.dentical.staff.util.PasswordUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Invalid username or password"
                    )
                }
                return@launch
            }

            if (!user.isActive) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Your account has been deactivated"
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
        }
    }
}
