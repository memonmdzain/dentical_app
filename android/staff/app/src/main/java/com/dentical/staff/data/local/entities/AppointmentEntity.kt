package com.dentical.staff.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AppointmentStatus {
    SCHEDULED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW
}

enum class AppointmentType(val displayName: String) {
    CONSULTATION("Consultation"),
    CLEANING("Cleaning / Prophylaxis"),
    FILLING("Filling"),
    ROOT_CANAL("Root Canal"),
    EXTRACTION("Extraction"),
    BRACES("Braces / Orthodontics"),
    XRAY("X-Ray"),
    WHITENING("Whitening"),
    CROWN_BRIDGE("Crown / Bridge"),
    OTHER("Other")
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
            childColumns = ["dentistId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("patientId"),
        Index("dentistId"),
        Index("scheduledAt")
    ]
)
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val dentistId: Long? = null,
    val type: AppointmentType = AppointmentType.CONSULTATION,
    val scheduledAt: Long,                          // epoch millis — date + time
    val durationMinutes: Int = 30,
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
