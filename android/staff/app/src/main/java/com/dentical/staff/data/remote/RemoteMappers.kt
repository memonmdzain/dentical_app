package com.dentical.staff.data.remote

import com.dentical.staff.data.local.entities.AppointmentEntity
import com.dentical.staff.data.local.entities.AppointmentStatus
import com.dentical.staff.data.local.entities.AppointmentType
import com.dentical.staff.data.local.entities.InvoiceEntity
import com.dentical.staff.data.local.entities.InvoiceStatus
import com.dentical.staff.data.local.entities.PatientEntity
import com.dentical.staff.data.local.entities.PaymentMode
import com.dentical.staff.data.local.entities.TreatmentEntity
import com.dentical.staff.data.local.entities.TreatmentStatus
import com.dentical.staff.data.local.entities.TreatmentVisitCrossRef
import com.dentical.staff.data.local.entities.UserEntity
import com.dentical.staff.data.local.entities.UserRole
import com.dentical.staff.data.local.entities.VisitEntity

fun UserEntity.toDto() = UserDto(
    id           = id,
    username     = username,
    passwordHash = passwordHash,
    fullName     = fullName,
    role         = role.name,
    isActive     = isActive,
    createdAt    = createdAt
)

fun PatientEntity.toDto() = PatientDto(
    id                = id,
    patientCode       = patientCode,
    fullName          = fullName,
    dateOfBirth       = dateOfBirth,
    gender            = gender,
    phone             = phone,
    isPhoneAvailable  = isPhoneAvailable,
    guardianName      = guardianName,
    guardianPhone     = guardianPhone,
    referralSource    = referralSource,
    referralDetail    = referralDetail,
    email             = email,
    address           = address,
    medicalConditions = medicalConditions,
    allergies         = allergies,
    createdAt         = createdAt,
    updatedAt         = updatedAt
)

fun AppointmentEntity.toDto() = AppointmentDto(
    id              = id,
    patientId       = patientId,
    dentistId       = dentistId,
    type            = type.name,
    scheduledAt     = scheduledAt,
    durationMinutes = durationMinutes,
    status          = status.name,
    notes           = notes,
    createdAt       = createdAt,
    updatedAt       = updatedAt
)

fun TreatmentEntity.toDto() = TreatmentDto(
    id             = id,
    patientId      = patientId,
    dentistId      = dentistId,
    procedure      = procedure.name,
    toothNumber    = toothNumber,
    description    = description,
    quotedCost     = quotedCost,
    visitsRequired = visitsRequired,
    status         = status.name,
    startDate      = startDate,
    completedDate  = completedDate,
    notes          = notes,
    createdAt      = createdAt,
    updatedAt      = updatedAt
)

fun VisitEntity.toDto() = VisitDto(
    id          = id,
    patientId   = patientId,
    visitDate   = visitDate,
    performedBy = performedBy,
    amountPaid  = amountPaid,
    costCharged = costCharged,
    paymentMode = paymentMode?.name,
    notes       = notes,
    createdAt   = createdAt
)

fun TreatmentVisitCrossRef.toDto() = TreatmentVisitCrossRefDto(
    treatmentId = treatmentId,
    visitId     = visitId,
    workDone    = workDone
)

// ── Reverse mappers: DTO → Room entity (used by pull-from-Supabase) ──────────

fun PatientDto.toEntity() = PatientEntity(
    id                = id,
    patientCode       = patientCode,
    fullName          = fullName,
    dateOfBirth       = dateOfBirth ?: 0L,
    gender            = gender,
    phone             = phone,
    isPhoneAvailable  = isPhoneAvailable,
    guardianName      = guardianName,
    guardianPhone     = guardianPhone,
    referralSource    = referralSource,
    referralDetail    = referralDetail,
    email             = email,
    address           = address,
    medicalConditions = medicalConditions,
    allergies         = allergies,
    createdAt         = createdAt,
    updatedAt         = updatedAt
)

fun AppointmentDto.toEntity() = AppointmentEntity(
    id              = id,
    patientId       = patientId,
    dentistId       = dentistId,
    type            = runCatching { AppointmentType.valueOf(type) }.getOrDefault(AppointmentType.CONSULTATION),
    scheduledAt     = scheduledAt,
    durationMinutes = durationMinutes,
    status          = runCatching { AppointmentStatus.valueOf(status) }.getOrDefault(AppointmentStatus.SCHEDULED),
    notes           = notes,
    createdAt       = createdAt,
    updatedAt       = updatedAt
)

fun TreatmentDto.toEntity() = TreatmentEntity(
    id             = id,
    patientId      = patientId,
    dentistId      = dentistId,
    procedure      = runCatching { AppointmentType.valueOf(procedure) }.getOrDefault(AppointmentType.CONSULTATION),
    toothNumber    = toothNumber,
    description    = description,
    quotedCost     = quotedCost,
    visitsRequired = visitsRequired,
    status         = runCatching { TreatmentStatus.valueOf(status) }.getOrDefault(TreatmentStatus.ONGOING),
    startDate      = startDate,
    completedDate  = completedDate,
    notes          = notes,
    createdAt      = createdAt,
    updatedAt      = updatedAt
)

fun VisitDto.toEntity() = VisitEntity(
    id          = id,
    patientId   = patientId,
    visitDate   = visitDate,
    performedBy = performedBy,
    amountPaid  = amountPaid,
    costCharged = costCharged,
    paymentMode = paymentMode?.let { runCatching { PaymentMode.valueOf(it) }.getOrNull() },
    notes       = notes,
    createdAt   = createdAt
)

fun TreatmentVisitCrossRefDto.toEntity() = TreatmentVisitCrossRef(
    treatmentId = treatmentId,
    visitId     = visitId,
    workDone    = workDone
)

fun InvoiceEntity.toDto() = InvoiceDto(
    id          = id,
    patientId   = patientId,
    totalAmount = totalAmount,
    paidAmount  = paidAmount,
    status      = status.name,
    dueDate     = dueDate,
    notes       = notes,
    createdAt   = createdAt,
    updatedAt   = updatedAt
)
