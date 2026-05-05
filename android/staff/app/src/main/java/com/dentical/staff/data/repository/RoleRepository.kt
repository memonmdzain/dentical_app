package com.dentical.staff.data.repository

import android.util.Log
import com.dentical.staff.data.local.dao.RoleDao
import com.dentical.staff.data.local.entities.PermissionEntity
import com.dentical.staff.data.local.entities.PermissionFlags
import com.dentical.staff.data.local.entities.RoleEntity
import com.dentical.staff.data.remote.PermissionDto
import com.dentical.staff.data.remote.RoleDto
import com.dentical.staff.data.remote.SupabaseSyncHelper
import com.dentical.staff.data.remote.UserRoleCrossRefDto
import com.dentical.staff.data.remote.toDto
import com.dentical.staff.data.remote.toEntity
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class RoleWithPermissions(
    val role: RoleEntity,
    val permissions: Map<String, PermissionFlags>
)

@Singleton
class RoleRepository @Inject constructor(
    private val roleDao: RoleDao,
    private val sync: SupabaseSyncHelper
) {
    fun getAllRoles(): Flow<List<RoleEntity>> = roleDao.getAllRoles()

    suspend fun getRoleWithPermissions(roleId: Long): RoleWithPermissions? {
        val role = roleDao.getRoleById(roleId) ?: return null
        val perms = roleDao.getPermissionsForRoleOnce(roleId)
        val map = perms.associate { p ->
            p.resource to PermissionFlags(p.canCreate, p.canRead, p.canUpdate, p.canDelete)
        }
        return RoleWithPermissions(role, map)
    }

    suspend fun createRole(
        name: String,
        description: String?,
        permissions: Map<String, PermissionFlags>
    ): Long {
        val role = RoleEntity(name = name, description = description, isSystem = false)
        val roleId = roleDao.insertRole(role)
        val permEntities = permissions.map { (resource, flags) ->
            PermissionEntity(
                roleId    = roleId,
                resource  = resource,
                canCreate = flags.canCreate,
                canRead   = flags.canRead,
                canUpdate = flags.canUpdate,
                canDelete = flags.canDelete
            )
        }
        roleDao.upsertPermissions(permEntities)

        val savedRole = role.copy(id = roleId)
        sync.fireAndForget {
            sync.supabase.from("roles").upsert(savedRole.toDto())
            sync.supabase.from("permissions").upsert(permEntities.map { it.toDto() })
        }
        return roleId
    }

    suspend fun updateRole(
        roleId: Long,
        name: String,
        description: String?,
        permissions: Map<String, PermissionFlags>
    ) {
        val existing = roleDao.getRoleById(roleId) ?: return
        val updated = existing.copy(name = name, description = description)
        roleDao.updateRole(updated)

        roleDao.deletePermissionsForRole(roleId)
        val permEntities = permissions.map { (resource, flags) ->
            PermissionEntity(
                roleId    = roleId,
                resource  = resource,
                canCreate = flags.canCreate,
                canRead   = flags.canRead,
                canUpdate = flags.canUpdate,
                canDelete = flags.canDelete
            )
        }
        roleDao.upsertPermissions(permEntities)

        sync.fireAndForget {
            sync.supabase.from("roles").upsert(updated.toDto())
            sync.supabase.from("permissions").delete { filter { eq("role_id", roleId) } }
            sync.supabase.from("permissions").upsert(permEntities.map { it.toDto() })
        }
    }

    suspend fun deleteRole(role: RoleEntity) {
        if (role.isSystem) return
        roleDao.deleteRole(role)
        sync.delete("roles", role.id)
    }

    suspend fun pullFromSupabase() {
        if (!sync.isConnected) return
        try {
            val roleDtos = sync.supabase.from("roles").select().decodeList<RoleDto>()
            roleDao.upsertAllRoles(roleDtos.map { it.toEntity() })

            val permDtos = sync.supabase.from("permissions").select().decodeList<PermissionDto>()
            roleDao.upsertAllPermissions(permDtos.map { it.toEntity() })

            val crossRefDtos = sync.supabase.from("user_role_cross_ref").select().decodeList<UserRoleCrossRefDto>()
            roleDao.upsertAllUserRoleCrossRefs(crossRefDtos.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Pull roles failed", e)
        }
    }
}
