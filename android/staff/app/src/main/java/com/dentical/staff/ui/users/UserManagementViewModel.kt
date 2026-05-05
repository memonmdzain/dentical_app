package com.dentical.staff.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.dao.UserDao
import com.dentical.staff.data.local.entities.UserWithRoles
import com.dentical.staff.data.repository.UserRepository
import com.dentical.staff.data.session.CurrentUserProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserListUiState(
    val users: List<UserWithRoles> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val canCreate: Boolean = false,
    val canUpdate: Boolean = false,
    val canDelete: Boolean = false
)

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val userDao: UserDao,
    private val userRepository: UserRepository,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    private val _currentUser = currentUserProvider.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val uiState: StateFlow<UserListUiState> = combine(
        userRepository.getAllUsers(),
        _searchQuery,
        _currentUser
    ) { users, query, currentUser ->
        val filtered = if (query.isBlank()) users
            else users.filter {
                it.user.fullName.contains(query, ignoreCase = true) ||
                it.user.username.contains(query, ignoreCase = true)
            }
        UserListUiState(
            users       = filtered,
            searchQuery = query,
            isLoading   = false,
            canCreate   = currentUser?.canCreate("user") == true,
            canUpdate   = currentUser?.canUpdate("user") == true,
            canDelete   = currentUser?.canDelete("user") == true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserListUiState())

    fun onSearchChange(query: String) { _searchQuery.value = query }

    fun toggleActive(userId: Long, currentlyActive: Boolean) {
        if (_currentUser.value?.canUpdate("user") != true) return
        viewModelScope.launch {
            if (currentlyActive) userDao.deactivateUser(userId)
            else userDao.activateUser(userId)
        }
    }
}
