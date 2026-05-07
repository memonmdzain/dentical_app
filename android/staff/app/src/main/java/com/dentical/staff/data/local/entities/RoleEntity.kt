package com.dentical.staff.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "roles")
data class RoleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val isSystem: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "permissions",
    foreignKeys = [
        ForeignKey(
            entity = RoleEntity::class,
            parentColumns = ["id"],
            childColumns = ["roleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("roleId"),
        Index(value = ["roleId", "resource"], unique = true)
    ]
)
data class PermissionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val roleId: Long,
    val resource: String,
    val canCreate: Boolean = false,
    val canRead: Boolean = false,
    val canUpdate: Boolean = false,
    val canDelete: Boolean = false
)

@Entity(
    tableName = "user_role_cross_ref",
    primaryKeys = ["userId", "roleId"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RoleEntity::class,
            parentColumns = ["id"],
            childColumns = ["roleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("roleId")]
)
data class UserRoleCrossRef(
    val userId: Long,
    val roleId: Long
)

// ── Permission model used at runtime ─────────────────────────────────────────

data class PermissionFlags(
    val canCreate: Boolean = false,
    val canRead: Boolean = false,
    val canUpdate: Boolean = false,
    val canDelete: Boolean = false
)

data class UserWithRoles(
    val user: UserEntity,
    val roles: List<RoleEntity>,
    val permissions: Map<String, PermissionFlags>
) {
    val isAdmin: Boolean get() = roles.any { it.name == "ADMIN" }

    fun canCreate(resource: String): Boolean = permissions[resource]?.canCreate == true
    fun canRead(resource: String): Boolean = permissions[resource]?.canRead == true
    fun canUpdate(resource: String): Boolean = permissions[resource]?.canUpdate == true
    fun canDelete(resource: String): Boolean = permissions[resource]?.canDelete == true
}

// Merges permissions from multiple roles by OR-ing each flag
fun mergePermissions(permissionList: List<PermissionEntity>): Map<String, PermissionFlags> {
    val map = mutableMapOf<String, PermissionFlags>()
    for (p in permissionList) {
        val existing = map[p.resource] ?: PermissionFlags()
        map[p.resource] = PermissionFlags(
            canCreate = existing.canCreate || p.canCreate,
            canRead   = existing.canRead   || p.canRead,
            canUpdate = existing.canUpdate || p.canUpdate,
            canDelete = existing.canDelete || p.canDelete
        )
    }
    return map
}
