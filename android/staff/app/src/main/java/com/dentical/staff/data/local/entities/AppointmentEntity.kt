package com.dentical.staff.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AppointmentStatus {
    SCHEDULED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW
}

@Entity(
    tableName = "appointments",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["staffId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("patientId"),
        Index("staffId"),
        Index("scheduledAt")
    ]
)
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val staffId: Long? = null,
    val title: String,
    val notes: String? = null,
    val scheduledAt: Long,
    val durationMinutes: Int = 30,
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
