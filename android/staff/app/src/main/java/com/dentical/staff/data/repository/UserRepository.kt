package com.dentical.staff.data.repository

import android.util.Log
import com.dentical.staff.data.local.dao.RoleDao
import com.dentical.staff.data.local.dao.UserDao
import com.dentical.staff.data.local.entities.UserEntity
import com.dentical.staff.data.local.entities.UserRoleCrossRef
import com.dentical.staff.data.local.entities.UserWithRoles
import com.dentical.staff.data.local.entities.mergePermissions
import com.dentical.staff.data.remote.SupabaseSyncHelper
import com.dentical.staff.data.remote.UserDto
import com.dentical.staff.data.remote.toEntity
import com.dentical.staff.util.PasswordUtil
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val roleDao: RoleDao,
    private val sync: SupabaseSyncHelper
) {
    fun getAllUsers(): Flow<List<UserWithRoles>> =
        userDao.getAllUsers().flatMapLatest { users ->
            if (users.isEmpty()) return@flatMapLatest flowOf(emptyList())
            combine(users.map { user ->
                roleDao.getRolesForUser(user.id).map { roles ->
                    val roleIds = roles.map { it.id }
                    val perms = if (roleIds.isEmpty()) emptyList()
                                else roleDao.getPermissionsForRoles(roleIds)
                    UserWithRoles(user, roles, mergePermissions(perms))
                }
            }) { it.toList() }
        }

    suspend fun getUserWithRoles(userId: Long): UserWithRoles? {
        val user = userDao.getUserById(userId) ?: return null
        val roles = roleDao.getRolesForUserOnce(userId)
        val roleIds = roles.map { it.id }
        val permissions = if (roleIds.isEmpty()) emptyList()
            else roleDao.getPermissionsForRoles(roleIds)
        return UserWithRoles(user, roles, mergePermissions(permissions))
    }

    suspend fun createUser(
        username: String,
        password: String,
        fullName: String,
        roleIds: List<Long>,
        isActive: Boolean = true
    ): Long {
        val user = UserEntity(
            username     = username,
            passwordHash = PasswordUtil.hash(password),
            fullName     = fullName,
            isActive     = isActive
        )
        val id = userDao.insertUser(user)
        val crossRefs = roleIds.map { UserRoleCrossRef(id, it) }
        roleDao.insertUserRoleCrossRefs(crossRefs)

        sync.fireAndForget {
            sync.supabase.from("users").upsert(
                mapOf(
                    "id"            to id,
                    "username"      to username,
                    "password_hash" to user.passwordHash,
                    "full_name"     to fullName,
                    "is_active"     to isActive,
                    "created_at"    to user.createdAt
                )
            )
            crossRefs.forEach { ref ->
                sync.supabase.from("user_role_cross_ref").upsert(
                    mapOf("user_id" to ref.userId, "role_id" to ref.roleId)
                )
            }
        }
        return id
    }

    suspend fun updateUser(
        userId: Long,
        fullName: String,
        roleIds: List<Long>,
        isActive: Boolean
    ) {
        val existing = userDao.getUserById(userId) ?: return
        val updated = existing.copy(fullName = fullName, isActive = isActive)
        userDao.updateUser(updated)

        roleDao.deleteUserRoles(userId)
        val crossRefs = roleIds.map { UserRoleCrossRef(userId, it) }
        roleDao.insertUserRoleCrossRefs(crossRefs)

        sync.fireAndForget {
            sync.supabase.from("users").upsert(
                mapOf(
                    "id"         to userId,
                    "username"   to existing.username,
                    "password_hash" to existing.passwordHash,
                    "full_name"  to fullName,
                    "is_active"  to isActive,
                    "created_at" to existing.createdAt
                )
            )
            sync.supabase.from("user_role_cross_ref").delete {
                filter { eq("user_id", userId) }
            }
            crossRefs.forEach { ref ->
                sync.supabase.from("user_role_cross_ref").upsert(
                    mapOf("user_id" to ref.userId, "role_id" to ref.roleId)
                )
            }
        }
    }

    suspend fun changePassword(userId: Long, newPassword: String) {
        val existing = userDao.getUserById(userId) ?: return
        val updated = existing.copy(passwordHash = PasswordUtil.hash(newPassword))
        userDao.updateUser(updated)
        sync.fireAndForget {
            sync.supabase.from("users").upsert(
                mapOf(
                    "id"            to userId,
                    "username"      to existing.username,
                    "password_hash" to updated.passwordHash,
                    "full_name"     to existing.fullName,
                    "is_active"     to existing.isActive,
                    "created_at"    to existing.createdAt
                )
            )
        }
    }

    suspend fun isUsernameTaken(username: String, excludeUserId: Long = -1L): Boolean {
        val user = userDao.getUserByUsername(username) ?: return false
        return user.id != excludeUserId
    }

    suspend fun pullFromSupabase() {
        if (!sync.isConnected) return
        try {
            val dtos = sync.supabase.from("users").select().decodeList<UserDto>()
            userDao.upsertAll(dtos.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Pull users failed", e)
        }
    }
}
