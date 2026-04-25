package com.dentical.staff.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    ADMIN, STAFF
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val fullName: String,
    val role: UserRole,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
