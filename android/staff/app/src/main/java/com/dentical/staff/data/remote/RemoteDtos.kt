package com.dentical.staff.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("id")            val id: Long = 0,
    @SerialName("username")      val username: String,
    @SerialName("password_hash") val passwordHash: String,
    @SerialName("full_name")     val fullName: String,
    @SerialName("role")          val role: String,
    @SerialName("is_active")     val isActive: Boolean = true,
    @SerialName("created_at")    val createdAt: Long
)

@Serializable
data class PatientDto(
    @SerialName("id")                 val id: Long = 0,
    @SerialName("patient_code")       val patientCode: String = "",
    @SerialName("full_name")          val fullName: String,
    @SerialName("date_of_birth")      val dateOfBirth: Long? = null,
    @SerialName("gender")             val gender: String,
    @SerialName("phone")              val phone: String? = null,
    @SerialName("is_phone_available") val isPhoneAvailable: Boolean = true,
    @SerialName("guardian_name")      val guardianName: String? = null,
    @SerialName("guardian_phone")     val guardianPhone: String? = null,
    @SerialName("referral_source")    val referralSource: String,
    @SerialName("referral_detail")    val referralDetail: String? = null,
    @SerialName("email")              val email: String? = null,
    @SerialName("address")            val address: String? = null,
    @SerialName("medical_conditions") val medicalConditions: String? = null,
    @SerialName("allergies")          val allergies: String? = null,
    @SerialName("created_at")         val createdAt: Long,
    @SerialName("updated_at")         val updatedAt: Long
)

@Serializable
data class AppointmentDto(
    @SerialName("id")               val id: Long = 0,
    @SerialName("patient_id")       val patientId: Long,
    @SerialName("dentist_id")       val dentistId: Long? = null,
    @SerialName("type")             val type: String,
    @SerialName("scheduled_at")     val scheduledAt: Long,
    @SerialName("duration_minutes") val durationMinutes: Int = 30,
    @SerialName("status")           val status: String,
    @SerialName("notes")            val notes: String? = null,
    @SerialName("created_at")       val createdAt: Long,
    @SerialName("updated_at")       val updatedAt: Long
)

@Serializable
data class TreatmentDto(
    @SerialName("id")               val id: Long = 0,
    @SerialName("patient_id")       val patientId: Long,
    @SerialName("dentist_id")       val dentistId: Long? = null,
    @SerialName("procedure")        val procedure: String,
    @SerialName("tooth_number")     val toothNumber: String? = null,
    @SerialName("description")      val description: String? = null,
    @SerialName("quoted_cost")      val quotedCost: Double? = null,
    @SerialName("visits_required")  val visitsRequired: Int? = null,
    @SerialName("status")           val status: String,
    @SerialName("start_date")       val startDate: Long,
    @SerialName("completed_date")   val completedDate: Long? = null,
    @SerialName("notes")            val notes: String? = null,
    @SerialName("created_at")       val createdAt: Long,
    @SerialName("updated_at")       val updatedAt: Long
)

@Serializable
data class VisitDto(
    @SerialName("id")           val id: Long = 0,
    @SerialName("patient_id")   val patientId: Long,
    @SerialName("visit_date")   val visitDate: Long,
    @SerialName("performed_by") val performedBy: String,
    @SerialName("amount_paid")  val amountPaid: Double = 0.0,
    @SerialName("cost_charged") val costCharged: Double = 0.0,
    @SerialName("payment_mode") val paymentMode: String? = null,
    @SerialName("notes")        val notes: String? = null,
    @SerialName("created_at")   val createdAt: Long
)

@Serializable
data class TreatmentVisitCrossRefDto(
    @SerialName("treatment_id") val treatmentId: Long,
    @SerialName("visit_id")     val visitId: Long,
    @SerialName("work_done")    val workDone: String
)

@Serializable
data class InvoiceDto(
    @SerialName("id")           val id: Long = 0,
    @SerialName("patient_id")   val patientId: Long,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("paid_amount")  val paidAmount: Double = 0.0,
    @SerialName("status")       val status: String,
    @SerialName("due_date")     val dueDate: Long,
    @SerialName("notes")        val notes: String? = null,
    @SerialName("created_at")   val createdAt: Long,
    @SerialName("updated_at")   val updatedAt: Long
)
