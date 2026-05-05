package com.dentical.staff.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Kept for migration seeding only — no longer stored on UserEntity
enum class UserRole { ADMIN, DENTIST, STAFF }

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val fullName: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val googleId: String? = null
)
