package com.dentical.staff.data.session

import com.dentical.staff.data.local.dao.RoleDao
import com.dentical.staff.data.local.dao.UserDao
import com.dentical.staff.data.local.entities.UserWithRoles
import com.dentical.staff.data.local.entities.mergePermissions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentUserProvider @Inject constructor(
    private val sessionManager: SessionManager,
    private val userDao: UserDao,
    private val roleDao: RoleDao
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: Flow<UserWithRoles?> = sessionManager.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) return@flatMapLatest flowOf(null)

            val userFlow = userDao.getUserByIdFlow(userId)
            val rolesFlow = roleDao.getRolesForUser(userId)

            combine(userFlow, rolesFlow) { user, roles ->
                if (user == null) return@combine null
                val roleIds = roles.map { it.id }
                val permissions = if (roleIds.isEmpty()) emptyList()
                    else roleDao.getPermissionsForRoles(roleIds)
                UserWithRoles(
                    user        = user,
                    roles       = roles,
                    permissions = mergePermissions(permissions)
                )
            }
        }
}
