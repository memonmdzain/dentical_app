package com.dentical.staff.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.dao.RoleDao
import com.dentical.staff.data.local.dao.UserDao
import com.dentical.staff.data.local.entities.RoleEntity
import com.dentical.staff.data.local.entities.UserEntity
import com.dentical.staff.data.local.entities.UserRoleCrossRef
import com.dentical.staff.data.local.entities.UserWithRoles
import com.dentical.staff.data.local.entities.mergePermissions
import com.dentical.staff.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserListUiState(
    val users: List<UserWithRoles> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val userDao: UserDao,
    private val roleDao: RoleDao,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(true)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UserListUiState> = combine(
        userDao.getAllUsers(),
        _searchQuery,
        _isLoading
    ) { users, query, loading ->
        val filtered = if (query.isBlank()) users
            else users.filter {
                it.fullName.contains(query, ignoreCase = true) ||
                it.username.contains(query, ignoreCase = true)
            }
        Triple(filtered, query, loading)
    }.flatMapLatest { (filtered, query, loading) ->
        // Enrich each user with their roles and permissions
        val enriched = filtered.map { user ->
            val roles = roleDao.getRolesForUserOnce(user.id)
            val roleIds = roles.map { it.id }
            val permissions = if (roleIds.isEmpty()) emptyList()
                else roleDao.getPermissionsForRoles(roleIds)
            UserWithRoles(user, roles, mergePermissions(permissions))
        }
        flowOf(UserListUiState(enriched, query, loading))
    }
    .onStart { _isLoading.value = false }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserListUiState())

    fun onSearchChange(query: String) { _searchQuery.value = query }

    fun toggleActive(userId: Long, currentlyActive: Boolean) {
        viewModelScope.launch {
            if (currentlyActive) userDao.deactivateUser(userId)
            else userDao.activateUser(userId)
        }
    }
}
