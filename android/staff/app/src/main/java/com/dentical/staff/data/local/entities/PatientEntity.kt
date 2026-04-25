package com.dentical.staff.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val dateOfBirth: Long,
    val gender: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val medicalConditions: String? = null,
    val allergies: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
