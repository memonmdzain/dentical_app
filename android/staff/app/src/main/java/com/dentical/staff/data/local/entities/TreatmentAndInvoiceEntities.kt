package com.dentical.staff.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TreatmentStatus { ONGOING, COMPLETED, CANCELLED }

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
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["dentistId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("patientId"), Index("dentistId")]
)
data class TreatmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val dentistId: Long? = null,
    val procedure: AppointmentType = AppointmentType.CONSULTATION,
    val toothNumber: String? = null,
    val description: String? = null,
    val quotedCost: Double? = null,
    val visitsRequired: Int? = null,
    val status: TreatmentStatus = TreatmentStatus.ONGOING,
    val startDate: Long = System.currentTimeMillis(),
    val completedDate: Long? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "visits",
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
data class VisitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val visitDate: Long,
    val performedBy: String,
    val amountPaid: Double = 0.0,
    val costCharged: Double = 0.0,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "treatment_visit_cross_ref",
    primaryKeys = ["treatmentId", "visitId"],
    foreignKeys = [
        ForeignKey(
            entity = TreatmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["treatmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = VisitEntity::class,
            parentColumns = ["id"],
            childColumns = ["visitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("visitId")]
)
data class TreatmentVisitCrossRef(
    val treatmentId: Long,
    val visitId: Long,
    val workDone: String
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
