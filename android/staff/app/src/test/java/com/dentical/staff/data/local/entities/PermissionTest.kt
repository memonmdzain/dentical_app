package com.dentical.staff.data.local.entities

import org.junit.Assert.*
import org.junit.Test

class PermissionTest {

    // ── Permission Merging ─────────────────────────────────────────────────────

    @Test
    fun `when a user has no roles assigned then they have no permissions on any resource`() {
        val result = mergePermissions(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `when a user has one role with full access to patients then they can create read update and delete patients`() {
        val permissions = listOf(
            PermissionEntity(roleId = 1, resource = "patients",
                canCreate = true, canRead = true, canUpdate = true, canDelete = true)
        )
        val result = mergePermissions(permissions)
        val flags = result["patients"]!!
        assertTrue(flags.canCreate)
        assertTrue(flags.canRead)
        assertTrue(flags.canUpdate)
        assertTrue(flags.canDelete)
    }

    @Test
    fun `when a user has two roles where one allows creating appointments and the other does not then they can still create appointments`() {
        val permissions = listOf(
            PermissionEntity(roleId = 1, resource = "appointments", canCreate = true),
            PermissionEntity(roleId = 2, resource = "appointments", canCreate = false)
        )
        val result = mergePermissions(permissions)
        assertTrue(result["appointments"]!!.canCreate)
    }

    @Test
    fun `when a user has two roles that both deny deleting a resource then they cannot delete that resource`() {
        val permissions = listOf(
            PermissionEntity(roleId = 1, resource = "treatments", canDelete = false),
            PermissionEntity(roleId = 2, resource = "treatments", canDelete = false)
        )
        val result = mergePermissions(permissions)
        assertFalse(result["treatments"]!!.canDelete)
    }

    @Test
    fun `when a user has roles covering different resources then permissions from both resources are available independently`() {
        val permissions = listOf(
            PermissionEntity(roleId = 1, resource = "patients", canRead = true),
            PermissionEntity(roleId = 1, resource = "appointments", canCreate = true)
        )
        val result = mergePermissions(permissions)
        assertTrue(result["patients"]!!.canRead)
        assertTrue(result["appointments"]!!.canCreate)
    }

    @Test
    fun `when two roles both partially cover the same resource then the final permissions are the union of both roles flags`() {
        val permissions = listOf(
            PermissionEntity(roleId = 1, resource = "treatments", canCreate = true, canRead = true),
            PermissionEntity(roleId = 2, resource = "treatments", canUpdate = true, canDelete = true)
        )
        val result = mergePermissions(permissions)
        val flags = result["treatments"]!!
        assertTrue(flags.canCreate)
        assertTrue(flags.canRead)
        assertTrue(flags.canUpdate)
        assertTrue(flags.canDelete)
    }

    // ── Admin Check ───────────────────────────────────────────────────────────

    @Test
    fun `when a user is assigned the ADMIN role then they are recognized as an admin`() {
        val user = userWithRoles(listOf(role("ADMIN")))
        assertTrue(user.isAdmin)
    }

    @Test
    fun `when a user is assigned only DENTIST and STAFF roles then they are not recognized as an admin`() {
        val user = userWithRoles(listOf(role("DENTIST"), role("STAFF")))
        assertFalse(user.isAdmin)
    }

    @Test
    fun `when a user has no roles at all then they are not recognized as an admin`() {
        val user = userWithRoles(emptyList())
        assertFalse(user.isAdmin)
    }

    // ── Per-Resource Permission Check ─────────────────────────────────────────

    @Test
    fun `when a user has access to patients but not settings then checking settings returns denied`() {
        val permissions = mapOf(
            "patients" to PermissionFlags(canRead = true)
        )
        val user = userWithRoles(emptyList(), permissions)
        assertTrue(user.canRead("patients"))
        assertFalse(user.canRead("settings"))
    }

    @Test
    fun `when a resource has never been assigned to any role then all four permission checks return denied`() {
        val user = userWithRoles(emptyList(), emptyMap())
        assertFalse(user.canCreate("invoices"))
        assertFalse(user.canRead("invoices"))
        assertFalse(user.canUpdate("invoices"))
        assertFalse(user.canDelete("invoices"))
    }

    @Test
    fun `when a user can update treatments then that does not imply they can delete treatments`() {
        val permissions = mapOf(
            "treatments" to PermissionFlags(canUpdate = true, canDelete = false)
        )
        val user = userWithRoles(emptyList(), permissions)
        assertTrue(user.canUpdate("treatments"))
        assertFalse(user.canDelete("treatments"))
    }

    @Test
    fun `when at least one role grants canDelete for a resource then the user can delete that resource regardless of other roles`() {
        val permissions = listOf(
            PermissionEntity(roleId = 1, resource = "patients", canDelete = false),
            PermissionEntity(roleId = 2, resource = "patients", canDelete = true)
        )
        val result = mergePermissions(permissions)
        val user = userWithRoles(emptyList(), result)
        assertTrue(user.canDelete("patients"))
    }

    // ── Role Properties ───────────────────────────────────────────────────────

    @Test
    fun `when a role is marked as a system role then it is distinguishable from custom roles`() {
        val systemRole = RoleEntity(id = 1, name = "ADMIN", isSystem = true)
        val customRole = RoleEntity(id = 2, name = "Receptionist", isSystem = false)
        assertTrue(systemRole.isSystem)
        assertFalse(customRole.isSystem)
    }

    @Test
    fun `when a role has no description then it is still valid and usable`() {
        val role = RoleEntity(id = 1, name = "STAFF", description = null, isSystem = true)
        assertEquals("STAFF", role.name)
        assertNull(role.description)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun role(name: String) = RoleEntity(id = 0, name = name)

    private fun userWithRoles(
        roles: List<RoleEntity>,
        permissions: Map<String, PermissionFlags> = emptyMap()
    ) = UserWithRoles(
        user = UserEntity(
            id = 1,
            username = "testuser",
            passwordHash = "hash",
            fullName = "Test User"
        ),
        roles = roles,
        permissions = permissions
    )
}
