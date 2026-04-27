package com.dentical.staff.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "treatments",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AppointmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["appointmentId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("patientId"), Index("appointmentId")]
)
data class TreatmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val appointmentId: Long? = null,
    val procedure: String,
    val description: String? = null,
    val toothNumber: String? = null,
    val performedAt: Long = System.currentTimeMillis(),
    val performedBy: String
)

enum class InvoiceStatus {
    DRAFT, UNPAID, PAID, OVERDUE, CANCELLED
}

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("patientId")]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val status: InvoiceStatus = InvoiceStatus.UNPAID,
    val dueDate: Long,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
