package com.dentical.staff.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "patients",
    indices = [Index("patientCode", unique = true)]
)
data class PatientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientCode: String = "",        // e.g. 10001, 10002
    val fullName: String,
    val dateOfBirth: Long,
    val gender: String,
    val phone: String? = null,
    val isPhoneAvailable: Boolean = true,
    val guardianName: String? = null,
    val guardianPhone: String? = null,
    val referralSource: String,          // Walk-in, Referral from Doctor, etc.
    val referralDetail: String? = null,  // Doctor name, platform, etc.
    val email: String? = null,
    val address: String? = null,
    val medicalConditions: String? = null,
    val allergies: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
