package com.dentical.staff.data.remote

import com.dentical.staff.data.local.entities.AppointmentEntity
import com.dentical.staff.data.local.entities.InvoiceEntity
import com.dentical.staff.data.local.entities.PatientEntity
import com.dentical.staff.data.local.entities.TreatmentEntity
import com.dentical.staff.data.local.entities.TreatmentVisitCrossRef
import com.dentical.staff.data.local.entities.UserEntity
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
